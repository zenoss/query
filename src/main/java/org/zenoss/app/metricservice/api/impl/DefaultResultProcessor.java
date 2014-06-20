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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.metric.impl.MetricService;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.buckets.Value;
import org.zenoss.app.metricservice.calculators.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Processes the output stream from the back end metric query storage into
 * buckets including the calculation of any RPN functions and references.
 *
 * @author Zenoss
 */
public class DefaultResultProcessor implements ResultProcessor,
    ReferenceProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultResultProcessor.class);

    private class BucketClosure implements Closure {
        public BucketClosure() {

        }

        public long ts;
        public Buckets<MetricKey, String>.Bucket bucket;
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
        BucketClosure b = (BucketClosure) closure;
        if (null == b) {
            throw new NullPointerException("null closure passed to lookup() method.");
        }
        /**
         * If they are looking for special values like "time" then give them
         * that.
         */
        if ("time".equalsIgnoreCase(name)) {
            return b.ts;
        }

        /**
         * Check for metrics or values in the bucket
         */
        Value v = b.bucket.getValueByShortcut(name);
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
    public Buckets<MetricKey, String> processResults(BufferedReader reader, List<MetricSpecification> queries, long bucketSize) throws ClassNotFoundException, IOException, UnknownReferenceException {

        Buckets<MetricKey, String> buckets = new Buckets<>(bucketSize);


        //MetricKey key;
        Map<MetricKey, MetricCalculator> calcs = new HashMap<>();
        MetricCalculatorFactory calcFactory = new MetricCalculatorFactory();

        // Walk the queries and build up a map of metric name to RPN
        // expressions

        /*
         * key is name + tags ... It has to be repeatable, each time generated
         * the same way
         */
        MetricKeyCache keyCache = new MetricKeyCache();
        for (MetricSpecification spec : queries) {
            addKeyToCacheAndSetUpCalculatorForExpression(calcs, calcFactory, keyCache, spec);
        }

        // Get a list of calculated values
        List<MetricSpecification> calculatedValues = MetricService.calculatedValueFilter(queries);
        BucketClosure closure = new BucketClosure();
        //Tags curTags = null;

        List<OpenTSDBQueryResult> allResults = new ArrayList<>();

        ObjectMapper mapper = Utils.getObjectMapper();
        //CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, OpenTSDBQueryResult.class);

        if (log.isDebugEnabled()) {
            reader = logDebugInformation(reader);
        }

        OpenTSDBQueryResult[] queryResult = mapper.readValue(reader, OpenTSDBQueryResult[].class);
        allResults.addAll(Arrays.asList(queryResult));

        Buckets<MetricKey, String>.Bucket previousBucket = null, currentBucket = null;
        long dataPointTimeStamp = 0, previousTs = 0;
        Tags curTags = null;
        for (OpenTSDBQueryResult result : allResults) {
            log.debug("processing result: {}", result.debugString());
            String metricName = result.metric;
            curTags = Tags.fromOpenTsdbTags(result.tags);
            for (Map.Entry<Long, String> dataPointEntry : result.dps.entrySet()) {
                previousTs = dataPointTimeStamp;
                double dataPointValue = Double.valueOf(dataPointEntry.getValue());
                dataPointTimeStamp = dataPointEntry.getKey();

                MetricKey key = keyCache.get(metricName, curTags);
                if (null == key) {
                    log.warn("null key retrieved for metric {} and tags {}", metricName, null == curTags ? "NULL" : curTags.toString());
                    continue;
                }

                MetricCalculator calc = calcs.get(key);
                if (null != calc) {
                    dataPointValue = calc.evaluate(dataPointValue);
                }

                buckets.add(key, key.getName(), dataPointTimeStamp, dataPointValue);
                previousBucket = currentBucket;
                currentBucket = buckets.getBucket(dataPointTimeStamp);
                if (previousBucket != null && currentBucket != previousBucket) {
                    for (MetricSpecification value : calculatedValues) {
                        calculateValue2(buckets, calcs, keyCache, closure, curTags, previousBucket, previousTs, value);
                        //calculateValue(buckets, calcs, keyCache, closure, curTags, previousBucket, previousTs, calc, value);
                    }
                }
            }
        }
        for (MetricSpecification value : calculatedValues) {
            calculateValue2(buckets, calcs, keyCache, closure, curTags, previousBucket, previousTs, value);
        }
        return buckets;
    }

    private void addKeyToCacheAndSetUpCalculatorForExpression(Map<MetricKey, MetricCalculator> calcs, MetricCalculatorFactory calcFactory, MetricKeyCache keyCache, MetricSpecification spec) throws ClassNotFoundException {
        MetricKey key = keyCache.put(MetricKey.fromValue(spec));
        String expr = Strings.nullToEmpty(spec.getExpression()).trim();
        if (!expr.isEmpty()) {
            MetricCalculator calc = calcFactory.newInstance(expr);
            calc.setReferenceProvider(this);
            calcs.put(key, calc);
        }
    }

    private void calculateValue2(Buckets<MetricKey, String> buckets, Map<MetricKey, MetricCalculator> calculators,
                                 MetricKeyCache keyCache, BucketClosure closure, Tags curTags,
                                 Buckets<MetricKey, String>.Bucket previousBucket, long previousTs,
                                 MetricSpecification metricSpecification) {
        MetricKey key = keyCache.get(metricSpecification.getName(), curTags);

        try {
            MetricCalculator calculator = calculators.get(metricSpecification.getName());
            if (null != calculator) {
                closure.ts = previousTs;
                closure.bucket = previousBucket;
                double val = calculator.evaluate(closure);
                buckets.add(key, key.getName(), previousTs, val);
            }
        } catch (UnknownReferenceException e) {
            /*
             * Just because a reference was not in the same bucket does not
             * mean a real failure. It is legitimate.
             */
        }
    }

    private void calculateValue(Buckets<MetricKey, String> buckets, Map<MetricKey, MetricCalculator> calcs,
                                MetricKeyCache keyCache, BucketClosure closure, Tags curTags,
                                Buckets<MetricKey, String>.Bucket previousBucket, long previousTs,
                                MetricCalculator calc, MetricSpecification value) {
        double val;
        log.debug("Processing calculatedValue {}", value);
        MetricKey k2 = keyCache.get(value.getName(), curTags);
        try {
            MetricCalculator calc2 = calcs.get(k2);
            if (null != calc2) {
                closure.ts = previousTs;
                closure.bucket = previousBucket;
                val = calc.evaluate(closure);
                buckets.add(k2, k2.getName(), previousTs, val);
            }
        } catch (UnknownReferenceException e) {
        /*
         * Just because a reference was not in the same bucket
         * does not mean a real failure. It is legitimate.
         */
        }
    }

    private BufferedReader logDebugInformation(BufferedReader reader) throws IOException {
        StringBuffer readerPeekBuffer = new StringBuffer(4096);
        long lineCount = 0;
        String line = reader.readLine();
        if (null != line) {
            lineCount++;
        }
        while (null != line) {
            readerPeekBuffer.append(line);
            readerPeekBuffer.append("\n");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            line = reader.readLine();
            if (null != line) {
                lineCount++;
            }
        }
        log.debug("LINES READ FROM OPENTSDB: {}", lineCount);

        String contents = readerPeekBuffer.toString();
        reader = new BufferedReader(new StringReader(contents));
        log.debug("Reader Content: {}", contents);
        return reader;
    }

}
