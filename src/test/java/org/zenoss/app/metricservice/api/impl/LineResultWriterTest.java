package org.zenoss.app.metricservice.api.impl;

import org.junit.Ignore;
import org.junit.Test;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/12/14
 * Time: 5:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class LineResultWriterTest {
    @Test
    @Ignore
    public void testWriteData() throws Exception {
        LineResultWriter victim = new LineResultWriter();
        Writer testWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(testWriter);
        List<MetricSpecification> queries = new ArrayList<>();
        Buckets<MetricKey, String> buckets = new Buckets<>();
        ReturnSet returnset = ReturnSet.ALL;
        long startTS = 0;
        long endTs = 10000;

        victim.writeData(jsonWriter, queries, buckets, returnset, startTS, endTs );
        System.out.println(String.format("results: %s", testWriter.toString()));
        assertTrue(testWriter.toString().length() > 0);
    }
}
