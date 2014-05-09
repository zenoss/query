package org.zenoss.app.metricservice;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/6/14
 * Time: 3:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class JerseyClientTest {
    @Test
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
        List<MetricSpecification> result = new ArrayList<MetricSpecification>();
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
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        result.put(tagName, makeList(tagValues));
        return result;
    }

    private static List<String> makeList(String... strings) {
        List<String> result = new ArrayList<String>();
        for (String s: strings) {
            result.add(s);
        }
        return result;
    }
}
