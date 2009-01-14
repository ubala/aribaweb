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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBrand.java#4 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;

public class AWBrand extends AWBaseObject
{
    // what is the right default for current version
    private String _currentVersion = "0";
    private String _brandName;

    public AWBrand (String brandName)
    {
        _brandName = brandName;
    }

    public String getName ()
    {
        return _brandName;
    }

    public String getCurrentVersion ()
    {
        return _currentVersion;
    }
    public void setCurrentVersion (String currentVersion)
    {
        _currentVersion = currentVersion;
    }

    public void publishVersion (AWRequestContext requestContext, String version)
    {
        AWMultiLocaleResourceManager mlrm =
            requestContext.application().resourceManager();
        AWResourceManager rm = mlrm.resolveBrand(getName(), version);

// Todo:
//        AWResourceManager[] resourceManagers = mlrm.resourceManagers();
//        for (int i=0; i < resourceManagers.length; i++) {
//            resourceManagers[i].setCacheEnabled(true);
//        }

        _currentVersion = version;
    }

    public boolean isResourceTestMode (AWRequestContext requestContext)
    {
        AWSession session = requestContext.session(false);
        if (session != null) {
            return session.isBrandTestMode();
        }

        return false;
    }

    public String getRequestVersion (AWRequestContext requestContext)
    {
        // check first for explicit brand version
        String brandVersion = requestContext.formValueForKey(AWBrandManager.BrandVersionKey);
        if (brandVersion == null) {
            brandVersion = getCurrentVersion();
        }
        return brandVersion;
    }

    public String getSessionVersion (AWRequestContext requestContext)
    {
        // check for brand version in the session if available
        AWSession session = requestContext.session(false);
        if (session != null) {
            return session.brandVersion();
        }
        return getRequestVersion(requestContext);
    }
}
