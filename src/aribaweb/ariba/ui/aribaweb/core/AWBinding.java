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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBinding.java#48 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceManagerDictionary;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWBindingApi;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ResourceService;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValueAccessor;
import ariba.util.expr.AribaExprEvaluator;
import ariba.util.fieldvalue.Expression;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

final class AWFormattedBinding extends AWVariableBinding
{
    private AWVariableBinding _binding;
    private AWBinding _formatterBinding;

    public void init (String bindingName, AWVariableBinding binding, AWBinding formatterBinding)
    {
        this.init(bindingName);
        _binding = binding;
        _formatterBinding = formatterBinding;
    }

    public boolean isSettableInComponent (Object object)
    {
        return _binding.isSettableInComponent(object);
    }

    public Object value (Object object)
    {
        String formattedString = null;
        Object objectValue = _binding.value(object);
        if (objectValue != null) {
            Object formatter = _formatterBinding.value(object);
            formattedString = (formatter == null) ?
                AWUtil.toString(objectValue) :
                AWFormatting.get(formatter).format(formatter, objectValue);
        }
        return formattedString;
    }

    public void setValue (Object value, Object object)
    {
        Object parsedObject = null;
        String stringValue = (String)value;
        if (stringValue != null) {
            Object formatter = _formatterBinding.value(object);
            if (formatter == null) {
                parsedObject = stringValue;
            }
            else try {
                parsedObject = AWFormatting.get(formatter).parseObject(formatter, stringValue);
            }
            catch (RuntimeException runtimeException) {
                if (object instanceof AWComponent) {
                    ((AWComponent)object).recordValidationError(runtimeException, this, value);
                }
                return;
            }
        }
        _binding.setValue(parsedObject, object);
    }

    protected String bindingDescription ()
    {
        return StringUtil.strcat(_binding.bindingDescription(), "|", _formatterBinding.bindingDescription());
    }
}

final class AWDefaultValueKeyPathBinding extends AWKeyPathBinding
{
    private AWBinding _defaultBinding;

    public void init (String bindingName, String fieldPathString, AWBinding defaultBinding)
    {
        super.init(bindingName, fieldPathString);
        _defaultBinding = defaultBinding;
    }

    public Object value (Object object)
    {
        Object objectValue = super.value(object);
        if (objectValue == null) {
            objectValue = _defaultBinding.value(object);
        }
        return objectValue;
    }
}

////////////////////////
// AWKeyPathBinding
////////////////////////
class AWKeyPathBinding extends AWVariableBinding
{
    // ** Its thread safe to use these globals because they are immutable.
    private FieldPath _fieldPath;

    public void init (String bindingName, FieldPath fieldPath)
    {
        this.init(bindingName);
        _fieldPath = fieldPath;
    }

    public void init (String bindingName, String fieldPathString)
    {
        // Becasue we cache fieldPaths, we don't use the shared pey paths
        // since we get better performance from the lookup skipping
        // built into FieldPath
        FieldPath fieldPath = new FieldPath(fieldPathString);
        this.init(bindingName, fieldPath);
    }

    public boolean isSettableInComponent (Object object)
    {
        return true;
    }

    public Object value (Object object)
    {
        Object objectValue = null;
        try {
            objectValue = _fieldPath.getFieldValue(object);
        }
        catch (AWBindingException bindingException) {
            throw bindingException;
        }
        catch (RuntimeException exception) {
            String message = formatBindingExceptionMessage(object, _fieldPath);
            throw getBindingException(message, exception);
        }
        if (_isDebuggingEnabled && object instanceof AWComponent) {
            AWComponent component = (AWComponent)object;
            String awdebugMessage = Fmt.S("%s: %s <== %s (%s%s)", component.name(), bindingName(), bindingDescription(), formatClassNameForObject(objectValue), formatDescriptionForObject(objectValue));
            debugString(awdebugMessage);
        }
        return objectValue;
    }

    public void setValue (Object value, Object object)
    {
        if (_isDebuggingEnabled && object instanceof AWComponent) {
            AWComponent component = (AWComponent)object;
            String awdebugMessage = Fmt.S("%s: %s ==> %s (%s%s)", component.name(), bindingName(), bindingDescription(), formatClassNameForObject(value), formatDescriptionForObject(value));
            debugString(awdebugMessage);
        }
        try {
            _fieldPath.setFieldValue(object, value);
        }
        catch (AWBindingException bindingException) {
            throw bindingException;
        }
        catch (RuntimeException runtimeException) {
            String message = formatBindingExceptionMessage(object, _fieldPath);
            throw getBindingException(message, runtimeException);
        }
    }

    public String fieldPath ()
    {
        return _fieldPath.toString();
    }

    public FieldPath fieldPathObject ()
    {
        return _fieldPath;
    }

    public Object fieldPathTargetInComponent (AWComponent component)
    {
        return component;
    }

    public String toString ()
    {
        return StringUtil.strcat(": <AWKeyPathBinding> ", bindingName(), "=\"", bindingDescription(), "\"");
    }

    protected String bindingDescription ()
    {
        return StringUtil.strcat("$", _fieldPath.toString());
    }

