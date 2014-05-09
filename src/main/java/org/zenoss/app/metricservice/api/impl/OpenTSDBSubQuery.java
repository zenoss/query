package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.api.model.Aggregator;

import java.util.HashMap;
import java.util.Map;

public class OpenTSDBSubQuery {
    public Aggregator aggregator = Aggregator.sum;
    public String metric = null;
    public boolean rate = false;
    public OpenTSDBRateOption rateOptions = null;
    public String downsample = null;
    public Map<String, String> tags = null;

    public void addTag(String key, String value) {
        if (null == tags) {
            tags = new HashMap<String, String>();
        }
        tags.put(key, value);
    }
}

