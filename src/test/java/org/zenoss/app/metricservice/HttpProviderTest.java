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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Optional;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.zenoss.app.metricservice.api.configs.MetricServiceConfig;
import org.zenoss.app.metricservice.api.impl.mocks.MockMetricStorage;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("prod")
public class HttpProviderTest extends ProviderTestBase {
    @Autowired
    ApplicationContext ctx;

    @Configuration
    @ComponentScan(basePackages = { "org.zenoss.app" })
    static class ContextConfiguration {
        @Bean
        public MetricServiceAppConfiguration getQueryAppConfiguration() {
            MetricServiceAppConfiguration config = new MetricServiceAppConfiguration();
            config.setAuthEnabled(false);

            // Set up the config so that it contacts our mock server for tsdb
            // queries
            config.getMetricServiceConfig().setOpenTsdbUrl(
                    "http://localhost:8089");
            return config;
        }
    }

    // Create a mock http server to stand in for our tsdb server
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration
            .wireMockConfig().port(8089));

    protected Map<?, ?> testQuery(Optional<String> id, Optional<String> start,
            Optional<String> end, Optional<ReturnSet> returnset,
            Optional<Boolean> series, Optional<String> downsample,
            Optional<Map<String, List<String>>> globalTags, String[] queries)
            throws Exception {
        MetricServiceAppConfiguration config = new ContextConfiguration()
                .getQueryAppConfiguration();
        MetricServiceConfig pref = config.getMetricServiceConfig();

        // Create some mock data for the request. This mock data will be
        // generated based on the request.
        List<MetricSpecification> queryList = new ArrayList<>();
        for (String query : queries) {
            queryList.add(MetricSpecification.fromString(query));
        }
        byte[] data = new MockMetricStorage().generateData(
                new ContextConfiguration().getQueryAppConfiguration(),
                id.or("not-specified"), start.or(pref.getDefaultStartTime()),
                end.or(pref.getDefaultEndTime()),
                returnset.or(pref.getDefaultReturnSet()),
                series.or(pref.getDefaultSeries()), downsample.orNull(),
                globalTags.orNull(), queryList);

        WireMock.stubFor(WireMock.head(WireMock.urlMatching(".*")).willReturn(
                WireMock.aResponse().withStatus(200)
                        .withHeader("Date", "Tue, 30 Apr 2013 14:12:34 GMT")));
        WireMock.stubFor(WireMock.get(WireMock.urlMatching("/q.*")).willReturn(
                WireMock.aResponse().withStatus(200)
                        .withHeader("Content-type", "text/plain")
                        .withHeader("Date", "Tue, 30 Apr 2013 14:12:34 GMT")
                        .withBody(new String(data))));
        return super.testQuery(id, start, end, returnset, series, downsample,
                globalTags, queries);
    }
}
