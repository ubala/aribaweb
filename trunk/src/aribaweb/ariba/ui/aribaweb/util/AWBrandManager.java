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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBrandManager.java#5 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;

import java.util.List;

public class AWBrandManager extends AWBaseObject
{
    private static final String BrandKey = "awbrand";
    private static final String BrandCookieKey = "awbrand";
    static final String BrandVersionKey = "awbrandversion";
    private static GrowOnlyHashtable BrandTable = new GrowOnlyHashtable();

    //////////////////////////
    // Factory methods / helper methods
    //////////////////////////
    public final AWBrand getBrand (AWRequestContext requestContext)
    {
        String brandName;

        AWSession session = requestContext.existingSession();
        if (session != null) {
            brandName = session.brandName();
        }
        else {
            brandName = getRequestBrand(requestContext);
        }

        if (brandName == null || !isBrandValid(requestContext, brandName)) {
            if (brandName != null) {
                Log.aribawebResource_brand.debug(
                    "Invalid brand name specified: %s", brandName);
            }

            return null;
        }

        AWBrand brand = (AWBrand)BrandTable.get(brandName);
        if (brand == null) {
            synchronized(BrandTable) {
                if (brand == null) {
                    brand = initBrand(brandName);
                    BrandTable.put(brandName,brand);
                }
            }
        }
        return brand;
    }

    protected AWBrand initBrand (String brandName)
    {
        return new AWBrand(brandName);
    }

    public String getRequestBrand (AWRequestContext requestContext)
    {
        String brandName = requestContext.formValueForKey(BrandKey);
        if (brandName == null) {
            brandName = requestContext.cookieValueForKey(BrandCookieKey);
        }
        return brandName;
    }

    public boolean isBrandValid (AWRequestContext requestContext,
                                 String brandName)
    {
        return true;
    }

    public void initBrandedResourceManager (AWMultiLocaleResourceManager base,
                            AWMultiLocaleResourceManager brandResourceManger,
                            String brandName,
                            String version)
    {
        // scan for resources dirs and copy them with additional brandNames
        List resourceDirs = base.resourceDirectories();
        for (int i=0, size=resourceDirs.size(); i < size; i++) {
            AWResourceDirectory rd =
                (AWResourceDirectory)resourceDirs.get(i);
            if (rd instanceof AWFileResourceDirectory) {

                String directoryPath = rd.directoryPath();

                // find ./docroot/ariba/xxx
                // insert ./docroot >>>> /config/branding/<brandName>/<version> <<<< /ariba/xxx
                int index = directoryPath.indexOf("./docroot/ariba");

                if (index != -1) {
                    directoryPath = StringUtil.strcat(
                        directoryPath.substring(0,index+9),
                        "/config/branding/",brandName, "/", version,
                        directoryPath.substring(index+9));

                    Log.aribawebResource_brand.debug(
                        "----> registering dir path: %s  url prefix: %s for brand %s version %s",
                        directoryPath, rd.urlPrefix(), brandName, version);
                    // Todo: check to see if file dir exists?
                    // if so, then need to update when dir is uploaded
                    brandResourceManger.registerResourceDirectory(directoryPath, rd.urlPrefix());
                }
            }
            else {
                // handle AWHttpResourceDirectory
            }
        }
    }
}