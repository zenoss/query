/*
 * Copyright (c) 2014, Zenoss and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Zenoss or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.zenoss.app.metricservice.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.buckets.Value;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;


public class JacksonResultsWriter {

    private static final Logger log = LoggerFactory.getLogger(JacksonResultsWriter.class);

    public void writeResults(Writer writer, List<MetricSpecification> queries, Buckets<IHasShortcut> buckets,
                             String id, String sourceId, long startTs, String startTimeConfig, long endTs,
                             String endTimeConfig, ReturnSet returnset, boolean series) throws IOException {
        if (series) {
            SeriesQueryResult results = makeResults(queries, buckets, id, sourceId, startTs, startTimeConfig, endTs, endTimeConfig, returnset);
            // write results (JSON serialization)
            String resultJson = Utils.jsonStringFromObject(results);
            log.debug("Resulting JSON: {}", resultJson);
            writer.write(resultJson);
        } else {
            UnsupportedOperationException e = new UnsupportedOperationException("Series is no longer supported.");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
    }

    private SeriesQueryResult makeResults(List<MetricSpecification> queries, Buckets<IHasShortcut> buckets,
                                          String id, String sourceId, long startTs, String startTimeConfig, long endTs,
                                          String endTimeConfig, ReturnSet returnset) {
        SeriesQueryResult result = new SeriesQueryResult();
        result.setClientId(id);
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

    private Collection<QueryResult> makeDataPointResults(Collection<MetricSpecification> queries,
                                                         Buckets<IHasShortcut> buckets, long startTs, long endTs,
                                                         ReturnSet returnset) {
        Collection<QueryResult> results = new ArrayList<>();
        if (null == buckets) {
            log.info("buckets is null - returning.");
            return results;
        }
        for (MetricSpecification query : queries) {
            if (false == query.getEmit()) {
                log.info("emit is false for metric {} - skipping.", query.getNameOrMetric());
                continue;
            }
            QueryResult qr = getQueryResult(buckets, startTs, endTs, returnset, query);
            results.add(qr);
        }
        log.debug("Returning collection with {} QueryResults.", results.size());
        return results;
    }

    private QueryResult getQueryResult(Buckets<IHasShortcut> buckets, long startTs, long endTs, ReturnSet returnset, MetricSpecification query) {
        QueryResult qr = new QueryResult();
        qr.setMetric(query.getNameOrMetric());
        qr.setTags(query.getTags());
        qr.setDatapoints(makeDataPoints(buckets, startTs, endTs, returnset, query.getNameOrMetric()));
        qr.setId(query.getId());
        return qr;
    }

    private List<QueryResultDataPoint> makeDataPoints(Buckets<IHasShortcut> buckets, long startTs, long endTs,
                                                      ReturnSet returnset, String metricShortcut) {
        List<QueryResultDataPoint> dataPoints = new ArrayList<>();
        SortedSet<Long> timestamps = buckets.getTimestamps();
        for (long bts : timestamps) {
            if (returnset == ReturnSet.ALL || (bts >= startTs && bts <= endTs)) {
                Buckets<IHasShortcut>.Bucket bucket = buckets.getBucket(bts);
                if (null != bucket) {
                    Value value = bucket.getValueByShortcut(metricShortcut);
                    if (null != value) {
                        dataPoints.add(new QueryResultDataPoint(bts, value.getValue()));
                    } else {
                        log.warn("No data point found for timestamp {}, metric {}", bts, metricShortcut);
                    }
                } else {
                    log.warn("Unable to retrieve value for timestamp {}", bts);
                }
            }
        }
        log.debug("returning collection with {} QueryResultDataPoints.", dataPoints.size());
        return dataPoints;
    }
}
