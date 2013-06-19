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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

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
@Profile({ "default", "prod" })
public class OpenTSDBPerformanceMetricQueryAPIImpl extends
		BasePerformanceMetricQueryAPIImpl {
	@Autowired
	QueryAppConfiguration config;

	private static final String SOURCE_ID = "OpenTSDB";

	protected BufferedReader getReader(QueryAppConfiguration config, String id,
			String startTime, String endTime, String tz,
			Boolean exactTimeWindow, Boolean series, List<MetricQuery> queries)
			throws IOException {
		StringBuilder buf = new StringBuilder(config
				.getPerformanceMetricQueryConfig().getOpenTsdbUrl());
		buf.append("/q?");
		if (!NOW.equals(startTime)) {
			buf.append("start=").append(startTime);
		}
		if (!NOW.equals(endTime)) {
			buf.append("&end=").append(endTime);
		}
		for (MetricQuery query : queries) {
			buf.append("&m=").append(query.toString());
		}
		buf.append("&ascii");

		return new BufferedReader(new InputStreamReader(
				new URL(buf.toString()).openStream()));
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
