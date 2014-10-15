package org.zenoss.app.metricservice.api.impl;/*
 * Copyright (c) 2014, Zenoss and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Zenoss or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class OpenTSDBQueryResultTest {
    private static final double TEST_POINT_VALUE = 1.2345;
    private static final long TEST_I = 47123l;

    @Test
    public void testDataPointsMapIsNotNullOnGet() {
        OpenTSDBQueryResult subject = new OpenTSDBQueryResult();
        assertTrue("DataPoints list is non-null on initialization", null != subject.getDataPoints());
    }

    @Test
    public void testDataPointsMapIsNotNullOnGetAfterSettingToNull() {
        OpenTSDBQueryResult subject = new OpenTSDBQueryResult();
        subject.setDataPoints(null);
        assertTrue("DataPoints list is non-null after setting null", null != subject.getDataPoints());
    }

    @Test
    public void testAddWorksAfterSettingDataPointsToNull() {
        OpenTSDBQueryResult subject = new OpenTSDBQueryResult();
        subject.setDataPoints(null);
        subject.addDataPoint(TEST_I, TEST_POINT_VALUE);
        assertTrue("DataPoints list is populated properly after setting null", subject.getDataPoints().containsKey(TEST_I));
    }
}
