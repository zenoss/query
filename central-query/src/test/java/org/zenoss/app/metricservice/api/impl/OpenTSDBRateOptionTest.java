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
import org.zenoss.app.metricservice.api.model.RateOptions;

import static org.junit.Assert.assertEquals;

public class OpenTSDBRateOptionTest {
    private static final Boolean COUNTER = true;
    private static final Long COUNTER_MAX = 13l;
    private static final Long RESET_THRESHOLD = 10l;
    private static final Boolean DROPRESETS = false;

    @Test
    public void testFromRateOptions() {
        RateOptions rateOptions = new RateOptions();
        rateOptions.setCounter(COUNTER);
        rateOptions.setCounterMax(COUNTER_MAX);
        rateOptions.setResetThreshold(RESET_THRESHOLD);
        rateOptions.setDropResets(DROPRESETS);
        OpenTSDBRateOption subject = new OpenTSDBRateOption(rateOptions);
        assertEquals(COUNTER, subject.counter);
        assertEquals((Long)COUNTER_MAX, Long.valueOf(subject.counterMax));
        assertEquals((Long)RESET_THRESHOLD, Long.valueOf(subject.resetValue));
        assertEquals(DROPRESETS, subject.dropResets);
    }

}
