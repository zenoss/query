package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.buckets.Value;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/23/14
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueryResultDataPoint {
    private long timestamp;
    private double value;

    public QueryResultDataPoint(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
