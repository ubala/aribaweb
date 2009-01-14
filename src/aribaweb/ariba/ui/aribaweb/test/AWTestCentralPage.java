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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/AWTestCentralPage.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.html.AWImage;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AWTestCentralPage extends AWComponent
{
    final public static String Name = "AWTestCentralPage";
    
    public AWTestCentralPage()
    {
    }

    public void init ()
    {
        super.init();
        Map m = session().dict();
        if (m.get(TestContext.Name) == null) {
            m.put(TestContext.Name, new TestContext());
        }
    }
    public List<Category> testCategoryList ()
    {
        return TestLinkManager.instance().getTestCategoryList();
    }

    private Category _currentCategory;
    public void setCurrentCategory (Category category)
    {
        _currentCategory = category;
    }

    public Category getCurrentCategory ()
    {
        return _currentCategory;
    }

    public List<TestUnit> categoryTestUnits ()
    {
        return _currentCategory.getTestUnitList();
    }

    private TestUnit _testUnit;
    public void setCurrentTestUnit (TestUnit testUnit)
    {
        _testUnit = testUnit;
    }

    public TestUnit getCurrentTestUnit ()
    {
        return _testUnit;
    }

    private TestLinkHolder _testUnitHolder;
    public void setCurrentTestUnitLink (TestLinkHolder testUnitLink)
    {
        _testUnitHolder = testUnitLink;
    }

    public TestLinkHolder getCurrentTestUnitLink ()
    {
        return _testUnitHolder;
    }

    public String reloadImageLink ()
    {
        return AWImage.imageUrl(requestContext(), this, "reloadLink.gif");
    }

    public String appImageLink ()
    {
        return AWImage.imageUrl(requestContext(), this, "appLink.gif");
    }

    public void clearTestContext ()
    {
        TestContext ts = TestContext.getTestContext(requestContext());
        ts.clear();
    }
//    protected boolean shouldValidateRequest ()
//    {
//        return false;
//    }
//
//    protected boolean shouldValidateSession ()
//    {
//        return false;
//    }
}
