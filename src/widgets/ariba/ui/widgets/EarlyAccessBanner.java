/*
    Copyright (c) 2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/EarlyAccessBanner.java#2 $

    Responsible: pshenoy
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
