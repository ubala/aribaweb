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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/WidgetsSessionState.java#11 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import javax.servlet.http.HttpSession;

public final class WidgetsSessionState
{
    private boolean _sidebarVisible = true;
    private boolean _hasSidebarNotch = true;
    private boolean _disabledLogoutAction = false;
    private boolean _disabledHomeAction = false;
    private Brand _brand;
    private ActionInterceptor _dialogActionInterceptor;

    public static WidgetsSessionState get (AWComponent component)
    {
        return get(component.httpSession());
    }

    public static WidgetsSessionState get (AWRequestContext requestContext)
    {
        return get(requestContext.httpSession());
    }

    public static WidgetsSessionState get (HttpSession httpSession)
    {
        String key = WidgetsSessionState.class.getName();

        WidgetsSessionState state = (WidgetsSessionState)httpSession.getAttribute(key);
        if (state == null) {
            state = new WidgetsSessionState();
            httpSession.setAttribute(key, state);
        }
        return state;
    }

    public void setBrand (Brand brand)
    {
        _brand = brand;
    }

    public Brand brand ()
    {
        return _brand;
    }

    public void setSidebarVisible (boolean flag)
    {
        _sidebarVisible = flag;
    }

    public boolean isSidebarVisible ()
    {
        return _sidebarVisible;
    }

    public void setHasSidebarNotch (boolean flag)
    {
        _hasSidebarNotch = flag;
    }

    public boolean hasSidebarNotch ()
    {
        return _hasSidebarNotch;
    }

    public boolean disabledLogoutAction ()
    {
        return _disabledLogoutAction;
    }

    public void setDisabledLogoutAction (boolean flag)
    {
        _disabledLogoutAction = flag;
    }

    public boolean disabledHomeAction ()
    {
        return _disabledHomeAction;
    }

    public void setDisabledHomeAction (boolean flag)
    {
        _disabledHomeAction = flag;
    }

    public void setDialogActionInterceptor (ActionInterceptor interceptor)
    {
        _dialogActionInterceptor = interceptor;
    }

    public ActionInterceptor dialogActionInterceptor ()
    {
        return _dialogActionInterceptor;
    }
}
