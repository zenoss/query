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
import org.zenoss.app.metricservice.api.impl.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zenoss
 * 
 */
public class TagsTest {

    @Test
    public void factoryTest() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2 tag2=value3");

        List<String> val1 = new ArrayList<>();
        val1.add("value1");
        val1.add("value2");

        List<String> val2 = new ArrayList<>();
        val2.add("value3");

        Map<String, List<String>> spec = new HashMap<>();
        spec.put("tag1", val1);
        spec.put("tag2", val2);
        Tags tags2 = Tags.fromValue(spec);

        Assert.assertEquals("two different constructors", tags1, tags2);
    }


    @Test
    public void anotherFactoryTest() {
        Tags tags1 = Tags.fromValue("tag-1=value-1|value-2 tag-2=value-3");

        List<String> val1 = new ArrayList<>();
        val1.add("value 1");
        val1.add("value 2");

        List<String> val2 = new ArrayList<>();
        val2.add("value 3");

        Map<String, List<String>> spec = new HashMap<>();
        spec.put("tag 1", val1);
        spec.put("tag 2", val2);
        Tags tags2 = Tags.fromValue(spec);

        Assert.assertEquals("two different constructors", tags1, tags2);
    }

    @Test
    public void tagsIdentityEquals() {
        Tags tags = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertEquals("identity", tags, tags);
    }

    @Test
    public void nullEquals() {
        Tags tags = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertNotEquals("null equals", tags, null);
    }

    @Test
    public void contentEquals() {
        Tags tags1 = Tags.fromValue("tag1=value1 tag2=value2");
        Tags tags2 = Tags.fromValue("tag2=value2 tag1=value1");
        Assert.assertEquals("same content, should be equal", tags1, tags2);
    }

    @Test
    public void notTagsEquals() {
        Tags tags = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertNotEquals("compare tags to string", tags, "Hello");
    }

    @Test
    public void differentContentEquals() {
        Tags tags1 = Tags.fromValue("tag1=value1 tag2=value2");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=valueB");
        Assert.assertNotEquals("different content, should not be equal", tags1,
                tags2);
    }

    @Test
    public void patternEquals() {
        Tags tags1 = Tags.fromValue("tag1=* tag2=*");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertNotEquals("wild card pattern should not be equal", tags1,
                tags2);
    }

    @Test
    public void noSizeEquals() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2");
        Tags tags2 = Tags.fromValue("tag1=valueA tag2=value4");
        Assert.assertNotEquals("no tag equals", tags1, tags2);
    }

    @Test
    public void noTagEquals() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2, tag3=value3");
        Tags tags2 = Tags.fromValue("tag1=valueA tag2=value4");
        Assert.assertNotEquals("no tag equals", tags1, tags2);
    }

    @Test
    public void tagsIdentityMatch() {
        Tags tags = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertTrue("identity match", tags.match(tags));
    }

    @Test
    public void nullMatch() {
        Tags tags = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertFalse("null match", tags.match(null));
    }

    @Test
    public void contentMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1 tag2=value2");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertTrue("same content, should match", tags1.match(tags2));
    }

    @Test
    public void differentContentMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1 tag2=value2");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=valueB");
        Assert.assertFalse("same content, should not match", tags1.match(tags2));
    }

    @Test
    public void wildcardMatch() {
        Tags tags1 = Tags.fromValue("tag1=* tag2=*");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=value2");
        Assert.assertTrue("wild card should match", tags1.match(tags2));
    }

    @Test
    public void choiceMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2 tag2=value3|value4");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=value4");
        Assert.assertTrue("choice", tags1.match(tags2));
    }

    @Test
    public void choiceNoMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2 tag2=value3|value4");
        Tags tags2 = Tags.fromValue("tag1=valueA tag2=value4");
        Assert.assertFalse("no choice", tags1.match(tags2));
    }

    @Test
    public void noSizeMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2");
        Tags tags2 = Tags.fromValue("tag1=valueA tag2=value4");
        Assert.assertFalse("no size match", tags1.match(tags2));
    }

    @Test
    public void noSizeMatch2() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2 tag2=value4 tag3=value3");
        Tags tags2 = Tags.fromValue("tag1=value1 tag2=value4");
        Assert.assertFalse("not enough tags match", tags1.match(tags2));
    }

    @Test
    public void noTagMatch() {
        Tags tags1 = Tags.fromValue("tag1=value1|value2 tag3=value3");
        Tags tags2 = Tags.fromValue("tag1=valueA tag2=value4");
        Assert.assertFalse("no tag match", tags1.match(tags2));
    }

    @Test
    public void sizeCheck() {
        Tags tags = Tags.fromValue("tag1=value1|value2 tag2=value3|value4");
        Assert.assertEquals("should be 2", 2, tags.size());
    }

    @Test
    public void stringTest() {
        Tags tags = Tags.fromValue("tag1=value1|value2 tag2=value3|value4");
        String s = tags.toString();

        Assert.assertTrue(
                "to string",
                s.equals("{tag2=value3|value4,tag1=value1|value2}")
                        || s.equals("{tag1=value1|value2,tag2=value3|value4}"));
    }
}
