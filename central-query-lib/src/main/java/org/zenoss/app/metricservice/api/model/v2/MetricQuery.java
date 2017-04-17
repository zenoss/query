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
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.RateOptions;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class MetricQuery {

    public static final Aggregator DEFAULT_AGGREGATOR = Aggregator.avg;

    @JsonProperty
    @NotNull
    private String metric = null;

    @JsonProperty
    @NotNull
    private Aggregator aggregator = DEFAULT_AGGREGATOR;

    @JsonProperty
    private String downsample = null;

    @JsonProperty
    private Boolean rate = Boolean.FALSE;

    @JsonProperty
    private RateOptions rateOptions = null;

    @JsonProperty
    private String expression = null;

    @JsonProperty
    private Map<String, List<String>> tags = new HashMap<>();

    @JsonProperty
    private List<Filter> filters = new ArrayList<>();


    /**
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @param expression the expression to set
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * @return the metric
     */
    public final String getMetric() {
        return metric;
    }

    /**
     * @param metric the metric to set
     */
    public final void setMetric(String metric) {
        this.metric = metric;
    }

    /**
     * @return the aggregator
     */
    public final Aggregator getAggregator() {
        return aggregator;
    }

    /**
     * @param aggregator the aggregator to set
     */
    public final void setAggregator(Aggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * @return the downsample
     */
    public final String getDownsample() {
        return downsample;
    }

    /**
     * @param downsample the downsample to set
     */
    public final void setDownsample(String downsample) {
        this.downsample = downsample;
    }

    /**
     * @return the rate
     */
    public final Boolean getRate() {
        return rate;
    }

    /**
     * @param rate the rate to set
     */
    public final void setRate(Boolean rate) {
        this.rate = rate;
    }

    /**
     * @return the rateOptions
     */
    public final RateOptions getRateOptions() {
        return rateOptions;
    }

    /**
     * @param rateOptions the rateOptions to set
     */
    public final void setRateOptions(RateOptions rateOptions) {
        this.rateOptions = rateOptions;
    }

    /**
     * @return the tags
     */
    public final Map<String, List<String>> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public final void setTags(Map<String, List<String>> tags) {
        if (null == tags) {
            this.tags.clear();
        } else {
            this.tags = tags;
        }
    }

    /**
     * @return the filters
     */
    public final List<Filter> getFilters() {
        return filters;
    }

    /**
     * @param filters the filters to set
     */
    public final void setFilters(List<Filter> filters) {
        if (null == filters) {
            this.filters.clear();
        } else {
            this.filters = filters;
        }
    }
}
