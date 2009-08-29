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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConstants.java#24 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWEncodedString;

abstract public class AWConstants extends AWBaseObject
{
    public static final AWEncodedString Ampersand = AWEncodedString.sharedEncodedString("&");
    public static final AWEncodedString Zero = AWEncodedString.sharedEncodedString("0");
    public static final AWEncodedString OnClick = AWEncodedString.sharedEncodedString("xclick");
    // See commment about onchange to the calls to AWGenericElement.registerBindingTranslation
    public static final AWEncodedString OnChange = AWEncodedString.sharedEncodedString("onchange");
    public static final AWEncodedString OnKeyPress = AWEncodedString.sharedEncodedString("xkeypress");
    public static final AWEncodedString OnKeyDown = AWEncodedString.sharedEncodedString("xkeydown");
    public static final AWEncodedString OnMouseUp = AWEncodedString.sharedEncodedString("xmouseup");
    public static final AWEncodedString Return = AWEncodedString.sharedEncodedString("return");
    public static final AWEncodedString OpenParen = new AWEncodedString("(");
    public static final AWEncodedString CloseParen = new AWEncodedString(")");
    public static final AWEncodedString This = AWEncodedString.sharedEncodedString("this");
    public static final AWEncodedString Comma = AWEncodedString.sharedEncodedString(",");
    public static final AWEncodedString SingleQuote = AWEncodedString.sharedEncodedString("'");
    public static final AWEncodedString Quote = AWEncodedString.sharedEncodedString("\"");
    public static final AWEncodedString QuestionMark = AWEncodedString.sharedEncodedString("?");
    public static final AWEncodedString Equals = AWEncodedString.sharedEncodedString("=");
    public static final AWEncodedString Space = AWEncodedString.sharedEncodedString(" ");
    public static final AWEncodedString Semicolon = AWEncodedString.sharedEncodedString(";");
    public static final AWEncodedString Null = AWEncodedString.sharedEncodedString("null");
    public static final AWEncodedString HashMark = AWEncodedString.sharedEncodedString("#");
    public static final AWEncodedString Javascript = AWEncodedString.sharedEncodedString("javascript");
    public static final AWEncodedString Colon = AWEncodedString.sharedEncodedString(":");
    public static final AWEncodedString LeftAngle = AWEncodedString.sharedEncodedString("<");
    public static final AWEncodedString RightAngle = AWEncodedString.sharedEncodedString(">");
    public static final AWEncodedString LeftSquare = AWEncodedString.sharedEncodedString("[");
    public static final AWEncodedString RightSquare = AWEncodedString.sharedEncodedString("]");
    public static final AWEncodedString Script = AWEncodedString.sharedEncodedString("script");
    public static final AWEncodedString Span = AWEncodedString.sharedEncodedString("span");
    public static final AWEncodedString Slash = AWEncodedString.sharedEncodedString("/");
    public static final AWEncodedString Newline = AWEncodedString.sharedEncodedString("\n");
    public static final AWEncodedString EmptyString = AWEncodedString.sharedEncodedString("");

    public static final AWEncodedString TagOnClick = new AWEncodedString("ariba.Handlers.hTagClick");
    public static final AWEncodedString TagOnKeyPress = new AWEncodedString("ariba.Handlers.hTagKeyDown");
    public static final AWEncodedString TagRefreshOnKeyPress = new AWEncodedString("ariba.Handlers.hTagRefreshKeyDown");
    public static final AWEncodedString OpenWindow = AWEncodedString.sharedEncodedString("Handlers.hOpenWindow");
    public static final AWEncodedString Event = AWEncodedString.sharedEncodedString("event");
    public static final AWEncodedString Class = AWEncodedString.sharedEncodedString("class");

    public static final int NotFound = -1;
    public static final int[] EmptyIntArray = new int[0];

    // session keys
    public static final String ComponentPathDebugFlagKey = "AWConstants.ComponentPathDebugEnabled";
    public static final String DropDebugEnabled = "AWConstants.DropDebugEnabled";
    public static final String ElementIdTracingEnabled = "AWConstants.ElementIdTracingEnabled";
}
