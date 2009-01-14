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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTRowAttributeLabelColumnRenderer.java#6 $
*/
package ariba.ui.table;

import ariba.util.core.StringUtil;

public final class AWTRowAttributeLabelColumnRenderer extends AWTDataTable.ColumnRenderer
{
    static final String CHECKED_NULL = "_NULL";

    public String _label;

    public void sleep ()
    {
        _label = null;
        super.sleep();
    }

    public String label ()
    {
        if (_label == null) {
            AWTDataTable.Column column = _table._pivotState._rowOverrideColumn;
            column.prepare(_table);
            _label = column.label(_table);
            if (_label == null) _label = CHECKED_NULL;
        }
        return _label == CHECKED_NULL ? null : _label;
    }

    public boolean renderLabel ()
    {
        return !StringUtil.nullOrEmptyOrBlankString(label());
    }

    public String indentationStyle ()
    {
        return _table.emitColumnStyle(null);
    }
}
