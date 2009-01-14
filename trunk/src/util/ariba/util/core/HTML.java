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

    $Id: //ariba/platform/util/core/ariba/util/core/HTML.java#23 $
*/

package ariba.util.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ariba.util.log.Log;

/**
    Some HTML helpers

    @aribaapi private
*/
public class HTML
{
    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /** No one should create one of these. */
    private HTML ()
    {
    }


    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

        // Unicode character for the euro
    private static final String EuroSymbolStr = "\u20ac";
    private static final char EuroSymbolChar = EuroSymbolStr.charAt(0);

        // Substring of the HTML numeric character value for the euro
    private static final String EuroNumericValue = "#8364";
    private static final int EuroNumericValueLength = EuroNumericValue.length();

        // HTML numeric character value for the euro
    private static final String EuroNumericStr = "&"+EuroNumericValue+";";


        // The euro char in WinLatin1 and MacLatin1(?).  Older browsers will
        // send this to us when a page is encoded in Latin1.
    private static final String ControlEuroStr = "\u0080";
    private static final char ControlEuroChar = ControlEuroStr.charAt(0);

        // mapping for some common HTML character entities
    public static final char[] entityMapChars = {
        '<',
        '>',
        '&',
        '\"',
        EuroSymbolChar,
        ControlEuroChar
    };
    public static final String[] entityMapStrings = {
        "lt",
        "gt",
        "amp",
        "quot",
        EuroNumericValue,
        EuroNumericValue
    };

        // mapping for some common XML character entities
    public static final char[] entityMapXMLChars = {
        '<',
        '>',
        '&',
        '\"',
        '\''
    };
    public static final String[] entityMapXMLStrings = {
        "lt",
        "gt",
        "amp",
        "quot",
        "apos"
    };

    public static final String[] safeHTMLStrings = {
        "lt;",
        "gt;",
        "amp;",
        "quot;",
        "apos;",
        "copy;",
        "deg;",
        "euro;",
        EuroNumericValue + ";",
        "nbsp;",
        "reg;",
    };

    static Map<String, String>safeHtmlCharacterMap = map (
            "&nbsp;", " ",
            "&lt;", "<",
            "&gt;", ">",
            "&amp;", "&",
            "&quot;", "\"",
            "&apos;", "\'");

    public static final String TagMeta = "meta";
    public static final String TagEndOfHead = "/head";
    public static final String TagHTTPEquivStr = "http-equiv";

    // a list of tag that the system will not force to have an enclosing
    // bracket.
    private static String[] doNotCloseTagList = { "br" };

    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Takes a raw text string and escapes any special character entities
        that need to be escaped in HTML (e.g. "&lt;" for "<", etc.).
    */
    public static String escape (String text)
    {
        if (text == null) {
            return text;
        }

        FastStringBuffer buf = new FastStringBuffer(text);
        escape(buf, false);
        return buf.toString();
    }

    /**
        Escapes HTML character entities in a FastStringBuffer.res
    */
    public static void escape (FastStringBuffer buf)
    {
        if (buf != null) {
            escape(buf, false);
        }
    }

    /**
        Takes a raw text string and escapes any special character entities
        that need to be escaped in HTML (e.g. "&lt;" for "<", etc.).  If the
        <b>doNBSPs</b> parameter is true, null or empty text strings will be
        converted to a single non-breaking space ("&nbsp").  If the <b>doBRs</b>
        parameter is true, newline characters ('\n') will be converted to HTML
        line breaks ("<BR>").

        @param text The text string to turn into HTML.
        @return the HTML string.
    */
    public static String fullyEscape (String text)
    {
        if (text == null || text.equals("")) {
            return "&nbsp;";
        }

        FastStringBuffer buf = new FastStringBuffer(text);
        escape(buf, true);
        return buf.toString();
    }

    /**
        Parses a string to get the meta charset. Returns null if there
        is no meta charset information.


        Charset is an attribute of the meta tag. The standard
        format is

        <meta http-equiv="Content-Type" content="text/html; charset=xxx">

        Note: IE has non-standard attribute for meta tag:

        <meta charset="xxx">
        there can be multiple <meta> tags in a document
        can use single or double quotes

        We do not handle this format.
    */

