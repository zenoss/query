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
package org.zenoss.app.metricservice.v2.remote;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.annotation.Timed;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.impl.OpenTSDBQueryResult;
import org.zenoss.app.metricservice.api.model.v2.MetricQuery;
import org.zenoss.app.metricservice.api.model.v2.MetricRequest;
import org.zenoss.app.metricservice.v2.QueryService;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;
import org.zenoss.dropwizardspring.annotations.Resource;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Resource(name = "v2query")
@Path("/api/v2/metric")
@Produces(MediaType.APPLICATION_JSON)
public class Resources {

    private static final Logger log = LoggerFactory.getLogger(Resources.class);

    @Autowired
    AppConfiguration configuration;

    @Autowired
    ZappSecurity security;

    @Autowired(required = true)
    QueryService api;

    public Resources() {
    }

    @POST
    @Path("/query")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    public Iterable<OpenTSDBQueryResult> query(@Valid MetricRequest metricRequest) {

        for (MetricQuery mq : metricRequest.getQueries()) {
            Map<String, List<String>> tags = addTentanId(mq.getTags());
            mq.setTags(tags);
        }
        Iterable<OpenTSDBQueryResult> result = api.query(metricRequest);
        return result;
    }


    String getTenantId() {
        Subject subject = security.getSubject();
        PrincipalCollection principals = subject.getPrincipals();
        ZenossTenant tenant = principals.oneByType(ZenossTenant.class);
        return tenant.id();
    }

    Map<String, List<String>> addTentanId(Map<String, List<String>> tags) {
        if (tags == null) {
            tags = Maps.newHashMap();
        }
        if (configuration.isAuthEnabled()) {
            String tenantId = getTenantId();
            tags.put("zenoss_tenant_id", Lists.newArrayList(tenantId));
        }

        return tags;
    }
}
