/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
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
package org.zenoss.app.metricservice.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 *
 */
@JsonInclude(Include.NON_NULL)
public class RateOptions {
    @JsonProperty
    private Boolean counter = null;
    
    @JsonProperty
    private Long counterMax = null;
    
    @JsonProperty
    private Long resetThreshold = null;

    @JsonProperty
    private Boolean dropResets = null;

    /**
     * @return the counter
     */
    public final Boolean getCounter() {
        return counter;
    }

    /**
     * @param counter the counter to set
     */
    public final void setCounter(Boolean counter) {
        this.counter = counter;
    }

    /**
     * @return the counterMax
     */
    public final Long getCounterMax() {
        return counterMax;
    }

    /**
     * @param counterMax the counterMax to set
     */
    public final void setCounterMax(Long counterMax) {
        this.counterMax = counterMax;
    }

    /**
     * @return the resetThreshold
     */
    public final Long getResetThreshold() {
        return resetThreshold;
    }

    /**
     * @param resetThreshold the resetThreshold to set
     */
    public final void setResetThreshold(Long resetThreshold) {
        this.resetThreshold = resetThreshold;
    }

    /**
     * @return the dropResets
     */
    public final Boolean getDropResets() {
        return dropResets;
    }

    /**
     * @param dropResets the dropResets to set
     */
    public final void setDropResets(Boolean dropResets) {
        this.dropResets = dropResets;
    }
}
