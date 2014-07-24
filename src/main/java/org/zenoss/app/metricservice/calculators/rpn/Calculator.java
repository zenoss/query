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

package org.zenoss.app.metricservice.calculators.rpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zenoss.app.metricservice.calculators.BaseMetricCalculator;
import org.zenoss.app.metricservice.calculators.Closure;
import org.zenoss.app.metricservice.calculators.UnknownReferenceException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A RPN expression metric calculator.
 */
public class Calculator extends BaseMetricCalculator {

    private static final Logger log = LoggerFactory.getLogger(Calculator.class);
    /**
     * Maintains the stack used for RPN evaluation
     */
    private List<Double> stack = new ArrayList<>();

    /**
     * push the given value on to the top of the evaluation stack
     * 
     * @param value
     *            value to push onto the stack
     */
    public void push(Double value) {
        stack.add(value);
    }

    /**
     * Removes and returns the top entry from the evaluation stack
     * 
     * @return the top entry of the evaluation stack
     */
    public Double pop() {
        return stack.remove(stack.size() - 1);
    }

    /**
     * Returns, but does not remove, the top entry from the evaluation stack
     * 
     * @return the top entry of the evaluation stack
     */
    public Double peek() {
        return stack.get(stack.size() - 1);
    }

    /**
     * Pops two values from the stack and compares them with the 'less than'
     * operator. If the comparison is true 1 is pushed back onto the stack, else
     * 0. The first value popped from the stack is considered the right hand
     * side and the second is considered the left hand side.
     */
    public void lt() {
        Double r = pop(), l = pop();
        push((double) (l < r ? 1 : 0));
    }

    /**
     * Pops two values from the stack and compares them with the 'less than or
     * equal to' operator. If the comparison is true 1 is pushed back onto the
     * stack, else 0. The first value popped from the stack is considered the
     * right hand side and the second is considered the left hand side.
     */
    public void le() {
        Double r = pop(), l = pop();
        push((double) (l <= r ? 1 : 0));
    }

    /**
     * Pops two values from the stack and compares them with the 'greater than'
     * operator. If the comparison is true 1 is pushed back onto the stack, else
     * 0. The first value popped from the stack is considered the right hand
     * side and the second is considered the left hand side.
     */
    public void gt() {
        Double r = pop(), l = pop();
        push((double) (l > r ? 1 : 0));
    }

    /**
     * Pops two values from the stack and compares them with the 'greater than
     * or equal to' operator. If the comparison is true 1 is pushed back onto
     * the stack, else 0. The first value popped from the stack is considered
     * the right hand side and the second is considered the left hand side.
     */
    public void ge() {
        Double r = pop(), l = pop();
        push((double) (l >= r ? 1 : 0));
    }

    /**
     * Pops two values from the stack and compares them with the 'equals'
     * operator. If the comparison is true 1 is pushed back onto the stack, else
     * 0. The first value popped from the stack is considered the right hand
     * side and the second is considered the left hand side.
     */
    public void eq() {
        push((double) (pop().equals(pop()) ? 1 : 0));
    }

    /**
     * Pops two values from the stack and compares them with the 'not equals'
     * operator. If the comparison is true 1 is pushed back onto the stack, else
     * 0. The first value popped from the stack is considered the right hand
     * side and the second is considered the left hand side.
     */
    public void ne() {
        push((double) (!pop().equals(pop()) ? 1 : 0));
    }

    /**
     * Pops a single value from the stack and if that value is Unknown/NaN then
     * pushes 1 onto the stack, else 0.
     */
    public void isUnknown() {
        push((double) (Double.isNaN(pop()) ? 1 : 0));
    }

    /**
     * Pops a single value from the stack and if that value is positive or
     * negative infinity than pushes 1 onto the stack, else 0.
     */
    public void isInfinity() {
        push((double) (Double.isInfinite(pop()) ? 1 : 0));
    }

    /**
     * Pops three values from the stack and if the third value popped is greater
     * than 0, then the first value popped is pushed back on the stack, else the
     * second item popped is pushed back on the stack.
     */
    public void ifte() {
        Double a = pop(), b = pop(), c = pop();
        push(c > 0.0 ? a : b);
    }

    /**
     * Pops two values from the stack and then pushes the minimum of those two
     * values back on the stack.
     */
    public void min() {
        push(Math.min(pop(), pop()));
    }

