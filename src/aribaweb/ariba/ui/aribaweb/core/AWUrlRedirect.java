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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWUrlRedirect.java#11 $
*/
package ariba.ui.aribaweb.core;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWEncodedString;

public class AWUrlRedirect extends AWRedirect
{
    public final static String PageName = AWUrlRedirect.class.getName();

    private static final AWEncodedString RedirectStringStart =
        new AWEncodedString("<script id='AWRefreshComplete'>");

    private static final AWEncodedString RedirectStmt =
        new AWEncodedString("ariba.awCurrWindow.location.href = '");

    private static final AWEncodedString RedirectStringFinish =
        new AWEncodedString("'</script>");

    private boolean selfRedirect = false;

    public boolean shouldCachePage ()
    {
        return !selfRedirect;
    }

    public void setSelfRedirect (boolean flag)
    {
        selfRedirect = flag;
    }

    protected void fullPageRedirect (AWRequestContext requestContext)
    {
        AWResponse response = requestContext.response();
        response.appendContent(RedirectStringStart);
        response.appendContent(AWCurrWindowDecl.currWindowDecl(requestContext));
        response.appendContent(RedirectStmt);
        response.appendContent(escapeJavascript(getRedirectUrl(requestContext)));
        response.appendContent(RedirectStringFinish);
    }
}