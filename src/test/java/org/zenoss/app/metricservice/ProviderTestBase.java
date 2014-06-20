/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
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
package org.zenoss.app.metricservice;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.yammer.dropwizard.testing.ResourceTest;
import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.metric.remote.MetricResources;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.PerformanceQuery;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import javax.ws.rs.core.MediaType;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 *
 */
public abstract class ProviderTestBase extends ResourceTest {
    private static final Logger log = LoggerFactory.getLogger(ProviderTestBase.class);

    @Autowired
    MetricResources resource;
    public static final String PERFORMANCE_QUERY_URL = "/api/performance/query";

    /*
     * (non-Javadoc)
     * 
     * @see com.yammer.dropwizard.testing.ResourceTest#setUpResources()
     */
    @Override
    protected void setUpResources() throws Exception {
        addResource(resource);
    }

    /**
     * If the value is present then add the given name/value pair as a query
     * parameter to the given buffer and return the '&' as the prefix, else just
     * return the given prefix.
     * 
     * @param buf
     *            the buffer to which to append
     * @param name
     *            the name of the query parameter
     * @param value
     *            the optional value of the query parameter
     * @param prefix
     *            the current prefix to use
     * @return the prefix that should be used for the next parameter
     */
    protected char addArgument(StringBuilder buf, String name,
            Optional<?> value, char prefix) {
        if (value.isPresent()) {
            try {
                buf.append(prefix)
                        .append(name)
                        .append('=')
                        .append(URLEncoder.encode(value.get().toString(),
                                "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // If encoding fails, then just try the value raw
                buf.append(prefix).append(name).append('=')
                        .append(value.get().toString());
            }
            return '&';
        }
        return prefix;
    }

    protected Map<?, ?> parseAndVerifyResponse(Optional<String> id,
            Optional<String> start, Optional<String> end,
            Optional<ReturnSet> returnset, Optional<Boolean> series,
            String[] queries, ClientResponse response) throws Exception {
        Object o;
        try (InputStreamReader reader = new InputStreamReader(
                response.getEntityInputStream())) {
            String jsonString = CharStreams.toString((reader));
            //String jsonString = reader.toString();
            log.debug("About to parse objects from JSON String: {}", jsonString);
            o = JSON.parse(jsonString);
            //o = JSON.parse(reader);
            Assert.assertNotNull("Unable to parse response as JSON Object", o);
            Assert.assertTrue("Parsed JSON object should be instance of Map.",Map.class.isInstance(o));
        }
        Map<?, ?> json = (Map<?, ?>) o;
        Assert.assertNotNull("Unable to cast response as map", o);


        // Verify the basic values
//        if (id.isPresent()) {
//            Assert.assertEquals(id.get(), json.get("clientId"));
//        }
        if (start.isPresent()) {
            Assert.assertEquals("StartTime value from object should match json value",start.get(), json.get("startTime"));
        }
        if (end.isPresent()) {
            Assert.assertEquals("EndTime value from object should match json value", end.get(), json.get("endTime"));
        }
        if (returnset.isPresent()) {
            Assert.assertEquals("ReturnSet from object should match json value.", returnset.get(), ReturnSet.fromJson((String) json.get("returnset")));
        }
        if (series.isPresent()) {
            Assert.assertEquals("Series get from object should match json value.", series.get(), json.get("series"));
        }
        Assert.assertNotNull("json value for results should not be null.",json.get("results"));

        // Verify the structure of the results
        Object[] results = (Object[]) json.get("results");
        log.debug("results = {}", Utils.jsonStringFromObject(results));

        if (results.length > 0) {
            // If we are using the mock generator then we can calculate
            // the number of entries in the results set
            int count = -1;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss-Z");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long s = (Long) json.get("startTimeActual");
            long e = (Long) json.get("endTimeActual");
            long dur = e - s;
            long step = 1;
            if (dur < 60) {
                step = 1;
            } else if (dur < 60 * 60) {
                step = 5;
            } else {
                step = 15;
            }

            count = (int) Math.floor(dur / step) + 1;

            if (true || (Boolean) json.get("series")) {

                // If we are a series then there will be at least 2 entries in
                // the result set; if our mock source then exactly two
                if ("mock".equals(json.get("source"))) {
                    Assert.assertEquals("length of queries and results should be same for mock source.", queries.length, results.length);
                } else {
                    Assert.assertTrue("length of results should be >= length of queries for non-mock source.", results.length >= queries.length);
                }

                for (Object r : results) {
                    Map<?, ?> value = (Map<?, ?>) r;
                    Assert.assertNotNull("no metric specification found", value.get("metric"));
                    Assert.assertNotNull("no data points array found", value.get("datapoints"));
                    Object[] dps = (Object[]) value.get("datapoints");
                    log.debug("count = {}; dps.length = {}", count, dps.length);
                    Assert.assertTrue("data points array is empty.", dps.length > 0);

                    switch (ReturnSet.fromJson((String) json.get("returnset"))) {
                    case EXACT:
                        Assert.assertEquals("EXACT - number of data points found should match count.", count, dps.length);
                        break;
                    case ALL:
                        Assert.assertTrue("ALL - number of data points found should be >= count", dps.length >= count);
                        break;
                    case LAST:
                        Assert.assertEquals("LAST - number of data points found should be 1", 1, dps.length);
                        break;
                    default:
                        break;
                    }
                    for (Object dpo : dps) {
                        Map<?, ?> dp = (Map<?, ?>) dpo;
                        Assert.assertNotNull("timestamp not found", dp.get("timestamp"));
                        Assert.assertNotNull("value not found", dp.get("value"));
                    }
                }
            } else {
                switch (ReturnSet.fromJson((String) json.get("returnset"))) {
                case EXACT:
                    // count is queries.length because the mock generates
                    // a data point for each step in the time span for each
                    // query
                    Assert.assertEquals("EXACT - number of data points found", count * queries.length, results.length);
                    break;
                case LAST:
                    // If the request was for the "last" value then there should
                    // be exactly one data point per metric requested
                    Assert.assertEquals("LAST - should have exactly 1 data point per metric.", queries.length, results.length);
                    break;
                case ALL:
                default:
                    break;
                }

                for (Object r : results) {
                    Map<?, ?> value = (Map<?, ?>) r;
                    Assert.assertNotNull("Metric should not be null.", value.get("metric"));
                    Assert.assertNotNull("Timestamp should not be null.", value.get("timestamp"));
                    Assert.assertNotNull("Value should not be null.", value.get("value"));
                }
            }
        }

        return json;
    }

    protected Map<?, ?> testPostQuery(Optional<String> id,
            Optional<String> start, Optional<String> end,
            Optional<ReturnSet> returnset, Optional<Boolean> series,
            String[] queries) throws Exception {
        // Build up a query object to post
        PerformanceQuery performanceQuery = new PerformanceQuery();
        if (start.isPresent()) {
            performanceQuery.setStart(start.get());
        }
        if (end.isPresent()) {
            performanceQuery.setEnd(end.get());
        }
        if (returnset.isPresent()) {
            performanceQuery.setReturnset(returnset.get());
        }
        if (series.isPresent()) {
            performanceQuery.setSeries(series.get());
        }
        List<MetricSpecification> list = new ArrayList<>();
        for (String s : queries) {
            list.add(MetricSpecification.fromString(s));
        }
        performanceQuery.setMetrics(list);
        Client client = client();
        client.setConnectTimeout(1000000);
        client.setReadTimeout(1000000);
        WebResource wr = client.resource("/api/performance/query");
        Assert.assertNotNull("WebResource for /api/performance/query was null.", wr);
        wr.accept(MediaType.APPLICATION_JSON);
        Builder request = wr.type(MediaType.APPLICATION_JSON);
        request.accept(MediaType.APPLICATION_JSON);
        ClientResponse response = request.post(ClientResponse.class, performanceQuery);
        Assert.assertNotNull("POST response was null.", response);
        Assert.assertEquals("Invalid response code", 200, response.getStatus());

        return parseAndVerifyResponse(Optional.<String> absent(), start, end,
                returnset, series, queries, response);
    }

    /**
     * Constructs a URI to test the performance query service given the various
     * optional parameters, invokes the URI and does some basic structural
     * checks on the results.
     * 
     * @param id
     *            the id of the request
     * @param start
     *            the start date/time of the request
     * @param end
     *            the end date/time of the request
     * @param returnset
     *            should the time window be honored
     * @param series
     *            should the results be separated into series
     * @param queries
     *            the list of queries
     * @return the response object parsed into an Map
     * @throws Exception
     *             if the process encounters an exception
     */
    protected Map<?, ?> testQuery(Optional<String> id, Optional<String> start,
            Optional<String> end, Optional<ReturnSet> returnset,
            Optional<Boolean> series, Optional<String> downsample,
            Optional<Map<String, List<String>>> globalTags, String[] queries)
            throws Exception {

        // Build up the URI query
        char prefix = '?';
        PerformanceQuery request = makeRequestObject(id, start, end, returnset, series, queries);

        // Invoke the URI and make sure we get a response
        Client client = client();
        client.setConnectTimeout(1000000);
        client.setReadTimeout(1000000);
        WebResource wr = client.resource(PERFORMANCE_QUERY_URL);
        Assert.assertNotNull(String.format("Null WebResource for %s", PERFORMANCE_QUERY_URL), wr);
        ClientResponse response = wr.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, request);
        Assert.assertNotNull("Null response from WebRequest.", response);
        Assert.assertEquals("Invalid response code", 200, response.getStatus());
        return parseAndVerifyResponse(id, start, end, returnset, series,
                queries, response);
    }

    protected PerformanceQuery makeRequestObject(Optional<String> id, Optional<String> start,
                                                 Optional<String> end, Optional<ReturnSet> returnset,
                                                 Optional<Boolean> series, String[] queries)
    {
        PerformanceQuery result = new PerformanceQuery();
        if (start.isPresent()) {
            result.setStart(start.orNull());
        }
        if (end.isPresent()) {
            result.setEnd(end.orNull());
        }
        if (returnset.isPresent()) {
            result.setReturnset(returnset.orNull());
        }
        if (series.isPresent()) {
            result.setSeries(series.or(false));
        }
        for (String query : queries) {
            result.getMetrics().add(MetricSpecification.fromString(query));
        }
        return result;
    }

    @Test
    public void queryTest10sAgo1QuerySeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestEpochInSeconds10sAgo1QuerySeries() throws Exception {
        String start = String
                .valueOf((new Date().getTime() / 1000) - 10L);
        testQuery(Optional.of("my-client-id"), Optional.of(start),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestEpochInMs10sAgo1QuerySeries() throws Exception {
        String start = String
                .valueOf(new Date().getTime() - (10 * 1000));
        testQuery(Optional.of("my-client-id"), Optional.of(start),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    @Ignore("Series=false no longer supported.")
    public void queryTest10sAgo1QueryNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(false), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTest10sAgo2QuerySeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1", "sum:laLoadInt5" });
    }

    @Test
    @Ignore("Series=false no longer supported.")
    public void queryTest10sAgo2QueryNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(false), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1", "sum:laLoadInt5" });
    }

    @Test

    public void queryTest10sAgo1QuerySeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1{tag1=*,tag2=*}" });
    }

    @Test
    @Ignore("Series=false no longer supported.")
    public void queryTest10sAgo1QueryNoSeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(false), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1{tag1=*,tag2=*}" });
    }

    @Test
    public void queryTest10sAgo2QuerySeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1{tag1=*,tag2=*}",
                        "sum:laLoadInt5{tag1=*,tag2=*}" });
    }

    @Test
    @Ignore("Series=false no longer supported.")
    public void queryTest10sAgo2QueryNoSeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.EXACT),
                Optional.of(false), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1{tag1=*,tag2=*}",
                        "sum:laLoadInt5{tag1=*,tag2=*}" });
    }

    @Test
    public void queryTestTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"),
                Optional.of(ReturnSet.ALL), Optional.of(true),
                Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    @Ignore("Series=false no longer supported.")
    public void queryTestTimeRangeNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"),
                Optional.of(ReturnSet.EXACT), Optional.of(false),
                Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestOutsideExactTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"),
                Optional.of(ReturnSet.EXACT), Optional.of(true),
                Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestOutsideTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"),
                Optional.of(ReturnSet.ALL), Optional.of(true),
                Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestLastValue() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("1h-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.LAST),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestLastValueWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("1h-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.LAST),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1{tag1=*,tag2=*}",
                        "sum:laLoadInt5{tag1=*,tag2=*}" });
    }

    @Test
    public void queryTestLastValueSeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("1h-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.LAST),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestLastValueSeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("1h-ago"),
                Optional.<String> absent(), Optional.of(ReturnSet.LAST),
                Optional.of(true), Optional.<String> absent(),
                Optional.<Map<String, List<String>>> absent(), new String[] {
                        "avg:laLoadInt1{tag1=*,tag2=*}",
                        "sum:laLoadInt5{tag1=*,tag2=*}" });
    }
}
