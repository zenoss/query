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

/**
 * Factory class to dynamically create instances of metric expression evaluation
 * engines based on a simplified URI specification. The URI specification is
 * essentially the expression language, followed by a colon, followed by an
 * intial expression.
 * 
 * If the initial expression is ommited, the colon can be ommited as well.
 * 
 * The implementation of the expression language calculated is dynamically
 * created by using a well known class name, Calculator, located in a subpackage
 * the shares the name of the expression language, i.e. if the expression
 * language is 'rpn', then the implementation class will be 'rpn.Calculator'.
 * 
 * The implementation class is searched for as a sub-package under the packages
 * specified in the system property
 * 'org.zenoss.app.metricservice.calculator.path'. This property contains a list
 * of base package names separated by a colons. The default path is the single
 * entry 'org.zenoss.app.metricservice.calculators'.
 */
public class MetricCalculatorFactory {
    private static final Logger log = LoggerFactory
            .getLogger(MetricCalculatorFactory.class);

    public static final String CALCULATOR_PATH_PROPERTY = "org.zenoss.app.metricservice.calculator.path";
    public static final String DEFAULT_CALCULATOR_PATH = "org.zenoss.app.metricservice.calculators";

    /**
     * Constructs an expression evaluator calculator based on the expression
     * given as the parameter.
     * 
     * @param expr
     *            a full or partial expression URI of the format
     *            <i>language></i>[:<i>language expression</i>].
     * @return the constructed expression calculator
     * @throws ClassNotFoundException
     *             if the expression calculator for the given language cannot be
     *             located or if it is found and can't be narrowed to a
     *             MetricCalculator.
     */
    public MetricCalculator newInstance(String expr)
            throws ClassNotFoundException {

        String[] terms = expr.split(":", 2);
        String[] paths = System.getProperty(CALCULATOR_PATH_PROPERTY,
                DEFAULT_CALCULATOR_PATH).split(":");

        // If we are in debug mode, log the search path
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
            log.debug(buf.toString());
        }

        StringBuilder classname = new StringBuilder();
        Class<? extends MetricCalculator> clazz = null;
        MetricCalculator calc = null;

        // Walk the path attempting to load the class.
        for (String path : paths) {
            classname.setLength(0);
            classname.append(path);
            classname.append('.');
            classname.append(terms[0]);
            classname.append('.');
            classname.append("Calculator");
            clazz = null;
            try {
                clazz = (Class<? extends MetricCalculator>) Class.forName(
                        classname.toString())
                        .asSubclass(MetricCalculator.class);
                calc = clazz.newInstance();
                if (terms.length > 1) {
                    calc.setExpression(terms[1]);
                }
                log.debug(
                        "Found class '{}' to evaluate expressions of type '{}'",
                        classname, terms[0]);
                return calc;
            } catch (Exception e) {
                log.debug(
                        "Expression evaluation implementation '{}' does not exist",
                        classname.toString());
                // ignore, maybe on the next path segment
            }
        }

        // Nothing found, throw an exception
        throw new ClassNotFoundException(
                String.format(
                        "Unable to find a class that implementations the expression evaluation for type '%s'",
                        terms[0]));
    }
}
