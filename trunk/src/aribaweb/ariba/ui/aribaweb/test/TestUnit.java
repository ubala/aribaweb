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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestUnit.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;

import java.util.List;

public class TestUnit
{
    private String _name;
    private String _mainName;
    private String _secondName;
    
    private List _uiWithParamLinks = ListUtil.list();
    private List _uiNoParamLinks = ListUtil.list();
    private List _noUIWithParamLinks = ListUtil.list();
    private List _noUiNoParamLinks = ListUtil.list();

    private List _noLinks = ListUtil.list();
    public TestUnit(String name, List<TestLinkHolder> links)
    {
        _name = name;
        _mainName = ClassUtil.stripPackageFromClassName(name);
        _secondName = ClassUtil.stripClassFromClassName(name);

        for (TestLinkHolder link : links) {
            if (link.isInteractive()) {
                if (link.requiresParam()) {
                    _uiWithParamLinks.add(link);
                }
                else {
                    _uiNoParamLinks.add(link);
                }
            }
            else {
                if (link.requiresParam()) {
                    _noUIWithParamLinks.add(link);
                }
                else {
                    _noUiNoParamLinks.add(link);
                }
            }
        }
    }

    public String getMainName ()
    {
        return _mainName;
    }
    public String getSecondaryName ()
    {
        return _secondName;
    }

    public List uiParamLinks ()
    {
        return _uiWithParamLinks;
    }

    public List uiNoParamLinks ()
    {
        return _uiNoParamLinks;
    }

    public List noUiParamLinks ()
    {
        return _noUIWithParamLinks;
    }

    public List noUiNoParamLinks ()
    {
        return _noUiNoParamLinks;
    }
    

    public boolean hasUiParamLinks ()
    {
        return _uiWithParamLinks.size() > 0;
    }

    public boolean hasUiNoParamLinks ()
    {
        return _uiNoParamLinks.size() > 0;
    }

    public boolean hasNoUiParamLinks ()
    {
        return _noUIWithParamLinks.size() > 0;
    }

    public boolean hasNoUiNoParamLinks ()
    {
        return _noUiNoParamLinks.size() > 0;
    }
}
