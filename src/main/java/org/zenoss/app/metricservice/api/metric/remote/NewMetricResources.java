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
package org.zenoss.app.metricservice.api.metric.remote;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.annotation.Timed;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.MetricServiceAPI;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.PerformanceQuery;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;
import org.zenoss.dropwizardspring.annotations.Resource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 */
@Resource(name = "query")
@Path("/api/performance/query2")
@Produces(MediaType.APPLICATION_JSON)
public class NewMetricResources {

    private static final Logger log = LoggerFactory.getLogger(NewMetricResources.class);

    @Autowired
    AppConfiguration configuration;

    @Autowired
    ZappSecurity security;

    @Autowired(required = true)
    MetricServiceAPI api;

    public NewMetricResources() {
    }

    public NewMetricResources(AppConfiguration configuration, ZappSecurity security, MetricServiceAPI api) {
        log.info("MetricResources constructor starting...");
        this.configuration = configuration;
        this.security = security;
        this.api = api;
    }

    @GET
    @Timed
    public Response query(@QueryParam("id") Optional<String> id,
                          @QueryParam("query") List<MetricSpecification> queries,
                          @QueryParam("start") Optional<String> startTime,
                          @QueryParam("end") Optional<String> endTime,
                          @QueryParam("returnset") Optional<ReturnSet> returnset,
                          @QueryParam("series") Optional<Boolean> series) {
        System.out.println("MetricResources.query - GET");
        log.info("Entered MetricResources.query with multiple params (GET).");
        Optional<Map<String, List<String>>> tags = getTags( null);
        return api.query(id, startTime, endTime, returnset, series,
                Optional.<String>absent(), Optional.<String>absent(),
                tags, queries);
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public Response query2(PerformanceQuery query) {
        System.out.println("NewMetricResources.query - POST");
        log.info("Entered NewMetricResources.query with single param (POST).");
        if (query == null) {
            return Utils.getErrorResponse(null, 400,
                    "Received an empty query request", "Empty Request");
        }
        Optional<String> id = Optional.absent();
        Optional<String> start = Optional.fromNullable(query
                .getStart());
        Optional<String> end = Optional.fromNullable(query.getEnd());
        Optional<ReturnSet> returnset = Optional.fromNullable(query
                .getReturnset());
        Optional<Boolean> series = Optional.fromNullable(query
                .getSeries());
        Optional<String> downsample = Optional.fromNullable(query
                .getDownsample());
        Optional<String> grouping = Optional.fromNullable(query
                .getGrouping());
        Optional<Map<String, List<String>>> tags = getTags( query.getTags());
        return api.newQuery(id, start, end, returnset, series, downsample,
                grouping, tags, query.getMetrics());
    }

    @OPTIONS
    public Response handleOptions(@HeaderParam("Access-Control-Request-Headers")  String request) {
        return api.options(request);
    }

    String getTenantId() {
        Subject subject = security.getSubject();
        PrincipalCollection principles = subject.getPrincipals();
        ZenossTenant tenant = principles.oneByType(ZenossTenant.class);
        return tenant.id();
    }

    Optional<Map<String, List<String>>> getTags(Map<String, List<String>> tags) {
        if (configuration.isAuthEnabled()) {
            if (tags == null) {
                tags = Maps.newHashMap();
            }
            String tenantId = getTenantId();
            tags.put("zenoss_tenant_id", Lists.newArrayList(tenantId));
        }

        return Optional.fromNullable(tags);
    }
}
