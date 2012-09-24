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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BaseTabSet.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.fieldvalue.OrderedList;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;

public final class BaseTabSet extends AWComponent
{
    private Object _tabList;
    public Object _selectedTab;
    private int _selectedTabIndex;

    protected void awake ()
    {
        _selectedTabIndex = -1;
    }

    protected void sleep ()
    {
        _tabList = null;
        _selectedTab = null;
        _selectedTabIndex = 0;
    }

    public Object tabList ()
    {
        if (_tabList == null) {
            _tabList = valueForBinding(AWBindingNames.list);
        }
        return _tabList;
    }

    public Object selectedTab ()
    {
        if (_selectedTab == null) {
            _selectedTab = valueForBinding(AWBindingNames.selection);
            if (_selectedTab == null) {
                Object tabList = tabList();
                OrderedList list = OrderedList.get(tabList);
                Object selectedTab = null;
                for (int i=0, size=list.size(tabList);
                     i < size && selectedTab == null; i++) {
                    selectedTab = list.elementAt(tabList, i);
                }
                Assert.that(selectedTab != null, "No visible tabs found in tabset.");
                setSelectedTab(selectedTab);
            }
        }
        return _selectedTab;
    }

    public void setSelectedTab (Object selectedTab)
    {
        _selectedTab = selectedTab;
        setValueForBinding(_selectedTab, AWBindingNames.selection);
    }

    private int selectedTabIndex ()
    {
        if (_selectedTabIndex == -1) {
            Object selectedTab = selectedTab();
            Object tabList = tabList();
            _selectedTabIndex = (selectedTab == null) ? 0 : OrderedList.get(tabList).indexOf(tabList, selectedTab);
        }
        return _selectedTabIndex;
    }

    public void pushElementId ()
    {
        // This depends upon the TabList pushing the index at the same time as the selection
        // (so the parent's index relfects the selectedIndex).
        // This is required because if a stateful subcomponent is used as a tab content,
        // the elementId for that subcomponent needs to be the same for each pass.
        AWRequestContext requestContext = requestContext();
        int selectedTabIndex = selectedTabIndex();
        for (int index = 0; index < selectedTabIndex; index++) {
            requestContext.incrementElementId();
        }
        requestContext.pushElementIdLevel();
    }

    public void popElementId ()
    {
        AWRequestContext requestContext = requestContext();
        requestContext.popElementIdLevel();
        int selectedTabIndex = selectedTabIndex();
        Object tabList = tabList();
        int tabListSize = OrderedList.get(tabList).size(tabList);
        for (int index = selectedTabIndex + 1; index < tabListSize; index++) {
            requestContext.incrementElementId();
        }
    }

    public boolean hasClass ()
    {
        return hasBinding(BindingNames.classBinding);
    }
    
    //If css class doesnt exist, we need to return pageTabWrapper 
    //otherwise, the tabset style will be garbled 
    public String cssClass () {
        String cssClass = stringValueForBinding(BindingNames.classBinding);
        
        if (StringUtil.nullOrEmptyOrBlankString(cssClass)) {
            return "pageTabWrapper";
        } else {
            return cssClass;
        }
    }

}
