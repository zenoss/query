package org.zenoss.app.metricservice.api.impl;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/7/14
 * Time: 2:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class OpenTSDBRateOption {
    public boolean counter = true;
    public long counterMax = 65535;
    public long resetValue = 0;
}
