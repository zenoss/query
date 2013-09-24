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

package org.zenoss.app.metricservice.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A comparable and matchable representation of metric tags
 * 
 * @author Zenoss
 */
public class Tags {

    /**
     * Maps a tag name to its values.
     */
    private Map<String, String> tags = new HashMap<String, String>();

    /*
     * Constructor
     */
    public Tags() {
    }

    /**
     * Constructs a Tags representation from a string representation of of a
     * tags list. Useful when dealing with the results from OpenTSDB.
     * 
     * @param value
     *            string representation of a tags list
     * @return tag representation
     */
    public static Tags fromValue(String value) {
        Tags tags = new Tags();
        int eq;

        for (String term : value.split(" ")) {
            eq = term.indexOf('=');
            if (eq != -1) {
                tags.tags.put(term.substring(0, eq), term.substring(eq + 1));
            }
        }
        return tags;
    }
    
    /**
     * Construct a Tags representation from a map of tag names to tag values.
     * 
     * @param tags
     *            the map of tag names to tag values
     * @return Tags representation.
     */
    public static Tags fromValue(Map<String, List<String>> tags) {
        Tags result = new Tags();
        StringBuilder buf = new StringBuilder();
        boolean pipe;
        for (Entry<String, List<String>> entry : tags.entrySet()) {
            buf.setLength(0);
            pipe = false;
            for (String value : entry.getValue()) {
                if (pipe) {
                    buf.append('|');
                } else {
                    pipe = true;
                }
                buf.append(value);
            }
            result.tags.put(entry.getKey(), buf.toString());
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('{');
        boolean comma = false;
        for (Entry<String, String> entry : tags.entrySet()) {
            if (comma) {
                buf.append(',');
            } else {
                comma = true;
            }
            buf.append(entry.getKey());
            buf.append('=');
            buf.append(entry.getValue());
        }
        buf.append('}');
        return buf.toString();
    }

    /**
     * Returns the number of tags in the representation
     * 
     * @return
     */
    public int size() {
        return tags.size();
    }

    /**
     * Determines if two instances are equivalent. Two instances are considered
     * equivalent if they have the same tags and if the tags have the same
     * values.
     * 
     * @param other
     *            instance to which to compare
     * @return true if they are equal, else false
     */
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Tags)) {
            return false;
        }
        
        Tags other = (Tags) object;
        if (this.tags.size() != other.tags.size()) {
            return false;
        }

        /*
         * Both the left and right side exist and they have the same number of
         * elements, so now the element have to be walked to determine if they
         * contain the same keys and values.
         */
        for (String key : this.tags.keySet()) {
            if (!other.tags.containsKey(key)
                    || !this.tags.get(key).equals(other.tags.get(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines is the given tags match the primary instances. Tags are
     * considered a match if the "other" has at minimum all the tags as "this"
     * and the "other" tags values match any patterns specified in the "this"
     * tag values.
     * 
     * @param other
     *            the instance to which to compare
     * @return true if they match, else false
     */
    public boolean match(Tags other) {
        if (other == null || this.tags.size() > other.tags.size()) {
            return false;
        }

        String btv, tv, ov;
        for (Entry<String, String> entry : tags.entrySet()) {
            /*
             * If the key is not in the "other" then done. Other may have more
             * but should contain all from "this"
             */
            if (!other.tags.containsKey(entry.getKey())) {
                return false;
            }

            tv = entry.getValue();
            ov = other.tags.get(entry.getKey());
            btv = '|' + tv + '|';

            /*
             * Now we need to look at the two values. if "tv" contains "*" or
             * then any "ov" is accepted. If "tv" contains "|" then we have to
             * check if any of the options are in "ov". Else we are looking for
             * an exact match
             */
            if (tv.indexOf('|') != -1) {
                if (btv.indexOf('|' + ov + '|') == -1) {
                    return false;
                }
            } else if (btv.indexOf("|*|") == -1 && !tv.equals(ov)) {
                return false;
            }
        }
        return true;
    }
}
