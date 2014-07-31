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

import static org.junit.Assert.assertEquals;
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
        assertEquals("Value with one entry should return that entry for getValue", val1, victim.getValue(), EPSILON);
        victim.add(val2);
        double expectedResult = (val1 + val2) / 2.0;
        assertEquals(String.format("Value with two entries should return the average of the entries for getValue (expected = %f, actual = %f",
            expectedResult, victim.getValue()), expectedResult,victim.getValue(), EPSILON);
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
        assertEquals(sum,victim.getSum(), EPSILON);
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
        assertEquals(sum,victim.getSum(), EPSILON);

    }

    @Test
    public void uninitializedValueShouldReturnNaNOnGet() {
        Value victim = new Value();
        assertTrue("An Uninitialized Value should return NaN on getValue().", Double.isNaN(victim.getValue()));
    }

    @Test
    public void uninitializedValueShouldReturnFalseForValueIsInterpolated() {
        Value victim = new Value();
        assertEquals("An Uninitialized Value should return false on valueIsInterpolated().", false, victim.valueIsInterpolated());
    }

    @Test
    public void valueWithOnlyInterpolatedDataShouldReturnTrueForValueIsInterpolated() {
        Value victim = new Value();
        victim.addInterpolated(1.234);
        assertEquals("A Value with only interpolated data should return true on valueIsInterpolated().", true, victim.valueIsInterpolated());
    }

    @Test
    public void valueWithInterpolatedAndRealDataShouldReturnTrueForValueIsInterpolated() {
        Value victim = new Value();
        victim.addInterpolated(1.234);
        victim.add(5.678);
        assertEquals("A Value with interpolated and real data should return true on valueIsInterpolated().", false, victim.valueIsInterpolated());
    }

    @Test
    public void valueWithInterpolatedAndRealDataShouldReturnRealDataOnGetValue() {
        Double realValue = 5.678;
        Double interpolatedValue = 1.234;
        Value victim = new Value();
        victim.addInterpolated(interpolatedValue);
        victim.add(realValue);
        assertEquals("A Value with interpolated and real data should return the real value on getValue().", realValue, victim.getValue(), EPSILON);
    }

    @Test
    public void valueWithRealDataAddedThenRemovedShouldReturnInterpolatedDataOnGetValue() {
        Double realValue = 5.678;
        Double interpolatedValue = 1.234;
        Value victim = new Value();
        victim.addInterpolated(interpolatedValue);
        victim.add(realValue);
        victim.remove(realValue);
        assertEquals("A Value with an interpolated data and real data added then removed should return the interpolated value on getValue().", interpolatedValue, victim.getValue(), EPSILON);
    }

    @Test
    public void valueWithOnlyInterpolatedDataShouldReturnInterpolatedDataOnGetValue() {
        Double realValue = 5.678;
        Double interpolatedValue = 1.234;
        Value victim = new Value();
        victim.addInterpolated(interpolatedValue);
        assertEquals("A Value with only interpolated data should return the interpolated value on getValue().", interpolatedValue, victim.getValue(), EPSILON);
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
        assertEquals("Appended sum should match.", appendedSum, victim.getSum(), EPSILON);
        assertEquals("Appended value (average) should match", appendedAverage, victim.getValue(), EPSILON);
        assertTrue("Appended count should match.", appendedCount == victim.getCount());
        for (Double value : newValues) {
            victim.remove(value);
        }
        assertEquals("After remove, value should equal original value.", originalValue, victim.getValue(), EPSILON);
        assertEquals("After remove, sum should equal original sum.", originalSum,victim.getSum(), EPSILON);
        assertTrue("After remove, count should equal original count.", originalCount == victim.getCount());
    }

    @Test
    public void valueWithAddedThenRemovedShouldReturnNaN() {
        double testValue = 1.2345;
        Value victim = new Value();
        victim.add(testValue);
        victim.remove(testValue);
        assertTrue("Adding then removing the same value should result in NaN.", Double.isNaN(victim.getValue()));
    }
}
