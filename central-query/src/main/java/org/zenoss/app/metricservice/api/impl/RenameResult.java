package org.zenoss.app.metricservice.api.impl;

/**
 * Created by maya on 5/12/17.
 */
public class RenameResult {

    public RenameResult(){
    }

    public String reason;
    public int code;
    public OpenTSDBRename request;
}
