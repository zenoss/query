package org.zenoss.app.metricservice.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.metric.impl.MetricService;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.buckets.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/19/14
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class JacksonResultsWriter {

    private static final Logger log = LoggerFactory.getLogger(JacksonResultsWriter.class);
    public void writeResults(JacksonWriter writer, List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset, boolean series) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
        if (series) {
            SeriesQueryResult results = makeResults(queries, buckets, id, sourceId, startTs, startTimeConfig, endTs, endTimeConfig, returnset);
            // write results (JSON serialization)
            writer.write(Utils.jsonStringFromObject(results));
        } else {
            //TODO: make a new exception type for this and throw it. Determine whether it needs to be a WebException or not.
            throw new Exception("non-series data is no longer supported.");
        }
    }

    private SeriesQueryResult makeResults(List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset) {
        SeriesQueryResult result = new SeriesQueryResult();
        result.setId(id);
        result.setClientId(MetricService.CLIENT_ID);
        result.setEndTime(endTimeConfig);
        result.setEndTimeActual(endTs);
        result.setReturnset(returnset);
        result.setSeries(true);
        result.setSource(sourceId);
        result.setStartTime(startTimeConfig);
        result.setStartTimeActual(startTs);
        result.addResults(makeDataPointResults(queries, buckets, startTs, endTs, returnset));
        return result;
    }


    private Collection<QueryResult> makeDataPointResults(Collection<MetricSpecification> queries, Buckets<MetricKey,
        String> buckets, long startTs, long endTs, ReturnSet returnset) {
        if (null == buckets) {
            return null;
        }
        Collection<QueryResult> results = new ArrayList<>();
        List<Long> timestamps = buckets.getTimestamps();
        for (MetricSpecification query : queries) {
            QueryResult qr = new QueryResult();
            qr.setMetric(query.getNameOrMetric());
            qr.setDatapoints(makeDataPoints(buckets, startTs, endTs, returnset, timestamps, query.getNameOrMetric()));
            results.add(qr);
        }
        return results;
    }

    private List<QueryResultDataPoint> makeDataPoints(Buckets<MetricKey, String> buckets, long startTs, long endTs,
                                                            ReturnSet returnset, List<Long> timestamps, String metricShortcut) {
        List<QueryResultDataPoint> dataPoints = new ArrayList<>();
        for (long bts : timestamps) {
            Buckets<MetricKey, String>.Bucket bucket;
            if ((returnset == ReturnSet.ALL || (bts >= startTs && bts <= endTs))
                && (bucket = buckets.getBucket(bts)) != null) {
                Value value = bucket.getValueByShortcut(metricShortcut);
                if (null != value) {
                    dataPoints.add(new QueryResultDataPoint(bts, value.getValue()));
                } else {
                    log.warn("No data point found for timestamp {}, metric {}", bts, metricShortcut);
                }
            }
        }
        return dataPoints;
    }
}
