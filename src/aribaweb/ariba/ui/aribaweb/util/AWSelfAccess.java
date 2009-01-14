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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWSelfAccess.java#1 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

import java.util.Locale;

// These exist to add keys to Object for use by class extension:  "self", "displayString"
public class AWSelfAccess extends ClassExtension
{
    private static final ClassExtensionRegistry ClassExtensionRegistry = new ClassExtensionRegistry();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

    static {
        registerClassExtension(Object.class, new AWSelfAccess());
    }

    public static void registerClassExtension (Class receiverClass, AWSelfAccess extension)
    {
        ClassExtensionRegistry.registerClassExtension(receiverClass, extension);
    }

    public static AWSelfAccess get (Object target)
    {
        return (AWSelfAccess)ClassExtensionRegistry.get(target.getClass());
    }

    ////////////
    // Api
    ////////////
    public Object self (Object receiver)
    {
        return receiver;
    }
}
