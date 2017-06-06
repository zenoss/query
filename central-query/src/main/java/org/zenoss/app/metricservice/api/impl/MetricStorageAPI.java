package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.api.model.v2.MetricRequest;
import org.zenoss.app.metricservice.api.model.v2.RenameRequest;

import java.io.Writer;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MetricStorageAPI {
    OpenTSDBQueryReturn query(MetricRequest query);

    List<OpenTSDBQueryResult> getResponse(MetricServiceAppConfiguration config,
                                          String id, String startTime, String endTime,
                                          ReturnSet returnset, String downsample, double downsampleMultiplier,
                                          Map<String, List<String>> tags, List<MetricSpecification> queries)
            throws IOException;

    String getSourceId();

    void rename(RenameRequest renameRequest, Writer writer);
}
