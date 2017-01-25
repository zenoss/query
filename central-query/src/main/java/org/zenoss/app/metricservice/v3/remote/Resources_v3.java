/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2015, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */

package org.zenoss.app.metricservice.v3.remote;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.annotation.Timed;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.model.v3.MetricQuery;
import org.zenoss.app.metricservice.api.model.v3.MetricRequest;
import org.zenoss.app.metricservice.api.model.v3.QueryResult;
import org.zenoss.app.metricservice.v3.QueryService;
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

@Resource(name = "v3query")
@Path("/api/v3/performance")
@Produces(MediaType.APPLICATION_JSON)
public class Resources_v3 {

    private static final Logger log = LoggerFactory.getLogger(Resources_v3.class);

    @Autowired
    AppConfiguration configuration;

    @Autowired
    ZappSecurity security;

    @Autowired(required = true)
    QueryService api;

    public Resources_v3() {
    }

    @POST
    @Path("/query")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult query(@Valid MetricRequest metricRequest) {

        for (MetricQuery mq : metricRequest.getQueries()) {
            Map<String, List<String>> tags = addTentanId(mq.getTags());
            mq.setTags(tags);
        }
        QueryResult result = api.query(metricRequest);
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
