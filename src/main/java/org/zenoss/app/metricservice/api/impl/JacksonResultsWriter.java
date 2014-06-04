/*
 * Copyright (c) 2014, Zenoss and/or its affiliates. All rights reserved.
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

import org.zenoss.app.metricservice.api.metric.impl.MetricService;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import java.io.IOException;
import java.util.List;

                                                                                             OT
public class JacksonResultsWriter {

    public void writeResults(JacksonWriter writer, List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset, boolean series) throws Exception {
        if (series) {
            SeriesQueryResult results = makeResults(queries, buckets, id, sourceId, startTs, startTimeConfig, endTs, endTimeConfig, returnset);
            // write results (JSON serialization)
            writer.write(Utils.jsonStringFromObject(results));
        } else {
            //TODO: make a new exception type for this and throw it. Determine whether it needs to be a WebException or not.
            throw new Exception("non-series data is no longer supported.");
        }
    }

    private SeriesQueryResult makeResults(List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset) {
        SeriesQueryResult result = new SeriesQueryResult();
        result.setId(id);
        result.setClientId(MetricService.CLIENT_ID);
        result.setEndTime(endTimeConfig);
        result.setEndTimeActual(endTs);
        result.setReturnset(returnset);
        result.setSeries(true);
        result.setSource(sourceId);
        result.setStartTime(startTimeConfig);
        result.setStartTimeActual(startTs);
        return result;
    }
}
