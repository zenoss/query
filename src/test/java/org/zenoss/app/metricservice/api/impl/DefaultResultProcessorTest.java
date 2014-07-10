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

import org.junit.Ignore;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.buckets.Value;
import org.zenoss.app.metricservice.calculators.Closure;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DefaultResultProcessorTest {

    private static final double EPSILON = 0.00000001;

    @Test
    public void testLookup() throws Exception {
        Closure closure = mock(Closure.class);
        Value myValue = new Value();
        myValue.add(1.0);
        when(closure.getValueByShortcut("name")).thenReturn(myValue);

        DefaultResultProcessor victim = new DefaultResultProcessor();
        double foundValue = victim.lookup("name", closure);
        assertEquals("lookup should return correct value for series.", myValue.getValue(), foundValue, EPSILON);
    }

    @Test
    @Ignore
    public void testProcessResults() throws Exception {
        DefaultResultProcessor victim = new DefaultResultProcessor();
        BufferedReader reader = null;
        List<MetricSpecification> queries = makeQueries();
        long bucketSize = 60;
        victim.processResults(reader, queries, bucketSize);
    }

    private List<MetricSpecification> makeQueries() {
        List<MetricSpecification> result = new ArrayList<>();
        String[] specifications = {"foo", "bar", "baz"};
        for (String specification : specifications) {
            result.add(MetricSpecification.fromString(specification));
        }
        return result;
    }
}