    //////////////
    // Validation
    //////////////
    protected void validate (AWValidationContext validationContext, AWComponent component, int bindingDirection)
    {
        String fieldName = _fieldPath.car();
        int missingBindingDirection = -1;

        if (_fieldPath.cdr() != null) {
            bindingDirection = FieldValue.Getter;
        }
        FieldValueAccessor fieldValueAccessor = null;
        try {
            if (bindingDirection == AWBindingApi.Both) {
                fieldValueAccessor = FieldValue.get(component).getAccessor(component, fieldName, FieldValue.Setter);
                if (fieldValueAccessor == null) {
                    missingBindingDirection = FieldValue.Setter;
                }
                fieldValueAccessor = FieldValue.get(component).getAccessor(component, fieldName, FieldValue.Getter);
                if (fieldValueAccessor == null) {
                    // if we're already missing the setter, then set to both
                    missingBindingDirection =
                        (missingBindingDirection == FieldValue.Setter) ?
                        AWBindingApi.Both :
                        FieldValue.Getter;
                }
            }
            else if (bindingDirection == AWBindingApi.Either) {
                try {
                    fieldValueAccessor = FieldValue.get(component).getAccessor(component, fieldName, FieldValue.Getter);
                }
                catch (RuntimeException runtimeException) {
                    // ignore exceptions in getting the Getter and just try to get the Setter
                }
                if (fieldValueAccessor == null) {
                    fieldValueAccessor = FieldValue.get(component).getAccessor(component, fieldName, FieldValue.Setter);
                }

                if (fieldValueAccessor == null) {
                    missingBindingDirection = AWBindingApi.Either;
                }
            }
            else {
                Assert.that(bindingDirection == FieldValue.Getter || bindingDirection == FieldValue.Setter, "Invalid bindingDirection: " + bindingDirection);
                fieldValueAccessor = FieldValue.get(component).getAccessor(component, fieldName, bindingDirection);
                if (fieldValueAccessor == null) {
                    missingBindingDirection = bindingDirection;
                }
            }
        } catch (RuntimeException runtimeException) {
            String message = formatBindingExceptionMessage(component, _fieldPath);
            if (AWBinding.ThrowValidationExceptions) {
                throw getBindingException(message,runtimeException);
            } else {
                String exceptionString =  "Exception resolving binding \"" + fieldName + "\": " + runtimeException.toString();
                component.componentDefinition().addInvalidValueForBinding(
                    validationContext, component, bindingName(), exceptionString);
                logString(StringUtil.strcat(message, "\n", exceptionString));
            }
            return;
        }

        if (fieldValueAccessor == null || missingBindingDirection != -1) {
            String message = formatBindingExceptionMessage(component, _fieldPath);

            String missingMethod = null;
            switch (missingBindingDirection) {
                case AWBindingApi.Both:
                    missingMethod = "both setter and getter";
                    break;
                case FieldValue.Getter:
                    missingMethod = "getter";
                    break;
                case FieldValue.Setter:
                    missingMethod = "setter";
                    break;
                case AWBindingApi.Either:
                    missingMethod = "either setter or getter";
                    break;
                default:
                    Assert.that(false, Fmt.S("Unknown binding direction %s", missingBindingDirection));
            }

            String exceptionString =
                Fmt.S("Unable to locate %s method(s) or field named \"" + fieldName + "\"", missingMethod);

            if (AWBinding.ThrowValidationExceptions) {
                try {
                    throw new RuntimeException(exceptionString);
                }
                catch (RuntimeException runtimeException) {
                    throw getBindingException(message,runtimeException);
                }
            }
            else {
                component.componentDefinition().addInvalidValueForBinding(
                    validationContext, component, bindingName(), exceptionString);
                logString(StringUtil.strcat(message, "\n", exceptionString));
            }
        }
    }
}

//////////////////////////////
// AWParentKeyPathBinding
//////////////////////////////
final class AWParentKeyPathBinding extends AWVariableBinding
{
    private FieldPath DummyFieldPath = new FieldPath("AWParentKeyPathBinding_DUMMY");
    private String _fieldPathString;
    private String _bindingKey;
    private FieldPath _additionalKeyPath;
    private AWBinding _defaultBinding;
    private FieldPath _primaryBindingFieldPath = DummyFieldPath;

    // ** Its thread safe to use these globals because they are immutable.

    public void init (String bindingName, String fieldPathString, AWBinding defaultBinding)
    {
        this.init(bindingName);
        // Because we cache fieldPaths, we don't use the shared key paths
        // since we get better performance from the lookup skipping
        // built into FieldPath
        FieldPath fieldPath = new FieldPath(fieldPathString);
        _fieldPathString = fieldPathString.intern();
        _bindingKey = fieldPath.car().intern();
        _additionalKeyPath = fieldPath.cdr();
        _defaultBinding = defaultBinding;
    }

    // todo: make this name more generic (lose "InComponent")
    public boolean isSettableInComponent (Object object)
    {
        // Only supported for AWComponent
        AWComponent component = (AWComponent)object;
        boolean isSettable = false;
        AWBinding binding = component.bindingForName(_bindingKey, true);
        if (binding != null) {
            isSettable = binding.isSettableInComponent(component.parent());
        }
        else if (_defaultBinding != null) {
            isSettable = _defaultBinding.isSettableInComponent(component);
        }
        return isSettable;
    }

    // todo: make this name more generic (lose "InComponent")
    protected boolean bindingExistsInParentForSubcomponent (AWComponent component)
    {
        boolean bindingExists = false;
        if (_defaultBinding != null) {
            bindingExists = _defaultBinding.bindingExistsInParentForSubcomponent(component);
        }
        if (!bindingExists) {
            bindingExists = primaryBinding(component.parent()) != null;
        }
        return bindingExists;
    }

    // takes care of the special case where the parent binding is just a binding and
    // continues recursion of debugValue up the parent binding chain.  (see AWNLSBinding).
    public Object debugValue (Object object)
    {
        // Only supported for AWComponent
        AWComponent component = (AWComponent)object;
        AWBinding primaryBinding = primaryBinding(component);
        if (primaryBinding == null || _additionalKeyPath != null) {
            return value(component);
        }

        // assume we have a primary binding and no additional key path binding
        return primaryBinding.debugValue(component.parent());
    }

    public Object value (Object object)
    {
        // ParentKeyPath bindings only work in for AWComponents
        AWComponent component = (AWComponent)object;
        Object objectValue = null;
        AWBinding primaryBinding = primaryBinding(component);
        if (primaryBinding == null) {
            if (_defaultBinding != null) {
                objectValue = _defaultBinding.value(component);
            }
        }
        else {
            objectValue = component.valueForBinding(primaryBinding);
            if ((_additionalKeyPath != null) && (objectValue != null)) {
                try {
                    objectValue = _additionalKeyPath.getFieldValue(objectValue);
                }
                catch (AWBindingException bindingException) {
                    throw bindingException;
                }
                catch (RuntimeException runtimeException) {
                    String message = formatBindingExceptionMessage(component, _additionalKeyPath);
                    throw getBindingException(message, runtimeException);
                }
            }
            if (_isDebuggingEnabled) {
                String awdebugMessage = Fmt.S("%s: %s <== %s (%s%s)", component.name(), bindingName(), bindingDescription(), formatClassNameForObject(objectValue), formatDescriptionForObject(objectValue));
                debugString(awdebugMessage);
            }
        }
        return objectValue;
    }

