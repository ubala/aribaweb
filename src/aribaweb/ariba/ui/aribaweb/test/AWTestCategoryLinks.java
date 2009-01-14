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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/AWTestCategoryLinks.java#2 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.util.core.ClassUtil;
import ariba.util.core.StringUtil;

public class AWTestCategoryLinks extends AWComponent
{
    public void setCurrentTestUnit (TestUnit testUnit)
    {
        setValueForBinding(testUnit, "currentTestUnit");
    }

    public TestUnit getCurrentTestUnit ()
    {
        return (TestUnit)valueForBinding("currentTestUnit");
    }

    public void setCurrentTestUnitLink (TestLinkHolder link)
    {
        setValueForBinding(link, "currentTestUnitLink");
    }

    public TestLinkHolder getCurrentTestUnitLink ()
    {
        return (TestLinkHolder)valueForBinding("currentTestUnitLink");
    }

    public AWResponseGenerating testUnitClick ()
    {
        return getCurrentTestUnitLink().click(requestContext());
    }

    public boolean currentTestUnitLinkActive ()
    {
        TestSessionSetup testSessionSetup = TestLinkManager.instance().getTestSessionSetup();
        if (!testSessionSetup.activateTestLinks(requestContext())) {
            return false;
        }
        else {
            return ((TestLinkHolder)valueForBinding("currentTestUnitLink")).isActive(requestContext());
        }
    }

    public boolean currentTestDisplayTestContextValue ()
    {
        return getCurrentTestUnit().displayTestContextValue() &&
                !StringUtil.nullOrEmptyOrBlankString(currentTestUnitContextValue());
    }

    public String currentTestUnitContextValue ()
    {
        TestSessionSetup testSessionSetup = TestLinkManager.instance().getTestSessionSetup();
        TestContext testContext = TestContext.getTestContext(requestContext());
        Class key = ClassUtil.classForName(getCurrentTestUnit().getFullName());
        String displayName = null;
        if (testContext.get(key) != null) {
            displayName = testSessionSetup.getObjectDisplayName(testContext.get(key));                                    
        }
        return displayName;
    }
}
