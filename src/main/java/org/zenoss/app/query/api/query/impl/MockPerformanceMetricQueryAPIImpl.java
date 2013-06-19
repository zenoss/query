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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
@Profile("dev")
public class MockPerformanceMetricQueryAPIImpl extends
		BasePerformanceMetricQueryAPIImpl {

	private static final String SOURCE_ID = "mock";
	private static final String MOCK_VALUE = "mock-value";
	private static final char EQ = '=';
	private static final char LF = '\n';

	@Autowired
	QueryAppConfiguration config;

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

		if (NOW.equals(v)) {
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

	protected BufferedReader getReader(QueryAppConfiguration config, String id,
			String startTime, String endTime, String tz,
			Boolean exactTimeWindow, Boolean series, List<MetricQuery> queries)
			throws IOException {
		StringBuilder buf = new StringBuilder();
		long start = parseDate(startTime);
		long end = parseDate(endTime);
		long dur = end - start;
		long step = 0;
		if (dur < 60) {
			step = 1;
		} else if (dur < 60 * 60) {
			step = 5;
		} else {
			step = 15;
		}

		// Return an array of results from 0.0 to 1.0 equally distributed
		// over the time range with 1 second steps.
		double inc = 1.0 / (double) (dur / step);

		for (MetricQuery query : queries) {
			double val = 0.0;
			for (long i = start; i <= end; i += step, val += inc) {
				buf.append(query.getMetric()).append(' ');
				buf.append(i).append(' ');
				buf.append(val);

				for (Map.Entry<String, String> tag : query.getTags().entrySet()) {
					buf.append(' ').append(tag.getKey()).append(EQ)
							.append(MOCK_VALUE);
				}
				buf.append(LF);
			}
		}
		return new BufferedReader(new StringReader(buf.toString()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zenoss.app.query.api.query.impl.BasePerformanceMetricQueryAPIImpl
	 * #getConfiguration()
	 */
	@Override
	protected QueryAppConfiguration getConfiguration() {
		return config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zenoss.app.query.api.query.impl.BasePerformanceMetricQueryAPIImpl
	 * #getSourceId()
	 */
	@Override
	protected String getSourceId() {
		return SOURCE_ID;
	}
}