    public static String getCharset (InputStream inStream)
      throws IOException
    {
            // inputstream should support mark, if not, error out
        Assert.that(inStream.markSupported(), "inputstream does not support mark");
        FastStringBuffer buf = new FastStringBuffer(256);
        String encoding = null;

            // this should be sufficient to find the meta charset tag
        int maxRead = 2048;
        inStream.mark(maxRead);

        int c, numRead = 0;

        while (numRead++ < maxRead && (c = inStream.read()) != -1) {

            if (c == '<') {

                    // is this a meta tag?
                if (match(inStream, TagMeta) != 0) {

                    while ((c = inStream.read()) != -1) {
                        c = Character.toLowerCase((char)c);
                        if (c == '>') {
                            break;
                        }
                        buf.append((char)c);
                    }

                    if (c != -1) {
                        encoding = parseMeta(buf.toString());
                        if (encoding != null) {
                            break;
                        }
                    }
                }
                else if(match(inStream, TagEndOfHead) != 0) {
                        // if we have reached the end of the
                        // head tag, then there is no charset
                    break;
                }
            }
            buf.truncateToLength(0);
        }
        inStream.reset();

        return encoding;
    }

    /*-----------------------------------------------------------------------
        Private Methods
      -----------------------------------------------------------------------*/

    /*
        This code is called so frequently that it has been de-objectified.
        Firstly we manually track the length of the buffer to avoid constantly
        doing a .length on the fsb.  Further moved away from using charAt()
        to a buffer that we have to refresh every time the buffer is updated.
        The char buffer is only used for the equiv work of charAt.
    */
    private static void escape (final FastStringBuffer buf, final boolean escapeNewlines)
    {
        int bufferLength = buf.length();
        for (int i = 0; i < bufferLength; i++) {

            char c = buf.charAt(i);
            if (c == '\n') {
                if (escapeNewlines) {
                    buf.setCharAt(i++, '<');
                    buf.insert("BR>", i);
                    bufferLength += 3;
                        // i gets one more added to it when we loop
                    i += 2;
                      // I will assume that after such manipulation the buffer changes!
                }
            }
            else {
                int mapLength = entityMapChars.length;
                for (int j = 0; j < mapLength && i < bufferLength; j++) {
                    if (c == entityMapChars[j]) {
                        String rep = entityMapStrings[j];
                        int    len = rep.length();
                        buf.setCharAt(i++, '&');
                        buf.insert(rep, i);
                        bufferLength += len;
                        buf.insert(";", i + len);

                        bufferLength++;

                            // i gets one more added to it when we loop
                        i += len;
                          // assume buffer has changed again.
                        break;
                    }
                }
            }
        }
    }

    /**
        Returns a string that "un-escapes" any numeric character
        values of the euro.  I.e., it translates "&#8364" to
        "\u20ac" and translates the WinLatin1 "\u0080" to "\u20ac".
    */
    public static String unescapeEuro (String escapedHtml)
    {
        if (StringUtil.nullOrEmptyString(escapedHtml)) {
            return escapedHtml;
        }

        boolean htmlChar = (escapedHtml.indexOf(EuroNumericStr) > -1);
        boolean ctrlChar = (escapedHtml.indexOf(ControlEuroStr) > -1);
        if (!htmlChar && !ctrlChar) {
            return escapedHtml;
        }

        FastStringBuffer buf = new FastStringBuffer(escapedHtml);
        if (htmlChar) {
            buf.replace(EuroNumericStr, EuroSymbolStr);
        }
        if (ctrlChar) {
            buf.replace(ControlEuroStr, EuroSymbolStr);
        }
        return buf.toString();
    }


