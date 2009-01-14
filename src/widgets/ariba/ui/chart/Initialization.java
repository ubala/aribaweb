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

    $Id: //ariba/platform/ui/widgets/ariba/ui/chart/Initialization.java#1 $
*/
package ariba.ui.chart;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.util.fieldvalue.FieldValue;

public final class Initialization
{
    private static boolean _DidInit = false;

    public static void init ()
    {
        if (!_DidInit) {
            _DidInit = true;
            // register our resources with the AW
            AWConcreteServerApplication application = (AWConcreteServerApplication)AWConcreteServerApplication.sharedInstance();

            application.resourceManager().registerPackageName("ariba.ui.chart", true);
        }
    }
}