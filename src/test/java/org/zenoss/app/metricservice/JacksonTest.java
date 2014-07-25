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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQuery;
import org.zenoss.app.metricservice.api.impl.OpenTSDBSubQuery;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.PerformanceQuery;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class JacksonTest {
    static class TestClass {
        public String aString;
        public int anInt;
        public Map<String,List<String>> fancyMap;
    }

    @Test
    public void testPerformancePostQuery() {
        PerformanceQuery victim = new PerformanceQuery();
        victim.setStart("1h-ago");
        victim.setMetrics(makeMetrics());
        writeJsonString(victim);
    }

    @Test
    public void testPerformanceGetQuery() {
        MetricSpecification spec = new MetricSpecification();
        spec.setName("MySpec");
        spec.setTags(makeTags("host", "morr-workstation"));
        spec.setAggregator(Aggregator.avg);
        spec.setMetric("df.bytes.free");
        List<MetricSpecification> specList = new ArrayList<>();
        specList.add(spec);
        writeJsonString(specList);
    }

    @Test
    public void testOpenTSDBQuery() {
        OpenTSDBQuery query = makeTestQuery();
        String json = Utils.jsonStringFromObject(query);
        say(String.format("Json = %s", json));
        assertTrue("tags missing from json", json.contains("tags"));
        assertTrue("metric missing from json",json.contains("metric"));
    }

    private OpenTSDBQuery makeTestQuery() {
        OpenTSDBQuery result = new OpenTSDBQuery();
        result.start = "1h-ago";
        result.end = "now";
        result.addSubQuery(makeSubQuery("df.bytes.free"));
        return result;
    }

    private OpenTSDBSubQuery makeSubQuery(String metric) {
        OpenTSDBSubQuery result = new OpenTSDBSubQuery();
        result.metric = metric;
        result.addTag("host", "morr-workstation");
        return result;
    }

    private void writeJsonString(Object object) {
        ObjectWriter ow = Utils.getObjectMapper().writer();
        String json = null;
        try {
            json = ow.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        say(String.format("JSON STRING:\"%s\"", json));
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
        result.setName("test query");
        return result;
    }

    private Map<String,List<String>> makeTags(String tagName, String... tagValues) {
        Map<String, List<String>> result = new HashMap<>();
        result.put(tagName, makeList(tagValues));
        return result;
    }

    private static void say(String format) {
        System.out.println(format);
    }

    private static TestClass makeTestClass() {
         TestClass result = new TestClass();
        result.anInt = 42;
        result.aString = "This is a string.";
        result.fancyMap = makeFancyMap();
        return result;
    }

    private static Map<String, List<String>> makeFancyMap() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("Numbers", makeList("one", "two", "three", "four", "five"));
        result.put("Letters", makeList("alpha", "beta", "gamma", "delta"));
        return result;
    }

    private static List<String> makeList(String... strings) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, strings);
        return result;
    }

}
