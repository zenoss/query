package org.zenoss.app.metricservice.buckets;

/*
 * Copyright (c) 2014, Zenoss and/or its affiliates. All rights reserved.
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

import org.zenoss.app.metricservice.api.impl.IHasShortcut;

import java.util.Collection;

/**
 * Interface for interpolating data series stored in the Buckets structure.
 */
public interface Interpolator {
    /**
     * Provides in-place interpolation for one or more data series contained in a Buckets structure.
     * There is no return value - Interpolated values are added directly to the Buckets structure.
     *
     * @param buckets -the data to be interpolated. Interpolation is done in-place by modifying this object
     *                (preferably using the Buckets.addInterpolated() method. This will keep interpolated values
     *                distinct from calculated/provided values.
     * @param keys - A collection of IHasShortcut keys indicating which series within the buckets structure
     *             are to be interpolated.
     *
     * @see Buckets
     */
    public void interpolate(Buckets<IHasShortcut> buckets, Collection<IHasShortcut> keys);
}
