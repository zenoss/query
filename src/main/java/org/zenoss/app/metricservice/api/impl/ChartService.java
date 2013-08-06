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
    ResourcePersistenceFactoryAPI persistenceFactory;

    private final ObjectMapper objectMapper;

    private static final String ZEN_CHART = "chart";

    private static final Logger log = LoggerFactory
            .getLogger(ChartService.class);

    // private ResourcePersistenceAPI persistence = null;

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

    /**
     * Utilized to encapsulate the execution of the specific resource logic in
     * resource and exception handlers.
     */
    private interface Worker {
        /**
         * Execute the resource method logic with the persistence API while
         * doing proper resource and exeception handling.
         * 
         * @param api
         *            handle to the persistence
         * @return resource response
         * @throws Exception
         *             any exception thrown but the logic.
         */
        public Response execute(ResourcePersistenceAPI api) throws Exception;
    }

    /**
     * Execute some business logic wrapped by proper resource handling of the
     * persistence reference and common exception handling.
     * 
     * @param worker
     *            the worker logic to execute
     * @return the response from the worker or an exception
     * @throws WebApplicationException
     *             if the worker throws an exception
     */
    private Response execute(Worker worker) throws WebApplicationException {
        ResourcePersistenceAPI api = null;
        try {

            // Get a handle to the persistence and fire of the work.
            api = persistenceFactory.getInstance(ZEN_CHART);
            return worker.execute(api);
        } catch (WebApplicationException w) {
            throw w;
        } catch (Exception e) {
            // Turn the exception into a WebApplication Exception
            throw new WebApplicationException(Utils.getErrorResponse(
                    Utils.NOT_SPECIFIED, 500,
                    "Exception while accessing resource", e.getMessage()));
        } finally {

            // Clean up the reference to the persistence
            if (api != null) {
                persistenceFactory.returnInstance(api);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#get()
     */
    @Override
    public Response get(final String id) {
        return execute(new Worker() {
            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                String content = api.getResourceById(id);

                if (content == null) {
                    throw new WebApplicationException(Response.status(404)
                            .build());
                }

                Chart chart = objectMapper.readValue(content, Chart.class);

                return Response.ok(chart).build();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#delete()
     */
    @Override
    public Response delete(final String id) {
        return execute(new Worker() {
            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                if (!api.delete(id)) {
                    return Response.status(404).build();
                }
                return Response.noContent().build();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#post()
     */
    @Override
    public Response post(final Chart chart) {
        return execute(new Worker() {

            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                String uuid = Utils.createUuid();
                String content = objectMapper.writeValueAsString(chart);
                if (!api.add(uuid, content)) {
                    // Attempt to clean up partials
                    try {
                        // persistence.delete(uuid);
                    } catch (Exception e) {
                        // ignore;
                    }
                    log.error("Error while creating resource");
                    throw new WebApplicationException(Utils.getErrorResponse(
                            null, 500, "Unable to create new resource",
                            "unknown error"));
                }

                return Response.created(new URI("/" + uuid)).build();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#put()
     */
    @Override
    public Response put(final String id, final Chart chart) {
        return execute(new Worker() {

            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                if (!api.exists(id)) {
                    // Not found, and we don't allow clients to pick there own
                    // UUIDs, so return not found.
                    return Response.status(404).build();
                }
                String content = objectMapper.writeValueAsString(chart);
                api.update(id, content);
                return Response.status(204).build();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.query.api.ChartAPI#getList()
     */
    @Override
    public Response getList(final Optional<Integer> start,
            final Optional<Integer> end, final Optional<Boolean> includeCount) {
        return execute(new Worker() {

            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                ChartList list = new ChartList();
                list.setStart(start.or(0));
                list.setEnd(end.or(list.getStart() + 9));
                list.setCount(includeCount.or(false) ? api.count() : null);
                list.setIds(api.range(list.getStart(), list.getEnd()));
                return Response.ok(list).build();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.api.ChartServiceAPI#getByName(java.lang.
     * String)
     */
    @Override
    public Response getByName(final String name) {
        return execute(new Worker() {

            @Override
            public Response execute(ResourcePersistenceAPI api)
                    throws Exception {
                String content = api.getResourceByName(name);

                if (content == null) {
                    throw new WebApplicationException(Utils.getErrorResponse(
                            null, 404, "Chart not found", "Not Found"));
                }

                Chart chart = objectMapper.readValue(content, Chart.class);

                return Response.ok(chart).build();
            }
        });
    }
}
