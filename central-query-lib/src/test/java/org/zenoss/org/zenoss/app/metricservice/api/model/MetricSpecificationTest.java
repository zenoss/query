/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
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
package org.zenoss.org.zenoss.app.metricservice.api.model;

import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.MetricSpecification;

import java.util.*;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 *
 */
public class MetricSpecificationTest {

    private MetricSpecification test(String source, String expected) {
        MetricSpecification mq = MetricSpecification.fromString(source);

        int idx = source.indexOf('{');
        if (idx >= 0) {
            // has tags
            String base = mq.toString();
            Assert.assertEquals(expected, base.substring(0, base.indexOf('{')));

            Map<String, List<String>> tags = mq.getTags();
            Assert.assertNotNull(tags);
            Assert.assertEquals(2, tags.size());
            Assert.assertEquals("value1", tags.get("tag1").get(0));
            Assert.assertEquals("value2", tags.get("tag2").get(0));

        } else {
            Assert.assertEquals(expected, mq.toString());
            Map<String, List<String>> tags = mq.getTags();
            Assert.assertNotNull(tags);
            Assert.assertEquals(0, tags.size());
        }
        return mq;
    }

    @Test
    public void fullAvgTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("avg:rate:10s-ago:laLoadInt",
            "avg:10s-ago:rate:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullSumTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("sum:rate:10s-ago:laLoadInt",
            "sum:10s-ago:rate:laLoadInt");
        Assert.assertEquals("sum", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullMinTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("min:rate:10s-ago:laLoadInt",
            "min:10s-ago:rate:laLoadInt");
        Assert.assertEquals("min", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullMaxTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("max:rate:10s-ago:laLoadInt",
            "max:10s-ago:rate:laLoadInt");
        Assert.assertEquals("max", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void noRateTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("avg:10s-ago:laLoadInt",
            "avg:10s-ago:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(false, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void noDownsampleTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("avg:rate:laLoadInt",
            "avg:rate:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertNull(mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullDefaultTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("laLoadInt", "avg:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(false, mq.getRate());
        Assert.assertNull(mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullAvgWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "avg:rate:10s-ago:laLoadInt{tag1=value1,tag2=value2}",
            "avg:10s-ago:rate:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullSumWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "sum:rate:10s-ago:laLoadInt{tag1=value1,tag2=value2}",
            "sum:10s-ago:rate:laLoadInt");
        Assert.assertEquals("sum", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullMinWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "min:rate:10s-ago:laLoadInt{tag1=value1,tag2=value2}",
            "min:10s-ago:rate:laLoadInt");
        Assert.assertEquals("min", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullMaxWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "max:rate:10s-ago:laLoadInt{tag1=value1,tag2=value2}",
            "max:10s-ago:rate:laLoadInt");
        Assert.assertEquals("max", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void noRateWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "avg:10s-ago:laLoadInt{tag1=value1,tag2=value2}",
            "avg:10s-ago:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(false, mq.getRate());
        Assert.assertEquals("10s-ago", mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void noDownsampleWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test(
            "avg:rate:laLoadInt{tag1=value1,tag2=value2}",
            "avg:rate:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(true, mq.getRate());
        Assert.assertNull(mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void fullDefaultWithTagsTest() {
        // The to result will be different from the input as the toString
        // method always outputs the downsample and then the rate
        MetricSpecification mq = test("laLoadInt{tag1=value1,tag2=value2}",
            "avg:laLoadInt");
        Assert.assertEquals("avg", mq.getAggregator().toString());
        Assert.assertEquals(false, mq.getRate());
        Assert.assertNull(mq.getDownsample());
        Assert.assertEquals("laLoadInt", mq.getMetric());
    }

    @Test
    public void downsampleFirstTest() {
        test("avg:10s-avg:rate:laLoadInt{tag1=value1,tag2=value2}",
            "avg:10s-avg:rate:laLoadInt");
    }

    @Test
    public void tagMergeTest() {
        MetricSpecification testSubject = new MetricSpecification();
        Map<String, List<String>> initialTags = new HashMap<>();
        initialTags.put("key1",new ArrayList<>(Arrays.asList("k1v1","k1v2","k1v3")));
        initialTags.put("key2",new ArrayList<>(Arrays.asList("k2v1","k2v2","k2v3")));
        testSubject.setTags(initialTags);
        Map<String, List<String>> additionalTags = new HashMap<>();
        additionalTags.put("key2", new ArrayList<>(Arrays.asList("k2v3", "k2v4", "k2v5")));
        additionalTags.put("key3", new ArrayList<>(Arrays.asList("k3v1", "k3v2")));
        testSubject.mergeTags(additionalTags);
        Map<String, List<String>> tsTags = testSubject.getTags();
        List<String> k1Tags = tsTags.get("key1");
        for (String value : Arrays.asList("k1v1","k1v2","k1v3")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 1", k1Tags.contains(value));
        }
        List<String> k2Tags = tsTags.get("key2");
        for (String value : Arrays.asList("k2v1","k2v2","k2v3","k2v4","k2v5")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 2",k2Tags.contains(value));
        }
        List<String> k3Tags = tsTags.get("key3");
        for (String value : Arrays.asList("k3v1","k3v2")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 3",k3Tags.contains(value));
        }
        List<String> k4Tags = tsTags.get("key4");
        Assert.assertNull("there should be no entry for key4.", k4Tags);
    }

    @Test
    public void tagMergeIntoNullTest() {
        MetricSpecification testSubject = new MetricSpecification();
        Map<String, List<String>> additionalTags = new HashMap<>();
        additionalTags.put("key2", new ArrayList<>(Arrays.asList("k2v3", "k2v4", "k2v5")));
        additionalTags.put("key3", new ArrayList<>(Arrays.asList("k3v1", "k3v2")));
        testSubject.mergeTags(additionalTags);
        Map<String, List<String>> tsTags = testSubject.getTags();
        List<String> k2Tags = tsTags.get("key2");
        for (String value : Arrays.asList("k2v3","k2v4","k2v5")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 2",k2Tags.contains(value));
        }
        List<String> k3Tags = tsTags.get("key3");
        for (String value : Arrays.asList("k3v1","k3v2")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 3",k3Tags.contains(value));
        }
        List<String> k4Tags = tsTags.get("key4");
        Assert.assertNull("there should be no entry for key4.", k4Tags);
    }

    @Test
    public void tagInitializeTest() {
        MetricSpecification testSubject = new MetricSpecification();
        Map<String, List<String>> initialTags = new HashMap<>();
        initialTags.put("key1",new ArrayList<>(Arrays.asList("k1v1","k1v2","k1v3")));
        initialTags.put("key2",new ArrayList<>(Arrays.asList("k2v1","k2v2","k2v3")));
        testSubject.setTags(initialTags);
        Map<String, List<String>> tsTags = testSubject.getTags();
        List<String> k1Tags = tsTags.get("key1");
        for (String value : Arrays.asList("k1v1","k1v2","k1v3")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 1", k1Tags.contains(value));
        }
        List<String> k2Tags = tsTags.get("key2");
        for (String value : Arrays.asList("k2v1","k2v2","k2v3")) {
            Assert.assertTrue("Tags should contain value " + value + " for key 2",k2Tags.contains(value));
        }
        testSubject.setTags(null);
        tsTags = testSubject.getTags();
        Assert.assertNotNull("After setting tags to null, getTags() should return an empty collection (not NULL)");
        Assert.assertTrue("After setting tags to null, getTags() should return an empty collection.", tsTags.isEmpty());
    }

    @Test
    public void tagRenderingTest() {
        String testString = "sum:10s-ago:rate:laLoadInt{tag2=thing|other thing,tag3=bar,tag1=value1|value2|value3}";
        MetricSpecification subject = MetricSpecification.fromString(testString);
        String generatedString = subject.toString();
        Assert.assertEquals(testString, generatedString);
    }

}
