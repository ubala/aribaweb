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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXDebugResourceActions.java#11 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWResource;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;

public class AWXDebugResourceActions extends AWDirectAction
{
    protected final static String ResourceNameKey = "rn";
    protected final static String ActionPrefix = "content/AWXDebugResourceActions/";
    public final static String Name = "AWXDebugResourceActions";

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this class
        // enable on a per action method as necessary
        return false;
    }

    public AWResponseGenerating contentAction ()
    {
        Assert.that(AWConcreteApplication.IsRapidTurnaroundEnabled, "Action available only if debugging is enabled -- not for production use.");
        String url = request().uri();
        String prefix = ActionPrefix;
        int index = url.indexOf(prefix);
        int keyStart = index + prefix.length();
        Assert.that(((index > 0) || keyStart < url.length()), "Incorrect form for request URL: %s", url);

        int keyEnd = url.indexOf('?', keyStart);
        if (keyEnd <=0) keyEnd = url.length();

        String resourceName = url.substring(keyStart, keyEnd);

        Assert.that(resourceName!=null, "No resource name on request URL");

        AWResource resource = application().resourceManager().resourceNamed(resourceName);
        Assert.that(resource!=null, "Can't find requested resource named: %s", resourceName);

        // System.out.println("*** Callback resourceName = " + resourceName + "   (url: " + url + "):   path: " + resource.fullUrl());

        AWResponse response = application().createResponse();
        ((AWBaseResponse)response).setContentFromStream(resource.inputStream());

        int dot = resourceName.lastIndexOf('.');
        if (dot > 0) {
            String ext = resourceName.substring(dot + 1);
            AWContentType type = AWContentType.contentTypeForFileExtension(ext);
            if (type != null) {
                response.setContentType(type);
            }            
        }

        response.setBrowserCachingEnabled(true);
        if (resourceName.endsWith(".gif")) {
            response.setHeaderForKey("max-age=3600", "Cache-control");  // 60 minutes of caching for images
        } else {
            response.setHeaderForKey("max-age=20", "Cache-control");  // expire after 20 seconds for .js / .css
        }

        return response;
    }

    public static String  urlForResourceNamed (AWRequestContext requestContext, String name)
    {
        Assert.that((StringUtil.nullOrEmptyString(name) || (AWConcreteApplication.sharedInstance().resourceManager().resourceNamed(name) != null)), "Unable to locate resource: %s", name);

        AWDirectActionUrl url = AWDirectActionUrl.checkoutUrl();

        if (requestContext != null) {
            url.setRequestContext(requestContext);
        }

        // hack: attach resource name additional component of the file path.  We do this so that when relative URLs are constructed by the
        // browser they are processed correctly
        url.setDirectActionName(ActionPrefix + name);

        String urlString = url.finishUrl();

        // strip query params (AW shouldn't need the awr=...
        int index = urlString.indexOf('?');
        if (index > 0) {
            urlString = urlString.substring(0, index);
        }
        return urlString;
    }
}
