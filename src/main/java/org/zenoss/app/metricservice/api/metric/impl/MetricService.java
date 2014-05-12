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
package org.zenoss.app.metricservice.api.metric.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.metricservice.api.impl.*;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zenoss
 */
@API
@Configuration
public class MetricService implements MetricServiceAPI {
    public static final String CLIENT_ID = "clientId";
    public static final String SOURCE = "source";
    public static final String START_TIME = "startTime";
    public static final String START_TIME_ACTUAL = "startTimeActual";
    public static final String END_TIME = "endTime";
    public static final String END_TIME_ACTUAL = "endTimeActual";
    public static final String RESULTS = "results";
    public static final String DATAPOINTS = "datapoints";
    public static final String AGGREGATOR = "aggregator";
    public static final String RATE = "rate";
    public static final String DOWNSAMPLE = "downsample";
    public static final String METRIC = "metric";
    public static final String RETURN_SET = "returnset";
    public static final String TIMESTAMP = "timestamp";
    public static final String SERIES = "series";
    public static final String VALUE = "value";
    public static final String TAGS = "tags";
    public static final String NOT_SPECIFIED = "not-specified";
    private static final Logger log = LoggerFactory.getLogger(MetricService.class);
    public final ObjectMapper objectMapper;
    public ResultProcessor resultsProcessor = new DefaultResultProcessor();
    public ResultWriter seriesResultsWriter = new SeriesResultWriter();
    public ResultWriter asIsResultsWriter = new LineResultWriter();
    @Autowired
    MetricServiceAppConfiguration config;
    @Autowired
    MetricStorageAPI api;

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

