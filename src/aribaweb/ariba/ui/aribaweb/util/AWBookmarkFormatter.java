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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWBookmarkFormatter.java#2 $
*/

package ariba.ui.aribaweb.util;

import java.text.ParseException;

import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.formatter.Formatter;

public abstract class AWBookmarkFormatter extends ClassExtension
{
    private static final ClassExtensionRegistry ClassExtensionRegistry = new ClassExtensionRegistry();

    public static void registerClassExtension (Class<?> type,
                                               AWBookmarkFormatter formatter)
    {
        ClassExtensionRegistry.registerClassExtension(type, formatter);
    }

    public abstract String formatObject (Object val);
    public abstract Object parseString (String val);

    public static String format (Object val)
    {
        AWBookmarkFormatter bformatter = (AWBookmarkFormatter)ClassExtensionRegistry.get(val.getClass());
        if (bformatter != null) {
            return bformatter.formatObject(val);
        }

        return Formatter.getFormatterForObject(val).getFormat(val);
    }

    public static Object parse (String val, Class<?> type) throws ParseException
    {
        AWBookmarkFormatter bformatter = (AWBookmarkFormatter)ClassExtensionRegistry.get(type);
        if (bformatter != null) {
            return bformatter.parseString(val);
        }
        return Formatter.parseString(val,type.getName());
    }
}