    /**
     * Pops two values from the stack and then pushes the maximum of those two
     * values back on the stack.
     */
    public void max() {
        push(Math.max(pop(), pop()));
    }

    /**
     * Pops two values from the stack and uses those to define a range. A third
     * value is popped from the stack and if the value is outside the defined
     * range Unknown/Nan is pushed on to the stack. If the value is within the
     * range it is pushed back on the stack.
     */
    public void limit() {
        Double b1 = pop(), b2 = pop(), val = pop();
        Double lower = Math.min(b1, b2);
        Double upper = Math.max(b1, b2);

        if (val < lower || val > upper) {
            push(Double.NaN);
        } else {
            push(val);
        }
    }

    /**
     * Pops two values from the stack, adds them together and pushes the result
     * back on the stack.
     */
    public void add() {
        push(pop() + pop());
    }

    /**
     * Pops two values from the stack, adds them together and pushes the result
     * on the stack. If both of the values are Unknown/NaN then Unknown/NaN is
     * the result. If only a single value is Unknown/NaN it is treated as a
     * value of 0 in the addition.
     */
    public void addnan() {
        Double r = pop(), l = pop();
        if (Double.isNaN(r) && Double.isNaN(l)) {
            push(Double.NaN);
        } else if (Double.isNaN(r)) {
            push(l + 0.0);
        } else if (Double.isNaN(l)) {
            push(0.0 + r);
        } else {
            push(l + r);
        }
    }

    /**
     * Pops two values from the stack, subtracts the first popped value from the
     * second and pushes the result back on the stack.
     */
    public void subtract() {
        Double r = pop(), l = pop();
        push(l - r);
    }

    /**
     * Pops two values from the stack and pushes their product back on the
     * stack.
     */
    public void multiply() {
        push(pop() * pop());
    }

    /**
     * Pops two values from the stack, divides the second by the first and
     * pushes the result onto the stack.
     */
    public void divide() {
        Double r = pop(), l = pop();
        push(l / r);
    }

    /**
     * Pops two values from the stack, calculate the modulo of the second by the
     * first and pushes the result onto the stack.
     */
    public void modulo() {
        Double r = pop(), l = pop();
        push(l % r);
    }

    /**
     * Pops a single value from the stack, calculate the sine value of that
     * value, and pushes the result on the stack. The calculation is done in
     * radians.
     */
    public void sin() {
        push(Math.sin(pop()));
    }

    /**
     * Pops a single value from the stack, calculate the cosine value of that
     * value, and pushes the result on the stack. The calculation is done in
     * radians.
     */
    public void cos() {
        push(Math.cos(pop()));
    }

    /**
     * Pops a single value from the stack, calculate the tangent value of that
     * value, and pushes the result on the stack. The calculation is done in
     * radians.
     */
    public void tan() {
        push(Math.tan(pop()));
    }

    /**
     * Pops a single value from the stack, calculate the arctangent value of
     * that value, and pushes the result on the stack. The calculation is done
     * in radians.
     */
    public void atan() {
        push(Math.atan(pop()));
    }

    /**
     * Pops two values from the stack, uses them to calculate sine, cosine
     * arctangent, and pushes the result on the stack. The first value popped is
     * the x or cosine value, the second value is the y or sine value. The
     * calculation is done in radians.
     */
    public void atan2() {
        Double x = pop(), y = pop();
        push(Math.atan2(y, x));
    }

    /**
     * Pops a single value from the stack, converts it to radians, and pushes
     * the result on to the stack.
     */
    public void deg2rad() {
        push(pop() * Math.PI / 180.0);
    }

    /**
     * Pops a single value from the stack, converts it to degress, and pushes
     * the result on to the stack.
     */
    public void rad2deg() {
        push(pop() * 180.0 / Math.PI);
    }

    /**
     * Pops a single value from the stack, calculate the log value of that
     * value, and pushes the result on the stack.
     */
    public void log() {
        push(Math.log(pop()));
    }

    /**
     * Pops a single value from the stack, calculate the exponential log value
     * of that value, and pushes the result on the stack.
     */
    public void exp() {
        push(Math.exp(pop()));
    }

    /**
     * Pops a single value from the stack, calculate the square root of that
     * value, and pushes the result on the stack.
     */
    public void sqrt() {
        push(Math.sqrt(pop()));
    }

