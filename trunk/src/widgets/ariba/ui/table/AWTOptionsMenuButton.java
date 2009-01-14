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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTOptionsMenuButton.java#8 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWComponent;

public final class AWTOptionsMenuButton extends AWComponent
{
    public Object _menuId;
    public boolean _generateMenu;

    protected void sleep ()
    {
        _menuId = null;
        _generateMenu = false;
    }

    public void computeMenuId ()
    {
        _generateMenu = false;
        _menuId = valueForBinding(ariba.ui.widgets.BindingNames.menuId);
        if (_menuId == null) {
            _menuId = requestContext().nextElementId();
            _generateMenu = true;
        }
    }
}
