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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zenoss.app.metricservice.calculators.MetricCalculator;

/**
 * @author david
 * 
 */
public class Calculator extends MetricCalculator {
    private List<Double> stack = new ArrayList<Double>();

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#push(java.lang.Double)
     */
    public void push(Double value) {
        stack.add(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#lt()
     */
    public void lt() {
        Double r = pop(), l = pop();
        push((double) (l < r ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#le()
     */
    public void le() {
        Double r = pop(), l = pop();
        push((double) (l <= r ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#gt()
     */
    public void gt() {
        Double r = pop(), l = pop();
        push((double) (l > r ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#ge()
     */
    public void ge() {
        Double r = pop(), l = pop();
        push((double) (l >= r ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#eq()
     */
    public void eq() {
        push((double) (pop() == pop() ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#ne()
     */
    public void ne() {
        push((double) (pop() != pop() ? 1 : 0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#isUnknown()
     */
    public void isUnknown() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#isInfinity()
     */
    public void isInfinity() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#ifte()
     */
    public void ifte() {
        Double a = pop(), b = pop(), c = pop();
        push(c > 0 ? a : b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#min()
     */
    public void min() {
        push(Math.min(pop(), pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#max()
     */
    public void max() {
        push(Math.max(pop(), pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#limit()
     */
    public void limit() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#add()
     */
    public void add() {
        push(pop() + pop());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#subtract()
     */
    public void subtract() {
        Double r = pop(), l = pop();
        push(l - r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#multiply()
     */
    public void multiply() {
        push(pop() * pop());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#divide()
     */
    public void divide() {
        Double r = pop(), l = pop();
        push(l / r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#modulo()
     */
    
    public void modulo() {
        Double r = pop(), l = pop();
        push(l % r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#addnan()
     */
    
    public void addnan() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#sin()
     */
    
    public void sin() {
        push(Math.sin(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#cos()
     */
    
    public void cos() {
        push(Math.cos(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#tan()
     */
    
    public void tan() {
        push(Math.tan(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#log()
     */
    
    public void log() {
        push(Math.log(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#exp()
     */
    
    public void exp() {
        push(Math.exp(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#sqrt()
     */
    
    public void sqrt() {
        push(Math.sqrt(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#atan()
     */
    
    public void atan() {
        push(Math.atan(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#atan2()
     */
    
    public void atan2() {
        Double x = pop(), y = pop();
        push(Math.atan2(y, x));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#floor()
     */
    
    public void floor() {
        push(Math.floor(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#ceil()
     */
    
    public void ceil() {
        push(Math.ceil(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#deg2rad()
     */
    
    public void deg2rad() {
        push(pop() * Math.PI / 180.0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#rad2deg()
     */
    
    public void rad2deg() {
        push(pop() * 180.0 / Math.PI);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#abs()
     */
    
    public void abs() {
        push(Math.abs(pop()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#sort()
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

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#rev()
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

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#avg()
     */
    
    public void avg() {
        int count = (int) Math.floor(pop());
        double sum = 0.0;

        for (int i = 0; i < count; ++i) {
            sum += pop();
        }
        push(sum / (double) count);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#trend()
     */
    
    public void trend() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#trendnan()
     */
    
    public void trendnan() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#unknown()
     */
    
    public void unknown() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#infinity()
     */
    
    public void infinity() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#negInfinity()
     */
    
    public void negInfinity() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#prev()
     */
    
    public void prev() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#duplicate()
     */
    
    public void duplicate() {
        push(peek());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#pop()
     */
    
    public Double pop() {
        return stack.remove(stack.size() - 1);
    }

    public Double peek() {
        return stack.get(stack.size() - 1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.zenoss.app.metricservice.rpn.Calculator#exchange()
     */
    
    public void exchange() {
        Double a = pop(), b = pop();
        push(a);
        push(b);
    }
    
    public List<Double> getStack() {
        return stack;
    }
    
    public void clear() {
        stack.clear();
    }
    
    @Override
    public double evaluate(String expression) {
        clear();
        return doEvaluate(expression);
    }    
    
    @Override
    public double evaluate(double value) {
        clear();
        push(value);
        return doEvaluate(getExpression());
    }
    
    @Override
    public double evaluate(double value, String expression) {
        clear();
        push(value);
        return doEvaluate(expression);
    }
    
    private double doEvaluate(String expression) {
        String[] terms = expression.split(",");
        String term;
        for (int i = 0; i < terms.length; ++i) {
            term = terms[i].trim().toLowerCase();
            switch (term.charAt(0)) {
            case '+':
                add();
                break;
            case '-':
                subtract();
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
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'c':
                if ("cos".equals(term)) {
                    cos();
                } else if ("ceil".equals(term)) {
                    ceil();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'd':
                if ("dup".equals(term)) {
                    duplicate();
                } else if ("deg2rad".equals(term)) {
                    deg2rad();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'e':
                if ("exchange".equals(term)) {
                    exchange();
                } else if ("exp".equals(term)) {
                    exp();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'f':
                if ("floor".equals(term)) {
                    floor();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'g':
                if ("gt".equals(term)) {
                    gt();
                } else if ("ge".equals(term)) {
                    ge();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'i':
                if ("if".equals(term)) {
                    ifte();
                } else if ("isinf".equals(term)) {
                    isInfinity();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'l':
                if ("limit".equals(term)) {
                    limit();
                } else if ("log".equals(term)) {
                    log();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'm':
                if ("min".equals(term)) {
                    min();
                } else if ("max".equals(term)) {
                    max();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 'r':
                if ("rev".equals(term)) {
                    rev();
                } else if ("rad2deg".equals(term)) {
                    rad2deg();
                } else {
                    throw new UnsupportedOperationException(term);
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
                    throw new UnsupportedOperationException(term);
                }
                break;
            case 't':
                if ("trend".equals(term)) {
                    trend();
                } else if ("trendnan".equals(term)) {
                    trendnan();
                } else if ("tan".equals(term)) {
                    tan();
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            default:
                if (Character.isDigit(term.charAt(0))) {
                    push(Double.valueOf(term));
                } else {
                    throw new UnsupportedOperationException(term);
                }
                break;
            }
        }
        return peek();
    }
}
