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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    // Time values
    public static final String NOW = "now";
    public static final String DEFAULT_START_TIME = "1h-ago";
    public static final String DEFAULT_END_TIME = NOW;

    private static final long HEURISTIC_EPOCH = 649753200000L;

    // Error tags
    public static final String NOT_SPECIFIED = "not-specified";
    public static final String CLIENT_ID = "id";
    public static final String ERRORS = "errors";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String ERROR_CAUSE = "errorSource";
    public static final String ERROR_PART = "errorPart";
    public static final String START = "start";
    public static final String END = "end";
    public static final String COUNT = "count";
    public static final double DEFAULT_DOWNSAMPLE_MULTIPLIER = 2.0;
    public static final int DAYS_PER_YEAR = 365;
    public static final int DAYS_PER_WEEK= 7;
    public static final int HOURS_PER_DAY = 24;
    public static final int MINUTES_PER_HOUR = 60;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR = MINUTES_PER_HOUR * SECONDS_PER_MINUTE;
    public static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    public static final int SECONDS_PER_WEEK = SECONDS_PER_DAY * DAYS_PER_WEEK;
    public static final int SECONDS_PER_YEAR = SECONDS_PER_DAY * DAYS_PER_YEAR;

    private Utils() {};

    private static final ObjectMapper mapper;
    static {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static Map<String, Object> makeError(String errorMessage, String errorCause, String errorPart) {
        Map<String, Object> error = new HashMap<>();
        error.put(ERROR_MESSAGE, errorMessage);
        error.put(ERROR_CAUSE, errorCause);
        error.put(ERROR_PART, errorPart);
        return error;
    }


    static class ErrorResponse {
        public String id;
        public String errorMessage;
        public String errorSource;
    }

    public static Response getErrorResponse(String id, int status,
                                            String message, String context) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.id = id;
        errorResponse.errorSource = context;
        errorResponse.errorMessage = message;

        return Response.status(status).entity(jsonStringFromObject(errorResponse)).build();
    }

    public static String createUuid() {
        return UUID.randomUUID().toString();
    }

    public static long parseDate(String value) throws ParseException {
        String v = value.trim();

        if (NOW.equals(v)) {
            return new Date().getTime() / 1000;
        }

        if (v.endsWith("-ago")) {
            return new Date().getTime() / 1000
                - parseDuration(v.substring(0, v.length() - 4));
        }

        if (v.indexOf('/') == -1) {
            /*
             * No dash, assume it is a number representing seconds or ms since
             * unix epoch.
             */
            long result = 0;
            try {
                result = Long.parseLong(v);

                /*
                 * Check to see if they gave us seconds or ms. This is really a
                 * heuristic as we can't really be sure. How we will check is if
                 * the value is > a well known epoch value then we will assume
                 * it is a ms value.
                 */
                if (result > HEURISTIC_EPOCH) {
                    result /= 1000;
                }
                return result;
            } catch (NumberFormatException nfe) {
                // Must not be a epoch number, keep trying something else
            }
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

    public static long parseDuration(String v) {

        /*
         * Be a bit lenient here. If the value is actually a downsample
         * specification, which would be a duration followed by a dash and an
         * aggregation method, simply use the direction bit.
         */
        int idx;
        char last;
        if ((idx = v.indexOf('-')) > 0) {
            idx -= 1;
            last = v.charAt(idx);
        } else {
            idx = v.length() - 1;
            last = v.charAt(idx);
        }

        long period = 0;
        try {
            period = Long.parseLong(v.substring(0, idx));
        } catch (NumberFormatException e) {
            return 0;
        }

        switch (last) {
            case 's':
                return period;
            case 'm':
                return period * SECONDS_PER_MINUTE;
            case 'h':
                return period * SECONDS_PER_HOUR;
            case 'd':
                return period * SECONDS_PER_DAY;
            case 'w':
                return period * SECONDS_PER_WEEK;
            case 'y':
                return period * SECONDS_PER_YEAR;
        }

        return 0;
    }

    public static  String jsonStringFromObject(Object object) {
        ObjectMapper mapper = getObjectMapper();
        ObjectWriter ow = mapper.writer();
        String json = null;
        try {
            json = ow.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static ObjectMapper getObjectMapper() {
        return mapper;
    }
}
