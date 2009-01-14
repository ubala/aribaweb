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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaPrintMenu.java#7 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWContentType;

public final class AribaPrintMenu extends AWComponent {
    private static final String ShouldOpenMSWordKey = "AribaPrintMenu.ShouldOpenMSWord";
    public Object _menuId;
    public boolean _submitForm;

    protected void awake ()
    {
        _submitForm = requestContext().currentForm() != null ||
                      requestContext().get(PageWrapperForm.CurrentHtmlFormKey) != null;
    }

    public AWComponent openPrintWindow ()
    {
        requestContext().put(ShouldOpenMSWordKey, Boolean.FALSE);
        return null;
    }

    public AWComponent openMSWord ()
    {
        requestContext().put(ShouldOpenMSWordKey, Boolean.TRUE);
        return null;
    }

    public boolean shouldOpenPrintWindow ()
    {
        Boolean rc = (Boolean)requestContext().get(ShouldOpenMSWordKey);
        boolean val = (rc != null) && (rc.booleanValue() == false);
        return val;
    }

    public boolean shouldOpenWord ()
    {
        Boolean rc = (Boolean)requestContext().get(ShouldOpenMSWordKey);
        boolean val = (rc != null) && (rc.booleanValue() == true);
        return val;
    }

    public AWResponseGenerating renderPrintPage ()
    {
        AWRequestContext requestContext = requestContext();
        requestContext.setIsPrintMode(true);
        requestContext.setFrameName("AWPrintPage");

        // re-render the page
        return null;
    }

    public AWResponseGenerating renderWord ()
    {
        AWResponse response = requestContext().response();
        if (response == null) {
            response = application().createResponse(request());
            requestContext().setResponse(response);
        }

        response.setContentType(AWContentType.ApplicationMSWord);
        return renderPrintPage();
    }
}
