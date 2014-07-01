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

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.zenoss.app.metricservice.api.impl.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public class JsonWriterIntegrationTest {

    private void test(String name, Object value, Optional<Boolean> appendComma)
            throws IOException {
        StringWriter sw = new StringWriter();
        boolean expectComma = false;
        boolean quoteValue = false;
        try (JsonWriter jw = new JsonWriter(sw)) {

            if (appendComma.isPresent()) {
                expectComma = appendComma.get();

                if (value instanceof Integer) {
                    jw.value(name, (Integer) value, appendComma.get());
                } else if (value instanceof Long) {
                    jw.value(name, (Long) value, appendComma.get());
                } else if (value instanceof Float) {
                    jw.value(name, (Float) value, appendComma.get());
                } else if (value instanceof Double) {
                    jw.value(name, (Double) value, appendComma.get());
                } else if (value instanceof Boolean) {
                    jw.value(name, (Boolean) value, appendComma.get());
                } else if (value instanceof String) {
                    quoteValue = true;
                    jw.value(name, (String) value, appendComma.get());
                } else {
                    Assert.fail("unknown value type");
                }
            } else {
                if (value instanceof Integer) {
                    jw.value(name, (Integer) value);
                } else if (value instanceof Long) {
                    jw.value(name, (Long) value);
                } else if (value instanceof Float) {
                    jw.value(name, (Float) value);
                } else if (value instanceof Double) {
                    jw.value(name, (Double) value);
                } else if (value instanceof Boolean) {
                    jw.value(name, (Boolean) value);
                } else if (value instanceof String) {
                    quoteValue = true;
                    jw.value(name, (String) value);
                } else {
                    Assert.fail("unknown value type");
                }
            }

            jw.flush();
            if (quoteValue) {
                Assert.assertEquals(String.format("\"%s\":\"%s\"%s", name,
                        value, (expectComma ? "," : "")), sw.toString());
            } else {
                Assert.assertEquals(String.format("\"%s\":%s%s", name, value,
                        (expectComma ? "," : "")), sw.toString());
            }
        }
    }

    private static final Optional<Boolean> OptFalse = Optional.of(false);
    private static final Optional<Boolean> OptTrue = Optional.of(true);
    private static final Optional<Boolean> OptAbsent = Optional
            .<Boolean> absent();

    @Test
    public void intValueTests() throws IOException {
        test("name", (int) 1, OptFalse);
        test("name", (int) 1, OptTrue);
        test("name", (int) 1, OptAbsent);
    }

    @Test
    public void longValueTests() throws IOException {
        test("name", (long) 1, OptFalse);
        test("name", (long) 1, OptTrue);
        test("name", (long) 1, OptAbsent);
    }

    @Test
    public void floatValueTests() throws IOException {
        test("name", (float) 1.123, OptFalse);
        test("name", (float) 1.123, OptTrue);
        test("name", (float) 1.123, OptAbsent);
    }

    @Test
    public void doubleValueTests() throws IOException {
        test("name", (double) 1.123, OptFalse);
        test("name", (double) 1.123, OptTrue);
        test("name", (double) 1.123, OptAbsent);
    }

    @Test
    public void booleanValueTests() throws IOException {
        test("name", (boolean) true, OptFalse);
        test("name", (boolean) true, OptTrue);
        test("name", (boolean) true, OptAbsent);
    }

    @Test
    public void stringValueTests() throws IOException {
        test("name", (String) "hello, world", OptFalse);
        test("name", (String) "hello, world", OptTrue);
        test("name", (String) "hello, world", OptAbsent);
    }

    @Test
    public void arrayTests() throws IOException {
        StringWriter sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS().arrayE();
            jw.flush();
            Assert.assertEquals("[]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS(null).arrayE();
            jw.flush();
            Assert.assertEquals("[]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS().arrayE(true);
            jw.flush();
            Assert.assertEquals("[],", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS().arrayE(false);
            jw.flush();
            Assert.assertEquals("[]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS("name").arrayE();
            jw.flush();
            Assert.assertEquals("\"name\":[]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS("name").arrayE(true);
            jw.flush();
            Assert.assertEquals("\"name\":[],", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.arrayS("name").arrayE(false);
            jw.flush();
            Assert.assertEquals("\"name\":[]", sw.toString());
        }
    }

    @Test
    public void objectTests() throws IOException {
        StringWriter sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS().objectE();
            jw.flush();
            Assert.assertEquals("{}", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS(null).objectE();
            jw.flush();
            Assert.assertEquals("{}", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS().objectE(true);
            jw.flush();
            Assert.assertEquals("{},", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS().objectE(false);
            jw.flush();
            Assert.assertEquals("{}", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS("name").objectE();
            jw.flush();
            Assert.assertEquals("\"name\":{}", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS("name").objectE(true);
            jw.flush();
            Assert.assertEquals("\"name\":{},", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter jw = new JsonWriter(sw)) {
            jw.objectS("name").objectE(false);
            jw.flush();
            Assert.assertEquals("\"name\":{}", sw.toString());
        }
    }
}
