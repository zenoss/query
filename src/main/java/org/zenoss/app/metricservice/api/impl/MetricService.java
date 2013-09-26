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
import java.io.Reader;
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
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;

/**
 * @author Zenoss
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
    protected static final String RETURN_SET = "returnset";
    protected static final String TIMESTAMP = "timestamp";
    protected static final String SERIES = "series";
    protected static final String VALUE = "value";
    protected static final String TAGS = "tags";

    protected static final String NOT_SPECIFIED = "not-specified";

    protected ResultProcessor resultsProcessor = new DefaultResultProcessor();
    protected ResultWriter seriesResultsWriter = new SeriesResultWriter();
    protected ResultWriter asIsResultsWriter = new LineResultWriter();

    private static final Logger log = LoggerFactory
            .getLogger(MetricService.class);

    public static List<MetricSpecification> metricFilter(
            List<? extends MetricSpecification> list) {
        List<MetricSpecification> result = new ArrayList<MetricSpecification>();
        if (list != null) {
            for (MetricSpecification spec : list) {
                if (spec.getMetric() != null) {
                    result.add((MetricSpecification) spec);
                }
            }
        }
        return result;
    }

    /**
     * It is a calculated value if it has a name, but no metric value
     * 
     * @param list
     * @return
     */
    public static List<MetricSpecification> valueFilter(
            List<? extends MetricSpecification> list) {
        List<MetricSpecification> result = new ArrayList<MetricSpecification>();
        if (list != null) {
            for (MetricSpecification spec : list) {
                if (spec.getName() != null && spec.getMetric() == null) {
                    result.add((MetricSpecification) spec);
                }
            }
        }
        return result;
    }

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

    /**
     * Used as a buffer filter class when return the "last" values for a query.
     * This essentially only return the next line from the underlying reader
     * when the timestamp delta goes negative (indicating that the tags have
     * changes in a query) this would mean the last value in a given series and
     * the value is returned as the next line in the file.
     */
    private static class LastFilter extends BufferedReader {
        private String lastLine = null;
        private long lastTs = -1;
        private final long startTs;
        private final long endTs;

        /**
         * @param in
         */
        public LastFilter(Reader in, Date startTs, Date endTs) {
            super(in);
            this.startTs = startTs.getTime() / 1000;
            this.endTs = endTs.getTime() / 1000;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.BufferedReader#readLine()
         */
        @Override
        public String readLine() throws IOException {
            // Read from the input stream until we see the timestamp
            // go backward and then return the previous value
            String line = null, result = null;
            long ts = -1;

            while ((line = super.readLine()) != null) {
                ts = Long.parseLong(line.split(" ", 4)[1]);
                // Remove any TS that is outside the start/end range
                if (ts < startTs || ts > endTs) {
                    continue;
                }
                if (ts < lastTs) {
                    lastTs = ts;
                    result = lastLine;
                    lastLine = line;
                    return result;
                }
                lastLine = line;
                lastTs = ts;
            }
            result = lastLine;
            lastLine = null;
            return result;
        }
    }

    private class Worker implements StreamingOutput {
        private final String id;
        private final String startTime;
        private final String endTime;
        private final ReturnSet returnset;
        private final Boolean series;
        private final String downsample;
        private final String grouping;
        private final Map<String, List<String>> tags;
        private final List<MetricSpecification> queries;
        private long start = -1;
        private long end = -1;

        public Worker(MetricServiceAppConfiguration config, String id,
                String startTime, String endTime, ReturnSet returnset,
                Boolean series, String downsample, String grouping,
                Map<String, List<String>> tags,
                List<MetricSpecification> queries) {
            if (queries == null) {
                // This really should never happen as the query check should
                // happen in our calling routine, but just in case.
                log.error("Attempt to create query worker without any queries specified");
                IllegalArgumentException t = new IllegalArgumentException(
                        "No queries specified");
                throw t;
            }
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.returnset = returnset;
            this.series = series;
            this.tags = tags;
            this.downsample = downsample;
            this.grouping = grouping;
            this.queries = queries;
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
            }

            if (serverTimeZone == null) {
                try {
                    serverTimeZone = api.getServerTimeZone();
                } catch (Exception e) {
                    log.error(
                            "Unable to determine timezone of the performance metric server: {} : {}",
                            e.getClass().getName(), e.getMessage());
                    throw new WebApplicationException(
                            Utils.getErrorResponse(
                                    id,
                                    500,
                                    String.format("Unable to determine timezone of the performance metric server"),
                                    e.getMessage()));
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
                        convertedEndTime, returnset, series, downsample, tags,
                        MetricService.metricFilter(queries));
                if (returnset == ReturnSet.LAST) {
                    reader = new LastFilter(reader, startDate, endDate);
                }
            } catch (WebApplicationException wae) {
                throw wae;
            } catch (Exception e) {
                log.error(String.format(
                        "Failed to connect to metric data source: %s : %s", e
                                .getClass().getName(), e.getMessage()), e);
                throw new WebApplicationException(
                        Utils.getErrorResponse(
                                id,
                                500,
                                String.format("Unable to connect to performance metric data source"),
                                e.getMessage()));

            }

            /**
             * TODO: Deal with no bucket specification better. Create a bucket
             * size of 1 second means that we are behaving correctly, but it
             * also means we are going a lot more work than we really need to as
             * we would just directly stream the results without processing them
             * into buckets.
             */
            long bucketSize = 1;
            if (grouping != null && grouping.length() > 1) {
                bucketSize = Utils.parseDuration(grouping);
            }
            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                    output))) {
                Buckets<MetricKey, String> buckets = resultsProcessor
                        .processResults(reader, queries, bucketSize);

                if (series) {
                    seriesResultsWriter.writeResults(writer, queries, buckets,
                            id, api.getSourceId(), start, startTime,
                            actual.format(startDate), end, endTime,
                            actual.format(endDate), returnset, series);
                } else {

                    asIsResultsWriter.writeResults(writer, queries, buckets,
                            id, api.getSourceId(), start, startTime,
                            actual.format(startDate), end, endTime,
                            actual.format(endDate), returnset, series);
                }
            } catch (Exception e) {
                log.error(
                        String.format(
                                "Server error while processing metric source %s : %s:%s",
                                api.getSourceId(), e.getClass().getName(),
                                e.getMessage()), e);
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
     * org.zenoss.app.metricservice.api.MetricServiceAPI#query(com.google.common
     * .base.Optional, com.google.common.base.Optional,
     * com.google.common.base.Optional, com.google.common.base.Optional,
     * com.google.common.base.Optional, com.google.common.base.Optional,
     * com.google.common.base.Optional, com.google.common.base.Optional,
     * java.util.List)
     */
    @Override
    public Response query(Optional<String> id, Optional<String> startTime,
            Optional<String> endTime, Optional<ReturnSet> returnset,
            Optional<Boolean> series, Optional<String> downsample,
            Optional<String> grouping,
            Optional<Map<String, List<String>>> tags,
            List<MetricSpecification> queries) {

        try {
            return Response.ok(
                    new Worker(config, id.or(NOT_SPECIFIED), startTime
                            .or(config.getMetricServiceConfig()
                                    .getDefaultStartTime()), endTime.or(config
                            .getMetricServiceConfig().getDefaultEndTime()),
                            returnset.or(config.getMetricServiceConfig()
                                    .getDefaultReturnSet()), series.or(config
                                    .getMetricServiceConfig()
                                    .getDefaultSeries()), downsample.orNull(),
                            grouping.orNull(), tags.orNull(), queries)).build();
        } catch (Exception e) {
            log.error(String.format(
                    "Error While attempting to query data soruce: %s : %s", e
                            .getClass().getName(), e.getMessage()));
            return Response.status(500).build();
        }
    }
}