    public void setValue (Object objectValue, Object object)
    {
        // ParentKeyPath bindings only work in for AWComponents
        AWComponent component = (AWComponent)object;
        AWBinding primaryBinding = primaryBinding(component);
        if (primaryBinding == null) {
            if (_defaultBinding != null) {
                _defaultBinding.setValue(objectValue, component);
            }
        }
        else {
            if (_isDebuggingEnabled) {
                String awdebugMessage = Fmt.S("%s: %s ==> %s (%s%s)",
                        component.name(), bindingName(), bindingDescription(),
                        formatClassNameForObject(objectValue), formatDescriptionForObject(objectValue));
                debugString(awdebugMessage);
            }
            if (_additionalKeyPath == null) {
                component.setValueForBinding(objectValue, primaryBinding);
            }
            else {
                Object primaryBindingValue = component.valueForBinding(primaryBinding);
                if (primaryBindingValue != null) {
                    try {
                        _additionalKeyPath.setFieldValue(primaryBindingValue, objectValue);
                    }
                    catch (AWBindingException bindingException) {
                        throw bindingException;
                    }
                    catch (RuntimeException runtimeException) {
                        String message = formatBindingExceptionMessage(component, _additionalKeyPath);
                        throw getBindingException(message, runtimeException);
                    }
                }
            }
        }
    }

    private AWBinding primaryBinding (AWComponent component)
    {
        if (_primaryBindingFieldPath == DummyFieldPath) {
            _primaryBindingFieldPath = null;
            if (component.useLocalPool()) {
                Field bindingField = component.lookupBindingField(_bindingKey);
                if (bindingField != null) {
                    _primaryBindingFieldPath = new FieldPath(bindingField.getName());
                }
            }
        }
        AWBinding primaryBinding = null;
        if (_primaryBindingFieldPath == null) {
            primaryBinding = component.bindingForName(_bindingKey, true);
        }
        else {
            primaryBinding = (AWBinding)_primaryBindingFieldPath.getFieldValue(component);
            if (primaryBinding != null) {
                if (!primaryBinding.bindingExistsInParentForSubcomponent(component)) {
                    primaryBinding = null;
                }
            }
        }
        return primaryBinding;
    }

    public String toString ()
    {
        return StringUtil.strcat("<", getClass().getName(), "> ", bindingName(), "=\"", bindingDescription(), "\"");
    }

    public String fieldPath ()
    {
        return _fieldPathString;
    }

    // todo: make this name more generic (lose "InComponent")
    public String effectiveKeyPathInComponent (AWComponent component)
    {
        String effectiveKeyPath = null;
        AWBinding binding = component.bindingForName(_bindingKey, true);
        if (binding != null) {
            AWComponent parentComponent = component.parent();
            effectiveKeyPath = binding.effectiveKeyPathInComponent(parentComponent);
            if (_additionalKeyPath != null) {
                effectiveKeyPath = StringUtil.strcat(effectiveKeyPath, ".", _additionalKeyPath.toString());
            }
        }
        return effectiveKeyPath;
    }

    protected String bindingDescription ()
    {
        String bindingDescription = null;
        if (_additionalKeyPath == null) {
            bindingDescription = StringUtil.strcat("$^", _bindingKey);
        }
        else {
            bindingDescription = StringUtil.strcat("$^", _bindingKey, ".", _additionalKeyPath.toString());
        }
        return bindingDescription;
    }
}

/////////////////////////
// AWLocalizedBinding -- for the AW's preferred localization scheme
/////////////////////////
final class AWLocalizedBinding extends AWVariableBinding
{
    private AWResourceManagerDictionary _localizedStringsHashtable = new AWResourceManagerDictionary();
    private String _defaultString;
    // need a way to optionally specify the key as part of the binding
    // for example foo="$[someKey]this is a string for someKey"
    private String _key;
    private String _comment;

    public void init (String bindingName, String keyString, String defaultString)
    {
        this.init(bindingName);
        _defaultString = defaultString.intern();
        int indexOfColon = keyString.indexOf(':');
        if (indexOfColon != -1) {
            _comment = keyString.substring(indexOfColon).intern();
            keyString = keyString.substring(0, indexOfColon);
        }
        _key = keyString.intern();
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    public void setValue (Object value, Object object)
    {
        throw new AWGenericException(getClass().getName() + ": setValue() not allowed for this type of binding.");
    }

    public Object value (Object object)
    {
        // Localized bindings only work in for AWComponents
        AWComponent component = (AWComponent)object;
        AWSingleLocaleResourceManager resourceManager =
            (AWSingleLocaleResourceManager)component.resourceManager();
        AWEncodedString localizedString = (AWEncodedString)_localizedStringsHashtable.get(resourceManager);
        if (localizedString == null) {
            synchronized (this) {
                localizedString = (AWEncodedString)_localizedStringsHashtable.get(resourceManager);
                if (localizedString == null) {
                    Map localizedStringsHashtable = AWLocal.loadLocalizedAWLStrings(component);
                    if (localizedStringsHashtable != null) {
                        String stringForKey = (String)localizedStringsHashtable.get(_key);
                        if (stringForKey != null) {
                            localizedString = AWEncodedString.sharedEncodedString(stringForKey);
                        }
                    }
                    if (localizedString == null) {
                        String string = resourceManager.pseudoLocalizeUnKeyed(_defaultString);
                        if (AWLocal.IsDebuggingEnabled) {
                            localizedString = AWEncodedString.sharedEncodedString(addEmbeddedContextToString(_key, string, component));
                        }
                        else {
                            localizedString = AWEncodedString.sharedEncodedString(string);
                        }
                    }
                    if (!AWConcreteApplication.IsRapidTurnaroundEnabled) {
                        _localizedStringsHashtable.put(resourceManager, localizedString);
                    }
                }
            }
        }
        return localizedString;
    }

    private String addEmbeddedContextToString (String key, String value, AWComponent component)
    {
        String returnVal = StringUtil.strcat(AWUtil.getEmbeddedContextBegin(key, component.namePath()),
                                             value,
                                             AWUtil.getEmbeddedContextEnd());
        return returnVal;
    }

    protected String bindingDescription ()
    {
        return StringUtil.strcat("$[", ((_key != null) ? _key : ""), ((_comment != null) ? ":" + _comment : ""), "]", _defaultString);
    }
}

/////////////////////////
// AWNLSBinding -- for Buyer's traditional localization scheme
/////////////////////////
final class AWNLSBinding extends AWVariableBinding
{
    private String _key;

