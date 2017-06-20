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

package org.zenoss.app.metricservice.v2.remote;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yammer.metrics.annotation.Timed;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonEncoding;

import org.zenoss.app.AppConfiguration;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.v2.*;
import org.zenoss.app.metricservice.v2.QueryService;
import org.zenoss.app.security.ZenossTenant;
import org.zenoss.app.zauthbundle.ZappSecurity;
import org.zenoss.dropwizardspring.annotations.Resource;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;
import java.util.Map;
import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;

@Resource(name = "v2query")
@Path("/api/v2/performance")
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
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult query(@Valid MetricRequest metricRequest) {

        for (MetricQuery mq : metricRequest.getQueries()) {
            Map<String, List<String>> tags = addTenantId(mq.getTags());
            mq.setTags(tags);
        }
        QueryResult result = api.query(metricRequest);
        return result;
    }

    /**
     * This method handles requests sent to the rename endpoint. The endpoint
     * can be used for renaming metric names or tag values. It can rename not
     * only an entity that matches wholly a word but also a list of entities
     * that shares a common prefix.
     * @param renameRequest The request includes information such as the names
     * before and after rename, the type of the entity that is going to be
     * renamed, the type of pattern search.
     * @return Response The result of each rename task is streamed back to the
     * requester.
     */
    @POST
    @Path("/rename")
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rename(@Valid final RenameRequest renameRequest){
        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream out)
                throws IOException, WebApplicationException {

                Writer writer = new BufferedWriter(new OutputStreamWriter(out));
                api.rename(renameRequest, writer);
                writer.flush();
            }
        };
        return  Response.ok(stream).header("X-Accel-Buffering","no").build();
    }


    String getTenantId() {
        Subject subject = security.getSubject();
        PrincipalCollection principals = subject.getPrincipals();
        ZenossTenant tenant = principals.oneByType(ZenossTenant.class);
        return tenant.id();
    }

    Map<String, List<String>> addTenantId(Map<String, List<String>> tags) {
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
