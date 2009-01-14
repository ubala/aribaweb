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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTColumnRenderer.java#21 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.StringUtil;

public final class AWTColumnRenderer extends AWTDataTable.ColumnRenderer
{
    private AWTColumn thisColumn ()
    {
        return (AWTColumn)column();
    }

    public void prepare ()
    {
        thisColumn().prepare(_table);
    }

    /** Evalators for the curentColumn's bindings */
    public Object columnWidth ()
    {
        AWTColumn column = thisColumn();
        return bindingValue(column._widthBinding);
    }

    public Object cc_style ()
    {
        return _table.emitColumnStyle((String)bindingValue(thisColumn()._styleBinding));
    }

    public Object cc_class ()
    {
        // if there is a class binidng directly on the columnRenderer (ie, from the
        // datatable), then use it
        String classString = stringValueForBinding(AWBindingNames.classBinding);
        if (StringUtil.nullOrEmptyOrBlankString(classString)) {
            // otherwise check if the column has a class binding
            classString = (String)bindingValue(thisColumn()._classBinding);
            if (!StringUtil.nullOrEmptyOrBlankString(classString)) {
                return StringUtil.strcat(classString, " ", _table.tdClass());
            }
        }
        return classString;
    }

    public Object cc_nowrap ()
    {
        return bindingValue(thisColumn()._nowrapBinding);
    }

    public String keyPathString ()
    {
        FieldPath path = thisColumn()._keyPath;
        return (path == null) ? null : path.fieldPathString();
    }

    public Object cc_sortKey ()
    {
        String key = (String)bindingValue(thisColumn()._sortKeyBinding);
        if (key == null) {
            key = keyPathString();
        }
        return key;
    }

    public AWTSortOrdering cc_sortOrdering ()
    {
        return thisColumn().createSortOrdering(_table);
    }

    public Object cc_label ()
    {
        return thisColumn().label(_table);
    }

    public Object cc_sortCaseSensitively ()
    {
        return bindingValue(thisColumn()._sortCaseSensitivelyBinding);
    }

    public Object cc_noSort ()
    {
        Object noSort = bindingValue(thisColumn()._noSortBinding);
        if (noSort == null) {
            noSort = (cc_sortKey() == null) ? Boolean.TRUE : Boolean.FALSE;
        }
        return noSort;
    }

    public Object disableSort ()
    {
        return (_table.renderToExcel()) ? Boolean.TRUE : cc_noSort();
    }

    public Object cc_action ()
    {
        return bindingValue(thisColumn()._actionBinding);
    }

    public Object cc_formatter ()
    {
        return bindingValue(thisColumn()._formatterBinding);
    }

    public boolean omitAction ()
    {
        return (thisColumn()._actionBinding == null) || (_table.renderToExcel());
    }

    public String columnVAlignment ()
    {
        AWBinding binding = thisColumn()._vAlignmentBinding;
        String alignment = (binding == null) ? null : binding.stringValue(parent());
        return (alignment!=null) ? alignment : _table.globalVAlignment();
    }

    public Object cc_align ()
    {
        return bindingValue(thisColumn()._alignBinding);
    }

    public void setValue (Object value)
    {
        thisColumn()._keyPath.setFieldValue(_table.displayGroup().currentItem(), value);
    }

    public boolean isBlank ()
    {
        return booleanBindingValue(thisColumn()._isBlankBinding, false) ||
               thisColumn()._keyPath == null;
    }

    public boolean showColumnHeader ()
    {
        return thisColumn().showColumnLabel(_table);
    }

    public Object value ()
    {
        FieldPath keyPath = thisColumn()._keyPath;
        Object value = (keyPath != null) ? keyPath.getFieldValue(_table.displayGroup().currentItem()): null;

        // Don't do the empty string replacement convenience if there's a formatter.  It should do that.
        return (value == null && (bindingValue(thisColumn()._formatterBinding) == null))
                    ? "" : value;
    }
}
