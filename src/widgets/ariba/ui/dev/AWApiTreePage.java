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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWApiTreePage.java#3 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.dev.AWApiFramePage;

public final class AWApiTreePage extends AWComponent
{
    public static boolean _didLoadAll = false;
    public AWApiFramePage _rootPage;

    public void setup (AWApiFramePage rootPage)
    {
        _rootPage = rootPage;
    }

    public void loadAllTemplates ()
    {
        _didLoadAll = true;
        ((AWConcreteApplication)application()).preinstantiateAllComponents(true, requestContext());
        _rootPage.initPackageList();
    }

    public boolean didLoadAll ()
    {
        return _didLoadAll;
    }

    public AWComponent openValidationErrorPage ()
    {
        // always force a reload-all
        loadAllTemplates();

        AWValidationErrorPage errorPage = (AWValidationErrorPage)pageWithName(AWValidationErrorPage.Name);
        errorPage.setup(page().validationContext(), null, null);
        return errorPage;
    }
}
