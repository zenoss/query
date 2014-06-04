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

package org.zenoss.app.metricservice;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Ignore;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import javax.ws.rs.core.MediaType;
import java.util.*;

public class JerseyClientTest {
    @Test
    @Ignore("not really a unit test")
    public void tryIt() {
        MetricSpecification request = makeMetricSpecification("df.bytes.percentused");
        Client c = Client.create();
        String url = "http://localhost:8888/api/performance/query";
        /*WebResource r = c.resource(url);
        r.type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(MetricSpecification.class, request);
        say(r.toString());
          */


        WebResource webResource = c.resource(url);
        ClientResponse response = webResource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, request);
        say(response.toString());

        //ListWrapper listWrapper = response.getEntity(ListWrapper.class);
    }

    private void say(String s) {
        System.out.println(s);
    }


    private List<MetricSpecification> makeMetrics() {
        List<MetricSpecification> result = new ArrayList<>();
        result.add(makeMetricSpecification("df.bytes.free"));
        return result;
    }

    private MetricSpecification makeMetricSpecification(String metricName) {
        MetricSpecification result = new MetricSpecification();
        result.setAggregator(Aggregator.avg);
        result.setMetric(metricName);
        result.setTags(makeTags("host", "morr-workstation"));
        return result;
    }

    private Map<String,List<String>> makeTags(String tagName, String... tagValues) {
        Map<String, List<String>> result = new HashMap<>();
        result.put(tagName, makeList(tagValues));
        return result;
    }

    private static List<String> makeList(String... strings) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, strings);
        return result;
    }
}
