/**
    Copyright (c) 1996-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/MaxEncodedString.java#4 $
    Responsible: rrao
*/
package ariba.util.core;

import ariba.util.core.MIME;
import ariba.util.core.Assert;
import ariba.util.log.Log;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ConcurrentHashMap;

public final class MaxEncodedString {
    
    /**
     * This is the maximum length in bytes for a VARCHAR2 column that can be created 
     * on Oracle.
     */
    public static final int MaxVC2Length = 4000;
    
    /**
     * If the first character in the value string is a space, SQLBuffer will add an
     * additional space in front of the string. We need to leave room of one byte to 
     * accommodate the additional space
     */
    public static final int MaxByteLength = (MaxVC2Length -1);
    
   
    private static final ConcurrentHashMap<String,CharsetEncoder> _encoders = 
                                new ConcurrentHashMap<String,CharsetEncoder>(3);
    
    private static final CharBufferPool _charBuffPool = 
                                new CharBufferPool(MaxVC2Length);
    
    private static final ByteBufferPool _byteBuffPool = 
                                new ByteBufferPool(MaxVC2Length);
    
    /**
     * 
     * @param src
     * @param srcBegin
     * @param maxByteLength
     * @param charsetName
     * @param byteBuff - out parameter that would hold the encoded bytes
     * @return max length of the string that can fit in the given database limit
     */
    public static int getMaxLength (String src, 
            int srcBegin, 
            int maxByteLength, 
            String charsetName, 
            ByteBuffer byteBuff)
    {
        if (!(srcBegin >= 0)) {
            Assert.that(false, "Offset cannot be negative");    
        }
        
        if (!(maxByteLength > 0)) {
            Assert.that(false, "maxByteLength should be > 0");
        }
        
        if (StringUtil.nullOrEmptyString(src)) {
            Log.i18n.debug("The input string was null. Returning 0");
            return 0;
        }
        
        if (maxByteLength > MaxByteLength) {
            maxByteLength = MaxByteLength;
        }
        
        CharsetEncoder encoder = getEncoder(charsetName);
        int maxBytesPerCharForCharset = (int)encoder.maxBytesPerChar();
        int inputStrLength = src.length();
        int substrLength = inputStrLength - srcBegin;
        
        if (Log.i18n.isDebugEnabled()) { 
            Log.i18n.debug("Input string: %s. \nMaxBytesPerChar: %s \nOffset: %s\n",
                src, maxBytesPerCharForCharset, srcBegin);
            Log.i18n.debug("Input string length: %s \ncharset: %s \nmaxByteLength: %s",
                inputStrLength, charsetName, maxByteLength);
            Log.i18n.debug("SubstrLength: %s",substrLength);
        }
        
        if (substrLength <= (maxByteLength/maxBytesPerCharForCharset)) {
            return substrLength;
        }
        
        /**
         * Fix 1-BQL4PT: There is no point in copying more than maxByteLength chars into
         * the CharBuffer. We will get a BufferOverFlowException if we try to copy more
         * than MaxVC2Length chars into the buffer; we must limit it. 
         * maxByteLength is guaranteed to be less than MaxVC2Length by the code above
         */
        if (substrLength > maxByteLength) {
            substrLength = maxByteLength;
        }

        CharBuffer charBuffer = _charBuffPool.get();
        ByteBuffer byteBuffer = (byteBuff != null ? byteBuff : _byteBuffPool.get());
        byteBuffer.limit(maxByteLength);
        
        int position = 0;
        try {
            charBuffer.put(src, srcBegin, substrLength);
            charBuffer.flip();
            encoder.encode(charBuffer, byteBuffer, true);
            position = charBuffer.position();
        }
        finally {
            _charBuffPool.release(charBuffer);
            _byteBuffPool.release(byteBuffer);
        }
        return position;
    }

    public static int getMaxLength (String src, int srcBegin, int maxByteLength)
    {
        return getMaxLength(src, srcBegin, maxByteLength, MIME.CharSetUTF, null);
    }

    public static final CharsetEncoder getEncoder (String charsetName)
    {
        CharsetEncoder encoder = _encoders.get(charsetName);
        if (encoder == null) {
            Charset charset = Charset.forName(charsetName);
            Assert.that(charset != null , "Invalid charset name %s", charsetName);
            encoder = charset.newEncoder();
            _encoders.put(charsetName, encoder);
        }
        return encoder;
    }

    /**
     * @param src
     * @param srcBegin
     * @param maxByteLength
     * @param charsetName
     * @return the maximum string that can fit in the given database limit
     */
    public static String getMaxLengthString (String src, 
            int srcBegin, int maxByteLength, String charsetName)
    {
        if (StringUtil.nullOrEmptyString(src)) {
            Log.i18n.debug("The input string was null. Returning null");
            return src;
        }
        
        srcBegin = (srcBegin < 0 ? 0 : srcBegin);
        int length = getMaxLength(src, srcBegin, maxByteLength, charsetName, null);
        int endIndex = srcBegin+length;
        if (Log.i18n.isDebugEnabled()) {
            Log.i18n.debug("srcBegin is %s", srcBegin);
            Log.i18n.debug("MaxLength is %s", length);
            Log.i18n.debug("Trying substring on %s. beginIndex: %s. endIndex: %s",src, 
                    srcBegin, endIndex);    
        }
        return src.substring(srcBegin, endIndex);
    }

    public static String getMaxLengthString (String src,
           int srcBegin, int maxByteLength)
    {
        return getMaxLengthString(src, srcBegin, maxByteLength, MIME.CharSetUTF);
    }
}