    public AWNLSBinding (String bindingName, String keyString)
    {
        this.init(bindingName);
        _key = keyString.intern();
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    public void setValue (Object value, Object object)
    {
        throw new AWGenericException(getClass().getName() + ": setValue() not allowed for this type of binding.");
    }

    public Object value (Object object)
    {
        // AWNLSBinding only supported for AWComponent
        AWComponent component = (AWComponent)object;
        return ResourceService.getService().getLocalizedCompositeKey(_key, component.preferredLocale());
    }

    protected String bindingDescription ()
    {
        return _key;
    }

    public Object debugValue (Object object)
    {
        return _key;
    }
}

/////////////////////////
// AWListBinding -- Static String list
/////////////////////////
final class AWListBinding extends AWVariableBinding
{
    private List _list;

    public void init (String bindingName, String keyString)
    {
        this.init(bindingName);
        // Crazy way to use Hashtable parser to parse array...
        Map map = MapUtil.map();
        String mapString = StringUtil.strcat("{m=(", keyString, ");}");
        ariba.util.core.MapUtil.fromSerializedString(map, mapString);
        _list = (List)map.get("m");
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    public void setValue (Object value, Object object)
    {
        throw new AWGenericException(getClass().getName() + ": setValue() not allowed for this type of binding.");
    }

    public Object value (Object object)
    {
        return _list;
    }

    protected String bindingDescription ()
    {
        return ("$( list )");
    }
}

/////////////////////////
// AWExpressionBinding -- AWExpr expresion
/////////////////////////
final class AWExpressionBinding extends AWVariableBinding implements AWBinding.ExpressionBinding {
    private String _expressionString;
    private AWBinding _substituteBinding;
    private Expression _expression;

    public void init (String bindingName, String keyString)
    {
        this.init(bindingName);
        _expressionString = keyString;
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    protected void assertExpression ()
    {
        if (_substituteBinding == null && _expression == null) {
            _expression = AribaExprEvaluator.instance().compile(_expressionString);
            // print out expression / parse tree
            /*
                System.out.println("EXPR: " + _expressionString);
                ((AribaExprEvaluator.Expression)_expression).printExprTree();
            */
        }
    }

    public void setValue (Object value, Object object)
    {
        assertExpression();
        if (_substituteBinding != null) {
            _substituteBinding.setValue(value, object);
        } else {
            ((AribaExprEvaluator.Expression)_expression).evaluateSet(object, value, null);
        }
    }

    public Object value (Object object)
    {
        // Assert.that(_substituteBinding != null, "Attempt to evalute AWExpressionBinding without preparing: %s=%s", bindingName(), bindingDescription());
        assertExpression();
        if (_substituteBinding != null) {
            return _substituteBinding.value(object);
        } else {
            return _expression.evaluate(object, null);
        }
    }

    protected String bindingDescription ()
    {
        return "${" + _expressionString + "}";
    }

    // ExpressionBinding support
    public String expressionString ()
    {
        return _expressionString;
    }

    public void setSubstituteBinding (AWBinding binding)
    {
        _substituteBinding = binding;
    }
}

/////////////////////////
// AWVariableBinding
/////////////////////////
abstract class AWVariableBinding extends AWBinding
{
    // ** Thread Safety Considerations: the timing feature is not thread safe and should not be used with multiple threads.

    public boolean isConstantValue ()
    {
        return false;
    }

    abstract public boolean isSettableInComponent (Object object);
}

/////////////////////////
// AWClassAccessorBinding
/////////////////////////
final class AWClassAccessorBinding extends AWVariableBinding
{
    Class _targetClass;
    FieldPath _fieldPath;

    public void init (String bindingName, Class targetClass, String fieldPathString)
    {
        this.init(bindingName);
        _targetClass = targetClass;
        _fieldPath = new FieldPath(fieldPathString);
    }

    public boolean isSettableInComponent (Object object)
    {
        return true;
    }

    public Object value (Object object)
    {
        try {
            return _fieldPath.getFieldValue(_targetClass);
        }
        catch (AWBindingException bindingException) {
            throw bindingException;
        }
        catch (RuntimeException runtimeException) {
            String message = formatBindingExceptionMessage(object, _fieldPath);
            throw getBindingException(message, runtimeException);
        }
    }

    public void setValue (Object value, Object object)
    {
        try {
            _fieldPath.setFieldValue(_targetClass, value);
        }
        catch (AWBindingException bindingException) {
            throw bindingException;
        }
        catch (RuntimeException runtimeException) {
            String message = formatBindingExceptionMessage(object, _fieldPath);
            throw getBindingException(message, runtimeException);
        }
    }

    public String fieldPath ()
    {
        return _fieldPath.toString();
    }

    public FieldPath fieldPathObject ()
    {
        return _fieldPath;
    }

    // todo: make this name more generic (lose "InComponent")
    public Object fieldPathTargetInComponent (AWComponent component)
    {
        return _targetClass;
    }

    protected String bindingDescription ()
    {
        return StringUtil.strcat(_targetClass.getName(), ".", _fieldPath.toString());
    }

    public String effectiveKeyPathInComponent (AWComponent component)
    {
        return StringUtil.strcat(_targetClass.getName(), ".", _fieldPath.toString());
    }
}

/////////////////////////
// AWConstantBinding
/////////////////////////
class AWConstantBinding extends AWBinding
{
    private Object _constantObject;
    private AWEncodedString _encodedString = AWUtil.UndefinedEncodedString;
    // todo: might want to consider creating separate bindings for boolean, string, int
    private boolean _booleanValue;

