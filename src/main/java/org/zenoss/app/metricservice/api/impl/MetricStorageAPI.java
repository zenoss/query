package org.zenoss.app.metricservice.api.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

public interface MetricStorageAPI {
    public BufferedReader getReader(MetricServiceAppConfiguration config,
            String id, String startTime, String endTime,
            ReturnSet returnset, Boolean series, String downsample,
            Map<String, List<String>> tags, List<MetricSpecification> queries)
            throws IOException;

    public String getSourceId();

    public TimeZone getServerTimeZone();
}
