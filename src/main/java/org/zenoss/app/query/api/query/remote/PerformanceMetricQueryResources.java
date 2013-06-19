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
package org.zenoss.app.query.api.query.remote;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.query.api.MetricQuery;
import org.zenoss.app.query.api.PerformanceMetricQueryAPI;
import org.zenoss.dropwizardspring.annotations.Resource;

import com.google.common.base.Optional;
import com.yammer.metrics.annotation.Timed;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@Resource
// Annotation ensures it is loaded and registered via Spring
@Path("/query")
@Produces(MediaType.APPLICATION_JSON)
public class PerformanceMetricQueryResources {

    @Autowired
    PerformanceMetricQueryAPI api;

    @Path("/performance")
    @Timed
    @GET
    public Response query(@QueryParam("id") Optional<String> id,
            @QueryParam("query") List<MetricQuery> queries,
            @QueryParam("start") Optional<String> startTime,
            @QueryParam("end") Optional<String> endTime,
            @QueryParam("tz") Optional<String> tz,
            @QueryParam("exact") Optional<Boolean> exactTimeWindow,
            @QueryParam("series") Optional<Boolean> series) {

        return api.query(id, startTime, endTime, tz, exactTimeWindow, series,
                queries);
    }
}