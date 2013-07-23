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

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.PerformanceQuery;
import org.zenoss.app.metricservice.api.remote.MetricResources;

import com.google.common.base.Optional;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.yammer.dropwizard.testing.ResourceTest;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public abstract class ProviderTestBase extends ResourceTest {

    @Autowired
    MetricResources resource;

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
            Optional<Boolean> exact, Optional<Boolean> series,
            String[] queries, ClientResponse response) throws Exception {
        Object o;
        try (InputStreamReader reader = new InputStreamReader(
                response.getEntityInputStream())) {
            o = JSON.parse(reader);
            Assert.assertNotNull("Unable to parse response as JSON Object", o);
            Assert.assertTrue(Map.class.isInstance(o));
        }
        Map<?, ?> json = (Map<?, ?>) o;

        // Verify the basic values
        if (id.isPresent()) {
            Assert.assertEquals(id.get(), json.get("clientId"));
        }
        if (start.isPresent()) {
            Assert.assertEquals(start.get(), json.get("startTime"));
        }
        if (end.isPresent()) {
            Assert.assertEquals(end.get(), json.get("endTime"));
        }
        if (exact.isPresent()) {
            Assert.assertEquals(exact.get(), json.get("exactTimeWindow"));
        }
        if (series.isPresent()) {
            Assert.assertEquals(series.get(), json.get("series"));
        }
        Assert.assertNotNull(json.get("results"));

        // Verify the structure of the results
        Object[] results = (Object[]) json.get("results");

        if (results.length > 0) {
            // If we are using the mock generator then we can calculate
            // the number of entries in the results set
            int count = -1;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss-Z");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            long s = sdf.parse((String) json.get("startTimeActual")).getTime() / 1000;
            long e = sdf.parse((String) json.get("endTimeActual")).getTime() / 1000;
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

            if ((Boolean) json.get("series")) {

                // If we are a series then there will be at least 2 entries in
                // the result set; if our mock source then exactly two
                if ("mock".equals(json.get("source"))) {
                    Assert.assertEquals(queries.length, results.length);
                } else {
                    Assert.assertTrue(results.length >= queries.length);
                }

                for (Object r : results) {
                    Map<?, ?> value = (Map<?, ?>) r;
                    Assert.assertNotNull("no metric specification found",
                            value.get("metric"));
                    Assert.assertNotNull("no data points array found",
                            value.get("datapoints"));
                    Object[] dps = (Object[]) value.get("datapoints");
                    Assert.assertTrue(dps.length > 0);
                    // if ("mock".equals(json.get("source"))) {
                    if ((Boolean) json.get("exactTimeWindow")) {
                        Assert.assertEquals("number of data points found",
                                count, dps.length);
                    }
                    for (Object dpo : dps) {
                        Map<?, ?> dp = (Map<?, ?>) dpo;
                        Assert.assertNotNull("timestampt not found",
                                dp.get("timestamp"));
                        Assert.assertNotNull("value not found", dp.get("value"));
                    }
                }
            } else {

                if ((Boolean) json.get("exactTimeWindow")) {
                    // count is * queries.length because the mock generates
                    // a data point for each step in the time span for each
                    // query
                    Assert.assertEquals(count * queries.length, results.length);
                }
                for (Object r : results) {
                    Map<?, ?> value = (Map<?, ?>) r;
                    Assert.assertNotNull(value.get("metric"));
                    Assert.assertNotNull(value.get("timestamp"));
                    Assert.assertNotNull(value.get("value"));
                }
            }
        }

        return (Map<?, ?>) json;
    }

    protected Map<?, ?> testPostQuery(Optional<String> id,
            Optional<String> start, Optional<String> end,
            Optional<Boolean> exact, Optional<Boolean> series, String[] queries)
            throws Exception {
        // Build up a query object to post
        PerformanceQuery pq = new PerformanceQuery();
        if (start.isPresent()) {
            pq.setStart(start.get());
        }
        if (end.isPresent()) {
            pq.setEnd(end.get());
        }
        if (exact.isPresent()) {
            pq.setExactTimeWindow(exact.get());
        }
        if (series.isPresent()) {
            pq.setSeries(series.get());
        }
        List<MetricSpecification> list = new ArrayList<MetricSpecification>();
        for (String s : queries) {
            list.add(MetricSpecification.fromString(s));
        }
        pq.setMetrics(list);

        WebResource wr = client().resource("/query/performance");
        Assert.assertNotNull(wr);
        wr.accept(MediaType.APPLICATION_JSON);
        Builder request = wr.type(MediaType.APPLICATION_JSON);
        request.accept(MediaType.APPLICATION_JSON);
        ClientResponse response = request.post(ClientResponse.class, pq);
        Assert.assertNotNull(response);
        Assert.assertEquals("Invalid response code", 200, response.getStatus());

        return parseAndVerifyResponse(Optional.<String> absent(), start, end,
                exact, series, queries, response);
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
     * @param exact
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
            Optional<String> end, Optional<Boolean> exact,
            Optional<Boolean> series, Optional<Map<String, String>> globalTags,
            String[] queries) throws Exception {

        // Build up the URI query
        char prefix = '?';
        StringBuilder buf = new StringBuilder("/query/performance");
        prefix = addArgument(buf, "id", id, prefix);
        prefix = addArgument(buf, "start", start, prefix);
        prefix = addArgument(buf, "end", end, prefix);
        prefix = addArgument(buf, "exact", exact, prefix);
        prefix = addArgument(buf, "series", series, prefix);
        for (String query : queries) {
            prefix = addArgument(buf, "query", Optional.of(query), prefix);
        }

        // Invoke the URI and make sure we get a response
        WebResource wr = client().resource(buf.toString());
        Assert.assertNotNull(wr);
        ClientResponse response = wr.get(ClientResponse.class);
        Assert.assertNotNull(response);
        Assert.assertEquals("Invalid response code", 200, response.getStatus());
        return parseAndVerifyResponse(id, start, end, exact, series, queries,
                response);
    }

    @Test
    public void queryTest10sAgo1QuerySeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTest10sAgo1QueryNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTest10sAgo2QuerySeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1", "sum:laLoadInt5" });
    }

    @Test
    public void queryTest10sAgo2QueryNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1", "sum:laLoadInt5" });
    }

    @Test
    public void queryTest10sAgo1QuerySeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1{btag1=value1,tag2=value2}" });
    }

    @Test
    public void queryTest10sAgo1QueryNoSeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1{tag1=value1,tag2=value2}" });
    }

    @Test
    public void queryTest10sAgo2QuerySeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1{tag1=value1,tag2=value2}",
                        "sum:laLoadInt5{tag1=value1,tag2=value2}" });
    }

    @Test
    public void queryTest10sAgo2QueryNoSeriesWithTags() throws Exception {
        testQuery(Optional.of("my-client-id"), Optional.of("10s-ago"),
                Optional.<String> absent(), Optional.of(true),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1{tag1=value1,tag2=value2}",
                        "sum:laLoadInt5{tag1=value1,tag2=value2}" });
    }

    @Test
    public void queryTestTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"), Optional.of(false),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestTimeRangeNoSeries() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"), Optional.of(true),
                Optional.of(true), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestOutsideExactTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"), Optional.of(true),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }

    @Test
    public void queryTestOutsideTimeRange() throws Exception {
        testQuery(Optional.of("my-client-id"),
                Optional.of("2013/04/30-16:00:00-GMT"),
                Optional.of("2013/04/30-18:00:00-GMT"), Optional.of(false),
                Optional.of(false), Optional.<Map<String, String>> absent(),
                new String[] { "avg:laLoadInt1" });
    }
}
