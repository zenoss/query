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
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
public class MetricService implements MetricServiceAPI {
    @Autowired
    MetricServiceAppConfiguration config;

    @Autowired
    MetricStorageAPI api;

    protected static final String CLIENT_ID = "clientId";
    protected static final String SOURCE = "source";
    protected static final String START_TIME = "startTime";
    protected static final String START_TIME_ACTUAL = "startTimeActual";
    protected static final String END_TIME = "endTime";
    protected static final String END_TIME_ACTUAL = "endTimeActual";
    protected static final String RESULTS = "results";
    protected static final String DATAPOINTS = "datapoints";
    protected static final String AGGREGATOR = "aggregator";
    protected static final String RATE = "rate";
    protected static final String DOWNSAMPLE = "downsample";
    protected static final String METRIC = "metric";
    protected static final String EXACT_TIME_WINDOW = "exactTimeWindow";
    protected static final String TIMESTAMP = "timestamp";
    protected static final String SERIES = "series";
    protected static final String VALUE = "value";
    protected static final String TAGS = "tags";

    protected static final String NOT_SPECIFIED = "not-specified";

    private static final Logger log = LoggerFactory
            .getLogger(MetricService.class);

    public MetricService() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.enable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    protected final ObjectMapper objectMapper;

    protected TimeZone serverTimeZone = null;

    
    private class Worker implements StreamingOutput {
        private final String id;
        private final String startTime;
        private final String endTime;
        private final Boolean exactTimeWindow;
        private final Boolean series;
        private final List<MetricSpecification> queries;
        private long start = -1;
        private long end = -1;

        public Worker(MetricServiceAppConfiguration config, String id,
                String startTime, String endTime, Boolean exactTimeWindow,
                Boolean series, List<MetricSpecification> queries) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.exactTimeWindow = exactTimeWindow;
            this.series = series;
            this.queries = queries;
        }

        private void writeAsSeries(JsonWriter writer, BufferedReader reader)
                throws NumberFormatException, IOException {
            long t = -1;
            String line = null;
            long ts = 0;
            double val = 0;
            boolean comma = false;
            
            // Because TSDB gives data that is outside the exact time range
            // requested it is not always known at any point if the further 
            // data is "valid" if we are triming to the exact time range. As
            // such we have to delay the comma before a new "series" until we
            // know we will actually have one.
            boolean precomma = false;
            
            boolean needHeader = true;

            while ((line = reader.readLine()) != null) {
                String terms[] = line.split(" ", 4);

                // Check the timestamp and if we went backwards in time that
                // means that we are onto the next query.
                ts = Long.valueOf(terms[1]);
                if (ts < t) {
                    // If we have written a header then we need to close
                    // out the JSON object
                    if (!needHeader) {
                        writer.arrayE().objectE(false);
                    }
                    needHeader = true;
                    comma = false;
                    precomma = true;
                }
                t = ts;
                if (!exactTimeWindow || (ts >= start && ts <= end)) {
                    if (needHeader) {
                        if (precomma) {
                            writer.write(',');
                            precomma = false;
                        }
                        writer.objectS().value(METRIC, terms[0], true);

                        // every entry in this series should have the same
                        // tags, so output them once, by just using the tags
                        // from the first entry
                        if (terms.length > 3) {
                            // The result has tags
                            writer.objectS(TAGS);
                            int eq = -1;
                            for (String tag : terms[3].split(" ")) {
                                // Bit of a hack here. We are using the fact
                                // that only on the first trip through this loop
                                // eq == -1 as an indicator that on before every
                                // value except the first we need to add a ','
                                if (eq != -1) {
                                    writer.write(',');
                                }
                                eq = tag.indexOf('=');
                                writer.value(tag.substring(0, eq),
                                        tag.substring(eq + 1));
                            }
                            writer.objectE(true);
                        }

                        writer.arrayS(DATAPOINTS);
                        needHeader = false;
                    }
                    if (comma) {
                        writer.write(',');
                    }
                    comma = true;
                    val = Double.valueOf(terms[2]);
                    writer.objectS().value(TIMESTAMP, ts, true)
                            .value(VALUE, val, false).objectE();
                }
            }
            if (!needHeader) {
                // end the last query, if we opened it
                writer.arrayE().objectE();
            }
        }

