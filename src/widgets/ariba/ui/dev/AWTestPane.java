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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWTestPane.java#1 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.test.TestContext;
import ariba.ui.aribaweb.test.AWTestCentralPage;
import ariba.ui.aribaweb.test.ValidationCenter;
import ariba.util.core.ListUtil;
import java.util.List;
import java.util.Set;

public class AWTestPane  extends AWComponent
{
    public String currentItem;

    public boolean isCurrentlyTesting ()
    {
        return TestContext.getTestContext(requestContext()) != null;
    }

    public AWComponent testCentalPage ()
    {
        return requestContext().pageWithName(AWTestCentralPage.Name);
    }

    public AWComponent testValidation ()
    {
        ValidationCenter newPage = (ValidationCenter)
            requestContext().pageWithName(ValidationCenter.class.getName());
        newPage.initialize(page());
        return newPage;
    }

    public List testContextObjectList ()
    {
        TestContext testContext = TestContext.getTestContext(requestContext());
        Set keys = testContext.keys();
        List objectList = ListUtil.list();
        for (Object key : keys) {
            objectList.add(((Class)key).getName());
            if (objectList.size() > 10) {
                break;
            }
        }
        return objectList;
    }

    public String getCurrentName ()
    {
        return currentItem;
    }
}
