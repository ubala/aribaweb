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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWServletResourceManager.java#4 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.util.core.Assert;
import ariba.util.core.MultiKeyHashtable;

public class AWServletResourceManager extends AWMultiLocaleResourceManager
{
    private MultiKeyHashtable _realmTable = new MultiKeyHashtable(2);

    public AWResourceManager resolveBrand (String brandName, String version,
                                           boolean shouldCreate)
    {
        // Bad: doublecheck locking
        // Note: using brandName to indicate that this AWRM is the
        // manager for a set of branded ARRM's
        Assert.that(getBrandName() == null,
                    "Invalid AWResourceManager used to resolve brand");
        AWResourceManager rm =
            (AWResourceManager)_realmTable.get(brandName, version);
        if (rm == null && shouldCreate) {
            synchronized(_realmTable) {
                rm = (AWResourceManager)_realmTable.get(brandName, version);
                if (rm == null) {
                    rm = createBrandedResourceManager(brandName, version);
                    rm.setBaseResourceManager(this);
//                    rm.setCacheEnabled(false);
                    _realmTable.put(brandName,version, rm);
                }
            }
        }
        return rm;
    }

    /**
     * Creates a multi-locale resource manager (AWServletResourceManager)
     * which has all required paths prepended with the brand name.
     * Note that this method is distinct from getBrand() in that getBrand()
     * returns a branded multi-locale resource manager which is chained to
     * the base multi-local resource manager via setBaseResourceManager.
     *
     * This method returns a mult-locale resource manager which is not chained
     * to a base resource manager which allows it to be used as the
     * multi-locale resource managers for branded single locale resource
     * managers.
     *
     * @param brandName
     * @aribaapi private
     */
    public AWMultiLocaleResourceManager createBrandedResourceManager (
        String brandName, String version)
    {
        AWServletResourceManager srm = new AWServletResourceManager();
        srm.setBrandName(brandName);

        AWApplication application =
            (AWApplication)AWConcreteApplication.sharedInstance();
        application.getBrandManager().initBrandedResourceManager(
            this, srm, brandName, version);

        return srm;
    }
}
