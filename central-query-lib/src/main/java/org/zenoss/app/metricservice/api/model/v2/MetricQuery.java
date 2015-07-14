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
import com.google.common.base.Joiner;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.Aggregator;
import org.zenoss.app.metricservice.api.model.RateFormatException;
import org.zenoss.app.metricservice.api.model.RateOptions;

import javax.validation.constraints.NotNull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
public class MetricQuery {

    public static final Aggregator DEFAULT_AGGREGATOR = Aggregator.avg;

    @JsonProperty
    private String id = null;

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
    private boolean emit = true;

    /**
     * @return id string
     */
    public String getId() {
        return id;
    }

    /**
     *  @param id - id to set for query
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @param expression
     *            the expression to set
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
     * @param metric
     *            the metric to set
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
     * @param aggregator
     *            the aggregator to set
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
     * @param downsample
     *            the downsample to set
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
     * @param rate
     *            the rate to set
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
     * @param rateOptions
     *            the rateOptions to set
     */
    public final void setRateOptions(RateOptions rateOptions) {
        this.rateOptions = rateOptions;
    }

    /**
     * @return the tags
     */
    public final Map<String, List<String>> getTags() {
        if (null == tags) {
            initializeTags();
        }
        return tags;
    }

    private void initializeTags() {
        tags = new HashMap<>();
    }

    /**
     * @param tags
     *            the tags to set
     */
    public final void setTags(Map<String, List<String>> tags) {
        if (null == tags) {
            initializeTags();
        } else {
            this.tags = tags;
        }
    }

    /**
     * @param newTags
     *            the tags to merge with the existing tags.
     */
    public final void mergeTags(Map<String, List<String>> newTags) {
        if (null == newTags) {
            return;
        }

        for (Map.Entry<String, List<String>> tagEntry : newTags.entrySet()) {
            if (tags.containsKey(tagEntry.getKey())) {
                List<String> tagList = tags.get(tagEntry.getKey());
                tagList.addAll(tagEntry.getValue());
            } else {
                tags.put(tagEntry.getKey(), tagEntry.getValue());
            }
        }
    }

    public boolean getEmit() {
        return emit;
    }

    public void setEmit(boolean emit) {
        this.emit = emit;
    }

    /**
     * Encodes the current instance into the URL query parameter format that <a
     * href="http://opentsdb.net/http-api.html#/q">OpenTSDB</a> supports.
     * <p/>
     * <em style="color: red">NOTE: This method supports a format that is
     * proposed to OpenTSDB, but is not yet committed. This format include
     * "rate" options to better support counter base metrics</em>
     *
     * @return OpenTSDB URL query formatted String instance
     */
    public String toString(boolean withRateOptions) {
        StringBuilder buf = new StringBuilder();
        if (aggregator != null) {
            buf.append(aggregator).append(':');
        }
        if (this.downsample != null) {
            buf.append(this.downsample).append(':');
        }
        if (rate) {
            buf.append("rate");
            if (withRateOptions && rateOptions != null) {
                buf.append('{');
                if (rateOptions.getCounter() != null) {
                    if (rateOptions.getCounter()) {
                        buf.append("counter");
                        if (rateOptions.getCounterMax() != null) {
                            buf.append(',');
                            buf.append(rateOptions.getCounterMax());
                        } else if (rateOptions.getResetThreshold() != null) {
                            buf.append(',');
                        }
                        if (rateOptions.getResetThreshold() != null) {
                            buf.append(',');
                            buf.append(rateOptions.getResetThreshold());
                        }
                    }
                }
                buf.append('}');
            }
            buf.append(':');
        }
        buf.append(metric);
        if (getTags() != null && getTags().size() > 0) {
            Map<String, List<String>> joined = new HashMap<>();
                joined.putAll(getTags());
            buf.append('{');
            boolean comma = false;
            for (Map.Entry<String, List<String>> tag : joined.entrySet()) {
                if (comma) {
                    buf.append(',');
                }
                comma = true;
                buf.append(tag.getKey()).append('=').append(Joiner.on("|").skipNulls().join(tag.getValue()));
            }
            buf.append('}');
        }
        return buf.toString();
    }
    /**
     * Encodes the current instance into the URL query parameter format that <a
     * href="http://opentsdb.net/http-api.html#/q">OpenTSDB</a> supports.
     * <p/>
     * <em style="color: red">NOTE: This method supports a format that is
     * proposed to OpenTSDB, but is not yet committed. This format include
     * "rate" options to better support counter base metrics</em>
     *
     * @return OpenTSDB URL query formatted String instance
     */
    public String toString() {
        return this.toString(false);
    }


    /**
     * @param value
     * @return
     */
    private static Map<String, List<String>> parseTags(String value) {
        Map<String, List<String>> tags = new HashMap<>();

        if (value == null || (value = value.trim()).length() == 0) {
            return tags;
        }
        String[] pairs = value.substring(1, value.length() - 1).split(",");
        for (String pair : pairs) {
            String[] terms = pair.split("=", 2);
            List<String> vals = new ArrayList<>();
            vals.add(terms[1].trim());
            tags.put(terms[0].trim(), vals);
        }
        return tags;
    }

