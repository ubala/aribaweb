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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXDebugResourceActions.java#17 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.http.multitab.MultiTabSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpSession;

public class AWXDebugResourceActions extends AWDirectAction
{
    public final static String Name = "AWXDebugResourceActions";
    public final static String ContentActionName = "content";
    protected final static String ActionPrefix = ContentActionName + "/" + Name + "/";

    // All three of these (two maps and the int) should be protected by syncing on IdsForResourceRoots
    private static int NextId = 0;
    private final static Map<String,String> ResourceRootsForIds = new GrowOnlyHashtable<String,String>();
    private final static Map<String,String> IdsForResourceRoots = new GrowOnlyHashtable<String,String>();

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this class
        // enable on a per action method as necessary
        return false;
    }

    private static AWResourceManager resourceManager (AWRequestContext requestContext)
    {
        // TODO this block is basically duplicating code in AWPage.resourceManager()

        AWResourceManager rm;
        HttpSession httpSession = requestContext.existingHttpSession();
        if (httpSession != null) {
            rm = AWSession.session(httpSession).resourceManager();
        }
        else {
            rm = AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
        }
        return rm;
    }

    public AWResponseGenerating contentAction ()
    {
        Assert.that(AWConcreteApplication.IsRapidTurnaroundEnabled || AWConcreteApplication.IsDirectConnectEnabled, "Action available only if debugging is enabled -- not for production use.");

        String[] path = request().requestHandlerPath();
        Assert.that(path.length > 3, "Incorrect form for request URL");
        Assert.that(path[0].equals(ContentActionName), "Incorrect form for request URL");
        Assert.that(path[1].equals(Name), "Incorrect form for request URL");
        String id = path[2];

        // Since it's a GrowOnlyHashtable, safe to access here without synchronization
        String dir = ResourceRootsForIds.get(id);
        Assert.that(dir != null, "Not a registered resource directory: %s", id);

        String resourceName = StringUtil.join(path, "/", 3, path.length - 3);
        // Make sure nobody tries to sneak out of the registered path,
        // e.g. AWXDebugResourceActions/99/../../../StevesBankAccount.qif.
        // In WebObjects world, requestHandlerPath() will already have
        // normalized away the "..", but not in servlet world, so we need
        // to do this check.
        Assert.that(resourceName.indexOf("..") == -1, "'..' not allowed in resource path");
        String filename = Fmt.S("%s/%s", dir, resourceName);
        // System.out.println("*** Callback resourceName = " + resourceName + "   (url: " + url + "):   path: " + resource.fullUrl());

        File f = new File(filename);
        AWResponse response = application().createResponse();

        try {
            ((AWBaseResponse)response).setContentFromStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            Assert.fail(e, "Requested file %s not found", resourceName);
        }

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

    public static String urlForResourceInDirectory (AWRequestContext requestContext, String dir, String resourcePath)
    {
        return urlForResourceInDirectory(requestContext, dir, resourcePath, false);
    }

    public static String urlForResourceInDirectory (AWRequestContext requestContext, String dir, String resourcePath,
                                                    boolean useFullURL)
    {
        String id;
        synchronized (IdsForResourceRoots) {
            id = IdsForResourceRoots.get(dir);
            if (id == null) {
                // Canonicalize the directory to forward-slashes, make sure it doesn't end with a slash
                String canonicalizedDir = dir.replace('\\', '/');
                if (canonicalizedDir.endsWith("/")) {
                    canonicalizedDir = canonicalizedDir.substring(0, canonicalizedDir.length() - 1);
                }

                // get next ID
                id = Integer.toString(NextId);
                NextId++;
                IdsForResourceRoots.put(dir, id);
                ResourceRootsForIds.put(id, canonicalizedDir);
                Log.aribawebResource_register.debug("AWXDebugResourceActions: ID %s -> %s", id, dir);
            }
        }

        AWDirectActionUrl url;
        if (useFullURL) {
             url = AWDirectActionUrl.checkoutFullUrl(requestContext);
        }
        else {
             url = AWDirectActionUrl.checkoutUrl();
        }

        if (requestContext != null) {
            url.setRequestContext(requestContext);
        }

        // Include the directory id as part of the URL path, not a query parameter, so
        // that relative URL references will work
        url.setDirectActionName(ActionPrefix + id + "/" + resourcePath);

        String urlString = url.finishUrl();

        // strip query params (AW shouldn't need the awr=...
        int index = urlString.indexOf('?');
        if (index > 0) {
            urlString = urlString.substring(0, index);
        }

        if (requestContext == null) {
            return urlString;
        }
        else {
            MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
            return multiTabSupport.insertTabInUri(urlString, requestContext.getTabIndex(),
                    true);
        }
    }

    public static String  urlForResourceNamed (AWRequestContext requestContext, String name)
    {
        return urlForResourceNamed(requestContext, name, false);
    }

    public static String  urlForResourceNamed (AWRequestContext requestContext, String name, boolean useFullURL)
    {
        Assert.that(!StringUtil.nullOrEmptyOrBlankString(name), "Unable to provide url for null/empty/blank resource");
        AWResourceManager resourceManager = resourceManager(requestContext);
        AWResource res = resourceManager.resourceNamed(name);

        if (!(res instanceof AWFileResource)) {
            boolean useFullUrl = requestContext.isMetaTemplateMode();
            boolean isSecure = useFullUrl ? requestContext.request() != null && requestContext.request().isSecureScheme() : false;
            return resourceManager.urlForResourceNamed(name, useFullUrl, isSecure, false);
        }

        String path = ((AWFileResource)res)._fullPath();
        Assert.that(path.endsWith(name), "Resource %s resolved to file path %s, which does not end with the requested resource!", name, path);

        // We allow relative references within this resource dir, since it was a registered resource dir
        String resourceDir = path.substring(0, path.length() - name.length());
        return urlForResourceInDirectory(requestContext, resourceDir, name, useFullURL);
    }
}
