/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestLinkClickCallback.java#1 $

    Responsible: achung
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;


public interface TestLinkClickCallback
{
    public AWResponseGenerating click (AWRequestContext requestContext,
                                       TestLinkHolder link,
                                       AWComponent returnPage);
}
