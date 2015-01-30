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
package org.zenoss.app.metricservice;

import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.Utils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class ErrorResponseTest {
    @Test
    public void nullTest() throws IOException {
        Response er = Utils.getErrorResponse(null, 200, null, null);
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should be empty", 0, obj.size());
    }

    @Test
    public void idTest() throws IOException {
        Response er = Utils.getErrorResponse("myid", 200, null, null);
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have only one value", 1, obj.size());
        Assert.assertEquals("should have id value", "myid", obj.get("id"));
    }

    @Test
    public void messageTest() throws IOException {
        Response er = Utils.getErrorResponse(null, 200, "mymessage", null);
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have only one value", 1, obj.size());
        Assert.assertEquals("should have message value", "mymessage", obj.get("errorMessage"));
    }

    @Test
    public void sourceTest() throws IOException {
        Response er = Utils.getErrorResponse(null, 200, null, "mysource");
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have only one value", 1, obj.size());
        Assert.assertEquals("should have source value", "mysource", obj.get("errorSource"));
    }

    @Test
    public void idMessageTest() throws IOException {
        Response er = Utils.getErrorResponse("myid", 200, "mymessage", null);
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have two values", 2, obj.size());
        Assert.assertEquals("should have id value", "myid", obj.get("id"));
        Assert.assertEquals("should have message value", "mymessage", obj.get("errorMessage"));
    }

    @Test
    public void idSourceTest() throws IOException {
        Response er = Utils.getErrorResponse("myid", 200, null, "mysource");
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have two values", 2, obj.size());
        Assert.assertEquals("should have id value", "myid", obj.get("id"));
        Assert.assertEquals("should have source value", "mysource", obj.get("errorSource"));
    }

    @Test
    public void messageSourceTest() throws IOException {
        Response er = Utils.getErrorResponse(null, 200, "mymessage", "mysource");
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have two values", 2, obj.size());
        Assert.assertEquals("should have message value", "mymessage", obj.get("errorMessage"));
        Assert.assertEquals("should have source value", "mysource", obj.get("errorSource"));
    }

    @Test
    public void allTest() throws IOException {
        Response er = Utils.getErrorResponse("myid", 200, "mymessage", "mysource");
        Map<String, Object> obj = Utils.getObjectMapper().reader(Map.class)
                .readValue((String) er.getEntity());
        Assert.assertNotNull(obj);
        Assert.assertEquals(200, er.getStatus());
        Assert.assertEquals("should have three values", 3, obj.size());
        Assert.assertEquals("should have id value", "myid", obj.get("id"));
        Assert.assertEquals("should have message value", "mymessage", obj.get("errorMessage"));
        Assert.assertEquals("should have source value", "mysource", obj.get("errorSource"));
    }
}
