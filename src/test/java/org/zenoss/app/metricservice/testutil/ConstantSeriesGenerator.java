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

import java.util.HashMap;
import java.util.Map;

public class ConstantSeriesGenerator implements SeriesGenerator {
    private final double generatedValue;

    public ConstantSeriesGenerator(double i) {
        generatedValue = i;
    }

    @Override
    public Map<Long, Double> generateValues(long startTimestamp, long endTimestamp, long step) {
        validateTimeArguments(startTimestamp, endTimestamp, step);
        Map<Long, Double> result = new HashMap<>();
        for (long x = startTimestamp; x <= endTimestamp; x += step) {
            result.put(x, generatedValue);
        }
        return result;
    }

    private static void validateTimeArguments(long startTimestamp, long endTimestamp, long step) {
        if (0 == step) {
            throw new IllegalArgumentException("Step value must be nonzero");
        }
        if (Long.signum(endTimestamp - startTimestamp) != Long.signum(step)) {
            throw new IllegalArgumentException("Time specification will result in infinite loop.");
        }

    }
}
