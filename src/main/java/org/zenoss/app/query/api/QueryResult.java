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

import java.util.Collections;
import java.util.List;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class QueryResult {
	private final Aggregator aggregator;
	private final boolean rate;
	private final String downsample;
	private final String metric;
	private final List<DataPoint> datapoints;

	public QueryResult(Aggregator aggregator, boolean rate, String downsample,
			String metric, List<DataPoint> datapoints) {
		this.aggregator = aggregator;
		this.rate = rate;
		this.downsample = downsample;
		this.metric = metric;
		this.datapoints = datapoints;
	}

	/**
	 * @return the aggregator
	 */
	public final Aggregator getAggregator() {
		return aggregator;
	}

	/**
	 * @return the rate
	 */
	public final boolean isRate() {
		return rate;
	}

	/**
	 * @return the downsample
	 */
	public final String getDownsample() {
		return downsample;
	}

	/**
	 * @return the metric
	 */
	public final String getMetric() {
		return metric;
	}

	/**
	 * @return the datapoints
	 */
	public final List<DataPoint> getDatapoints() {
		return Collections.unmodifiableList(datapoints);
	}
}
