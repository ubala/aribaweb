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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPageWrapper.java#2 $
*/
package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.util.core.StringUtil;

public class AWPageWrapper extends AWComponent
{
    public String favIconUrl ()
    {
        return AWImage.imageUrl(requestContext(), this, "favicon.ico");
    }
    
    public String debugTitle ()
    {
        String debugTitle = null;
        if (application().isDebuggingEnabled()) {
            if (AWConcreteServerApplication.IsAutomationPageTitleTestModeEnabled) {
                debugTitle = pageComponent().name();
            }
            else {
                debugTitle = stringValueForBinding(ariba.ui.aribaweb.html.BindingNames.debugTitle);
                if (StringUtil.nullOrEmptyOrBlankString(debugTitle)) {
                    debugTitle = pageComponent().name();
                }
            }
        }
        return debugTitle;
    }
}
