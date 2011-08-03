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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWJavascriptString.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import java.util.Map;

/**
    A string class that prevents "bad" javascript from being included in the pages.

    @aribaapi
*/
final public class AWJavascriptString extends AWPrimitiveString
{
    // contains the list of characters/strings that can cause security problems
    // if they are included in urls set in javastring.

    // Adding "<" to the list of BadChars to prevent strings that contain
    // "<script>" or any other tag from bring executed in the browser.
    // Any strings that contain "<" will now be rejected. If there is a
    // need to use "<" in a string, for example to allow arithmatic operations
    // in string used in JavaScript, then "<" can be removed from the BadChars
    // list but then an addition check for tags needs to be added, like match
    // pattern : TagsPattern = Pattern.compile("<[^<]*>", Pattern.MULTILINE);
    // Now we added a check for "<" instead of the complete tag for simplicity.

    private final static String[] BadChars= {
        // in "string", "url encoded string" format
        "\"", "%22",
        "'", "%27",
        "<", "%3C",
        "(", "%28",
        ")", "%29"
    };

    private AWBinding _escape;

    // ** Thread Safety Considerations: This is shared but ivar is immutable.

    public void init (String tagName, Map bindingsHashtable)
    {
        super.init(tagName, bindingsHashtable);
        _escape = (AWBinding)bindingsHashtable.remove(AWBindingNames.escape);
    }

    protected AWEncodedString stringValueForObjectInComponent (Object objectValue,
                                                               AWComponent component)
    {
        AWEncodedString encodedString =
            super.stringValueForObjectInComponent(objectValue, component);

        Object escapeValue = _escape != null ? _escape.value(component) : null;
        boolean escape = escapeValue != null && ((Boolean)escapeValue).booleanValue();

        String stringValue = encodedString.string();
        if (!StringUtil.nullOrEmptyOrBlankString(stringValue)) {
            if (escape) {
                // escape single quote, double quote, and backslash
                stringValue = stringValue.replaceAll("('|\"|\\\\)", "\\\\$1");
                encodedString = AWEncodedString.sharedEncodedString(stringValue);
            }
            else if (shouldThrowException(component)) {
                validateStringValue(stringValue);
            }
            else {
                // encode the javascript before we included in the page.
                encodedString = new AWEncodedString("");
            }
        }
        return encodedString;
    }

    /**
         An AWGenericException is thrown if 'stringValue contains BadChars.
     */
    public static void validateStringValue (String stringValue)
    {
        for (int i=0; i<BadChars.length; i++) {
            if (stringValue.indexOf(BadChars[i]) != -1) {
                throw new AWGenericException(
                    Fmt.S("Illegal character or string '%s' included in javascript: %s",
                        BadChars[i], stringValue));
            }
        }
    }

    /**
        Specifed whether we should throw any exception during validation.
        The better fix would be to strip down the error page so it does not
        display any javascript.  In that case, we can validate all pages.
    */
    private boolean shouldThrowException (AWComponent component)
    {

        boolean throwException = true;

        // do not throw exception if we are displaying the error page.
        throwException = !(component.pageComponent() instanceof
            AWHandleExceptionPage);

        return throwException;
    }
}
