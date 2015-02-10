package org.zenoss.app.metricservice.api.impl;

import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.util.ArrayList;
import java.util.Collection;

public class SeriesQueryResultTest {

    @Test
    public void testGetAndSetClientId() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertNull("getClientId on Uninitialized SeriesQueryResult should return null.", subject.getClientId());
        String testId = "Test Client ID";
        subject.setClientId(testId);
        Assert.assertEquals("After setClientId, getClientId should return string passed in.", testId, subject.getClientId());
    }

    @Test
    public void testGetAndSetEndTime() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertNull("getEndTime on Uninitialized SeriesQueryResult should return null.", subject.getEndTime());
        String testEndTime = "Test End Time";
        subject.setEndTime(testEndTime);
        Assert.assertEquals("After setEndTime, getEndTime should return string passed in.", testEndTime, subject.getEndTime());
    }

    @Test
    public void testGetAndSetEndTimeActual() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertEquals("getEndTimeActual on Uninitialized SeriesQueryResult should return zero.", 0, subject.getEndTimeActual());
        long testEndTimeActual = 1423522480;
        subject.setEndTimeActual(testEndTimeActual);
        Assert.assertEquals("After setEndTimeActual, getEndTimeActual should return string passed in.", testEndTimeActual, subject.getEndTimeActual());
    }

    @Test
    public void testGetAndSetReturnset() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertNull("getReturnset on Uninitialized SeriesQueryResult should return null.", subject.getReturnset());
        ReturnSet testReturnSet = ReturnSet.EXACT;
        subject.setReturnset(testReturnSet);
        Assert.assertEquals("After setReturnset, getReturnset should return string passed in.", testReturnSet, subject.getReturnset());
    }

    @Test
    public void testGetAndSetSeries() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertFalse("isSeries on Uninitialized SeriesQueryResult should return false.", subject.isSeries());
        boolean testSeries = true;
        subject.setSeries(testSeries);
        Assert.assertEquals("After setSeries, isSeries should return value passed in.", testSeries, subject.isSeries());
    }

    @Test
    public void testGetAndSetSource() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertNull("getSource on Uninitialized SeriesQueryResult should return null.", subject.getSource());
        String testSource = "Test Source";
        subject.setSource(testSource);
        Assert.assertEquals("After setSource, getSource should return string passed in.", testSource, subject.getSource());
    }


    @Test
    public void testGetAndSetStartTime() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertNull("getStartTime on Uninitialized SeriesQueryResult should return null.", subject.getStartTime());
        String testStartTime = "Test End Time";
        subject.setStartTime(testStartTime);
        Assert.assertEquals("After setStartTime, getStartTime should return string passed in.", testStartTime, subject.getStartTime());
    }

    @Test
    public void testGetAndSetStartTimeActual() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertEquals("getStartTimeActual on Uninitialized SeriesQueryResult should return zero.", 0, subject.getStartTimeActual());
        long testStartTimeActual = 1423522480;
        subject.setStartTimeActual(testStartTimeActual);
        Assert.assertEquals("After setStartTimeActual, getStartTimeActual should return string passed in.", testStartTimeActual, subject.getStartTimeActual());
    }

    @Test
    public void testGetAndAddResults() throws Exception {
        SeriesQueryResult subject = new SeriesQueryResult();
        Assert.assertTrue("getResults on Uninitialized SeriesQueryResult should return empty collection.", subject.getResults().isEmpty());
        Collection<QueryResult> testResults = new ArrayList<>();
        QueryResult testQueryResult = new QueryResult();
        testQueryResult.setMetric("Test Metric");
        testQueryResult.setQueryStatus(new QueryStatus(QueryStatus.QueryStatusEnum.WARNING, "Test Query Status Message"));
        testResults.add(testQueryResult);
        subject.addResults(testResults);
        Assert.assertEquals("After addResults, getResults should return object passed in.", testResults, subject.getResults());
    }

    @Test
    public void testCopyConstructor() {
        Assert.fail("TBD: Implement copy constructor test.");
    }
}