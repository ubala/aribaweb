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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTRowDetailRenderer.java#17 $
*/
package ariba.ui.table;

public final class AWTRowDetailRenderer extends AWTDataTable.ColumnRenderer
{
    boolean _renderingOnTop;

    // todo: subclass from AWTColumn to match structure on element side

    private AWTRowDetail rowDetail ()
    {
        return (AWTRowDetail)column();
    }

    public Object cc_style ()
    {
        Object style = bindingValue(rowDetail()._styleBinding);
        if (style == null) {
            style = "padding:4px";
        }
        return style;
    }

    public String tdClass ()
    {
        AWTRowDetail column = (AWTRowDetail)column();
        return _table.tdClass(!(booleanBindingValue(column._showRowLine, false) || _renderingOnTop));
    }

    public boolean hasLeadingCols ()
    {
        return _table.colsBeforeData() > 0;
    }

    public int visibleLeadingCols ()
    {
        return _table.colsBeforeData() - (_table.hasInvisibleSelectionColumn() ? 1 : 0);
    }
    
    public boolean showRow ()
    {
        AWTRowDetail column = (AWTRowDetail)column();
        if ((column.contentElement()!=null) && booleanBindingValue(column._isVisibleBinding, true)) {
            _renderingOnTop = (booleanBindingValue(column._renderBeforeRow, false));
            if (!booleanBindingValue(column._showRowLine, _renderingOnTop)) _table.skipTopLine();
            return true;
        }
        return false;
    }

    public boolean nestedTableLayout ()
    {
        AWTRowDetail column = (AWTRowDetail)column();
        return booleanBindingValue(column._nestedTableLayout, false);
    }
}
