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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPopup.java#56 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWAction;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWConcreteTemplate;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWRefreshRegion;
import ariba.ui.aribaweb.core.AWInputId;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWEditableRegion;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.fieldvalue.OrderedList;
import ariba.util.core.Assert;

// subclassed for validation
public class AWPopup extends AWComponent
{
    private static final String DelayTakeValues = "delayTakeValues";
    private static final String[] SupportedBindingNames = {
        BindingNames.list, BindingNames.item, BindingNames.index,
        BindingNames.selection, BindingNames.noSelectionString,
        BindingNames.onChange, BindingNames.isRefresh, BindingNames.action,
        BindingNames.disabled, AWPopup.DelayTakeValues,
        BindingNames.name, BindingNames.size, BindingNames.editable
        // is size not used?
    };

    private static final AWEncodedString PopupChanged = AWEncodedString.sharedEncodedString("return ariba.Handlers.hPopupChanged(this, event);");
    private static final AWEncodedString PopupKeyDown = AWEncodedString.sharedEncodedString("return ariba.Handlers.hActionPopupKeyDown(this, event);");
    private static final AWEncodedString ActionClicked = AWEncodedString.sharedEncodedString("return ariba.Handlers.hPopupAction(this, event);");
    private static final AWEncodedString OnMouseWheel = AWEncodedString.sharedEncodedString("return ariba.Handlers.hMouseWheelOnPopup(this, event);");
    public static final String ActionIndicator = "awaction";
    public static final String NoSelectionString = "awnosel";
    public AWEncodedString _elementId;
    public AWEncodedString _currentOptionId;
    public AWEncodedString _noSelectionString;
    public Integer _currentIndex;
    public AWAction _currentAction;
    public Object _orderedList;
    public AWAction[] _actionList;
    private AWBinding _actionBinding;
    private Object _currentItem;
    public Object _selectedItem;
    private boolean _disabled;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _noSelectionString = encodedStringValueForBinding(BindingNames.noSelectionString);
        _selectedItem = valueForBinding(BindingNames.selection);
        _orderedList = valueForBinding(BindingNames.list);
        _actionList = AWPopup.initActionList(componentReference());
        _disabled = booleanValueForBinding(BindingNames.disabled) ||
            AWEditableRegion.disabled(requestContext());
        _actionBinding = bindingForName(BindingNames.action);
        if (booleanValueForBinding(BindingNames.isRefresh) && _actionBinding == null) {
            _actionBinding = AWRefreshRegion.NullActionBinding;
        }
    }

    protected void sleep ()
    {
        _noSelectionString = null;
        _selectedItem = null;
        _orderedList = null;
        _actionList = null;
        _disabled = false;
        _currentOptionId = null;
        _actionBinding = null;
        _elementId = null;
        _currentIndex = null;
        _currentAction = null;
        _currentItem = null;
    }

    public static AWAction[] initActionList (AWComponentReference componentReference)
    {
        AWAction[] actionList = (AWAction[])componentReference.userData();
        if (actionList == null) {
            AWElement contentElement = componentReference.contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate contentTemplate = (AWTemplate)contentElement;
                actionList = (AWAction[])contentTemplate.extractElementsOfClass(AWAction.class);
                componentReference.setUserData(actionList);
                AWConcreteTemplate newTemplate = new AWConcreteTemplate();
                newTemplate.init();
                AWElement[] elementArray = contentTemplate.elementArray();
                int elementArrayLength = elementArray.length;
                for (int index = 0; index < elementArrayLength; index++) {
                    AWElement currentElement = elementArray[index];
                    if (currentElement instanceof AWAction) {
                        break;
                    }
                    newTemplate.add(currentElement);
                }
                componentReference.setContentElement(newTemplate);
            }
        }
        return actionList;
    }

    public void setCurrentItem (Object object)
    {
        _currentItem = object;
        setValueForBinding(_currentItem, BindingNames.item);
    }

    public void setCurrentIndex (Integer integer)
    {
        _currentIndex = integer;
        setValueForBinding(integer, BindingNames.index);
    }

    public AWEncodedString onChangeString ()
    {
        AWEncodedString onChangeString = null;
        AWBinding onChangeBinding = bindingForName(BindingNames.onChange);
        if (_actionBinding != null) {
            onChangeString = PopupChanged;
        }
        else if (_actionList != null && _actionList.length > 0) {
            onChangeString = ActionClicked;
        }
        else if (onChangeBinding != null) {
            // todo: remove this code -- cannot let app developers usurp the onChange.
            onChangeString = encodedStringValueForBinding(onChangeBinding);
        }
        return onChangeString;
    }

    public AWEncodedString onMouseWheelString ()
    {
        AWEncodedString onMouseWheelString = null;
        if (onChangeString() != null) {
            onMouseWheelString = OnMouseWheel;
        }
        return onMouseWheelString;
    }

    public AWEncodedString onKeyDownString ()
    {
        AWEncodedString onKeyDownString = null;
        if ((_actionList != null && _actionList.length > 0) || _actionBinding != null) {
            onKeyDownString = PopupKeyDown;
        }
        return onKeyDownString;
    }

    public boolean isHiddenFieldSender ()
    {
        return AWGenericActionTag.isHiddenFieldSender(request(), _elementId);
    }

    public String isCurrentItemSelected ()
    {
        if (_currentItem == null) {
            return null;
        }
        return _currentItem.equals(_selectedItem) ? BindingNames.awstandalone : null;
    }

    protected void setSelection (Object selection)
    {
        setValueForBinding(selection, BindingNames.selection);
    }

    /**
     * This method allows for overriding in AWPopupClassic.
     * In the new mode of AWPopup operation, we defer pushing the formValue
     * until invokeAction iff the AWPopup was the item clicked and there is
     * an action binding on the popup.  In classic mode, we do not defer
     * and DO push the value during takeValues, but not in invokeAction.
     *
     * When an AWPopup is used as an action item, its somewhat abused and really
     * should behave like a Popupmenu item (ie AWHyperlink semantics).  The new mode
     * of operation approximates that better, and avoids many problems where
     * UI control flow and binding api's are broken by changes during takeValues.
     */
    protected boolean shouldDeferTakeValues ()
    {
        return _actionBinding != null && isHiddenFieldSender() &&
                   (!this.hasBinding(AWPopup.DelayTakeValues) || booleanValueForBinding(AWPopup.DelayTakeValues));
    }

    public void setFormValue (String formValue)
    {
        if (_disabled || shouldDeferTakeValues()) {
            // - user who disables the <select> should not expect the selection to be pushed,
            //   so skip any operation for now.
            // - Also, if the action is defined and not running in class mode, do not take the value
            //   during takeValue, rather take it during invokeAction and push it then.
            // - If we set the form value in invoke action, we need to delay the firing of any triggers
            //   until after invoke action has occurred.
            requestContext().setDataValuePushedInInvokeAction(true);
            return;
        }
        if (!(ActionIndicator.equals(formValue) || "awnop".equals(formValue))) {
            pushSelection(formValue);
        }
    }

    private void pushSelection (String formValue)
    {
        if (!formValue.equals(NoSelectionString)) {
            // set the selection on the parent and let invokeAction continue looking for hit
            try {
                int index = Integer.parseInt(formValue);
                Object selection = OrderedList.get(_orderedList).elementAt(_orderedList, index);
                setSelection(selection);                }
            catch (NumberFormatException numberFormatException) {
                if(!AWConcreteApplication.IsDebuggingEnabled){
                    String msg = ariba.util.i18n.LocalizedJavaString.getLocalizedString(
                        AWPopup.class.getName(), 1, 
                        "An error has occurred while processing your request. Refresh the page and try again.", 
                        preferredLocale());
                    Object errKey = AWErrorManager.getErrorKeyForComponent(this);
                    if(errKey == null) {
                        errKey = _elementId;
                    }
                    recordValidationError(errKey, msg, "");
                }
                else if ("".equals(formValue)) {
                    if (AWConcreteApplication.IsDebuggingEnabled) {
                        Assert.that(
                            requestContext().allowFailedComponentRendezvous(),
                            "AWPopup: Bad form value received (\"\").  "
                                + "This usually occurs when an AWIf or AWFor was altered"
                                + " during applyValues or invokeAction.");
                    }
                }
                else {
                    throw new AWGenericException(numberFormatException);
                }
            }
        }
        else {
            setSelection(null);
        }
    }

    public AWResponseGenerating regularItemClicked ()
    {
        if (shouldDeferTakeValues()) {
            String selection = request().formValueForKey(_elementId.string());
            pushSelection(selection);
            // need to fire triggers after data has been pushed but before the action is invoked in order
            // to maintain the order that has originally been used by Buyer.
            page().pageComponent().postTakeValueActions();
        }
        return (AWResponseGenerating)valueForBinding(_actionBinding);
    }

    public AWResponseGenerating actionItemClicked ()
    {
        return (AWResponseGenerating)valueForBinding(_currentAction._action);
    }

    public boolean isActionVisible ()
    {
        AWBinding binding = _currentAction._isVisible;
        return binding == null ?
            true :
            booleanValueForBinding(binding);
    }

        // recording & playback
        // provide a better semantic key
    protected AWBinding _debugPrimaryBinding ()
    {
        return componentReference().bindingForName(BindingNames.list, parent());
    }

    public String isDisabled ()
    {
        AWRequestContext requestContext = requestContext();
        return (_disabled || requestContext.isPrintMode()) ?
            BindingNames.awstandalone : null;
    }

    public Object popupId ()
    {
        return AWInputId.getAWInputId(requestContext());
    }

    public boolean isEditable ()
    {
        boolean editable = true;
        if (hasBinding(BindingNames.editable)) {
            editable = booleanValueForBinding(BindingNames.editable);
        }
        return editable && !requestContext().isPrintMode();
    }
}
