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

package org.zenoss.app.metricservice;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zenoss.app.metricservice.calculators.*;
import org.zenoss.app.metricservice.calculators.rpn.Calculator;

import java.util.Date;
import java.util.List;

/**
 * @author david
 * 
 */
public class RpnTest {

    @Before
    public void before() {
        System.setProperty(MetricCalculatorFactory.CALCULATOR_PATH_PROPERTY,
                MetricCalculatorFactory.DEFAULT_CALCULATOR_PATH);
    }

    @Test
    public void basicAddition() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5,1,10,+,+");
        Assert.assertEquals(16.0, result, 0.0);
    }

    @Test
    public void basicSubtraction() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5,1,10,-,-");
        Assert.assertEquals(14.0, result, 0.0);
    }

    @Test
    public void basicMultiplication() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5,1,10,*,*");
        Assert.assertEquals(50.0, result, 0.0);
    }

    @Test
    public void basicDivision() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5,1,10,/,/");
        Assert.assertEquals(50.0, result, 0.0);
    }

    @Test
    public void min() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5, 10, min");
        Assert.assertEquals(5.0, result, 0.0);
    }

    @Test
    public void max() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5, 10, max");
        Assert.assertEquals(10.0, result, 0.0);
    }

    @Test
    public void dup() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("5, dup, +");
        Assert.assertEquals(10.0, result, 0.0);
    }

    @Test
    public void example1() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("128,8,*");
        Assert.assertEquals(1024.0, result, 0.0);
    }

    @Test
    public void example2() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("1024,7000,gt");
        Assert.assertEquals(0.0, result, 0.0);
    }

    @Test
    public void example3() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("0, 7000,2024,if");
        Assert.assertEquals(7000.0, result, 0.0);
    }

    @Test
    public void example4() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("128,8,*,7000,GT,7000,128,8,*,IF");
        Assert.assertEquals(7000.0, result, 0.0);
    }

    @Test
    public void mod() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        double result = calc.evaluate("1234,100,%");
        Assert.assertEquals(34.0, result, 0.0);
    }

    @Test
    public void sort() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        String in = "9, 3, 4, 5, 1, 2, 8, 6, 7, 0, 10, sort";
        double[] out = { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 };

        calc.evaluate(in);

        Calculator rpn = (Calculator) calc;
        for (double anOut : out) {
            Assert.assertEquals(anOut, rpn.pop(), 0.0);
        }
    }

    @Test
    public void rev() throws ClassNotFoundException, UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        String in = "9, 3, 4, 5, 1, 2, 8, 6, 7, 0, 10, rev";
        double[] out = { 9, 3, 4, 5, 1, 2, 8, 6, 7, 0 };

        calc.evaluate(in);

        Calculator rpn = (Calculator) calc;
        for (double anOut : out) {
            Assert.assertEquals(anOut, rpn.pop(), 0.0);
        }
    }

    @Test
    public void UnknownType() {
        try {
            new MetricCalculatorFactory().newInstance("foo");
            Assert.fail("Found class where none should exist");
        } catch (ClassNotFoundException e) {
            // ignore, expected
        }
    }

    @Test
    public void CustomPathFail() {
        System.setProperty(MetricCalculatorFactory.CALCULATOR_PATH_PROPERTY,
                "foo.bar.does.not.exist");
        try {
            new MetricCalculatorFactory().newInstance("rpn");
            Assert.fail("Found class where none should exist");
        } catch (ClassNotFoundException e) {
            // ignore, expected
        }
    }

    @Test
    public void CustomPathSucceed() throws ClassNotFoundException {
        System.setProperty(MetricCalculatorFactory.CALCULATOR_PATH_PROPERTY,
                "foo.bar.does.not.exist:"
                        + MetricCalculatorFactory.DEFAULT_CALCULATOR_PATH);
        new MetricCalculatorFactory().newInstance("rpn");
    }

    @Test
    public void NanAndInfinityTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals("Positive infinity", 1.0,
                calc.evaluate("inf, isinf"), 0.0);
        Assert.assertEquals("Unknown", 1.0, calc.evaluate("unkn, un"), 0.0);
        Assert.assertEquals("Negative infinity", 1.0,
                calc.evaluate("neginf, isinf"), 0.0);
        Assert.assertEquals("Not Infinity", 0.0, calc.evaluate("234.2, isinf"),
                0.0);
        Assert.assertEquals("Not Unknown", 0.0, calc.evaluate("234.2, un"), 0.0);
        Assert.assertEquals("Infinity, Not Unknown", 0.0,
                calc.evaluate("inf, un"), 0.0);
        Assert.assertEquals("Unknown, Not Infinity", 0.0,
                calc.evaluate("unkn, isinf"), 0.0);
    }

    @Test
    public void TimeTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals("Now", new Date().getTime() / 1000,
                calc.evaluate("now"), 5.0);
    }

    @Test
    public void LimitTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals("In Limit", 5.0, calc.evaluate("5, 0, 10, limit"),
                0.0);
        Assert.assertEquals("Below Limit", 1.0,
                calc.evaluate("-5, 0, 10, limit, un"), 0.0);
        Assert.assertEquals("Above Limit", 1.0,
                calc.evaluate("15, 0, 10, limit, un"), 0.0);
    }

    @Test
    public void AddNanTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals("Left side unknown", 5.0,
                calc.evaluate("unkn, 5, addnan"), 0.0);
        Assert.assertEquals("Right side unknown", 5.0,
                calc.evaluate("5, unkn, addnan"), 0.0);
        Assert.assertEquals("Both sides unknown", 1.0,
                calc.evaluate("unkn, unkn, addnan, un"), 0.0);

    }

    @Test
    public void exchangeTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals(1.0, calc.evaluate("1,2,exc"), 0.0);
        List<Double> stack = ((Calculator) calc).getStack();
        Assert.assertEquals("stack length", 2, stack.size());
        Assert.assertEquals("top", 1.0, stack.get(1), 0.0);
        Assert.assertEquals("under top", 2.0, stack.get(0), 0.0);
    }

    @Test
    public void regression1() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        Assert.assertEquals(
                0.0,
                calc.evaluate(100.0,
                        "1024,*,DUP,2146787328,LT,EXC,2146787328,/,1.0,-,-100,*,0.0,IF"),
                0.0);
        // 2146738 1024 * DUP 2146787328 LT EXC 2146787328 / 1.0 - -100 * 0.0 IF
        Assert.assertEquals(
                -2.39,
                calc.evaluate(2146738.0,
                        "1024,*,DUP,2146787328,LT,EXC,2146787328,/,1.0,-,-100,*,0.0,IF"),
                0.5);
    }

    @Test
    public void referenceTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        calc.setReferenceProvider(new ReferenceProvider() {

            @Override
            public double lookup(String name, Closure closure)
                    throws UnknownReferenceException {
                if (name.equals("ref1")) {
                    return 2.0;
                } else if (name.equals("ref2")) {
                    return 3.0;
                }
                throw new UnknownReferenceException(name);
            }
        });

        Assert.assertEquals(6.0, calc.evaluate("ref1,ref2,*"), 0.0);
    }

    @Test
    public void unknownReferenceTest() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        calc.setReferenceProvider(new ReferenceProvider() {

            @Override
            public double lookup(String name, Closure closure)
                    throws UnknownReferenceException {
                if (name.equals("ref1")) {
                    return 2.0;
                }
                throw new UnknownReferenceException(name);
            }
        });

        try {
            calc.evaluate("ref1,ref2,*");
        } catch (UnknownReferenceException ure) {
            return;
        }
        Assert.fail("should have exceptioned out with an unknown reference");
    }

    @Test
    public void referenceTestWithClosure() throws ClassNotFoundException,
            UnknownReferenceException {
        MetricCalculator calc = new MetricCalculatorFactory()
                .newInstance("rpn");
        final Closure data = new Closure() {
        };

        calc.setReferenceProvider(new ReferenceProvider() {

            @Override
            public double lookup(String name, Closure closure)
                    throws UnknownReferenceException {
                Assert.assertEquals("closure object is not what was expected",
                        data, closure);
                if (name.equals("ref1")) {
                    return 2.0;
                } else if (name.equals("ref2")) {
                    return 3.0;
                }
                throw new UnknownReferenceException(name);
            }
        });

        Assert.assertEquals(6.0, calc.evaluate("ref1,ref2,*", data), 0.0);
    }
}
