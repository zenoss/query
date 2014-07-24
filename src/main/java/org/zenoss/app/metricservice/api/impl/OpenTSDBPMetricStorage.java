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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


@API
@Configuration
@Profile({"default", "prod"})
public class OpenTSDBPMetricStorage implements MetricStorageAPI {
    @Autowired
    MetricServiceAppConfiguration config;

    private static final Logger log = LoggerFactory
        .getLogger(OpenTSDBPMetricStorage.class);

    private static final String SOURCE_ID = "OpenTSDB";

    /*
     * (non-Javadoc)
     *
     * @see
     * org.zenoss.app.query.api.impl.MetricStorageAPI#getReader(org.zenoss.app
     * .query.QueryAppConfiguration, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.Boolean, java.lang.Boolean, java.util.List)
     */
    @Override
    public BufferedReader getReader(MetricServiceAppConfiguration config,
                                    String id, String startTime, String endTime, ReturnSet returnset,
                                    Boolean series, String downsample, double downsampleMultiplier,
                                    Map<String, List<String>> globalTags,
                                    List<MetricSpecification> queries) throws IOException {

        OpenTSDBQuery query = new OpenTSDBQuery();

        // This could maybe be better - for now, it works : end time defaults to 'now', start time does not default.
        query.start = startTime;
        if (!Utils.NOW.equals(endTime)) {
            query.end = endTime;
        }

        String appliedDownsample = createModifiedDownsampleRequest(downsample, downsampleMultiplier);
        log.info("Specified Downsample = {}, Specified Multiplier = {}, Applied Downsample = {}.", downsample, downsampleMultiplier, appliedDownsample);

        for (MetricSpecification metricSpecification : queries) {
            String oldDownsample = metricSpecification.getDownsample();
            if (null != oldDownsample && !oldDownsample.isEmpty()) {
                log.info("Overriding specified series downsample ({}) with global specification of {}", oldDownsample, appliedDownsample);
            }
            metricSpecification.setDownsample(appliedDownsample);
            query.addSubQuery(openTSDBSubQueryFromMetricSpecification(metricSpecification));
        }

        String jsonQueryString = Utils.jsonStringFromObject(query);
        if (log.isInfoEnabled()) {
            log.info("OpenTSDB POST JSON: {}", jsonQueryString);
        }
        HttpResponse response = postRequestToOpenTsdb(config, jsonQueryString);

        log.info("Response code from OpenTSDB POST: {}", response.getStatusLine().getStatusCode());

        throwWebExceptionIfHttpResponseIsBad(response);

        return new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
    }

    private static void throwWebExceptionIfHttpResponseIsBad(HttpResponse response) throws WebApplicationException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (isNotOk(statusCode)) {
            String content = "";
            try {
                content = streamToString(response.getEntity().getContent());
            } catch (IOException e) {
                content = "Unable to read content from openTSDB.";
            }
            throw new WebApplicationException(
                Response.status(statusCode)
                    .entity("Operation failed: " + response.getStatusLine().toString() + "Response from OpenTSDB: " + content)
                    .build());
        }
    }

    private static String streamToString(InputStream stream) {
        Scanner s = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static boolean isNotOk(int statusCode) {
        return ((statusCode / 100) != 2);
    }

    private static HttpResponse postRequestToOpenTsdb(MetricServiceAppConfiguration config, String jsonQueryString) throws IOException {
        String postUrl = String.format("%s/api/query", config.getMetricServiceConfig().getOpenTsdbUrl());
        log.info("POSTing JSON to URL: {}", postUrl);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(postUrl);
        log.info("Query to OpenTSDB: {}",jsonQueryString);
        StringEntity input = new StringEntity(jsonQueryString);
        input.setContentType("application/json");
        postRequest.setEntity(input);
        return httpClient.execute(postRequest);
    }

    private static OpenTSDBSubQuery openTSDBSubQueryFromMetricSpecification(MetricSpecification metricSpecification) {
        OpenTSDBSubQuery result = null;
        if (null != metricSpecification) {
            result = new OpenTSDBSubQuery();
            result.aggregator = metricSpecification.getAggregator();
            result.downsample = metricSpecification.getDownsample();
            result.metric = metricSpecification.getMetric();
            result.rate = metricSpecification.getRate();
            result.rateOptions = openTSDBRateOptionFromRateOptions(metricSpecification.getRateOptions());
            Map<String, List<String>> tags = metricSpecification.getTags();
            if (null != tags) {
                for (Map.Entry<String, List<String>> tagEntry : tags.entrySet()) {
                    for (String tagValue : tagEntry.getValue()) {
                        result.addTag(tagEntry.getKey(), tagValue);
                    }
                }
            }
        }
        return result;
    }

    private static OpenTSDBRateOption openTSDBRateOptionFromRateOptions(RateOptions rateOptions) {
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



    public static String parseAggregation(String v) {
        String result = "";
        int dashPosition = v.indexOf('-');
        if (dashPosition > 0 && dashPosition < v.length()) {
            result = v.substring(dashPosition + 1);
        }
        return result;
    }

    public static String createModifiedDownsampleRequest(String downsample, double downsampleMultiplier) {
        if (null == downsample || downsample.isEmpty() || downsampleMultiplier <= 0.0) {
            log.warn("Bad downsample or multiplier. Returning original downsample value.");
            return downsample;
        }
        long duration = Utils.parseDuration(downsample);
        String aggregation = parseAggregation(downsample);
        long newDuration = (long)(duration / downsampleMultiplier);
        if (newDuration <= 0) {
            log.warn("Applying value {} of downsampleMultiplier to downsample value of {} would result in a request with resolution finer than 1 sec. returning 1 second.", downsampleMultiplier, downsample);
            newDuration = 1;
        }
        return String.format("%ds-%s", newDuration, aggregation);
    }

}
