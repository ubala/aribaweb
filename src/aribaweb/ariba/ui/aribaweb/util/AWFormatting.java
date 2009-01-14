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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFormatting.java#5 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;

import java.util.Locale;

abstract public class AWFormatting extends ClassExtension
{
    private static final ClassExtensionRegistry ClassExtensionRegistry = new ClassExtensionRegistry();

    // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

    static {
        registerClassExtension(java.text.Format.class, new AWFormatting_JavaFormat());
        registerClassExtension(ariba.util.formatter.Formatter.class, new AWFormatting_AribaFormatter());
        registerClassExtension(AWFormatter.class, new AWFormatting_AWFormatter());
    }

    public static void registerClassExtension (Class receiverClass, AWFormatting formatClassExtension)
    {
        ClassExtensionRegistry.registerClassExtension(receiverClass, formatClassExtension);
    }

    public static AWFormatting get (Object target)
    {
        return (AWFormatting)ClassExtensionRegistry.get(target.getClass());
    }

    ////////////
    // Api
    ////////////
    abstract public Object parseObject (Object receiver, String stringToParse);
    abstract public String format (Object receiver, Object objectToFormat);

    public Object parseObject (Object receiver, String stringToParse, Locale locale)
    {
        return parseObject(receiver, stringToParse);
    }

    public String format (Object receiver, Object objectToFormat, Locale locale)
    {
        return format(receiver, objectToFormat);
    }
}
