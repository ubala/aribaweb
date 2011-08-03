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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TabSet.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWDropContainer;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.PerformanceState;

// subclassed by ValidatingTabSet.
public class TabSet extends AWComponent
{
    private AWBinding _indexBinding;
    public int _currentIndex;
    public AWComponentReference _currentTab;
    public AWComponentReference _selectedTab;
//    public Object _selectedTabDropType;
    private AWComponentReference[] _tabs;

    public void init ()
    {
        super.init();

        _indexBinding =  bindingForName(AWBindingNames.index, true);
        /*
        if (pageComponent() != this) {  // (pageComponent() == this) tests whether we are being instantiated for validation
        }
        */
    }

    public boolean isStateless ()
    {
        // This is stateful (primarily) so we can avoid
        // reconstructing the tabs list each time.
        return false;
    }

    private AWComponentReference[] extractTabsArray ()
    {
        AWComponentReference[] tabsArray = null;
        AWElement contentElement = componentReference().contentElement();
        if (contentElement instanceof AWTemplate) {
            AWTemplate contentTemplate = (AWTemplate)contentElement;
            tabsArray = (AWComponentReference[])contentTemplate.extractElementsOfClass(AWComponentReference.class);
        }
        else if (contentElement instanceof AWComponentReference) {
            tabsArray = new AWComponentReference[] {
                (AWComponentReference)contentElement,
            };
        }
        else {
            // (pageComponent() == this) tests whether we are being instantiated for validation
            throw new AWGenericException("contentElement not an AWTemplate or AWComponentReference");
        }
        return tabsArray;
    }

    public AWComponentReference[] tabs ()
    {
        return _tabs;
    }

    public Object currentTabLabel ()
    {
        AWBinding binding = _currentTab.bindingForName(AWBindingNames.label);
        Object value = encodedStringValueForBinding(binding);
        return value == null ? (Object)" - " : value;
    }

    public Object currentTabSemanticKey ()
    {
        AWBinding binding = _currentTab.bindingForName(AWBindingNames.awname);
        return encodedStringValueForBinding(binding);        
    }


    public Object currentTabDropType ()
    {
        return dropType(_currentTab);
    }

    private Object dropType (AWComponentReference tab)
    {
        AWBinding binding = tab.bindingForName(AWBindingNames.dropType);
        return binding == null ? null : encodedStringValueForBinding(binding);
    }

    private AWResponseGenerating dropAction (AWComponentReference tab)
    {
        AWBinding binding =  tab.bindingForName(AWDropContainer.DropActionBinding);
        return binding == null ? null : (AWComponent)binding.value(parent());
    }

    public AWResponseGenerating currentTabDropAction ()
    {
        return dropAction(_currentTab);
    }

    public void setSelectedTab (AWComponentReference tab)
    {
        _selectedTab = tab;
        if (_indexBinding != null) {
            setValueForBinding(_currentIndex, _indexBinding);
        }
//        _selectedTabDropType = dropType(_selectedTab);
    }

//    public boolean isSelectedTabNotDropTarget ()
//    {
//        return _selectedTabDropType == null;
//    }
//
//    public AWResponseGenerating selectedTabDropAction ()
//    {
//        return dropAction(_selectedTab);
//    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        tabArray().computeVisibleTabs(this, _tabs);
        if (_indexBinding != null) {
            int selectedIndex = intValueForBinding(_indexBinding);
            if (selectedIndex < _tabs.length) {
                _selectedTab = _tabs[selectedIndex];
//                 AWBinding binding= _selectedTab.bindingForName(AWDropContainer.DropActionBinding);
//                _selectedTabDropType = encodedStringValueForBinding(binding);
            }
        }
        super.renderResponse(requestContext, component);

        if (PerformanceState.threadStateEnabled() && _selectedTab != null) {
            AWBinding labelBinding =_selectedTab.bindingForName(BindingNames.label);
            PerformanceState.getThisThreadHashtable().setDestinationArea(labelBinding._bindingDescription());
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWComponentReference selectedTab = _selectedTab;
        
        if (PerformanceState.threadStateEnabled() && selectedTab != null) {
            AWBinding labelBinding = _selectedTab.bindingForName(BindingNames.label);
            PerformanceState.getThisThreadHashtable().setSourceArea(labelBinding._bindingDescription());
        }

        AWResponseGenerating actionResults = super.invokeAction(requestContext, component);
        if (selectedTab != _selectedTab) {
            recordBacktrackState(selectedTab);
        }
        return actionResults;
    }

    private TabArray tabArray ()
    {
        TabArray tabArray = (TabArray)componentReference().userData();
        if (tabArray == null) {
            AWComponentReference[] allTabs = extractTabsArray();
            Assert.that(allTabs != null && allTabs.length > 0,  "No tabs found");
            tabArray = new TabArray(allTabs);
            componentReference().setUserData(tabArray);
            _tabs = null;
        }
        if (_tabs == null) _tabs = tabArray.newTabList();

        return tabArray;
    }

    /*-----------------------------------------------------------------------
        Backtracking
      -----------------------------------------------------------------------*/

    public Object restoreFromBacktrackState (Object backtrackState)
    {
        AWComponentReference currentlySelectedTab = _selectedTab;
        AWComponentReference tab = (AWComponentReference)backtrackState;
        _currentIndex = AWUtil.indexOfIdentical(tabs(), tab);
        setSelectedTab(tab);
        return currentlySelectedTab;
    }
}

final class TabArray
{
    private final AWComponentReference[] _allTabs;
    private final boolean _hasVisibleBindings;

    protected TabArray(AWComponentReference[] tabs)
    {
        _allTabs = tabs;
        _hasVisibleBindings = initHasVisibleBindings(tabs);
    }

    protected AWComponentReference[] newTabList ()
    {
        return (AWComponentReference[])_allTabs.clone();
    }

    private boolean initHasVisibleBindings (AWComponentReference[] tabs)
    {
        for (int index = tabs.length - 1; index > -1; index--) {
            if (tabs[index].bindingForName(AWBindingNames.isVisible) != null) {
                return true;
            }
        }
        return false;
    }

    protected void computeVisibleTabs (AWComponent component,
                                       AWComponentReference[] visibleTabs)
    {
        // An invisible tab is passed as null, not eliminated from the list.  We need the
        // list to stay the same length so that stateful components will remain in the
        // same place.
        if (_hasVisibleBindings) {
            for (int index = _allTabs.length - 1; index > -1; index--) {
                AWComponentReference currentTab = _allTabs[index];
                AWBinding isVisibleBinding = currentTab.bindingForName(AWBindingNames.isVisible);
                visibleTabs[index] = (isVisibleBinding == null || component.booleanValueForBinding(isVisibleBinding)) ?
                    currentTab : null;
            }
        }
    }
}
