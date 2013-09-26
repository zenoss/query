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

import java.io.IOException;
import java.util.List;

import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

/**
 * @author Zenoss
 * 
 */
abstract public class BaseResultWriter implements ResultWriter {

    /**
     * Writes the data associated with the buckets to the output stream.
     * 
     * @param writer
     *            stream on which to output the JSON representation of the
     *            buckets.
     * @param queries
     *            request that generated the data
     * @param buckets
     *            buckets that contain the data to be output
     * @param returnset
     *            specifies if all results are returned or only those in the
     *            specified query range (this is needed because by default
     *            OpenTSDB may return results outside of the query range).
     * @param startTs
     *            ms since epoch that represents the start of the query range
     * @param endTs
     *            ms since epoch that represents the end of the query range
     * @throws IOException
     *             when an IO exception occurs writing to the output stream
     */
    abstract protected void writeData(JsonWriter writer,
            List<MetricSpecification> queries,
            Buckets<MetricKey, String> buckets, ReturnSet returnset,
            long startTs, long endTs) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.api.impl.ResultWriter#writeResults(org.zenoss
     * .app.metricservice.api.impl.JsonWriter,
     * org.zenoss.app.metricsevice.buckets.Buckets, java.util.List,
     * java.lang.String, java.lang.String, long, java.lang.String,
     * java.lang.String, long, java.lang.String, java.lang.String,
     * org.zenoss.app.metricservice.api.model.ReturnSet, boolean)
     */
    public void writeResults(JsonWriter writer,
            List<MetricSpecification> queries,
            Buckets<MetricKey, String> buckets, String id, String sourceId,
            long startTs, String startTimeConfig, String startTimeActual,
            long endTs, String endTimeConfig, String endTimeActual,
            ReturnSet returnset, boolean series) throws IOException {

        writer.objectS().value(MetricService.CLIENT_ID, id, true)
                .value(MetricService.SOURCE, sourceId, true)
                .value(MetricService.START_TIME, startTimeConfig, true)
                .value(MetricService.START_TIME_ACTUAL, startTimeActual, true)
                .value(MetricService.END_TIME, endTimeConfig, true)
                .value(MetricService.END_TIME_ACTUAL, endTimeActual, true)
                .value(MetricService.RETURN_SET, returnset, true)
                .value(MetricService.SERIES, series, true)
                .arrayS(MetricService.RESULTS);

        writeData(writer, queries, buckets, returnset, startTs, endTs);

        writer.arrayE().objectE(); // end the whole thing
        writer.flush();
    }
}
