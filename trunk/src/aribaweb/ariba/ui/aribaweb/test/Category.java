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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/Category.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.util.core.ListUtil;

import java.util.List;

public class Category
{
    private String _name;
    private List<TestUnit> _testUnitList = ListUtil.list();

    public Category (String name)
    {
        _name = name;
    }

    public String getName ()
    {
        return _name;
    }
    
    public void add (TestUnit testUnit)
    {
        _testUnitList.add(testUnit);
    }

    public List<TestUnit> getTestUnitList ()
    {
        return _testUnitList;
    }

}
