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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWCurrWindowDecl.java#6 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;

public final class AWCurrWindowDecl extends AWComponent
{
    private static final String TopWindowDecl = "if (!window.ariba) ariba= parent.ariba || {}; ariba.awCurrWindow = parent;";
    private static final String CurrWindowDecl = "var awCW = (window.name == 'AWRefreshFrame') ? parent : window; ariba = awCW.ariba || {}; ariba.awCurrWindow = awCW;";

    public String getCurrWindowDecl ()
    {
        return currWindowDecl(requestContext());
    }

    public static String currWindowDecl (AWRequestContext requestContext)
    {
        AWEncodedString frameName = requestContext.frameName();
        if (frameName == null) {
            frameName = requestContext.request().frameName();
        }
        if (frameName != null) {
            return Fmt.S(CurrWindowDecl, frameName.toString(), frameName.toString());
        }
        return TopWindowDecl;
    }
}