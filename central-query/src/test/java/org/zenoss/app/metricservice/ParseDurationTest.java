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
package org.zenoss.app.metricservice;

import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.Utils;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class ParseDurationTest {

    @Test
    public void testDuration_1s() {
        Assert.assertEquals("1s", 1, Utils.parseDuration("1s"));
    }

    @Test
    public void testDuration_1m() {
        Assert.assertEquals("1m", 60, Utils.parseDuration("1m"));
    }

    @Test
    public void testDuration_1h() {
        Assert.assertEquals("1h", 60 * 60, Utils.parseDuration("1h"));
    }

    @Test
    public void testDuration_1d() {
        Assert.assertEquals("1d", 60 * 60 * 24, Utils.parseDuration("1d"));
    }

    @Test
    public void testDuration_1w() {
        Assert.assertEquals("1w", 60 * 60 * 24 * 7, Utils.parseDuration("1w"));
    }

    @Test
    public void testDuration_1y() {
        Assert.assertEquals("1y", 60 * 60 * 24 * 365, Utils.parseDuration("1y"));
    }
    
    @Test
    public void testDuration_5h() {
        Assert.assertEquals("5h", 60 * 60 * 5, Utils.parseDuration("5h"));
    }
    
    @Test
    public void testDuration_badType1h() {
        Assert.assertEquals("1z", 0, Utils.parseDuration("1z"));
    }
    
    @Test
    public void testDuration_badPeriod() {
        Assert.assertEquals("dh", 0, Utils.parseDuration("dh"));
    }
}
