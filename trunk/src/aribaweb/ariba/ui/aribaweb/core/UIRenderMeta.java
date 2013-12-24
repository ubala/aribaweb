/*
    Copyright (c) 1996-2013 Ariba, Inc.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/UIRenderMeta.java#2 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

/**
 @aribaapi ariba
 */
public abstract class UIRenderMeta extends ClassExtension
{
    protected static final ClassExtensionRegistry
            ExtensionRegistry = new ClassExtensionRegistry();

    /*
    used to indicate which rendering version the system should use.
    the default is AW5 and AW6 is considered next gen
    unspecified will default to the value in the session
    */
    public enum RenderVersion { AW5, AW6, NoStyle, Unspecified }

    /**
     Put a ClassExtension implementation of the interface into the cache.
     @param targetObjectClass The root class for which the classExtension
                              applies.
     */
    public static void registerClassExtension (
            Class targetObjectClass, UIRenderMeta uiRenderMeta)
    {
        ExtensionRegistry.registerClassExtension(
                targetObjectClass, uiRenderMeta);
    }

    /**
     Retrieve a ClassExtension registered by registerClassExtension(...).
     Note that this will clone the ClassExtension objects which are
     registered so that each subclass will have its own classExtension
     implementation.  See ClassExtensionRegistry for details on this.

     @param targetClass the class for which a classExtension applies
     @return the classExtension which applies for the UIMeta
     */
    public static UIRenderMeta get (Class targetClass)
    {
        return (UIRenderMeta)ExtensionRegistry.get(targetClass);
    }

    public static UIRenderMeta get (Object target)
    {
        return get(target.getClass());
    }

    /**
     * Override this in your particular UIMeta implementation.
     * @return The rendering version.
     */
    public RenderVersion getRenderVersion (Object businessObject)
    {
        return RenderVersion.AW5;
    }
}
