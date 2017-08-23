/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2015, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */

package org.zenoss.app.metricservice.v2.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.impl.*;
import org.zenoss.app.metricservice.api.impl.QueryStatus.QueryStatusEnum;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.api.model.v2.*;
import org.zenoss.app.metricservice.api.model.v2.QueryResult;
import org.zenoss.app.metricservice.buckets.Value;
import org.zenoss.app.metricservice.calculators.*;
import org.zenoss.app.metricservice.v2.QueryService;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.io.Writer;
import java.io.IOException;

@API
public class QueryServiceImpl implements QueryService {
    private static final Logger log = LoggerFactory.getLogger(QueryServiceImpl.class);

    @Autowired
    public MetricServiceAppConfiguration config;

    @Autowired
    public MetricStorageAPI metricStorage;


    @Override
    public QueryResult query(final MetricRequest query) {
        log.debug("Thread {}: entering MetricService.query()", Thread.currentThread().getId());

        //metrics that have calculation expression
        List<MetricQuery> expressionQueries = Lists.newArrayListWithCapacity(query.getQueries().size());
        //metrics that have no calculation expression
        List<MetricQuery> simpleQueries = Lists.newArrayListWithCapacity(query.getQueries().size());

        for (MetricQuery metricQuery : query.getQueries()) {
            if (Strings.isNullOrEmpty(metricQuery.getExpression())) {
                simpleQueries.add(metricQuery);
            } else {
                expressionQueries.add(metricQuery);
            }
        }
        QueryResultBuilder qrb = new QueryResultBuilder();
        Iterable<OpenTSDBQueryResult> metrics = null;
        if (!simpleQueries.isEmpty()) {
            for (MetricQuery mq : simpleQueries) {
                OpenTSDBQueryReturn otsdbResults = getOpenTSDBQueryResults(Collections.singletonList(mq), query);
                qrb.setStatus(otsdbResults.getStatus());
                for (OpenTSDBQueryResult m : otsdbResults.getResults()) {
                    qrb.addSeries(m.metric, m.getDataPoints(), m.tags);
                }
            }
        }
        if (!expressionQueries.isEmpty()) {
            //group by expression so we can query apply the same expression to all results of a query
            ImmutableMap<String, Collection<MetricQuery>> grouped = Multimaps.index(expressionQueries, new Function<MetricQuery, String>() {
                @Nullable
                @Override
                public String apply(@Nullable MetricQuery metricQuery) {
                    return metricQuery.getExpression();
                }
            }).asMap();

            for (Entry<String, Collection<MetricQuery>> specs : grouped.entrySet()) {
                OpenTSDBQueryReturn otsdbResults = getOpenTSDBQueryResults(specs.getValue(), query);
                //TODO: check result and throw exception?
                //APPLY RPN here
                try {
                    applyRPN(specs, otsdbResults.getResults());
                    for (OpenTSDBQueryResult m : otsdbResults.getResults()) {
                        qrb.addSeries(m.metric, m.getDataPoints(), m.tags);
                    }
                    qrb.setStatus(otsdbResults.getStatus());
                } catch (UnknownReferenceException | BadExpressionException e) {
                    QueryStatus status = new QueryStatus();
                    status.setMessage(e.getMessage());
                    status.setStatus(QueryStatusEnum.ERROR);
                    qrb.setStatus(status);
                }
            }
        }
        return qrb.build();
    }

    @Override
    public void rename(RenameRequest renameRequest, Writer writer) {
        String patternType = renameRequest.getPatternType();
        if (patternType.equals(RenameRequest.PTYPE_PREFIX)) {
            metricStorage.renamePrefix(renameRequest, writer);
        } else if (patternType.equals(RenameRequest.PTYPE_WHOLE)) {
            metricStorage.renameWhole(renameRequest, writer);
        }
    }

    private OpenTSDBQueryReturn getOpenTSDBQueryResults(Collection<MetricQuery> metricQueries, MetricRequest query) {
        MetricRequest newQuery = new MetricRequest();
        newQuery.setStart(query.getStart());
        newQuery.setEnd(query.getEnd());
        newQuery.setReturnset(query.getReturnset());
        newQuery.setQueries(metricQueries);
        OpenTSDBQueryReturn results = metricStorage.query(newQuery);

        Optional<String> start = Optional.fromNullable(query.getStart());
        Optional<String> end = Optional.fromNullable(query.getEnd());
        Optional<ReturnSet> returnset = Optional.fromNullable(query.getReturnset());
        String startTime = start.or(config.getMetricServiceConfig().getDefaultStartTime());
        String endTime = end.or(config.getMetricServiceConfig().getDefaultEndTime());
        ReturnSet returnSet = returnset.or(config.getMetricServiceConfig().getDefaultReturnSet());

        List<Object> errors = new ArrayList<>();
        // Validate start time
        long startTimestamp = parseTimeWithErrorHandling(startTime, Utils.START, errors);
        // Validate end time
        long endTimestamp = parseTimeWithErrorHandling(endTime, Utils.END, errors);
        if (ReturnSet.ALL != returnSet) {
            for (OpenTSDBQueryResult series : results.getResults()) {
                if (returnSet == ReturnSet.LAST) {
                    filterLastReturnSet(startTimestamp, endTimestamp, series);
                } else if (returnSet == ReturnSet.EXACT) {
                    filterExactReturnSet(startTimestamp, endTimestamp, series);
                }
            }
        }
        return results;
    }

