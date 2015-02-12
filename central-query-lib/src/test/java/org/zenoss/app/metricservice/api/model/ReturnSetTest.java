package org.zenoss.app.metricservice.api.model;

import org.junit.Assert;
import org.junit.Test;

public class ReturnSetTest {

    private static final String TEST_RETURN_SET_JSON = "test_return_set";

    @Test
    public void testToJson() throws Exception {
        ReturnSet subject = ReturnSet.TEST_RETURN_SET;
        String returnSetJson = subject.toJson();
        Assert.assertEquals(TEST_RETURN_SET_JSON, returnSetJson);
    }

    @Test
    public void testFromJson() throws Exception {
        ReturnSet subject = ReturnSet.fromJson(TEST_RETURN_SET_JSON);
        Assert.assertEquals(ReturnSet.TEST_RETURN_SET, subject);
    }
}