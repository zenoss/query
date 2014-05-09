package org.zenoss.app.metricservice.api.impl;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/8/14
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenTSDBDataPoint {
    public String metric;
    public long timestamp;
    public double value;
}
