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

package org.zenoss.app.metricservice.buckets;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NewValueTest {
    private static final Logger log = LoggerFactory.getLogger(NewValueTest.class);
    private static final double EPSILON = 0.0000000000001;

    @Test
    public void testGetMean() {
        NewValue victim = new NewValue();
        double test = victim.getMean();
        assertTrue("getMean() on an uninitialized value should return NaN.", Double.isNaN(test));
        double val1 = 1.1, val2 = 2.0;
        victim.push(val1);
        assertEquals("Value with one entry should return that entry for getMean", val1, victim.getMean(), EPSILON);
        victim.push(val2);
        double expectedResult = (val1 + val2) / 2.0;
        assertEquals("Value with two entries should return the average of the entries for getMean", expectedResult,victim.getMean(), EPSILON);
        Double [] testValues = new Double[] {1.0, -1.0, 2.0, 2.1, 0.0, 53.0, -27.5};
        victim.clear();
        for (double value : testValues) {
            victim.push(value);
        }
        double mean = getMean(testValues);
        assertEquals("Calculated variance should match variance from test object.", mean, victim.getMean(), EPSILON);

    }

    @Test
    public void testGetSum() throws Exception {
        NewValue victim = new NewValue();
        Double[] testValues = new Double[]{1.0, 1.1, 2.0, -23.4, 0.0, 1.0, 2.1};
        double sum = 0.0;
        for (Double value : testValues) {
            victim.push(value);
            sum += value;
        }
        assertEquals(sum,victim.getSum(), EPSILON);
    }

    @Test
    public void testGetCount() throws Exception {
        NewValue victim = new NewValue();
        Double[] testValues = new Double[]{1.0, 1.1, 2.0, 2.1};
        for (Double value : testValues) {
            victim.push(value);
        }
        assertTrue(testValues.length == victim.getNumDataValues());

    }

    @Test
    public void testAdd() throws Exception {
        NewValue victim = new NewValue();
        Double[] testValues = new Double[]{1.0, -1.0, 2.0, 2.1, 0.0, 53.0, -27.5};
        double sum = 0.0;
        for (Double value : testValues) {
            victim.push(value);
            sum += value;
        }
        assertEquals("Calculated sum should match sum from test object.", sum,victim.getSum(), EPSILON);

    }

    @Test
    public void testGetVariance() {
        NewValue victim = new NewValue();
        Double[] testValues = new Double[]{1.0, -1.0, 2.0, 2.1, 0.0, 53.0, -27.5};
        for (double value : testValues) {
            victim.push(value);
        }
        double variance = getVariance(testValues);
        assertEquals("Calculated variance should match variance from test object.", variance, victim.getVariance(), EPSILON);
        assertEquals("Calculated standard deviation should match standard deviation from test object.", Math.sqrt(variance), victim.getStandardDeviation(), EPSILON);
    }

    private double getVariance(Double[] testValues) {
        double mean = getMean(testValues);
        double temp = 0.0;
        for (double a : testValues) {
            double delta = mean - a;
            temp += (delta * delta);
        }
        double result = temp / (testValues.length - 1);
        log.info("getVariance: returning {}", result);
        return result;
    }

    private double getMean(Double[] testValues) {
        double sum = 0;
        for (double a : testValues) {
            sum += a;
        }
        double result =  sum / testValues.length;
        log.info("getMean: returning {}", result);
        return result;
    }

}
