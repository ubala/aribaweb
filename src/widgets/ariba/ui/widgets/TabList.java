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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TabList.java#5 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWHtmlForm;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.html.AWBaseImage;
import ariba.ui.aribaweb.html.AWForm;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldvalue.OrderedList;

public final class TabList extends AWComponent
{
    private static AWEncodedString TabTextStyle = new AWEncodedString("tabText");
    public Object _tabDefinitions;
    public Object _visibleTabDefinitions;
    private OrderedList _allTabs;
    private OrderedList _visibleTabs;
    public String _barExtensionBackground;
    public AWEncodedString _elementId;
    public boolean _isSubmitForm;
    private Object _currentTabDefinition;
    private int _currentIndex;
    private Object _selectedTabDefinition;
    private AWBinding _labelBinding;
    private AWBinding _tipBinding;
    private boolean _wasPreviousSelected;
    private AWBinding _semanticKeyBinding;
    public AWEncodedString _allTabsMenuId;

    public static void setTabTextStyle (String styleString)
    {
        TabTextStyle = new AWEncodedString(styleString);
    }

    public AWEncodedString tabTextStyle ()
    {
        return TabTextStyle;
    }

    protected void awake ()
    {
        _tabDefinitions = valueForBinding(AWBindingNames.list);
        _allTabs = OrderedList.get(_tabDefinitions);
        _visibleTabDefinitions = valueForBinding(AWBindingNames.visibles);
        if (_visibleTabDefinitions != null) {
            _visibleTabs = OrderedList.get(_visibleTabDefinitions);
        }
        else {
            _visibleTabDefinitions = _tabDefinitions;
            _visibleTabs = _allTabs;
        }
        _isSubmitForm = initSubmitForm();
        _tipBinding = bindingForName(AWBindingNames.tip, false);
    }

    protected void sleep ()
    {
        _selectedTabDefinition = null;
        _wasPreviousSelected = false;
        _tabDefinitions = null;
        _visibleTabDefinitions = null;
        _allTabs = null;
        _visibleTabs = null;
        _isSubmitForm = false;
        _semanticKeyBinding = null;
        _barExtensionBackground = null;
        _elementId = null;
        _currentTabDefinition = null;
        _currentIndex = 0;
        _labelBinding = null;
        _tipBinding = null;
        _allTabsMenuId = null;
    }

    private boolean initSubmitForm ()
    {
        boolean isSubmitForm = booleanValueForBinding(AWBindingNames.submitForm);
        if (!isSubmitForm) {
            AWHtmlForm currentForm = requestContext().currentForm();
            if (currentForm != null) {
                isSubmitForm = ((AWForm)currentForm).submitFormDefault();
            }
        }
        return isSubmitForm;
    }

    public void setCurrentTabDefinition (Object currentTabDefinition)
    {
        _currentTabDefinition = currentTabDefinition;
        setValueForBinding(_currentTabDefinition, AWBindingNames.item);
    }

    public Object currentTabDefinition ()
    {
        return _currentTabDefinition;
    }

    protected Object selectedTabDefinition ()
    {
        if (_selectedTabDefinition == null) {
            _selectedTabDefinition = valueForBinding(AWBindingNames.selection);
            if (_selectedTabDefinition == null) {
                _selectedTabDefinition = _visibleTabs.elementAt(_visibleTabDefinitions, 0);
            }
        }
        return _selectedTabDefinition;
    }

    public AWEncodedString currentLabel ()
    {
        return encodedStringValueForBinding(_labelBinding);
    }

    public AWEncodedString currentTip ()
    {
        return encodedStringValueForBinding(_tipBinding);
    }

    public boolean showTip ()
    {
        return booleanValueForBinding(AWBindingNames.showTip) &&
               !nullOrEmptyTip();
    }

    public AWResponseGenerating closeTabTip ()
    {
        boolean showTip = booleanValueForBinding(AWBindingNames.showTip);
        if (!showTip) {
            return null;
        }

        setValueForBinding(false, AWBindingNames.showTip);
        return null;
    }
  
    private boolean nullOrEmptyTip ()
    {
        AWEncodedString tip = currentTip();
        return tip == null ||
               StringUtil.nullOrEmptyOrBlankString(tip.string());
    }
    
    public void setCurrentIndex (int index)
    {
        _currentIndex = index;
        setValueForBinding(_currentIndex, AWBindingNames.index);
    }