    // ** No thread issues here -- _constantObject is immutable.

    public void init (String bindingName, Object objectValue)
    {
        this.init(bindingName);
        if (AWBindingNames.awstandalone.equals(objectValue)) {
            objectValue = AWBindingNames.awstandalone;
        }
        else if (AWBindingNames.intType.equals(objectValue)) {
            objectValue = AWBindingNames.intType;
        }
        else if (AWBindingNames.booleanType.equals(objectValue)) {
            objectValue = AWBindingNames.booleanType;
        }
        setConstantObject(objectValue);
        _booleanValue = AWBinding.computeBooleanValue(objectValue);
    }

    protected boolean isDynamicBinding ()
    {
        return ((_constantObject == null) || (_constantObject == Boolean.TRUE) || (_constantObject == Boolean.FALSE));
    }


    public void reinit (Object value)
    {
        setConstantObject(value);
    }

    protected void setConstantObject (Object objectValue)
    {
        _constantObject = objectValue instanceof String ? ((String)objectValue).intern() : objectValue;
        _encodedString = (_constantObject instanceof AWEncodedString) ? (AWEncodedString)_constantObject : AWUtil.UndefinedEncodedString;
    }

    public boolean isConstantValue ()
    {
        return true;
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    public Object value (Object object)
    {
        if (_isDebuggingEnabled && object instanceof AWComponent) {
            AWComponent component = (AWComponent)object;
            String componentName = (component == null) ? "(null component)" : component.name();
            String awdebugMessage = Fmt.S("%s: %s <== constant: (%s%s)", componentName, bindingName(), formatClassNameForObject(_constantObject), formatDescriptionForObject(_constantObject));
            debugString(awdebugMessage);
        }
        return _constantObject;
    }

    public AWEncodedString encodedStringValue (Object object)
    {
        if (_encodedString == AWUtil.UndefinedEncodedString) {
            _encodedString = super.encodedStringValue(object);
        }
        return _encodedString;
    }

    public void setValue (Object value, Object object)
    {
        throw new AWGenericException("*** Error: attempt to set value on constant binding with value: " + _constantObject);
    }

    public String toString ()
    {
        return StringUtil.strcat("<AWConstantBinding> ", bindingName(), "=\"", bindingDescription(), "\"");
    }

    protected String bindingDescription ()
    {
        return (_constantObject == null) ? "$null" : _constantObject.toString();
    }

    public boolean booleanValue (Object object)
    {
        return _booleanValue;
    }
}

/////////////////////////
// AWDynamicConstantBinding
/////////////////////////
final class AWDynamicConstantBinding extends AWConstantBinding
{
    private AWBinding _binding;
    // Since _binding is used nulled out after the first evaluation,
    // we need another reference to the containing binding for
    // semantic key generation to work.
    private AWBinding _debugBinding;

    public void init (String bindingName, AWBinding binding)
    {
        this.init(bindingName);
        _binding = binding;
        _debugBinding = binding;
    }

    public void init (String bindingName, String fieldPathString)
    {
        AWBinding binding = AWBinding.bindingWithNameAndKeyPath(bindingName, fieldPathString);
        this.init(bindingName, binding);
    }

    protected boolean isDynamicBinding ()
    {
        return true;
    }

    public boolean isConstantValue ()
    {
        return (_binding == null);
    }

    public Object value (Object object)
    {
        if (_binding != null) {
            Object constantObject = _binding.value(object);
            setConstantObject(constantObject);
            _binding = null;
        }
        return super.value(object);
    }

    public String effectiveKeyPathInComponent (AWComponent component)
    {
        return _debugBinding.effectiveKeyPathInComponent(component);
    }

}

///////////////////////
// AWBooleanNotBinding
///////////////////////
final class AWBooleanNotBinding extends AWVariableBinding
{
    private AWBinding _binding;
    private AWBinding _defaultBinding;

    public void init (String bindingName, AWBinding binding)
    {
        this.init(bindingName);
        _binding = binding;
    }

    public void init (String bindingName, String fieldPathString, AWBinding defaultBinding)
    {
        AWBinding binding =
            AWBinding.bindingWithNameAndKeyPath(bindingName, fieldPathString);
        this.init(bindingName, binding);
        _defaultBinding = defaultBinding;
    }

    protected boolean isDynamicBinding ()
    {
        return true;
    }

    public boolean isConstantValue ()
    {
        return false;
    }

    public boolean isSettableInComponent (Object object)
    {
        return false;
    }

    public Object value (Object object)
    {
        Object objectValue = _binding.value(object);
        // if the object value is null and there is a default binding, then use it
        // otherwise, evaluate the binding -- note that a null binding evaluates to false
        if (objectValue == null && _defaultBinding != null) {
            objectValue = _defaultBinding.value(object);
        }
        else {
            boolean flag = _binding.booleanValue(object);
            objectValue = flag ? Boolean.FALSE : Boolean.TRUE;
        }

        return objectValue;
    }

    public void setValue (Object value, Object object)
    {
        Assert.that(false, "unsupported usage of $! operator");
    }

    protected String bindingDescription ()
    {
        return "$!" + _binding.bindingDescription();
    }
}

/**
    Represents a binding between a named property and a constant or dynamic
    expression in the context of the parent component.
    <p>
    Many binding subtypes are supported, including:
    <ol>
    <li>{@link AWConstantBinding}: e.g.: "10" or "A long string"
    <li>{@link AWKeyPathBinding}: e.g.: "$userName" or "$project.costCenter.budget" or "$delete"</li>
    <li>{@link AWExpressionBinding}: e.g. '${firstName + " " + lastName}' or '${pageWithName("Page2")}'</li>
    <li>{@link AWLocalizedBinding}: e.g. "$[a002]Delete Items"</li>
    </ol>
 */
abstract public class AWBinding extends AWBaseObject implements Cloneable
{
    // ThrowValidationExceptions is false for now (May 8, 2002) but should be made
    // true by default at some point to force the correction of these problems.
    public static boolean ThrowValidationExceptions = false;
    public static final AWBinding DummyBinding = new AWConstantBinding();
    public static final String NullKey = "null";
    public static final String TrueKey = "true";
    public static final String FalseKey = "false";
    protected boolean _isDebuggingEnabled = false;
    private AWEncodedString _name;

