package ariba.ui.aribaweb.core;

import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWContentType;

import java.io.Writer;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
    An <code>AWResponse</code> capable of populating an <code>OutputStream</code>.
*/

public class AWWriterResponse extends AWBaseResponse
{
    static {
        ClassUtil.classTouch("AWUtil");
    }

    public  static final String IndentChars = "    ";
    private AWCharacterEncoding _characterEncoding;
    private int _indentationLevel = 0;
    boolean _lastWasNewline = false;
    private Writer _writer;

    public AWWriterResponse(Writer writer, AWCharacterEncoding characterEncoding)
    {
        _characterEncoding = characterEncoding;
        _writer = writer;
    }

    public AWWriterResponse(Writer writer)
    {
        this(writer, AWCharacterEncoding.UTF8);
    }

    public void flush ()
    {
        if (_writer != null) {
            try {
                _writer.flush();
            }
            catch (IOException e) {

            }
        }
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return _characterEncoding;
    }

    /* Important: Don't forget to call the flush method after calling appendConent */
    public void appendContent (AWEncodedString encodedString)
    {
        if (encodedString != null) {
            String val = encodedString.toString();
            writeString(val, 0, val.length(), _characterEncoding.name);
        }
    }

    /* Important: Don't forget to call the flush method after calling appendConent */
    public void appendContent (String string)
    {
        if (string != null) {
            writeString(string, 0, string.length(), _characterEncoding.name);
        }
    }

    /**
        Write the given string from the specified range [beginIndex, onePastLast) and the charset,
        while taking care of indentation and triming beginning white spaces. It is assumed that
        0 <= beginIndex < onePastLast <= val.length(). Note that we don't check for this condition,
        the caller is responsible to make sure the above condition is valid.
        @param val the input string to output, must not be <code>null</code>
        @param beginIndex the index to the first char in the string to start processing
        @param onePastLast the index to the first char in the string to stop processing
        @param charSetName the charset name
    */
    private void writeString (String val, int beginIndex,
                              int onePastLast, String charSetName)
    {
        // if we're indenting, we need to scan for newlines so we can insert interposing indentation characters
        if (_indentationLevel > 0) {
            // scan for newlines
            int lastIndex = onePastLast;

            for (int i=beginIndex; i<lastIndex; i++) {
                if (val.charAt(i)=='\n') {
                    _lastWasNewline = true;
                }
                else {
                    // indent between the newline and the next non-newline character.
                    // NOTE: we need to wait until the next char in case the newline is followed
                    // by the end of an indentation block
                    if (_lastWasNewline) {
                        _lastWasNewline = false;
                        // flush what we have, indent, and recurse with what's ahead
                        _writeToStream(val, beginIndex,i, charSetName);

                        // indent
                        for (int j=0; j<_indentationLevel; j++) {
                            _writeToStream(IndentChars, 0,
                                           IndentChars.length(), charSetName);
                        }

                        // skip any other whitespace
                        while ((i < lastIndex) && (val.charAt(i) == ' ')) {
                            i++;
                        }

                        // is there anything left?
                        if (i < lastIndex) {
                            writeString(val, i,lastIndex, charSetName);
                            return;
                        }
                    }
                }
            }
        }
        _writeToStream(val, beginIndex, onePastLast, charSetName);
    }

