package org.zenoss.app.metricservice.api.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.Utils;

import java.util.*;

public class PerformanceQueryTest {

    private static final String START_STRING = "Start String";
    private static final String END_STRING = "End String";
    private static final List<MetricSpecification> TEST_METRICS = new ArrayList<>();
    private static final Map<String, List<String>> TEST_TAGS = new HashMap<>();
    private static final String DOWNSAMPLE_STRING = "Test Downsample";
    private static final double DOWNSAMPLE_MULTIPLIER = 42.0;
    private static final double EPSILON = 0.000001;
    private static final ReturnSet TEST_RETURNSET = ReturnSet.EXACT;

    private PerformanceQuery subject;

    @Before
    public void setUp() {
        subject = new PerformanceQuery();
        TEST_METRICS.add(MetricSpecification.fromString("sum:10s-ago:rate:laLoadInt{tag2=thing|other thing,tag3=bar,tag1=value1|value2|value3}"));
        TEST_TAGS.put("TEST_TAG", Arrays.asList("test value 1", "test value 2", "test value 3"));
    }

    @Test
    public void testGetAndSetStart() throws Exception {
        Assert.assertEquals("getStart() on uninitialized PerformanceQuery should return default value.", Utils.DEFAULT_START_TIME, subject.getStart());
        subject.setStart(START_STRING);
        Assert.assertEquals("getStart() on initialized PerformanceQuery should return value passed in.", START_STRING, subject.getStart());
    }

    @Test
    public void testGetAndSetEnd() throws Exception {
        Assert.assertEquals("getEnd() on uninitialized PerformanceQuery should return default value.", Utils.DEFAULT_END_TIME, subject.getEnd());
        subject.setEnd(END_STRING);
        Assert.assertEquals("getEnd() on initialized PerformanceQuery should return value passed in.", END_STRING, subject.getEnd());
    }

    @Test
    public void testGetAndSetSeries() throws Exception {
        Assert.assertNull("getSeries() on uninitialized PerformanceQuery should return null.", subject.getSeries());
        subject.setSeries(true);
        Assert.assertTrue("getSeries() on initialized PerformanceQuery should return value passed in.", subject.getSeries());
    }


    @Test
    public void testGetAndSetMetrics() throws Exception {
        Assert.assertTrue("getMetrics() on uninitialized PerformanceQuery should return empty array.", null != subject.getMetrics() && subject.getMetrics().isEmpty());
        subject.setMetrics(TEST_METRICS);
        Assert.assertEquals("getMetrics() on initialized PerformanceQuery should return value passed in.", TEST_METRICS, subject.getMetrics());
    }

    @Test
    public void testGetAndSetTags() throws Exception {
        Assert.assertNull("getTags() on uninitialized PerformanceQuery should return null.", subject.getTags());
        subject.setTags(TEST_TAGS);
        Assert.assertEquals("getTags() on initialized PerformanceQuery should return value passed in.", TEST_TAGS, subject.getTags());
    }

    @Test
    public void testGetAndSetDownsample() throws Exception {
        Assert.assertNull("getDownsample() on uninitialized PerformanceQuery should return null.", subject.getDownsample());
        subject.setDownsample(DOWNSAMPLE_STRING);
        Assert.assertEquals("getDownsample() on initialized PerformanceQuery should return value passed in.", DOWNSAMPLE_STRING, subject.getDownsample());
    }

    @Test
    public void testGetAndSetDownsampleMultiplier() throws Exception {
        Assert.assertEquals("getDownsampleMultiplier() on uninitialized PerformanceQuery should return null.", Utils.DEFAULT_DOWNSAMPLE_MULTIPLIER, subject.getDownsampleMultiplier(), EPSILON);
        subject.setDownsampleMultiplier(DOWNSAMPLE_MULTIPLIER);
        Assert.assertEquals("getDownsampleMultiplier() on initialized PerformanceQuery should always return default.", Utils.DEFAULT_DOWNSAMPLE_MULTIPLIER, subject.getDownsampleMultiplier(), EPSILON);
    }

    @Test
    public void testGetAndSetReturnset() throws Exception {
        Assert.assertNull("getReturnset() on uninitialized PerformanceQuery should return null.", subject.getReturnset());
        subject.setReturnset(TEST_RETURNSET);
        Assert.assertEquals("getReturnset() on initialized PerformanceQuery should return value passed in.", TEST_RETURNSET, subject.getReturnset());
    }
}
