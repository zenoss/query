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

package org.zenoss.app.metricservice.buckets;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.impl.IHasShortcut;

import java.io.PrintStream;
import java.util.*;

/**
 * Provides a utility to bucket metrics into defined sized chunks, averaging the
 * values when multiple values fall in a single bucket.
 * 
 * @author Zenoss
 * 
 * @param <P>
 *            primary key type
 */
public class Buckets<P extends IHasShortcut> {

    private final Logger log = LoggerFactory.getLogger(Bucket.class);
    /**
     * Default bucket size of 5 minutes
     */
    public static final long DEFAULT_BUCKET_SIZE = 5l * 60; // 5 Minutes

    /**
     * Set of buckets indexed by time in seconds
     */
    private final Map<Long, Buckets<P>.Bucket> bucketList = new TreeMap<>();

    /**
     * Specifies the size of each bucket in seconds
     */
    private long secondsPerBucket = DEFAULT_BUCKET_SIZE;

    private OccurrenceCounter<P> primaryKeyOccurrences = new OccurrenceCounter<>();

    public Map<Long, Bucket> getBucketList() {
        return bucketList;
    }

    /**
     * Each bucket maintains a summary of the values that fall within that
     * bucket. This includes the sum of all the values added for a given metric
     * and the number of values per metric. This minimal information allows for
     * the calculation of the average as well as implements easy addition and
     * removal of values.
     * 
     * @author Zenoss
     */
    public final class Bucket {

        /**
         * Map from the primary key to the values within a bucket
         */
        @JsonProperty("values")
        private final Map<P, Value> values = new HashMap<>();

        /**
         * Map from the shortcut key to the values within a bucket
         */
        @JsonProperty("valuesByName")
        private Map<String, Value> valuesByName = new HashMap<>();

        /**
         * Add a value to a bucket
         * 
         * @param primaryKey
         *            primary key for the value
         * @param value
         *            value to add
         */
        private final void add(final P primaryKey,final double value) {

            Value holder = getOrCreateValue(primaryKey);

            // Add the value
            holder.add(value);
        }

        /**
         * Returns a value based on primary key lookup
         * 
         * @param key
         *            primary key
         * @return the value associated with the primary key or null
         */
        public final Value getValue(final P key) {
            return getOrCreateValue(key);
        }

        private final Value getOrCreateValue(final P primaryKey) {
            if (null == primaryKey) {
                throw new IllegalArgumentException("primary Key cannot be null.");
            }
            if (null == primaryKey.getShortcut()) {
                throw new IllegalArgumentException("shortcut cannot be null.");
            }

            // Fetch existing value, if it exists
            Value value = values.get(primaryKey);

            // If value does not exists, create and add
            if (value == null) {
                value = new Value();
                values.put(primaryKey, value);
                valuesByName.put(primaryKey.getShortcut(), value);
            }
            return value;
        }

        /**
         * Returns a value based on the shortcut key lookup
         * 
         * @param shortcut
         *            shortcut key
         * @return the value associated with the shortcut key or null
         */
        public final Value getValueByShortcut(String shortcut) {
            if (null == shortcut) {
                throw new IllegalArgumentException("shortcut cannot be null.");
            }
            Value result = valuesByName.get(shortcut);
            if (null == result) {
                result = new Value();
            }
            return result;
        }

        public boolean hasValue(Object key) {
            Value value = values.get(key);
            return (value != null && value.getCount() > 0l);
        }

        private void addInterpolated(P primaryKey, double value) {
            Value holder = getOrCreateValue(primaryKey);

            // Add the value
            holder.addInterpolated(value);
        }
    }

    /**
     * Default, no parameter constructor. Creates an instance of buckets with a
     * default bucket size.
     */
    public Buckets() {
    }

    /**
     * Constructs an instance of buckets with a specified bucket size.
     * 
     * @param secondsPerBucket
     *            the number of seconds per each bucket
     */
    public Buckets(final long secondsPerBucket) {
        if (secondsPerBucket > 0l) {
            this.secondsPerBucket = secondsPerBucket;
        } else {
            log.warn("secondsPerBucket must be positive. {} was specified. Defaulting to {}.", secondsPerBucket, this.secondsPerBucket);
        }
    }

