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
package org.zenoss.app.query.api.query.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
@Profile({ "default", "prod" })
public class OpenTSDBPerformanceMetricQueryAPIImpl extends
        BasePerformanceMetricQueryAPIImpl {
    @Autowired
    QueryAppConfiguration config;

    private static final Logger log = LoggerFactory
            .getLogger(OpenTSDBPerformanceMetricQueryAPIImpl.class);

    private static final String SOURCE_ID = "OpenTSDB";

    /**
     * Attempt to determine the time zone of the server on which the TSD is
     * executing. The server time zone is determined by performing a "HTTP HEAD"
     * operation on the server and looking to see if the server returned a Date
     * header value. If not, then the default port is attempted (if not used
     * originally). If neither are found then it will default to the value set
     * in the configuration.
     * 
     * If a date header is found, then that is assumed it is in the format
     * "EEE, d MMM yyyy HH:mm:ss Z" and the last term (space separated) is the
     * time zone. This information is extracted, converted to a time zone {@see
     * TimeZone#getTimeZone(String)}, and returned.
     * 
     * @return the TSD server's time zone
     */
    protected TimeZone getServerTimeZone() {

        String sdate = null;
        HttpURLConnection connection = null;
        URL url = null;
        try {
            // Do a HEAD on the TSDB server and see if it reports the Date
            url = new URL(getConfiguration().getPerformanceMetricQueryConfig()
                    .getOpenTsdbUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection
                    .setConnectTimeout(config.getPerformanceMetricQueryConfig()
                            .getConnectionTimeoutMs());
            connection.setReadTimeout(config.getPerformanceMetricQueryConfig()
                    .getConnectionTimeoutMs());
            sdate = connection.getHeaderField("Date");
            connection.disconnect();
            connection = null;
            if (log.isDebugEnabled()) {
                if (sdate == null) {
                    log.debug(
                            "Unable to find 'Date' header value from HTTP HEAD @ {}",
                            url);
                } else {
                    log.debug(
                            "Found 'Date' header value from HTTP HEAD @ {} : {}",
                            url, sdate);
                }
            }
            if (sdate == null) {
                // HEAD did not return value, try standard port, if not already
                // tried.
                if (url.getPort() != url.getDefaultPort()) {
                    url = new URL(url.getProtocol(), url.getHost(),
                            url.getDefaultPort(), "");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(config
                            .getPerformanceMetricQueryConfig()
                            .getConnectionTimeoutMs());
                    connection.setReadTimeout(config
                            .getPerformanceMetricQueryConfig()
                            .getConnectionTimeoutMs());
                    sdate = connection.getHeaderField("Date");
                    connection.disconnect();
                    connection = null;
                    if (log.isDebugEnabled()) {
                        if (sdate == null) {
                            log.debug(
                                    "Unable to find 'Date' header value from HTTP HEAD using default port @ {}",
                                    url);
                        } else {
                            log.debug(
                                    "Found 'Date' header value from HTTP HEAD using default port @ {} : {}",
                                    url, sdate);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(
                    "Failed to connect to server via '{}' to retrieve server time zone information: {} : {}",
                    url, e.getClass().getName(), e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (sdate != null) {
            String[] terms = sdate.split(" ");
            TimeZone tz = TimeZone.getTimeZone(terms[terms.length - 1]);
            return tz;
        }

        // Date not found, grab the default from the
        // configuration
        if (log.isDebugEnabled()) {
            log.debug(
                    "Returning default time zone information from configuration: {}",
                    getConfiguration().getPerformanceMetricQueryConfig()
                            .getDefaultTsdTimeZone());
        }
        return TimeZone.getTimeZone(getConfiguration()
                .getPerformanceMetricQueryConfig().getDefaultTsdTimeZone());
    }

    private WebApplicationException generateException(
            HttpURLConnection connection) {
        int code = 500;
        try {
            code = connection.getResponseCode();
            InputStream is = connection.getErrorStream();

            // Read the entire buffer as is should be a very short HTML page.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);

            Pattern pattern = Pattern
                    .compile("The reason provided was:\\<blockquote\\>(.*)\\</blockquote>\\</blockquote\\>");
            Matcher matcher = pattern.matcher(baos.toString());
            if (matcher.find()) {
                String message = matcher.group(1);
                if (message != null) {
                    JSONObject error = new JSONObject();
                    error.put(ERROR_MESSAGE, message);
                    return new WebApplicationException(Response.status(code)
                            .entity(error.toString()).build());
                }
            } else {
                log.error("MESSAGE NOT FOUND");
            }

            return new WebApplicationException(Response.status(code).build());
        } catch (Throwable t) {
            t.printStackTrace();
            log.error(
                    "Unexpected error while attempting to parse response from OpenTSDB: {} : {}",
                    t.getClass().getName(), t.getMessage());
            try {
                JSONObject error = new JSONObject();
                error.put(ERROR_MESSAGE, t.getMessage());
                return new WebApplicationException(Response.status(code)
                        .entity(error.toString()).build());
            } catch (Throwable i) {
                return new WebApplicationException(code);
            }
        }
    }

    protected BufferedReader getReader(QueryAppConfiguration config, String id,
            String startTime, String endTime, Boolean exactTimeWindow,
            Boolean series, List<MetricQuery> queries) throws IOException {
        StringBuilder buf = new StringBuilder(config
                .getPerformanceMetricQueryConfig().getOpenTsdbUrl());
        buf.append("/q?");
        if (!NOW.equals(startTime)) {
            buf.append("start=").append(URLEncoder.encode(startTime, "UTF-8"));
        }
        if (!NOW.equals(endTime)) {
            buf.append("&end=").append(URLEncoder.encode(endTime, "UTF-8"));
        }
        for (MetricQuery query : queries) {
            buf.append("&m=").append(
                    URLEncoder.encode(query.toString(), "UTF-8"));
        }
        buf.append("&ascii");

        if (log.isDebugEnabled()) {
            log.debug("OpenTSDB GET: {}", buf.toString());
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(
                buf.toString()).openConnection();
        connection.setConnectTimeout(config.getPerformanceMetricQueryConfig()
                .getConnectionTimeoutMs());
        connection.setReadTimeout(config.getPerformanceMetricQueryConfig()
                .getConnectionTimeoutMs());

        if (Math.floor(connection.getResponseCode() / 100) != 2) {
            // OpenTSDB through an error, attempt to parse out the reason from
            // any response information that was send back.

            throw generateException(connection);
        }

        return new BufferedReader(new InputStreamReader(
                connection.getInputStream()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.query.api.query.impl.BasePerformanceMetricQueryAPIImpl
     * #getConfiguration()
     */
    @Override
    protected QueryAppConfiguration getConfiguration() {
        return config;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.query.api.query.impl.BasePerformanceMetricQueryAPIImpl
     * #getSourceId()
     */
    @Override
    protected String getSourceId() {
        return SOURCE_ID;
    }
}
