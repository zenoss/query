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
package org.zenoss.app.metricservice.v2;

import org.zenoss.app.metricservice.api.model.v2.MetricRequest;
import org.zenoss.app.metricservice.api.model.v2.QueryResult;
import org.zenoss.app.metricservice.api.model.v2.RenameRequest;
import org.zenoss.app.metricservice.api.model.v2.RenameResult;

public interface QueryService {

    QueryResult query(MetricRequest query);

    RenameResult rename(RenameRequest renameRequest);
}
