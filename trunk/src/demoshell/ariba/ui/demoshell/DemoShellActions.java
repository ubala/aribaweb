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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/DemoShellActions.java#12 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import java.util.Map;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.util.core.MapUtil;
import ariba.util.core.Assert;

public class DemoShellActions extends AWDirectAction
{
    static int sequenceNum = 0;
    static Map _componentsByKey;
    static final String LookupKey = "key";

    static {
        _componentsByKey = MapUtil.map();
    }

    static String tableActionName ()
    {
        return "table/DemoShellActions";
    }

    static public String urlRegisteringResponseComponent (AWComponent component)
    {
        String key = component.httpSession().getId() + "-" + sequenceNum++;
        _componentsByKey.put(key, component);

        String url = AWDirectActionUrl.fullUrlForDirectAction(tableActionName(), component.requestContext(), LookupKey, key);

        Log.demoshell.debug("*** DirectAction registration.  %s -> %s", url, component);

        return url;
    }

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this class
        // enable on a per action method as necessary
        return false;
    }

    public AWComponent tableAction ()
    {
        String key = request().formValueForKey(LookupKey);
        AWComponent component = (AWComponent)_componentsByKey.get(key);
        _componentsByKey.remove(key);

        Log.demoshell.debug("*** DirectAction callback.  %s -> %s", key, component);

        return component;
    }

    protected final static String ActionPrefix = "showPage/DemoShellActions/";

    public AWResponseGenerating showPageAction ()
    {
        String pageName = request().formValueForKey("page");
        if (pageName == null) {
            String url = request().uri();
            String prefix = ActionPrefix;
            int index = url.indexOf(prefix);
            int keyStart = index + prefix.length();
            Assert.that(((index > 0) || keyStart < url.length()), "Incorrect form for request URL: %s", url);

            int keyEnd = url.indexOf('?', keyStart);
            if (keyEnd <=0) keyEnd = url.length();

            pageName = url.substring(keyStart, keyEnd);

            Assert.that(pageName !=null, "Request missing page key on query string or path after direct action");
        }

        if (pageName.equals("Main")) {
            return DemoShell.sharedInstance().mainPage(requestContext());
        }

        AWComponent page = pageWithName(pageName);

        Assert.that(page !=null, "Couldn't find component for page: %s", pageName);

        return page;
    }
}
