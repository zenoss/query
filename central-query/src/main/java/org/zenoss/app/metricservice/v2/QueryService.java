package org.zenoss.app.metricservice.v2;

import org.zenoss.app.metricservice.api.impl.OpenTSDBQueryResult;
import org.zenoss.app.metricservice.api.model.v2.MetricRequest;

public interface QueryService {

    Iterable<OpenTSDBQueryResult> query(MetricRequest query);

}
