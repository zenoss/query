package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MetricStorageAPI {
    public BufferedReader getReader(MetricServiceAppConfiguration config,
            String id, String startTime, String endTime,
            ReturnSet returnset, Boolean series, String downsample, double downsampleMultiplier,
            Map<String, List<String>> tags, List<MetricSpecification> queries)
            throws IOException;

    public String getSourceId();

}
