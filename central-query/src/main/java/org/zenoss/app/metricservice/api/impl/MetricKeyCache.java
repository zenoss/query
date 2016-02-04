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

import java.util.*;

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

        String cacheKey = getCacheKey(key);

        List<MetricKey> list = map.get(cacheKey);
        if (list == null) {
            list = new ArrayList<>();
            map.put(cacheKey, list);
        }
        list.add(key);
        return key;
    }

    private String makeCacheKey(String metric,  String name, String id){
        if (name == null){
            name = "DEFAULT_NAME";
        }
        if (id == null){
            id = "DEFAULT_ID";
        }
        return metric + "<>" + name + "<>" + id;

    }
    private String getCacheKey(MetricKey key) {
        return makeCacheKey(key.getMetric(), key.getName(), key.getId());
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
    public MetricKey get(String metric, String name, String id, Tags tags) {
        String cacheKey = makeCacheKey(metric, name, id);
        List<MetricKey> list = map.get(cacheKey);
        if (list != null) {
            for (MetricKey key : list) {
                if (null != tags && tags.equals(key.getTags()) || key.getTags() == null || key.getTags().match(tags)) {
                    return key;
                }
            }
        }
        return null;
    }
}