    // ** No thread issues here -- _bindingName is immutable and the flags don't matter.

    abstract public boolean isConstantValue ();
    abstract public boolean isSettableInComponent (Object object);
    abstract protected String bindingDescription ();

    // TODO: temporary hack way to make bindingDescription public...
    public String _bindingDescription ()
    {
        return bindingDescription();
    }

    abstract public Object value (Object object);
    abstract public void setValue (Object value, Object object);


    public Object debugValue (Object object)
    {
        return value(object);
    }

    public void init (String bindingName)
    {
        this.init();
        _name = AWEncodedString.sharedEncodedString(bindingName.intern());
    }

    protected boolean isDynamicBinding ()
    {
        return true;
    }

    public void reinit (Object value)
    {
        throw new AWGenericException("reinit not supported for: " + getClass().getName() + " " + this);
    }

    public String bindingName ()
    {
        return _name == null ? "-unnamed-" : _name.string();
    }

    public AWEncodedString name ()
    {
        return _name;
    }

    public String fieldPath ()
    {
        return null;
    }

    public FieldPath fieldPathObject ()
    {
        return null;
    }

    public Object fieldPathTargetInComponent (AWComponent component)
    {
        return null;
    }

    public String effectiveKeyPathInComponent (AWComponent component)
    {
        return fieldPath();
    }

    public void setIsDebuggingEnabled (boolean isDebuggingEnabled)
    {
        _isDebuggingEnabled = isDebuggingEnabled;
    }

    public boolean isDebuggingEnabled ()
    {
        return _isDebuggingEnabled;
    }

    protected boolean bindingExistsInParentForSubcomponent (AWComponent component)
    {
        return true;
    }

    public void setValue (int intValue, Object object)
    {
        Integer integerValue = Constants.getInteger(intValue);
        setValue(integerValue, object);
    }

    public void setValue (boolean booleanValue, Object object)
    {
        Boolean booleanObject = booleanValue ? Boolean.TRUE : Boolean.FALSE;
        setValue(booleanObject, object);
    }

    public String stringValue (Object object)
    {
        Object objectValue = value(object);
        return AWUtil.toString(objectValue);
    }

    public AWEncodedString encodedStringValue (Object object)
    {
        AWEncodedString encodedString = null;
        Object objectValue = value(object);
        if ((objectValue != null) && !(objectValue instanceof AWEncodedString)) {
            encodedString = AWEncodedString.sharedEncodedString(AWUtil.toString(objectValue));
        }
        else {
            encodedString = (AWEncodedString)objectValue;
        }
        return encodedString;
    }

    public static boolean computeBooleanValue (Object value)
    {
        if (value == null) {
            return false;
        }
        else if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }
        else if (value instanceof Integer) {
            return ((Integer)value).intValue() == 0 ? false : true;
        }
        else {
            return true;
        }
    }

    public boolean booleanValue (Object object)
    {
        try {
            Object value = value(object);
            return AWBinding.computeBooleanValue(value);
        }
        catch (Exception exception) {
            String message = formatBindingExceptionMessage(object, fieldPathObject());
            throw getBindingException(message, exception);
        }
    }

    public int intValue (Object object)
    {
        int intValue = 0;
        Object value = value(object);
        if (value instanceof String) {
            intValue = Integer.parseInt((String)value);
        }
        else if (value instanceof Number) {
            intValue = ((Number)value).intValue();
        }
        else {
            String message = Fmt.S(
                "%s: attempt to compute intValue for binding not bound to an String or Number."
                + "Component: %s binding: (%s=\"%s\")",
                getClass().getName(), object, bindingName(), bindingDescription());
            throw new AWGenericException(message);
        }
        return intValue;
    }

    public double doubleValue (Object object)
    {
        double doubleValue = 0;
        Object value = value(object);
        if (value instanceof String) {
            doubleValue = Double.parseDouble((String)value);
        }
        else if (value instanceof Number) {
            doubleValue = ((Number)value).doubleValue();
        }
        else {
            String message = Fmt.S(
                "%s: attempt to compute doubleValue for binding not bound to an String or Number."
                + "Component: %s binding: (%s=\"%s\")",
                getClass().getName(), object, bindingName(), bindingDescription());
            throw new AWGenericException(message);
        }
        return doubleValue;
    }

    //////////////////
    // Creation
    //////////////////
    private static String classNameForKeyPath (String fieldPathString)
    {
        String targetClassName = null;
        int lastDotIndex = fieldPathString.lastIndexOf('.');
        if (lastDotIndex != -1) {
            int penultimateDotIndex = fieldPathString.lastIndexOf('.', lastDotIndex - 1);
            boolean isUpperCase = Character.isUpperCase(fieldPathString.charAt(penultimateDotIndex + 1));
            if (isUpperCase) {
                targetClassName = fieldPathString.substring(0, lastDotIndex);
                boolean isJavaIdentifier = Character.isJavaIdentifierStart(targetClassName.charAt(0));
                if (isJavaIdentifier) {
                    AWMultiLocaleResourceManager resourceManager = AWConcreteApplication.SharedInstance.resourceManager();
                    Class targetClass = resourceManager.classForName(targetClassName);
                    if (targetClass == null) {
                        targetClassName = classNameForKeyPath(targetClassName);
                    }
                }
            }
            else {
                String substring = fieldPathString.substring(0, lastDotIndex);
                targetClassName = classNameForKeyPath(substring);
            }
        }
        return targetClassName;
    }

