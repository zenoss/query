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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration to specify the "type" or results that should be returned by the
 * performance metric query.
 * 
 * @author David Bainbridge <dbainbridge@zenoss.com>
 */
public enum ReturnSet {
    /**
     * By default, TSDB returns results that are outside the explicitly
     * requested time window. This ReturnSet value indicates that the entire
     * result set generated by TSDB should be returned, even if some of the
     * values are outside the requested time range.
     */
    ALL,

    /**
     * This specifies that the returned result set should only include those
     * results that fall within the requested time range and no "extra" results
     * should be included in the returned data.
     */
    EXACT,

    /**
     * This indicates that only the last, and presumably latest, values for each
     * metric should be returned. If a results is not returned for a metric that
     * means that there is no "latest" value within the requested time range.
     */
    LAST;

    /**
     * Provides a method used to generate a string for the resource when
     * converting the object to JSON. This just allows the JSON to be lowercase.
     * 
     * @return a lower cased string representation of the enumeration value.
     */
    @JsonValue
    public String toJson() {
        return toString().toLowerCase();
    }

    /**
     * Parses the given string to return the value for the enumeration. This
     * allows the JSON to specify the value in either lower or upper case.
     * 
     * @param name
     *            value to convert to an enumeration
     * @return ReturnSet enumeration that was parsed from the string.
     */
    @JsonCreator
    public static ReturnSet fromJson(final String name) {
        return valueOf(name.toUpperCase());
    }
}
