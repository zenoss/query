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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a cache of metric keys. This class does not check for duplication
 * when adding keys to the cache. This is the responsibility of the client.
 * 
 * @author Zenoss
 */
public class  MetricKeyCache {

    /**
     * Current set of all metric keys in the cache
     */
    Set<MetricKey> keys = new HashSet<>();

    /**
     * Map of a simple string name to the metric keys
     */
    Map<String, List<MetricKey>> map = new HashMap<>();

    /**
     * Place a metric key in the cache and return the key just added. This
     * method does not check for duplications.
     * 
     * @param key
     *            the key to add
     * @return the key added
     */
    public MetricKey put(MetricKey key) {
        keys.add(key);
        List<MetricKey> list = map.get(key.getMetric());
        if (list == null) {
            list = new ArrayList<>();
            map.put(key.getMetric(), list);
        }
        list.add(key);
        return key;
    }

    /**
     * Fetches a given metric key based on the metric name and tags. A key
     * matches if it has the correct metric name and if the keys tags map to the
     * given tags. Thus the keys tags may contain wild cards and pipes.
     * 
     * @param metric
     *            metric to search for
     * @param tags
     *            tags to search for
     * @return matching metric key or null.
     */
    public MetricKey get(String metric, Tags tags) {
        List<MetricKey> list = map.get(metric);
        if (list != null) {
            for (MetricKey key : list) {
                if (null != tags && tags.equals(key.getTags()) || key.getTags() == null || key.getTags().match(tags)) {
                    return key;
                }
            }
        }
        return null;
    }

    /**
     * Fetches a metric key that matches the given metric key. A key matches if
     * it has the correct metric name and if the keys tags map to the given
     * tags. Thus the keys tags may contain wild cards and pipes.
     * 
     * @param key
     *            the key to match
     * @return matched key or null
     */
    public MetricKey get(MetricKey key) {
        // Check to see if it is in our key set and if so just return it
        if (keys.contains(key)) {
            return key;
        }

        // Now the harder / longer check
        return get(key.getMetric(), key.getTags());
    }

    /**
     * Dump the contents of the cache to a print stream. Useful for debugging
     * 
     * @param ps
     *            PrintStream instance to which to dump the information
     */
    public void dump(PrintStream ps) {
        for (Map.Entry<String, List<MetricKey>> stringListEntry : map.entrySet()) {
            ps.format("%s : %s%n", stringListEntry.getKey(), stringListEntry.getValue());
        }
    }
}
