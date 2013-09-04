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

package org.zenoss.app.metricservice.calculators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.api.impl.RedisResourcePersistenceFactory;

/**
 * @author david
 * 
 */
public abstract class MetricCalculator {
    private static final Logger log = LoggerFactory
            .getLogger(MetricCalculator.class);

    abstract public double evaluate(double value, String expression);

    abstract public double evaluate(double value);

    private String expr = null;

    public void setExpression(String expr) {
        this.expr = expr;
    }

    public String getExpression() {
        return this.expr;
    }

    private static final String DEFAULT_PACKAGE_PATH = "org.zenoss.app.metricservice.calculators";
    private static final String PACKAGE_PATH = System.getProperty(
            "org.zenoss.app.metricservice.calculator.path",
            DEFAULT_PACKAGE_PATH);

    @SuppressWarnings("unchecked")
    public static MetricCalculator create(String expr)
            throws ClassNotFoundException {

        String[] terms = expr.split(":", 2);
        String[] paths = PACKAGE_PATH.split(":");

        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder("Search for '");
            buf.append(terms[0]);
            buf.append('.');
            buf.append("Calculator");
            buf.append("' in ");
            char prefix = '[';
            for (String path : paths) {
                buf.append(prefix);
                buf.append(path);
                prefix = ',';
            }
            buf.append(']');
            log.error(buf.toString());
        }
        StringBuilder classname = new StringBuilder();
        Class<? extends MetricCalculator> clazz = null;
        MetricCalculator calc = null;
        for (String path : paths) {
            classname.setLength(0);
            classname.append(path);
            classname.append('.');
            classname.append(terms[0]);
            classname.append('.');
            classname.append("Calculator");
            clazz = null;
            try {
                clazz = (Class<? extends MetricCalculator>) Class
                        .forName(classname.toString());
                calc = clazz.newInstance();
                calc.setExpression(terms[1]);
                log.debug("Found class '{}' to evaluate expressions of type '{}'",
                        classname, terms[0]);
                return calc;
            } catch (Exception e) {
                log.debug(
                        "Expression evaluation implementation '{}' does not exist",
                        classname.toString());
                // ignore, maybe on the next path segment
            }
        }
        throw new ClassNotFoundException(
                String.format(
                        "Unable to find a class that implementations the expression evaluation for type '{}'",
                        terms[0]));
    }
}
