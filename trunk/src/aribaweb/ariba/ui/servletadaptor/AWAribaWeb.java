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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWAribaWeb.java#2 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.util.core.Assert;

/**
    This class allows for usage of the AribaWeb framework without an app server running.
*/
abstract public class AWAribaWeb extends Object
{
    private static AWServletApplication ApplicationSharedInstance;

    public static void initialize ()
    {
        Assert.that(ApplicationSharedInstance == null,
                    "AWAribaWeb.initialize() should only be called once");
        ApplicationSharedInstance = new AWServletApplication();
        ApplicationSharedInstance.init();
    }

    public static void registerResourceDirectory (String directoryPath, String urlPrefix)
    {
        Assert.that(ApplicationSharedInstance != null,
            "AWAribaWeb.initialize() must be called before calling " +
            "registerResourceDirectory(String directoryPath, String urlPrefix)");
        AWMultiLocaleResourceManager resourceManager =
            ApplicationSharedInstance.resourceManager();
        resourceManager.registerResourceDirectory(directoryPath, urlPrefix);
    }

    public static AWComponent pageWithName (String pageName)
    {
        AWRequestContext requestContext = ApplicationSharedInstance.createRequestContext(null);
        return ApplicationSharedInstance.createPageWithName(pageName, requestContext);
    }
}
