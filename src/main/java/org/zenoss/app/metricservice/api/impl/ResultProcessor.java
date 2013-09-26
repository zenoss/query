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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.zenoss.app.metricservice.api.model.MetricSpecification;
import org.zenoss.app.metricservice.buckets.Buckets;
import org.zenoss.app.metricservice.calculators.UnknownReferenceException;

/**
 * Specifies the interface for implementations that process the results from the
 * metric storage. Implementation should process the data into a Buckets
 * representation.
 * 
 * @author Zenoss
 */
public interface ResultProcessor {
    /**
     * Processes the input stream from the metric storage engine to a Buckets
     * representation
     * 
     * @param reader
     *            input stream from which lines should be read from the the
     *            metric storage engine
     * @param queries
     *            queries that generated the results
     * @param bucketsize
     *            the size of the buckets that should be generated
     * @return
     * @throws IOException
     *             when an exception occurs when reading from the metric storage
     *             engine
     * @throws ClassNotFoundException
     *             when a calculation engine cannot be loaded
     * @throws UnknownReferenceException
     *             when a reference in an expression cannot be found.
     */
    public Buckets<MetricKey, String> processResults(BufferedReader reader,
            List<MetricSpecification> queries, long bucketsize)
            throws IOException, ClassNotFoundException,
            UnknownReferenceException;
}
