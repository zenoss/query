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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;

import redis.clients.jedis.JedisPool;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
@Configuration
@Profile({ "default", "prod" })
public class RedisResourcePersistenceFactory implements
        ResourcePersistenceFactoryAPI {
    private static final Logger log = LoggerFactory
            .getLogger(RedisResourcePersistenceFactory.class);

    @Autowired
    MetricServiceAppConfiguration config;

    private JedisPool pool = null;

    public RedisResourcePersistenceFactory() {
    }

    private void connect() {
        if (pool == null) {
            synchronized (this) {
                if (pool == null) {
                    String[] parts = config.getChartServiceConfig()
                            .getRedisConnection().split(":", 2);
                    try {
                        switch (parts.length) {
                        case 1:
                            pool = new JedisPool(parts[0]);
                            break;
                        case 2:
                            pool = new JedisPool(parts[0],
                                    Integer.parseInt(parts[1]));
                            break;
                        default:
                            pool = null;
                            log.error(String.format(
                                    "Invalid Redis connection string, '%s'",
                                    config.getChartServiceConfig()
                                            .getRedisConnection()));
                        }
                    } catch (Throwable t) {
                        log.error(String
                                .format("Unable to connect to Redis for resource storage, %s : %s",
                                        t.getClass().getName(), t.getMessage()));
                        pool = null;
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.api.impl.ResourcePersistenceFactoryAPI#
     * getInstance()
     */
    @Override
    public ResourcePersistenceAPI getInstance(String resourceType) {
        connect();
        if (pool == null) {
            throw new RuntimeException("Not Connected");
        }
        return new RedisResourcePersistence(resourceType, pool.getResource());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.api.impl.ResourcePersistenceFactoryAPI#
     * returnInstance()
     */
    @Override
    public void returnInstance(ResourcePersistenceAPI api) {
        if (pool == null) {
            throw new RuntimeException("Not Connected");
        }
        pool.returnResource(((RedisResourcePersistence) api)
                .getImplementation());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.api.impl.ResourcePersistenceFactoryAPI#
     * isConnected()
     */
    @Override
    public boolean isConnected() {
        connect();
        return pool != null;
    }
}
