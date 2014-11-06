/*
 * Copyright (c) 2014, Zenoss and/or its affiliates. All rights reserved.
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;

class CallableQueryExecutor implements Callable<OpenTSDBQueryResult> {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CallableQueryExecutor.class);
    private final HttpContext context;
    private final HttpPost httpPost;
    private final DefaultHttpClient httpClient;
    private final OpenTSDBQuery query;

    public CallableQueryExecutor(DefaultHttpClient httpClient, OpenTSDBQuery query, String queryURL) {
        this.context = new BasicHttpContext();
        this.query = query;
        this.httpClient = httpClient;
        httpPost = new HttpPost(queryURL);
        String jsonQueryString = Utils.jsonStringFromObject(query);
        StringEntity input = null;
        try {
            input = new StringEntity(jsonQueryString);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException converting json string {} to StringEntity: {}", jsonQueryString, e.getMessage());
            throw new IllegalArgumentException("Could not create StringEntity from query.", e);
        }
        input.setContentType("application/json");
        httpPost.setEntity(input);
    }

    @Override
    public OpenTSDBQueryResult call() {
        OpenTSDBQueryResult result = defaultResult();
        HttpEntity entity = null;
        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != Response.Status.OK.getStatusCode()) {
                log.warn("HTTP Execute returned status {}. Reason: {}", status.getStatusCode(), status.getReasonPhrase());
                String content = EntityUtils.toString(response.getEntity());
                log.debug("####### RESPONSE CONTENT:#########\n{}", content);
                OpenTSDBErrorResponse tsdbResponse = Utils.getObjectMapper().readValue(content, OpenTSDBErrorResponse.class);
                log.info("Response object: {}", Utils.jsonStringFromObject(tsdbResponse));
                result.setStatus(new QueryStatus(QueryStatus.QueryStatusEnum.ERROR, tsdbResponse.error.message));
            } else {
                entity = response.getEntity();
                OpenTSDBQueryResult [] resultArray = null;
                String contentString = EntityUtils.toString(entity);
                try {
                    resultArray = Utils.getObjectMapper().readValue(contentString, OpenTSDBQueryResult[].class);
                } catch (IOException e) {
                    log.warn("Unable to parse HTTP response as OpenTSDBQueryResult.");
                    result.setStatus(new QueryStatus(QueryStatus.QueryStatusEnum.WARNING,
                        String.format("Could not parse content as OpenTSDBQueryResult[]. Content: \"%s\"", contentString)));
                }
                if (null != resultArray && resultArray.length > 0) {
                    result = resultArray[0];
                    result.setStatus(new QueryStatus(QueryStatus.QueryStatusEnum.SUCCESS, ""));
                }
            }
        } catch (ClientProtocolException e) {
            log.error("ClientProtocolException executing and processing query: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("IOException stack trace: {}", e.getStackTrace());
            }
            result.setStatus(new QueryStatus(QueryStatus.QueryStatusEnum.ERROR,
                String.format("%s executing and processing query: %s", e.getClass().getName(), e.getMessage())));
        } catch (IOException e) {
            log.error("IOException executing and processing query: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("IOException stack trace: {}", e.getStackTrace());
            }
            result.setStatus(new QueryStatus(QueryStatus.QueryStatusEnum.ERROR,
                String.format("%s executing and processing query: %s", e.getClass().getName(), e.getMessage())));
        } finally {
            EntityUtils.consumeQuietly(entity);
            httpPost.releaseConnection();
        }
        return result;
    }

    private OpenTSDBQueryResult defaultResult() {
        OpenTSDBQueryResult result = new OpenTSDBQueryResult();
        result.metric = query.queries.get(0).metric;
        result.tags = query.queries.get(0).tags;
        return result;
    }

}
