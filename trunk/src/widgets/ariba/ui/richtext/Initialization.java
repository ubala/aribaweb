/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/ui/widgets/ariba/ui/richtext/Initialization.java#3 $

    Responsible: kngan
*/

package ariba.ui.richtext;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.util.fieldvalue.FieldValue;

public class Initialization
{
    private static boolean _DidInit = false;

    public static void init ()
    {
        if (!_DidInit) {
            _DidInit = true;
            // register our resources with the AW
            AWConcreteServerApplication application = (AWConcreteServerApplication)AWConcreteServerApplication.sharedInstance();
            String resourceUrl = (String)FieldValue.getFieldValue(application, "resourceUrl");

            application.resourceManager().registerResourceDirectory("./ariba/ui/richtext", resourceUrl+"ariba/ui/richtext/");
            application.resourceManager().registerPackageName("ariba.ui.richtext", true);
        }
    }

}
