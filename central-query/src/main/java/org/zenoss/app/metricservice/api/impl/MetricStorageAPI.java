package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricQuery;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MetricStorageAPI {
    Iterable<OpenTSDBQueryResult> query(MetricQuery query);

    public Iterable<OpenTSDBQueryResult> getResponse(MetricServiceAppConfiguration config,
                                                     String id, String startTime, String endTime,
                                                     ReturnSet returnset, String downsample, double downsampleMultiplier,
                                                     Map<String, List<String>> tags, List<MetricSpecification> queries, boolean allowWildCard)
            throws IOException;


    public String getSourceId();

}
