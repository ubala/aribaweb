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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTButtonArea.java#7 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import java.util.Map;

public class AWTButtonArea extends AWTDataTable.Column
{
    public AWBinding      _isVisibleBinding;

    public void init (String tagName, Map bindingsHashtable)
    {
        _isVisibleBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.isVisible);
        super.init(tagName, bindingsHashtable);
    }

    boolean isVisible (AWTDataTable table)
    {
        return (_isVisibleBinding == null) ? true
                : table.booleanValueForBinding(_isVisibleBinding);
    }

    public String rendererComponentName ()
    {
        return AWTButtonAreaRenderer.class.getName();
    }

    public void initializeColumn (AWTDataTable table)
    {
        table.setGlobalButtonArea(this);
    }
}