        private void writeAsIs(JsonWriter writer, BufferedReader reader)
                throws IOException {
            String line = null;
            long ts = -1;
            double val = 0;
            boolean comma = false;
            while ((line = reader.readLine()) != null) {
                String terms[] = line.split(" ", 4);

                // Check the timestamp and if we went backwards in time that
                // means that we are onto the next query.
                ts = Long.valueOf(terms[1]);
                if (!exactTimeWindow || (ts >= start && ts <= end)) {
                    if (comma) {
                        writer.write(',');
                    }
                    comma = true;
                    val = Double.valueOf(terms[2]);
                    writer.objectS().value(METRIC, terms[0], true)
                            .value(TIMESTAMP, ts, true)
                            .value(VALUE, val, terms.length > 3);

                    if (terms.length > 3) {
                        // The result has tags
                        writer.objectS(TAGS);
                        int eq = -1;
                        for (String tag : terms[3].split(" ")) {
                            // Bit of a hack here. We are using the fact that
                            // only on the first trip through this loop eq == -1
                            // as an indicator that on before every value except
                            // the first we need to add a ','
                            if (eq != -1) {
                                writer.write(',');
                            }
                            eq = tag.indexOf('=');
                            writer.value(tag.substring(0, eq),
                                    tag.substring(eq + 1));
                        }
                        writer.objectE();
                    }
                    writer.objectE();
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
         */
        @Override
        public void write(OutputStream output) throws IOException,
                WebApplicationException {

            // Validate the input parameters
            List<Object> errors = new ArrayList<Object>();

            // Validate start time
            try {
                start = Utils.parseDate(startTime);
            } catch (ParseException e) {
                log.error("Failed to parse start time option of '{}': {} : {}",
                        startTime, e.getClass().getName(), e.getMessage());
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(Utils.ERROR_MESSAGE, String.format(
                        "Unable to parse specified start time value of '%s'",
                        startTime));
                error.put(Utils.ERROR_CAUSE, e.getMessage());
                error.put(Utils.ERROR_PART, Utils.START);
                errors.add(error);
            }

            // Validate end time
            try {
                end = Utils.parseDate(endTime);
            } catch (ParseException e) {
                log.error("Failed to parse end time option of '{}': {} : {}",
                        endTime, e.getClass().getName(), e.getMessage());
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(Utils.ERROR_MESSAGE, String.format(
                        "Unable to parse specified end time value of '%s'",
                        startTime));
                error.put(Utils.ERROR_CAUSE, e.getMessage());
                error.put(Utils.ERROR_PART, Utils.END);
                errors.add(error);
            }

            // Validate that there is at least one (1) metric specification
            if (queries.size() == 0) {
                log.error("No queries specified for request");
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(Utils.ERROR_MESSAGE,
                        "At least one (1) metric query term must be specified, none found");
                error.put(Utils.ERROR_CAUSE, METRIC);
                error.put(Utils.ERROR_PART, METRIC);
                errors.add(error);
            }

            if (errors.size() > 0) {
                Map<String, Object> response = new HashMap<String, Object>();
                response.put(CLIENT_ID, id);
                response.put(Utils.ERRORS, errors);
                throw new WebApplicationException(Response
                        .status(400)
                        .entity(objectMapper.writer().writeValueAsString(
                                response)).build());
                // throw new WebApplicationException(Response.status(400)
                // .entity(new JSONObject(response).toString()).build());
            }

            if (serverTimeZone == null) {
                try {
                    serverTimeZone = api.getServerTimeZone();
                } catch (Throwable t) {
                    log.error(
                            "Unable to determine timezone of the performance metric server: {} : {}",
                            t.getClass().getName(), t.getMessage());
                    throw new WebApplicationException(
                            Utils.getErrorResponse(
                                    id,
                                    500,
                                    String.format("Unable to determine timezone of the performance metric server"),
                                    t.getMessage()));
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
            sdf.setTimeZone(serverTimeZone);
            SimpleDateFormat actual = new SimpleDateFormat(
                    "yyyy/MM/dd-HH:mm:ss-Z");
            actual.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date startDate = new Date(start * 1000);
            Date endDate = new Date(end * 1000);

            String convertedStartTime = sdf.format(startDate);
            String convertedEndTime = sdf.format(endDate);

            BufferedReader reader = null;
            try {
                reader = api.getReader(config, id, convertedStartTime,
                        convertedEndTime, exactTimeWindow, series, queries);
            } catch (WebApplicationException wae) {
                throw wae;
            } catch (Throwable t) {
                log.error("Failed to connect to metric data source: {} : {}", t
                        .getClass().getName(), t.getMessage());
                throw new WebApplicationException(
                        Utils.getErrorResponse(
                                id,
                                500,
                                String.format("Unable to connect to performance metric data source"),
                                t.getMessage()));

            }

            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                    output))) {

                writer.objectS()
                        .value(CLIENT_ID, id, true)
                        .value(SOURCE, api.getSourceId(), true)
                        .value(START_TIME, startTime, true)
                        .value(START_TIME_ACTUAL, actual.format(startDate),
                                true).value(END_TIME, endTime, true)
                        .value(END_TIME_ACTUAL, actual.format(endDate), true)
                        .value(EXACT_TIME_WINDOW, exactTimeWindow, true)
                        .value(SERIES, series, true).arrayS(RESULTS);

                // convert the start / end times to longs so we can determine if
                // the returned results are outside the bounds of the requested
                // area. this only needs to be done if the query was for the
                // exact time window.
                if (series) {
                    writeAsSeries(writer, reader);
                } else {
                    writeAsIs(writer, reader);
                }

                writer.arrayE().objectE(); // end the whole thing
                writer.flush();
            } catch (Throwable t) {
                log.error(
                        "Server error while processing metric source {} : {}:{}",
                        api.getSourceId(), t.getClass().getName(), t.getMessage());
                throw new WebApplicationException(Response.status(500).build());
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.query.api.PerformanceMetricQueryAPI#query(com.google.common
     * .base.Optional, com.google.common.base.Optional,
     * com.google.common.base.Optional, com.google.common.base.Optional,
     * com.google.common.base.Optional, java.util.List)
     */
    @Override
    public Response query(Optional<String> id, Optional<String> startTime,
            Optional<String> endTime, Optional<Boolean> exactTimeWindow,
            Optional<Boolean> series, List<MetricSpecification> queries) {

        try {
            return Response.ok(
                    new Worker(config, id.or(NOT_SPECIFIED), startTime
                            .or(config.getMetricServiceConfig()
                                    .getDefaultStartTime()), endTime.or(config
                            .getMetricServiceConfig()
                            .getDefaultEndTime()), exactTimeWindow.or(config
                            .getMetricServiceConfig()
                            .getDefaultExactTimeWindow()), series.or(config
                            .getMetricServiceConfig()
                            .getDefaultSeries()), queries)).build();
        } catch (Throwable t) {
            return Response.status(500).build();
        }
    }
}
