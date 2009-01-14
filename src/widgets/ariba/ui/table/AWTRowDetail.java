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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTRowDetail.java#14 $
*/
package ariba.ui.table;

import java.util.Map;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;

public final class AWTRowDetail extends AWTColumn implements AWTDataTable.DetailColumn
{
    public AWBinding      _isVisibleBinding;
    public AWBinding      _nestedTableLayout;
    public AWBinding      _showRowLine;
    public AWBinding      _renderBeforeRow;

    public void init (String tagName, Map bindingsHashtable)
    {
        _isVisibleBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.isVisible);
        _nestedTableLayout = (AWBinding)bindingsHashtable.remove(BindingNames.nestedTableLayout);
        _showRowLine = (AWBinding)bindingsHashtable.remove(BindingNames.showRowLine);
        _renderBeforeRow = (AWBinding)bindingsHashtable.remove(BindingNames.renderBeforeRow);
        super.init(tagName, bindingsHashtable);
    }

    public String rendererComponentName ()
    {
        return AWTRowDetailRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        table.setDetailColumn(this);
    }

    public boolean renderBeforeRow (AWTDataTable table)
    {
        return (_renderBeforeRow != null) && table.booleanValueForBinding(_renderBeforeRow);
    }
}
