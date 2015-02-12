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

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class RateOptionsTest {

    @Test
    public void parseCounter() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertNull(ms.getRateOptions().getCounterMax());
        Assert.assertNull(ms.getRateOptions().getResetThreshold());
        Assert.assertEquals("avg:rate{counter}:laLoadInt", ms.toString(true));
    }

    @Test
    public void parseCounterThreshold() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter,,2000}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertNull(ms.getRateOptions().getCounterMax());
        Assert.assertEquals((long) 2000, (long) ms.getRateOptions()
                .getResetThreshold());
        Assert.assertEquals("avg:rate{counter,,2000}:laLoadInt",
                ms.toString(true));
    }

    @Test
    public void parseCounterSpaceThreshold() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter, ,2000}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertNull(ms.getRateOptions().getCounterMax());
        Assert.assertEquals((long) 2000, (long) ms.getRateOptions()
                .getResetThreshold());
        Assert.assertEquals("avg:rate{counter,,2000}:laLoadInt",
                ms.toString(true));
    }

    @Test
    public void parseCounterMax() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter,987654321}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertEquals((long) 987654321, (long) ms.getRateOptions()
                .getCounterMax());
        Assert.assertNull(ms.getRateOptions().getResetThreshold());
        Assert.assertEquals("avg:rate{counter,987654321}:laLoadInt",
                ms.toString(true));
    }

    @Test
    public void parseCounterMaxSpace() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter,987654321, }:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertEquals((long) 987654321, (long) ms.getRateOptions()
                .getCounterMax());
        Assert.assertNull(ms.getRateOptions().getResetThreshold());
        Assert.assertEquals("avg:rate{counter,987654321}:laLoadInt",
                ms.toString(true));
    }

    @Test
    public void parseFullTest() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:rate{counter," + Long.MAX_VALUE
                        + ",2000}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertEquals(Long.MAX_VALUE, (long) ms.getRateOptions()
                .getCounterMax());
        Assert.assertEquals((long) 2000, (long) ms.getRateOptions()
                .getResetThreshold());
        Assert.assertEquals("avg:rate{counter," + Long.MAX_VALUE
                + ",2000}:laLoadInt", ms.toString(true));

    }

    @Test
    public void parseFullWithDownsampleTest() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:10m-avg:rate{counter," + Long.MAX_VALUE
                        + ",2000}:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNotNull(ms.getRateOptions());
        Assert.assertTrue(ms.getRateOptions().getCounter());
        Assert.assertEquals(Long.MAX_VALUE, (long) ms.getRateOptions()
                .getCounterMax());
        Assert.assertEquals((long) 2000, (long) ms.getRateOptions()
                .getResetThreshold());
        Assert.assertEquals("avg:10m-avg:rate{counter," + Long.MAX_VALUE
                + ",2000}:laLoadInt", ms.toString(true));

    }

    @Test
    public void parseNoRateWithDownsampleTest() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:10m-avg:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNull(ms.getRateOptions());
    }

    @Test
    public void parseJustRateWithDownsampleTest() {
        MetricSpecification ms = MetricSpecification
                .fromString("avg:10m-avg:rate:laLoadInt");
        Assert.assertNotNull(ms);
        Assert.assertNull(ms.getRateOptions());
    }

    @Test
    public void parseNoCounter() {
        try {
            MetricSpecification
                    .fromString("avg:rate{987654321,987654321}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseEmpty() {
        try {
            MetricSpecification.fromString("avg:rate{}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseBadRequest1() {
        try {
            MetricSpecification.fromString("avg:rate{:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseBadRequest2() {
        try {
            MetricSpecification.fromString("avg:1m-avg:rate{:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseBadRequest3() {
        try {
            MetricSpecification.fromString("avg:10m-avg:bad-value:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseBadMaxFormat() {
        try {
            MetricSpecification
                    .fromString("avg:1m-avg:rate{counter,NotANumber}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseBadResetFormat() {
        try {
            MetricSpecification
                    .fromString("avg:1m-avg:rate{counter,,NotANumber}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseTooManyOptions() {
        try {
            MetricSpecification
                    .fromString("avg:1m-avg:rate{counter,123123,123123,ExtraOption}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

    @Test
    public void parseNoOptions() {
        try {
            MetricSpecification.fromString("avg:1m-avg:rate{}:laLoadInt");
            Assert.fail("should have gotten exception");
        } catch (WebApplicationException wae) {
            Assert.assertEquals("bad request", 400, wae.getResponse()
                    .getStatus());
        }
    }

}
