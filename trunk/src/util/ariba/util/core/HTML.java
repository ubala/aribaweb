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

    $Id: //ariba/platform/util/core/ariba/util/core/HTML.java#42 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final int NOT_FOUND = -1;

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
        "middot;"
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
    // bracket. Used in shouldCloseTag(...).
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
       Escapes a string. The flag unsafeOnly determines a unsafe only escape or
       a full escape.
       @return the escaped string
    */
    public static String escape (String str, boolean unsafeOnly)
    {
        return unsafeOnly ? escapeUnsafe(str) : escape(str);
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
        }
        catch(HTMLSyntaxException hse) {
            Log.util.debug("HTML.escapeUnsafe: Exception in parsing HTML: %s ", hse.getMessage());
        }
        // do a full escape if unsafe escape failed or safe config undefined
        return escape(str);
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

    private static Pattern scriptPattern = 
        Pattern.compile("<(script[^>]*>)",Pattern.CASE_INSENSITIVE);

    /**
     * All tags within a HTML attribute are ignored by the browser. But with XMLHTTP 
     * requests, we search for the script tags and execute them. Fixing this on 
     * the client is expensive.
     * 
     *  This method replaces all quotes with &QUOT; and the opening angle bracket of the 
     *  script tag with a ?. 
     * 
     * @param attr
     * @return String with escaped quotes and ? for opening angle bracket of script tag.
     */
    public static String escapeHTMLAttribute (String attr)
    {
        if(StringUtil.nullOrEmptyString(attr)) {
            return attr;
        }
        String ret = StringUtil.replaceCharByString(attr, '"', "&QUOT;");
        return scriptPattern.matcher(ret).replaceAll("?$1");
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
 
    /**
     * This is a helper function to getCharset(...).
     * @param inStream
     * @param matchString
     * @return
     * @throws IOException
     */
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
        <meta http-equiv="content-type" content="text/html; charset=xxx">
        This is a helper function to getCharset().
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

    /**
     * This is a helper function to parseMeta.
     * @param str
     * @param pos
     * @return
     */
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

    /**
        Wraps a source FastStringBuffer for copy, replace, and append operations.
        Copies are lazy.  If no replace or append are called,
        the copies do not actually happen,
        and the source FastStringBuffer is used on toString()
        Otherwise, a replace or append call triggers any deferred copies,
        and a new destination FastStringBuffer is allocated and used.
    */
    static class LazyFastStringBuffer
    {
        private FastStringBuffer _srcBuffer;
        private char[] _srcBufferChars;
        private int _copyOffset;
        private FastStringBuffer _destBuffer;

        public LazyFastStringBuffer (FastStringBuffer srcBuffer)
        {
            _srcBuffer = srcBuffer;
            _srcBufferChars = srcBuffer.getBuffer();
            _copyOffset = 0;
        }

        /**
            Replaces one char from the source buffer
            with the given string at the current offset.
            This will activate the destination buffer.
        */
        public void replaceChar (String str)
        {
            append(str);
            _copyOffset++;
        }

        /**
            Appends the given string.
            This will activate the destination buffer.
        */
        public void append (String str)
        {
            activate(str.length());
            _destBuffer.append(str);
        }

        /**
            Copies one char from the source buffer
            at the current offset.
        */
        public void copyChar ()
        {
            if (_destBuffer != null) {
                _destBuffer.append(_srcBufferChars[_copyOffset]);
            }
            _copyOffset++;
        }

        /**
            Copies a char range from the source buffer
            beginning at the current offset and
            ending at the given endOffset.
        */
        public void copyCharRange (int endOffset)
        {
            if (_destBuffer != null) {
                _destBuffer.appendCharRange(_srcBufferChars , _copyOffset, endOffset);
            }
            _copyOffset = endOffset;
        }

        private void activate (int extendBy)
        {
            if (_destBuffer == null) {
                _destBuffer = new FastStringBuffer(_srcBuffer.length() + extendBy);
                _destBuffer.appendCharRange(_srcBufferChars, 0, _copyOffset);
            }
        }

        public String toString ()
        {
            return _destBuffer != null ? _destBuffer.toString() : _srcBuffer.toString();
        }
    }

    /**
       Escapes unsafe tags/attributes in the string stored in the buffer
       This is a helper function to escapeUnsafe(String).
       This function does not fully support the HTML specification by not handling some technically valid input
       like < /p>.
       @return the escaped string
    */
    private static String escapeUnsafe (FastStringBuffer buf)
    {
        Log.util.debug("HTML.escapeUnsafe: Got    :%s", buf);
        if (buf == null) {
            return "";
        }
        // uses a list for tag matching
        ArrayList tags = new ArrayList();
        int bufferLength = buf.length();
        int i = -1;
        // how many continuous space characters (not newlines)
        int spaceCount = 0;
        LazyFastStringBuffer escapedBuf = new LazyFastStringBuffer(buf);
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

                    // skip the offset over the / in a closing tag like </p>
                    // this fails for < /p>, should be == buf.charAt(nextNonWhitespaceCharIndex)
                    boolean closingTag =
                            offset < bufferLength && '/' == buf.charAt(offset);
                    if (closingTag) {
                        ++offset;
                    }

                    // find the close bracket
                    int close = buf.indexOf(">", offset);
                    if (close == -1) {
                        throw new HTMLSyntaxException(
                            "HTML.escapeUnsafe: Missing '>'.");
                    }

                    // find the next opening bracket
                    int nextOpen = buf.indexOf("<", offset);
                    if (nextOpen > 0 && nextOpen < close) {
                        throw new HTMLSyntaxException(
                            "HTML.escapeUnsafe: Found '<' " +
                            "between '<' and '>'.");
                    }
                    
                    // find the end index of the tag, not including the attributes
                    int tagEndsAt = findTagEndIndex(buf, offset, close);
                    if ((tagEndsAt - offset) <= 0) {
                        throw new HTMLSyntaxException("HTML.escapeUnsafe: Empty tag.");
                    }

                    String tag = buf.substring(offset, tagEndsAt).trim();

                    // skip over things like <!--+ some random comment -->
                    if (tag.matches("!--+")) {
                        String commentClose = buf.substring(close - 2, close);
                        if ("--".equals(commentClose)) {
                            escapedBuf.copyCharRange(close+1);
                            i = close;
                            continue;
                        }
                    }

                    boolean selfClosed = buf.charAt(close-1) == '/';

                    if (closingTag) {
                        // if the tag is a closing, it must match the last open tag
                        if (tags.isEmpty() ||
                            !tag.equalsIgnoreCase(
                                tags.get(tags.size()-1).toString())) {
                             throw new HTMLSyntaxException(
                                 Fmt.S("Tag %s closed but not currently open.",
                                     tag));
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
                                
                                // this can throw an exception, leading to the
                                // initial < being HTML encoded along with the rest
                                // of the tag, and it's closing tag
                                processAttrs(buf, offset, close);
                                bufferLength = buf.length();
                            }
                            // discard <br>s
                            if (!selfClosed && !closingTag && !tag.equals("br")) {
                                tags.add(tag);
                            }
                            escapedBuf.copyCharRange(close+1);
                            // max of close or offset
                            i = close > offset ? close : offset;
                            done = true;
                            break;
                        }
                    }
                }
                catch (HTMLSyntaxException hse) {
                    Log.util.debug("HTML.escapeUnsafe: Exception in parsing HTML: %s ", hse.getMessage());
                }
            }
            else if (c == '&') {
                // skip over safe HTML encoded entities (ie, "&nbsp;")
                for (int j = 0; j < safeHTMLStrings.length; j++) {
                    String safeHTMLString = safeHTMLStrings[j];
                    if (buf.startsWith(safeHTMLString, i+1)) {
                        // skip over the nbsp
                        int safeHTMLLength = safeHTMLString.length();
                        escapedBuf.copyCharRange(i + safeHTMLLength + 1);
                        i += safeHTMLLength;
                        done = true;
                        break;
                    }
                }
            }
            else if (c == ' ') {
                // convert multiple spaces to nbsp
                spaceCount++;
                if (spaceCount == 1) {
                    escapedBuf.copyChar();
                }
                else if (spaceCount > 1) {
                    escapedBuf.replaceChar("&nbsp;");
                }                
                done = true;
            }
            else if (c == '\n') {
                // convert newline to break
                escapedBuf.replaceChar("<br/>");
                done = true;
            }
            if (!done) {
                // the char has not been processed, escape it
                escapeChar(c, buf, escapedBuf, bufferLength, i);
            }
        }
        // Force closing open safe tags
        if (!tags.isEmpty()) {
            Log.util.debug("HTML.escapeUnsafe: Force closing of unclosed tags:%s", tags);
            for (int n = tags.size()-1; n >= 0; --n) {
                String tag = (String)tags.get(n);
                if (shouldCloseTag(tag)) {
                    escapedBuf.append("</");
                    escapedBuf.append(tag);
                    escapedBuf.append(">");
                }
            }
        }
        String result = escapedBuf.toString();
        Log.util.debug("HTML.escapeUnsafe: Returns:%s", result);
        return result;
    }

    /**
     * This returns the index of the position right after the tag name.
     * Example: the string "<p style=''/>" will return 2, for the space right after 'p'.
     * This is a helper function to escapeUnsafe(...).
     * @param buf
     * @param starts
     * @param ends
     * @return
     */
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

    /**
       Escape a char in the buffer at index p.
       This is a helper function to escapeUnsafe(...).
       @return the index unchanged or the end index of the replacement string in the buffer
    */
    private static void escapeChar (char c, FastStringBuffer buf, LazyFastStringBuffer escapedBuf, int bufferLength, int p)
    {
        int mapLength = entityMapChars.length;
        boolean escaped = false;
        for (int j = 0; j < mapLength; j++) {
            if (c == entityMapChars[j]) {
                String rep = '&' + entityMapStrings[j] + ';';
                escapedBuf.replaceChar(rep);
                escaped = true;
                break;
            }
        }
        if (!escaped) {
            escapedBuf.copyChar();
        }
    }

    /*
        This code is called so frequently that it has been de-objectified.
        Firstly we manually track the length of the buffer to avoid constantly
        doing a .length on the fsb.  Further moved away from using charAt()
        to a buffer that we have to refresh every time the buffer is updated.
        The char buffer is only used for the equiv work of charAt.
        This is a helper function to escape(...) and fullyEscape(...).
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

    /*
       Checks to see if the HTML should be closed or not.
       Tags are checked case-incensitive.
       This is a helper function to escapeUnsafe(...).
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


    /**
       Process the attributes portion of a safe element, which is in the range
       of [from, to] of the buffer.  This is a helper function to escapeUnsafe(...).
       @return the end index of the attribute string which may have been modified
    */
    private static void processAttrs (FastStringBuffer buf, int from, int to)
    {
        // the attrs will be treated as the first space after the tag name
        // to the closing >, ie, for "<p style=''>" the attrs will be " style=''>"
        int p = from;
        while (++p < to) {
            final char c = buf.charAt(p);

            // skip over space and /
            if (c == ' ' || c == '/') {
                continue;
            }
            
            // index of '='
            final int eqIndex = buf.indexOf("=", p);

            // index of 1st quote after '=', "double quote 1 index"
            final boolean foundEqIndex = eqIndex > p;
            int dq1Index = foundEqIndex ? findIndexOfQuote(buf, eqIndex, to) : NOT_FOUND;

            // index of 2nd quote (has to be the same kind of the 1st) after '='
            // "double quote 2 index"
            final boolean foundDQ1Index = dq1Index > eqIndex;
            int dq2Index = foundDQ1Index ?
                 findIndexOfChar(buf, dq1Index+1, to, buf.charAt(dq1Index)) : NOT_FOUND;

            // If the attribute is not enclosed with quotes, then parse the
            // attr value assuming the value without space.
            final boolean isQuoted = (dq1Index != NOT_FOUND);
            if (!isQuoted) {
                // find the first non-whitespace char after "=".
                dq1Index = skipWhiteSpace(buf, eqIndex+1, to);
                // find the end index of the token, delimitered by whitespace
                // and xml end tag
                final boolean foundUnquotedDQ1Index = dq1Index > eqIndex;
                dq2Index = foundUnquotedDQ1Index ?
                           findEndTokenIndex(buf, dq1Index, to) : NOT_FOUND;
            }

            // sanity check
            if (eqIndex <= p || eqIndex >= to ||
                dq1Index <= eqIndex || dq1Index >= to ||
                dq2Index <= dq1Index || dq2Index > to)
            {
                // cannot find the definition of an attribute
                throw new HTMLSyntaxException(
                    Fmt.S("HTML.escapeUnsafe: Unmatched quotes found in %s",
                        buf.substring(from, to)));
            }

            // we finally have enough information to figure out the attribute name and value
            String attr = buf.substring(p, eqIndex).trim();
            // attribute value
            String value = (isQuoted ?
                            buf.substring(dq1Index+1, dq2Index).toLowerCase() :
                            buf.substring(dq1Index, dq2Index+1).toLowerCase());
            // check if the value contains any unsafe string
            if (!isSafeAttributeValue(attr, value)) {
                throw new HTMLSyntaxException(
                    Fmt.S("HTML.escapeUnsafe: Attribute %s=%s is not safe.",
                        attr, value));
            }
            boolean isSafe = false;
            for (int j = 0; j < safeAttrs.length; ++j) {
                if (attr.equalsIgnoreCase(safeAttrs[j])) {
                    p = dq2Index;
                    isSafe = true;
                    break;
                }
            }

            // another safety check
            if (!isSafe) {
                throw new HTMLSyntaxException(
                    Fmt.S("HTML.escapeUnsafe: Unsafe attribute %s found.",
                        attr));
            }
        }
    }

    /**
       Returns the first index of a char in the buffer in the range of [from, to]
       This is a helper function to processAttrs(...).
    */
    private static int findIndexOfChar (FastStringBuffer buf, int from, int to, char c)
    {
        int p = from-1;
        while (++p < to)
            if (c == buf.charAt(p)) {
                return p;
            }
        return NOT_FOUND;
    }

    /**
     * Return the first non-whitespace char in the buffer in range of [from, to]
     * This is a helper function to processAttrs(...).
     */
    private static int skipWhiteSpace (FastStringBuffer buf, int from, int to)
    {
        int p = from-1;
        while (++p <= to)
            if (!Character.isWhitespace(buf.charAt(p))) {
                return p;
            }
        return NOT_FOUND;
    }

    /**
     Returns the index of the first quote in the buffer in the range of [from, to]
     This is a helper function to processAttrs(...).
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
        return NOT_FOUND;
    }

    /**
     * Return the first whitespace char or XML end tag char
     * in the buffer in range of [from, to].  It is expected buf[from] has
     * no leading white space. This is a helper function to processAttrs(...).
     */
    private static int findEndTokenIndex (FastStringBuffer buf, int from, int to)
    {
        final String regex = "\\s+|(?:/?\\s*>)";
        final String attributeValuePairs = buf.substring(from, to + 1);
        final Pattern pattern = CompiledRegexCache.get(regex);
        final Matcher matcher = pattern.matcher(attributeValuePairs);
        if (matcher.find()) {
            return from + matcher.start();
        }
        return NOT_FOUND;
    }

    private static Pattern linebreaksToBr = Pattern.compile("\\r?\\n");
    public static String linebreaksToBRTags (String input)
    {
        return linebreaksToBr.matcher(input).replaceAll("<br />");
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

    /*
       Escaping unsafe tags and attributes

       safeTags and safeAttrs stores the definitions of safe tags and attributes
       unsafeAttrValuesInTag defines unsafe substrings in attribute values in tags
       unsafeAttrValues defines unsafe substrings in standalone attribute values
       safeConfigDefined will be true if any of the two arrays is not empty
    */
    private static String[] safeTags = new String[0];

    private static String[] safeAttrs = new String[0];

    private static final String[] unsafeAttrValuesInTag = {
        "javascript\\s*:",
        "vbscript\\s*:",
        "url\\s*\\(",
        "expression\\s*\\("        
    };

    private static final String[] unsafeAttrValues = {
        "javascript\\s*:",
        "vbscript\\s*:",
        "url\\s*\\(",
        "expression\\s*\\(",
        "\""
    };

    private static final List<Pattern> unsafeAttributeValuePatterns =
        ListUtil.list(unsafeAttrValues.length);

    static {
        for (String regex : unsafeAttrValues) {
            int patternFlags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
            Pattern pattern = Pattern.compile(regex, patternFlags);
            unsafeAttributeValuePatterns.add(pattern);
        }
    }

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

    public static boolean isSafeAttributeValue (String value)
    {
        for (Pattern pattern : unsafeAttributeValuePatterns) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                // matcher.find() also changes the state of the matcher on success
                // see matcher.reset() if the matcher object is ever reused 
                return false;
            }
        }
        return true;
    }

    public static boolean isSafeAttributeValue (String attribute, String value)
    {
        final boolean hasComments = attributeCanHaveCommentsInValue(attribute);
        String strippedValue = value;
        if (hasComments) {
            String commentRegex = "/\\*.*?\\*/";
            Pattern commentPattern = CompiledRegexCache.get(commentRegex);
            strippedValue = commentPattern.matcher(value).replaceAll("");
        }
        return isSafeAttributeValue(strippedValue);
    }

    /**
     * We expect the implementation to grow in the future.
     *
     * @param attribute
     * 
     * @return
     */
    private static boolean attributeCanHaveCommentsInValue (String attribute)
    {
        return "style".equalsIgnoreCase(attribute);
    }

    private static class HTMLSyntaxException extends RuntimeException
    {
        HTMLSyntaxException (String msg)
        {
            super(msg);
        }
    }

    /**
        safeTagBegin and safeTagEnd are the custom escape sequences used for tags
        stb and ste are shorthands for safeTagBegin and safeTagEnd
     */
    private static final String safeTagBegin = "&STB;", stb = safeTagBegin;
    private static final String safeTagEnd = "&STE;", ste = safeTagEnd;

    /**
        This segment of regex will cause the entire match to fail
        if we aren't actually in a tag (if we're in content).
        It means: match until we reach the beginning or end of a tag,
        then, if the next substring isn't the end of a tag, fail.
        The following forms won't match and cause entire regex to fail:
        href=content<nextTag>
        href= "more content" &stb;
        These will match and allow the entire match to succeed:
        attribute="value">
        attribute = "value" attribute2=value&ste;
        attribute=HeavyEmphasis!!!!!!!!!!!!!!!!!!&ste;

        Before reading the pattern below, remember that:
        ?= is lookahead and match (note that lookaheads don't capture)
        ?! is lookahead and don't match
        ?: is the "non-capturing" directive.

        The idiom (?:(?!regex).)* means match until regex.
        It is similar to [^chars] but much less intuitive.
    */
    private static final String endOfTagRegex =
        "(?=(?:(?![<>]|"+stb+"|"+ste+").)*(?:>|"+ste+"))";

    /**
        This string holds a regex that will match a HTML string literal.
        It takes into account quoted and unquoted values.
        See {@link #endOfTagRegex} for regex documentation.
     */
    private static final String stringLiteral =
        // This part matches a quoted string literal
        "(?:(?:\\s*\"[^\"]*\")|" +
        // This next bit allows unquoted attributes to match.
        // The following forms won't match:
        // href = stuff
        // href =/>
        // href =&stb;
        // href =&ste;
        // It will allow matches to:
        // href =stuff
        "(?:(?![\\s<>]|/\\s*>|"+stb+"|"+ste+").)*)";

    // This is intended for use by HTMLTest.java only
    public static String getSafeTagBegin ()
    {
        return safeTagBegin;
    }

    // This is intended for use by HTMLTest.java only
    public static String getSafeTagEnd ()
    {
        return safeTagEnd;
    }

    /**
        Filters out any unsafe HTML and returns a safe HTML string
        @aribaapi private
    */
    public static String filterUnsafeHTML (String str)
    {
        String filteredString = Filterer.filterUnsafeHTML(str);
        return filteredString;
    }

    /**
     * This filters out css margins in html attribute values.
     * @aribaapi private
     */
    public static String filterMargins (String html)
    {
        return Filterer.filterCSSMargins(html);
    }

    /**
     * @see #linksOpenInNewWindow
     */
    private static final Pattern linksWithoutTargetsPattern =
        createLinksWithoutTargetsPattern();

    /**
     * This will return an html string where links (a tags) without a target attribute
     * have a target attribute inserted with a value of _blank.  This makes the tag open
     * the link in a new window or tab, as specified in the browser's settings.
     * @param html The HTML string to modify.
     * @return A new HTML string where the links open in a new window or tab.
     */
    public static String linksOpenInNewWindow (String html)
    {
        String str = "<a$1 target=\"_blank\"$2";
        return linksWithoutTargetsPattern.matcher(html).replaceAll(str);
    }

    /**
     * @see #linksOpenInNewWindow
     * @see #linksWithoutTargetsPattern
     * @return
     */
    private static Pattern createLinksWithoutTargetsPattern ()
    {
        String regex;
        /* basic form
            regex = "<a[^>]*>";
            fail on target=
            regex = "<a((?:(?!target).)*)"+regexIsInTag;
        */
        // unless it's in a value
        String attributeValuePairRegex = "\\s*(?:(?!(?:target|[<>=])).)*\\s*="
            + stringLiteral;
        String avpr = attributeValuePairRegex;
        regex = "<a((?:"+avpr+")*)\\s*(/?\\s*>)";
        return Pattern.compile(regex, Pattern.DOTALL);
    }

    private static final Pattern htmlTagAtStartPattern =
            Pattern.compile("\\G\\s*(<\\s*([^\\s/>]*)[^>]*>)");
    public static String trimHTML (String html)
    {
        Pattern pattern = htmlTagAtStartPattern;
        Matcher matcher = pattern.matcher(html);
        FastStringBuffer buffer = new FastStringBuffer();
        int index = 0;
        while (matcher.find()) {
            index = matcher.end(1);
            String tagName = matcher.group(2);
            if (!tagName.equals("br") && !tagName.equals("p")) {
                buffer.append(html.substring(matcher.start(1), matcher.end(1)));
            }
        }
        buffer.append(html.substring(index));
        return buffer.toString();
    }

    // This is used by convertToPlainText(...) and fullyConvertToPlainText(...).
    private static final Pattern RemoveTagsPattern =
        Pattern.compile("<[^<]*?>", Pattern.MULTILINE);

    public static String convertToPlainText (String richText)
    {
        if (richText == null) {
            return null;
        }
        return RemoveTagsPattern.matcher(richText).replaceAll("");
    }

    /**
     * Very simple heuristic as to whether <code>text</code> is HTML. Not
     * very reliable.
     * @param text
     * @return
     */
    public static boolean isProbablyHtml (String text)
    {
        return RemoveTagsPattern.matcher(text).find();
    }

    /**
     * Convert a string to plain text and convert encoded safe html tags
     *
     * @param text
     * @return converted string
     */
    public static String fullyConvertToPlainText (String text)
    {
        String converted = text.replaceAll("(<br/>)|(<br>)|(</div>)|(</[p|P]>)","\n");
        converted = converted.replaceAll(">>",">&gt;");
        converted = RemoveTagsPattern.matcher(converted).replaceAll("");

        // Todo: Fix slow, multi-match, impl!  (Should create one (precomputed) combo regex)
        for (Map.Entry<String,String> e : safeHtmlCharacterMap.entrySet()) {
            converted = converted.replaceAll(e.getKey(), e.getValue());
        }

        return converted;
    }

    /**
     * Convert a string to plain text by removing all html including encoded html
     * this method really attempts to take the string
     * and extract the html markup even in the case
     * that the client generated the html and xml
     * encoded it so thats its not markup but instead content
     * so we go through and look for that content that
     * looks like markup and strip it out.
     *
     * @param text
     * @return converted string
     */
    public static String convertToPlainTextNoHTML (String text)
    {
        String converted = text.replaceAll("&amp;","&");
        for (Map.Entry<String,String> e : safeHtmlCharacterMap.entrySet()) {
            converted = converted.replaceAll(e.getKey(), e.getValue());
        }
        converted = converted.replaceAll("(<br/>)|(<br>)|(</div>)|(</[p|P]>)","\n");
        converted = RemoveTagsPattern.matcher(converted).replaceAll("");

        return converted;
    }

    // todo: move to MapUtil
    // This is used to initialize safeHtmlCharacterMap
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

    private abstract static class CompiledRegexCache
    {
        private static Pattern get(String regex)
        {
            if (regexCache.containsKey(regex)) {
                return regexCache.get(regex);
            }
            else {
                final int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
                final Pattern pattern = Pattern.compile(regex, flags);
                regexCache.put(regex, pattern);
                return pattern;
            }
        }

        private static Map<String, Pattern> regexCache = MapUtil.map();
    }

    private abstract static class Filterer
    {

        private static final Pattern unsafeHTMLPattern = createUnsafeHTMLPattern();
        private static final String escapedEquals = "&AEQ;";
        private static final Pattern _slashEndOfTagPattern = Pattern.compile("^/\\s*>");
        private static final ThreadLocal<Matcher> _slashEndOfTagMatchers =
            new ThreadLocal<Matcher>()
        {
            protected Matcher initialValue ()
            {
                return _slashEndOfTagPattern.matcher("");
            }
        };

        /**
            This function encodes equals signs found in HTML attribute values.
            Technically it will mistakenly encode equals signs outside of html tags
            but after unencoding it becomes irrelevant.
            This is a helper function for filterUnsafeHTML().
            @aribaapi private
        */
        private static String encodeEqualsInStringLiteral (String str)
        {
            FastStringBuffer buf = null;
            boolean inQuotedStringLiteral = false;
            boolean inRawStringLiteral = false;
            for (int i = 0; i < str.length(); i++)
            {
                char c = str.charAt(i);
                if ( c == '=') {
                    if (inRawStringLiteral || inQuotedStringLiteral) {
                        if (buf == null) {
                            buf = new FastStringBuffer(str.substring(0, i));
                        }
                        buf.append(escapedEquals);
                        // do not append =
                        continue;
                    }
                    else {
                        inRawStringLiteral = true;
                    }
                }
                else if ( c == '\"') {
                    inQuotedStringLiteral = ! inQuotedStringLiteral;
                    inRawStringLiteral = false;
                }
                else if (Character.isWhitespace(c) || c == '>' ) {
                    // white space, > or /> end the raw string literal
                    inRawStringLiteral = false;
                }
                else if (c == '/') {
                    Matcher matcher = _slashEndOfTagMatchers.get();
                    matcher.reset(str);
                    if (matcher.find(i)) {
                        inRawStringLiteral = false;
                    }
                }
                if (buf != null) {
                    buf.append(c);
                }
            }
            return buf != null ? buf.toString() : str;
        }

        /**
           Undoes the work of {@link #encodeEqualsInStringLiteral(String)}.
           @aribaapi private
        */
        private static String unencodeEqualsInStringLiteral (String str)
        {
            return str.replaceAll(escapedEquals, "=");
        }

        /**
           Creates and returns a compiled efficient regular expression.
           Removes the tags and content of HTML/XML comments, iframes,
           scripts and styles.  This is designed to be used internally by
           filterUnsafeHTML.
           @pre Any HTML is well formed and valid content is HTML escaped correctly.
           @return Returns a compiled pattern.
           @aribaapi private
         */
        private static Pattern createUnsafeHTMLPattern ()
        {
            // Flag anything commented out for removal, iframes, scripts and styles
            // Also flag the content
            String regex = "(?:<!--.*?-->)";
            regex += "|(?:<\\s*iframe[^>]*>[^<]*<\\s*/\\s*iframe\\s*>)";
            regex += "|(?:<\\s*script[^>]*>[^<]*<\\s*/\\s*script\\s*>)";
            regex += "|(?:<\\s*style[^>]*>[^<]*<\\s*/\\s*style\\s*>)";

            // Include all remaining tags, but keep their content
            // match all opening tags, closing tags and tags w/o content
            //            <    /      aTag     /   >
            regex += "|(?:<\\s*/?\\s*[^\\s>]++[^>]*>)";
            // remove all remaining attributes
            //                 theAttr=
            regex += "|(?:\\s+[^\\s=]+\\s*=" + stringLiteral + endOfTagRegex + ")";
            return Pattern.compile(regex, Pattern.DOTALL);
        }
        
        private static List<Pattern> unsafeAttributeValueInTagPatterns =
            ListUtil.list();

        private static Pattern safeTagPattern =
            Pattern.compile(stb+"((?:(?!"+ste+").)*)"+ste, Pattern.DOTALL);

        static {
            for (String regex : unsafeAttrValuesInTag) {
                String regexInTag = Fmt.S("(%s)%s", regex, endOfTagRegex);
                final int flags = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
                Pattern pattern = Pattern.compile(regexInTag, flags);
                unsafeAttributeValueInTagPatterns.add(pattern);
            }
        }

        public static String filterUnsafeHTML (String str)
        {
            // specially escape some equal signs(=)
            str = encodeEqualsInStringLiteral(str);
            str = safeTagPattern.matcher(str).replaceAll("&amp;STB;$1&amp;STE;");

            // prevent safe tags from being stripped
            for (String safeTag : safeTags) {
                String safeTagPattern =
                    Fmt.S("<\\s*(/?\\s*(?i:%s)(?=[\\s/>])[^>]*)>", safeTag);
                str = str.replaceAll(safeTagPattern, stb + "$1" + ste);
            }

            // prevent safe attributes from being stripped
            for (String safeAttributes : safeAttrs) {
                Pattern safeAttributePattern = getSafeAttributePattern(safeAttributes);
                str = safeAttributePattern.matcher(str).replaceAll(" @sf@$1@sf@$2@sf@");
            }

            // remove all remaining unsafe HTML tags and attributes
            String safeHTML = unsafeHTMLPattern.matcher(str).replaceAll(" ");

            // invalidate unsafe substrings in HTML attribute values
            for (Pattern pattern : unsafeAttributeValueInTagPatterns) {
                safeHTML = pattern.matcher(safeHTML).replaceAll("x$1");
            }

            // bring back the safe HTML
            safeHTML = safeTagPattern.matcher(safeHTML).replaceAll("<$1>");
            safeHTML = safeHTML.replaceAll("@sf@(.*?)@sf@(.*?)@sf@", "$1=$2");
            safeHTML = unencodeEqualsInStringLiteral(safeHTML);
            safeHTML = safeHTML.trim();
            // class="MsoNormal" will come from copy & pasted MSWord documents
            // we eliminate the css that goes with MsoNormal and eliminate the string as well
            safeHTML = safeHTML.replaceAll(" class=\"MsoNormal\"", "");
            return safeHTML;
        }


        /**
            Returns a pattern for finding a <code>safeAttr</code> in a string.
        */
        private static final Pattern getSafeAttributePattern (String safeAttr)
        {
            return Pattern.compile(
                // This first bit matches the initial tag and quoted attributes.
                //        href    =     "stuff"
                Fmt.S("\\s(%s)\\s*=(" + stringLiteral + ")" + endOfTagRegex, safeAttr),
                Pattern.DOTALL);
        }

        /**
         * This holds the pattern to filter css margins in html attribute values.
         */
        private static final Pattern filterMarginsPattern = createFilterMarginsPattern();

        /**
         * This filters out css margins in html attribute values.
         * @aribaapi private
         */
        public static String filterCSSMargins (String str)
        {
            return filterMarginsPattern.matcher(str).replaceAll(" ");
        }

        /**
            This creates the pattern used by filterMargins().
            This function assumes that the attribute is quoted.
        */
        private static Pattern createFilterMarginsPattern ()
        {
            // Remember:
            // (?>regex) is an independent group, it "locks in" the match
            //     and disables backtracking here for efficiency.
            // ?+ *+ and ++ are the possessive quantifiers and also disable backtracking

            // This part matches the margin property.
            //                 margin-left  :0 3px 10 em ;
            String regex = "(?>margin[^<:]*+:[^\\\";<>]++);?+" + endOfTagRegex;
            return Pattern.compile(regex, Pattern.DOTALL);
        }
    }
}
