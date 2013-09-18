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

import org.zenoss.app.metricservice.api.model.MetricSpecification;

/**
 * A combination key that uniquely identifies a metric based on the metric name
 * queried and the tags associated with that query.
 * 
 * @author Zenoss
 * 
 */
public class MetricKey {
    /**
     * Name of the metric specification
     */
    private String name = null;

    /**
     * Metric value this key is for
     */
    private String metric = null;

    /**
     * Tags associated with the metric query
     */
    private Tags tags = null;

    /**
     * Determines if to instances are equivalent. They are considered equivalent
     * if their metric and tags are equivalent.
     * 
     * @param other
     *            instance to which to compare
     * @return true if the two instance are equivalent, else false.
     */
    public boolean equals(MetricKey other) {
        if (other == null || !this.metric.equals(other.metric)) {
            return false;
        }

        return this.tags.equals(other);
    }

    /**
     * Metric accessor
     * 
     * @return metric
     */
    public String getMetric() {
        return metric;
    }

    /**
     * Tags accessor
     * 
     * @return tags
     */
    public Tags getTags() {
        return tags;
    }

    /**
     * Name accessor
     * 
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Constructs an instance from a MetricSpecification
     * 
     * @param spec
     *            instance from which to create the MetricKey
     * @return MetricKey instance
     */
    static public MetricKey fromValue(MetricSpecification spec) {
        MetricKey key = new MetricKey();
        key.metric = spec.getMetricOrName();
        key.name = spec.getNameOrMetric();
        if (spec.getTags() != null && spec.getTags().size() > 0) {
            key.tags = Tags.fromValue(spec.getTags());
        }
        return key;
    }

    /**
     * Constructs an instance from given parameters
     * 
     * @param name
     *            name
     * @param metric
     *            metric
     * @param tags
     *            tags
     * @return MetricKey instance
     */
    static public MetricKey fromValue(String name, String metric, String tags) {
        MetricKey key = new MetricKey();
        key.name = name;
        key.metric = metric;
        if (tags != null) {
            key.tags = Tags.fromValue(tags);
        }
        return key;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("[%s,  %s, %s]", metric, name, tags);
    }
}
