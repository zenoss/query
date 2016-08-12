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
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                                      q
 */
package org.zenoss.app.metricservice.api.metric.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.metricservice.api.impl.DefaultResultProcessor;
import org.zenoss.app.metricservice.api.impl.IHasShortcut;
import org.zenoss.app.metricservice.api.impl.JacksonResultsWriter;
import org.zenoss.app.metricservice.api.impl.JacksonWriter;
import org.zenoss.app.metricservice.api.impl.MetricStorageAPI;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQueryResult;
import org.zenoss.app.metricservice.api.impl.ResultProcessor;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


@API
@Configuration
public class MetricService implements MetricServiceAPI {
    public static final String CLIENT_ID = "clientId";
    public static final String METRIC = "metric";
    public static final String ID = "id";
    public static final String NOT_SPECIFIED = "not-specified";
    private static final Logger log = LoggerFactory.getLogger(MetricService.class);
    public final ObjectMapper objectMapper;
    public JacksonResultsWriter jacksonResultsWriter = new JacksonResultsWriter();
    @Autowired
    MetricServiceAppConfiguration config;
    @Autowired
    MetricStorageAPI api;
    private String corsHeaders;

    public MetricService() {
        objectMapper = Utils.getObjectMapper();
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
                    log.debug("MetricFilter: filtering out metricSpecification {} - no metric value found.", spec.getNameOrMetric());
                }
            }
        }
        return result;
    }

    /**
     * It is a calculated value if it has a name, but no metric value, or if it has an expression
     *
     * @param list
     * @return
     */
    public static List<MetricSpecification> calculatedValueFilter(
            List<? extends MetricSpecification> list) {
        List<MetricSpecification> result = new ArrayList<>();
        if (list != null) {
            for (MetricSpecification spec : list) {
                if (spec.getName() != null && spec.getMetric() == null || spec.getExpression() != null) {
                    result.add(spec);
                }
            }
        }
        return result;
    }

    private static Response makeCORS(Response.ResponseBuilder responseBuilder, String returnMethod) {
        Response.ResponseBuilder rb = responseBuilder //Response.ok()
                .type(MediaType.APPLICATION_JSON)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, OPTIONS");

        if (!Strings.isNullOrEmpty(returnMethod)) {
            rb.header("Access-Control-Allow-Headers", returnMethod);
        }

        return rb.build();
    }


    @Override
    public Response query(Optional<String> id, Optional<String> start, Optional<String> end,
                          Optional<ReturnSet> returnset, Optional<Boolean> series, Optional<String> downsample,
                          double downsampleMultiplier, Optional<Map<String, List<String>>> tags,
                          List<MetricSpecification> metrics) {
        log.debug("Thread {}: entering MetricService.query()", Thread.currentThread().getId());
        //series should always be true.
        if (!series.or(this.config.getMetricServiceConfig().getDefaultSeries())) {
            UnsupportedOperationException e = new UnsupportedOperationException("Series is no longer supported.");
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
        return makeCORS(Response.ok(
                new MetricServiceWorker(id.or(NOT_SPECIFIED),
                        start.or(config.getMetricServiceConfig().getDefaultStartTime()),
                        end.or(config.getMetricServiceConfig().getDefaultEndTime()),
                        returnset.or(config.getMetricServiceConfig().getDefaultReturnSet()),

                        downsample.orNull(), downsampleMultiplier,
                        tags.orNull(),
                        metrics), MediaType.APPLICATION_JSON));
    }

    @Override
    public Response options(String request) {
        corsHeaders = request;
        return makeCORS(Response.ok(), request);
    }

    private Response makeCORS(Response.ResponseBuilder responseBuilder) {
        return makeCORS(responseBuilder, corsHeaders);
    }

    private class MetricServiceWorker implements StreamingOutput {
        private final String id;
        private final String startTime;
        private final String endTime;
        private final ReturnSet returnset;
        private final String downsample;
        private final double downsampleMultiplier;
        private final Map<String, List<String>> tags;
        private final List<MetricSpecification> queries;
        private long start = -1;
        private long end = -1;

        private MetricServiceWorker(String id,
                                    String startTime, String endTime, ReturnSet returnset,
                                    String downsample, double downsampleMultiplier,
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
            this.tags = tags;
            this.downsample = downsample;
            this.downsampleMultiplier = downsampleMultiplier;
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
            log.debug("write() entry.");
            Iterable<OpenTSDBQueryResult> otsdbResponse = null;
            try {
                // The getReader call queries the datastore (e.g. openTSDB) and returns a otsdbResponse for streaming the results.
                otsdbResponse = api.getResponse(config, id, convertedStartTime, convertedEndTime, returnset,
                        downsample, downsampleMultiplier, tags, metricFilter(queries));
                if (null == otsdbResponse) {
                    throw new IOException("Unable to get otsdbResponse from api.");
                }

                if (returnset == ReturnSet.LAST) {
                    log.debug("Applying last filter.");
                    otsdbResponse = translateOpenTsdbInputToLastInput(otsdbResponse, start, end); //new LastFilter(otsdbResponse, start, end);
                }
            } catch (WebApplicationException wae) {
                // Log 404 messages at lower level.
                if (Response.Status.NOT_FOUND.getStatusCode() == getStatusFromWebApplicationException(wae)) {
                    log.debug("Caught web exception ({}). Status: 404 (Not found). Rethrowing.", wae.getMessage());
                } else {
                    log.error("Caught web exception ({}). Rethrowing.", wae.getMessage());
                }
                throw wae;
            } catch (IOException e) {
                log.error("Failed to connect to metric data source: {} : {}", e.getClass().getName(), e.getMessage(), e);
                throw new WebApplicationException(
                        Utils.getErrorResponse(
                                id,
                                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                String.format("Unable to connect to performance metric data source: %s", e.getMessage()),
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
            if (downsample != null && downsample.length() > 1) {
                bucketSize = Utils.parseDuration(downsample);
                log.debug("Downsample was {}: setting bucketSize to {}.", downsample, bucketSize);

            }
            try {
                writeResultsUsingJacksonWriter(output, otsdbResponse, bucketSize);
            } catch (ClassNotFoundException e) {
                throw new WebApplicationException(
                        Utils.getErrorResponse(id,
                                Response.Status.NOT_FOUND.getStatusCode(),
                                String.format("Unable to write results: %s", e.getMessage()),
                                e.getMessage()));
            }
        }

        private int getStatusFromWebApplicationException(WebApplicationException wae) {
            // Response.getStatus uses -1 for 'not set'. We will, too.
            int result = -1;
            Response response = wae.getResponse();
            if (null != response) {
                result = response.getStatus();
            }
            return result;
        }

        /**
         * translateOpenTsdbInputToLastInput:
         * <p/>
         * Handler for 'last' specification - reads through datapoints in series, remembering and returning the datapoint
         * with the greatest timestamp betweeen start and end.
         */
        private Iterable<OpenTSDBQueryResult> translateOpenTsdbInputToLastInput(Iterable<OpenTSDBQueryResult> queryResult, long start, long end) throws IOException {

            // make a new list of resulsts, containing only the last data points per series (between start and end)
            // iterate through list, modifying each of the members in-place.
            List<OpenTSDBQueryResult> lastResults = new LinkedList<>();
            for (OpenTSDBQueryResult originalResult : queryResult) {
                replaceSeriesDataPointsWithLastInRangeDataPoint(originalResult, start, end);
                lastResults.add(originalResult);
            }
            return lastResults;

        }

        private void replaceSeriesDataPointsWithLastInRangeDataPoint(OpenTSDBQueryResult series, long startTimeStamp, long endTimeStamp) {
            long currentPointTimeStamp;
            SortedMap<Long, Double> dataPointSingleton = new TreeMap<>();
            Map.Entry<Long, Double> lastDataPoint = null;
            for (Map.Entry<Long, Double> dataPoint : series.getDataPoints().entrySet()) {
                currentPointTimeStamp = dataPoint.getKey();
                if (currentPointTimeStamp < startTimeStamp || currentPointTimeStamp > endTimeStamp) {
                    continue;
                }
                if (null == lastDataPoint || currentPointTimeStamp > lastDataPoint.getKey()) {
                    lastDataPoint = dataPoint;
                }
            }
            if (null != lastDataPoint) {
                dataPointSingleton.put(lastDataPoint.getKey(), lastDataPoint.getValue());
            }
            series.setDataPoints(dataPointSingleton);
        }

        private void writeResultsUsingJacksonWriter(OutputStream output, Iterable<OpenTSDBQueryResult> results, long bucketSize)
                throws IOException, ClassNotFoundException {
            log.debug("Using JacksonWriter to generate JSON results.");
            try (JacksonWriter writer = new JacksonWriter(new OutputStreamWriter(output, "UTF-8"))) {
                log.debug("processing results");
                ResultProcessor processor = new DefaultResultProcessor(results, queries, bucketSize);
                Buckets<IHasShortcut> buckets = processor.processResults();
                log.debug("results processed.");
                jacksonResultsWriter.writeResults(writer, queries, buckets,
                        id, api.getSourceId(), start, startTime, end, endTime, returnset);
                log.debug("back from jacksonResultsWriter");
            }
        }

        private void validateParameters() throws JsonProcessingException {
            List<Object> errors = new ArrayList<>();

            // Validate start time
            start = parseTimeWithErrorHandling(startTime, Utils.START, errors);
            // Validate end time
            end = parseTimeWithErrorHandling(endTime, Utils.END, errors);

            // Validate that there is at least one (1) metric specification
            validateQueriesWithErrorHandling(errors);

            if (errors.size() > 0) {
                Map<String, Object> response = new HashMap<>();
                response.put(CLIENT_ID, id);
                response.put(Utils.ERRORS, errors);
                throw new WebApplicationException(Response
                        .status(Response.Status.BAD_REQUEST.getStatusCode())
                        .entity(objectMapper.writer().writeValueAsString(response))
                        .build());
            }
        }

        private void validateQueriesWithErrorHandling(List<Object> errors) {
            if (queries.size() == 0) {
                log.error("No queries specified for request");
                errors.add(Utils.makeError("At least one (1) metric query term must be specified, none found", METRIC, METRIC));
            }
            for (MetricSpecification query : queries) {
                query.validateWithErrorHandling(errors);
                query.mergeTags(this.tags);
            }
        }

        private long parseTimeWithErrorHandling(String timeString, String timeTypeDescription, List<Object> errors) {
            long result = -1;
            try {
                result = Utils.parseDate(timeString);
            } catch (ParseException e) {
                handleTimeParseException(errors, timeString, e, timeTypeDescription);
            }
            return result;
        }

        private void handleTimeParseException(List<Object> errors, String startTime, ParseException e, String timeType) {
            log.error("Failed to parse {} time option of '{}': {} : {}", timeType, startTime, e.getClass().getName(), e.getMessage());
            String errorString = String.format("Unable to parse specified %s time value of '%s'", timeType, startTime);
            errors.add(Utils.makeError(errorString, e.getMessage(), timeType));
        }

    }

}