    /**
     * Pops a single value from the stack, rounds down the value to the nearest
     * integer value, and pushes that value onto the stack.
     */
    public void floor() {
        push(Math.floor(pop()));
    }

    /**
     * Pops a single value from the stack, rounds up the value to the nearest
     * integer value, and pushes that value onto the stack.
     */
    public void ceil() {
        push(Math.ceil(pop()));
    }

    /**
     * Pops a single value from the stack, calculates its absolute value, and
     * pushes the result on the stack.
     */
    public void abs() {
        push(Math.abs(pop()));
    }

    /**
     * Pops a single value from the stack that is used as a count and then pops
     * that many more values from the the stack, sorts them and then pushes them
     * back on the stack in ascending order.
     */
    public void sort() {
        int count = (int) Math.floor(pop());
        double[] list = new double[count];
        for (int i = 0; i < count; ++i) {
            list[i] = pop();
        }
        Arrays.sort(list);
        for (int i = 0; i < count; ++i) {
            push(list[i]);
        }
    }

    /**
     * Pops a single value from the stack that is used as a count and then pops
     * that many more values from the the stack and then pushes them back on the
     * stack in reverse order.
     */
    public void rev() {
        int count = (int) Math.floor(pop());
        double[] list = new double[count];
        for (int i = 0; i < count; ++i) {
            list[i] = pop();
        }
        for (int i = 0; i < count; ++i) {
            push(list[i]);
        }
    }

    /**
     * Pops a single value from the stack that is used as a count and then pops
     * that many more values from the the stack, calculates the average of the
     * values, and pushes the result onto the stack.
     */
    public void avg() {
        int count = (int) Math.floor(pop());
        double sum = 0.0;

        for (int i = 0; i < count; ++i) {
            sum += pop();
        }
        push(sum / (double) count);
    }

    /**
     * Pushes the Unknown/NaN value onto the stack.
     */
    public void unknown() {
        push(Double.NaN);
    }

    /**
     * Pushes the positive infinity value onto the stack.
     */
    public void infinity() {
        push(Double.POSITIVE_INFINITY);
    }

    /**
     * Pushes the negative infinity value onto the stack.
     */
    public void negInfinity() {
        push(Double.NEGATIVE_INFINITY);
    }

    /**
     * Duplicates the top value of the stack back on to the stack.
     */
    public void duplicate() {
        push(peek());
    }

    /**
     * Pushes the current time as expressed as seconds since the unix epoch onto
     * the stack.
     */
    public void now() {
        push(Math.floor(new Date().getTime() / 1000l));
    }

    /**
     * Swaps the first two values on the stack.
     */
    public void exchange() {
        Double a = pop(), b = pop();
        push(a);
        push(b);
    }

    /**
     * Returns a copy of the current stack.
     * 
     * @return a copy of the current stack.
     */
    public List<Double> getStack() {
        List<Double> copy = new ArrayList<>();
        copy.addAll(stack);
        return copy;
    }

