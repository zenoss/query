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
package org.zenoss.app.query.api.query.configs;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class PerformanceMetricQueryConfig {
    @JsonProperty
    private Boolean defaultExactTimeWindow = Boolean.TRUE;

    @JsonProperty
    private String defaultStartTime = "1h-ago";

    @JsonProperty
    private String defaultEndTime = "now";

    @JsonProperty
    private Boolean defaultSeries = Boolean.FALSE;

    @JsonProperty
    private String openTsdbUrl = "http://tsdb.zenaas.com:4242";

    @JsonProperty
    private String defaultTsdTimeZone = "UTC";

    /**
     * @return the defaultExactTimeWindow
     */
    public final Boolean getDefaultExactTimeWindow() {
        return defaultExactTimeWindow;
    }

    /**
     * @return the defaultStartTime
     */
    public final String getDefaultStartTime() {
        return defaultStartTime;
    }

    /**
     * @return the defaultEndTime
     */
    public final String getDefaultEndTime() {
        return defaultEndTime;
    }

    /**
     * @return the defaultSeries
     */
    public final Boolean getDefaultSeries() {
        return defaultSeries;
    }

    /**
     * @return the openTsdbUrl
     */
    public final String getOpenTsdbUrl() {
        return openTsdbUrl;
    }

    /**
     * @return the defaultTsdTimeZone
     */
    public final String getDefaultTsdTimeZone() {
        return defaultTsdTimeZone;
    }

    /**
     * @param defaultExactTimeWindow
     *            the defaultExactTimeWindow to set
     */
    public final void setDefaultExactTimeWindow(Boolean defaultExactTimeWindow) {
        this.defaultExactTimeWindow = defaultExactTimeWindow;
    }

    /**
     * @param defaultStartTime
     *            the defaultStartTime to set
     */
    public final void setDefaultStartTime(String defaultStartTime) {
        this.defaultStartTime = defaultStartTime;
    }

    /**
     * @param defaultEndTime
     *            the defaultEndTime to set
     */
    public final void setDefaultEndTime(String defaultEndTime) {
        this.defaultEndTime = defaultEndTime;
    }

    /**
     * @param defaultSeries
     *            the defaultSeries to set
     */
    public final void setDefaultSeries(Boolean defaultSeries) {
        this.defaultSeries = defaultSeries;
    }

    /**
     * @param openTsdbUrl
     *            the openTsdbUrl to set
     */
    public final void setOpenTsdbUrl(String openTsdbUrl) {
        this.openTsdbUrl = openTsdbUrl;
    }

    /**
     * @param defaultTsdTimeZone
     *            the defaultTsdTimeZone to set
     */
    public final void setDefaultTsdTimeZone(String defaultTsdTimeZone) {
        this.defaultTsdTimeZone = defaultTsdTimeZone;
    }
}
