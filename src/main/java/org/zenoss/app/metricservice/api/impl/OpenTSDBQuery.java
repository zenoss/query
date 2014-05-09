package org.zenoss.app.metricservice.api.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.zenoss.app.metricservice.api.model.Aggregator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/7/14
 * Time: 2:18 PM
 *
 */
public class OpenTSDBQuery {
    public String start = null;
    public String end = null;
    public List<OpenTSDBSubQuery> queries;
    public boolean noAnnotations = false;
    public boolean globalAnnotations = false;
    public boolean msResolution = false;
    public boolean showTSUIDs = false;

    public void addSubQuery(OpenTSDBSubQuery openTSDBSubQuery) {
        if (null == queries) {
            queries = new ArrayList<>();
        }
        queries.add(openTSDBSubQuery);
    }

    @JsonIgnore
    public boolean isValidQuery() {
        return (null != start) && (null != end);
    }
}

