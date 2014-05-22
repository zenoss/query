package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.api.metric.impl.MetricService;
import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.api.model.ReturnSet;
import org.zenoss.app.metricservice.buckets.Buckets;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/19/14
 * Time: 5:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class JacksonResultsWriter {

    public void writeResults(JacksonWriter writer, List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset, boolean series) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
        if (series) {
            SeriesQueryResult results = makeResults(queries, buckets, id, sourceId, startTs, startTimeConfig, endTs, endTimeConfig, returnset);
            // write results (JSON serialization)
            writer.write(Utils.jsonStringFromObject(results));
        } else {
            //TODO: make a new exception type for this and throw it. Determine whether it needs to be a WebException or not.
            throw new Exception("non-series data is no longer supported.");
        }
    }

    private SeriesQueryResult makeResults(List<MetricSpecification> queries, Buckets<MetricKey, String> buckets, String id, String sourceId, long startTs, String startTimeConfig, long endTs, String endTimeConfig, ReturnSet returnset) {
        SeriesQueryResult result = new SeriesQueryResult();
        result.setId(id);
        result.setClientId(MetricService.CLIENT_ID);
        result.setEndTime(endTimeConfig);
        result.setEndTimeActual(endTs);
        result.setReturnset(returnset);
        result.setSeries(true);
        result.setSource(sourceId);
        result.setStartTime(startTimeConfig);
        result.setStartTimeActual(startTs);
        return result;
    }
}
