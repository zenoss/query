/*
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

package org.zenoss.app.metricservice.api.impl;

import org.zenoss.app.metricservice.api.model.RateOptions;

public class OpenTSDBRateOption {
    /*
     * The default rate options combination is treated as "dropcounter"
     * at REST API level to avoid wraparound spikes
     */
    public boolean counter = true;
    public long counterMax = Long.MAX_VALUE;
    public long resetValue = 0;
    public boolean dropResets = true;

    public OpenTSDBRateOption() {}

    public OpenTSDBRateOption(RateOptions rateOptions) {
        if (null != rateOptions) {
            if (null != rateOptions.getCounter()) {
                counter = rateOptions.getCounter();
            }
            if (null != rateOptions.getCounterMax()) {
                counterMax = rateOptions.getCounterMax();
            }
            if (null != rateOptions.getResetThreshold()) {
                resetValue = rateOptions.getResetThreshold();
            }
            if (null != rateOptions.getDropResets()) {
                dropResets = rateOptions.getDropResets();
            }
        }
    }
}
