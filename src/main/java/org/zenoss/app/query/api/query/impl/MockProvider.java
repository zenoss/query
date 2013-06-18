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
package org.zenoss.app.query.api.query.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;

import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class MockProvider implements StreamingOutput {

	private final String id;
	private final String startTime;
	private final String endTime;
	private final String tz;
	private final Boolean exactTimeWindow;
	private final List<MetricQuery> queries;

	public MockProvider(QueryAppConfiguration config, Optional<String> id,
			Optional<String> startTime, Optional<String> endTime,
			Optional<String> tz, Optional<Boolean> exactTimeWindow,
			List<MetricQuery> queries) {
		this.id = id.or("not-specified");
		this.startTime = startTime.or(config.getPerformanceMetricQueryConfig()
				.getDefaultStartTime());
		this.endTime = endTime.or(config.getPerformanceMetricQueryConfig()
				.getDefaultEndTime());
		this.tz = tz.or(config.getPerformanceMetricQueryConfig()
				.getDefaultTimeZone());
		this.exactTimeWindow = exactTimeWindow.or(config
				.getPerformanceMetricQueryConfig().getDefaultExactTimeWindow());
		this.queries = queries;
	}

	private long parseDuration(String v) {
		char last = v.charAt(v.length() - 1);

		int period = 0;
		try {
			period = Integer.parseInt(v.substring(0, v.length() - 1));
		} catch (NumberFormatException e) {
			return 0;
		}

		switch (last) {
		case 's':
			return period;
		case 'm':
			return period * 60;
		case 'h':
			return period * 60 * 60;
		case 'd':
			return period * 60 * 60 * 24;
		case 'w':
			return period * 60 * 60 * 24 * 7;
		case 'y':
			return period * 60 * 60 * 24 * 265;
		}

		return 0;
	}

	private long parseDate(String value) {
		String v = value.trim();

		if ("now".equals(v)) {
			return new Date().getTime() / 1000;
		} else if (v.endsWith("-ago")) {
			return new Date().getTime() / 1000
					- parseDuration(v.substring(0, v.length() - 4));
		}
		try {
			return new SimpleDateFormat().parse(v).getTime() / 1000;
		} catch (ParseException e) {
			return new Date().getTime() / 1000;
		}
	}

	private void generateData(JsonWriter writer, long start, long end,
			Map<String, String> tags) throws IOException {
		long dur = end - start;
		long step = 0;
		if (dur < 60) {
			step = 1;
		} else if (dur < 60 * 60) {
			step = 5;
		} else {
			step = 15;
		}

		// Return an array of results from 0.0 to 1.0 equally distributed over
		// the time range with 1 second steps.
		double inc = 1.0 / (double) (dur / step);
		double val = 0.0;

		for (long i = start; i <= end; i += step, val += inc) {
			writer.objectS().value("timestamp", i, true)
					.value("value", val, true).objectS("tags");
			int count = tags.size();
			for (Map.Entry<String, String> entry : tags.entrySet()) {
				writer.value(entry.getKey(), entry.getValue(), (--count) > 0);
			}
			writer.objectE().objectE(i < end);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
	 */
	@Override
	public void write(OutputStream output) throws IOException,
			WebApplicationException {
		try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(output))) {
			writer.objectS().value("clientId", id, true)
					.value("startTime", startTime, true)
					.value("endTime", endTime, true)
					.value("timeZone", tz, true)
					.value("exactTimeWindow", exactTimeWindow, true)
					.arrayS("results");
			long start = parseDate(startTime);
			long end = parseDate(endTime);
			for (MetricQuery query : queries) {
				writer.objectS()
						.value("aggregator", query.getAggregator().toString(),
								true)
						.value("rate", query.isRate(), true)
						.value("downsample",
								(query.getDownsample() == null ? "not-specified"
										: query.getDownsample()), true)
						.value("metric", query.getMetric(), true)
						.arrayS("datapoints");
				generateData(writer, start, end, query.getTags());
				writer.arrayE().objectE();
			}
			writer.arrayE().objectE();
			writer.flush();
		}
	}
}
