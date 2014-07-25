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
package org.zenoss.app.metricservice.testutil;

import org.zenoss.app.metricservice.api.impl.OpenTSDBQueryResult;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class DataReaderGenerator {
    private Collection<OpenTSDBQueryResult> results = new ArrayList<OpenTSDBQueryResult>();

    public BufferedReader makeReader() {
        // generate reader that spits out JSON for an array of OpenTSDBQueryResult
        String resultString = Utils.jsonStringFromObject(results);
        StringReader sr = new StringReader(resultString);
        return new BufferedReader(sr);
    }

    public void addSeries(MetricSpecification specification, SeriesGenerator dataGen, long start, long end, long step) {
        results.add(makeQueryResult(specification, dataGen, start, end, step));
    }

    private OpenTSDBQueryResult makeQueryResult(MetricSpecification specification, SeriesGenerator dataGen, long start, long end, long step) {
        OpenTSDBQueryResult result = new OpenTSDBQueryResult();
        result.addTags(specification.getTags());
        Map<Long, Double> generatedValues = dataGen.generateValues(start, end, step);
        SortedMap<Long, String> dataPoints = new TreeMap<>();
        for (Map.Entry<Long, Double> entry : generatedValues.entrySet()) {
            dataPoints.put(entry.getKey(), entry.getValue().toString());
        }
        result.setDataPoints(dataPoints);
        result.metric = specification.getNameOrMetric();
        return result;
    }
}
