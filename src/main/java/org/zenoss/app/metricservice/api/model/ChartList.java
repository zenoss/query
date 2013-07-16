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
 * PROFITS; OR BUSINESS IntegerERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.zenoss.app.metricservice.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Resource class that represents a list of resource ID, including information
 * about the list, including index range and option total count
 * 
 * @author David Bainbridge <dbainbridge@zenoss.com>
 */
@JsonInclude(Include.NON_NULL)
public class ChartList {

    @JsonProperty(required = true)
    private Integer start = null;

    @JsonProperty(required = true)
    private Integer end = null;

    @JsonProperty(required = false)
    private Long count = null;

    @JsonProperty(required = true)
    List<String> ids = new ArrayList<String>();

    /**
     * @return the start
     */
    public final Integer getStart() {
        return start;
    }

    /**
     * @param start
     *            the start to set
     */
    public final void setStart(Integer start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public final Integer getEnd() {
        return end;
    }

    /**
     * @param end
     *            the end to set
     */
    public final void setEnd(Integer end) {
        this.end = end;
    }

    /**
     * @return the ids
     */
    public final List<String> getIds() {
        return ids;
    }

    /**
     * @param uuids
     *            the ids to set
     */
    public final void setIds(List<String> ids) {
        this.ids = ids;
    }

    /**
     * @return the count
     */
    public final Long getCount() {
        return count;
    }

    /**
     * @param count
     *            the count to set
     */
    public final void setCount(Long count) {
        this.count = count;
    }
}
