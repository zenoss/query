package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Created by maya on 5/12/17.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenameRequest {
    @JsonProperty(required=true)
    @NotNull
    private String oldPrefix;

    @JsonProperty(required=true)
    @NotNull
    private String newPrefix;

    /**
     * @return the old device id prefix
     */
    public final String getOldPrefix() { return oldPrefix; }

    /**
     * @param oldPrefix the prefix that will be overwritten
     */
    public final void setOldPrefix(String oldPrefix) { this.oldPrefix = oldPrefix; }

    /**
     * @return the new device id prefix
     */
    public final String getNewPrefix() { return newPrefix; }

    /**
     * @param newPrefix the new prefix
     */
    public final void setNewPrefix(String newPrefix) { this.newPrefix = newPrefix; }
}