    private boolean isFirstNonNullTab ()
    {
            // if any previous tabs were not null,
            // than this cannot be the first non null tab
        for (int i = 0; i < _currentIndex; i++) {
            Object tab = _visibleTabs.elementAt(_visibleTabDefinitions, i);
            if (tab != null) {
                return false;
            }
        }

            // theoretically this should not be called if current tab is null,
            // but add a check just to be safe
        if (_currentTabDefinition == null) {
            return false;
        }

        return true;
    }

    private boolean isLastNonNullTab ()
    {
        if (hasHiddenTabs()) return false;
            // if any next tabs were not null,
            // than this cannot be the last non null tab
        int length = _visibleTabs.length(_visibleTabDefinitions);
        for (int i = length - 1; i > _currentIndex; i--) {
            Object tab = _visibleTabs.elementAt(_visibleTabDefinitions, i);
            if (tab != null) {
                return false;
            }
        }

            // theoretically this should not be called if current tab is null,
            // but add a check just to be safe
        if (_currentTabDefinition == null) {
            return false;
        }

        return true;
    }

    private boolean isBeforeTabSelected ()
    {
        Object currentTabSelected = selectedTabDefinition();
        int selectedIndex = _visibleTabs.indexOf(_visibleTabDefinitions, currentTabSelected);

        if (_currentIndex >= selectedIndex) return false;

        for (int i = selectedIndex - 1 ; i > _currentIndex; i--) {
            Object tab = _visibleTabs.elementAt(_visibleTabDefinitions, i);
            if (tab != null) {
                return false;
            }
        }

            // theoretically this should not be called if current tab is null,
            // but add a check just to be safe
        if (_currentTabDefinition == null) {
            return false;
        }

        return true;
    }

    public boolean currentTabSelected ()
    {
        return (_currentTabDefinition == selectedTabDefinition());
    }

    public String currentTabCssClass ()
    {
        String cssClass = "";
        if (isFirstNonNullTab()) {
            cssClass = StringUtil.strcat(cssClass, "firstTab");
        }
        if (isLastNonNullTab()) {
            cssClass = StringUtil.strcat(cssClass, " lastTab");
        }
        if (isBeforeTabSelected()) {
            cssClass = StringUtil.strcat(cssClass, " tabPreSelected");
        }
        if (currentTabSelected()) {
            cssClass = StringUtil.strcat(cssClass, " tabSelected");
        }
        else {
            cssClass = StringUtil.strcat(cssClass, " tab");
        }
        return cssClass;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _semanticKeyBinding = bindingForName(AWBindingNames.awname, false);
        _labelBinding = bindingForName(AWBindingNames.label, false);
        _tipBinding = bindingForName(AWBindingNames.tip, false);
        _barExtensionBackground = AWBaseImage.imageUrl(requestContext, component, "awxBarExtension.gif");
        
        super.renderResponse(requestContext, component);
        _labelBinding = null;
        _tipBinding = null;
        _barExtensionBackground = null;
    }

    ///////////////////////
    // Url/Action Handling
    ///////////////////////
    public AWResponseGenerating tabClicked ()
    {
        boolean tabSetIsDisabled = booleanValueForBinding(AWBindingNames.disabled);
        if (tabSetIsDisabled) {
            return null;
        }

        // This will NOT be called when used from JSP
        setValueForBinding(_currentTabDefinition, AWBindingNames.selection);
        return (AWComponent)valueForBinding(AWBindingNames.action);
    }

    public AWEncodedString currentTabSemanticKey ()  
    {
        return encodedStringValueForBinding(_semanticKeyBinding);
    }

    public boolean isNotDropTarget ()
    {
        return valueForBinding(AWBindingNames.dropType) == null;
    }

    ///////////////////////
    // Visible Tabs
    ///////////////////////
    public boolean hasHiddenTabs ()
    {
        boolean hasHiddenTabs = false;
        if (_visibleTabDefinitions != null) {
            hasHiddenTabs =
                _allTabs.size(_tabDefinitions) > _visibleTabs.size(_visibleTabDefinitions);            
        }
        return hasHiddenTabs;
    }

    public String currentTabMenuItemStyle ()
    {
        if (_visibleTabs.indexOf(_visibleTabDefinitions, _currentTabDefinition) > -1) {
            return "font-weight:bold";
        }
        return null;
    }
}
