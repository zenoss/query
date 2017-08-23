/*
* © Zenoss, Inc. 2017, all rights reserved.
*  Use is subject to terms as shown in the License.zenoss file.
*/

package org.zenoss.app.metricservice.calculators.rpn;

import org.zenoss.app.metricservice.calculators.BadExpressionException;

public class RPNException extends BadExpressionException {
    private static final long serialVersionUID = -478064808047872067L;

    public RPNException(String expression, Exception e) {
        super(expression, e);
    }
}
