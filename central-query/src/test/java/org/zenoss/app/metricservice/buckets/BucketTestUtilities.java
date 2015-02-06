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

import org.zenoss.app.metricservice.api.impl.IHasShortcut;
import org.zenoss.app.metricservice.api.impl.MetricKey;
import org.zenoss.app.metricservice.testutil.ConstantSeriesGenerator;
import org.zenoss.app.metricservice.testutil.SeriesGenerator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class BucketTestUtilities {
    public static Buckets<IHasShortcut> makeAndPopulateTestBuckets() {
        Buckets<IHasShortcut> result = new Buckets<>(51);
        MetricKey metric1 = MetricKey.fromValue("Metric1", "GizmosPerGadget", "device=dev1 Series=M1");
        SeriesGenerator generator = new ConstantSeriesGenerator(5.0);
        Map<Long, Double> metric1Values = generator.generateValues(3, 1203, 7);
        for (Map.Entry<Long, Double> e : metric1Values.entrySet()) {
            result.add(metric1, e.getKey(), e.getValue());
        }
        MetricKey metric2 = MetricKey.fromValue("Metric2", "WidgetThroughput", "device=dev1 Series=M2");
        Map<Long, Double> metric2Values = generator.generateValues(0, 1200, 13);
        for (Map.Entry<Long, Double> e : metric2Values.entrySet()) {
            result.add(metric2, e.getKey(), e.getValue());
        }
        return result;
    }

    public static void dumpBucketsToStdout(Buckets<IHasShortcut> testSubject) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream testStream = new PrintStream(baos);
        testSubject.dump(testStream);
        System.out.println(String.format("TestSubject: %s", baos.toString()));
    }
}
