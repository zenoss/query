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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.yammer.dropwizard.testing.ResourceTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.metricservice.api.model.Chart;
import org.zenoss.app.metricservice.api.model.ChartList;
import org.zenoss.app.metricservice.api.model.Datapoint;
import org.zenoss.app.metricservice.api.model.Range;
import org.zenoss.app.metricservice.api.remote.ChartResources;

import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public abstract class ChartServiceTestBase extends ResourceTest {

    @Autowired
    ChartResources resource;

    /*
     * (non-Javadoc)
     * 
     * @see com.yammer.dropwizard.testing.ResourceTest#setUpResources()
     */
    @Override
    protected void setUpResources() throws Exception {
        addResource(resource);
    }

    private List<Chart> createCharts(int numTags, int numDatapoints,
            int numCharts) {
        // Add 100 charts so we can list them in groups of 10
        Range range = new Range();
        range.setStart("1h-ago");
        range.setEnd("now");

        Map<String, String> tags = new HashMap<String, String>();
        for (int i = 0; i < numTags; ++i) {
            tags.put("Name=" + i, "Value=" + i);
        }

        List<Datapoint> datapoints = new ArrayList<Datapoint>();
        for (int i = 0; i < numDatapoints; ++i) {
            Datapoint dp = new Datapoint();
            dp.setAggregator("avg");
            dp.setMetric("laLoadInt" + i);
            datapoints.add(dp);
        }

        List<Chart> charts = new ArrayList<Chart>();
        for (int i = 0; i < numCharts; ++i) {
            Chart chart = new Chart();
            chart.setName("chart-" + i);
            chart.setRange(range);
            chart.setTags(tags);
            chart.setDatapoints(datapoints);
            charts.add(chart);
        }

        return charts;
    }

    private List<String> addCharts(List<Chart> charts) {
        List<String> uuids = new ArrayList<String>();

        WebResource wr = client().resource("/api/performance/chart");
        Assert.assertNotNull(wr);
        wr.accept(MediaType.APPLICATION_JSON);

        for (Chart chart : charts) {
            Builder request = wr.type(MediaType.APPLICATION_JSON);
            request.accept(MediaType.APPLICATION_JSON);
            ClientResponse response = request.post(ClientResponse.class, chart);
            Assert.assertNotNull(response);

            Assert.assertEquals("Invalid response code creating test charts",
                    201, response.getStatus());
            Assert.assertNotNull("Location not set creating test charts",
                    response.getLocation().getPath());
            uuids.add(response.getLocation().getPath());
        }

        return uuids;
    }

    private void deleteCharts(List<String> uuids) {
        for (String uuid : uuids) {
            WebResource wr = client().resource(uuid);
            Assert.assertNotNull(wr);
            ClientResponse response = wr.delete(ClientResponse.class);
            Assert.assertNotNull(response);

            Assert.assertEquals("Invalid response code deleting test charts",
                    204, response.getStatus());
        }
    }

    @Test
    public void testAddCharts() {
        List<String> ids = null;
        try {
            WebResource wr = client().resource(
                    "/api/performance/chart?start=-1&end=0&includeCount=true");
            ClientResponse response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            ChartList list = response.getEntity(ChartList.class);
            long before = list.getCount();

            List<Chart> charts = createCharts(5, 5, 100);
            ids = addCharts(charts);

            wr = client().resource("/api/performance/chart?start=-1&end=0&includeCount=true");
            response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            list = response.getEntity(ChartList.class);
            long after = list.getCount();

            Assert.assertEquals("not able to create expected number of charts",
                    100, after - before);

            for (String id : ids) {
                wr = client().resource(id);
                response = wr.get(ClientResponse.class);
                Assert.assertEquals("Unable to find new resource " + id, 200,
                        response.getStatus());
            }
        } finally {
            if (ids != null) {
                deleteCharts(ids);
            }
        }
    }

    @Test
    public void testUpdateCharts() {
        List<String> ids = null;
        try {
            // Add and verify charts
            WebResource wr = client().resource(
                    "/api/performance/chart?start=-1&end=0&includeCount=true");
            ClientResponse response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            ChartList list = response.getEntity(ChartList.class);
            long before = list.getCount();

            List<Chart> charts = createCharts(5, 5, 100);
            ids = addCharts(charts);

            wr = client().resource("/api/performance/chart?start=-1&end=0&includeCount=true");
            response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            list = response.getEntity(ChartList.class);
            long after = list.getCount();

            Assert.assertEquals("not able to create expected number of charts",
                    100, after - before);

            for (String id : ids) {
                wr = client().resource(id);
                response = wr.get(ClientResponse.class);
                Assert.assertEquals("Unable to find new resource " + id, 200,
                        response.getStatus());
            }

            Chart update = createCharts(1, 1, 1).get(0);
            for (String id : ids) {
                wr = client().resource(id);
                Builder request = wr.type(MediaType.APPLICATION_JSON);
                update.setName(id.substring("/chart/".length()));
                response = request.put(ClientResponse.class, update);
                Assert.assertEquals("Unable to update chart", 204,
                        response.getStatus());
            }

            // Make sure the count hasn't changed (assumes we are sole writer)
            wr = client().resource("/api/performance/chart?start=-1&end=0&includeCount=true");
            response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            list = response.getEntity(ChartList.class);
            long afterUpdate = list.getCount();
            Assert.assertEquals(after, afterUpdate);

            for (String id : ids) {
                wr = client().resource(id);
                response = wr.get(ClientResponse.class);
                Assert.assertEquals("Unable to find new resource " + id, 200,
                        response.getStatus());
                Chart chart = response.getEntity(Chart.class);
                Assert.assertEquals("name was not updated",
                        id.substring("/chart/".length()), chart.getName());
                Assert.assertEquals("tags were not updated", 1, chart
                        .getTags().size());
                Assert.assertEquals("data points were not updated", 1, chart
                        .getDatapoints().size());
            }

        } finally {
            if (ids != null) {
                deleteCharts(ids);
            }
        }
    }

    @Test
    public void testDeleteCharts() {
        List<String> ids = null;
        try {
            // Add and verify charts
            WebResource wr = client().resource(
                    "/api/performance/chart?start=-1&end=0&includeCount=true");
            ClientResponse response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            ChartList list = response.getEntity(ChartList.class);
            long before = list.getCount();

            List<Chart> charts = createCharts(5, 5, 100);
            ids = addCharts(charts);

            wr = client().resource("/api/performance/chart?start=-1&end=0&includeCount=true");
            response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            list = response.getEntity(ChartList.class);
            long after = list.getCount();

            Assert.assertEquals("not able to create expected number of charts",
                    100, after - before);

            for (String id : ids) {
                wr = client().resource(id);
                response = wr.get(ClientResponse.class);
                Assert.assertEquals("Unable to find new resource " + id, 200,
                        response.getStatus());
            }

            // Delete the charts
            deleteCharts(ids);

            // Make sure the count is back to normal
            wr = client().resource("/api/performance/chart?start=-1&end=0&includeCount=true");
            response = wr.get(ClientResponse.class);
            Assert.assertEquals("Unable to get count", 200,
                    response.getStatus());
            list = response.getEntity(ChartList.class);
            long afterDelete = list.getCount();
            Assert.assertEquals("not all charts deleted", before, afterDelete);

            for (String id : ids) {
                wr = client().resource(id);
                response = wr.get(ClientResponse.class);
                Assert.assertEquals("Resource found, but should be not found: "
                        + id, 404, response.getStatus());
            }
            ids = null;
        } finally {
            if (ids != null) {
                deleteCharts(ids);
            }
        }
    }

    @Test
    public void testListCharts() {
        List<String> ids = null;
        try {
            List<Chart> charts = createCharts(5, 5, 100);
            ids = addCharts(charts);

            // There should be at least 100 charts currently in persistent
            // layer,
            // so walk through the first 100 in groups of ten.
            int start = 0;
            int end = 9;
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < 10; ++i) {
                buf.setLength(0);
                buf.append("/api/performance/chart?start=").append(start).append("&end=")
                        .append(end);
                WebResource wr = client().resource(buf.toString());
                Assert.assertNotNull(wr);
                ClientResponse response = wr.get(ClientResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals("Unexpected response code", 200,
                        response.getStatus());
                ChartList list = response.getEntity(ChartList.class);
                Assert.assertEquals("Wrong number of UUIDs returned", 10, list
                        .getIds().size());

                try {
                    new ObjectMapper().writeValueAsString(list);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                start = end + 1;
                end = start + 9;
            }
        } finally {
            if (ids != null) {
                deleteCharts(ids);
            }
        }
    }

    @Test
    public void testByName() {
        List<String> ids = null;

        try {
            List<Chart> charts = createCharts(5, 5, 100);
            ids = addCharts(charts);

            // Fetch 10 random charts by name
            Random r = new Random();
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < 10; ++i) {
                int idx = r.nextInt(charts.size());
                buf.setLength(0);
                buf.append("/api/performance/chart/name/").append(charts.get(idx).getName());
                WebResource wr = client().resource(buf.toString());
                Assert.assertNotNull(wr);
                ClientResponse response = wr.get(ClientResponse.class);
                Assert.assertNotNull(response);
                Assert.assertEquals("Unexpected response code", 200,
                        response.getStatus());
                Chart chart = response.getEntity(Chart.class);
                Assert.assertNotNull(chart);
                Assert.assertEquals(charts.get(idx).getName(), chart.getName());
            }
        } finally {
            if (ids != null) {
                deleteCharts(ids);
            }
        }
    }

}
