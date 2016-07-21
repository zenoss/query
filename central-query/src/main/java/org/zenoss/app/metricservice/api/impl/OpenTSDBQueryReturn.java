/*
 * ****************************************************************************
 *
 *  Copyright (C) Zenoss, Inc. 2015, all rights reserved.
 *
 *  This content is made available according to terms specified in
 *  License.zenoss distributed with this file.
 *
 * ***************************************************************************
 */
package org.zenoss.app.metricservice.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OpenTSDBQueryReturn {
    private static final Logger log = LoggerFactory.getLogger(OpenTSDBQueryReturn.class);

    final List<OpenTSDBQueryResult> results;
    final private QueryStatus status;

    public OpenTSDBQueryReturn(OpenTSDBQueryResult[] results, QueryStatus status) {
        this.results = Collections.unmodifiableList(Arrays.asList(results));
        if (null == status) {
            log.warn("OpenTSDBQueryReturn constructor was called with null status. Defaulting to 'UNKNOWN'");
            this.status = new QueryStatus();
        } else {
            this.status = status;
        }
    }

    public List<OpenTSDBQueryResult> getResults() {
        return results;
    }

    public QueryStatus getStatus() {
        return status;
    }

}