    /**
     * Clears the current stack.
     */
    public void clear() {
        stack.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(java
     * .lang.String, org.zenoss.app.metricservice.calculators.Closure)
     */
    @Override
    public double evaluate(String expression, Closure closure)
            throws UnknownReferenceException {
        clear();
        return doEvaluate(expression, closure);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(double
     * , org.zenoss.app.metricservice.calculators.Closure)
     */
    @Override
    public double evaluate(double value, Closure closure)
            throws UnknownReferenceException {
        clear();
        push(value);
        return doEvaluate(getExpression(), closure);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(org
     * .zenoss.app.metricservice.calculators.Closure)
     */
    @Override
    public double evaluate(Closure closure) throws UnknownReferenceException {
        clear();
        return doEvaluate(getExpression(), closure);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(double
     * , java.lang.String, org.zenoss.app.metricservice.calculators.Closure)
     */
    @Override
    public double evaluate(double value, String expression, Closure closure)
            throws UnknownReferenceException {
        clear();
        push(value);
        return doEvaluate(expression, closure);
    }

    private void pushReference(String reference, Closure closure)
            throws UnknownReferenceException {
        if (getReferenceProvider() == null) {
            log.error("Unable to get reference provider. Throwing exception.");
            throw new UnknownReferenceException(reference);
        }
        double referenceValue = getReferenceProvider().lookup(reference, closure);
        push (referenceValue);
    }

    /**
     * Evaluates the given expression based on the current state of the stack.
     * The expression is assumed to be a comma separated list of terms in a
     * format similar to that leveraged by RRDTool's RPN evaluation.
     * 
     * @param expression
     *            expression to evaluate
     * @return the value on the top of the stack at the end of the evaluation,
     *         the value is not removed from the stack.
     */
    private double doEvaluate(String expression, Closure closure)
            throws UnknownReferenceException {
        String[] terms = expression.split(",");
        String term;
        String ref;
        for (String term1 : terms) {
            term = (ref = term1.trim()).toLowerCase();
            if (term.length() == 0) {
                continue;
            }
            switch (term.charAt(0)) {
                case '+':
                    add();
                    break;
                case '-':
                    if (term.length() == 1) {
                        subtract();
                    } else {
                        push(Double.valueOf(term));
                    }
                    break;
                case '/':
                    divide();
                    break;
                case '*':
                    multiply();
                    break;
                case '%':
                    modulo();
                    break;
                case 'a':
                    if ("avg".equals(term)) {
                        avg();
                    } else if ("abs".equals(term)) {
                        abs();
                    } else if ("atan".equals(term)) {
                        atan();
                    } else if ("atan2".equals(term)) {
                        atan2();
                    } else if ("addnan".equals(term)) {
                        addnan();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'c':
                    if ("cos".equals(term)) {
                        cos();
                    } else if ("ceil".equals(term)) {
                        ceil();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'd':
                    if ("dup".equals(term)) {
                        duplicate();
                    } else if ("deg2rad".equals(term)) {
                        deg2rad();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'e':
                    if ("exc".equals(term)) {
                        exchange();
                    } else if ("exp".equals(term)) {
                        exp();
                    } else if ("eq".equals(term)) {
                        eq();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'f':
                    if ("floor".equals(term)) {
                        floor();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'g':
                    if ("gt".equals(term)) {
                        gt();
                    } else if ("ge".equals(term)) {
                        ge();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'i':
                    if ("if".equals(term)) {
                        ifte();
                    } else if ("isinf".equals(term)) {
                        isInfinity();
                    } else if ("isunkn".equals(term)) {
                        isInfinity();
                    } else if ("inf".equals(term)) {
                        infinity();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'l':
                    if ("limit".equals(term)) {
                        limit();
                    } else if ("log".equals(term)) {
                        log();
                    } else if ("lt".equals(term)) {
                        lt();
                    } else if ("le".equals(term)) {
                        le();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'm':
                    if ("min".equals(term)) {
                        min();
                    } else if ("max".equals(term)) {
                        max();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'n':
                    if ("neginf".equals(term)) {
                        negInfinity();
                    } else if ("now".equals(term)) {
                        now();
                    } else if ("ne".equals(term)) {
                        ne();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'r':
                    if ("rev".equals(term)) {
                        rev();
                    } else if ("rad2deg".equals(term)) {
                        rad2deg();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 's':
                    if ("sqrt".equals(term)) {
                        sqrt();
                    } else if ("sort".equals(term)) {
                        sort();
                    } else if ("sin".equals(term)) {
                        sin();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 't':
                    if ("tan".equals(term)) {
                        tan();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                case 'u':
                    if ("unkn".equals(term)) {
                        unknown();
                    } else if ("un".equals(term)) {
                        isUnknown();
                    } else {
                        pushReference(ref, closure);
                    }
                    break;
                default:
                    if (Character.isDigit(term.charAt(0))) {
                        push(Double.valueOf(term));
                    }
                    if (Character.isLetter(term.charAt(0))) {
                        pushReference(ref, closure);
                    }
                    break;
            }
        }
        return peek();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(double
     * , java.lang.String)
     */
    @Override
    public double evaluate(double value, String expression)
            throws UnknownReferenceException {
        return evaluate(value, expression, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(java
     * .lang.String)
     */
    @Override
    public double evaluate(String expression) throws UnknownReferenceException {
        return evaluate(expression, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate(double
     * )
     */
    @Override
    public double evaluate(double value) throws UnknownReferenceException {
        return evaluate(value, (Closure) null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.calculators.MetricCalculator#evaluate()
     */
    @Override
    public double evaluate() throws UnknownReferenceException {
        return evaluate((Closure) null);
    }
}
