/*
    Copyright (c) 2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/EarlyAccessBanner.java#2 $
 */
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

/**
 * Component that shows a Early Access Banner on the top of the 
 * Login/App pages to indicate that the app is running in Early access mode
 * 
 * @author pshenoy
 */
public class EarlyAccessBanner extends AWComponent
{
    private static Delegate EarlyAccessDelegate;

    public static void setDelegate (Delegate delegate)
    {
        EarlyAccessDelegate = delegate;
    }
    
    /**
     *  Flag whether to show the early access banner
     *  
     *  @return boolean
     */  
    public boolean showEarlyAccessBanner ()
    {
        if (EarlyAccessDelegate != null) {
            return EarlyAccessDelegate.isInEarlyAccess(requestContext());
        }
        return false;
    }

    /**
     *  Delegate that checks for whether the app is in early access mode
     *  
     *  @return boolean
     */
    public static interface Delegate
    {
        public boolean isInEarlyAccess (AWRequestContext requestContext);
    }
}
