/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTScrollTableWrapper.java#26 $
*/

package ariba.ui.table;

import ariba.ui.table.BindingNames;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;

public final class AWTScrollTableWrapper extends AWComponent
{
    private static final String FullWidth = "100%";

    public Object _tableId;
    public Object _tableHeaderId;
    public boolean _disableRefresh;
    public Object _topIndexId;
    public Object _leftPosId;
    public Object _topOffsetId;
    public Object _isMaximizedId;

    public void awake ()
    {
        _disableRefresh = !booleanValueForBinding(BindingNames.useRefresh);
    }

    protected void sleep ()
    {
        _isMaximizedId = null;
        _tableId = null;
        _leftPosId = null;
        _tableHeaderId = null;
        _disableRefresh = false;
        _topIndexId = null;
        _topOffsetId = null;
    }

    public static boolean scrollingAllowed (AWRequestContext requestContext)
    {
        AWRequest request = requestContext.request();
        AWSession session = requestContext.session(false);
        boolean isAccessibilityEnabled =
            session != null ? session.isAccessibilityEnabled() : false;
        // IE 6.0+ and anything else (assume Firefox)
        return request != null
                    && (!request.isBrowserMicrosoft() || !request.isBrowserIE55())
                    && !isAccessibilityEnabled
                    && !requestContext.isPrintMode();
    }

    public boolean enableScrolling ()
    {
        // No scrolling if accessibility is on (ADA) or if thery're not on IE-Windows
        return booleanValueForBinding(BindingNames.enableScrolling) && scrollingAllowed(requestContext());
    }

    public Object leftPos ()
    {
        AWBinding lp = bindingForName("leftPos");
        return lp != null ? valueForBinding(lp) : Constants.getInteger(0);
    }

    public void setLeftPos (int val)
    {
        AWBinding lp = bindingForName("leftPos");
        if (lp != null) {
            setValueForBinding(val, lp);
        }
    }

    public boolean isStateless ()
    {
        return true;
    }

    public String getDivStyle ()
    {
        String style = null;
        String width = (String)valueForBinding(AWBindingNames.width);
        if (width != null && !width.equals(FullWidth)) {
            // todo: this generates garbage
            return Fmt.S("width:%s;", width);
        }
        return style;
    }

    public boolean hasHeadingRowsTemplate ()
    {
        return hasSubTemplateNamed("headingRows");
    }

    public String tableBodySemanticKey ()
    {
        String tableSemanticKey = stringValueForBinding(BindingNames.awname);
        if (tableSemanticKey != null) {
            return StringUtil.strcat(tableSemanticKey, ":tableBody");
        }
        return null;
    }

}
