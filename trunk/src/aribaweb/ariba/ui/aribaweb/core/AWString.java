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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWString.java#18 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;
import ariba.util.core.StringUtil;

import java.lang.reflect.Field;

// subclassed by catalog/admin/client/apps/buyer/catalog/ACCPaddedString
public class AWString extends AWPrimitiveString
{
    protected static final String StandAloneStringTag = "StandAloneString";

    private AWBinding _formatter;
    private AWBinding _escapeHtmlBinding = null;
    private AWBinding _useBrBinding = null;
    private AWBinding _useNbspBinding = null;
    private AWBinding _escapeUnsafeHtmlBinding = null;
    private boolean _escapeHtml = false;
    private boolean _useBr = false;
    private boolean _useNbsp = false;
    private boolean _escapeUnsafeHtml = false;

    /*
        Escaping - AWString has several options on how its value string is escaped
        as it is rendered for HTML.  The bindings that control the escaping are
        escapeHtml & escapeUnsafeHtml.
        If escapeUnsafeHtml is present and true then we will escape all unsafe html
        tags in the string, leaving the safe tags (such as bold & italic) in the
        resulting Html output.
        Otherwise we consider escapeHtml.  If it is missing or present and true then
        we will escape all Html tags in the string.
        The last case is that escapeUnsafeHtml is missing or set false and escapeHtml
        is present and set false.  In this case no escaping is done.  Don't do this
        unless you absolutely know what you are doing because it is a security hole.
    */

    // ** Thread Safety Considerations: This is shared but ivar is immutable.

    public void init (String tagName, Map bindingsHashtable)
    {
        _formatter = (AWBinding)bindingsHashtable.remove(AWBindingNames.formatter);
        AWBinding escapeHtmlBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.escapeHtml);
        if (escapeHtmlBinding == null) {
            _escapeHtml = true;
        }
        else {
            if (escapeHtmlBinding.isConstantValue()) {
                _escapeHtml = AWIf.evaluateConditionInComponent(escapeHtmlBinding, null, false);
            }
            else {
                _escapeHtmlBinding = escapeHtmlBinding;
            }
        }
        AWBinding escapeUnsafeHtmlBinding = (AWBinding)bindingsHashtable.remove("escapeUnsafeHtml");
        if (escapeUnsafeHtmlBinding != null) {
            if (escapeUnsafeHtmlBinding.isConstantValue()) {
                _escapeUnsafeHtml = AWIf.evaluateConditionInComponent(escapeUnsafeHtmlBinding, null, false);
            }
            else {
                _escapeUnsafeHtmlBinding = escapeUnsafeHtmlBinding;
            }
        }
        AWBinding useBrBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.useBR);
        if (useBrBinding != null) {
            if (useBrBinding.isConstantValue()) {
                _useBr = useBrBinding.booleanValue(null);
            }
            else {
                _useBrBinding = useBrBinding;
            }
        }
        AWBinding useNbspBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.useNbsp);
        if (useNbspBinding != null) {
            if (useNbspBinding.isConstantValue()) {
                _useNbsp = useNbspBinding.booleanValue(null);
            }
            else {
                _useNbspBinding = useNbspBinding;
            }
        }
        super.init(tagName, bindingsHashtable);
    }

    public AWBinding _formatter ()
    {
        return _formatter;
    }

    public AWBinding _escapeHtml ()
    {
        return _escapeHtmlBinding;
    }

    public AWBinding _escapeUnsafeHtml ()
    {
        return _escapeUnsafeHtmlBinding;
    }

    public boolean _doEscapeHtml ()
    {
        return _escapeHtml;
    }

    public boolean _doEscapeUnsafeHtml ()
    {
        return _escapeUnsafeHtml;
    }

    protected AWEncodedString stringValueForObjectInComponent (Object objectValue, AWComponent component)
    {
        AWEncodedString encodedString = null;
        if (_formatter != null) {
            Object formatter = _formatter.value(component);
            if (formatter != null) {
                objectValue = AWFormatting.get(formatter).format(formatter, objectValue);
            }
        }
        boolean unsafeOnly = _escapeUnsafeHtmlBinding == null ? _escapeUnsafeHtml :
                                                                _escapeUnsafeHtmlBinding.booleanValue(component);
        boolean doEscapeHtml = unsafeOnly ? true :
                               ((_escapeHtmlBinding == null) ? _escapeHtml : _escapeHtmlBinding.booleanValue(component));
        if (unsafeOnly) {
            encodedString = component.escapeUnsafeString(objectValue);
        }
        else if (doEscapeHtml) {
            encodedString = component.escapeString(objectValue);
        }
        else if (objectValue instanceof AWEncodedString) {
            encodedString = (AWEncodedString)objectValue;
        }
        else {
            String stringValue = (objectValue instanceof String) ? (String)objectValue : AWUtil.toString(objectValue);
            encodedString = AWEncodedString.sharedEncodedString(stringValue);
        }
        boolean useBR = (_useBrBinding == null) ? _useBr : _useBrBinding.booleanValue(component);
        boolean useNbsp = (_useNbspBinding == null) ? _useNbsp : _useNbspBinding.booleanValue(component);

        if (useBR || useNbsp) {
            String string = encodedString.string();
            if (useBR) {
                string = StringUtil.replaceCharByString(string, '\n', "<br/>");
            }
            if (useNbsp) {
                string = StringUtil.replaceCharByString(string, ' ', "&nbsp;");
            }
            encodedString = new AWEncodedString(string);
        }
        return encodedString;
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

    public void appendTo (StringBuffer buffer)
    {
        if (StandAloneStringTag.equals(tagName())) {
            buffer.append(_value().bindingDescription());
        }
        else {
            super.appendTo(buffer);
        }
    }
}
