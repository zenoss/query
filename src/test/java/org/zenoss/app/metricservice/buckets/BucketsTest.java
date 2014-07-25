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
import org.zenoss.app.metricservice.api.impl.IHasShortcut;
import org.zenoss.app.metricservice.api.impl.MetricKey;

import static org.junit.Assert.*;

public class BucketsTest {
    private static final double EPSILON = 0.001;

    @Test
    public void testAdd() throws Exception {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        testSubject.add(MetricKey.fromValue("", "", ""), 123, 1.234);
        testSubject.add(MetricKey.fromValue("My.Metric.Formal.Name", "MyMetric", "Foo=Bar"), 123, 4.567);
        Buckets.Bucket bucket = testSubject.getBucket(123);
        assertEquals("getValueByShortcut should return the value put in with that shortcut",
            bucket.getValueByShortcut("").getValue(), 1.234, EPSILON);
        assertEquals("getValueByShortcut should return the value put in with that shortcut",
            bucket.getValueByShortcut("My.Metric.Formal.Name").getValue(), 4.567, EPSILON);
    }

    private Buckets<IHasShortcut> makeTestBuckets() {
        return new Buckets<>();
    }

    @Test
    public void testGetBucket() throws Exception {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        MetricKey key = MetricKey.fromValue("My.Metric.Formal.Name", "MyMetric", "Foo=Bar");
        testSubject.add(key, 123, 4.567);
        Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(123);

        assertEquals("getValue should return the value put in with that key", bucket.getValue(key).getValue(), 4.567, EPSILON);
        assertEquals("getValueByShortcut should return the value put in with that shortcut", bucket.getValueByShortcut("My.Metric.Formal.Name").getValue(), 4.567, EPSILON);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addWithNullKeyShouldThrowException() {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        testSubject.add(null,12345, 1.234);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addWithEmptyKeyShouldThrowException() {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        MetricKey emptyKey = new MetricKey();
        testSubject.add(emptyKey, 123, 1.234);
    }

    @Test (expected = IllegalArgumentException.class)
    public void addWithKeyHavingNullShortcutShouldThrowException() {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        MetricKey emptyKey = MetricKey.fromValue(null, "MyMetric", "Foo=Bar");
        assertNull("getShortcut on this key should be null", emptyKey.getShortcut());
        testSubject.add(emptyKey, 123, 1.234);
    }


    @Test
    public void testGetTimestamps() throws Exception {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        assertTrue("GetTimestamps", null != testSubject.getTimestamps());
    }

    @Test
    public void testGetSecondsPerBucket() throws Exception {
        Buckets<IHasShortcut> testSubject = makeTestBuckets();
        assertTrue("Buckets should default to 300 seconds per bucket.", 300 == testSubject.getSecondsPerBucket());
        Buckets<IHasShortcut> testSubject2 = new Buckets<>(123);
        assertTrue("Buckets created with specified seconds per bucket should have that value.", 123 == testSubject2.getSecondsPerBucket());
    }

    @Test
    public void testDump() throws Exception {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        BucketTestUtilities.dumpBucketsToStdout(testSubject);
    }

    @Test
    public void testGetValuePastEndOfDataReturnsNaN() {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        final Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(1500);
        assertNull("get bucket outside of time series should return null.", bucket);
    }
    @Test
    public void testGetValueNotInDataReturnsNaNByShortcut() {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        final Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(1000);
        assertNotNull("get bucket in time series should return a bucket.", bucket);
        double valueForNonexistentSeries = bucket.getValueByShortcut("nonexistentSeries").getValue();
        assertEquals("Value for nonexistent series should be NaN", Double.NaN, valueForNonexistentSeries, EPSILON);
    }
    @Test
    public void testGetValueNotInDataReturnsNaNByMetricKey() {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        final Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(1000);
        assertNotNull("get bucket in time series should return a bucket.", bucket);
        MetricKey nonexistentMetric = MetricKey.fromValue("NonexistentMetric", "Nonexistent Series", "device=dev1 Series=M1");
        double valueForNonexistentSeries = bucket.getValue(nonexistentMetric).getValue();
        assertEquals("Value for nonexistent series should be NaN", Double.NaN, valueForNonexistentSeries, EPSILON);
    }

    @Test
    public void specifyingNegativeBucketSizeDefaultsValue() {
        Buckets<IHasShortcut> testSubject = new Buckets<>(-25l);
        assertEquals("If a negative bucket is specified, buckets should have default bucket size.", Buckets.DEFAULT_BUCKET_SIZE, testSubject.getSecondsPerBucket());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValueWithNullShouldThrowException() {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        final Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(1000);
        bucket.getValue(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValueByShortcutWithNullShouldThrowException() {
        Buckets<IHasShortcut> testSubject = BucketTestUtilities.makeAndPopulateTestBuckets();
        final Buckets<IHasShortcut>.Bucket bucket = testSubject.getBucket(1000);
        bucket.getValueByShortcut(null);
    }

}
