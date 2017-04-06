package org.zenoss.app.metricservice.api.impl;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


public class OpenTSDBFilter {
    private String type = null;
    private String tagk = null;
    private String filter = null;
    private Boolean groupBy = false;

    public OpenTSDBFilter(String type, String tagk, String filter, Boolean groupBy) {
        this.type = type;
        this.tagk = tagk;
        this.filter = filter;
        this.groupBy = groupBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTagk() {
        return tagk;
    }

    public void setTagk(String tagk) {
        this.tagk = tagk;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Boolean getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(Boolean groupBy) {
        this.groupBy = groupBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) return false;

        OpenTSDBFilter that = (OpenTSDBFilter) o;

        return new EqualsBuilder()
            .append(type, that.type)
            .append(tagk, that.tagk)
            .append(filter, that.filter)
            .append(groupBy, that.groupBy)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(type)
            .append(tagk)
            .append(filter)
            .append(groupBy)
            .toHashCode();
    }
}
