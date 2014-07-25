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
import org.zenoss.app.metricservice.api.impl.*;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.calculators.UnknownReferenceException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.text.ParseException;
import java.util.*;


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
                    log.warn("skipping metricSpecification {} - no metric value found.", spec.getNameOrMetric());
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
    public static List<MetricSpecification> calculatedValueFilter(
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

    private static Response makeCORS(Response.ResponseBuilder responseBuilder, String returnMethod) {
        Response.ResponseBuilder rb = responseBuilder //Response.ok()
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
        log.info("entering MetricService.query()");
        return makeCORS(Response.ok(
            new MetricServiceWorker(id.or(NOT_SPECIFIED),
                start.or(config.getMetricServiceConfig().getDefaultStartTime()),
                end.or(config.getMetricServiceConfig().getDefaultEndTime()),
                returnset.or(config.getMetricServiceConfig().getDefaultReturnSet()),
                series.or(config.getMetricServiceConfig().getDefaultSeries()),
                downsample.orNull(), downsampleMultiplier,
                tags.orNull(),
                metrics)));
    }

    @Override
    public Response options(String request) {
        log.info("entering MetricService.options()");
        corsHeaders = request;
        return makeCORS(Response.ok(), request);
    }

    private Response makeCORS(Response.ResponseBuilder responseBuilder) {
        return makeCORS(responseBuilder, corsHeaders);
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

        /**
         * @param in
         */
        private LastFilter(Reader in, long startTs, long endTs) {
            super(in);
            this.startTs = startTs;
            this.endTs = endTs;
            log.info("LastFilter constructed.");
        }

        /*
         * (non-Javadoc)
         *
         * @see java.io.BufferedReader#readLine()
         */
        @Override
        public String readLine() throws IOException {
            log.info("readLine() entry.");
            // Read from the input stream until we see the timestamp
            // go backward and then return the previous value
            String line;

            StringBuilder jsonString = new StringBuilder();
            while ((line = super.readLine()) != null) {
                log.debug("line = {}", line);
                jsonString.append(line);
            }
            ObjectMapper mapper = Utils.getObjectMapper();
            SeriesQueryResult originalResult;
            originalResult = mapper.readValue(jsonString.toString(), SeriesQueryResult.class);
            for (QueryResult series : originalResult.getResults()) {
                replaceSeriesDataPointsWithLastInRangeDataPoint(series);
            }
            // write results (JSON serialization)
            String resultJson = Utils.jsonStringFromObject(originalResult);
            log.debug("Resulting JSON: {}", resultJson);
            return resultJson;
        }

        private void replaceSeriesDataPointsWithLastInRangeDataPoint(QueryResult series) {
            long ts;
            List<QueryResultDataPoint> dataPointSingleton = new ArrayList<>(1);
            QueryResultDataPoint lastDataPoint = null;
            for (QueryResultDataPoint dataPoint : series.getDatapoints()) {
                ts = dataPoint.getTimestamp();
                if (ts < startTs || ts > endTs) {
                    continue;
                }
                if (null == lastDataPoint || ts > lastDataPoint.getTimestamp()) {
                    lastDataPoint = dataPoint;
                }
            }
            if (null != lastDataPoint) {
                dataPointSingleton.add(lastDataPoint);
            }
            series.setDatapoints(dataPointSingleton);
        }
    }

    private class MetricServiceWorker implements StreamingOutput {
        private final String id;
        private final String startTime;
        private final String endTime;
        private final ReturnSet returnset;
        private final Boolean outputAsSeries;
        private final String downsample;
        private final double downsampleMultiplier;
        private final Map<String, List<String>> tags;
        private final List<MetricSpecification> queries;
        private long start = -1;
        private long end = -1;

        private MetricServiceWorker(String id,
                                    String startTime, String endTime, ReturnSet returnset,
                                    Boolean outputAsSeries, String downsample, double downsampleMultiplier,
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
            this.outputAsSeries = outputAsSeries;
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
            log.info("write() entry.");
            BufferedReader reader = null;
            try {
                // The getReader call queries the datastore (e.g. openTSDB) and returns a reader for streaming the results.
                reader = api.getReader(config, id, convertedStartTime, convertedEndTime, returnset, outputAsSeries,
                    downsample, downsampleMultiplier, tags, metricFilter(queries));
                if (null == reader) {
                    throw new IOException("Unable to get reader from api.");
                }

                log.info("returnset = {}", returnset);
                if (returnset == ReturnSet.LAST) {
                    log.info("Applying last filter.");
                    reader = translateOpenTsdbInputToLastInput(reader, start, end); //new LastFilter(reader, start, end);
                }
            } catch (WebApplicationException wae) {
                log.error(String.format("Caught web exception (%s). Rethrowing.", wae.getMessage()));
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
                log.info("Downsample was {}: setting bucketSize to {}.", downsample, bucketSize);

            }
            try {
                writeResultsUsingJacksonWriter(output, reader, bucketSize);
            } catch (ClassNotFoundException e) {
                throw new WebApplicationException(
                    Utils.getErrorResponse(id,
                        Response.Status.NOT_FOUND.getStatusCode(),
                        String.format("Unable to write results: %s", e.getMessage()),
                        e.getMessage()));
            } catch (UnknownReferenceException e) {
                throw new WebApplicationException(
                    Utils.getErrorResponse(id,
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        String.format("Unable to write results: %s", e.getMessage()),
                        e.getMessage()));
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }

        /**
         * translateOpenTsdbInputtoLastInput:
         *
         * Handler for 'last' specification - reads through datapoints in series, remembering and returning the datapoint
         * with the greatest timestamp betweeen start and end.
         *
         * */
        private BufferedReader translateOpenTsdbInputToLastInput(BufferedReader reader, long start, long end) throws IOException {

            // read query results from datastream (reader)
            List<OpenTSDBQueryResult> allResults = new ArrayList<>();
            ObjectMapper mapper = Utils.getObjectMapper();
            OpenTSDBQueryResult[] queryResult = mapper.readValue(reader, OpenTSDBQueryResult[].class);
            allResults.addAll(Arrays.asList(queryResult));

            // make a new list of resulsts, containing only the last data points per series (between start and end)
            List<OpenTSDBQueryResult> lastDataPointResults = getLastDataPoints(allResults, start, end);

            // encode the results as JSON for return
            String resultJson = Utils.jsonStringFromObject(lastDataPointResults);

            return new BufferedReader(new StringReader(resultJson));
        }

        private List<OpenTSDBQueryResult> getLastDataPoints(List<OpenTSDBQueryResult> allResults, long start, long end) {
            // iterate through list, modifying each of the members in-place.
            for (OpenTSDBQueryResult originalResult : allResults) {
                replaceSeriesDataPointsWithLastInRangeDataPoint(originalResult, start, end);
            }
            return allResults;

        }

        private void replaceSeriesDataPointsWithLastInRangeDataPoint(OpenTSDBQueryResult series, long startTimeStamp, long endTimeStamp) {
            long currentPointTimeStamp;
            SortedMap<Long, String> dataPointSingleton = new TreeMap<>();
            Map.Entry<Long, String> lastDataPoint = null;
            for (Map.Entry<Long, String> dataPoint : series.getDataPoints().entrySet()) {
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

        private void writeResultsUsingJacksonWriter(OutputStream output, BufferedReader reader, long bucketSize)
            throws IOException, UnknownReferenceException, ClassNotFoundException {
            log.info("Using JacksonWriter to generate JSON results.");
            try (JacksonWriter writer = new JacksonWriter(new OutputStreamWriter(output, "UTF-8"))) {
                log.debug("processing results");
                ResultProcessor processor = new DefaultResultProcessor(reader, queries, bucketSize);
                Buckets<IHasShortcut> buckets = processor.processResults();
                log.debug("results processed.");
                //log.info("calling jacksonResultsWriter. Buckets = {}", Utils.jsonStringFromObject(buckets));
                jacksonResultsWriter.writeResults(writer, queries, buckets,
                    id, api.getSourceId(), start, startTime, end, endTime, returnset, outputAsSeries);
                log.info("back from jacksonResultsWriter");
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
