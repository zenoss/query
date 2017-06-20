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
    private String oldName;

    @JsonProperty(required=true)
    @NotNull
    private String newName;

    @JsonProperty(required=true)
    @NotNull
    private String type;

    @JsonProperty(required=true)
    @NotNull
    private String patternType;

    public final String getOldName() { return oldName; }

    public final void setOldName(String oldName) { this.oldName = oldName; }

    public final String getNewName() { return newName; }

    public final void setNewName(String newName) { this.newName = newName; }

    public final String getType() { return type; }

    public final String getPatternType() { return patternType; }
}