    private static RateOptions parseRateOptions(String content) {
        RateOptions options = new RateOptions();

        // Format is "counter[,[max][,reset]]"
        String[] parts = content.substring("rate{".length(),
            content.length() - 1).split(",");

        if (parts[0].trim().length() == 0 || parts.length > 3) {
            throw new RateFormatException("invalid number of options");
        }

        if (!"counter".equals(parts[0].trim())) {
            throw new RateFormatException(
                "first option must be value \"counter\"");
        }
        options.setCounter(true);

        if (parts.length > 1) {
            // We have at least a max value, so this should either be an empty
            // string or a long value. Only set value if length > 0
            String v = parts[1].trim();
            if (v.length() > 0) {
                try {
                    options.setCounterMax(Long.parseLong(v));
                } catch (NumberFormatException nfe) {
                    throw new RateFormatException(
                        String.format(
                            "Unable to parse counter max value '%s' as type long",
                            v), nfe);
                }
            }

            // We have a reset value, again only parse if the length > 0
            if (parts.length > 2) {
                v = parts[2].trim();
                if (v.length() > 0) {
                    try {
                        options.setResetThreshold(Long.parseLong(v));
                    } catch (NumberFormatException nfe) {
                        throw new RateFormatException(
                            String.format(
                                "Unable to parse counter reset value '%s' as type long",
                                v), nfe);
                    }
                }
            }
        }

        return options;
    }

    /**
     * Parse the URL metric parameter format supported by <a
     * href="http://opentsdb.net/http-api.html#/q">OpenTSDB</a> into the metric
     * services {@link MetricQuery} model object.
     * <p/>
     * <em style="color: red">NOTE: This method supports a format that is
     * proposed to OpenTSDB, but is not yet committed. This format include
     * "rate" options to better support counter base metrics</em>
     *
     * @param content
     *            the metric specification in the OpenTSDB format
     * @return model representation of the URL metric query parameter
     * @see MetricQuery
     */
    public static MetricQuery fromString(String content) {

        // Determine if there are tags in this query specification. This will
        // be a simple check, if there is a pattern '{' ... at the end
        // of the value, then strip it off as the tags.
        String[] terms = content.split(":", 4);
        int idx = terms[terms.length - 1].indexOf('{');

        String metric = null;
        Map<String, List<String>> tags = null;
        if (idx >= 0) {
            tags = parseTags(terms[terms.length - 1]
                .substring(idx).trim());
            metric = terms[terms.length - 1].substring(0, idx);
        } else {
            tags = new HashMap<>();
            metric = terms[terms.length - 1];
        }

        Aggregator aggregator = DEFAULT_AGGREGATOR;
        if (terms.length > 1) {
            aggregator = Aggregator.valueOf(terms[0].trim());
        }
        boolean rate = false;
        String downsample = null;
        RateOptions rateOptions = null;
        if (terms.length > 2) {
            if (terms[1].trim().startsWith("rate")) {
                rate = true;
                if (terms[1].indexOf('{') > -1) {
                    try {
                        rateOptions = parseRateOptions(terms[1].trim());
                    } catch (Exception e) {
                        throw new WebApplicationException(
                            Utils.getErrorResponse(null, Response.Status.BAD_REQUEST.getStatusCode(),
                                e.getMessage(), e.getClass().getName()));
                    }
                }
                if (terms.length > 3) {
                    downsample = terms[2].trim();
                }
            } else {
                downsample = terms[1].trim();
                if (terms.length > 3 && terms[2].trim().startsWith("rate")) {
                    rate = true;
                    if (terms[2].indexOf('{') > -1) {
                        try {
                            rateOptions = parseRateOptions(terms[2].trim());
                        } catch (Exception e) {
                            throw new WebApplicationException(
                                Utils.getErrorResponse(null, Response.Status.BAD_REQUEST.getStatusCode(), e
                                    .getMessage(), e.getClass()
                                    .getName()));
                        }
                    }
                } else if (terms.length >= 4) {
                    // They specified enough terms to include "rate", but the
                    // term that should be "rate" is some other random value,
                    // so this is a bad request.
                    throw new WebApplicationException(
                        Utils.getErrorResponse(
                            null,
                            Response.Status.BAD_REQUEST.getStatusCode(),
                            String.format(
                                "unknown value '%s' specified, when only 'rate' value is allowed",
                                terms[2].trim()), "RequestParse"));
                }
            }
        }

        MetricQuery ms = new MetricQuery();
        ms.aggregator = aggregator;
        ms.downsample = downsample;
        ms.rate = rate;
        ms.rateOptions = rateOptions;
        ms.metric = metric;
        ms.setTags(tags);
        return ms;
    }

    public void validateWithErrorHandling(List<Object> errors) {
        // Add error if '*' is specified within a tag
        if (true){return;}
        if (null != tags) {
            for (Map.Entry<String, List<String>> entry : tags.entrySet()) {
                for (String tagValue : entry.getValue()) {
                    if (tagValue.contains("blamo")) {
                        String tagKey = entry.getKey();
                        String errorMessage = String.format("Tag %s has value %s, which contains '*'.", tagKey, tagValue);
                        String tagLocation = String.format("Value %s in tag %s of series %s", tagValue, tagKey, getMetric());
                        errors.add(Utils.makeError(errorMessage,"Tag values may not contain '*'", tagLocation));
                    }
                }
            }
        }
    }
}
