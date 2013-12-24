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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTMetaColumnRenderer.java#18 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.table.AWTDataTable;
import ariba.ui.widgets.HTMLActions;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.Constants;
import ariba.util.core.StringUtil;

public final class AWTMetaColumnRenderer extends AWTDataTable.ColumnRenderer
{
    AWTMetaContentRenderer _groupView;
    Boolean _useDirectAction;

    public void sleep ()
    {
        _groupView = null;
        _useDirectAction = null;
        super.sleep();
    }

    public boolean useDirectAction ()
    {
        if (_useDirectAction == null) {
            _useDirectAction = Constants.getBoolean(thisColumn()._sessionless);
        }
        return _useDirectAction.booleanValue();
    }

    public AWTMetaColumn thisColumn ()
    {
        return (AWTMetaColumn)column();
    }

    public AWTMetaContentRenderer groupView ()
    {
        if (_groupView == null) {
            _groupView = (AWTMetaContentRenderer)env().peek("groupView");
        }
        return _groupView;
    }

    /**
     * Virtually all of the above should actually be implemented in terms of the GroupView!
     *
     * Or, not implemented at all -- we just switch in a Controller, and its bindings will be to
     * "$groupView.xyz" ...
     */

    public Object columnWidth ()
    {
        // min width for image columns
        return column().isValueColumn() ? null : "1px";
    }
    
    /** Evalators for the curentColumn's bindings */
    public Object cc_width ()
    {
        return null;
    }

    public Object cc_style ()
    {
        return StringUtil.strcat("padding:4px;", thisColumn().style());
    }

    public Object cc_nowrap ()
    {
        return null;
    }

    public String keyPathString ()
    {
        return thisColumn().keyPathString();
    }

    public Object cc_sortKey ()
    {
        return keyPathString();
    }

    public Object cc_label ()
    {
        return thisColumn().label(_table);
    }

    public Object cc_sortCaseSensitively ()
    {
        return null;
    }

    public Object cc_noSort ()
    {
        return null;
    }

    public Object disableSort ()
    {
        return (_table.renderToExcel()) ? Boolean.TRUE : cc_noSort();
    }

    public Object cc_formatter ()
    {
        return thisColumn().formatter();
    }

    protected String actionString ()
    {
        FieldPath actionFieldPath = thisColumn().actionFieldPath();
        String actionString = (String)actionFieldPath.getFieldValue(_table.displayGroup().currentItem());
        return actionString;
    }

    protected String actionTarget ()
    {
        FieldPath actionTargetFieldPath = thisColumn().actionTargetFieldPath();
        String actionTarget = (String)actionTargetFieldPath.getFieldValue(_table.displayGroup().currentItem());
        return actionTarget;
    }

    public boolean hasAction ()
    {
        return (actionString() != null) && (!_table.renderToExcel());
    }

    public String directActionURL ()
    {
        return HTMLActions.directActionURL(actionString(), requestContext());
    }

    public AWResponseGenerating actionClicked ()
    {
        // For XMLHTTP during file download
        AWRedirect.disallowInternalDispatch(requestContext());
        return HTMLActions.handleAction(actionString(), actionTarget(), _table);
    }

    public String columnVAlignment ()
    {
        return _table.globalVAlignment();
    }

    public Object cc_align ()
    {
        return thisColumn().align();
    }

    public void setValue (Object value)
    {
        thisColumn().fieldPath().setFieldValue(_table.displayGroup().currentItem(), value);
    }

    public Object value ()
    {
        Object value = thisColumn().fieldPath().getFieldValue(_table.displayGroup().currentItem());
        // Don't do the empty string replacement convenience if there's a formatter.  It should do that.
        return value;
    }

    public boolean isAbsolute ()
    {
        Object value = value();
        return (value instanceof String
                && !StringUtil.nullOrEmptyString((String)value)
                && ((String)value).indexOf(':') > 0);
    }
}
