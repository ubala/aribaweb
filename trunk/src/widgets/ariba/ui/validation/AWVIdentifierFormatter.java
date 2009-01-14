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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/AWVIdentifierFormatter.java#2 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.util.AWFormatter;
import ariba.util.core.Assert;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.FastStringBuffer;

import java.util.Locale;
import java.text.ParseException;

public class AWVIdentifierFormatter extends AWFormatter
{
    private final Locale _locale;
    GrowOnlyHashtable _labelsForIdentifier;

    public AWVIdentifierFormatter(Locale locale)
    {
        _locale = locale;
        _labelsForIdentifier = new GrowOnlyHashtable();
        Assert.that(_locale != null, "locale must not be null");
    }

    public Object parseObject (String stringToParse) throws ParseException
    {
        Assert.that(false, "parseObject not supported -- output-only formatter");
        return null;
    }

    public String format (Object objectToFormat)
    {
        String label = (String) _labelsForIdentifier.get(objectToFormat);
        if (label != null) return label;

        label = decamelize(objectToFormat.toString());
        _labelsForIdentifier.put(objectToFormat, label);

        return label;
    }

    final static GrowOnlyHashtable _LabelForIdentifier = new GrowOnlyHashtable();
    public static String decamelize (String string)
    {
        String result = (String)_LabelForIdentifier.get(string);
        if (result == null) {
            boolean allCaps = true;
            FastStringBuffer buf = new FastStringBuffer();
            int lastUCIndex = -1;
            for (int i=0, len = string.length(); i < len; i++) {
                char c = string.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i-1 != lastUCIndex) buf.append(' ');
                    lastUCIndex = i;
                }
                else if (Character.isLowerCase(c)) {
                    if (i==0) c = Character.toUpperCase(c);
                    allCaps = false;
                }
                else if (c == '_') {
                    c = ' ';
                }
                buf.append(c);
            }

            // do mixed (initial word) case for all-caps strings
            if (allCaps) {
                boolean inWord = false;
                for (int i=0, c=buf.length(); i < c; i++) {
                    char ch = buf.charAt(i);
                    if (Character.isLetter(ch)) {
                        if (inWord && Character.isUpperCase(ch)) {
                            buf.setCharAt(i, Character.toLowerCase(ch));
                        }
                        inWord = true;
                    } else {
                        inWord = false;
                    }
                }
            }

            result = buf.toString();
            _LabelForIdentifier.put(string, result);
        }
        return result;
    }

}
