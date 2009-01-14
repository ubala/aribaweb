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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/Initialization.java#17 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.widgets.Widgets;
import ariba.ui.widgets.XMLUtil;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.core.ClassUtil;

public final class Initialization
{
    private static boolean _DidInit = false;

    public static void init ()
    {
        if (!_DidInit) {
            _DidInit = true;
            Log.table.setLevel(Log.DebugLevel);
            // register our resources with the AW
            AWConcreteServerApplication application = (AWConcreteServerApplication)AWConcreteServerApplication.sharedInstance();
            String resourceUrl = application.resourceUrl();

            application.resourceManager().registerResourceDirectory("./ariba/ui/table",
                    resourceUrl+"ariba/ui/table/",
                    false);
            application.resourceManager().registerPackageName("ariba.ui.table", true);

            // hook up the CSVDataSource with the DataTable
            AWTDataTable.setCSVSourceFactory(new AWTDataTable.CSVSourceFactory() {
                public AWTDataSource dataSourceForPath(String csvPath, AWComponent parentComponent) {
                    return AWTCSVDataSource.dataSourceForPath(csvPath, parentComponent);
                }
            });
        }
    }
}
