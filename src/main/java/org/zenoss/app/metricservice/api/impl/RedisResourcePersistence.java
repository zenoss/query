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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.model.Chart;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class RedisResourcePersistence implements ResourcePersistenceAPI {
    private static final Logger log = LoggerFactory
            .getLogger(RedisResourcePersistence.class);

    private Jedis jedis = null;
    private String idHashName = null;
    private String listName = null;

    public RedisResourcePersistence(String resourceType, Jedis impl) {
        jedis = impl;
        idHashName = resourceType + "_HASH";
        listName = resourceType + "_LIST";
    }

    public Jedis getImplementation() {
        return jedis;
    }

    @Override
    public void connect(String prefix, String host) {
        connect(prefix, host, -1);
    }

    @Override
    public void connect(String prefix, String host, int port) {
        if (jedis == null) {
            idHashName = prefix + "_HASH";
            listName = prefix + "_LIST";
            if (port == -1) {
                jedis = new Jedis(host, 6379, 10000);
            } else {
                jedis = new Jedis(host, port, 10000);
            }
            try {
                jedis.ping();
            } catch (Throwable t) {
                try {
                    jedis.disconnect();
                } catch (Throwable ignore) {
                    // ignore
                }
                jedis = null;
                throw t;
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (jedis != null) {
            return jedis.isConnected();
        }
        return false;
    }

    @Override
    public void disconnect() {
        if (jedis != null && jedis.isConnected()) {
            jedis.disconnect();
            jedis = null;
        }
    }

    @Override
    public void ping() {
        if (jedis != null) {
            jedis.ping();
        } else {
            throw new WebApplicationException(Utils.getErrorResponse(null, 500,
                    "Not connected to persistence",
                    "Not connected to persistence"));

        }
    }

    @Override
    public String getResourceById(String id) {
        return jedis.hget(idHashName, id);
    }

    @Override
    public String getResourceByName(String name) {
        // No good way to deal with this in redis
        List<String> charts = null;
        try {
            charts = new ArrayList<String>(jedis.hvals(idHashName));
        } catch (Throwable t) {
            log.error(
                    "Unexpected error while attempting to fetch names from persistence: {} : {}",
                    t.getClass().getName(), t.getMessage());
        }

        // Walk the list of charts and if we find one with the attribute
        // name and value specified
        ObjectMapper om = new ObjectMapper();
        ObjectReader reader = om.reader(Chart.class);
        Chart chart = null;

        for (String content : charts) {
            try {
                chart = reader.readValue(content);
                if (name.equals(chart.getName())) {
                    return content;
                }
            } catch (Throwable t) {
                throw new WebApplicationException(Utils.getErrorResponse(null,
                        500, "unable to read chart from persistence", t
                                .getClass().getName() + ":" + t.getMessage()));
            }
        }
        return null;
    }

    @Override
    public boolean delete(String id) {
        Transaction txn = jedis.multi();
        txn.lrem(listName, 1, id);
        txn.hdel(idHashName, id);
        List<Object> responses = txn.exec();

        return (responses != null && !responses.isEmpty() && ((Long) responses
                .get(0)) == 1);
    }

    @Override
    public boolean add(String uuid, String content) {
        Transaction txn = jedis.multi();
        txn.rpush(listName, uuid);
        txn.hset(idHashName, uuid, content);
        List<Object> responses = txn.exec();

        return responses != null && responses.size() == 2;
    }

    @Override
    public boolean update(String uuid, String content) {
        return jedis.hset(idHashName, uuid, content) != null;
    }

    @Override
    public boolean exists(String id) {
        return jedis.hexists(idHashName, id);
    }

    @Override
    public List<String> range(int start, int end) {
        return jedis.lrange(listName, start, end);
    }

    @Override
    public long count() {
        return jedis.llen(listName);
    }
}
