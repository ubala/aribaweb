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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWHiddenFormValueManager.java#2 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;

/**
    Each page has a hidden form value manager.  Components
    that need to send values to/from the client side can use
    this mechanism by registering a handler with this manager.
    The registering component does not need to add any INPUT
    elements in the component.

    @aribaapi
*/
public class AWHiddenFormValueManager
{
    private List _hiddenValues = ListUtil.list();

    public AWHiddenFormValueManager ()
    {
    }

    public void registerHiddenFormValue (AWHiddenFormValueHandler handler)
    {
        Assert.that(handler.getName() != null,
            "AWHiddenFormValueHandler.getName() cannot return null");
        for (int i = 0; i < _hiddenValues.size(); i++) {
            AWHiddenFormValueHandler curHandler =
                (AWHiddenFormValueHandler)_hiddenValues.get(i);
            if (handler.getName().equals(curHandler.getName())) {
                // replace the existing one
                _hiddenValues.set(i, handler);
                return;
            }
        }
        _hiddenValues.add(handler);
    }

    public void removeHiddenFormValue (String name)
    {
        for (int i = 0; i < _hiddenValues.size(); i++) {
            AWHiddenFormValueHandler curHandler =
                (AWHiddenFormValueHandler)_hiddenValues.get(i);
            if (name.equals(curHandler.getName())) {
                _hiddenValues.remove(i);
            }
        }
    }

    public List getHiddenValueHandlers ()
    {
        return _hiddenValues;
    }
}
