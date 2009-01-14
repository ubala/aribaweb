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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRemotePageWrapper.java#8 $
*/

package ariba.ui.aribaweb.core;

import javax.servlet.http.HttpSession;

/**
    Used by slave apps to render a page wrapper that was passed in via punchin from
    a Master.  If a slave app wants to run in both slave, and standalone mode, it should
    create a simple page wrapper component which either switches in this component, or
    the local page wrapper.  Something like:

    <AWIf ifTrue="$session.isSlave">
        <AWRemotePageWrapper>
            <AWIncludeContent/>
        </AWRemotePageWrapper>
    <AWElse/>
        <MyAppsPageWrapper someBinding="blah">
            <AWIncludeContent/>
        </MyAppsPageWrapper>
    </AWIf>
*/

// subclases ANPRemotePageWrapper
public class AWRemotePageWrapper extends AWComponent
{
    public static final String DynamicTagMarker = "<AW>";
    private static final String RemoteTemplateKey = "AWRemoteTemplate";
    private static AWTemplateParser RemoteTemplateParser;

    static {
        RemoteTemplateParser = new AWRemoteTemplateParser();
        RemoteTemplateParser.init((AWApplication)AWConcreteApplication.sharedInstance());
    }

    public static void handlePunchin (HttpSession httpSession, String templateString)
    {
        //System.out.println("**** parsing: " + templateString);
        AWTemplate template = RemoteTemplateParser.templateFromString(templateString, RemoteTemplateKey);
        //System.out.println("**** resulting remote template: " + template);
        httpSession.setAttribute(RemoteTemplateKey, template);
    }

    public AWTemplate template ()
    {
        AWTemplate template = (AWTemplate)httpSession().getAttribute(RemoteTemplateKey);
        if (template == null) {
            super.template();
        }
        return template;
    }
}
