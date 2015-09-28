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

package org.zenoss.app.metricservice.api.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.metric.impl.MetricService;
import org.zenoss.app.metricservice.api.model.InterpolatorType;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.buckets.Interpolator;
import org.zenoss.app.metricservice.buckets.InterpolatorFactory;
import org.zenoss.app.metricservice.buckets.Value;
import org.zenoss.app.metricservice.calculators.Closure;
import org.zenoss.app.metricservice.calculators.MetricCalculator;
import org.zenoss.app.metricservice.calculators.MetricCalculatorFactory;
import org.zenoss.app.metricservice.calculators.ReferenceProvider;
import org.zenoss.app.metricservice.calculators.UnknownReferenceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes the output stream from the back end metric query storage into
 * buckets including the calculation of any RPN functions and references.
 *
 * @author Zenoss
 */
public class DefaultResultProcessor implements ResultProcessor,
        ReferenceProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultResultProcessor.class);
    private final Multimap<InterpolatorType, IHasShortcut> interpolatorMap = ArrayListMultimap.create();
    private final Iterable<OpenTSDBQueryResult> results;
    private final List<MetricSpecification> queries;
    private final long bucketSize;
    private Map<MetricKey, MetricCalculator> calculatorMap;
    private MetricKeyCache keyCache;
    private Buckets<IHasShortcut> buckets;

    public DefaultResultProcessor(Iterable<OpenTSDBQueryResult> results, List<MetricSpecification> queries, long bucketSize) {
        this.results = results;
        this.queries = queries;
        this.bucketSize = bucketSize;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.zenoss.app.metricservice.calculators.ReferenceProvider#lookup
     * (java .lang.String, org.zenoss.app.metricservice.calculators.Closure)
     */
    @Override
    public double lookup(String name, Closure closure)
            throws UnknownReferenceException {
        if (null == closure) {
            throw new NullPointerException("null closure passed to lookup() method.");
        }
        /**
         * If they are looking for special values like "time" then give them
         * that.
         */
        if ("time".equalsIgnoreCase(name)) {
            return closure.getTimeStamp();
        }

        /**
         * Check for metrics or values in the bucket
         */
        Value v = closure.getValueByShortcut(name);
        if (v == null) {
            throw new UnknownReferenceException(name);
        }
        return v.getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.zenoss.app.metricservice.api.impl.ResultProcessor#processResults(
     * java.io.BufferedReader, java.util.List, long)
     */
    @Override
    public Buckets<IHasShortcut> processResults() throws IOException, ClassNotFoundException {
        initialize();

        for (MetricSpecification metricSpecification : queries) {
            preProcessQuerySpecifications(metricSpecification);
        }

        // Get a list of calculated values
        List<MetricSpecification> calculatedValues = MetricService.calculatedValueFilter(queries);

        long dataPointTimeStamp = 0l;
        Tags curTags = null;

        // iterate over results (a result is a data series - with metric name, collection of points, tags, etc.
        for (OpenTSDBQueryResult result : this.results) {
            String metricName = result.metric;
            curTags = Tags.fromOpenTsdbTags(result.tags);
            MetricKey key = keyCache.get(metricName, curTags);
            QueryStatus status = result.getStatus();
            log.debug(String.format("Adding QueryStatus %s for key %s (hashcode: %d)", status.getMessage(), key.toString(), key.hashCode()));
            buckets.addQueryStatus(key, status);

            // iterate over data points for the current series
            for (Map.Entry<Long, Double> dataPointEntry : result.dps.entrySet()) {
                double dataPointValue = dataPointEntry.getValue();
                dataPointTimeStamp = dataPointEntry.getKey();

                buckets.add(key, dataPointTimeStamp, dataPointValue);
            } // iterate over data points in this series
        } //iterate over all series in result set
        interpolateValues(buckets);
        calculateValues(calculatedValues, buckets);
        return buckets;
    }

    private void interpolateValues(Buckets<IHasShortcut> buckets) {
        for (InterpolatorType interpolatorType : interpolatorMap.keys()) {
            log.debug("Interpolating for type: [{}].", interpolatorType);
            Collection<IHasShortcut> foo = interpolatorMap.get(interpolatorType);
            for (IHasShortcut series : foo) {
                log.debug("Series [{}] interpolated with [{}] interpolator.", series.getShortcut(), interpolatorType);
            }
            Interpolator interpolator = InterpolatorFactory.getInterpolator(interpolatorType);
            interpolator.interpolate(buckets, interpolatorMap.get(interpolatorType));
        }
    }

    private void initialize() {
        buckets = new Buckets<>(bucketSize);
        calculatorMap = new HashMap<>();
        keyCache = new MetricKeyCache();
    }


    private void calculateValues(List<MetricSpecification> calculatedValues, Buckets<IHasShortcut> buckets) {
        for (MetricSpecification metricSpecification : calculatedValues) {
            Tags tags = Tags.fromValue(metricSpecification.getTags());
            MetricKey key = keyCache.get(metricSpecification.getName(), tags);
            MetricCalculator calculator = calculatorMap.get(key);
            if (null != calculator) {
                for (Long timestamp : buckets.getTimestamps()) {
                    Buckets<IHasShortcut>.Bucket bucket = buckets.getBucket(timestamp);
                    if (null == bucket) {
                        log.error("Null bucket at timestamp {}", timestamp);
                        throw new NullPointerException("unexpected null bucket");
                    }

                    BucketClosure closure = new BucketClosure(timestamp, bucket);
                    double val = 0.0;
                    try {
                        val = calculator.evaluate(closure);
                        buckets.add(key, timestamp, val);
                    } catch (UnknownReferenceException e) {
                        log.debug("UnknownReferenceException swallowed for calculation at timestamp {}: {}", timestamp, e);
                        /*
                         * Just because a reference was not in the same bucket does not
                         * mean a real failure. It is legitimate.
                         */
                    }
                }
            }
        }
    }

    private void preProcessQuerySpecifications(MetricSpecification spec) throws ClassNotFoundException {
        MetricKey key = keyCache.put(MetricKey.fromValue(spec));
        String expr = Strings.nullToEmpty(spec.getExpression()).trim();
        if (!expr.isEmpty()) {
            MetricCalculator calc = MetricCalculatorFactory.newInstance(expr);
            calc.setReferenceProvider(this);
            calculatorMap.put(key, calc);
        }
        interpolatorMap.put(spec.getInterpolator(), key);
    }

    private static class BucketClosure implements Closure {
        private long ts;
        public Buckets<IHasShortcut>.Bucket bucket;

        private BucketClosure(long timestamp, Buckets<IHasShortcut>.Bucket bucket) {
            this.ts = timestamp;
            this.bucket = bucket;
        }

        @Override
        public long getTimeStamp() {
            return ts;
        }

        @Override
        public Value getValueByShortcut(String name) {
            return bucket.getValueByShortcut(name);
        }
    }
}
