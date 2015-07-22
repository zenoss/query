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
package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.zenoss.app.metricservice.api.impl.QueryStatus;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class QueryResult {


    private List<Series> series;
    private List<QueryStatus> statuses;

    public List<Series> getSeries() {
        return series;
    }

    public void setSeries(List<Series> series) {
        this.series = series;
    }

    public void setStatuses(List<QueryStatus> statuses) {
        this.statuses = statuses;
    }

    public List<QueryStatus> getStatuses() {
        return statuses;
    }

    public static class Series {
        @JsonSerialize(using = DatapointSerializer.class)
        public SortedMap<Long, Double> datapoints;
        private String metric;
        private Map<String, String> tags;

        public SortedMap<Long, Double> getDatapoints() {
            return datapoints;
        }

        public void setDatapoints(SortedMap<Long, Double> datapoints) {
            this.datapoints = datapoints;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }

}
