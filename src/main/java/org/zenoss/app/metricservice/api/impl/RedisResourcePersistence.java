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

import java.util.List;

import javax.ws.rs.WebApplicationException;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.annotations.API;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

@API
@Configuration
@Profile({ "default", "prod" })
public class RedisResourcePersistence implements ResourcePersistenceAPI {

    private Jedis jedis = null;
    private String hashName = null;
    private String listName = null;

    @Override
    public void connect(String prefix, String host) {
        connect(prefix, host, -1);
    }

    @Override
    public void connect(String prefix, String host, int port) {
        if (jedis == null) {
            hashName = prefix + "_HASH";
            listName = prefix + "_LIST";
            if (port == -1) {
                jedis = new Jedis(host);
            } else {
                jedis = new Jedis(host, port);
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
    public String getResource(String id) {
        return jedis.hget(hashName, id);
    }

    @Override
    public boolean delete(String id) {
        Transaction txn = jedis.multi();
        txn.lrem(listName, 1, id);
        txn.hdel(hashName, id);
        List<Object> responses = txn.exec();

        return (responses != null && !responses.isEmpty() && ((Long) responses
                .get(0)) == 1);
    }

    @Override
    public boolean add(String uuid, String content) {
        Transaction txn = jedis.multi();
        txn.rpush(listName, uuid);
        txn.hset(hashName, uuid, content);
        List<Object> responses = txn.exec();

        return responses != null && responses.size() == 2;
    }

    @Override
    public boolean update(String uuid, String content) {
        return jedis.hset(hashName, uuid, content) != null;
    }

    @Override
    public boolean exists(String id) {
        return jedis.hexists(hashName, id);
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
