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

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.core.Response;

public class Utils {

    // Time values
    public static final String NOW = "now";
    public static final String DETAULT_START_TIME = "1h-ago";
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

    static public Response getErrorResponse(String id, int status,
            String message, String context) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonWriter response = new JsonWriter(new OutputStreamWriter(baos));
            response.objectS();
            String prefix = "";
            if (id != null) {
                response.value(CLIENT_ID, id);
                prefix = ",";
            }
            if (message != null) {
                response.write(prefix);
                response.value(ERROR_MESSAGE, message);
                prefix = ",";
            }
            if (context != null) {
                response.write(prefix);
                response.value(ERROR_CAUSE, context);
            }
            response.objectE();
            response.close();
            return Response.status(status).entity(baos.toString()).build();
        } catch (Exception e) {
            return Response.status(status).build();
        }
    }

    static public String createUuid() {
        return UUID.randomUUID().toString();
    }

    static public long parseDate(String value) throws ParseException {
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
         * aggregation method, simply use the durection bit.
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

        int period = 0;
        try {
            period = Integer.parseInt(v.substring(0, idx));
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
            return period * 60 * 60 * 24 * 365;
        }

        return 0;
    }
}
