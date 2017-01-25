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
package org.zenoss.app.metricservice.api.model.v3;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

public class DatapointSerializer extends JsonSerializer<SortedMap<Long, Double>> {


    @Override
    public void serialize(SortedMap<Long, Double> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeStartArray();
        for (Map.Entry<Long, Double> dp : value.entrySet()) {
            jgen.writeStartArray();
            jgen.writeNumber(dp.getKey());
            jgen.writeNumber(dp.getValue());
            jgen.writeEndArray();
        }
        jgen.writeEndArray();
    }
}
