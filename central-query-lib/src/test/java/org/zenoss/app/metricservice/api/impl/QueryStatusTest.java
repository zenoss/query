package org.zenoss.app.metricservice.api.impl;

import org.junit.Assert;
import org.junit.Test;

public class QueryStatusTest {

    @Test
    public void testEquals() throws Exception {
        String testMessage = "Message for Unit Test";
        QueryStatus one = new QueryStatus(QueryStatus.QueryStatusEnum.WARNING, testMessage);
        Assert.assertEquals("A QueryStatus object should be equal to itself.", one, one);
        QueryStatus other = new QueryStatus(QueryStatus.QueryStatusEnum.WARNING, testMessage);
        Assert.assertEquals("Two QueryStatus objects created with the same parameters should be equal.", one, other);
        other.setMessage("A different message");
        Assert.assertNotEquals("QueryStatus objects with different messages should not be equal.", one, other);
        other.setMessage(testMessage);
        Assert.assertEquals("QueryStatus objects should be equal after setting message back to same value", one, other);
        other.setStatus(QueryStatus.QueryStatusEnum.ERROR);
        Assert.assertNotEquals("QueryStatusObjects with different statuses should not be equal.", one, other);
    }

    @Test
    public void testHashCode() throws Exception {
        String testMessage = "Message for Unit Test";
        QueryStatus.QueryStatusEnum testStatus = QueryStatus.QueryStatusEnum.WARNING;
        QueryStatus one = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("A QueryStatus object should have the same hashcode as itself.", one.hashCode(), one.hashCode());
        QueryStatus other = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("Two QueryStatus objects created with the same parameters should have same hashcodes.", one.hashCode(), other.hashCode());
        other.setMessage("A different message");
        Assert.assertNotEquals("QueryStatus objects with different messages should have different hashcodes.", one.hashCode(), other.hashCode());
        other.setMessage(testMessage);
        Assert.assertEquals("QueryStatus objects should have the same hashcode after setting message back to same value", one.hashCode(), other.hashCode());
        other.setStatus(QueryStatus.QueryStatusEnum.ERROR);
        Assert.assertNotEquals("QueryStatusObjects with different statuses should have different hashcodes.", one.hashCode(), other.hashCode());

    }

    @Test
    public void testGetStatus() throws Exception {
        String testMessage = "Message for Unit Test";
        QueryStatus.QueryStatusEnum testStatus = QueryStatus.QueryStatusEnum.WARNING;
        QueryStatus one = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("getStatus should return status passed to constructor.", testStatus, one.getStatus());;
    }

    @Test
    public void testSetStatus() throws Exception {
        String testMessage = "Message for Unit Test";
        QueryStatus.QueryStatusEnum testStatus = QueryStatus.QueryStatusEnum.WARNING;
        QueryStatus.QueryStatusEnum newStatus = QueryStatus.QueryStatusEnum.ERROR;
        QueryStatus one = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("getStatus should return status passed to constructor.", testStatus, one.getStatus());;
        one.setStatus(newStatus);
        Assert.assertEquals("getStatus should return status set with setStatus.", newStatus, one.getStatus());;
    }

    @Test
    public void testGetMessage() throws Exception {
        String testMessage = "Message for Unit Test";
        QueryStatus.QueryStatusEnum testStatus = QueryStatus.QueryStatusEnum.WARNING;
        QueryStatus one = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("getMessage should return message passed to constructor.", testMessage, one.getMessage());;


    }

    @Test
    public void testSetMessage() throws Exception {
        String testMessage = "Message for Unit Test";
        String newMessage = "New Unit Test Message";
        QueryStatus.QueryStatusEnum testStatus = QueryStatus.QueryStatusEnum.WARNING;
        QueryStatus one = new QueryStatus(testStatus, testMessage);
        Assert.assertEquals("getMessage should return message passed to constructor.", testMessage, one.getMessage());;
        one.setMessage(newMessage);
        Assert.assertEquals("getMessage should return status set with setStatus.", newMessage, one.getMessage());;
    }
}