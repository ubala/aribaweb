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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBackTrack.java#6 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;

public final class AWBackTrack extends AWComponent
{
    private final static AWEncodedString HistoryBacktrack = new AWEncodedString(
        "<html><head><meta http-equiv=\"Pragma\" content=\"no-cache\"><meta http-equiv=\"Expires\" content=\"-1\">" +
        "</head><body><script language=\"JavaScript\">history.go(-1);</script></body></html>");

    public static final String PageName = "AWBackTrack";

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this page
        return false;
    }

    public boolean shouldCachePage ()
    {
        return false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWResponse response = requestContext.response();
        response.appendContent(HistoryBacktrack);
    }
}