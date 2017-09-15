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

/**
 * Interface to be implemented by all expression calculators. This interfaces
 * defines the methods required to execute expressions on performance metrics.
 */
public interface MetricCalculator {

    void setReferenceProvider(ReferenceProvider provider);

    /**
     * Stores an expression so that the calculator can evaluate the expression
     * multiple times with out having to set the expression each time.
     * 
     * @param expression
     *            the expression to save
     */
    void setExpression(String expression);

    /**
     * Retrieve the stored expression
     * 
     * @return the stored expression
     */
    String getExpression();

    /**
     * Evaluate the given expression using the given value as an initial value
     * into that expression. i.e. in the case of an RPN evaluator the value
     * might be pushed on the stack before the expression is evaluated and in
     * the case of a precedence evaluation the value might be considered the
     * left had side.
     * 
     * @param value
     *            the initial value in the evaluation
     * @param expression
     *            the expression to evaluate
     * @return result of the expression evaluation
     */
    double evaluate(double value, String expression, Closure closure)
            throws UnknownReferenceException, BadExpressionException;

    double evaluate(double value, String expression)
            throws UnknownReferenceException, BadExpressionException;

    /**
     * Evaluate the given expression.
     * 
     * @param expression
     *            the expression to evaluate
     * @return result of the expression evaluation
     */
    public double evaluate(String expression, Closure closure)
            throws UnknownReferenceException, BadExpressionException;

    public double evaluate(String expression) throws UnknownReferenceException, BadExpressionException;

    /**
     * Evaluate the saved expression using the given value as an initial value
     * into that expression. i.e. in the case of an RPN evaluator the value
     * might be pushed on the stack before the expression is evaluated and in
     * the case of a precedence evaluation the value might be considered the
     * left had side.
     * 
     * @param value
     *            the initial value in the evaluation
     * @return result of the saved expression evaluation
     */
    public double evaluate(double value, Closure closure)
            throws UnknownReferenceException, BadExpressionException;

    public double evaluate(double value) throws UnknownReferenceException, BadExpressionException;

    public double evaluate(Closure closure) throws UnknownReferenceException, BadExpressionException;

    public double evaluate() throws UnknownReferenceException, BadExpressionException;
}
