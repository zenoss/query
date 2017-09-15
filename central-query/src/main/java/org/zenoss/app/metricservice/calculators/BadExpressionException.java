/*
* © Zenoss, Inc. 2017, all rights reserved.
*  Use is subject to terms as shown in the License.zenoss file.
*/

package org.zenoss.app.metricservice.calculators;

public class BadExpressionException extends Exception {
    private static final long serialVersionUID = 3505599798296249875L;

    public BadExpressionException(String expression, Exception e) {
        super(String.format("Unable to apply expression %s", expression), e);
    }
}
