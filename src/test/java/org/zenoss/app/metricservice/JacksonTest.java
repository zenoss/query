package org.zenoss.app.metricservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.Ignore;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQuery;
import org.zenoss.app.metricservice.api.impl.OpenTSDBSubQuery;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.PerformanceQuery;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/6/14
 * Time: 10:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class JacksonTest {
    static class TestClass {
        public String aString;
        public int anInt;
        public Map<String,List<String>> fancyMap;
    }

    @Test
    @Ignore
    public void testSerialization() throws JsonProcessingException {
        TestClass subject = makeTestClass();
        ObjectWriter ow = new ObjectMapper().writer();
        String json = ow.writeValueAsString(subject);
        say(String.format("JSON STRING:\"%s\"", json));
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
                                   /*
    public Response oldQuery(@QueryParam("id") Optional<String> id,
                          @QueryParam("oldQuery") List<MetricSpecification> queries,
                          @QueryParam("start") Optional<String> startTime,
                          @QueryParam("end") Optional<String> endTime,
                          @QueryParam("returnset") Optional<ReturnSet> returnset,
                          @QueryParam("series") Optional<Boolean> series) {*/
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
        ObjectWriter ow = new ObjectMapper().writer();
        String json = null;
        try {
            json = ow.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
        result.setName("test oldQuery");
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
