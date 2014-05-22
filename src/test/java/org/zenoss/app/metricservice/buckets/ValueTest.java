package org.zenoss.app.metricservice.buckets;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/19/14
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
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
