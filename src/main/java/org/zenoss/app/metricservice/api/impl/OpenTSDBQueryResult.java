package org.zenoss.app.metricservice.api.impl;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/8/14
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class OpenTSDBQueryResult {
    public List<String> aggregateTags;
    public Map<Long,String> dps;
    public String metric;
    public Map<String, String> tags;
}
