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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/VirtualForm.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Constants;
import ariba.util.core.ClassUtil;

public class VirtualForm extends AWComponent
{
    private static final String VirtualFormKey = "AW" + ClassUtil.stripPackageFromClassName(VirtualForm.class.getName());
    public AWEncodedString _elementId;

    /**
     * This can be accessed by other components to determine if they're within a VirtualForm
     * and do the proper thing wrt to bubbling up events.
     * @param requestContext
     */
    public static int virtualFormCount (AWRequestContext requestContext)
    {
        Integer virtualFormCount = (Integer)requestContext.get(VirtualFormKey);
        return virtualFormCount == null ? 0 : virtualFormCount.intValue();
    }

    protected void awake ()
    {
        incrementVirtualFormCount(1);
    }

    protected void sleep ()
    {
        incrementVirtualFormCount(-1);
        _elementId = null;
    }

    private final void incrementVirtualFormCount (int amount) {
        AWRequestContext requestContext = requestContext();
        int intValue = VirtualForm.virtualFormCount(requestContext);
        Integer virtualFormCount = Constants.getInteger(intValue + amount);
        requestContext.put(VirtualFormKey, virtualFormCount);
    }
}
