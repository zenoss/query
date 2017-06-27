package org.zenoss.app.metricservice.api.impl;

public class OpenTSDBRenameResult {

    public OpenTSDBRenameResult(){
    }

    private String result;
    private String error;

    public String getResult() { return this.result; }
    public String getError() { return this.error; }
}
