/*
 * Copyright (c) 2016, Zenoss and/or its affiliates. All rights reserved.
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

import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.impl.QueryStatus.QueryStatusEnum;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

class MetricSpecCallable implements Callable<OpenTSDBQueryResult> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(MetricSpecCallable.class);
    private final MetricSpecification mSpec;
    private final OpenTSDBClient client;
    private final String start;
    private final String end;

    public MetricSpecCallable(DefaultHttpClient httpClient, String start, String end, MetricSpecification mSpec, String queryURL) {
        this.start = start;
        this.end = end;
        client = new OpenTSDBClient(httpClient, queryURL);
        this.mSpec = mSpec;
    }

    @Override
    public OpenTSDBQueryResult call() {

        //convert spec to otsdbquery
        OpenTSDBQuery query = new OpenTSDBQuery();
        query.start = this.start;
        if (!Utils.NOW.equals(this.end)) {
            query.end = this.end;
        }

        query.addSubQuery(creatOTSDBSubQuery(mSpec));

        OpenTSDBQueryReturn queryResult = this.client.query(query);
        OpenTSDBQueryResult result;
        if (queryResult.getStatus().getStatus() != QueryStatusEnum.SUCCESS) {
            result = new OpenTSDBQueryResult();
            result.metric = query.queries.get(0).metric;
            result.tags = query.queries.get(0).tags;
            result.setStatus(queryResult.getStatus());
        } else {
            result = queryResult.getResults().get(0);
        }
        if (result != null) {
            result.metricSpecId = this.mSpec.getId();
            result.metricSpecName = this.mSpec.getNameOrMetric();
        }
        return result;
    }


    private static OpenTSDBSubQuery creatOTSDBSubQuery(MetricSpecification metricSpecification) {
        OpenTSDBSubQuery result = null;
        if (null != metricSpecification) {
            result = new OpenTSDBSubQuery();
            result.aggregator = metricSpecification.getAggregator();
            result.downsample = metricSpecification.getDownsample();

            // escape the name of the metric since OpenTSDB doesn't like spaces
            String metricName = metricSpecification.getMetric();
            metricName = metricName.replace(" ", OpenTSDBPMetricStorage.SPACE_REPLACEMENT);
            result.metric = metricName;


            result.rate = metricSpecification.getRate();
            result.rateOptions = new OpenTSDBRateOption(metricSpecification.getRateOptions());
            Map<String, List<String>> tags = metricSpecification.getTags();
            if (null != tags) {
                for (Map.Entry<String, List<String>> tagEntry : tags.entrySet()) {
                    for (String tagValue : tagEntry.getValue()) {
                        //apply metric-consumer sanitization to tags in query
                        result.addTag(Tags.sanitizeKey(tagEntry.getKey()), Tags.sanitizeValue(tagValue, false));
                    }
                }
            }
        }
        return result;
    }


}
