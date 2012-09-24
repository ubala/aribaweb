/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWEncodedString.java#21 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.HTML;
import ariba.util.core.StringUtil;

/*
  Currently this only allows for three encodings.  If we need more, it may make sense to change the implementation to use a byte[][] rather than discrete instance variables.  Another thought is to use a BlockHeap for the arrays and avoid the allocation overhaed of having thousands of very short arrays.
 */

// subclassed by AWEscapedString and others like it
public class AWEncodedString extends AWBaseObject
{
    private static int ThreadLocalCacheSize = 128;
    private static AWSizeLimitedHashtable SharedEncodedStrings = new AWSizeLimitedHashtable(16 * ThreadLocalCacheSize);
    private static ThreadLocal ThreadLocalEncodedStrings = new ThreadLocal();
    private static boolean DebuggingEnabled = false;
    private static final int MaxStringLength = 512;
    private static final AWCharacterEncoding ZerothCharacterEncoding = AWCharacterEncoding.UTF8;
    private static final AWCharacterEncoding FirstCharacterEncoding = AWCharacterEncoding.ISO8859_1;
    public static final AWEncodedString Undefined = new AWEncodedString("undefined");

    private final int _hashCode;
    private final String _string;
    protected final byte[] _bytes0;
    private byte[] _bytes1;
    protected AWEscapedString _next;

    public static void setSharedStringsLimit (int size)
    {
        synchronized (SharedEncodedStrings) {
            SharedEncodedStrings = new AWSizeLimitedHashtable(size);
        }
    }

    public static AWEncodedString sharedEncodedString (String string)
    {
        AWEncodedString sharedEncodedString = null;
        if (string != null) {
            if (string.length() >= MaxStringLength) {
                sharedEncodedString = new AWEncodedString(string);
            }
            else if (AWUtil.AllowsConcurrentRequestHandling) {
                AWSizeLimitedHashtable encodedStringsCache = (AWSizeLimitedHashtable)ThreadLocalEncodedStrings.get();
                if (encodedStringsCache == null) {
                    encodedStringsCache = new AWSizeLimitedHashtable(ThreadLocalCacheSize);
                    ThreadLocalEncodedStrings.set(encodedStringsCache);
                }
                sharedEncodedString = (AWEncodedString)encodedStringsCache.get(string);
                if (sharedEncodedString == null) {
                    synchronized (SharedEncodedStrings) {
                        sharedEncodedString = (AWEncodedString)SharedEncodedStrings.get(string);
                        if (sharedEncodedString == null) {
                            sharedEncodedString = new AWEncodedString(string);
                            if (string.length() < MaxStringLength) {
                                SharedEncodedStrings.put(string, sharedEncodedString);
                            }
                        }
                        if (string.length() < MaxStringLength) {
                            encodedStringsCache.put(string, sharedEncodedString);
                        }
                    }
                }
            }
            else {
                sharedEncodedString = (AWEncodedString)SharedEncodedStrings.get(string);
                if (sharedEncodedString == null) {
                    sharedEncodedString = new AWEncodedString(string);
                    if (string.length() < MaxStringLength) {
                        SharedEncodedStrings.put(string, sharedEncodedString);
                    }
                }
            }
        }
        return sharedEncodedString;
    }

    public static AWEncodedString sharedEncodedString (Object object)
    {
        String stringValue = (object instanceof String) ? (String)object : object.toString();
        return AWEncodedString.sharedEncodedString(stringValue);
    }

    public AWEncodedString (String stringValue)
    {
        super();
        Assert.that(stringValue != null, "stringValue must be non-null");
        _string = stringValue;
        _hashCode = stringValue.hashCode();
        _bytes0 = ZerothCharacterEncoding.getBytes(_string);
    }

    public boolean mustQuoteAsAttribute ()
    {
        // only implement for EscapedString -- return conservative value
        return true;
    }

    ////////////////
    // Utility
    ////////////////
    protected byte[] uniqueBytes (byte[] targetBytes)
    {
        byte[] uniqueBytes = targetBytes;
        if (AWUtil.equals(targetBytes, _bytes0)) {
            uniqueBytes = _bytes0;
        }
        else if (AWUtil.equals(targetBytes, _bytes1)) {
            uniqueBytes = _bytes1;
        }
        return uniqueBytes;
    }

    private byte[] bytesForCharacterEncoding (String string, AWCharacterEncoding characterEncoding)
    {
        byte[] bytes = characterEncoding.getBytes(string);
        return uniqueBytes(bytes);
    }

    /** @deprecated  use string()
    */
    public String computeString ()
    {
        return _string;
    }

    ////////////////
    // Accessors
    ////////////////
    public static void setDebuggingEnabled (boolean value)
    {
        DebuggingEnabled = value;
    }

    public static boolean debuggingEnabled ()
    {
        return DebuggingEnabled;
    }

