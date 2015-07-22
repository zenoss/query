/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2015, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */
package org.zenoss.app.metricservice.v2.impl;

import org.zenoss.app.metricservice.api.impl.QueryStatus;
import org.zenoss.app.metricservice.api.model.v2.QueryResult;
import org.zenoss.app.metricservice.api.model.v2.QueryResult.Series;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public final class QueryResultBuilder {


    private List<Series> seriesList = new ArrayList<>();
    private List<QueryStatus> statuses = new ArrayList<>();

    public QueryResultBuilder addSeries(String metricName, SortedMap<Long, Double> datapoints, Map<String, String> tags) {

        if (datapoints == null) {
            throw new IllegalArgumentException("datapoints cannot be null");
        }
        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }

        if (metricName == null || metricName.isEmpty()) {
            throw new IllegalArgumentException("metric name cannot be null or empty");
        }

        Series series = new Series();
        series.setMetric(metricName);
        series.setTags(tags);
        series.setDatapoints(datapoints);

        seriesList.add(series);
        return this;
    }

    public QueryResult build() {
        if (statuses.isEmpty()) {
            throw new IllegalArgumentException("statuses cannot be empty");
        }

        QueryResult qr = new QueryResult();
        qr.setSeries(seriesList);
        qr.setStatuses(statuses);
        return qr;
    }

    public void setStatus(QueryStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("statuses cannot be null");
        }
        this.statuses.add(status);
    }
}
