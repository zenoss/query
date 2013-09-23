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

package org.zenoss.app.metricsevice.buckets;

/**
 * Value holder for a metric's value within a bucket.
 * 
 * @author Zenoss
 */
final public class Value {

    /**
     * The running sum of all the values added into this value
     */
    private double sum = 0.0;

    /**
     * The count of items added into this value
     */
    private long count = 0;

    /**
     * The average of the values added into the value
     * 
     * @return average of the value
     */
    final public double getValue() {
        return sum / (double) count;
    }

    /**
     * Returns the running total of the value
     * 
     * @return running total
     */
    final public double getSum() {
        return sum;
    }

    /**
     * Returns the number of items added to the value
     * 
     * @return the number of items added
     */
    final public long getCount() {
        return count;
    }

    /**
     * Add a given number to the value
     * 
     * @param value
     *            the number to add
     */
    final public void add(final double value) {
        sum += value;
        count++;
    }

    /**
     * Removes a given number from the value
     * 
     * @param value
     *            the number to remove
     */
    final public void remove(final double value) {
        sum -= value;
        count--;
    }
}