    private void applyRPN(Entry<String, Collection<MetricQuery>> specs, Iterable<OpenTSDBQueryResult> result) throws UnknownReferenceException, BadExpressionException {
        for (final OpenTSDBQueryResult r : result) {
            MetricCalculator calc;
            try {
                calc = MetricCalculatorFactory.newInstance(specs.getKey());
                calc.setReferenceProvider(new ReferenceProvider() {
                    @Override
                    public double lookup(String name, Closure closure) throws UnknownReferenceException {
                        if (null == closure) {
                            throw new NullPointerException("null closure passed to lookup() method.");
                        }
                        /**
                         * If they are looking for special values like "time" then give them
                         * that.
                         */
                        if ("time".equalsIgnoreCase(name)) {
                            return closure.getTimeStamp();
                        }

                        /**
                         * Check for metrics or values in the bucket
                         */
                        Value v = closure.getValueByShortcut(name);
                        if (v == null) {
                            throw new UnknownReferenceException(name);
                        }
                        return v.getValue();
                    }
                });
            } catch (ClassNotFoundException e) {
                throw new WebApplicationException(new Exception("calculator not found for " + specs.getKey()));
            }
            for (final Entry<Long, Double> dp : r.getDataPoints().entrySet()) {
                double newVal = calc.evaluate(new Closure() {
                    @Override
                    public long getTimeStamp() {
                        return dp.getKey();
                    }

                    @Override
                    public Value getValueByShortcut(String name) {
                        if (!r.metric.equals(name)) {
                            return null;
                        }
                        Value val = new Value();
                        val.add(dp.getValue());
                        return val;
                    }
                });
                log.debug("metric {}, tags {}, timestamp {}, original {} new val {}", r.metric, r.tags, dp.getKey(), dp.getValue(), newVal);
                r.getDataPoints().put(dp.getKey(), newVal);
            }
        }
    }

    private long parseTimeWithErrorHandling(String timeString, String timeTypeDescription, List<Object> errors) {
        long result = -1;
        try {
            result = Utils.parseDate(timeString);
        } catch (ParseException e) {
            log.error("Failed to parse {} time option of '{}': {} : {}", timeTypeDescription, timeString, e.getClass().getName(), e.getMessage());
            String errorString = String.format("Unable to parse specified %s time value of '%s'", timeTypeDescription, timeString);
            errors.add(Utils.makeError(errorString, e.getMessage(), timeTypeDescription));
        }
        return result;
    }

    private void filterExactReturnSet(long startTimestamp, long endTimestamp, OpenTSDBQueryResult series) {
        log.debug("Applying exact filter. start {}; end {}", startTimestamp, endTimestamp);
        long currentPointTimeStamp;
        SortedMap<Long, Double> filteredDataPoints = new TreeMap<>();
        Iterator<Entry<Long, Double>> iter = series.getDataPoints().entrySet().iterator();
        while(iter.hasNext()){
            Entry<Long, Double> datapoint = iter.next();
            currentPointTimeStamp = datapoint.getKey();
            if (currentPointTimeStamp < startTimestamp || currentPointTimeStamp > endTimestamp) {
                log.debug("filtering datapoint {}: start{}, end{}", datapoint, startTimestamp, endTimestamp);
                iter.remove();
            }
        }
    }

    private void filterLastReturnSet(long startTimestamp, long endTimestamp, OpenTSDBQueryResult series) {
        if (series.getDataPoints().isEmpty()) {
            log.debug("Applying last filter skipped, no values.");
            return;
        }
        log.debug("Applying last filter.");
        long currentPointTimeStamp;
        SortedMap<Long, Double> dataPointSingleton = new TreeMap<>();
        Map.Entry<Long, Double> lastDataPoint = null;
        for (Map.Entry<Long, Double> dataPoint : series.getDataPoints().entrySet()) {
            currentPointTimeStamp = dataPoint.getKey();
            if (currentPointTimeStamp < startTimestamp || currentPointTimeStamp > endTimestamp) {
                continue;
            }
            if (null == lastDataPoint || currentPointTimeStamp > lastDataPoint.getKey()) {
                lastDataPoint = dataPoint;
            }
        }
        if (null != lastDataPoint) {
            dataPointSingleton.put(lastDataPoint.getKey(), lastDataPoint.getValue());
        }
        series.setDataPoints(dataPointSingleton);
    }

}
