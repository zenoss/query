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
package org.zenoss.app.metricservice.api.impl;

import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.ChartServiceAPI;
import org.zenoss.app.metricservice.api.model.Chart;
import org.zenoss.app.metricservice.api.model.ChartList;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;

/**
 * @author david
 * 
 */
@API
@Configuration
public class ChartService implements ChartServiceAPI {

    @Autowired
    MetricServiceAppConfiguration config;

    @Autowired
    ResourcePersistenceAPI persistence;

    private final ObjectMapper objectMapper;

    private static final String ZEN_CHART = "chart";

    private static final Logger log = LoggerFactory
            .getLogger(ChartService.class);

    public ChartService() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        objectMapper.enable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    private void connect() {
        if (!persistence.isConnected()) {
            synchronized (this) {
                if (!persistence.isConnected()) {
                    String[] parts = config.getChartServiceConfig()
                            .getRedisConnection().split(":");
                    if (parts.length == 1) {
                        persistence.connect(ZEN_CHART, parts[0]);
                    } else if (parts.length == 2) {
                        persistence.connect(ZEN_CHART, parts[0],
                                Integer.parseInt(parts[1]));
                    } else {
                        // An error, what was put as the connection string?!?
                        log.error(
                                "Invalid connection string specified ({}), should be host:port; will attempt to connect on localhost.",
                                config.getChartServiceConfig()
                                        .getRedisConnection());
                        persistence.connect(ZEN_CHART, "localhost");
                    }
                    try {
                        persistence.ping();
                    } catch (Throwable t) {
                        try {
                            persistence.disconnect();
                        } catch (Throwable ignore) {
                            // ignore
                        }
                        throw new WebApplicationException(
                                Utils.getErrorResponse(
                                        Utils.NOT_SPECIFIED,
                                        500,
                                        "Unable to connect to resource storage",
                                        t.getMessage()));
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#get()
     */
    @Override
    public Response get(String id) {
        try {
            connect();
            String content = persistence.getResource(id);

            if (content == null) {
                throw new WebApplicationException(Response.status(404).build());
            }

            Chart chart = objectMapper.readValue(content, Chart.class);

            return Response.ok(chart).build();

        } catch (WebApplicationException w) {
            throw w;
        } catch (Throwable t) {
            throw new WebApplicationException(Utils.getErrorResponse(
                    Utils.NOT_SPECIFIED, 500, "Unable to create resource",
                    t.getMessage()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#delete()
     */
    @Override
    public Response delete(String id) {
        try {
            connect();
            if (!persistence.delete(id)) {
                return Response.status(404).build();
            }
            return Response.noContent().build();
        } catch (Throwable t) {
            throw new WebApplicationException(Utils.getErrorResponse(
                    Utils.NOT_SPECIFIED, 500, "Unable to delete resource",
                    t.getMessage()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#post()
     */
    @Override
    public Response post(Chart chart) {
        try {
            connect();
            String uuid = Utils.createUuid();
            String content = objectMapper.writeValueAsString(chart);
            if (!persistence.add(uuid, content)) {
                // Attempt to clean up partials
                try {
                    // persistence.delete(uuid);
                } catch (Throwable t) {
                    // ignore;
                }
                log.error("Error while creating resource");
                throw new WebApplicationException(Utils.getErrorResponse(null,
                        500, "Unable to create new resource", "unknown error"));
            }

            return Response.created(new URI("/" + uuid)).build();
        } catch (Throwable t) {
            log.error("Error while creating resource", t);
            throw new WebApplicationException(Utils.getErrorResponse(null, 500,
                    "Unable to create new resource", t.getMessage()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#put()
     */
    @Override
    public Response put(String id, Chart chart) {
        try {
            connect();
            // Check to see if the specified ID exists
            if (!persistence.exists(id)) {
                // Not found, and we don't allow clients to pick there own
                // UUIDs, so return not found.
                return Response.status(404).build();
            }
            String content = objectMapper.writeValueAsString(chart);
            persistence.update(id, content);
            return Response.status(204).build();
        } catch (WebApplicationException w) {
            throw w;
        } catch (Throwable t) {
            throw new WebApplicationException(Utils.getErrorResponse(
                    Utils.NOT_SPECIFIED, 500, "Unable to create resource",
                    t.getMessage()));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#getList()
     */
    @Override
    public Response getList(Optional<Integer> start, Optional<Integer> end, Optional<Boolean> includeCount) {
        connect();
        
        ChartList list = new ChartList();
        list.setStart(start.or(0));
        list.setEnd(end.or(list.getStart() + 9));
        list.setCount(includeCount.or(false) ? persistence.count() : null);
        list.setIds(persistence.range(list.getStart(), list.getEnd()));
        return Response.ok(list).build();
    }
}
