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
package org.zenoss.app.metricservice;

import javax.validation.Valid;

import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.configs.ChartServiceConfig;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class MetricServiceAppConfiguration extends AppConfiguration {
    @Valid
    @JsonProperty("metrics")
    private MetricServiceConfig metricServiceConfig = new MetricServiceConfig();

    @Valid
    @JsonProperty("charts")
    private ChartServiceConfig chartServiceConfig = new ChartServiceConfig();

    public ChartServiceConfig getChartServiceConfig() {
        return chartServiceConfig;
    }

    public MetricServiceConfig getMetricServiceConfig() {
        return metricServiceConfig;
    }
}
