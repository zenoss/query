package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.api.model.Datapoint;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/23/14
 * Time: 9:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryResult {
    private List<QueryResultDataPoint> datapoints;
    private String metric;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public List<QueryResultDataPoint> getDatapoints() {
        return datapoints;
    }

    public void setDatapoints(List<QueryResultDataPoint> datapoints) {
        this.datapoints = datapoints;
    }
}
