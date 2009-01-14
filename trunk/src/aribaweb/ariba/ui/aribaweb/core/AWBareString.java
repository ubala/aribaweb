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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBareString.java#10 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.StringUtil;

public final class AWBareString extends AWBaseElement
{
    private static GrowOnlyHashtable WhitespaceBareStrings = new GrowOnlyHashtable();
    private AWEncodedString _encodedString;

    // ** Thread Safety Considerations:  Although instances of this class are shared by many threads, no locking is required because, once the parsing is complete and the _bytes is established, it is immutable.

    public static AWBareString getInstance (String string)
    {
        AWBareString bareString = null;
        if (AWUtil.isWhitespace(string)) {
            bareString = (AWBareString)WhitespaceBareStrings.get(string);
            if (bareString == null) {
                string = string.intern();
                bareString = new AWBareString();
                bareString.init(string);
                WhitespaceBareStrings.put(string, bareString);
            }
        }
        else {
            bareString = new AWBareString();
            bareString.init(string);
        }
        return bareString;
    }

    public void init (String stringValue)
    {
        this.init();
        _encodedString = AWEncodedString.sharedEncodedString(stringValue);
        if (stringValue.length() < 1) {
            debugString("*** efficiency warning: AWBareString length of 0. Please report this ***");
        }
    }

    public void init (StringBuffer stringBuffer)
    {
        this.init(stringBuffer.toString());
    }

    public String string ()
    {
        return _encodedString.string();
    }

    protected AWEncodedString encodedString ()
    {
        return _encodedString;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        requestContext.response().appendContent(_encodedString);
    }

    public void appendTo (StringBuffer buffer)
    {
        buffer.append(string());
    }

    public String toString ()
    {
        return StringUtil.strcat(getClass().getName(), ": \"", string(), "\"");
    }
}
