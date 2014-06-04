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

import static org.junit.Assert.assertTrue;

public class ValueTest {
    private static final double EPSILON = 0.0000000000001;

    @Test
    public void testGetValue() {
        Value victim = new Value();
        double test = victim.getValue();
        assertTrue("getValue() on an uninitialized value should return NaN.", Double.isNaN(test));
        double val1 = 1.1, val2 = 2.0;
        victim.add(val1);
        assertTrue("Value with one entry should return that entry for getValue", val1 == victim.getValue());
        victim.add(val2);
        double expectedResult = (val1 + val2) / 2.0;
        assertTrue(String.format("Value with two entries should return the average of the entries for getValue (expected = %f, actual = %f",
            expectedResult, victim.getValue()), expectedResult == victim.getValue());
    }

    @Test
    public void testGetSum() throws Exception {
        Value victim = new Value();
        Double[] testValues = new Double[]{1.0, 1.1, 2.0, -23.4, 0.0, 1.0, 2.1};
        double sum = 0.0;
        for (Double value : testValues) {
            victim.add(value);
            sum += value;
        }
        assertTrue(sum == victim.getSum());
    }

    @Test
    public void testGetCount() throws Exception {
        Value victim = new Value();
        Double[] testValues = new Double[]{1.0, 1.1, 2.0, 2.1};
        for (Double value : testValues) {
            victim.add(value);
        }
        assertTrue(testValues.length == victim.getCount());

    }

    @Test
    public void testAdd() throws Exception {
        Value victim = new Value();
        Double[] testValues = new Double[]{1.0, -1.0, 2.0, 2.1, 0.0, 53.0, -27.5};
        double sum = 0.0;
        for (Double value : testValues) {
            victim.add(value);
            sum += value;
        }
        assertTrue(sum == victim.getSum());

    }

    @Test
    public void testRemove() throws Exception {
        Value victim = new Value();
        Double[] testValues = new Double[]{1.0, -1.0, 2.0, 2.1, 0.0, 53.0, -27.5};
        for (Double value : testValues) {
            victim.add(value);
        }
        double originalValue = victim.getValue();
        double originalSum = victim.getSum();
        long  originalCount = victim.getCount();

        Double[] newValues = new Double[] {2.1, -27.0, 2.1, 0.0 };
        double newValueSum = 0.0;
        long newValueCount = newValues.length;
        for (Double value : newValues) {
            newValueSum += value;
            victim.add(value);
        }
        double appendedSum = originalSum + newValueSum;
        long appendedCount = originalCount + newValueCount;
        double appendedAverage = appendedSum / appendedCount;
        assertTrue(String.format("Appended sum should match. expected %f, got %f", appendedSum, victim.getSum()), isPrettyMuchEqual(appendedSum,victim.getSum()));
        assertTrue("Appended value (average) should match", isPrettyMuchEqual(appendedAverage, victim.getValue()));
        assertTrue("Appended count should match.", appendedCount == victim.getCount());
        for (Double value : newValues) {
            victim.remove(value);
        }
        assertTrue("After remove, value should equal original value.", isPrettyMuchEqual(originalValue, victim.getValue()));
        assertTrue("After remove, sum should equal original sum.", isPrettyMuchEqual(originalSum,victim.getSum()));
        assertTrue("After remove, count should equal original count.", originalCount == victim.getCount());
    }

    private boolean isPrettyMuchEqual(double originalValue, double value) {
        return (Math.abs(originalValue - value) < EPSILON);
    }
}
