package org.zenoss.app.metricservice.api.model.v2;

import java.util.ArrayList;

/**
 * Created by maya on 5/22/17.
 */
public class SuggestResult {

    public ArrayList<String> suggestions = new ArrayList<>();
    public String reasonPhrase;
    public int statusCode;
}
