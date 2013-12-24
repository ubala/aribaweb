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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Chooser.java#42 $
*/


package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.test.TestContext;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.util.core.Fmt;
import ariba.util.core.HTML;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class Chooser extends AWComponent
{
    private static int MaxLength = 10;
    public static int MaxRecentSelected = 4;
    public static String Matches = "Matches";
    public static String FilteredSelections = "FilteredSelections";

    private static final String allowFullMatchOnInput = "allowFullMatchOnInput";

    private static final String[] SupportedBindingNames = {
        BindingNames.selections, BindingNames.selectAction,
        BindingNames.selectionSource, BindingNames.maxLength,
        BindingNames.formatter, BindingNames.disabled,
        BindingNames.size, BindingNames.state,
        BindingNames.multiSelect, BindingNames.searchAction,
        BindingNames.noSelectionString,
        BindingNames.errorKey, BindingNames.basic,
        BindingNames.classBinding,
        allowFullMatchOnInput
    };

    public static String NoSelectionString = "(none selected)";
    public static String MoreSelectedString = "{0} more selected...";
    public static String NoMatchFoundString = "No match found";
    public AWEncodedString _chooserId;
    public AWEncodedString _menuId;
    private ChooserState _chooserState;
    private Object /*AWFormatting*/ _formatter;
    private boolean _disabled;
    private boolean _fullMatchNeeded;
    private Object _errorKey;
    private boolean _allowFullMatchOnInput;
    public boolean _basic;
    private List _selectionList;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _chooserState = (ChooserState)valueForBinding(BindingNames.state);
        _formatter = valueForBinding(BindingNames.formatter);
        _disabled = booleanValueForBinding(BindingNames.disabled) ||
            AWEditableRegion.disabled(requestContext());
        _allowFullMatchOnInput = booleanValueForBinding(allowFullMatchOnInput);
        _basic = booleanValueForBinding(BindingNames.basic);
    }

    protected void sleep ()
    {
        _chooserId = null;
        _menuId = null;
        _chooserState = null;
        _formatter = null;
        _disabled = false;
        _errorKey = null;
        _allowFullMatchOnInput = false;
        _selectionList = null;
        _fullMatchNeeded = false;
    }

    public String noSelectionString ()
    {
        String noSelectionString = stringValueForBinding(BindingNames.noSelectionString);
        return noSelectionString != null ? noSelectionString : localizedJavaString(1, NoSelectionString);
    }

    public String behavior ()
    {
        return _disabled || isReadOnly() ? null : "CH";
    }

    public void setChooserId (AWEncodedString chooserId)
    {
        _chooserId = chooserId;
        _menuId =
            AWEncodedString.sharedEncodedString(StringUtil.strcat("CHM", chooserId.string()));
    }

    public ChooserState chooserState ()
    {
        return _chooserState;
    }

    public String currentItemString ()
    {
        Object currentItem = _chooserState.currentItem();
        return (_formatter == null)
            ? currentItem.toString()
            : AWFormatting.get(_formatter).format(_formatter, currentItem, preferredLocale());
    }

    /**
        Method used to set the semantic key on the drop down image of the chooser.
        This allows the recorder identify the image that the user clicks on.
     */
    public String chooserDropDownSemanticKey ()
    {
        return AWConcreteApplication.IsAutomationTestModeEnabled == true ?
                _debugCompositeSemanticKey("displayValue:dropDown") : "";
    }

    public String chooserAddLinkSemanticKey ()
    {
        return AWConcreteApplication.IsAutomationTestModeEnabled == true ?
                _debugCompositeSemanticKey("displayValue:addLink") : "";
    }

    public String chooserRemoveLinkSemanticKey ()
    {
        return AWConcreteApplication.IsAutomationTestModeEnabled == true ?
                _debugCompositeSemanticKey("displayValue:removeLink") : "";
    }

    public String currentItemHighlightedString ()
    {
        String itemString = currentItemString();
        String pattern = _chooserState.pattern();
        if (pattern != null) {
            pattern = StringUtil.escapeRegEx(pattern);
            pattern = StringUtil.strcat("(?i)(", pattern, ")");
            try {
                itemString = itemString.replaceAll(pattern, "HighlightBegin$1HighlightEnd");
            }
            catch (PatternSyntaxException e) {
                // swallow
            }
        }
        itemString = HTML.escape(itemString);
        if (pattern != null) {
            itemString = itemString.replaceAll("HighlightBegin", "<b>");
            itemString = itemString.replaceAll("HighlightEnd", "</b>");
        }
        return itemString;
    }

    private String displayObjectString ()
    {
        String displayValue = null;
        Object displayObject = _chooserState.displayObject();
        if (displayObject != null) {
            displayValue = (_formatter == null)
                ? displayObject.toString()
                : AWFormatting.get(_formatter).format(_formatter, displayObject, preferredLocale());
        }
            return displayValue;
    }

    public String displayValue ()
    {
        String displayValue = _chooserState.isInvalid() || _basic ? _chooserState.pattern() : null;
        if (displayValue == null) {
            displayValue = displayObjectString();
        }
        if (displayValue == null) {
            displayValue = noSelectionString();
        }
        
        _chooserState.setPrevDisplayValue(displayValue);
        return displayValue;
    }

    /**
        we only do a match if the incoming strings differs from the previous
        value - if the underlying selected object has changed we don't rematch
     */
    public void setDisplayValue (String displayValue)
    {
        String previouslyDisplayed = _chooserState.getPrevDisplayValue();
        String noSelectionString = noSelectionString();
        
        boolean valueChanged = !noSelectionString.equals(displayValue);
        //We don't process the request if the user has not entered any value
        if (valueChanged) {
            //There are cases where the form would get submitted exactly when the 
            //chooser gets focus. In which case, display value will be an empty string
            //and we don't need to process this request.

            //But we still need to ensure the previous value was a no selection string
            //to avoid loosing user delete action i.e.  if the chooser already had a
            //value and the user deletes it.
            if (noSelectionString.equals(previouslyDisplayed)
                && StringUtil.nullOrEmptyString(displayValue))
            {
                valueChanged = false;
            }
        }

        if (valueChanged) {
            boolean hasChanged = (previouslyDisplayed == null ||
                                  !previouslyDisplayed.equals(displayValue));
            _chooserState.hasChanged(hasChanged);
            _chooserState.setPattern(displayValue);
        }
        if (_chooserState.isInvalid()) {
            // we don't validate on every take, so read from cached condition in chooser state
            recordValidationError(errorKey(), noMatchFoundString(), _chooserState.pattern());
        }
    }

    public boolean isReadOnly ()
    {
        AWRequestContext requestContext = requestContext();
        return requestContext.isPrintMode() || requestContext.isExportMode();
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_chooserState == null) {
            _chooserState = new ChooserState();
            boolean multiSelect = booleanValueForBinding(BindingNames.multiSelect);
            _chooserState.setMultiSelect(multiSelect);
            setValueForBinding(_chooserState, BindingNames.state);
        }
        // allow
        _chooserState.clearRecentSelectedObjects();
        if (errorManager().isValidationRequiredInAppend() &&
            _chooserState.isInvalid()) {
            // vjagannath 5/3/2013
            // Check for null error key before we attempt to record the validation error.
            // Currently this is a stateless component and there are scenarios where the
            // error key may be null (Please refer CR# 1-C4KIT3). In such scenarios we
            // don't record the error as it could lead to a FAE if we attempt
            // to do so.
            Object errorKey = errorKey();
            if (errorKey != null) {
            // update error manager in case revalidation is required
                recordValidationError(errorKey,
                                      noMatchFoundString(),
                                      _chooserState.pattern());
            }
        }
        template().elementArray()[1].renderResponse(requestContext, this);
        _chooserState.setFocus(false);
        _chooserState.setRender(false);
        requestContext.incrementElementId();
    }

    public boolean isInvalid ()
    {
        return errorManager().errantValueForKey(errorKey()) != null;
    }

    public boolean showBullet ()
    {
        return !_chooserState.multiSelect() &&
               _chooserState.isSelectedItem() &&
               !_chooserState.isInvalid();
    }

    public boolean showCheck ()
    {
        return _chooserState.multiSelect() &&
               _chooserState.isSelectedItem();
    }

    public String recentSelectedStyle ()
    {
        return ListUtil.lastElement(_chooserState.selectedObjects()) == _chooserState.currentItem() ? "display:none" : null;
    }

    public int maxRecentSelected ()
    {
        return MaxRecentSelected;
    }

    public boolean showMoreSelected ()
    {
        return _chooserState.selectedObjects().size() >= MaxRecentSelected;
    }

    private String moreSelectedString (int offset)
    {
        int moreSelected = _chooserState.selectedObjects().size() - _chooserState.recentSelectedDisplayed() + offset;
        if (moreSelected < 2) {
            return null;
        }
        String format = localizedJavaString(2, MoreSelectedString);
        return Fmt.Si(format, Integer.toString(moreSelected));
    }

    public String moreSelectedString ()
    {
        return moreSelectedString(0);
    }

    public String moreSelectedStringPlusOne ()
    {
        return moreSelectedString(1);
    }

    public String moreSelectedStringPlusTwo ()
    {
        return moreSelectedString(2);
    }

    private String noMatchFoundString ()
    {
        return localizedJavaString(3, NoMatchFoundString);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        _fullMatchNeeded = true;
        template().elementArray()[1].applyValues(requestContext, this);
        requestContext.incrementElementId();

        processFullMatch();
    }

    private void processFullMatch ()
    {
        //_chooserState.hasChanged is set when the user actually types something in.
        //  We need to check this because there are cases when the user selects something
        //  in name table, the field is populated without any typing.
        //_fullMatchNeeded is to ensure we do not run a full match when the user selects
        //  something from the drop down or if it's blur event and the full match was
        //  already run
        if(_chooserState.hasChanged() && _fullMatchNeeded) {
            String pattern = _chooserState.pattern();
            boolean changedLastFullMatch = _chooserState.getLastFullMatchPattern() == null ||
                !_chooserState.getLastFullMatchPattern().equals(pattern);

            if (changedLastFullMatch) {
                if (StringUtil.nullOrEmptyOrBlankString(pattern)) {
                    resetChooser();
                }
                else {
                    match(true, pattern, null);
                    validateAndRecordErrors();
                }
            }
        }
    }

    public boolean isSender ()
    {
        return _chooserId.equals(requestContext().requestSenderId());
    }

    private Object errorKey ()
    {
        if (_errorKey == null) {
            _errorKey = AWErrorManager.getErrorKeyForComponent(this);
        }
        if (_errorKey == null) {
            _errorKey = _chooserId;
        }

        return _errorKey;
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating response = template().elementArray()[1].invokeAction(requestContext, this);
        if (response == null) {
            requestContext.pushElementIdLevel();
            response = template().elementArray()[3].invokeAction(requestContext, this);
            requestContext.popElementIdLevel();
        }
        else {
            requestContext.incrementElementId();
        }
        return response;
    }

    public String selectValue ()
    {
        return "-1";
    }

    public String toggleValue ()
    {
        return "-1";
    }

    public boolean getRemoveValue ()
    {
        return false;
    }

    public String getSelectionList ()
    {
        return null;
    }

    public boolean displayRecentlyViewed ()
    {
        // Don't display the recent viewed items during record and play back
        // so they can not be used during recording, which  forces the
        // user to search for the item they need to select.  If we allow
        // users to select from the recently viewed items, it could cause tests
        // to give false failures, because an item that selected during the recoding face
        // from the recent item list might not be present in the recent item list during playback. 
        return !TestContext.isTestAutomationMode(requestContext());
    }

    public void setSelectionList (String listId)
    {
        if (Matches.equals(listId)) {
            _selectionList = _chooserState.matches();
        }
        else if (FilteredSelections.equals(listId)) {
            _selectionList =_chooserState.filteredSelections();
        }
    }

    public void setSelectValue (String selectionIndexValue)
    {
        if (StringUtil.nullOrEmptyOrBlankString(selectionIndexValue)) {
            return;
        }
        int selectionIndex = Integer.parseInt(selectionIndexValue);
        if (selectionIndex > -1) {
            selectAction(_selectionList, selectionIndex);
            _fullMatchNeeded = false;
        }
    }

    public void setToggleValue (String selectionIndexValue)
    {
        // This might happen if browser behaves unpredictably e.g. sending multiple
        // dom events for onblur, onselect etc. Putting the safety net for typeahead
        // so that we do not throw a NumberFormatException to the user. -Kiran 10/18/2013
        if (StringUtil.nullOrEmptyOrBlankString(selectionIndexValue)) {
            return;
        }

        int selectionIndex = Integer.parseInt(selectionIndexValue);
        if (selectionIndex > -1) {
            _chooserState.setAddMode(true);
            List selections = (List)valueForBinding(BindingNames.selections);
            selectAction(selections, selectionIndex);
            _fullMatchNeeded = false;
        }
    }

    /**
       The size of multi-select choosers can change based on user selections.
       When the chooser is embedded inside a table, the table's size is dependent
       on the size of the chooser and it needs to be notified when there is a 
       change.
       
       We track changes by capturing the size of the selected objects on each request.
     */
    public boolean itemCountChanged ()
    {
        int lastItemCount = _chooserState.lastDisplayedCount();
        int currItemCount = _chooserState.recentSelectedDisplayed();
        boolean ret = false;

        // On first render item count in chooser state is set to -1
        if (lastItemCount != currItemCount && lastItemCount != -1) {
            ret = true;
        }
        _chooserState.setLastDisplayedCount(currItemCount);

        return ret;
    }

    public void setRemoveValue (boolean removeValue)
    {
        if (removeValue) {
            _chooserState.setAddMode(true);
            _chooserState.updateSelectedObjects();
            updateState();
        }
    }

    protected void selectAction (List selections, int selectionIndex)
    {
        if (selections.size() > selectionIndex) {
            Object item = selections.get(selectionIndex);
            if (_basic) {
                _chooserState.setPattern(item.toString());
                searchAction();
            }
            else {
                _chooserState.updateSelectedObjects(item);
            }
            updateState();
        }
    }

    protected void updateState ()
    {
        _chooserState.setFocus(true);
        _chooserState.setRender(true);
        _chooserState.setIsInvalid(false);
        _chooserState.setAddMode(false);
        clearValidationError(errorKey());
    }

    public boolean getFullMatchValue ()
    {
        return false;
    }

    public void setFullMatchValue (boolean fullMatchValue)
    {
        if (!fullMatchValue) {
            return;
        }
        String pattern = _chooserState.pattern();
        if (StringUtil.nullOrEmptyString(pattern)) {
            resetChooser();
        }
        else {
            match(true);
            validateAndRecordErrors();
        }
        _chooserState.setRender(true);
        _fullMatchNeeded = false;
    }

    private void resetChooser ()
    {
        // if blank, and we had valid object, then set it back
        if (!_chooserState.addMode() && !_chooserState.isInvalid()) {
            Object selectedObject = _chooserState.selectedObject();
            if (selectedObject != null) {
                _chooserState.setSelectionState(selectedObject, false);
            }
        }
        _chooserState.setIsInvalid(false);
        _chooserState.setAddMode(false);
        clearValidationError(errorKey());        
    }

    protected void validateAndRecordErrors ()
    {
        if (_basic) {
            return;
        }
        Object match = null;
        List filterSelections = _chooserState.filteredSelections();
        if (filterSelections != null && filterSelections.size() > 0) {
            match = filterSelections.get(0);
        }
        else {
            List matches = _chooserState.matches();
            if (matches != null && matches.size() == 1) {
                match = matches.get(0);
            }
        }
        if (match != null) {
            _chooserState.updateSelectedObjects(match);
            _chooserState.setIsInvalid(false);
            _chooserState.setAddMode(false);
            clearValidationError(errorKey());
        }
        else {
            if (!_chooserState.addMode() && !_chooserState.isInvalid()) {
                _chooserState.setSelectionState(_chooserState.selectedObject(), false);
            }
            _chooserState.setIsInvalid(true);
            recordValidationError(errorKey(), noMatchFoundString(), _chooserState.pattern());
        }
    }

    public AWResponse matchAction ()
    {
        match(_allowFullMatchOnInput);

        AWResponse response = application().createResponse();
        AWRequestContext requestContext = requestContext();
        requestContext.setXHRRCompatibleResponse(response);        
        template().elementArray()[3].renderResponse(requestContext, this);
        return response;
    }

    public boolean canAdd ()
    {
        return !chooserState().isInvalid()
            && !ListUtil.nullOrEmptyList(chooserState().selectedObjects());
    }

    private void match (boolean fullMatch)
    {
        String pattern = request().formValueForKey("chsp");
        String addMode = request().formValueForKey("chadd");
        match(fullMatch, pattern, addMode);
    }

    private void match (boolean fullMatch, String pattern, String addMode)
    {
        _chooserState.setPattern(pattern);
        if (addMode != null) {
            _chooserState.setAddMode(true);
        }
        ChooserSelectionSource selectionSource =
            (ChooserSelectionSource)valueForBinding(BindingNames.selectionSource);

        List selections = (List)valueForBinding(BindingNames.selections);
        int filteredSelectionsSize = 0;
        if (selections != null) {
            List filteredSelections = ListUtil.copyList(selections);
            Object selectedObject = _chooserState.selectedObject();
            if (_chooserState.multiSelect()) {
                List selectedOjects = _chooserState.selectedObjects();
                int size = selectedOjects.size();
                for (int i = 0; i < size; i++) {
                    selectedObject = selectedOjects.get(i);
                    if (_chooserState.addMode() ||
                        _chooserState.selectedObject() != selectedObject) {
                        filteredSelections.remove(selectedObject);
                    }
                }
            }

            filteredSelections = selectionSource.match(filteredSelections, pattern);
            _chooserState.setFilteredSelections(filteredSelections);
            filteredSelectionsSize = filteredSelections.size();
        }

        if (fullMatch && filteredSelectionsSize == 0) {
            _chooserState.setLastFullMatchPattern(pattern);
            int maxLength = intValueForBinding(BindingNames.maxLength);
            maxLength = maxLength > 0 ? maxLength : MaxLength;
            List matches = selectionSource.match(pattern, maxLength);
            _chooserState.setMatches(matches);
            if (selections != null && matches != null) {
                for (Object o : selections) {
                    matches.remove(o);
                }
            }
        }
        else {
            _chooserState.setMatches(null);
        }
    }

    public AWResponseGenerating textAction ()
    {
        if (_basic) {
            return searchAction();
        }
        return null;
    }

    public AWResponseGenerating searchAction ()
    {
        if (hasBinding(BindingNames.searchAction)) {
            return (AWResponseGenerating)valueForBinding(BindingNames.searchAction);
        }
        ChooserPanel panel =
            (ChooserPanel)pageWithName(ChooserPanel.class.getName());
        ChooserSelectionSource selectionSource =
            (ChooserSelectionSource)valueForBinding(BindingNames.selectionSource);
        panel.setup(_chooserState, selectionSource, _formatter);
        return panel;
    }
    
    public boolean isDisabled ()
    {
        return _disabled;
    }

    public String wrapperClass ()
    {
        String wrapperClass = "chWrapper ";
        String cssClass = stringValueForBinding(BindingNames.classBinding);
        if (!StringUtil.nullOrEmptyOrBlankString(cssClass)) {
            wrapperClass += cssClass;           
        }
        return wrapperClass;
    }

    public String menuClass ()
    {
        String menuClass = "awmenu ";
        String cssClass = stringValueForBinding(BindingNames.classBinding);
        if (!StringUtil.nullOrEmptyOrBlankString(cssClass)) {
            menuClass += cssClass + "Menu";
        }
        return menuClass;
    }
    
    public String cssClass ()
    {
        String ret = "chText ";
        if (valueForBinding(ariba.ui.aribaweb.html.BindingNames.size) == null) {
            ret = "chText chTW ";
        }
        if (chooserState().isInvalid()) {
            ret += "chInvalidSelection";
        }
        else if (noSelectionString().equals(displayValue())) {
            ret += "chNoSelection";
        }
        else if (!_basic) {
            ret += "chValidSelection";
        }
        return ret;
    }
}