    /**
        Writes valid chars from the specified input string beginning from a
        given index. Any invalid chars from the string within the specified range
        is skipped.
        @param val the input string, must not be <code>null</code>
        @param beginIndex the first index of the String to process
        @param endIndex the first index of the string to stop processing. EndIndex must
        be greater than beginIndex (note that we do not check for this).
        @param charSetName the charset name
    */
    private void _writeToStream (String val, int beginIndex,
                                 int endIndex, String charSetName)
    {
        /* lastIndex is the index the reference the start of valid data. As we parse the
            input string for invalid data, we remmember the lastIndex, and keep on parsing.
            When we find an invalid data (which will be at index i), we will write the good
            valid data in the interval [lastIndex, i).
        */
        int lastIndex=beginIndex;
        try {
            for (int i=beginIndex;i < endIndex; i++) {

                int cn = (int)val.charAt(i);
                /*
                    According to xml spec,
                    Char
                            ::=
                        #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
                */
                if (((cn < 32) && (cn != 9) &&
                    (cn != 10) &&
                    (cn != 13 )) ||
                    ((cn > 55295) && (cn < 57344)) ||
                    ((cn > 65533) && (cn < 65536)) ||
                    (cn > 1114111)) {
                    // Invalid Char
                    // write whatever we have and skip this
                    if (i > lastIndex) {
                        _writer.write(val, lastIndex, i-lastIndex);
                    }
                    lastIndex=i+1;
                }
            }
            // write the lsat batch of valid chars
            _writer.write(val,lastIndex, endIndex-lastIndex);
        }
        catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    public void setContentType (AWContentType contentType)
    {
        // This response does not require a content type.
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        _characterEncoding = characterEncoding;
    }

    /////////////////////////////////////////////
    // Unsupported AWResponse Interface API's
    /////////////////////////////////////////////
    public void init (AWCharacterEncoding characterEncoding)
    {
        throwNotSupported("init (AWCharacterEncoding characterEncoding)");
    }

    public void init ()
    {
        throwNotSupported("init ()");
    }

    public void startRefreshRegion (AWEncodedString refreshRegionName)
    {
        throwNotSupported("startRefreshRegion (AWEncodedString refreshRegionName)");
    }

    public void stopRefreshRegion (AWEncodedString refreshRegionName)
    {
        throwNotSupported("stopRefreshRegion (AWEncodedString refreshRegionName)");
    }

    public boolean hasRefreshRegions ()
    {
        throwNotSupported("hasRefreshRegions ()");
        return false;
    }

    public void setRefreshRegionPollInterval (int pollInterval)
    {
        throwNotSupported("setRefreshRegionPollInterval (int pollInterval)");
    }

    public void setPreviousResponse (AWResponse response)
    {
        throwNotSupported("setPreviousResponse (AWResponse response)");
    }

    public void appendContent (char contentChar)
    {
        throwNotSupported("appendContent (char contentChar)");
    }

    public void setContentFromFile (String filePath)
    {
        throwNotSupported("setContentFromFile (String filePath)");
    }

    public void setContent (byte[] bytes)
    {
        throwNotSupported("setContentFromFile (bytes[] bytes)");
    }

    public String contentString ()
    {
        throwNotSupported("contentString ()");
        return null;
    }

    public byte[] content ()
    {
        throwNotSupported("content ()");
        return null;
    }

    public void setHeaderForKey (String headerValue, String headerKey)
    {
        throwNotSupported("setHeaderForKey (String headerValue, String headerKey)");
    }

    public void setHeadersForKey (String[] headerValues, String headerKey)
    {
        throwNotSupported("setHeadersForKey (String[] headerValues, String headerKey)");
    }

    public AWContentType contentType ()
    {
        return AWContentType.TextXml;
    }

    public void setStatus (int statusCode)
    {
        throwNotSupported("setStatus (int statusCode)");
    }

    public void setBrowserCachingEnabled (boolean flag)
    {
        throwNotSupported("setBrowserCachingEnabled (boolean flag)");
    }

    public boolean browserCachingEnabled ()
    {
        throwNotSupported("browserCachingEnabled ()");
        return false;
    }

    public void disableClientCaching ()
    {
        throwNotSupported("disableClientCaching ()");
    }

    public AWCookie createCookie (String cookieName, String cookieValue)
    {
        throwNotSupported("createCookie (String cookieName, String cookieValue)");
        return null;
    }

    public void addCookie (AWCookie cookie)
    {
        throwNotSupported("addCookie (AWCookie cookie)");
    }

    public Map elementIdToSemanticKeyTable ()
    {
        throwNotSupported("elementIdToSemanticKeyTable ()");
        return null;
    }

    public Map semanticKeyToElementIdTable ()
    {
        throwNotSupported("semanticKeyToElementIdTable ()");
        return null;
    }

    public void _debugSetSemanticKeyMapping (byte[] bytes)
    {
        throw new AWGenericException(
            "Not supported: _debugSetSemanticKeyMapping (byte[] bytes)");
    }

    public byte[] _debugGetSemanticKeyMapping ()
    {
        throw new AWGenericException("Not supported: _debugGetSemanticKeyMapping ()");
    }

    public Map _debugHeaders ()
    {
        throw new AWGenericException("Not supported: _debugHeaders ()");
    }

    public void _debugSetRecordPlaybackParameters (AWRecordingManager recordingMgr,
                                                   boolean appendSemanticKeys)
    {
        throw new AWGenericException("Not supported: _debugSetRecordPlaybackParameters" +
                                     "(AWRecordingManager recordingMgr, " +
                                     "boolean appendSemanticKeys)");
    }

    ///////////////////////
    // AWReponseGenerating
    ///////////////////////
    public AWResponse generateResponse ()
    {
        return this;
    }

    public String generateStringContents ()
    {
        if (!(_writer instanceof StringWriter)) {
            throw new AWGenericException("Can't generate string for this type of writer: "
                        + _writer.getClass());
        }
        return _writer.toString();
    }

    //////////////
    // Util
    //////////////
    private void throwNotSupported (String methodDescription)
    {
        throw new AWGenericException("Not supported: " + methodDescription);
    }


    // Support for formatting -- automatic indentation
    // This is called by the AXIndenation tag
    public void incrementIndentationLevel ()
    {
        _indentationLevel++;
    }

    public void decrementIndentationLevel ()
    {
        _indentationLevel--;
        Assert.that(_indentationLevel >= 0,
                    "Indentation level should not go negative...");
    }
}
