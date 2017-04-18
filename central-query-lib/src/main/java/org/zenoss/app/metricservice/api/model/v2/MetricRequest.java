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

package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


import com.google.common.collect.Lists;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.ReturnSet;

import java.util.Collection;
import java.util.List;

/**
 *
 */
@JsonInclude(Include.NON_NULL)
public class MetricRequest {
    @JsonProperty
    @NotNull
    private String start = Utils.DEFAULT_START_TIME;
    
    @JsonProperty
    private String end = Utils.DEFAULT_END_TIME;

    @JsonProperty(required=true)
    @NotNull
    @Size(min=1)
    @Valid
    private List<MetricQuery> queries = null;

    @JsonProperty(value="returnset")
    private ReturnSet returnset = null;


    /**
     * @return the start
     */
    public final String getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public final void setStart(String start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public final String getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public final void setEnd(String end) {
        this.end = end;
    }

    /**
	 * @return the returnset
	 */
	public ReturnSet getReturnset() {
		return returnset;
	}

	/**
	 * @param returnset the returnset to set
	 */
	public void setReturnset(ReturnSet returnset) {
		this.returnset = returnset;
	}

    public List<MetricQuery> getQueries() {
        return queries;
    }

    public void setQueries(Collection<MetricQuery> metrics) {
        this.queries= Lists.newArrayList(metrics);
    }

    public void setQuery(MetricQuery... metrics) {
        this.queries= Lists.newArrayList(metrics);
    }
}
