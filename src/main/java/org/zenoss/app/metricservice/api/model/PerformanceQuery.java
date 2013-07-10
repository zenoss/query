/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Zenoss or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.zenoss.app.metricservice.api.model;

import java.util.List;

import org.zenoss.app.metricservice.api.impl.Utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@JsonInclude(Include.NON_NULL)
public class PerformanceQuery {
    @JsonProperty
    private String start = Utils.DETAULT_START_TIME;
    
    @JsonProperty
    private String end = Utils.DEFAULT_END_TIME;
    
    @JsonProperty(required=true)
    private List<MetricSpecification> metrics = null;
    
    @JsonProperty(value="exact")
    private Boolean exactTimeWindow = null;
    
    @JsonProperty(value="series")
    private Boolean series = null;

    /**
     * @return the start
     */
    public final String getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public final void setStart(String start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public final String getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public final void setEnd(String end) {
        this.end = end;
    }

    /**
     * @return the exactTimeWindow
     */
    public final Boolean getExactTimeWindow() {
        return exactTimeWindow;
    }

    /**
     * @param exactTimeWindow the exactTimeWindow to set
     */
    public final void setExactTimeWindow(Boolean exactTimeWindow) {
        this.exactTimeWindow = exactTimeWindow;
    }

    /**
     * @return the series
     */
    public final Boolean getSeries() {
        return series;
    }

    /**
     * @param series the series to set
     */
    public final void setSeries(Boolean series) {
        this.series = series;
    }

    /**
     * @return the metrics
     */
    public final List<MetricSpecification> getMetrics() {
        return metrics;
    }

    /**
     * @param metrics the metrics to set
     */
    public final void setMetrics(List<MetricSpecification> metrics) {
        this.metrics = metrics;
    } 
}
