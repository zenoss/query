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
import org.zenoss.app.metricservice.api.impl.MetricKey;
import org.zenoss.app.metricservice.api.impl.Tags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BucketsTest {
    @Test
    public void testAdd() throws Exception {
        Buckets<MetricKey, String> victim = makeTestBuckets();
        victim.add(null, "", 123,1.234 );
        victim.add(MetricKey.fromValue("MyKey","MyMetric", "Foo=Bar"),"My.Metric.Formal.Name",123,4.567);
        Buckets.Bucket something = victim.getBucket(123);
        assertTrue("getValueByShortcut should return the value put in with that shortcut", something.getValueByShortcut("").getValue() == 1.234);
        assertTrue("getValueByShortcut should return the value put in with that shortcut", something.getValueByShortcut("My.Metric.Formal.Name").getValue() == 4.567);
    }

    private Buckets<MetricKey, String> makeTestBuckets() {
        return new Buckets<>();
    }

    @Test
    public void testGetBucket() throws Exception {

    }

    @Test
    public void testGetTimestamps() throws Exception {
        Buckets<MetricKey,String> victim = makeTestBuckets();
        assertTrue("GetTimestamps", null != victim.getTimestamps());
    }

    @Test
    public void testGetSecondsPerBucket() throws Exception {
        Buckets<MetricKey, String> victim = makeTestBuckets();
        assertTrue("Buckets should default to 300 seconds per bucket.", 300 == victim.getSecondsPerBucket());
        Buckets<MetricKey, String> victim2 = new Buckets<>(123);
        assertTrue("Buckets created with specified seconds per bucket should have that value.", 123 == victim2.getSecondsPerBucket());

    }

    @Test
    public void testDump() throws Exception {

    }
}
