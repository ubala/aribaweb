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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/DialogContentWrapper.java#5 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public class DialogContentWrapper extends AWComponent
{
    private static final String AllowDialogContent = "AllowDialogContent";

    public static void allowDialogDisplay (AWRequestContext requestContext)
    {
        requestContext.page().put(AllowDialogContent, Boolean.TRUE);
    }

    public static boolean showAsDialog (AWRequestContext requestContext)
    {
        Boolean showAsDialog =
            (Boolean)requestContext.pageComponent().env().peek("showAsDialog");
        return showAsDialog != null ? showAsDialog.booleanValue() : false;
    }

    public boolean isDialogContentAllowed ()
    {
        return pageComponent() instanceof Allowed
               || requestContext().page().get(AllowDialogContent) != null;
    }

    public boolean hasTOCContent ()
    {
        return AribaToc.hasVisibleContent(requestContext());
    }

    public boolean showAsDialog ()
    {
        return isDialogContentAllowed() && !hasTOCContent();
    }

    public static interface Allowed {
    }
}