    public static List<MetricSpecification> metricFilter(List<? extends MetricSpecification> list) {
        List<MetricSpecification> result = new ArrayList<>();
        if (list != null) {
            for (MetricSpecification spec : list) {
                if (spec.getMetric() != null) {
                    result.add(spec);
                } else {
                    log.warn("skipping metricSpecification - no metric value found.");
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
    public static List<MetricSpecification>  valueFilter(
            List<? extends MetricSpecification> list) {
        List<MetricSpecification> result = new ArrayList<>();
        if (list != null) {
            for (MetricSpecification spec : list) {
                if (spec.getName() != null && spec.getMetric() == null) {
                    result.add(spec);
                }
            }
        }
        return result;
    }

    @Override
    public Response query(Optional<String> id, Optional<String> start, Optional<String> end, Optional<ReturnSet> returnset, Optional<Boolean> series, Optional<String> downsample, Optional<String> grouping, Optional<Map<String, List<String>>> tags, List<MetricSpecification> metrics) {
        log.info("entering MetricService.query()");
        try {
            return makeCORS(Response.ok(
                    new NewWorker(config, id.or(NOT_SPECIFIED),
                            start.or(config.getMetricServiceConfig().getDefaultStartTime()),
                            end.or(config.getMetricServiceConfig().getDefaultEndTime()),
                            returnset.or(config.getMetricServiceConfig().getDefaultReturnSet()),
                            series.or(config.getMetricServiceConfig().getDefaultSeries()),
                            downsample.orNull(),
                            grouping.orNull(),
                            tags.orNull(),
                            metrics)));
        } catch (Exception e) {
            log.error(String.format(
                    "Error While attempting to query data source: %s : %s", e
                    .getClass().getName(), e.getMessage()));
            return makeCORS(Response.status(500));
        }
    }

    @Override
    public Response options(String request) {
        log.info("entering MetricService.options()");
        corsHeaders = request;
        return makeCORS(Response.ok(), request);
    }
    /**
     * Used as a buffer filter class when return the "last" values for a query.
     * This essentially only return the next line from the underlying reader
     * when the timestamp delta goes negative (indicating that the tags have
     * changes in a query) this would mean the last value in a given series and
     * the value is returned as the next line in the file.
     */
    private static class LastFilter extends BufferedReader {
        private final long startTs;
        private final long endTs;
        private String lastLine = null;
        private long lastTs = -1;

        /**
         * @param in
         */
        public LastFilter(Reader in, long startTs, long endTs) {
            super(in);
            this.startTs = startTs;
            this.endTs = endTs;
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

    private class NewWorker implements StreamingOutput {
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

        //private final Logger log = LoggerFactory.getLogger(NewWorker.class);

        public NewWorker(MetricServiceAppConfiguration config, String id,
                         String startTime, String endTime, ReturnSet returnset,
                         Boolean series, String downsample, String grouping,
                         Map<String, List<String>> tags,
                         List<MetricSpecification> queries) {
            if (queries == null) {
                // This really should never happen as the query check should
                // happen in our calling routine, but just in case.
                log.error("Attempt to create query worker without any queries specified");
                throw new IllegalArgumentException("No queries specified");
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

            validateParameters();
            // Validate the input parameters. Throw exception if any are bad.

            String convertedStartTime = Long.toString(start);
            String convertedEndTime = Long.toString(end);

            BufferedReader reader = null;
            try {
                reader = api.getReader(config, id, convertedStartTime, convertedEndTime, returnset, series, downsample, tags, MetricService.metricFilter(queries));
                if (null == reader) {
                    throw new Exception("Unable to get reader from api.");
                }
                if (returnset == ReturnSet.LAST) {
                    reader = new LastFilter(reader, start, end);
                }
            } catch (WebApplicationException wae) {
                throw wae;
            } catch (Exception e) {
                log.error(String.format("Failed to connect to metric data source: %s : %s", e.getClass().getName(), e.getMessage()), e);
                throw new WebApplicationException(
                        Utils.getErrorResponse(
                                id,
                                500,
                                String.format("Unable to connect to performance metric data source"),
                                e.getMessage()));
            }

            /**
             * Deal with no bucket specification better. Create a bucket size of
             * 1 second means that we are behaving correctly, but it also means
             * we are going a lot more work than we really need to as we would
             * just directly stream the results without processing them into
             * buckets.
             */
            long bucketSize = 1;
            if (grouping != null && grouping.length() > 1) {
                bucketSize = Utils.parseDuration(grouping);
            }
            try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(output))) {
                Buckets<MetricKey, String> buckets = resultsProcessor.processResults(reader, queries, bucketSize);

                if (series) {
                    seriesResultsWriter.writeResults(writer, queries, buckets,
                            id, api.getSourceId(), start, startTime, end, endTime, returnset, series);
                } else {
                    asIsResultsWriter.writeResults(writer, queries, buckets,
                            id, api.getSourceId(), start, startTime,
                            end, endTime, returnset, series);
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

        private void validateParameters() throws JsonProcessingException {
            List<Object> errors = new ArrayList<>();

            // Validate start time
            try {
                start = Utils.parseDate(startTime);
            } catch (ParseException e) {
                log.error("Failed to parse start time option of '{}': {} : {}", startTime, e.getClass().getName(), e.getMessage());
                errors.add(makeError(String.format("Unable to parse specified start time value of '%s'", startTime), e.getMessage(), Utils.START));
            }

            // Validate end time
            try {
                end = Utils.parseDate(endTime);
            } catch (ParseException e) {
                log.error("Failed to parse end time option of '{}': {} : {}", endTime, e.getClass().getName(), e.getMessage());
                errors.add(makeError(String.format("Unable to parse specified end time value of '%s'", startTime), e.getMessage(), Utils.END));
            }

            // Validate that there is at least one (1) metric specification
            if (queries.size() == 0) {
                log.error("No queries specified for request");
                errors.add(makeError("At least one (1) metric oldQuery term must be specified, none found", METRIC, METRIC));
            }

            if (errors.size() > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put(CLIENT_ID, id);
                response.put(Utils.ERRORS, errors);
                throw new WebApplicationException(Response
                        .status(400)
                        .entity(objectMapper.writer().writeValueAsString(response))
                        .build());
            }
        }

        private Map<String, Object> makeError(String errorMessage, String errorCause, String errorPart) {
            Map<String, Object> error = new HashMap<>();
            error.put(Utils.ERROR_MESSAGE, errorMessage);
            error.put(Utils.ERROR_CAUSE, errorCause);
            error.put(Utils.ERROR_PART, errorPart);
            return error;
        }
    }


    /***************BEGIN CORS HACK***************/
    private String corsHeaders;

    private Response makeCORS(Response.ResponseBuilder responseBuilder, String returnMethod) {
        Response.ResponseBuilder rb = responseBuilder //Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        if (!"".equals(returnMethod)) {
            rb.header("Access-Control-Allow-Headers", returnMethod);
        }

        return rb.build();
    }

    private Response makeCORS(Response.ResponseBuilder responseBuilder) {
        return makeCORS(responseBuilder, corsHeaders);
    }

    /*************************END CORS HACK*******************************/
}
