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
package org.zenoss.app.metricservice.api.impl.mocks;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.impl.MetricStorageAPI;
import org.zenoss.app.metricservice.api.impl.OpenTSDBPMetricStorage;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
@Profile("dev")
public class MockMetricStorage implements MetricStorageAPI {

    private static final Logger log = LoggerFactory
            .getLogger(OpenTSDBPMetricStorage.class);

    private static final String SOURCE_ID = "mock";
    private static final String MOCK_VALUE = "mock-value";
    private static final char EQ = '=';
    private static final char LF = '\n';

    @Autowired
    MetricServiceAppConfiguration config;


    public byte[] generateData(MetricServiceAppConfiguration config, String id,
            String startTime, String endTime, ReturnSet returnset,
            Boolean series, String downsample, Map<String, List<String>> tags,
            List<MetricSpecification> queries) throws IOException {
        log.debug("Generate data for '{}' to '{}' requested", startTime,
                endTime);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        long start = 0;
        try {
            start = Utils.parseDate(startTime);
        } catch (ParseException e) {
            log.debug(
                    "Unable to parse start time specification of '{}' : {}:{}",
                    startTime, e.getClass().getName(), e.getMessage());
            return null;
        }
        long end = 0;
        try {
            end = Utils.parseDate(endTime);
        } catch (ParseException e) {
            log.debug("Unable to parse end time specification of '{}' : {}:{}",
                    endTime, e.getClass().getName(), e.getMessage());
            return null;
        }

        long preStart = start - 60; // 60 seconds
        long postEnd = end + 60; // 60 seconds
        long dur = end - start;

        log.debug("Generating mock data for '{}' to '{}', a duraction of '{}'",
                start, end, dur);
        long step = 0;
        if (dur < 60) {
            step = 1;
        } else if (dur < 60 * 60) {
            step = 5;
        } else {
            step = 15;
        }
        log.debug("Mock data geneated at an interval of '{}' seconds", step);

        // Return an array of results from 0.0 to 1.0 equally distributed
        // over the time range with 1 second steps.
        double inc = 1.0 / (double) (dur / step);

        int count = 0;
        StringBuilder buf = new StringBuilder();
        for (MetricSpecification query : queries) {
            double val = 0.0;
            for (long i = preStart; i <= postEnd; i += step) {
                buf.setLength(0);
                buf.append(query.getMetric()).append(' ');
                buf.append(i).append(' ');

                if (i >= start && i <= end) {
                    buf.append(val);
                    val += inc;
                } else {
                    buf.append(2.0);
                }

                // Need to join the global tags with the per metric tags,
                // overriding any global tag with that specified per metric
                if (tags != null || query.getTags() != null) {
                    Map<String, List<String>> joined = new HashMap<>();
                    if (tags != null) {
                        joined.putAll(tags);
                    }
                    if (query.getTags() != null) {
                        joined.putAll(query.getTags());
                    }
                    for (Map.Entry<String, List<String>> tag : joined.entrySet()) {
                        buf.append(' ').append(tag.getKey()).append(EQ)
                                .append(MOCK_VALUE);
                    }
                }
                buf.append(LF);
                baos.write(buf.toString().getBytes());
                count++;
            }
        }

        log.debug("Generated {} lines of data", count);
        return baos.toByteArray();
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
            Boolean series, String downsample, Map<String, List<String>> tags,
            List<MetricSpecification> queries) throws IOException {
        byte[] data = generateData(config, id, startTime, endTime, returnset,
                series, downsample, tags, queries);
        return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(data)));
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
