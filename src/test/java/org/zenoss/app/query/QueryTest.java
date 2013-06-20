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
package org.zenoss.app.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.zenoss.app.query.api.Aggregator;
import org.zenoss.app.query.api.MetricQuery;
import org.zenoss.app.query.api.query.impl.MockPerformanceMetricQueryAPIImpl;
import org.zenoss.app.query.api.query.remote.PerformanceMetricQueryResources;

import com.google.common.base.Optional;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.json.ObjectMapperFactory;
import com.yammer.dropwizard.testing.ResourceTest;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        org.zenoss.app.query.api.query.remote.PerformanceMetricQueryResources.class,
        org.zenoss.app.query.api.query.impl.MockPerformanceMetricQueryAPIImpl.class }, loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("dev")
@Configuration
@ComponentScan(basePackages = { "org.zenoss.app",
        "org.zenoss.app.query.api.query.remote",
        "org.zenoss.app.query.api.query.impl",
        "org.zenoss.app.query.api.query.configs",
        "org.zenoss.app.query.api.query.api" })
@Configurable(autowire = Autowire.BY_NAME)
@TestExecutionListeners(inheritListeners = true)
public class QueryTest extends ResourceTest {
    // private final QueryApp app = Mockito.mock(QueryApp.class);
    private final Environment environment = Mockito.mock(Environment.class);
    private QueryApp app = new QueryApp();
    private final PerformanceMetricQueryResources impl = Mockito.mock(PerformanceMetricQueryResources.class);

    // private final Bootstrap<QueryAppConfiguration> bconfig = new
    // Bootstrap<QueryAppConfiguration>(app);
    private final QueryAppConfiguration config = new QueryAppConfiguration();

    @Before
    public void setup() throws Exception {
        ObjectMapperFactory omf = new ObjectMapperFactory();
        Mockito.when(environment.getObjectMapperFactory()).thenReturn(omf);
    }

    @Test
    public void buildsAThingResource() throws Exception {

        app.run(config, environment);
        PerformanceMetricQueryResources pqr = client().resource("/query/performance").get(PerformanceMetricQueryResources.class);
        //AsyncWebResource awr = client().asyncResource("/query/performance");
        //PerformanceMetricQueryResources pqr = awr.get(PerformanceMetricQueryResources.class);

         List<MetricQuery> queries = new ArrayList<MetricQuery>();
         queries.add(new MetricQuery(Aggregator.avg, "", false, "laLoadInt1",
         new HashMap<String, String>()));
        //
        // PerformanceMetricQueryResources pmqr = new
        // PerformanceMetricQueryResources();
        pqr.query(Optional.of("sample"), queries, Optional.of("1m-ago"),
                Optional.of("now"), Optional.of(true), Optional.of(false));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.yammer.dropwizard.testing.ResourceTest#setUpResources()
     */
    // @Override
    protected void setUpResources() throws Exception {
        addResource(new PerformanceMetricQueryResources());
    }
}