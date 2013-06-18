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
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;
import org.zenoss.app.query.api.PerformanceMetricQueryAPI;

import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
@Profile({ "default", "prod" })
public class OpenTSDBPerformanceMetricQueryAPIImpl extends
		BasePerformanceMetricQueryAPIImpl implements PerformanceMetricQueryAPI {
	@Autowired
	QueryAppConfiguration config;

	private static final String SOURCE_ID = "OpenTSDB";

	private class Worker implements StreamingOutput {

		private final String id;
		private final String startTime;
		private final String endTime;
		private final String tz;
		private final Boolean exactTimeWindow;
		private final List<MetricQuery> queries;

		public Worker(QueryAppConfiguration config, String id,
				String startTime, String endTime, String tz,
				Boolean exactTimeWindow, List<MetricQuery> queries) {
			this.id = id;
			this.startTime = startTime;
			this.endTime = endTime;
			this.tz = tz;
			this.exactTimeWindow = exactTimeWindow;
			this.queries = queries;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
		 */
		@Override
		public void write(OutputStream output) throws IOException,
				WebApplicationException {
			try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					output))) {
				writer.objectS().value(CLIENT_ID, id, true)
						.value(SOURCE, SOURCE_ID, true)
						.value(START_TIME, startTime, true)
						.value(END_TIME, endTime, true)
						.value(TIME_ZONE, tz, true)
						.value(EXACT_TIME_WINDOW, exactTimeWindow, true)
						.arrayS(RESULTS);
				for (MetricQuery query : queries) {
					writer.objectS()
							.value(AGGREGATOR,
									query.getAggregator().toString(), true)
							.value(RATE, query.isRate(), true)
							.value(DOWNSAMPLE,
									(query.getDownsample() == null ? NOT_SPECIFIED
											: query.getDownsample()), true)
							.value(METRIC, query.getMetric(), true)
							.arrayS(DATAPOINTS);
					writer.arrayE().objectE();
				}
				writer.arrayE().objectE();
				writer.flush();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zenoss.app.query.api.PerformanceMetricQueryAPI#query(com.google.common
	 * .base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional, java.util.List)
	 */
	@Override
	public Response query(Optional<String> id, Optional<String> startTime,
			Optional<String> endTime, Optional<String> tz,
			Optional<Boolean> exactTimeWindow, List<MetricQuery> queries) {

		return Response
				.ok(new Worker(config, id.or(NOT_SPECIFIED), startTime
						.or(config.getPerformanceMetricQueryConfig()
								.getDefaultStartTime()),
						endTime.or(config.getPerformanceMetricQueryConfig()
								.getDefaultEndTime()), tz.or(config
								.getPerformanceMetricQueryConfig()
								.getDefaultTimeZone()), exactTimeWindow
								.or(config.getPerformanceMetricQueryConfig()
										.getDefaultExactTimeWindow()), queries))
				.build();
	}
}
