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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVFormatterAccess.java#5 $
*/

package ariba.ui.validation;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.ui.aribaweb.core.AWComponent;

/**
     * Class Extension to add "formatters" key to AWComponent.
     * (so templates can do bindings like "$formatters.dateFormat").
     */
public abstract class AWVFormatterAccess extends ClassExtension
{
    abstract public Object formatters (Object target);


    public static AWVFormatterAccess get (Object target)
    {
        return (AWVFormatterAccess)_ClassExtensionRegistry.get(target);
    }

    public static void init () {
        _ClassExtensionRegistry.registerClassExtension(AWComponent.class,
                                          new AWVFormatterAccess_AWComponent());
    }

    /**
        Register the known subclasses.
    */
    protected static final ClassExtensionRegistry
        _ClassExtensionRegistry = new ClassExtensionRegistry();

    /**
     * Instance for AWComponent
     */
    public static class AWVFormatterAccess_AWComponent extends AWVFormatterAccess
    {
        public Object formatters (Object target)
        {
            return AWVFormatterFactory.formattersForComponent((AWComponent)target);
        }
    }
}