    /**
        Converts all characters in a string to XML-compatible characters.
    */
    public static String escapeXMLString (String buffer)
    {
        FastStringBuffer newBuffer = new FastStringBuffer(buffer.length()+10);
    loop:
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            int mapLength = entityMapXMLChars.length;
            for (int j = 0; j < mapLength; j++) {
                if (c == entityMapXMLChars[j]) {
                    newBuffer.append('&');
                    newBuffer.append(entityMapXMLStrings[j]);
                    newBuffer.append(';');
                    continue loop;
                }
            }
            newBuffer.append(c);
        }
        return newBuffer.toString();
    }

    public static void main (String[] args)
    {
        args = new String[] {
            "foo",
            "foo<",
            "foo>bar",
            "&bar",
            "\"",
            "a<b>c&\"'",
        };
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            Fmt.F(SystemUtil.out(), "%s=%s\n", arg, escapeXMLString(arg));
        }
        SystemUtil.exit(0);
    }

    private static String getAttributeValue (String str, int pos)
    {
        /*
            html allows spaces before and after the equal sign
            that defines the value of an attribute
            also, allows double or single quotes
        */
        String returnString = null;


        if (pos >= 0) {
            boolean found = false;
            boolean inValue = false;
            int startPos = -1;

            while (pos < str.length() && !found) {
                char c = str.charAt(pos);
                switch (c) {
                    case ' ':
                          // skip space
                        break;

                    case '\"':
                    case '\'':
                          // end of string
                        found = true;
                        returnString =  str.substring(startPos, pos);
                        returnString.trim();
                        break;
                default:
                    if (!inValue) {
                        startPos = pos;
                        inValue = true;
                    }
                    break;
                }
                pos++;
            }
        }
        return returnString;
    }

    /*
        <meta http-equiv="content-type" content="text/html; charset=xxx">

    */
    private static String parseMeta (String str)
    {
        String encoding = null;
        int pos = 0;

        pos = str.indexOf(TagHTTPEquivStr);

        if (pos >= 0) {
            pos = str.indexOf(MIME.HeaderContentType);
            if (pos >= 0) {
                pos = str.indexOf(MIME.ParameterCharSet);
                if (pos >= 0) {
                    pos = str.indexOf('=', pos+MIME.ParameterCharSet.length());
                    encoding = getAttributeValue(str, pos+1);
                }
            }
        }
        return encoding;
    }

    private static int match (InputStream inStream, String matchString)
      throws IOException
    {
        int c;
        int i;

        for (i = 0; (c = inStream.read()) != -1 && i < matchString.length(); i++) {
            c = Character.toLowerCase((char)c);
            if ( c != matchString.charAt(i)) {
                break;
            }
        }

        return i;
    }

    /*
       Escaping unsafe tags and attributes

       safeTags and safeAttrs stores the definitions of safe tags and attributes
       safeConfigDefined will be true if any of the two arrays is not empty
    */
    private static String[] safeTags = new String[0];
    private static String[] safeAttrs = new String[0];
    private static boolean safeConfigDefined = false;

    /**
       Sets the safe tag and attribute definitions
    */
    public static void setSafeConfig (String[] tags, String[] attrs)
    {
        if (tags != null && attrs != null) {
            safeTags = tags;
            safeAttrs = attrs;
            safeConfigDefined = (tags.length + attrs.length) > 0;
        }
    }

    /**
       Returns the index of the first quote in the buffer in the range of [from, to]
    */
    private static int findIndexOfQuote (FastStringBuffer buf, int from, int to)
    {
        int p = from-1;
        while (++p < to) {
            char c = buf.charAt(p);
            if (c == '"' || c == '\'') {
                return p;
            }
        }
        return -1;
    }

    /**
       Returns the first index of a char in the buffer in the range of [from, to]
    */
    private static int findIndexOfChar (FastStringBuffer buf, int from, int to, char c)
    {
        int p = from-1;
        while (++p < to)
            if (c == buf.charAt(p)) {
                return p;
            }
        return -1;
    }

    /**
     * Return the first non-whitespace char in the buffer in range of [from, to]
     */
    private static int skipWhiteSpace (FastStringBuffer buf, int from, int to)
    {
        int p = from-1;
        while (++p <= to)
            if (!Character.isWhitespace(buf.charAt(p))) {
                return p;
            }
        return -1;
    }

    /**
     * Return the first whitespace char or XML end tag char
     * in the buffer in range of [from, to].  It is expected buf[from] has
     * no leading white space.
     */
    private static int findEndTokenIndex (FastStringBuffer buf, int from, int to)
    {
        int p = from-1;
        while (++p <= to) {
            char c = buf.charAt(p);
            if (Character.isWhitespace(c) || c == '/' || c == '>') {
                return (p > from ? p-1 : -1);
            }
        }
        return -1;
    }

    private static final String[] unsafeAttrValues = {
        "javascript",
        "vbscript",
        "url"
    };
    /**
       Process the attributes portion of a safe element, which is in the range
       of [from, to] of the buffer.
       @return the end index of the attribute string which may have been modified
    */
    private static int processAttrs (FastStringBuffer buf, int from, int to)
    {
        int p = from;
        while (++p < to) {
            char c = buf.charAt(p);
            if (c == ' ' || c == '/') {
                continue;
            }
            // index of '='
            int eqIndex = buf.indexOf("=", p);
            // index of 1st quote after '='
            int dq1Index = eqIndex > p ? findIndexOfQuote(buf, eqIndex, to) : -1;
            // index of 2nd quote (has to be the same kind of the 1st) after '='
            int dq2Index = dq1Index > eqIndex ?
                 findIndexOfChar(buf, dq1Index+1, to, buf.charAt(dq1Index)) : -1;

            // If the attribute is not enclosed with quotes, then parse the
            // attr value assuming the value without space.
            boolean isQuoted = (dq1Index != -1);
            if (!isQuoted) {
                // find the first non-whitespace char after "=".
                dq1Index = skipWhiteSpace(buf, eqIndex+1, to);
                // find the end index of the token, delimitered by whitespace
                // and xml end tag
                dq2Index = dq1Index > eqIndex ?
                           findEndTokenIndex(buf, dq1Index, to) : -1;
            }
            if (eqIndex <= p || eqIndex >= to ||
                dq1Index <= eqIndex || dq1Index >= to ||
                dq2Index <= dq1Index || dq2Index >= to) {
                // cannot find the definition of an attribute
                throw new HTMLSyntaxException(Fmt.S("HTML.escapeUnsafe: Unmatched quotes found in %s", buf.substring(from, to)));
            }
            String attr = buf.substring(p, eqIndex).trim();
            String value = (isQuoted ?
                            buf.substring(dq1Index+1, dq2Index).toLowerCase() :
                            buf.substring(dq1Index, dq2Index+1).toLowerCase());
            // check if the value contains any unsafe string
            if (!isSafeAttributeValue(value)) {
                throw new HTMLSyntaxException(Fmt.S("HTML.escapeUnsafe: Attribute %s=%s is not safe.", attr, value));
            }
            boolean isSafe = false;
            for (int j = 0; j < safeAttrs.length; ++j) {
                if (attr.equalsIgnoreCase(safeAttrs[j])) {
                    // the attribtue is safe, skip it
                    if (dq1Index > eqIndex+1) {
                        buf.moveChars(dq1Index, eqIndex+1);
                        to -= dq1Index - (eqIndex+1);
                        p = dq2Index - (dq1Index - (eqIndex+1));
                    }
                    else {
                        p = dq2Index;
                    }
                    isSafe = true;
                    break;
                }
            }
            if (!isSafe) {
                throw new HTMLSyntaxException(Fmt.S("HTML.escapeUnsafe: Unsafe attribute %s found.", attr));
            }
        }
        return to;
    }

    public static boolean isSafeAttributeValue (String value)
    {
        for (int i = 0; i < unsafeAttrValues.length; ++i) {
            if (value.indexOf(unsafeAttrValues[i]) > -1) {
                return false;
            }
        }
        return true;
    }

    /**
       Escape a char in the buffer at index p
       @return the index unchanged or the end index of the replacement string in the buffer
    */
    private static int escapeChar (char c, FastStringBuffer buf, int bufferLength, int p)
    {
        int mapLength = entityMapChars.length;
        for (int j = 0; j < mapLength; j++) {
            if (c == entityMapChars[j]) {
                String rep = entityMapStrings[j];
                int    len = rep.length();
                buf.setCharAt(p++, '&');
                buf.insert(rep, p);
                bufferLength += len;
                buf.insert(";", p + len);
                bufferLength++;
                p += len;
                break;
            }
        }
        return p;
    }

    private static int findTagEndIndex (FastStringBuffer buf, int starts, int ends)
    {
        int p = starts-1;
        while (++p < ends) {
            char c = buf.charAt(p);
            if (Character.isWhitespace(c) || c == '/' || c == '>') {
                break;
            }
        }
        return p;
    }

    private static class HTMLSyntaxException extends RuntimeException
    {
        HTMLSyntaxException (String msg)
        {
            super(msg);
        }
    }

    /**
       Escapes a string. The flag unsafeOnly determines a unsafe only escape or
       a full escape.
       @return the escaped string
    */
    public static String escape (String str, boolean unsafeOnly)
    {
        return unsafeOnly ? escapeUnsafe(str) : escape(str);
    }

    /**
       Escapes unsafe tags and attributes in the given String
       @return the escaped string
    */
    public static String escapeUnsafe (String str)
    {
        // for an empty or null string, returns a non-breaking-space
        if (StringUtil.nullOrEmptyOrBlankString(str)) {
            return "&nbsp;";
        }
        try {
            // escape unsafe only if safe config is defined
            if (safeConfigDefined) {
                return escapeUnsafe(new FastStringBuffer(str));
            }
        } catch(HTMLSyntaxException hse) {
            Log.util.debug("HTML.escapeUnsafe: Exception in parsing HTML: %s ", hse.getMessage());
        }
        // do a full escape if unsafe escape failed or safe config undefined
        return escape(str);
    }

    /**
       Escapes unsafe tags/attributes in the string stored in the buffer
       @return the escaped string
    */
    private static String escapeUnsafe (FastStringBuffer buf)
    {
        Log.util.debug("HTML.escapeUnsafe: Got    :%s", buf);
        // uses a list for tag matching
        ArrayList tags = new ArrayList();
        int bufferLength = buf != null ? buf.length() : 0;
        int i = -1;
        int spaceCount = 0;
        while (++i < bufferLength) {
            char c = buf.charAt(i);
            boolean done = false;
            if (c != ' ') {
                spaceCount = 0;
            }
            if (c == '<') {
                try {
                    // a tag is detected
                    int offset = i + 1;
                    boolean closingTag = offset < bufferLength && '/' == buf.charAt(offset);
                    if (closingTag) {
                        ++offset;
                    }
                    // find the close bracket
                    int close = buf.indexOf(">", offset);
                    if (close == -1) {
                        throw new HTMLSyntaxException("HTML.escapeUnsafe: Missing '>'.");
                    }
                    int nextOpen = buf.indexOf("<", offset);
                    if (nextOpen > 0 && nextOpen < close) {
                        throw new HTMLSyntaxException("HTML.escapeUnsafe: Found '<' between '<' and '>'.");
                    }
                    // find the end index of the tag
                    int tagEndsAt = findTagEndIndex(buf, offset, close);
                    if ((tagEndsAt - offset) <= 0) {
                        throw new HTMLSyntaxException("HTML.escapeUnsafe: Empty tag.");
                    }
                    String tag = buf.substring(offset, tagEndsAt).trim();
                    if (tag.matches("!--+")) {
                        String commentClose = buf.substring(close - 2, close);
                        if ("--".equals(commentClose)) {
                            i = close;
                            continue;
                        }
                    }
                    boolean selfClosed = buf.charAt(close-1) == '/';
                    if (closingTag) {
                        // if the tag is a closing, it must match the last open tag
                        if (tags.size() == 0 || !tag.equalsIgnoreCase(tags.get(tags.size()-1).toString())) {
                             throw new HTMLSyntaxException(Fmt.S("Tag %s closed but not currently open.", tag));
                        }
                        tags.remove(tags.size()-1);
                     }
                    // check if the tag is safe
                    for (int j = 0; j < safeTags.length; ++j) {
                        if (tag.equalsIgnoreCase(safeTags[j])) {
                            // the tag is safe, skip it
                            offset += tag.length();
                            if (close > offset+1 && buf.charAt(offset) == ' ') {
                                // process the attributes, if any
                                close = processAttrs(buf, offset, close);
                                bufferLength = buf.length();
                            }
                            if (!selfClosed && !closingTag) {
                                tags.add(tag);
                            }
                            i = close > offset ? close : offset;
                            done = true;
                            break;
                        }
                    }
                } catch (HTMLSyntaxException hse) {
                    Log.util.debug("HTML.escapeUnsafe: Exception in parsing HTML: %s ", hse.getMessage());
                }
            }
            else if (c == '&') {
                for (int j = 0; j < safeHTMLStrings.length; j++) {
                    String safeHTMLString = safeHTMLStrings[j];
                    if (buf.startsWith(safeHTMLString, i+1)) {
                        // skip over the nbsp
                        i += safeHTMLString.length();
                        done = true;
                        break;
                    }
                }
            }
            else if (c == ' ') {
                // convert multiple spaces to nbsp
                spaceCount++;
                if (spaceCount > 1) {
                    buf.setCharAt(i, '&');
                    buf.insert("nbsp;", i + 1);
                    i += 5;
                    bufferLength += 5;
                }
                done = true;
            }
            else if (c == '\n') {
                // convert newline to break
                buf.setCharAt(i, '<');
                buf.insert("br/>", i + 1);
                i += 4;
                bufferLength += 4;
                done = true;
            }
            if (!done) {
                // the char has not been processed, escape it
                int p = escapeChar(c, buf, bufferLength, i);
                bufferLength += p-i;
                i = p;
            }
        }
        // Force closing open safe tags
        if (tags.size() > 0) {
            Log.util.debug("HTML.escapeUnsafe: Force closing of unclosed tags:%s", tags);
            for (int n = tags.size()-1; n >= 0; --n) {
                String tag = (String)tags.get(n);
                if (shouldCloseTag(tag)) {
                    buf.append("</");
                    buf.append(tag);
                    buf.append(">");
                }
	        }
        }
        Log.util.debug("HTML.escapeUnsafe: Returns:%s", buf);
        return bufferLength > 0 ? buf.toString() : "";
    }

    /**
        Filters out any unsafe HTML and returns a safe HTML string 
        @aribaapi private
    */
    public static String filterUnsafeHTML (String str)
    {
        // prevent safe tags from being stripped
        for (String safeTag : safeTags) {
            String safeTagPattern =
                Fmt.S("<(/%s)>", safeTag);
            str = str.replaceAll(safeTagPattern, "!sf!$1!sf!");
            safeTagPattern =
                Fmt.S("<(%s[^>]*/?)>", safeTag);
            str = str.replaceAll(safeTagPattern, "!sf!$1!sf!");
        }

        // prevent safe attributes from being stripped
        for (String safeAttributes : safeAttrs) {
            String safeAttributePattern =
                Fmt.S(" (%s)=(\"[^\"]*\")", safeAttributes);
            str = str.replaceAll(safeAttributePattern, " @sf@$1@sf@$2@sf@");
        }

        // stripped all remaining unsafe HTML
        String safeHTML = str.replaceAll("<script>.*</script>", "");
        safeHTML = safeHTML.replaceAll("<iframe>.*</iframe>", "");
        safeHTML = safeHTML.replaceAll("<[^/]*/>", "");
        safeHTML = safeHTML.replaceAll("<[^<]*>", "");
        safeHTML = safeHTML.replaceAll(" [^ ]*=\"[^\"]*\"", "");
        for (String unsafeAttrVal : unsafeAttrValues) {
            String unsafeAttrValPattern =
                Fmt.S("(%s:)", unsafeAttrVal);
            safeHTML = safeHTML.replaceAll(unsafeAttrValPattern, "x$1");
        }

        // bring back the safe HTML
        safeHTML = safeHTML.replaceAll("!sf!(.*?)!sf!", "<$1>");
        safeHTML = safeHTML.replaceAll("@sf@(.*?)@sf@(.*?)@sf@", "$1=$2");
        return safeHTML;
    }

    /*
       Checks to see if the HTML should be closed or not.
       Tags are checked case-incensitive.
    */
    private static boolean shouldCloseTag (String tag)
    {
        int tagCount = doNotCloseTagList.length;
        for (int i =0; i<tagCount; i++) {
            if (doNotCloseTagList[i].equalsIgnoreCase(tag)) {
                return false;
            }
        }
        return true;
    }


    private static final Pattern RemoveTagsPattern =
        Pattern.compile("<[^<]*?>", Pattern.MULTILINE);

    public static String convertToPlainText (String richText)
    {
        if (richText == null) {
            return null;
        }
        return RemoveTagsPattern.matcher(richText).replaceAll("");
    }

    public static String fullyConvertToPlainText (String text)
    {
        String converted = text.replaceAll("(<br/>)|(<br>)|(</div>)","\n");
        converted = converted.replaceAll(">>",">&gt;");
        converted = RemoveTagsPattern.matcher(converted).replaceAll("");

        // Todo: Fix slow, multi-match, impl!  (Should create one (precomputed) combo regex)
        for (Map.Entry<String,String> e : safeHtmlCharacterMap.entrySet()) {
            converted = converted.replaceAll(e.getKey(), e.getValue());
        }

        return converted;
    }

    // todo: move to MapUtil
    static <K,V> Map<K,V> map (Object ...keysAndValues)
    {
        int c = keysAndValues.length;
        Assert.that(c % 2 == 0, "Must have even number of args: key, value, ... : %s");
        Map<K,V> result = new HashMap<K,V>(c/2);
        for (int i=0; i < c; i += 2) {
            result.put((K)keysAndValues[i], (V)keysAndValues[i+1]);
        }
        return result;
    }       
}
