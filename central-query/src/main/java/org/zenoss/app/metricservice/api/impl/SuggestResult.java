package org.zenoss.app.metricservice.api.impl;

import java.util.ArrayList;

/**
 * Created by maya on 5/22/17.
 */
public class SuggestResult {

    public ArrayList<String> suggestions = new ArrayList<>();
    public String reasonPhrase;
    public int statusCode;
}
