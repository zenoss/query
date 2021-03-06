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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.BucketTestUtilities;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.testutil.ConstantSeriesGenerator;
import org.zenoss.app.metricservice.testutil.SeriesGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class JacksonResultsWriterTest {
    private static final String TESTID = "TEST_ID";
    private static final String TEST_SOURCE_ID = "test_source_id";
    private static final long DATA_START_TIMESTAMP = 1078033800;
    private static final long DATA_END_TIMESTAMP = 1078034400;
    private static final long DATA_TIMESTAMP_STEP = 600;

    @Test
    public void testMakeResults() throws IOException {
        JacksonResultsWriter victim = new JacksonResultsWriter();
        String[] queryStrings = new String[] {
                "avg:laLoadInt1{tag1=*,tag2=*}",
                "sum:laLoadInt5{tag1=*,tag2=*}" };

        List<MetricSpecification> queries = makeTestQueries(queryStrings);
        long startTs = DATA_START_TIMESTAMP;
        long endTs = DATA_END_TIMESTAMP;
        long step = DATA_TIMESTAMP_STEP;
        Buckets<IHasShortcut> buckets = makeTestBuckets(queries, new ConstantSeriesGenerator(10.0), startTs, endTs, step);
        BucketTestUtilities.dumpBucketsToStdout(buckets);
        String id = TESTID;
        String sourceId = TEST_SOURCE_ID;
        String startTimeConfig = Long.toString(startTs);
        String endTimeConfig = Long.toString(endTs);
        ReturnSet returnset = ReturnSet.ALL;

        Collection<QueryResult> expectedResults = new ArrayList<>();

        String qr1str = "{\"metric\":\"laLoadInt1\",\"tags\":{\"tag1\":[\"*\"],\"tag2\":[\"*\"]},\"datapoints\":[{\"timestamp\":1078033800, \"value\":10.0},{\"timestamp\":1078034400, \"value\":10.0}]}";
        String qr2str = "{\"metric\":\"laLoadInt5\",\"tags\":{\"tag1\":[\"*\"],\"tag2\":[\"*\"]},\"datapoints\":[{\"timestamp\":1078033800, \"value\":10.0},{\"timestamp\":1078034400, \"value\":10.0}]}";
        ObjectMapper mapper = new ObjectMapper();
        QueryResult qr1 = mapper.readValue(qr1str, QueryResult.class);
        QueryResult qr2 = mapper.readValue(qr2str, QueryResult.class);
        expectedResults.add(qr1);
        expectedResults.add(qr2);

        final SeriesQueryResult seriesQueryResult = victim.makeResults(queries, buckets, id, sourceId, startTs, startTimeConfig, endTs, endTimeConfig, returnset);

        Assert.assertNotNull(seriesQueryResult);
        Assert.assertEquals("client ID mismatch in query result", id, seriesQueryResult.getClientId());
        Assert.assertEquals("endTime mismatch in query result", endTimeConfig, seriesQueryResult.getEndTime());
        Assert.assertEquals("endTimeActual mismatch in query result", endTs, seriesQueryResult.getEndTimeActual());
        Assert.assertEquals("returnset mismatch in query result", returnset, seriesQueryResult.getReturnset());
        Assert.assertEquals("series mismatch in query result", true, seriesQueryResult.isSeries());
        Assert.assertEquals("source mismatch in query result", sourceId, seriesQueryResult.getSource());
        Assert.assertEquals("startTime mismatch in query result", startTimeConfig, seriesQueryResult.getStartTime());
        Assert.assertEquals("startTimeActual mismatch in query result", startTs, seriesQueryResult.getStartTimeActual());
        Collection<QueryResult> ar = seriesQueryResult.getResults();
        Assert.assertEquals("result sizes mismatch in query results", expectedResults.size(), ar.size());
        Assert.assertEquals("results mismatch in query results", expectedResults, ar);
    }

    private Buckets<IHasShortcut> makeTestBuckets(List<MetricSpecification> queries, SeriesGenerator generator, long startTimestamp, long endTimestamp, long step) {
        Buckets<IHasShortcut> result = new Buckets<>();
        for (MetricSpecification query : queries) {
            MetricKey key = MetricKey.fromValue(query);
            Map<Long, Double> dataPoints = generator.generateValues(startTimestamp, endTimestamp, step);
            int i = 0;
            for (Map.Entry<Long, Double> entry : dataPoints.entrySet()) {
                i++;
                result.add(key, entry.getKey(), entry.getValue());
            }
            say(String.format("Added %d values for metric %s to result.", i, key.getMetric()));
        }
        say("makeTestBuckets: generated buckets:");
        say(Utils.jsonStringFromObject(result));
        return result;
    }

    private List<MetricSpecification> makeTestQueries(String[] queries) {
        List<MetricSpecification> result = new ArrayList<>();
        for (String query : queries) {
            result.add(MetricSpecification.fromString(query));
        }
        return result;
    }

    private void say(String message) {
        System.out.println(message);
    }

}
