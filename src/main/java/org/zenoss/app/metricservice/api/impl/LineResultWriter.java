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
import org.zenoss.app.metricservice.buckets.Value;

/**
 * Writes the query results in a single line format where each line contains the
 * metric name and a value. This format is similar to the raw format returns
 * from the underlying query engine, but contains some redundant information.
 * <p>
 * This class will output the results sequentially by metric name
 * 
 * @author Zenoss 
 */
public class LineResultWriter extends BaseResultWriter {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.api.impl.BaseResultWriter#writeData(org.
     * zenoss.app.metricservice.api.impl.JsonWriter, java.util.List,
     * org.zenoss.app.metricsevice.buckets.Buckets,
     * org.zenoss.app.metricservice.api.model.ReturnSet, long, long)
     */
    @Override
    public void writeData(JsonWriter writer, List<MetricSpecification> queries,
            Buckets<MetricKey, String> buckets, ReturnSet returnset,
            long startTs, long endTs) throws IOException {

        List<Long> timestamps = buckets.getTimestamps();
        Buckets<MetricKey, String>.Bucket bucket;
        Value value;
        String name;
        boolean comma = false;
        long downsample = buckets.getDownsample();

        /*
         * Iterate over the queries and for each each metric or value output its
         * values. This means that we are going to have to take multple trips
         * over all buckets, but it is either that or use lots of memory.
         */
        for (MetricSpecification query : queries) {
            // Only return the result if the query is meant to be emitted.
            if (query.getEmit()) {
                for (long bts : timestamps) {
                    bts *= downsample;
                    if ((returnset == ReturnSet.ALL || (bts >= startTs && bts <= endTs))
                            && (bucket = buckets.getBucket(bts)) != null) {
                        if ((value = bucket.getValueByShortcut((name = query
                                .getNameOrMetric()))) != null) {
                            if (comma) {
                                writer.write(',');
                            }
                            comma = true;
                            // bucket.getKeyByName(query.getNameOrMetric());
                            writer.objectS()
                                    .value(MetricService.METRIC, name, true)
                                    .value(MetricService.TIMESTAMP, bts, true)
                                    .value(MetricService.VALUE,
                                            value.getValue(), false);
                            writer.objectE();
                        }
                    }
                }
            }
        }
    }
}
