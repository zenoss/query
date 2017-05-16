package org.zenoss.app.metricservice.api.impl;

/**
 * Created by maya on 5/15/17.
 */
public class OpenTSDBRenameReturn {
    final OpenTSDBRenameResult result;

    public OpenTSDBRenameReturn(OpenTSDBRenameResult result) {
        this.result = result;
    }

    public OpenTSDBRenameResult getResult() {
        return result;
    }
}
