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
package org.zenoss.app.metricservice.health;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.dropwizardspring.annotations.HealthCheck;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@HealthCheck
public class OpenTsdbHealthCheck extends com.yammer.metrics.core.HealthCheck {
    @Autowired
    MetricServiceAppConfiguration config;

    protected OpenTsdbHealthCheck() {
        super("OpenTSDB");
    }

    @Override
    protected Result check() throws Exception {
        try {
            URL url = new URL(config.getMetricServiceConfig()
                    .getOpenTsdbUrl() + "/version?json");
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection
                    .setConnectTimeout(config.getMetricServiceConfig()
                            .getConnectionTimeoutMs());
            connection.setReadTimeout(config.getMetricServiceConfig()
                    .getConnectionTimeoutMs());
            if (Math.floor(connection.getResponseCode() / 100) != 2) {
                return Result
                        .unhealthy("Unexpected result code from OpenTSDB Server: "
                                + connection.getResponseCode());
            }

            // Exception if unable to parse object from input stream
            new ObjectMapper().reader(Map.class)
                    .readValue(connection.getInputStream()).toString();

            return Result.healthy();
        } catch (Throwable t) {
            return Result.unhealthy(t);
        }
    }
}
