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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class OpenTSDBQueryResult {
    public List<String> aggregateTags;

    public SortedMap<Long,Double> dps = new TreeMap<>();
    public String metric;
    public Map<String, String> tags;
    public List<String> tsuids = new ArrayList<>();

    public QueryStatus getStatus() {
        if (null == status) {
            status = new QueryStatus();
        }
        return status;
    }

    public void setStatus(QueryStatus status) {
        this.status = status;
    }

    private QueryStatus status;

    public String debugString() {
        return Objects.toStringHelper(getClass())
                .add("aggregateTags", aggregateTags)
                .add("dps", dps)
                .add("metric", metric)
                .add("tags", tags)
                .add("tsuids", tsuids)
                .toString();
    }

    public void addTags(Map<String, List<String>> tagsToAdd) {
        if (null == tags) {
            tags = new HashMap<>();
        }
        for (Map.Entry<String, List<String>> entry : tagsToAdd.entrySet()) {
            tags.put(entry.getKey(), entry.getValue().get(0));
        }
    }

    public void addDataPoint(long i, double pointValue) {
        if (null == dps) {
            dps = new TreeMap<>();
        }
        dps.put(i, pointValue);
    }

    @JsonIgnore
    public SortedMap<Long, Double> getDataPoints() {
        if (null == dps) {
            dps = new TreeMap<>();
        }
        return dps;
    }

    public void setDataPoints(SortedMap<Long, Double> dps) {
        this.dps = dps;
    }
}
