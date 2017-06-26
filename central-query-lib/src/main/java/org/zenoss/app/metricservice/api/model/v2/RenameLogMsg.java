package org.zenoss.app.metricservice.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

/**
 * This class is for the log messages streamed back to the requester.
 *     type: message type
 *     content: the content of the message
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RenameLogMsg {
    @JsonProperty(required=true)
    @NotNull
    private String type;

    @JsonProperty(required=true)
    @NotNull
    private String content;

    public static final String TYPE_INFO = "info";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_PROGRESS = "progress";

    public void setType(String type) { this.type = type; }
    public final String getType() { return this.type; }
    public void setContent(String content) { this.content = content; }
    public final String getContent() { return this.content; }
}
