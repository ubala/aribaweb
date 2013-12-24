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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWGenericElement.java#48 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWStringDictionary;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public final class AWGenericElement extends AWBindableElement
{
    private static int ActionCount = 0;
    private static Object ActionCountLock = new Object();
    private static final String[] EmptyStringArray = {};
    private static GrowOnlyHashtable ValueBindingKeyPaths;
    private static final AWEncodedString SlashRightAngle = new AWEncodedString("/>");
    private static final AWEncodedString AWNameEquals = new AWEncodedString(" awname=\"");
    private static final AWEncodedString AWStandalone = new AWEncodedString(AWBindingNames.awstandalone);

    private AWBinding _tagName;
    private AWBinding _name;
    private AWBinding _elementId;
    private AWBinding _isSender;
    private AWBinding _invokeAction;
    private AWBinding _formValuesKey;
    private AWBinding _formValues;
    private AWBinding _formValue;
    private AWBinding _otherBindings;
    private AWBinding _omitOrEmitTags;
    private AWBinding _semanticKey;
    private AWEncodedString _encodedTagName;
    private AWBindingDictionary _unrecognizedBindingsDictionary;
    private boolean _doOmitTags;
    private boolean _hasFormValues;
    private Boolean _isClosedElement;
    private boolean _hasSemenaticKey;
    private AWBinding _type; // record & playback
    private AWBinding _value; //record & playback

    // ** Thread Safety Considerations: This is shared but all ivars are immutable -- no locking required.

    /*
     Note:  At one point I was considering factoring out the rarely-used ivars of this
     class and only adding an instance of those if needed.  However, I measured the number
     of instances of this class which are used in a good-sized app and found that 
     there are only 84 instances in the entire system.  This number is so small that its
     not worth trying to save memory by factoring out the rarely-used ivars, especially
     if you consider the recurring cost of accessing those ivars.  In any case, below is
     the break down I was proposing.

     Common Ivars (appear in all elements)
        _tagName;
        _unrecognizedBindingsDictionary;
        _otherIvars; (always non-null; will use shared instance of empty OtherIvars at minimum)

     Other Ivars (appear generally in primitive components or very rarely)
        _name;
        _elementId;
        _formValuesKey
        _formValues
        _formValue
        _isSender
        _invokeAction
        _otherBindings
        _omitOrEmitTags
        _doOmitTags
        _isClosedElement
    */

    public void init (String tagName, Map bindingsHashtable)
    {
        // ** This must be a constant assoc.
        AWBinding isClosed = (AWBinding)bindingsHashtable.remove(AWBindingNames.isClosed);
        if (isClosed != null) {
            _isClosedElement = isClosed.booleanValue(null) ? Boolean.TRUE : Boolean.FALSE;
        }
        _tagName = (AWBinding)bindingsHashtable.remove(AWBindingNames.tagName);
        if (_tagName == null) {
            _tagName = (AWBinding)bindingsHashtable.remove(AWBindingNames.elementName);
            if (_tagName == null) {
                _encodedTagName = AWEncodedString.sharedEncodedString(tagName);
            }
        }
        if (_tagName != null && _tagName.isConstantValue()) {
            AWEncodedString tagNameObject = _tagName.encodedStringValue(null);
            if (tagNameObject != null) {
                _encodedTagName = tagNameObject;
            }
        }
        _name = (AWBinding)bindingsHashtable.remove(AWBindingNames.name);
        _elementId = (AWBinding)bindingsHashtable.remove(AWBindingNames.elementId);
        _isSender = (AWBinding)bindingsHashtable.remove(AWBindingNames.isSender);
        _invokeAction = (AWBinding)bindingsHashtable.remove(AWBindingNames.invokeAction);
        _formValuesKey = (AWBinding)bindingsHashtable.remove(AWBindingNames.formValuesKey);
        _formValues = (AWBinding)bindingsHashtable.remove(AWBindingNames.formValues);
        _formValue = (AWBinding)bindingsHashtable.remove(AWBindingNames.formValue);
        _hasFormValues = _formValue != null || _formValues != null;
        _otherBindings = (AWBinding)bindingsHashtable.remove(AWBindingNames.otherBindings);
        _omitOrEmitTags = (AWBinding)bindingsHashtable.remove(AWBindingNames.omitTags);
        if (_omitOrEmitTags != null) {
            _doOmitTags = true;
        }
        else {
            _omitOrEmitTags = (AWBinding)bindingsHashtable.remove(AWBindingNames.emitTags);
            _doOmitTags = false;
        }
        _semanticKey = (AWBinding)bindingsHashtable.remove(AWBindingNames.semanticKeyBindingName());
        _hasSemenaticKey = _semanticKey != null;
        _unrecognizedBindingsDictionary = escapedBindingsDictionary(bindingsHashtable);

        // pass down EmptyHashtable since we have effectively removed all bindings at this point
        // due to fact that we created the _unrecognizedBindingsDictionary from the reminaing.
        // I would have cleared the bindingsHashtable after that, but we have code below
        // to remove _type and _value and I didn't want to disturb that.
        super.init(tagName, EmptyHashtable);
        if (AWConcreteApplication.IsAutomationTestModeEnabled &&
            (ValueBindingKeyPaths == null)) {
            ValueBindingKeyPaths = new GrowOnlyHashtable();
        }
        //INSTANCE_COUNT++;
        //debugString("**** genericElementCount: " + INSTANCE_COUNT);
        _type = (AWBinding)bindingsHashtable.remove(AWBindingNames.type);
        _value = (AWBinding)bindingsHashtable.remove(AWBindingNames.value);
    }

    public void init (String tagName, Map bindingsHashtable, boolean isContainer)
    {
        this.init(tagName, bindingsHashtable);
        if (isContainer) {
            _isClosedElement = Boolean.FALSE;
        }
    }

    public AWBinding _tagName ()
    {
        return _tagName;
    }

    public AWBinding _omitOrEmitTags ()
    {
        return _omitOrEmitTags;
    }

    public AWBinding _nameBinding ()
    {
        return _name;
    }

    public AWEncodedString _encodedTagName ()
    {
        return _encodedTagName;
    }

    public AWBindingDictionary _unrecognizedBindingsDictionary ()
    {
        return _unrecognizedBindingsDictionary;
    }

    public boolean _doOmitTags ()
    {
        return _doOmitTags;
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] allBindings = super.allBindings();
        if (_unrecognizedBindingsDictionary != null) {
            List bindingVector = _unrecognizedBindingsDictionary.elementsVector();
            AWBinding[] myBindings = new AWBinding[bindingVector.size()];
            bindingVector.toArray(myBindings);
            allBindings = (AWBinding[])(AWUtil.concatenateArrays(allBindings, myBindings));
        }
        return allBindings;
    }

    protected static boolean hasGenericElementAttributes (Map bindingsHashtable)
    {
        boolean hasGenericElementAttributes = false;
        if ((bindingsHashtable.get(AWBindingNames.invokeAction) != null) ||
            (bindingsHashtable.get(AWBindingNames.elementId) != null) ||
            (bindingsHashtable.get(AWBindingNames.tagName) != null) ||
            (bindingsHashtable.get(AWBindingNames.isSender) != null) ||
            (bindingsHashtable.get(AWBindingNames.formValuesKey) != null) ||
            (bindingsHashtable.get(AWBindingNames.formValues) != null) ||
            (bindingsHashtable.get(AWBindingNames.formValue) != null) ||
            (bindingsHashtable.get(AWBindingNames.omitTags) != null) ||
            (bindingsHashtable.get(AWBindingNames.emitTags) != null) ||
            (bindingsHashtable.get(AWBindingNames.isClosed) != null)) {
            hasGenericElementAttributes = true;
        }
        return hasGenericElementAttributes;
    }

    protected String staticTagName ()
    {
        return _tagName == null ? _encodedTagName.string() : _tagName.isConstantValue() ? _tagName.stringValue(null) : "DynamicTagName";
    }

    private AWEncodedString semanticKeyValue (AWComponent component, AWBinding binding)
    {
        AWEncodedString semanticKeyValue = null;
        if (binding != null) {
            Object objectValue = binding.debugValue(component);
            if ((objectValue != null) && !(objectValue instanceof AWEncodedString)) {
                semanticKeyValue = AWEncodedString.sharedEncodedString(AWUtil.toString(objectValue));
            }
            else {
                semanticKeyValue = (AWEncodedString)objectValue;
            }
        }
        return semanticKeyValue;
    }

    private AWEncodedString computeSemanticKey (AWRequestContext requestContext, AWComponent component,
                                                AWEncodedString tagName, AWEncodedString nameString)
    {
        AWEncodedString semanticKey = null;
        if (_semanticKey != null) {
            semanticKey = semanticKeyValue(component, _semanticKey);
        }
        else if (_otherBindings != null) {
            // If the genericElement is bound to otherBindings, then walk up and get the semanticKey
            // from the component reference (_semanticKey is a special case of a supported binding).
            AWBinding semanticKeyBinding = component.bindingForName(AWBindingNames.semanticKeyBindingName());
            semanticKey = semanticKeyValue(component.parent(), semanticKeyBinding);
        }
        semanticKey = computeRecordPlaybackSemanticKey(requestContext, component, tagName, nameString, semanticKey);
        return semanticKey;
    }

    private final AWEncodedString initNameString (AWRequestContext requestContext, AWComponent component, AWElementIdPath elementIdPath)
    {
        // Note: initNameString must always evaluate even though omitTags may be true (ie
        // tagName == null).  This is because the elementIdPath is advanced in here and we
        // need to ensure it always does this for the sake of stateful components.

        AWEncodedString encodedName = null;
        if (_elementId != null) {
            if (elementIdPath == null) {
                elementIdPath = requestContext.nextElementIdPath();
            }
            encodedName = elementIdPath.elementId();
            _elementId.setValue(encodedName, component);
        }
        if (_name != null) {
            encodedName = _name.encodedStringValue(component);
            if (encodedName == null) {
                // _name is special since a null value means 'use elementIdPath' rather than drop altogether
                if (elementIdPath == null) {
                    elementIdPath = requestContext.nextElementIdPath();
                }
                encodedName = elementIdPath.elementId();
            }
        }
        else {
            encodedName = elementIdPath == null ? null : elementIdPath.elementId();
        }
        return encodedName;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        AWElementIdPath elementIdPath = _hasFormValues ? requestContext.nextElementIdPath() : null;
        AWEncodedString nameString = initNameString(requestContext, component, elementIdPath);
        if (_hasFormValues) {
            String formValuesKey = null;
            if (_formValuesKey == null) {
                formValuesKey = nameString.string();
            }
            else {
                formValuesKey = _formValuesKey.stringValue(component);
            }
            AWRequest request = requestContext.request();
            if (_formValues != null) {
                String[] formValuesArray = request.formValuesForKey(formValuesKey, false);
                if (formValuesArray == null) {
                    formValuesArray = EmptyStringArray;
                }
                _formValues.setValue(formValuesArray, component);
            }
            if (_formValue != null) {
                Object formValue = request.formValueForKey(formValuesKey, false);
                if (formValue == null) {
                     formValue = "";
                }
                _formValue.setValue(formValue, component);
            }
            requestContext.popFormInputElementId();
        }
    }

    protected boolean _idMatches (AWElementIdPath elementId, AWEncodedString nameString, AWComponent component)
    {
        AWBinding idBinding = (_unrecognizedBindingsDictionary != null)
                                ? _unrecognizedBindingsDictionary.get("id") : null;
        AWEncodedString target = elementId.elementId();

        if (nameString != null && nameString.equals(target)) return true;
        // AWBinding idBinding = _unrecognizedBindingsDictionary.get("id");
        return (idBinding != null
                && ((nameString = idBinding.encodedStringValue(component)) != null)
                && nameString.equals(target));
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWElementIdPath elementIdPath = _hasFormValues ? requestContext.nextElementIdPath() : null;
        AWEncodedString nameString = initNameString(requestContext, component, elementIdPath);

        // Check if this is a pathDebugRequest.  If so, we bail on a match on our name or id
        if (requestContext.isPathDebugRequest() && _idMatches(requestContext.requestSenderIdPath(), nameString, component))
            return requestContext.pageComponent();

        if (_invokeAction != null) {
            boolean isSender = false;
            if (_isSender != null) {
                isSender = _isSender.booleanValue(component);
            }
            else if (nameString != null) {
                String requestSenderId = requestContext.requestSenderId();
                isSender = nameString.equals(requestSenderId);
            }
            if (isSender) {
                AWPage page =  component.page();
                if (page.hasFormValueManager()) {
                    page.formValueManager().processAllQueues();
                }

                // Note:
                // - promoteNewErrors makes the new errors readable (ie old errors from
                //   previous cycle are now gone)
                // - We cannot promote new errors until form value queue is processed.
                AWErrorManager.AWNewErrorManager errorManager = (AWErrorManager.AWNewErrorManager)component.page().errorManager();
                errorManager.promoteNewErrors();

                if (AWConcreteApplication.IsActionLoggingEnabled) {
                    synchronized (ActionCountLock) {
                        ActionCount++;
                    }
                    String actionCount = AWUtil.toString(ActionCount);
                    String actionKeyPath = AWRecordingManager.actionEffectiveKeyPathInComponent(
                            _invokeAction,
                            component);
                    String componentPath = component.componentPath().toString();
                    String sessionId = component.httpSession().getId();
                    AWApplication application = requestContext.application();
                    String actionLogMessage = Fmt.S("** %s: sessionId: \"%s\" componentPath: \"%s\" elementName: \"%s\" action: \"$%s\"", actionCount, sessionId, componentPath, nameString, actionKeyPath);
                    application.logActionMessage(actionLogMessage);
                    actionResults = (AWResponseGenerating)_invokeAction.value(component);
                    application.logActionMessage(Fmt.S("** %s: DONE **", actionCount));
                }
                else {
                    actionResults = (AWResponseGenerating)_invokeAction.value(component);
                }
                if (actionResults == null) {
                    actionResults = component.page().pageComponent();
                }
            }
        }
        return actionResults;
    }

    public AWEncodedString tagNameInComponent (AWComponent component)
    {
        AWEncodedString tagName = null;
        boolean emitTags;

        if (_omitOrEmitTags == null) {
            emitTags = true;
        }
        else {
            emitTags = AWIf.evaluateConditionInComponent(_omitOrEmitTags, component, false);
            if (_doOmitTags) {
                emitTags = !emitTags;
            }
        }
        if (emitTags) {
            if (_encodedTagName != null) {
                tagName = _encodedTagName;
            }
            else {
                tagName = _tagName.encodedStringValue(component);
            }
        }
        return tagName;
    }

    private static GrowOnlyHashtable _TranslatedBindings = new GrowOnlyHashtable();
    public static void registerBindingTranslation (String eventName, String eventNameAlt)
    {
        _TranslatedBindings.put("on"+eventName, AWEncodedString.sharedEncodedString("x"+eventName));
        _TranslatedBindings.put("on"+eventNameAlt, AWEncodedString.sharedEncodedString("x"+eventName));
    }

    static {
        registerBindingTranslation("mousein", "MouseIn");
        registerBindingTranslation("mouseover", "MouseOver");
        registerBindingTranslation("mouseout", "MouseOut");
        registerBindingTranslation("mousedown", "MouseDown");
        registerBindingTranslation("mouseup", "MouseUp");
        registerBindingTranslation("click", "Click");
        registerBindingTranslation("keydown", "KeyDown");
        registerBindingTranslation("keypress", "KeyPress");
        registerBindingTranslation("blur", "Blur");
        registerBindingTranslation("focus", "Focus");

        // These events don't bubble (in IE anyway) so we can't capture them in behaviors
        // registerBindingTranslation("change", "Change");
        // registerBindingTranslation("mousewheel", "MouseWheel");
    }

    protected static class EventFilter extends AWBinding.NameFilter
    {
        public String translate (String orig)
        {
            AWEncodedString translatedString;
            if (orig.startsWith("on") && (translatedString = (AWEncodedString)_TranslatedBindings.get(orig)) != null) {
                return translatedString.string();
            }
            return orig;
        }
    }
    private final static EventFilter EventFilterSingleton = new EventFilter();

    /**
        Create BindingDictionary, and convert any "on..." handlers with "x..." handlers
     */
    public static AWBindingDictionary escapedBindingsDictionary(Map bindingsHashtable)
    {
        if (bindingsHashtable.isEmpty()) return null;
        return AWBinding.bindingsDictionary(bindingsHashtable, EventFilterSingleton);
    }

    public static void appendHtmlAttributeToResponse (AWResponse response, AWComponent component, AWEncodedString bindingName, AWEncodedString attributeValue)
    {
        if (attributeValue != null) {
            response.appendContent(AWConstants.Space);
            response.appendContent(bindingName);

            if (!((attributeValue == AWGenericElement.AWStandalone) || attributeValue.equals(AWGenericElement.AWStandalone))) {
                response.appendContent(AWConstants.Equals);

                AWEncodedString escapedValue = component.escapeAttribute(attributeValue);
                if (escapedValue.mustQuoteAsAttribute()) {
                    response.appendContent(AWConstants.Quote);
                    response.appendContent(escapedValue);
                    response.appendContent(AWConstants.Quote);
                } else {
                    response.appendContent(escapedValue);
                }
            }
        }
    }

    public static void appendHtmlAttributeToResponse (AWResponse response, AWComponent component, String bindingName, AWEncodedString attributeValue)
    {
        appendHtmlAttributeToResponse(response, component, AWEncodedString.sharedEncodedString(bindingName), attributeValue);
    }

    public static void appendOtherBindings (AWResponse response, AWComponent component, AWBindingDictionary unrecognizedBindingsDictionary, AWBinding otherBindings)
    {
        if (unrecognizedBindingsDictionary != null) {
            boolean pathDebug = AWConcreteApplication.IsDebuggingEnabled && 
                            component.requestContext().componentPathDebuggingEnabled();
            for (int index = unrecognizedBindingsDictionary.size() - 1; index >= 0; index--) {
                // Note: bindingName needs to be encodedString
                AWEncodedString bindingName = unrecognizedBindingsDictionary.nameAt(index);
                AWBinding binding = unrecognizedBindingsDictionary.elementAt(index);
                AWEncodedString attributeValue = binding.encodedStringValue(component);
                AWGenericElement.appendHtmlAttributeToResponse(response, component, bindingName, attributeValue);
                if (pathDebug && bindingName.equals(AWBindingNames.id)) {
                    AWDebugTrace trace = component.requestContext().debugTrace();
                    trace.pushElementId(attributeValue);
                    trace.popElementId();
                }
            }
        }
        if (otherBindings != null) {
            AWStringDictionary otherBindingsValues = (AWStringDictionary)otherBindings.value(component);
            if (otherBindingsValues != null) {
                for (int index = otherBindingsValues.size() - 1; index >= 0; index--) {
                    AWEncodedString currentBindingName = otherBindingsValues.nameAt(index);
                    AWEncodedString currentBindingValue = otherBindingsValues.elementAt(index);

                    // translate any "on..." handlers
                    String bindingString = currentBindingName.string();
                    AWEncodedString translatedBinding;
                    if (bindingString.startsWith("on") && (translatedBinding = (AWEncodedString)_TranslatedBindings.get(bindingString)) != null) {
                        currentBindingName = translatedBinding;
                    }

                    AWGenericElement.appendHtmlAttributeToResponse(response, component, currentBindingName, currentBindingValue);
                }
            }
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component, AWEncodedString tagName)
    {
        AWElementIdPath elementIdPath = null;
        if (_hasFormValues) {
            /**
             * For each generic element that has form values, we record the glid in the order they are consumed.
             * Then, when the request comes back in, we use this list to help us navigate to the form elements
             * in question.
            */
            elementIdPath = requestContext.nextElementIdPath();
            if (!requestContext.isPrintMode()) {
                requestContext.recordFormInputId(elementIdPath);
            }
        }
        AWEncodedString nameString = initNameString(requestContext, component, elementIdPath);
        if (tagName != null) {
            AWResponse response = requestContext.response();
            response.appendContent(AWConstants.LeftAngle);
            response.appendContent(tagName);
            appendOtherBindings(response, component, _unrecognizedBindingsDictionary, _otherBindings);
            if ((_name != null) && (nameString != null)) {
                appendHtmlAttributeToResponse(response, component, _name.name(), nameString);
            }
            if (AWConcreteApplication.IsAutomationTestModeEnabled && requestContext.isDebuggingEnabled()) {
                AWEncodedString semanticKey = computeSemanticKey(requestContext, component, tagName, nameString);
                if (semanticKey != null) {
                    response.appendContent(AWNameEquals);
                    AWEncodedString escapedSemanticKey = component.escapeAttribute(semanticKey);
                    response.appendContent(escapedSemanticKey);
                    response.appendContent(AWConstants.Quote);
                }
            }
            if (_isClosedElement == null) {
                _isClosedElement = component.shouldCloseElements() ? Boolean.TRUE : Boolean.FALSE;
            }
            response.appendContent(_isClosedElement.booleanValue() ? SlashRightAngle : AWConstants.RightAngle);
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWEncodedString tagName = tagNameInComponent(component);
        renderResponse(requestContext, component, tagName);
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }

    private AWEncodedString computeRecordPlaybackSemanticKey (AWRequestContext requestContext, AWComponent component,
                                                AWEncodedString tagName, AWEncodedString name,
                                                AWEncodedString semanticKeyFromBinding)
    {
        AWEncodedString semanticKey =
            semanticKeyFromBinding == null ? null: semanticKeyFromBinding;
        String semanticKeyString =
            semanticKeyFromBinding == null ? null : semanticKeyFromBinding.string();
        String nameString = null;
        if (_formValuesKey != null) {
            nameString = _formValuesKey.stringValue(component);
        }
        else {
            nameString = name == null ? null : name.string();
        }
        String elementIdString = nameString;
        if (semanticKeyString == null) {
            if (tagName != null &&
                _type != null &&
                "input".equalsIgnoreCase(tagName.string())) {
                String value = _value == null ? null : _value.stringValue(component);
                if (value != null && nameString != null) {
                    String type = _type.stringValue(component);
                    if ("hidden".equalsIgnoreCase(type)) {
                        // In here, we have a hidden field or radio field
                        if (nameString.equals(AWComponentActionRequestHandler.FormSenderKey) ||
                            nameString.equals(AWComponentActionRequestHandler.SenderKey)) {
                            // element id is the form value in this case
                            semanticKeyString = nameString;
                            elementIdString = value;
                        }
                    }
                    else if ("radio".equalsIgnoreCase(type)) {
                        // element id is the form value in this case
                        elementIdString = value;
                    }
                }
            }
        }
        if (semanticKeyString == null) {
            // Only tags which represent action items or form input elements need semantic keys
            if (_hasFormValues || _invokeAction != null || _hasSemenaticKey) {
                semanticKeyString = component._debugCompositeSemanticKey(null);
            }
        }
        if (semanticKeyString != null) {
            String semanticKeyWithPrefix = AWRecordingManager.applySemanticKeyPrefix(requestContext, semanticKeyString, null);
            semanticKeyString = AWRecordingManager.registerSemanticKey(elementIdString, semanticKeyWithPrefix, requestContext);
            if (semanticKey == null) {
                semanticKey = AWEncodedString.sharedEncodedString(semanticKeyString);
            }
            else if (semanticKey != null && !semanticKey.string().equals(semanticKeyString)) {
                semanticKey = AWEncodedString.sharedEncodedString(semanticKeyString);
            }

        }

        return semanticKey;
    }
}
