package org.zenoss.app.metricservice.api.impl;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryResultTest {

    @Test
    public void testGetAndSetQueryStatus() throws Exception {
        QueryResult subject = new QueryResult();
        Assert.assertNull("QueryStatus of uninitialized QueryResult object should be null", subject.getQueryStatus());
        QueryStatus newQueryStatus = new QueryStatus();
        subject.setQueryStatus(newQueryStatus);
        Assert.assertEquals("QueryStatus of initialized QueryResult object should return the object used to initialize it.", newQueryStatus, subject.getQueryStatus());
    }

    @Test
    public void testGetAndSetId() throws Exception {
        QueryResult subject = new QueryResult();
        Assert.assertNull("Id of uninitialized QueryResult object should be null", subject.getId());
        String newId = new String("Test Id");
        subject.setId(newId);
        Assert.assertEquals("Id of initialized QueryResult object should return the object used to initialize it.", newId, subject.getId());
    }

    @Test
    public void testGetAndSetMetric() throws Exception {
        QueryResult subject = new QueryResult();
        Assert.assertNull("Metric of uninitialized QueryResult object should be null.", subject.getMetric());
        String newMetric = new String("Test Metric");
        subject.setMetric(newMetric);
        Assert.assertEquals("Metric of initialized QueryResult should return the string used to initialize it.", newMetric, subject.getMetric());
    }

    @Test
    public void testGetAndSetDatapoints() throws Exception {
        QueryResult subject = new QueryResult();
        Assert.assertNull("Datapoints of uninitialized QueryResult object should be null.", subject.getDatapoints());
        List<QueryResultDataPoint> newDatapoints = new ArrayList<>();
        subject.setDatapoints(newDatapoints);
        Assert.assertEquals("Datapoints of initialized QueryResult should return the string used to initialize it.", newDatapoints, subject.getDatapoints());
    }

    @Test
    public void testGetAndSetTags() throws Exception {
        QueryResult subject = new QueryResult();
        Assert.assertTrue("tags of uninitialized QueryResult object should be an empty collection", subject.getTags().isEmpty());
        Map<String,List<String>> newTags = new HashMap<>();
        subject.setTags(newTags);
        Assert.assertEquals("Tags of initialized QueryResult should return the string used to initialize it.", newTags, subject.getTags());
    }

    @Test
    public void testQueryResultCopyConstructor() {
        QueryResult subject = new QueryResult();
        subject.setMetric("Test Metric");
        subject.setId("Test ID");
        QueryStatus newQueryStatus = new QueryStatus();
        subject.setQueryStatus(newQueryStatus);
        List<QueryResultDataPoint> newDatapoints = new ArrayList<>();
        subject.setDatapoints(newDatapoints);
        Map<String, List<String>> newTags = new HashMap<>();
        subject.setTags(newTags);

        QueryResult clone = new QueryResult(subject);

        Assert.assertEquals("Metric of copied QueryResult should match original", subject.getMetric(), clone.getMetric());
        Assert.assertEquals("Id of copied QueryResult should match original", subject.getId(), clone.getId());
        Assert.assertEquals("QueryStatus of copied QueryResult should match original", subject.getQueryStatus(), clone.getQueryStatus());
        Assert.assertEquals("Datapoints of copied QueryResult should match original", subject.getDatapoints(), clone.getDatapoints());
        Assert.assertEquals("Tags of copied QueryResult should match original", subject.getTags(), clone.getTags());
    }

}