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
package org.zenoss.app.metricservice.api.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import com.google.common.io.ByteStreams;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 *
 */
@API
@Configuration
@Profile({ "default", "prod" })
public class OpenTSDBPMetricStorage implements MetricStorageAPI {
    @Autowired
    MetricServiceAppConfiguration config;

    private static final Logger log = LoggerFactory
            .getLogger(OpenTSDBPMetricStorage.class);

    private static final String SOURCE_ID = "OpenTSDB";

    private WebApplicationException generateException(
            HttpURLConnection connection) {
        int code = 500;
        try {
            code = connection.getResponseCode();
            InputStream is = connection.getErrorStream();

            // Read the entire buffer as is should be a very short HTML page.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] content = ByteStreams.toByteArray(is);

            Pattern pattern = Pattern
                    .compile("The reason provided was:\\<blockquote\\>(.*)\\</blockquote>\\</blockquote\\>");
            Matcher matcher = pattern.matcher(new String(content, "UTF-8"));
            if (matcher.find()) {
                String message = matcher.group(1);
                if (message != null) {
                    baos = new ByteArrayOutputStream();
                    JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                            baos));
                    writer.objectS();
                    writer.value(Utils.ERROR_MESSAGE, message);
                    writer.objectE();
                    writer.close();
                    return new WebApplicationException(Response.status(code)
                            .entity(baos.toString()).build());
                }
            } else {
                log.error("MESSAGE NOT FOUND");
            }

            return new WebApplicationException(Response.status(code).build());
        } catch (Exception e) {
            log.error(
                    "Unexpected error while attempting to parse response from OpenTSDB: {} : {}",
                    e.getClass().getName(), e.getMessage());
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JsonWriter writer = new JsonWriter(new OutputStreamWriter(baos));
                writer.objectS();
                writer.value(Utils.ERROR_MESSAGE, e.getMessage());
                writer.objectE();
                writer.close();
                return new WebApplicationException(Response.status(code)
                        .entity(baos.toString()).build());
            } catch (Exception ee) {
                return new WebApplicationException(code);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.zenoss.app.query.api.impl.MetricStorageAPI#getReader(org.zenoss.app
     * .query.QueryAppConfiguration, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.Boolean, java.lang.Boolean, java.util.List)
     */
    public BufferedReader getReader(MetricServiceAppConfiguration config,
            String id, String startTime, String endTime, ReturnSet returnset,
            Boolean series, String downsample,
            Map<String, List<String>> globalTags,
            List<MetricSpecification> queries) throws IOException {
        StringBuilder buf = new StringBuilder(config.getMetricServiceConfig()
                .getOpenTsdbUrl());
        buf.append("/q?");
        if (!Utils.NOW.equals(startTime)) {
            buf.append("start=").append(URLEncoder.encode(startTime, "UTF-8"));
        }
        if (!Utils.NOW.equals(endTime)) {
            buf.append("&end=").append(URLEncoder.encode(endTime, "UTF-8"));
        }
        for (MetricSpecification query : queries) {
            buf.append("&m=").append(
                    URLEncoder.encode(query.toString(downsample, globalTags,
                            this.config.getMetricServiceConfig()
                                    .getSendRateOptions()), "UTF-8"));
        }
        buf.append("&ascii&silent");

        if (log.isDebugEnabled()) {
            log.debug("OpenTSDB GET: {}", buf.toString());
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(
                buf.toString()).openConnection();
        connection.setConnectTimeout(config.getMetricServiceConfig()
                .getConnectionTimeoutMs());
        connection.setReadTimeout(config.getMetricServiceConfig()
                .getConnectionTimeoutMs());

        if (Math.floor(connection.getResponseCode() / 100) != 2) {
            // OpenTSDB through an error, attempt to parse out the reason from
            // any response information that was send back.

            throw generateException(connection);
        }

        /*
         * Check to make sure that we have the right content returned to us, in
         * that, if the content type is not 'text/plain' then something is
         * wrong.
         */
        if (!"text/plain".equals(connection.getContentType())) {
            /*
             * Log the response in order to help support.
             */
            if (log.isErrorEnabled()) {
                try (InputStream is = connection.getInputStream()) {
                    byte[] content = ByteStreams.toByteArray(is);

                    log.error(
                            "Invalid response content type of: '{}', full returned content '{}'",
                            connection.getContentType(), new String(content));

                } catch (Exception e) {
                    log.error(
                            "Invalid response content type of: '{}', exception attempting to read content '{}:{}'",
                            connection.getContentType(),
                            e.getClass().getName(), e.getMessage());
                }
            }
            throw new WebApplicationException(Utils.getErrorResponse(id, 500,
                    "Unknown severe request error", "unknown"));
        }

        return new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.zenoss.app.query.api.impl.MetricStorageAPI#getSourceId()
     */
    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }
}
