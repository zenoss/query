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
package org.zenoss.app.metricservice;

import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;
import org.zenoss.app.metricservice.api.model.ReturnSet;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class MetricServiceConfigTest {

    private static final String START_TIME = "2013/04/30-07:00:00-GMT";
    private static final String END_TIME = "2013/04/30-08:00:00-GMT";
    private static final ReturnSet RETURN_SET = ReturnSet.EXACT;
    private static final boolean SERIES = false;
    private static final String TSDB_TZ = "UTC";
    private static final String TSDB_URL = "http://localhost:4242";
    private static final int CONN_TIMEOUT_MS = 1000;
    private static final int EXEC_THREAD_POOL_MAX_SIZE = 37;
    private static final int EXEC_THREAD_POOL_CORE_SIZE = 17;
    private static final int MAX_POOL_CONNECTIONS_PER_ROUTE = 20;
    private static final int MAX_TOTAL_POOL_CONNECTIONS = 234;
    private static final Boolean SEND_RATE_OPTIONS = true;

    @Test
    public void testPerformanceMetricQueryConfig() {
        MetricServiceConfig config = new MetricServiceConfig();
        config.setDefaultStartTime(START_TIME);
        config.setDefaultEndTime(END_TIME);
        config.setDefaultReturnSet(RETURN_SET);
        config.setDefaultSeries(SERIES);
        config.setDefaultTsdTimeZone(TSDB_TZ);
        config.setOpenTsdbUrl(TSDB_URL);
        config.setConnectionTimeoutMs(CONN_TIMEOUT_MS);
        config.setExecutorThreadPoolMaxSize(EXEC_THREAD_POOL_MAX_SIZE);
        config.setExecutorThreadPoolCoreSize(EXEC_THREAD_POOL_CORE_SIZE);
        config.setMaxPoolConnectionsPerRoute(MAX_POOL_CONNECTIONS_PER_ROUTE);
        config.setMaxTotalPoolConnections(MAX_TOTAL_POOL_CONNECTIONS);
        config.setSendRateOptions(SEND_RATE_OPTIONS);

        Assert.assertEquals(START_TIME, config.getDefaultStartTime());
        Assert.assertEquals(END_TIME, config.getDefaultEndTime());
        Assert.assertEquals(RETURN_SET, config.getDefaultReturnSet());
        Assert.assertEquals(SERIES, config.getDefaultSeries());
        Assert.assertEquals(TSDB_TZ, config.getDefaultTsdTimeZone());
        Assert.assertEquals(TSDB_URL, config.getOpenTsdbUrl());
        Assert.assertEquals(CONN_TIMEOUT_MS, config.getConnectionTimeoutMs());
        Assert.assertEquals(EXEC_THREAD_POOL_MAX_SIZE, config.getExecutorThreadPoolMaxSize());
        Assert.assertEquals(EXEC_THREAD_POOL_CORE_SIZE, config.getExecutorThreadPoolCoreSize());
        Assert.assertEquals(MAX_POOL_CONNECTIONS_PER_ROUTE, config.getMaxPoolConnectionsPerRoute());
        Assert.assertEquals(MAX_TOTAL_POOL_CONNECTIONS, config.getMaxTotalPoolConnections());
        Assert.assertEquals(SEND_RATE_OPTIONS, config.getSendRateOptions());
    }

    @Test
    public void testPerformanceMetricQueryConfigStripsTrailingSlashFromURL() {
        MetricServiceConfig config = new MetricServiceConfig();
        config.setOpenTsdbUrl(TSDB_URL + '/');
        Assert.assertEquals(TSDB_URL, config.getOpenTsdbUrl());
    }
}
