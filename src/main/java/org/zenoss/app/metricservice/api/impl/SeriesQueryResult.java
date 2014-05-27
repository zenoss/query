package org.zenoss.app.metricservice.api.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/20/14
 * Time: 10:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class SeriesQueryResult {
    private String clientId;
    private String endTime;
    private long endTimeActual;
    private ReturnSet returnset;
    private boolean series;
    private String source;
    private String startTime;
    private long startTimeActual;
    private String id;
    @JsonProperty("results")
    private List<QueryResult> results;

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTimeActual(long endTimeActual) {
        this.endTimeActual = endTimeActual;
    }

    public long getEndTimeActual() {
        return endTimeActual;
    }

    public void setReturnset(ReturnSet returnset) {
        this.returnset = returnset;
    }

    public ReturnSet getReturnset() {
        return returnset;
    }

    public void setSeries(boolean series) {
        this.series = series;
    }

    public boolean isSeries() {
        return series;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTimeActual(long startTimeActual) {
        this.startTimeActual = startTimeActual;
    }

    public long getStartTimeActual() {
        return startTimeActual;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addResults(Collection<QueryResult> queryResults) {
        if (null == results) {
            results = new ArrayList<QueryResult>();
        }
        results.addAll(queryResults);
    }
}
