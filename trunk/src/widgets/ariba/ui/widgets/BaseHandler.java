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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BaseHandler.java#14 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.MultiKeyHashtable;

/**
    @aribaapi private
*/
abstract public class BaseHandler implements Cloneable
{
    private static String DefaultHandlerName = "";
    private static BaseHandler NotFoundHandler = new BaseHandler() {};
        // Maps HandlerBaseClass/name to Class
    private static final MultiKeyHashtable handlerImpls = new MultiKeyHashtable(2);

    private String _name;

    public String name ()
    {
        return _name;
    }

    public Object clone ()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new Error("CloneNotSupported thrown in super - should never happen");
        }
    }

    public static void setDefaultHandler (Class handlerClass, BaseHandler handler)
    {
        setHandler(DefaultHandlerName, handlerClass, handler);
    }

    public static void setHandler (String name, Class handlerClass, BaseHandler handler)
    {
        prepareHandler(handler, name);
        handlerImpls.put(handlerClass, name, handler);
    }

    public static BaseHandler resolveHandler (String name, Class handlerClass)
    {
        BaseHandler handler;

        synchronized (handlerImpls) {
            handler = (BaseHandler)handlerImpls.get(handlerClass, name);
            if (handler == null) {
                // Use the default if there is one
                handler = (BaseHandler)handlerImpls.get(handlerClass, DefaultHandlerName);
                if (handler == null) {
                    // Use this class as a not found marker.
                    handler = NotFoundHandler;
                }
                else {
                    handler = (BaseHandler)handler.clone();
                }
                setHandler(name, handlerClass, handler);
            }
        }
        return handler == NotFoundHandler ? null : handler;
    }

    public static BaseHandler resolveHandler (AWComponent component, String name, Class handlerClass)
    {
        component = null;
        return resolveHandler(name, handlerClass);
    }

    protected static void prepareHandler (BaseHandler handler, String name)
    {
        if (handler != null && handler != NotFoundHandler) {
            handler._name = name;
        }
    }
}