    private void setBytes (byte[] bytes, AWCharacterEncoding characterEncoding)
    {
        if (characterEncoding == ZerothCharacterEncoding) {
            throw new AWGenericException("Already set zeroth encoding");
        }
        else if (characterEncoding == FirstCharacterEncoding) {
            _bytes1 = bytes;
        }
        else {
            throw new AWGenericException("Unsuported characterEncoding: " + characterEncoding.name);
        }
    }

    public String toString ()
    {
        return _string;
    }

    public String string ()
    {
        return _string;
    }

    public byte[] bytes (AWCharacterEncoding characterEncoding)
    {
        byte[] bytes = null;
        if (characterEncoding == ZerothCharacterEncoding) {
            return _bytes0;
        }
        else if (characterEncoding == FirstCharacterEncoding) {
            bytes = _bytes1;
        }
        else if (characterEncoding == AWCharacterEncoding.Ksc56011987 ||
                 characterEncoding == AWCharacterEncoding.Big5 ||
                 characterEncoding == AWCharacterEncoding.Gb2312) {
            // This is special case for Korean and Chinese Faxes.  No need to cache since not used in interactive apps.
            return characterEncoding.getBytes(string());
        }
        else {
            throw new AWGenericException("Unsuported characterEncoding: " + characterEncoding.name);
        }
        if (bytes == null) {
            bytes = bytesForCharacterEncoding(_string, characterEncoding);
            setBytes(bytes, characterEncoding);
        }
        return bytes;
    }

    public int hashCode ()
    {
        return _hashCode;
    }

    public boolean equals (Object otherObject)
    {
        boolean equals = false;
        if (this == otherObject) {
            equals = true;
        }
        else if (otherObject instanceof AWEncodedString) {
            if (_hashCode != otherObject.hashCode()) {
                equals = false;
            }
            else {
                AWEncodedString otherEncodedString = (AWEncodedString)otherObject;
                equals = _string.equals(otherEncodedString.string());
            }
        }
        else {
            _string.equals(otherObject);
        }
        return equals;
    }

    public boolean equals (String string)
    {
        return _string.equals(string);
    }

    public int compareTo (AWEncodedString otherEncodedString)
    {
        // must compare strings, not byte arrays, here to get proper sort order.
        return _string.compareTo(otherEncodedString == null ? null : otherEncodedString.string());
    }

    ///////////////////
    // Escaped Strings
    ///////////////////
    public AWEscapedString htmlEscapedString ()
    {
        return escapedString(AWEscapedString.HtmlEscaped);
    }

    public AWEscapedString htmlUnsafeEscapedString ()
    {
        return escapedString(AWEscapedString.HtmlUnsafeEscaped);
    }

    public AWEscapedString htmlAttributeString ()
    {
        return escapedString(AWEscapedString.HtmlAttributeEscaped);
    }

    public AWEscapedString xmlEscapedString ()
    {
        return escapedString(AWEscapedString.XmlEscaped);
    }

    protected AWEscapedString escapedString (int escapingType)
    {
        AWEscapedString escapedString = null;
        if (_next != null) {
            escapedString = _next.escapedString(escapingType);
        }
        else {
            escapedString = new AWEscapedString(this, escapingType);
            _next = escapedString;
        }
        return escapedString;
    }

    public static AWEncodedString htmlEscapedString (Object object)
    {
        AWEncodedString encodedString = AWEncodedString.sharedEncodedString(object);
        return encodedString.htmlEscapedString();
    }

    public static AWEncodedString htmlUnsafeEscapedString (Object object)
    {
        AWEncodedString encodedString = AWEncodedString.sharedEncodedString(object);
        return encodedString.htmlUnsafeEscapedString();
    }

    public static AWEncodedString xmlEscapedString (Object object)
    {
        AWEncodedString encodedString = AWEncodedString.sharedEncodedString(object);
        return encodedString.xmlEscapedString();
    }
}

final class AWEscapedString extends AWEncodedString
{
    protected static final int HtmlEscaped = 0;
    protected static final int HtmlAttributeEscaped = 1;
    protected static final int XmlEscaped = 2;
    protected static final int HtmlUnsafeEscaped = 3;
    protected static final int EscapeTypeMask = 0x0F;
    protected static final int DidCheckQuotingMask = 0x10;
    protected static final int NeedsQuotesMask = 0x20;

    private int _escapingType;
    private AWEncodedString _original;

