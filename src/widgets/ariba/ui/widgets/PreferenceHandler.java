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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PreferenceHandler.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

/**
 * Note: currently cannot share a single PreferenceHandler instance for multiple
 * preference keys due to current implementation of prepareHandler.
 * @aribaapi private
*/
public class PreferenceHandler extends BaseHandler
{
    private boolean _enabled = true;

    public static void setDefaultHandler (PreferenceHandler handler)
    {
        BaseHandler.setDefaultHandler(PreferenceHandler.class, handler);
    }

    public static void setHandler (String action, PreferenceHandler handler)
    {
        BaseHandler.setHandler(action, PreferenceHandler.class, handler);
    }

    public static PreferenceHandler resolveHandlerInComponent (String preference,
                                                               AWComponent component)
    {
        PreferenceHandler handler =
            (PreferenceHandler)resolveHandler(component, preference,
                                              PreferenceHandler.class);
        prepareHandler(handler,preference);
        return handler;
    }

    public static PreferenceHandler resolveHandler (String preference)
    {
        PreferenceHandler handler =
            (PreferenceHandler)resolveHandler(preference, PreferenceHandler.class);
        // ### todo --- NOT thread safe -- Handler is shared so can't register the
        // same handler instance for multiple preference names.
        prepareHandler(handler,preference);
        return handler;
    }

    public PreferenceHandler ()
    {
        this(true);
    }

    public PreferenceHandler (boolean isEnabled)
    {
        super();
        _enabled = isEnabled;
    }

    public boolean isEnabled (AWRequestContext requestContext)
    {
        return _enabled;
    }

    public void setBooleanPreference (AWRequestContext requestContext,
                                      String key, boolean flag)
    {
    }

    public boolean getBooleanPreference (AWRequestContext requestContext, String key)
    {
        return false;
    }
}