    /**
     * Add a value to the buckets
     * 
     * @param primaryKey
     *            primary key for the vlaue
     * @param timestamp
     *            timestamp of the value (will be rounded based on secondsPerBucket
     *            size)
     * @param value
     *            value to add
     */
    public final void add(final P primaryKey, final long timestamp, final double value) {
        long ts = getBucketTimestamp(timestamp);
        // Get existing or create new bucket for this timestamp
        Bucket b = bucketList.get(ts);
        if (b == null) {
            b = new Bucket();
            bucketList.put(ts, b);
        }

        // Add the value
        b.add(primaryKey, value);
        addOccurrence(primaryKey, timestamp);
    }

    /**
     * Add an value to the buckets. If the bucket already has an interpolated value, update it.
     *
     * @param primaryKey
     *            primary key for the vlaue
     * @param timestamp
     *            timestamp of the value (will be rounded based on secondsPerBucket
     *            size)
     * @param value
     *            value to add
     */
    public final void addInterpolated(final P primaryKey, final long timestamp, final double value) {
        long ts = getBucketTimestamp(timestamp);

        // Get existing or create new bucket for this timestamp
        Bucket b = bucketList.get(ts);
        if (b == null) {
            b = new Bucket();
            bucketList.put(ts, b);
        }
        // Add the value
        b.addInterpolated(primaryKey, value);
    }

    private void addOccurrence(P primaryKey, long timestamp) {
        primaryKeyOccurrences.logOccurrence(primaryKey, timestamp);
    }

    /**
     * Returns a bucket for a specified timestamp or null if a bucket does not
     * exists for the timestamp.
     * 
     * @return bucket of the given timestamp (that will be downsampled) or null
     */
    public final Buckets<P>.Bucket getBucket(long timestamp) {
        long bucketTimestamp = getBucketTimestamp(timestamp);
        return bucketList.get(bucketTimestamp);
    }

    private long getBucketTimestamp(long timestamp) {
        return (timestamp / secondsPerBucket) * secondsPerBucket;
    }

    /**
     * Returns a sorted copy of the timestamps for the buckets. The timestamps
     * returned are the downsampled values. To get the correct timestamp these
     * values should be multiplied by the secondsPerBucket value.
     * 
     * @return sorted list of downsampled time values
     */
    public final SortedSet<Long> getTimestamps() {
        SortedSet<Long> result = new TreeSet<>(bucketList.keySet());
        return result;
    }

    public final Set<P> getPrimaryKeys() {
        return primaryKeyOccurrences.getKeys();
    }

    /**
     * Returns the secondsPerBucket value
     * 
     * @return secondsPerBucket
     */
    public final long getSecondsPerBucket() {
        return secondsPerBucket;
    }

    /**
     * Dumps the contents of the buckets to the given print stream. This can be
     * useful for debugging
     * 
     * @param ps: printstream instance to use for the dump
     */
    public final void dump(PrintStream ps) {
        List<Long> keys = new ArrayList<>(bucketList.keySet());
        Collections.sort(keys);

        for (long k : keys) {
            ps.format("BUCKET: %d (%d) (%s)%n", k, k * secondsPerBucket, new Date(k * secondsPerBucket * 1000));
            for (P key : bucketList.get(k).values.keySet()) {
                Value value = bucketList.get(k).values.get(key);
                ps.format("    %-40s : %10.2f (%10.2f / %d)%n", key.toString(),
                        value.getValue(),
                        value.getSum(),
                        value.getCount());
            }
        }
    }

    private class OccurrenceCounter<K> {
        private final Map<K, Long> occurrenceMap = new HashMap<>();

        public void logOccurrence(K key, long timestamp) {
            increment(key);
        }

        private void increment(K key) {
            Long count = occurrenceMap.get(key);
            if (null == count) {
                occurrenceMap.put(key,Long.valueOf(1));
            } else {
                occurrenceMap.put(key, count++);
            }
        }

        public Set<K> getKeys() {
            return Collections.unmodifiableSet(occurrenceMap.keySet());
        }
    }
}
