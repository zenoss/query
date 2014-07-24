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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class QueryResult {

    public QueryResult() {}

    public QueryResult(QueryResult other) {
        this.id = other.id;
        this.metric = other.metric;
        this.datapoints = new ArrayList<>(other.datapoints.size());
        Collections.copy(datapoints, other.datapoints);
    }

    private List<QueryResultDataPoint> datapoints;
    private String metric;
    private final Multimap<String, String> tags = HashMultimap.create();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String id;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public List<QueryResultDataPoint> getDatapoints() {
        return datapoints;
    }

    public void setDatapoints(List<QueryResultDataPoint> datapoints) {
        this.datapoints = datapoints;
    }

    public void setTags(Map<String, List<String>> newTags) {
        tags.clear();
        for (Map.Entry<String, List<String>> entry : newTags.entrySet()) {
            tags.putAll(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Collection<String>> getTags() {
        return tags.asMap();
    }
}