    private static String escape (AWEncodedString original, int escapingType)
    {
        String escapedString = null;
        String originalString = original.string();
        switch (escapingType) {
            case HtmlEscaped: {
                if (debuggingEnabled()) {
                    escapedString = AWUtil.escapeHTMLExceptEmbeddedContext(originalString);
                }
                else {
                    escapedString = HTML.escape(originalString);
                }
                break;
            }
            case HtmlAttributeEscaped: {
                escapedString = HTML.escapeHTMLAttribute(originalString);
                break;
            }
            case XmlEscaped: {
                escapedString = AWXMLDocument.XMLEncode(originalString);
                break;
            }
            case HtmlUnsafeEscaped: {
                if (debuggingEnabled()) {
                    escapedString = AWUtil.escapeHTMLExceptEmbeddedContext(originalString, true);
                }
                else {
                    escapedString = HTML.escapeUnsafe(originalString);
                }
                break;
            }
            default: {
                throw new AWGenericException("AWEscapedString: unsupported escaping type: " + escapingType);
            }
        }
        if (escapedString.equals(originalString)) {
            escapedString = originalString;
        }
        return escapedString;
    }

    public AWEscapedString (AWEncodedString original, int escapingType)
    {
        super(escape(original, escapingType));
        _escapingType = escapingType;
        _original = original;
    }

    protected AWEscapedString escapedString (int escapingType)
    {
        AWEscapedString escapedString = null;
        if (escapingType == (_escapingType & EscapeTypeMask)) {
            escapedString = this;
        }
        else if (_next != null) {
            escapedString = _next.escapedString(escapingType);
        }
        else {
            escapedString = new AWEscapedString(_original, escapingType);
            _next = escapedString;
        }
        return escapedString;
    }

    public AWEscapedString htmlEscapedString ()
    {
        return escapedString(HtmlEscaped);
    }

    public AWEscapedString htmlUnsafeEscapedString ()
    {
        return escapedString(HtmlUnsafeEscaped);
    }

    public AWEscapedString htmlAttributeString ()
    {
        return escapedString(HtmlAttributeEscaped);
    }

    public AWEscapedString xmlEscapedString ()
    {
        return escapedString(XmlEscaped);
    }

    public boolean mustQuoteAsAttribute ()
    {
        if ((_escapingType & DidCheckQuotingMask) == 0) {
            boolean needsQuotes = true;
            int len = _bytes0.length;
            if ((len > 0) && (_escapingType != XmlEscaped)) {
                int i;
                for (i=0; i<len; i++) {
                    if (!Character.isJavaIdentifierPart((char)_bytes0[i])) break;
                }
                if (i == len) needsQuotes=false;
            }
            if (needsQuotes) _escapingType |= NeedsQuotesMask;
            _escapingType |= DidCheckQuotingMask;
        }

        return ((_escapingType & NeedsQuotesMask) != 0);
    }
    
    /**
     * Overriding this allows for reusing the unique bytes from the original encoded string.
     * If the bytes are the same, we keep only the original copy.
     * @param targetBytes
     * @return
     */
    protected byte[] uniqueBytes (byte[] targetBytes)
    {
        return _original.uniqueBytes(targetBytes);
    }
}

final class AWXMLDocument
{
    /*

     This code comes from ANUtilities/XMLDocument.java.  We need to promote that stuff, or find a suitable substitute to do the following.

    */

    // escaped char need to be less than 64 otherwise change XMLEncode
    static final char[] charsToEscape = {
        '&',/*38*/
        '<',/*60*/
        '>',/*62*/
        '\'',/*34*/
        '"',/*34*/
        //'\r',/*13*/
        //'\n'/*10*/
    };

    // set the max char of the array in charsToEscape for optimization
    static final char maxChar = '>';

    static final char[][] escapeSequences = {
        "&amp;".toCharArray(),
        "&lt;".toCharArray(),
        "&gt;".toCharArray(),
        "&apos;".toCharArray(),
        "&quot;".toCharArray(),
        //"&#013;".toCharArray(),
        //"&#010;".toCharArray()
    };

    public static FastStringBuffer XMLEncode (String statusText, FastStringBuffer buf)
    {
        if (statusText == null) {
            return null;
        }

        int preIndex=0;
        for (int i=0; i< statusText.length(); i++) {
            char[] escape = null;
            char charToCheck = statusText.charAt(i);

            if (charToCheck > maxChar) {
                continue;
            }

            for (int j=0; j< charsToEscape.length; j++) {
                if (charToCheck == charsToEscape[j]) {
                    escape=escapeSequences[j];
                    break;
                }
            }
            if (escape == null) {
                continue;
            }
            else {
                if (buf == null) {
                    /* we already know that we need extra chars,
                    so create the buffer with len greater than orginal chars*/
                    buf = new FastStringBuffer((statusText.length() + 10));
                }
                buf.appendStringRange(statusText, preIndex, i);
                buf.append(escape);
                preIndex = (i+1);
            }
        }

        if (buf != null) {
            buf.appendStringRange(statusText, preIndex, statusText.length());
        }

        return buf;
    }

    public static String XMLEncode (String statusText)
    {
        FastStringBuffer buf = XMLEncode(statusText, null);
        if (buf == null) {
            return statusText;
        }
        return buf.toString();
    }
}
