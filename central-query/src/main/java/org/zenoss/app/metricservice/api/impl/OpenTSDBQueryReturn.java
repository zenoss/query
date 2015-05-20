package org.zenoss.app.metricservice.api.impl;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class OpenTSDBQueryReturn {

    final List<OpenTSDBQueryResult> results;
    final private QueryStatus status;

    public OpenTSDBQueryReturn(OpenTSDBQueryResult[] results, QueryStatus status) {
        this.results = ImmutableList.copyOf(results);
        this.status = status;
    }

    public List<OpenTSDBQueryResult> getResults() {
        return results;
    }
    public QueryStatus getStatus() {
        return status;
    }

}
