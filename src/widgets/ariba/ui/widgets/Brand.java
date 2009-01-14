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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Brand.java#13 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;
import java.io.File;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class Brand
{
    private static final String BrandParameter = "brand";
    private static final Brand NotFound = new Brand();
    private static final GrowOnlyHashtable Brands = new GrowOnlyHashtable();
    
    private String _name;
    private AWMultiLocaleResourceManager _resourceManager;

    public static Brand brandForSession (HttpSession httpSession)
    {
        return WidgetsSessionState.get(httpSession).brand();
    }

    public static void setBrandForSession (Brand brand, HttpSession httpSession)
    {
        WidgetsSessionState.get(httpSession).setBrand(brand);
    }

    public static Brand sharedBrand (HttpServletRequest request)
    {
        String name = request.getParameter(BrandParameter);
        return name == null ? null : sharedBrand(name);
    }
    
    public static Brand sharedBrand (String name)
    {
        Brand brand = (Brand)Brands.get(name);

        if (brand == null) {
            AWMultiLocaleResourceManager resourceManager = resourceManagerForBrandName(name);
            if (resourceManager == null) {
                brand = NotFound;
            }
            else {
                brand = new Brand(name, resourceManager);
            }
            Brands.put(name, brand);
        }
        
        return brand == NotFound ? null : brand;
    }

    private Brand ()
    {
    }
    
    private Brand (String name, AWMultiLocaleResourceManager resourceManager)
    {
        _name = name;
        _resourceManager = resourceManager;
    }

    public String name ()
    {
        return _name;
    }
    
    public AWResourceManager resourceManagerForLocale (Locale locale)
    {
        return _resourceManager.resourceManagerForLocale(locale);
    }

    public static AWMultiLocaleResourceManager resourceManagerForBrandName (String name)
    {
        String directory = Widgets.getDelegate().getDirectoryForBrand(name);
        String urlPrefix = Widgets.getDelegate().getUrlPrefixForBrand(name);

        if (directory == null || urlPrefix == null) {
            return null;
        }
        
        AWApplication application = AWConcreteApplication.defaultApplication();
        AWMultiLocaleResourceManager resourceManager = application.createResourceManager();

            // Register the app specific brand directory (e.g. brands/simple/Buyer)
        String appName = application.name();
        String appDirectory = StringUtil.strcat(directory, File.separator, appName);
        String appUrlPrefix = StringUtil.strcat(urlPrefix, "/", appName);
        resourceManager.registerResourceDirectory(appDirectory, appUrlPrefix);

            // Register the brand directory itself (e.g. brands/simple)
        resourceManager.registerResourceDirectory(directory, urlPrefix);

            // Chain to the default resource manager so non branded resources
            // are found.
        resourceManager.setNextResourceManager(application.resourceManager());
        return resourceManager;
    }
}
