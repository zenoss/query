package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * Created by maya on 5/12/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenameRequest {
    @JsonProperty(required=true)
    @NotNull
    private String oldId;

    @JsonProperty(required=true)
    @NotNull
    private String newId;

    public final String getOldId() { return oldId; }

    public final void setOldId(String oldId) { this.oldId = oldId; }

    public final String getNewId() { return newId; }

    public final void setNewId(String newId) { this.newId = newId; }
}
