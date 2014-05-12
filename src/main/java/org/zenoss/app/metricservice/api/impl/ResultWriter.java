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
 * Specifies the interface to an implementation that will output the data
 * contained in the buckets to the specified JSON writer.
 * 
 * @author Zenoss
 */
public interface ResultWriter {

    /**
     * Writes to the given JSON writer the results contained in the given
     * buckets with appropriate header information derived from the parameters.
     * 
     * @param writer
     *            stream on which to output the JSON representation of the
     *            buckets.
     * @param queries
     *            request that generated the data
     * @param buckets
     *            buckets that contain the data to be output
     * @param id
     *            client id of the request
     * @param sourceId
     *            id of the storage from which the data was queried
     * @param startTs
     *            ms since epoch that represents the start of the oldQuery range
     * @param startTimeConfig
     *            Configuration specification that represents the start of the
     *            oldQuery range
     * @param endTs
     *            ms since epoch that represents the end of the oldQuery range
     * @param endTimeConfig
     *            Configuration specification that represents the end of the
     *            oldQuery range
     * @param returnset
     *            specifies if all results are returned or only those in the
     *            specified oldQuery range (this is needed because by default
     *            OpenTSDB may return results outside of the oldQuery range).
     * @param series
     *            specifies is the write will be as a series or in line. this is
     *            used for the header information and probably should be remove.
     * 
     * @throws IOException
     *             when an IO exception occurs writing to the output stream.
     */
    public void writeResults(JsonWriter writer,
            List<MetricSpecification> queries,
            Buckets<MetricKey, String> buckets, String id, String sourceId,
            long startTs, String startTimeConfig, long endTs, String endTimeConfig,
            ReturnSet returnset, boolean series) throws IOException;
}
