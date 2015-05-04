package org.zenoss.app.metricservice.api.impl;

import org.junit.Assert;
import org.junit.Test;

public class QueryResultDataPointTest {

    private static final double EPSILON = 0.000000000000001;
    private final long initialTime = 1423522480;
    private final double initialValue = 3.1415926535;

    @Test
    public void testGetTimestamp() throws Exception {
        QueryResultDataPoint subject = new QueryResultDataPoint();
        Assert.assertEquals("Timestamp of uninitialized QueryResultDataPoint should be zero.", subject.getTimestamp(), 0);
        QueryResultDataPoint initializedSubject = new QueryResultDataPoint(initialTime, initialValue);
        Assert.assertEquals("Timestamp of initialized QueryResultDataPoint should match given value.", initializedSubject.getTimestamp(), initialTime);
    }

    @Test
    public void testSetTimestamp() throws Exception {
        QueryResultDataPoint subject = new QueryResultDataPoint();
        Assert.assertEquals("Timestamp of uninitialized QueryResultDataPoint should be zero.", subject.getTimestamp(), 0);
        subject.setTimestamp(initialTime);
        Assert.assertEquals("Timestamp of initialized QueryResultDataPoint should match given value.", subject.getTimestamp(), initialTime);
    }

    @Test
    public void testGetValue() throws Exception {
        QueryResultDataPoint subject = new QueryResultDataPoint();
        Assert.assertEquals("Value of uninitialized QueryResultDataPoint should be zero.", subject.getValue(), 0.0, EPSILON);
        QueryResultDataPoint initializedSubject = new QueryResultDataPoint(initialTime, initialValue);
        Assert.assertEquals("Value of initialized QueryResultDataPoint should match given value.", initializedSubject.getValue(), initialValue, EPSILON);
    }

    @Test
    public void testSetValue() throws Exception {
        QueryResultDataPoint subject = new QueryResultDataPoint();
        Assert.assertEquals("Value of uninitialized QueryResultDataPoint should be zero.", subject.getValue(), 0.0, EPSILON);
        subject.setValue(initialValue);
        Assert.assertEquals("Value of initialized QueryResultDataPoint should match given value.", subject.getValue(), initialValue, EPSILON);
    }
}