    /**
        Grammar (with the leading $ stripped):

        1) keypath   = localized | constant | variable ("|" formatter) | ariba_expression
        2) localized = "["key"]"
        3) variable  = field (":" default)
        4) default   = "$"keypath | literal
        5) formatter = "$"field
        6) constant  = boolean | "="field | null
        7) boolean   = true | false
        8) field     = booleanNot | "^" parent binding name | instance accessor | class accessor
        9) booleanNot  = "!" field
    */
    public static AWBinding bindingWithNameAndKeyPath (String bindingName, String keyPathString)
    {
        AWBinding binding = null;
        char firstChar = keyPathString.charAt(0);
        if (firstChar == '[') {
            int indexOfRightBrace = keyPathString.indexOf(']');
            String keyString = keyPathString.substring(1, indexOfRightBrace);
            binding = new AWLocalizedBinding();
            ((AWLocalizedBinding)binding).init(bindingName, keyString, keyPathString.substring(indexOfRightBrace + 1));
        }
        else if (firstChar == '(') {
            int indexOfRightBrace = keyPathString.indexOf(')');
            String listString = keyPathString.substring(1, indexOfRightBrace);
            binding = new AWListBinding();
            ((AWListBinding)binding).init(bindingName, listString);
        }
        else if (firstChar == ':') {  // Deprecated
            String expressionString = keyPathString.substring(1);
            binding = new AWExpressionBinding();
            ((AWExpressionBinding)binding).init(bindingName, expressionString);
        }
        else if (firstChar == '{') {
            String expressionString = keyPathString.substring(1).trim();
            Assert.that(expressionString.endsWith("}"), "Expression binding must end with '}'");
            expressionString = expressionString.substring(0,expressionString.length()-1);
            binding = new AWExpressionBinding();
            ((AWExpressionBinding)binding).init(bindingName, expressionString);
        } else {
            binding = constantBinding(bindingName, keyPathString);
        }
        if (binding == null) {
            String variableString = keyPathString;
            AWBinding formatterBinding = null;
            int pipeIndex = keyPathString.indexOf('|');
            if (pipeIndex != -1) {
                variableString = keyPathString.substring(0, pipeIndex);
                String formatterString = keyPathString.substring(pipeIndex + 1);
                formatterBinding = formatterBinding(bindingName, formatterString);
            }
            AWVariableBinding variableBinding = variableBinding(bindingName, variableString);
            if (formatterBinding != null) {
                binding = new AWFormattedBinding();
                ((AWFormattedBinding)binding).init(bindingName, variableBinding, formatterBinding);
            }
            else {
                binding = variableBinding;
            }
        }
        return binding;
    }

    /**
        See 5) from above
     */
    private static AWBinding formatterBinding (String bindingName, String formatterString)
    {
        if (formatterString.length() < 2) {
            throw new AWGenericException("invalid formatter binding: " + formatterString);
        }
        char firstChar = formatterString.charAt(0);
        if (firstChar == '$') {
            return fieldBinding(bindingName, formatterString.substring(1), null);
        }
        else if (firstChar == '^') {
            // we allow ^ without $
            return fieldBinding(bindingName, formatterString, null);
        }
        else {
            throw new AWGenericException("invalid formatter binding: " + formatterString);
        }
    }

    /**
        See 4) from above
    */
    private static AWVariableBinding variableBinding (String bindingName, String variableString)
    {
        int colonIndex = variableString.indexOf(':');
        String fieldPathString = variableString;
        AWBinding defaultBinding = null;
        if (colonIndex > 0) {
            fieldPathString = variableString.substring(0, colonIndex);
            String defaultString = variableString.substring(colonIndex + 1);
            defaultBinding = defaultBinding(bindingName, defaultString);
        }
        return (AWVariableBinding)fieldBinding(bindingName, fieldPathString, defaultBinding);
    }

    /**
        See 5) from above
    */
    private static AWBinding defaultBinding (String bindingName, String defaultString)
    {
        char firstChar = defaultString.charAt(0);
        AWBinding defaultBinding = null;
        if (firstChar == '$') {
            return bindingWithNameAndKeyPath(bindingName,  defaultString.substring(1));
        }
        else if (firstChar == '^') {
            // we allow ^ without $
            // recursion
            defaultBinding = variableBinding(bindingName, defaultString);
        }
        else {
            defaultBinding = bindingWithNameAndConstant(bindingName, defaultString);
        }
        return defaultBinding;
    }

    /**
        See 6) from above
     */
    private static AWBinding constantBinding (String bindingName, String constantString)
    {
        AWBinding constantBinding = null;
        if (constantString.length() < 2) {
            throw new AWGenericException("invalid constant binding: " + constantString);
        }
        char firstChar = constantString.charAt(0);
        if (constantString.equals(TrueKey) || constantString.equals(FalseKey)) {
            Boolean booleanObject = Boolean.valueOf(constantString);
            constantBinding = new AWConstantBinding();
            ((AWConstantBinding)constantBinding).init(bindingName, booleanObject);
        }
        else if (constantString.equals(NullKey)) {
            constantBinding = new AWConstantBinding();
            ((AWConstantBinding)constantBinding).init(bindingName, null);
        }
        else if (firstChar == '=') {
            constantBinding = new AWDynamicConstantBinding();
            ((AWDynamicConstantBinding)constantBinding).init(bindingName, constantString.substring(1));
        }
        return constantBinding;
    }

    /**
        See 8) from above
    */
    public static AWBinding fieldBinding (String bindingName, String fieldPathString, AWBinding defaultBinding)
    {
        if (StringUtil.nullOrEmptyOrBlankString(fieldPathString)) {
            throw new AWGenericException("invalid field binding: " + fieldPathString);
        }
        AWBinding fieldBinding = null;
        char firstChar = fieldPathString.charAt(0);
        if (firstChar == '!') {
            fieldBinding = new AWBooleanNotBinding();
            ((AWBooleanNotBinding)fieldBinding).init(bindingName, fieldPathString.substring(1), defaultBinding);
        }
        else if (firstChar == '^') {
            fieldBinding = new AWParentKeyPathBinding();
            ((AWParentKeyPathBinding)fieldBinding).init(bindingName, fieldPathString.substring(1), defaultBinding);
        }
        else {
            String targetClassName = classNameForKeyPath(fieldPathString);
            if (targetClassName != null) {
                int targetClassNameLength = targetClassName.length();
                String subsequentKeyPath = fieldPathString.substring(targetClassNameLength + 1);
                Class targetClass = AWUtil.classForName(targetClassName);
                fieldBinding =
                    bindingWithNameTargetClassAndKeyPath(bindingName, targetClass, subsequentKeyPath);
            }
            else if (defaultBinding == null) {
                fieldBinding= new AWKeyPathBinding();
                ((AWKeyPathBinding)fieldBinding).init(bindingName, fieldPathString);
            }
            else {
                fieldBinding= new AWDefaultValueKeyPathBinding();
                ((AWDefaultValueKeyPathBinding)fieldBinding).init(bindingName, fieldPathString, defaultBinding);
            }
        }
        return fieldBinding;
    }

