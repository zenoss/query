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
package org.zenoss.app.query.api;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public final class MetricQuery {
	private final String metric;
	private final Aggregator aggregator;
	private final String downsample;
	private final boolean rate;
	private final Map<String, String> tags;

	private static final Aggregator DEFAULT_AGGREGATOR = Aggregator.avg;

	public MetricQuery(Aggregator aggregator, String downsample, boolean rate,
			String metric, Map<String, String> tags) {
		this.aggregator = aggregator;
		this.downsample = downsample;
		this.rate = rate;
		this.metric = metric;
		this.tags = tags;
	}

	private static Map<String, String> parseTags(String value) {
		Map<String, String> tags = new HashMap<String, String>();

		if (value == null || (value = value.trim()).length() == 0) {
			return tags;
		}
		String[] pairs = value.substring(1, value.length() - 1).split(",");
		for (String pair : pairs) {
			String[] terms = pair.split("=", 2);
			tags.put(terms[0].trim(), terms[1].trim());
		}
		return tags;
	}

	public static MetricQuery fromString(String value) {

		// Determine if there are tags in this query specification. This will
		// be a simple check, if there is a pattern '{' ... at the end
		// of the value, then strip it off as the tags.
		String[] terms = value.split(":", 4);
		int idx = terms[terms.length - 1].indexOf('{');

		String metric = null;
		Map<String, String> tags = null;
		if (idx >= 0) {
			tags = parseTags(terms[terms.length - 1].substring(idx).trim());
			metric = terms[terms.length - 1].substring(0, idx);
		} else {
			tags = new HashMap<String, String>();
			metric = terms[terms.length - 1];
		}

		Aggregator aggregator = DEFAULT_AGGREGATOR;
		if (terms.length > 1) {
			aggregator = Aggregator.valueOf(terms[0].trim());
		}
		boolean rate = false;
		String downsample = null;
		if (terms.length > 2) {
			if ("rate".equals(terms[1].trim())) {
				rate = true;
				if (terms.length > 3) {
					downsample = terms[2].trim();
				}
			} else {
				downsample = terms[1].trim();
				if (terms.length > 3 && "rate".equals(terms[1].trim())) {
					rate = true;
				}
			}
		}

		return new MetricQuery(aggregator, downsample, rate, metric, tags);
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		if (aggregator != null) {
			buf.append(aggregator).append(':');
		}
		if (downsample != null) {
			buf.append(downsample).append(':');
		}
		if (rate) {
			buf.append("rate:");
		}
		buf.append(metric);
		if (tags.size() > 0) {
			buf.append('{');
			boolean comma = false;
			for (Map.Entry<String, String> tag : tags.entrySet()) {
				if (comma) {
					buf.append(',');
				}
				comma = true;
				buf.append(tag.getKey()).append('=').append(tag.getValue());
			}
			buf.append('}');
		}
		return buf.toString();
	}

	/**
	 * @return the aggregator
	 */
	public final Aggregator getAggregator() {
		return aggregator;
	}

	/**
	 * @return the downsample
	 */
	public final String getDownsample() {
		return downsample;
	}

	/**
	 * @return the rate
	 */
	public final boolean isRate() {
		return rate;
	}

	/**
	 * @return the metric
	 */
	public final String getMetric() {
		return metric;
	}

	/**
	 * @return the tags
	 */
	public final Map<String, String> getTags() {
		return tags;
	}
}
