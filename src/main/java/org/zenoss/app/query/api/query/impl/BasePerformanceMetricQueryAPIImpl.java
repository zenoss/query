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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;
import org.zenoss.app.query.api.PerformanceMetricQueryAPI;

import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public abstract class BasePerformanceMetricQueryAPIImpl implements
        PerformanceMetricQueryAPI {
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
    protected static final String NOW = "now";

    // Error tags
    protected static final String ERRORS = "errors";
    protected static final String ERROR_MESSAGE = "errorMessage";
    protected static final String ERROR_CAUSE = "errorSource";
    protected static final String ERROR_PART = "errorPart";
    protected static final String START = "start";
    protected static final String END = "end";

    private static final Logger log = LoggerFactory
            .getLogger(BasePerformanceMetricQueryAPIImpl.class);

    protected abstract BufferedReader getReader(QueryAppConfiguration config,
            String id, String startTime, String endTime,
            Boolean exactTimeWindow, Boolean series, List<MetricQuery> queries)
            throws IOException;

    protected abstract QueryAppConfiguration getConfiguration();

    protected abstract String getSourceId();

    protected abstract TimeZone getServerTimeZone();

    protected TimeZone serverTimeZone = null;

    protected long parseDuration(String v) {
        char last = v.charAt(v.length() - 1);

        int period = 0;
        try {
            period = Integer.parseInt(v.substring(0, v.length() - 1));
        } catch (NumberFormatException e) {
            return 0;
        }

        switch (last) {
        case 's':
            return period;
        case 'm':
            return period * 60;
        case 'h':
            return period * 60 * 60;
        case 'd':
            return period * 60 * 60 * 24;
        case 'w':
            return period * 60 * 60 * 24 * 7;
        case 'y':
            return period * 60 * 60 * 24 * 265;
        }

        return 0;
    }

    protected long parseDate(String value) throws ParseException {
        String v = value.trim();

        if (NOW.equals(v)) {
            return new Date().getTime() / 1000;
        } else if (v.endsWith("-ago")) {
            return new Date().getTime() / 1000
                    - parseDuration(v.substring(0, v.length() - 4));
        }
        try {
            return new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss-Z").parse(v)
                    .getTime() / 1000;
        } catch (ParseException e) {
            // If it failed to parse with a timezone then attempt to parse
            // w/o and use the default timezone
            return new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").parse(v)
                    .getTime() / 1000;
        }
    }

    private class Worker implements StreamingOutput {

        private final String id;
        private final String startTime;
        private final String endTime;
        private final Boolean exactTimeWindow;
        private final Boolean series;
        private final List<MetricQuery> queries;
        private long start = -1;
        private long end = -1;

        public Worker(QueryAppConfiguration config, String id,
                String startTime, String endTime, Boolean exactTimeWindow,
                Boolean series, List<MetricQuery> queries) {
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
                        writer.arrayE().objectE(true);
                    }
                    needHeader = true;
                    comma = false;
                }
                t = ts;
                if (!exactTimeWindow || (ts >= start && ts <= end)) {
                    if (needHeader) {
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

        private Response getErrorResponse(int status, String message,
                Object cause) {
            try {
                JSONObject response = new JSONObject();
                response.put(CLIENT_ID, id);
                response.put(ERROR_MESSAGE, message);
                response.put(ERROR_CAUSE, cause);
                return Response.status(status).entity(response.toString())
                        .build();
            } catch (Throwable t) {
                return Response.status(status).build();
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
                start = parseDate(startTime);
            } catch (ParseException e) {
                log.error("Failed to parse start time option of '{}': {} : {}",
                        startTime, e.getClass().getName(), e.getMessage());
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(ERROR_MESSAGE, String.format(
                        "Unable to parse specified start time value of '%s'",
                        startTime));
                error.put(ERROR_CAUSE, e.getMessage());
                error.put(ERROR_PART, START);
                errors.add(error);
            }

            // Validate end time
            try {
                end = parseDate(endTime);
            } catch (ParseException e) {
                log.error("Failed to parse end time option of '{}': {} : {}",
                        endTime, e.getClass().getName(), e.getMessage());
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(ERROR_MESSAGE, String.format(
                        "Unable to parse specified end time value of '%s'",
                        startTime));
                error.put(ERROR_CAUSE, e.getMessage());
                error.put(ERROR_PART, END);
                errors.add(error);
            }

            // Validate that there is at least one (1) metric specification
            if (queries.size() == 0) {
                log.error("No queries specified for request");
                Map<String, Object> error = new HashMap<String, Object>();
                error.put(ERROR_MESSAGE,
                        "At least one (1) metric query term must be specified, none found");
                error.put(ERROR_CAUSE, METRIC);
                error.put(ERROR_PART, METRIC);
                errors.add(error);
            }

            if (errors.size() > 0) {
                Map<String, Object> response = new HashMap<String, Object>();
                response.put(CLIENT_ID, id);
                response.put(ERRORS, errors);
                throw new WebApplicationException(Response.status(400)
                        .entity(new JSONObject(response).toString()).build());
            }

            if (serverTimeZone == null) {
                try {
                    serverTimeZone = getServerTimeZone();
                } catch (Throwable t) {
                    log.error(
                            "Unable to determine timezone of the performance metric server: {} : {}",
                            t.getClass().getName(), t.getMessage());
                    throw new WebApplicationException(
                            getErrorResponse(
                                    500,
                                    String.format("Unable to determine timezone of the performance metric server"),
                                    t));
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
                reader = getReader(getConfiguration(), id, convertedStartTime,
                        convertedEndTime, exactTimeWindow, series, queries);
            } catch (WebApplicationException e) {
                try {
                    throw new WebApplicationException(getErrorResponse(e
                            .getResponse().getStatus(),
                            new JSONObject(e.getResponse().getEntity()
                                    .toString()).getString(ERROR_MESSAGE),
                            getSourceId()));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    throw new WebApplicationException(getErrorResponse(500,
                            e.getMessage(), getSourceId()));
                }
            } catch (Throwable t) {
                log.error("Failed to connect to metric data source: {} : {}", t
                        .getClass().getName(), t.getMessage());
                throw new WebApplicationException(
                        getErrorResponse(
                                500,
                                String.format("Unable to connect to performance metric data source"),
                                t));

            }

            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                    output))) {

                writer.objectS()
                        .value(CLIENT_ID, id, true)
                        .value(SOURCE, getSourceId(), true)
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
                        getSourceId(), t.getClass().getName(), t.getMessage());
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
            Optional<Boolean> series, List<MetricQuery> queries) {

        QueryAppConfiguration config = getConfiguration();

        try {
            return Response.ok(
                    new Worker(config, id.or(NOT_SPECIFIED), startTime
                            .or(config.getPerformanceMetricQueryConfig()
                                    .getDefaultStartTime()), endTime.or(config
                            .getPerformanceMetricQueryConfig()
                            .getDefaultEndTime()), exactTimeWindow.or(config
                            .getPerformanceMetricQueryConfig()
                            .getDefaultExactTimeWindow()), series.or(config
                            .getPerformanceMetricQueryConfig()
                            .getDefaultSeries()), queries)).build();
        } catch (Throwable t) {
            return Response.status(500).build();
        }
    }
}
