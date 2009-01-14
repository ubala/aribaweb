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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWHiddenFormValue.java#2 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWHiddenFormValueHandler;
import ariba.ui.aribaweb.util.AWUtil;

public class AWHiddenFormValue extends AWComponent
{
    public static String ValueBinding = "value";

    private AWHiddenFormValueHandler _handler;

    protected void sleep ()
    {
        super.sleep();
        _handler = null;
    }

    private AWHiddenFormValueHandler getHandler ()
    {
        if (_handler == null) {
            _handler = (AWHiddenFormValueHandler)valueForBinding(ValueBinding);
        }
        return _handler;
    }

    public String formValue ()
    {
        AWHiddenFormValueHandler handler = getHandler();
        String value = (handler != null ? handler.getValue() : null);
        if (value != null) {
            value = AWUtil.escapeHtml(value).string();
        }
        return value;
    }

    public void setFormValue (String newString)
    {
        AWHiddenFormValueHandler handler = getHandler();
        if (handler != null) {
            handler.setValue(newString);
        }
    }

    public String getName ()
    {
        AWHiddenFormValueHandler handler = getHandler();
        return (handler != null ? handler.getName() : null);
    }
}
