package org.zenoss.app.metricservice.buckets;

import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.MetricKey;
import org.zenoss.app.metricservice.api.impl.Tags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/12/14
 * Time: 6:12 PM
 * To change this template use File | Settings | File Templates.
 */
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
