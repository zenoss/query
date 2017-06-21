package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * This class contains the information neccessary to fulfil a rename operation.
 *     oldName: A search word
 *     newName: A word that will replace the search word.
 *     type: Either metric or tagv.
 *     patternType: Either prefix or whole. If prefix, a list of entities that
 *                  shares oldName as a prefix will be renamed altogether.
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

    public static final String TYPE_METRIC = "metric";
    public static final String TYPE_TAGV = "tagv";

    public final String getOldName() { return oldName; }

    public final String getNewName() { return newName; }

    public final String getType() { return type; }

    public final String getPatternType() { return patternType; }
}
