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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBindableElement.java#17 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;

import java.util.Map;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

abstract public class AWBindableElement extends AWBaseElement implements AWBindable
{
    protected static final String _list = "_list";
    protected static final String _item = "_item";
    protected static final String _ifTrue = "_ifTrue";
    protected static final String _ifFalse = "_ifFalse";
    protected static final String _equalNull = "_equalNull";
    protected static final String _notEqualNull = "_notEqualNull";
    private static final String ConflictingBindingsMessage = "BindableElement may not have both _list and _if bindings";
    private AWEncodedString _tagName;

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        AWBindableElement bindableElement = null;
        if (!AWBindingNames.UseNamePrefixBinding) {
            bindingsHashtable.remove(AWBindingNames.namePrefix);
        }
        boolean hasIfBindings = hasIfBindings(bindingsHashtable);
        if (hasListBindings(bindingsHashtable)) {
            Assert.that(!hasIfBindings, ConflictingBindingsMessage);
            AWWrapperFor wrapperFor = new AWWrapperFor();
            // Note: this removes some bindings.
            wrapperFor.init(elementName, bindingsHashtable);
            wrapperFor.setContentElement(this);
            bindableElement = wrapperFor;
        }
        else if (hasIfBindings) {
            AWWrapperIf wrapperIf = new AWWrapperIf();
            // Note: this removes some bindings.
            wrapperIf.init(elementName, bindingsHashtable);
            wrapperIf.conditionBlock().setContentElement(this);
            bindableElement = wrapperIf;
        }
        else {
            bindableElement = this;
        }
        this.init(elementName, bindingsHashtable);
        setTemplateName(templateName);
        setLineNumber(lineNumber);
        return bindableElement;
    }

    private boolean hasListBindings(Map bindingsHashtable)
    {
        return bindingsHashtable.get(AWBindableElement._list) != null;
    }

    private boolean hasIfBindings (Map bindingsHashtable)
    {
        return bindingsHashtable.get(AWBindableElement._ifTrue) != null
            || bindingsHashtable.get(AWBindableElement._ifFalse) != null
            || bindingsHashtable.get(AWBindableElement._equalNull) != null
            || bindingsHashtable.get(AWBindableElement._notEqualNull) != null;
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        this.init();
        _tagName = AWEncodedString.sharedEncodedString(tagName.intern());
        if (bindingsHashtable != null && bindingsHashtable.size() > 0) {
            Log.aribawebvalidation_bindableElement.debug("%s: Some bindings were not removed: %s", tagName, bindingsHashtable);
        }
    }

    public String tagName ()
    {
        return (_tagName == null) ? null : _tagName.string();
    }

    protected AWEncodedString encodedTagName ()
    {
        return _tagName;
    }

    public AWBinding[] allBindings ()
    {
        List bindingVector = ListUtil.list();
        allBindings(bindingVector);
        AWBinding[] bindings = new AWBinding[bindingVector.size()];
        bindingVector.toArray(bindings);
        return bindings;
    }

    private void allBindings (List bindingVector)
    {
        Class classObject = getClass();

        while (classObject != AWBindableElement.class) {
            Field[] fields = classObject.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if ((!Modifier.isStatic(field.getModifiers())) &&
                    AWBinding.class.isAssignableFrom(field.getType())) {
                    try {
                        AWBinding binding = (AWBinding)getFieldValue(field);
                        AWUtil.addElement(bindingVector, binding);
                    }
                    catch (IllegalArgumentException ex) {
                        logString("Error: " + getClass().getName() + ":getFieldValue" +
                                  ex.toString());

                    }
                    catch (IllegalAccessException ex) {
                        logString("Error: " + getClass().getName() + ":getFieldValue" +
                                  ex.toString());
                    }
                }
            }
            classObject = classObject.getSuperclass();
        }
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
        throw new IllegalAccessException();
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        validate(validationContext, component, getClass());
    }

    private void validate (AWValidationContext validationContext, AWComponent component, Class bindableClass)
    {
        // Iterate through all fields and, for those that are AWBinding type, call validate on them.
        Field[] declaredFields = bindableClass.getDeclaredFields();
        Class awbindingClass = AWBinding.class;
        for (int index = declaredFields.length - 1; index > -1; index--) {
            Field field = declaredFields[index];
            if (awbindingClass.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                try {
                    AWBinding binding = (AWBinding)field.get(this);
                    if (binding != null) {
                        // Note: Ignores direction for now.
                        binding.validate(validationContext, component, AWBindingApi.Either);
                    }
                }
                catch (IllegalAccessException illegalAccessException) {
                    Log.logStack(Log.aribaweb,
                                 Fmt.S("Warning: Unable to validate binding: %s",
                                       field.getName()));
                }
            }
        }
        Class superclass = bindableClass.getSuperclass();
        if (AWBindable.class.isAssignableFrom(superclass)) {
            validate(validationContext, component, superclass);
        }
    }

    public void appendTo (StringBuffer buffer)
    {
        buffer.append("<").append(tagName());

        AWBinding[] bindings = allBindings();
        for (int i=0; i<bindings.length; i++) {
            AWBinding binding = bindings[i];
            buffer.append(" ").append(binding.bindingName());
            buffer.append("=\"").append(binding.bindingDescription()).append("\"");
        }
        closeString(buffer);
    }

    protected void closeString (StringBuffer buffer)
    {
        buffer.append("/>");
    }
}

final class AWWrapperFor extends AWFor
{
    public void init (String tagName, Map bindingsHashtable)
    {
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.list, bindingsHashtable.remove(AWBindableElement._list));
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.item, bindingsHashtable.remove(AWBindableElement._item));
        super.init(tagName, bindingsHashtable);
    }

    public void add (AWElement element)
    {
        ((AWElementContaining)contentElement()).add(element);
    }
}

final class AWWrapperIf extends AWIf
{
    public void init (String tagName, Map bindingsHashtable)
    {
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.ifTrue, bindingsHashtable.remove(AWBindableElement._ifTrue));
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.ifFalse, bindingsHashtable.remove(AWBindableElement._ifFalse));
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.equalNull, bindingsHashtable.remove(AWBindableElement._equalNull));
        AWUtil.putNonNull(bindingsHashtable, AWBindingNames.notEqualNull, bindingsHashtable.remove(AWBindableElement._notEqualNull));
        super.init(tagName, bindingsHashtable);
    }

    protected AWContainerElement conditionBlock ()
    {
        return conditionBlocks()[0];
    }

    public void add (AWElement element)
    {
        ((AWElementContaining)conditionBlock().contentElement()).add(element);
    }
}
