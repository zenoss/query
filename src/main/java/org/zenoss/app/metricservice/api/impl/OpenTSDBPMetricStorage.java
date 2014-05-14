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

import com.google.common.io.ByteStreams;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.RateOptions;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 */
@API
@Configuration
@Profile({"default", "prod"})
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

        OpenTSDBQuery query = new OpenTSDBQuery();

        if (!Utils.NOW.equals(startTime)) {
            query.start = startTime;
        }
        if (!Utils.NOW.equals(endTime)) {
            query.end = endTime;
        }

        for (MetricSpecification metricSpecification : queries) {
            query.addSubQuery(openTSDBSubQueryFromMetricSpecification(metricSpecification));
        }

        String jsonQueryString = Utils.jsonStringFromObject(query);
        if (log.isDebugEnabled()) {
            log.debug("OpenTSDB POST JSON: {}", jsonQueryString);
        }
        String postUrl = String.format("%s/api/query", config.getMetricServiceConfig().getOpenTsdbUrl());
        log.info("POSTing JSON to URL: {}", postUrl);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(postUrl);
        StringEntity input = new StringEntity(jsonQueryString);
        input.setContentType("application/json");
        postRequest.setEntity(input);
        HttpResponse response = httpClient.execute(postRequest);
        log.info("Response code: {}", response.getStatusLine().getStatusCode());

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : "
                    + response.getStatusLine().getStatusCode());
        }

        return new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
    }

    private OpenTSDBSubQuery openTSDBSubQueryFromMetricSpecification(MetricSpecification metricSpecification) {
        OpenTSDBSubQuery result = null;
        if (null != metricSpecification) {
            result = new OpenTSDBSubQuery();
            result.aggregator = metricSpecification.getAggregator();
            result.downsample = metricSpecification.getDownsample();
            result.metric = metricSpecification.getMetric();
            result.rate = metricSpecification.getRate();
            result.rateOptions = openTSDBRateOptionFromRateOptions(metricSpecification.getRateOptions());
        }
        return result;  //To change body of created methods use File | Settings | File Templates.
    }

    private OpenTSDBRateOption openTSDBRateOptionFromRateOptions(RateOptions rateOptions) {
        OpenTSDBRateOption result = null;
        if (null != rateOptions) {
            result = new OpenTSDBRateOption();
            result.counter = rateOptions.getCounter();
            result.counterMax = rateOptions.getCounterMax();
            result.resetValue = rateOptions.getResetThreshold();
        }
        return result;
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