    public static AWBinding bindingWithNameTargetClassAndKeyPath (String bindingName, Class targetClass, String fieldPathString)
    {
        AWClassAccessorBinding classAccessorBinding = new AWClassAccessorBinding();
        classAccessorBinding.init(bindingName, targetClass, fieldPathString);
        return classAccessorBinding;
    }

    public static AWBinding bindingWithNameAndConstant (String bindingName, Object constantObject)
    {
        AWConstantBinding constantBinding = new AWConstantBinding();
        constantBinding.init(bindingName, constantObject);
        return constantBinding;
    }

    public static AWBinding bindingWithNameAndNLSKey (String bindingName, String key)
    {
        return new AWNLSBinding(bindingName, key);
    }

    protected String formatClassNameForObject (Object objectValue)
    {
        return (objectValue == null) ? "" : StringUtil.strcat(objectValue.getClass().getName(), ": ");
    }

    protected String formatDescriptionForObject (Object objectValue)
    {
        String formattedDescription = "null";
        if (objectValue != null) {
            formattedDescription = (objectValue instanceof String) ? StringUtil.strcat("\"", objectValue.toString(), "\"") : objectValue.toString();
        }
        return formattedDescription;
    }

    protected String keyValuePairDescription ()
    {
        return StringUtil.strcat(bindingName(), "=\"", bindingDescription(), "\"");
    }

    /////////////////
    // Binding Dict
    /////////////////
    abstract public static class NameFilter
    {
        abstract public String translate (String orig);
    }

    public static AWBindingDictionary bindingsDictionary (Map bindingsHashtable, NameFilter filter)
    {
        // This is provided to allow users of AWIncludeComponent's awbindingsDictionary to
        // convert a Map of AWBindings to an AWBindingDictionary with uniqued keys.
        // (Use LinkedHashMap to keep bindings in original order)
        Map uniquedHashtable = new LinkedHashMap(bindingsHashtable.size());
        if (!bindingsHashtable.isEmpty()) {
            Iterator bindingEnumerator = bindingsHashtable.values().iterator();
            while (bindingEnumerator.hasNext()) {
                AWBinding currentBinding = (AWBinding)bindingEnumerator.next();
                String uniqueString = currentBinding.bindingName();
                if (filter != null) uniqueString = filter.translate(uniqueString);
                uniquedHashtable.put(uniqueString, currentBinding);
            }
        }
        return new AWBindingDictionary(uniquedHashtable);
    }

    public static AWBindingDictionary bindingsDictionary (Map bindingsHashtable)
    {
        return bindingsDictionary(bindingsHashtable, null);
    }

    public static boolean hasDynamicBindings (Map bindingsHashtable)
    {
        boolean hasDynamicBindings = false;
        if (!bindingsHashtable.isEmpty()) {
            Iterator associationIterator = bindingsHashtable.values().iterator();
            while (associationIterator.hasNext()) {
                AWBinding currentBinding = (AWBinding)associationIterator.next();
                if (currentBinding.isDynamicBinding()) {
                    hasDynamicBindings = true;
                    break;
                }
            }
        }
        return hasDynamicBindings;
    }

    protected Object clone ()
    {
        Object clonedObject = null;
        try {
            clonedObject = super.clone();
        }
        catch (CloneNotSupportedException cloneNotSupportedException) {
            throw new AWGenericException(cloneNotSupportedException);
        }
        return clonedObject;
    }

    protected String formatBindingExceptionMessage (Object object, FieldPath fieldPath)
    {
        String message = null;
        if (object instanceof AWComponent) {
            AWComponent component = (AWComponent)object;
            message = Fmt.S("The following exception occurred while evaluating fieldpath: %s, Component: %s",
                    toString(), component.toString());
        }
        else {
            message = Fmt.S("The following exception occurred while evaluating fieldpath: %s\n", fieldPath);
        }
        return message;
    }

    protected void validate (AWValidationContext validationContext, AWComponent component, int bindingDirection)
    {
        // no-op as default
    }

    protected void validate (AWValidationContext validationContext, AWComponentDefinition componentDefinition)
    {
        // no-op as default
    }

    protected void validate (AWValidationContext validationContext,
                             AWComponent component, AWComponentDefinition componentDefinition)
    {
        // This must check to make sure binding name is valid as per the componentDefinition
        // and that the binding is a legal binding in the component.
        // For now, I'll just make sure the binding is legal in the current component.

        // component is the component which contains the element that has the binding
        // component definition is the definition of the element that has the binding
        int bindingDirection = AWBindingApi.Either;
        AWApi componentApi = componentDefinition.componentApi();
        if (componentApi != null) {
            AWBindingApi bindingApi = componentApi.getBindingApi(bindingName());
            if (bindingApi != null) {
                // catch in case they failed to specify a "direction" on their <binding> tag
                try {
                    bindingDirection = bindingApi.direction();
                } catch (RuntimeException runtimeException) {
                    componentDefinition.addInvalidValueForBinding(validationContext, component, "direction", "binding tag: missing or invalid 'direction' attribute");
                }

            }
        }

        // right side validations
        validate(validationContext, component, bindingDirection);
        validate(validationContext, componentDefinition);
    }

    protected final AWGenericException getBindingException (String message, Exception exception)
    {
        AWGenericException wrappedException = null;
        if (exception instanceof AWGenericException) {
            wrappedException = (AWGenericException)exception;
            wrappedException.addMessage(message);
        }
        else {
            wrappedException = new AWBindingException(message, exception);
        }
        return wrappedException;
    }

    public final class AWBindingException extends AWGenericException
    {
        public AWBindingException (String message, Exception exception)
        {
            super(message, exception);
        }
    }

    public interface ExpressionBinding {
        public String expressionString ();
        public void setSubstituteBinding (AWBinding binding);
    }
}
