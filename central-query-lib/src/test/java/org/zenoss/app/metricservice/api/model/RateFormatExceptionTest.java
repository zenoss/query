package org.zenoss.app.metricservice.api.model;

import org.junit.Assert;
import org.junit.Test;

public class RateFormatExceptionTest {
    private static final String TEST_MESSAGE = "Test message";
    private static final Exception TEST_EXCEPTION = new Exception("Test");
    private RateFormatException subject;


    @Test
    public void testDefaultConstructor() {
        subject = new RateFormatException();
        Assert.assertNotNull(subject);
    }


    @Test
    public void testMessageConstructor() {
        subject = new RateFormatException(TEST_MESSAGE);
        Assert.assertNotNull(subject);
        Assert.assertEquals(TEST_MESSAGE, subject.getMessage());
    }

    @Test
    public void testExceptionConstructor() {
        subject = new RateFormatException(TEST_EXCEPTION);
        Assert.assertNotNull(subject);
        Assert.assertEquals(TEST_EXCEPTION, subject.getCause());
    }

    @Test
    public void testMessageAndExceptionConstructor() {
        subject = new RateFormatException(TEST_MESSAGE,TEST_EXCEPTION);
        Assert.assertNotNull(subject);
        Assert.assertEquals(TEST_MESSAGE, subject.getMessage());
        Assert.assertEquals(TEST_EXCEPTION, subject.getCause());
    }

    @Test
    public void testMessageAndExceptionfConstructor() {
        subject = new RateFormatException(TEST_MESSAGE, TEST_EXCEPTION, true, false);
        Assert.assertNotNull(subject);
        Assert.assertEquals(TEST_MESSAGE, subject.getMessage());
        Assert.assertEquals(TEST_EXCEPTION, subject.getCause());
        Assert.assertEquals(0, subject.getStackTrace().length);
    }


}