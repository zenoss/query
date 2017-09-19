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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Ordering;
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

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

public class OpenTSDBClient {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(OpenTSDBClient.class);

    private final DefaultHttpClient httpClient;
    private final String providedURL;

    private static final ObjectMapper objectMapper = Utils.getObjectMapper();

    public OpenTSDBClient(DefaultHttpClient httpClient, String url) {
        this.httpClient = httpClient;
        this.providedURL = url;
    }

    public SuggestResult suggest(OpenTSDBSuggest suggest) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpPost httpPost = new HttpPost(providedURL);
        final String jsonQueryString = Utils.jsonStringFromObject(suggest);
        StringEntity input;
        try {
            input = new StringEntity(jsonQueryString);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException converting json string {} to StringEntity: {}", jsonQueryString, e.getMessage());
            throw new IllegalArgumentException("Could not create StringEntity from query.", e);
        }
        input.setContentType("application/json");
        httpPost.setEntity(input);
        SuggestResult result = new SuggestResult();
        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            String json = EntityUtils.toString(response.getEntity());
            result.suggestions = objectMapper.readValue(json, ArrayList.class);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpPost.releaseConnection();
        }

        return result;
    }

    public DropResult dropCache(String dropCacheUrl) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpGet httpDrop = new HttpGet(dropCacheUrl);
        final HttpResponse dropResponse;
        try {
            dropResponse = httpClient.execute(httpDrop, context);
            StatusLine status = dropResponse.getStatusLine();
            DropResult result = new DropResult();
            result.reasonPhrase = status.getReasonPhrase();
            result.statusCode = status.getStatusCode();
            log.debug("Dropping OpenTSDB cache.. %s, %i", status.getReasonPhrase(), status.getStatusCode());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpDrop.releaseConnection();
        }
        return null;
    }

    public RenameResult rename(OpenTSDBRename rename) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpPost httpPost = new HttpPost(providedURL);
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
        result.request = rename;
        try {
            HttpResponse response = httpClient.execute(httpPost, context);
            StatusLine status = response.getStatusLine();
            InputStream in = response.getEntity().getContent();
            OpenTSDBRenameResult content = objectMapper.readValue(in, OpenTSDBRenameResult.class);
            result.reason = content.getError();
            result.code = status.getStatusCode();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpPost.releaseConnection();
        }

        return result;
    }

    public OpenTSDBQueryReturn query(OpenTSDBQuery query, boolean ignoreRateOption, long rateCutoffDate) {
        ArrayList<OpenTSDBQueryReturn> results = new ArrayList<>();
        if (ignoreRateOption) {
            boolean spansCutoff = false;
            try {
                long startTs = Utils.parseDate(query.start);
                long endTs = Utils.parseDate(query.end);

                if (rateCutoffDate > startTs && rateCutoffDate < endTs) {
                    spansCutoff = true;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            ArrayList<OpenTSDBSubQuery> cutoffQueries = new ArrayList<>();
            Iterator<OpenTSDBSubQuery> iter = query.queries.iterator();
            // find rate queries and remove if spanning cutoff otherwise just remove rate option
            while (iter.hasNext()) {
                OpenTSDBSubQuery q = iter.next();
                if (q.rate) {
                    if (spansCutoff) {
                        cutoffQueries.add(q);
                        iter.remove();
                    } else {
                        q.rate = false;
                    }
                }
            }
            if (cutoffQueries.size() > 0) {
                //we have rate queries and they span the cutoff date
                //create a new query, one for pre cutoff and one for post cutoff
                //if original query still has sub queries (gauges) make that request as well
                ArrayList<OpenTSDBQueryReturn> responses = new ArrayList<>(2);
                for (OpenTSDBSubQuery q : cutoffQueries) {
                    OpenTSDBQuery preCutoff = new OpenTSDBQuery();
                    preCutoff.start = query.start;
                    preCutoff.end = String.valueOf(rateCutoffDate);

                    OpenTSDBQuery postCutoff = new OpenTSDBQuery();
                    postCutoff.start = String.valueOf(rateCutoffDate);
                    postCutoff.end = query.end;

                    preCutoff.addSubQuery(q);
                    OpenTSDBSubQuery postQ = new OpenTSDBSubQuery();
                    //post cutoff queries are stored as already calculated rates
                    postQ.rate = false;
                    postQ.rateOptions = q.rateOptions;
                    postQ.aggregator = q.aggregator;
                    postQ.metric = q.metric;
                    postQ.downsample = q.downsample;
                    postQ.tags = q.tags;
                    postQ.filters = q.filters;
                    postCutoff.addSubQuery(postQ);

                    OpenTSDBQueryReturn preResult = this.query(preCutoff);
                    if (preResult.getStatus().getStatus() == QueryStatus.QueryStatusEnum.ERROR) {
                        return preResult;
                    }
                    OpenTSDBQueryReturn postResult = this.query(postCutoff);
                    if (postResult.getStatus().getStatus() == QueryStatus.QueryStatusEnum.ERROR) {
                        return postResult;
                    }
                    results.add(this.mergeResults(preResult, postResult));
                    responses.clear();
                }
            }
        }
        if (!query.queries.isEmpty()) {
            OpenTSDBQueryReturn result = this.query(query);
            if (result.getStatus().getStatus() == QueryStatus.QueryStatusEnum.ERROR) {
                return result;
            }
            results.add(result);
        }
        return this.combine(results);
    }

    private OpenTSDBQueryReturn combine(Collection<OpenTSDBQueryReturn> results) {
        if (results.size() == 1) {
            return results.iterator().next();
        }

        ArrayList<OpenTSDBQueryResult> combined = new ArrayList<>();
        for( OpenTSDBQueryReturn x : results){
            combined.addAll(x.getResults());
        }
        OpenTSDBQueryResult[] finalResults = combined.toArray(new OpenTSDBQueryResult[results.size()]);
        return new OpenTSDBQueryReturn(finalResults, new QueryStatus(QueryStatus.QueryStatusEnum.SUCCESS, ""));
    }

    private OpenTSDBQueryReturn mergeResults(OpenTSDBQueryReturn preCutoff, OpenTSDBQueryReturn postCutoff) {
        //Join the results of one metric query that has been split,
        // every query can return multiple results so make a key from the metricname and aggregated tags
        Map<String, OpenTSDBQueryResult> results = new HashMap<>();
        OpenTSDBQueryReturn[] both = new OpenTSDBQueryReturn[]{preCutoff, postCutoff};
        for (OpenTSDBQueryReturn input : both) {
            for (OpenTSDBQueryResult x : input.getResults()) {
                TreeMap<String, String> tags = new TreeMap<>(x.tags);
                String key = x.metric + tags;
                if (!results.containsKey(key)) {
                    results.put(key, x);
                } else {
                    results.get(key).getDataPoints().putAll(x.getDataPoints());
                }
            }
        }
        OpenTSDBQueryResult[] finalResults = results.values().toArray(new OpenTSDBQueryResult[results.size()]);
        return new OpenTSDBQueryReturn(finalResults, new QueryStatus(QueryStatus.QueryStatusEnum.SUCCESS, ""));
    }

    private OpenTSDBQueryReturn query(OpenTSDBQuery query) {
        final BasicHttpContext context = new BasicHttpContext();
        final HttpPost httpPost = new HttpPost(providedURL);

        final String jsonQueryString = Utils.jsonStringFromObject(query);
        log.info("JPL query");
        log.info(jsonQueryString);
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
                } else {
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
