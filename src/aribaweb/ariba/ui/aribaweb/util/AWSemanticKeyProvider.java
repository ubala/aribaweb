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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSemanticKeyProvider.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWAction;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

/**

Sample Usage:

    int index = AWOrderedList.get(someList).indexOf(someList, someObject);
*/
abstract public class AWSemanticKeyProvider extends ClassExtension
                                            implements SemanticKeyProvider
{
    private static ClassExtensionRegistry SKClassExtensionRegistry = new ClassExtensionRegistry();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.
    static {
        registerClassExtension(String.class, new AWSemanticKeyProvider_String());
        registerClassExtension(AWAction.class, new AWSemanticKeyProvider_AWAction());
    }

    /////////////////
    // ClassExtension Caching
    /////////////////
    public static void registerClassExtension (Class receiverClass, AWSemanticKeyProvider classExtension)
    {
        SKClassExtensionRegistry.registerClassExtension(receiverClass, classExtension);
    }

    public static SemanticKeyProvider get (Object receiver)
    {
        try {
            if (receiver instanceof SemanticKeyProvider) {
                return (SemanticKeyProvider)receiver;
            }
            else {
                return (SemanticKeyProvider)SKClassExtensionRegistry.get(receiver);
            }
        }
        catch(ClassCastException e) {
            return null;
        }
    }

    public static SemanticKeyProvider get (Class targetClass)
    {
        try {
            return (SemanticKeyProvider)SKClassExtensionRegistry.get(targetClass);
        }
        catch(ClassCastException e) {
            return null;
        }        
    }
}
