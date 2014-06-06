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
