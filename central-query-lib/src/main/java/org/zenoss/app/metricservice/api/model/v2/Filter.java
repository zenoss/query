package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * This class represents the API filter model object for OpenTSDB query filters.
 * Support for filters was added to OpenTSDB in v2.2.
 * @see <a href="http://opentsdb.net/docs/build/html/api_http/query/index.html">
 *          OpenTSDB Query API Endpoints</a>
 */
public class Filter {
    @JsonProperty
    @NotNull
    private String type = null;

    @JsonProperty
    @NotNull
    private String tagk = null;

    @JsonProperty
    @NotNull
    private String filter = null;

    @JsonProperty
    @NotNull
    private Boolean groupBy = false;

    @JsonCreator
    public Filter(@JsonProperty("type") String type,
                  @JsonProperty("tagk") String tagk,
                  @JsonProperty("filter") String filter,
                  @JsonProperty("groupBy") Boolean groupBy) {
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
}
