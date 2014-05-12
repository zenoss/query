package org.zenoss.app.metricservice.api.impl;

import com.google.common.base.Objects;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/12/14
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class MetricKeyTest {
    private static final String basicMetric = "Test Metric";
    private static final String basicName = "Test Name";
    private static final String basicTag = "Key=Value";
    private static final String basicTagWithDifferentKey = "Key1=Value";
    private static final String basicTagWithDifferentValue = "Key=Value1";
    private static final Tags basicTagValue = Tags.fromValue(basicTag);
    private static final String basicMetricSpecificationString = String.format("avg:false:%s{%s}", basicMetric, basicTag);

    @Test
    public void testEquals() throws Exception {
        MetricKey victim = new MetricKey();
        MetricKey other = new MetricKey();
        assertTrue("Two newly created MetricKeys should be equal.", victim.equals(other));
        assertFalse("Equal comparison with null should return false.", victim.equals(null));
        assertFalse("Equal comparison with different class type should return false.", victim.equals(Integer.valueOf(3)));
    }

    @Test public void testDifferentMetricNamesShouldNotBeEqual() {
        MetricKey victim = makeBasicMetricKey();
        MetricKey other = MetricKey.fromValue(basicName + "modified", basicMetric, basicTag);
        assertFalse("MetricKeys differing only in metric name should not be equal.", victim.equals(other));
    }

    private MetricKey makeBasicMetricKey() {
        return MetricKey.fromValue(basicName, basicMetric, basicTag);
    }

    @Test public void testDifferentMetricKeyNamesShouldNotBeEqual() {
        MetricKey victim = makeBasicMetricKey();
        MetricKey other = MetricKey.fromValue(basicName, basicMetric + "modified", basicTag);
        assertFalse("MetricKeys differing only in name should not be equal.", victim.equals(other));
    }

    @Test public void testDifferentTagsShouldNotBeEqual() {
        MetricKey victim = makeBasicMetricKey();
        MetricKey other = MetricKey.fromValue(basicName, basicMetric, basicTagWithDifferentValue);
        assertFalse("MetricKeys differing only in tags should not be equal.", victim.equals(other));
        other = MetricKey.fromValue(basicName, basicMetric, basicTagWithDifferentKey);
        assertFalse("MetricKeys differing only in tags should not be equal.", victim.equals(other));
    }

    @Test public void testTwoMetricKeyObjectsWithSameValuesShouldBeEqual() {
        MetricKey victim = MetricKey.fromValue("testName", "testMetric", "TestTags=1");
        MetricKey other = MetricKey.fromValue("testName", "testMetric", "TestTags=1");
        assertTrue("MetricKeys with identical members should be equal.", victim.equals(other));
    }


    @Test
    public void testGetMetric() throws Exception {
        MetricKey victim = makeBasicMetricKey();
        assertTrue("getMetric should return the metric value passed in.", Objects.equal(basicMetric, victim.getMetric()));
    }

    @Test
    public void testGetTags() throws Exception {
        String tagsValue = "Tag1=value1";
        MetricKey victim = makeBasicMetricKey();
        assertTrue("getTags should return the tag value passed in.", Objects.equal(basicTagValue, victim.getTags()));
    }

    @Test
    public void testGetName() throws Exception {
        MetricKey victim = makeBasicMetricKey();
        assertTrue("getName should return the metric value passed in.", Objects.equal(basicName, victim.getName()));
    }

    @Test
    public void testFromValue() throws Exception {
        MetricKey victim = MetricKey.fromValue(basicName, basicMetric, basicTag);
        assertTrue("Name was not same as passed to fromValue().", Objects.equal(victim.getName(), basicName));
        assertTrue("MetricName was not same as passed to fromValue().", Objects.equal(victim.getMetric(), basicMetric));
        assertTrue("Tags value was not same as passed to fromValue().", Objects.equal(victim.getTags(), basicTagValue));
    }

    @Test
    public void testFromValue2() throws Exception {

        MetricSpecification basicMetricSpecification = getMetricSpecification();
        MetricKey victim = MetricKey.fromValue(basicMetricSpecification);
        MetricKey other = makeBasicMetricKey();
        assertTrue("FromValue with metricSpecification did not match fromValue from strings.", victim.equals(other));
    }

    private MetricSpecification getMetricSpecification() {
        MetricSpecification basicMetricSpecification = MetricSpecification.fromString(basicMetricSpecificationString);
        basicMetricSpecification.setName(basicName);
        return basicMetricSpecification;
    }

    @Test
    public void testToString() throws Exception {

    }
}
