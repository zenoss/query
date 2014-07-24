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

import org.junit.Test;
import org.zenoss.app.metricservice.api.model.InterpolatorType;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.buckets.Value;
import org.zenoss.app.metricservice.calculators.Closure;
import org.zenoss.app.metricservice.testutil.ConstantSeriesGenerator;
import org.zenoss.app.metricservice.testutil.DataReaderGenerator;
import org.zenoss.app.metricservice.testutil.SeriesGenerator;
import org.zenoss.app.metricservice.testutil.YEqualsXSeriesGenerator;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class DefaultResultProcessorTest {

    private static final double EPSILON = 0.00000001;
    private static final double CONST_VALUE = 2.0;
    private static final String CALCULATED_VALUE_SERIES_NAME = "CalculatedValue";

    // The commented-out lines below are an 'easy' case for debugging.
//    private static final long START_TIME = 100;
//    private static final long END_TIME = 120;
//    private static final long BUCKET_SIZE = 4;
//    private static final long HOURLY_STEP = 2;
//    private static final long DAILY_STEP = HOURLY_STEP * 3;

    private static final long START_TIME = 1388534400; // Midnight, 1/1/14
    private static final long END_TIME = 1389744000; // Midnight, 1/15/14
    private static final long BUCKET_SIZE = 3600;
    private static final long HOURLY_STEP = 3600;
    private static final long DAILY_STEP = HOURLY_STEP * 24;


    @Test
    public void testLookup() throws Exception {
        Closure closure = mock(Closure.class);
        Value myValue = new Value();
        myValue.add(1.0);
        when(closure.getValueByShortcut("name")).thenReturn(myValue);

        DefaultResultProcessor victim = new DefaultResultProcessor(null, null, Buckets.DEFAULT_BUCKET_SIZE);
        double foundValue = victim.lookup("name", closure);
        assertEquals("lookup should return correct value for series.", myValue.getValue(), foundValue, EPSILON);
    }

    @Test
    public void testProcessResultsWithConstantSeries() throws Exception {
        BufferedReader reader = makeReader();  // creates data for test
        List<MetricSpecification> queries = makeQueries(); // creates queries for test
        DefaultResultProcessor victim = new DefaultResultProcessor(reader, queries, BUCKET_SIZE);
        Buckets<IHasShortcut> results = victim.processResults();
        assertNotNull("Result of processing query should not be null", results);
        assertEquals("Seconds per bucket should match specified bucket size.", BUCKET_SIZE, results.getSecondsPerBucket());
        for (Long timestamp : results.getTimestamps()) {
            Buckets.Bucket bucket = results.getBucket(timestamp);
            assertNotNull(String.format("Null bucket found at timestamp %d.", timestamp), bucket);
            for (MetricSpecification query : queries) {
                String nameOrMetric = query.getNameOrMetric();
                Value value = bucket.getValueByShortcut(nameOrMetric);
                String pointDescriptor = String.format("series %s at timestamp %d", nameOrMetric, timestamp);
                assertNotNull(String.format("Missing value for %s.", pointDescriptor), value);
                if (query.getNameOrMetric().equals(CALCULATED_VALUE_SERIES_NAME)) {
                    System.out.println(String.format("value of %s at %d is %f", nameOrMetric, timestamp, value.getValue()));
                    assertEquals(String.format("Value of %s not correct.", pointDescriptor), CONST_VALUE + CONST_VALUE, value.getValue(), EPSILON);
                } else if (null != value) {
                   assertEquals(String.format("Value of %s not correct.", pointDescriptor), CONST_VALUE, value.getValue(), EPSILON);
                }
            }
        }
    }

    @Test
    public void testProcessResultsWithYEqualsXSeries() throws Exception {
        BufferedReader reader = makeYEqualsXReader();  // creates data for test
        List<MetricSpecification> queries = makeQueries(); // creates queries for test
        DefaultResultProcessor victim = new DefaultResultProcessor(reader, queries, BUCKET_SIZE);
        Buckets<IHasShortcut> results = victim.processResults();
        assertNotNull("Result of processing query should not be null", results);
        assertEquals("Seconds per bucket should match specified bucket size.", BUCKET_SIZE, results.getSecondsPerBucket());
        for (Long timestamp : results.getTimestamps()) {
            Buckets.Bucket bucket = results.getBucket(timestamp);
            assertNotNull(String.format("Null bucket found at timestamp %d.", timestamp), bucket);
            for (MetricSpecification query : queries) {
                String nameOrMetric = query.getNameOrMetric();
                Value value = bucket.getValueByShortcut(nameOrMetric);
                String pointDescriptor = String.format("series %s at timestamp %d", nameOrMetric, timestamp);
                assertNotNull(String.format("Missing value for %s.", pointDescriptor), value);
                if (query.getNameOrMetric().equals(CALCULATED_VALUE_SERIES_NAME)) {
                    System.out.println(String.format("value of %s at %d is %f", nameOrMetric, timestamp, value.getValue()));
                    assertEquals(String.format("Value of %s not correct.", pointDescriptor), timestamp + timestamp, value.getValue(), EPSILON);
                } else if (null != value) {
                    assertEquals(String.format("Value of %s not correct.", pointDescriptor), timestamp, value.getValue(), EPSILON);
                }
            }
        }
    }

    private BufferedReader makeReader() {
        DataReaderGenerator generator = new DataReaderGenerator();
        SeriesGenerator dataGen = new ConstantSeriesGenerator(CONST_VALUE);
        generator.addSeries(MetricSpecification.fromString("hourlyMetric"), dataGen, START_TIME, END_TIME, HOURLY_STEP);
        generator.addSeries(MetricSpecification.fromString("dailyMetric"), dataGen, START_TIME, END_TIME, DAILY_STEP);
        return generator.makeReader();
    }

    private BufferedReader makeYEqualsXReader() {
        DataReaderGenerator generator = new DataReaderGenerator();
        SeriesGenerator dataGen = new YEqualsXSeriesGenerator();
        MetricSpecification dailySpecification = MetricSpecification.fromString("dailyMetric");
        dailySpecification.setInterpolator(InterpolatorType.linear);
        generator.addSeries(MetricSpecification.fromString("hourlyMetric"), dataGen, START_TIME, END_TIME, HOURLY_STEP);
        generator.addSeries(MetricSpecification.fromString("dailyMetric"), dataGen, START_TIME, END_TIME, DAILY_STEP);
        return generator.makeReader();
    }

    private List<MetricSpecification> makeQueries() {
        List<MetricSpecification> result = new ArrayList<>();
        MetricSpecification hourlySpec = MetricSpecification.fromString("hourlyMetric");
        result.add(hourlySpec);

        MetricSpecification dailySpec = MetricSpecification.fromString("dailyMetric");
        dailySpec.setInterpolator(InterpolatorType.linear);
        result.add(dailySpec);

        MetricSpecification calculatedExpression = new MetricSpecification();
        calculatedExpression.setName(CALCULATED_VALUE_SERIES_NAME);
        calculatedExpression.setExpression("rpn:hourlyMetric,dailyMetric,+");
        result.add(calculatedExpression);

        return result;
    }
}
