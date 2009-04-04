package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.util.AWFormatting;
import ariba.ui.widgets.ChooserSelectionSource;
import ariba.ui.widgets.BindingNames;
import ariba.ui.widgets.ChooserState;
import ariba.ui.widgets.ChooserSelectionState;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.OrderedList;
import ariba.util.fieldvalue.RelationshipField;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.Compare;
import ariba.util.formatter.Formatter;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.text.ParseException;

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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/GenericChooser.java#7 $
*/
public class GenericChooser extends AWComponent implements ChooserSelectionState
{
    enum ChooserType { Radio, Checkbox, Popup, PopupControl, Chooser }

    public Object _item;
    public List _list;
    public ChooserSelectionSource _chooserSource;
    public ChooserState _chooserState;
    public ChooserType _type;
    public boolean _isMulti;
    public String _displayKey;
    public Object /*AWFormatting*/ _formatter;
    public Object /*AWFormatting*/ _chooserFormatter;
    Object _object;
    FieldPath _keyPath;

    public boolean isStateless ()
    {
        return false;
    }

    public void init()
    {
        super.init();
        _keyPath = FieldPath.sharedFieldPath(stringValueForBinding("key"));
        _isMulti = booleanValueForBinding(BindingNames.multiSelect);
        _displayKey = stringValueForBinding("displayKey");
        if (_displayKey == null) _displayKey = "self";
        _formatter = valueForBinding(AWBindingNames.formatter);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _object = valueForBinding("object");
        if (_object == null) _object = parent();

        AWBinding listBinding = bindingForName(AWBindingNames.list);
        Object choiceSource;
        if (listBinding != null) {
            _list = (List)valueForBinding(listBinding);
            if (_list == null) _list = ListUtil.list();
            choiceSource = _list;
        }
        else {
            String className = stringValueForBinding("destinationClass");
            Assert.that(className != null, "Chooser must have non-null binding for list or destinationClass");
            Map choiceParams = (Map)valueForBinding("choiceProviderParams");
            choiceSource = ChoiceSourceRegistry.choiceSourceForClass(className, choiceParams);
        }

        String type = stringValueForBinding(AWBindingNames.type);
        if (type != null) {
            _type = ChooserType.valueOf(type);
        }
        else {
            int projectedSize = ChoiceSource.get(choiceSource).expectedCount(choiceSource);
            if (_isMulti) {
                _type = (projectedSize <= 0 || projectedSize > 6)
                        ? ChooserType.Chooser : ChooserType.Checkbox;
            } else {
                _type = (projectedSize <= 0 || projectedSize > 20)
                        ? ChooserType.Chooser
                        : (projectedSize < 6) ? ChooserType.Radio : ChooserType.Popup;
            }
        }


        if (_type == ChooserType.Chooser) {
            if  (_chooserSource == null) {
                _chooserSource = ChoiceSource.get(choiceSource).chooserSelectionSource(choiceSource, _displayKey);
                _chooserState = new ChooserState(this);
                _chooserState.setMultiSelect(_isMulti);
                _chooserFormatter = new FieldPathFormatter(_displayKey, _formatter);
            }
        }
        else {
            _list = ChoiceSource.get(choiceSource).list(choiceSource);
        }

        super.renderResponse(requestContext, component);
    }

    /*
    public void sleep ()
    {
        _list = null;
        _item = null;
        _chooserSource = null;
        _chooserState = null;
        super.sleep();
    }
    */

    public Object displayValue ()
    {
        return FieldValue.getFieldValue(_item, _displayKey);
    }

    public Object selection ()
    {
        return _keyPath.getFieldValue(_object);
    }

    public void setSelection (Object value)
    {
        _keyPath.setFieldValue(_object, value);
    }


    // ChooserSelectionState
    public void setSelectionState (Object selection, boolean selected)
    {
        if (selected == isSelected(selection)) return;

        if (_isMulti) {
            if (selected) {
                RelationshipField.addTo(_object, _keyPath, selection);
            }
            else {
                RelationshipField.removeFrom(_object, _keyPath, selection);
            }
        }
        else {
            if (!selected) selection = null;
            setSelection(selection);
        }
    }

    public Object selectedObject ()
    {
        if (_isMulti) {
            List list = selectedObjects();
            return ListUtil.nullOrEmptyList(list) ? null : ListUtil.lastElement(list);
        }
        return selection();
    }

    public List selectedObjects ()
    {
        Object selection = selection();
        if (_isMulti && selection == null) {
            selection = Collections.EMPTY_LIST;
        }
        return (_isMulti) ? OrderedList.get(selection).toList(selection) : Arrays.asList(selection);
    }

    public boolean isSelected (Object selection)
    {
        if (_isMulti) return selectedObjects().contains(selection);
        Object cur = selectedObject();
        return (cur == selection)
                || ((cur != null) &&  selectedObject().equals(selection));
    }


    /*
    <Binding key="destinationClass" type="String" direction="get">
        Can be used in place of "list" to trigger obtaining the list
        of choices by was of the ChoiceSource class extension.
    </Binding>

    <Binding key="multiSelect" type="boolean" direction="get" default="$false">
        Is this a List property, or a to-one.
    </Binding>

    <Binding key="type" type="String" direction="get">
        The style of chooser to use (Radio, Checkbox, Popup, PopupControl, Chooser)
        Defaults based on cardinality of the list and whether it's multiSelect.
    </Binding>
    
     */

    public static class FieldPathFormatter extends Formatter
    {
        FieldPath _fieldPath;
        Object /*AWFormatting*/ _chainedFormatter;

        public FieldPathFormatter (String fieldPathString, Object /*AWFormatting*/ chainedFormatter)
        {
            _fieldPath = new FieldPath(fieldPathString);
            _chainedFormatter = chainedFormatter;
        }

        protected String formatObject(Object object, Locale locale) {
            Object value = _fieldPath.getFieldValue(object);
            if (value == null) return null;
            return (_chainedFormatter == null) ? value.toString()
                    : AWFormatting.get(_chainedFormatter).format(_chainedFormatter, value, locale);
        }

        protected Object parseString(String string, Locale locale) throws ParseException {
            Assert.that(false, "operation not supported");
            return null;
        }

        public Object getValue(Object object, Locale locale) {
            Object value = _fieldPath.getFieldValue(object);
            return (_chainedFormatter == null || !(_chainedFormatter instanceof Formatter))
                    ? value
                    : ((Formatter)_chainedFormatter).getValue(value, locale);
        }

        protected int compareObjects(Object o1, Object o2, Locale locale) {
            Object v1 = getValue(o1, locale), v2 = getValue(o2, locale);
            return (_chainedFormatter == null || !(_chainedFormatter instanceof Compare))
                    ? ((Comparable)v1).compareTo(v2)
                    : (_chainedFormatter instanceof Formatter)
                        ? ((Formatter)_chainedFormatter).compare(v1, v2, locale)
                        : ((Compare)_chainedFormatter).compare(v1, v2);
        }
    }
}
