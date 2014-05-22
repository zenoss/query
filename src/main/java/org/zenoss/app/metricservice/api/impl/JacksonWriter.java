package org.zenoss.app.metricservice.api.impl;

import java.io.BufferedWriter;
import java.io.Writer;

/**
 * Created with IntelliJ IDEA.
 * User: morr
 * Date: 5/19/14
 * Time: 5:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class JacksonWriter extends BufferedWriter  {
    public JacksonWriter(Writer out) {
        super(out);
    }
}
