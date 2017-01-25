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
package org.zenoss.app.metricservice.v3;

import org.zenoss.app.metricservice.api.model.v3.MetricRequest;
import org.zenoss.app.metricservice.api.model.v3.QueryResult;

public interface QueryService {

    QueryResult query(MetricRequest query);

}
