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
package org.zenoss.app.metricservice.api.impl;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.model.v2.RenameRequest;
import org.zenoss.app.metricservice.api.model.v2.RenameResult;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class OpenTSDBClient {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OpenTSDBClient.class);


    private final DefaultHttpClient httpClient;
    private final String queryURL;


    public OpenTSDBClient(DefaultHttpClient httpClient, String url) {
        this.httpClient = httpClient;
        this.queryURL = url;
    }

    public RenameResult rename(OpenTSDBRename rename, String dropCacheUrl) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpPost httpPost = new HttpPost(queryURL);
        final String jsonQueryString = Utils.jsonStringFromObject(rename);
        StringEntity input;
        try {
            input = new StringEntity(jsonQueryString);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException converting json string {} to StringEntity: {}", jsonQueryString, e.getMessage());
            throw new IllegalArgumentException("Could not create StringEntity from query.", e);
        }
        input.setContentType("application/json");
        httpPost.setEntity(input);
        RenameResult result = new RenameResult();
        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            StatusLine status = response.getStatusLine();
            result.reason = status.getReasonPhrase();
            result.code = status.getStatusCode();

// Dropcaches
//            if(result.code == Response.Status.OK.getStatusCode()) {
//                HttpGet httpDrop = new HttpGet(dropCacheUrl);
//                HttpResponse dropResponse = httpClient.execute(httpDrop, context);
//                status = dropResponse.getStatusLine();
//                log.warn("Dropping OpenTSDB cache.. %s, %i", status.getReasonPhrase(), status.getStatusCode());
//                // TODO: Check for error status codes from the cache drop
//            } else {
//                // TODO: do something for bad status returns from the rename
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpPost.releaseConnection();
        }

        return result;
    }

    public OpenTSDBQueryReturn query(OpenTSDBQuery query) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpPost httpPost = new HttpPost(queryURL);
        final String jsonQueryString = Utils.jsonStringFromObject(query);
        StringEntity input;
        try {
            input = new StringEntity(jsonQueryString);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException converting json string {} to StringEntity: {}", jsonQueryString, e.getMessage());
            throw new IllegalArgumentException("Could not create StringEntity from query.", e);
        }
        input.setContentType("application/json");
        httpPost.setEntity(input);

        QueryStatus queryStatus = null;
        HttpEntity entity = null;
        OpenTSDBQueryResult[] resultArray = new OpenTSDBQueryResult[]{};
        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != Response.Status.OK.getStatusCode()) {
                String message = status.getReasonPhrase();
                entity = response.getEntity();
                if (null != entity) {
                    String content = EntityUtils.toString(response.getEntity());
                    OpenTSDBErrorResponse tsdbResponse = Utils.getObjectMapper().readValue(content, OpenTSDBErrorResponse.class);
                    log.info("Response code {}, message: {}", tsdbResponse.error.code, tsdbResponse.error.message);
                    log.debug("Response object: {}", Utils.jsonStringFromObject(tsdbResponse));
                    message = tsdbResponse.error.message;
                }else{
                    log.info("HTTP Execute returned status {}. Reason: {}", status.getStatusCode(), status.getReasonPhrase());
                }
                queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.ERROR, message);
            } else {
                entity = response.getEntity();
                String contentString = EntityUtils.toString(entity);
                try {
                    resultArray = Utils.getObjectMapper().readValue(contentString, OpenTSDBQueryResult[].class);
                } catch (IOException e) {
                    log.warn("Unable to parse HTTP response as OpenTSDBQueryResult.");
                    queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.WARNING,
                            String.format("Could not parse content as OpenTSDBQueryResult[]. Content: \"%s\"", contentString));
                }
                if (null != resultArray && resultArray.length > 0) {
                    queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.SUCCESS, "");
                }
                if (null == queryStatus) {
                    queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.WARNING, "OpenTSDB query was successful, but no data was returned.");
                }
            }
        } catch (ClientProtocolException e) {
            log.error("ClientProtocolException executing and processing query: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("IOException stack trace: {}", e.getStackTrace());
            }
            queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.ERROR,
                    String.format("%s executing and processing query: %s", e.getClass().getName(), e.getMessage()));
        } catch (IOException e) {
            log.error("IOException executing and processing query: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("IOException stack trace: {}", e.getStackTrace());
            }
            queryStatus = new QueryStatus(QueryStatus.QueryStatusEnum.ERROR,
                    String.format("%s executing and processing query: %s", e.getClass().getName(), e.getMessage()));
        } finally {
            EntityUtils.consumeQuietly(entity);
            log.debug("releasing connection.");
            httpPost.releaseConnection();
        }

        return new OpenTSDBQueryReturn(resultArray, queryStatus);
    }
}
