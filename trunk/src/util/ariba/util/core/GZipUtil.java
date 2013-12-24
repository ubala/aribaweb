/*
    Copyright (c) 2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/GZipUtil.java#2 $

    Responsible: cwwilkinson
*/
package ariba.util.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class to perform gzip on strings, byte arrays, etc.
 */
public abstract class GZipUtil
{
    /**
     * Compresses the string value into an array of bytes.
     *
     * @param value -  the string value to compress
     * @return byte[] - the array of bytes containing the compressed value.
     * @throws RuntimeException - if there is a decompression issue (rare).
     */
    public static byte[] compress (String value) throws RuntimeException
    {

        try {
            if (StringUtil.nullOrEmptyOrBlankString(value)) {
                return value.getBytes("UTF-8");
            }
            return compress(value.getBytes("UTF-8"));
        }
        catch (IOException e) {
            throw new IllegalStateException("cannot compress " + value, e);
        }
    }

    /**
     * Compresses the byte array given using GZIP.
     *
     * @param value - the uncompressed data
     * @return the compressed bytes.  if the input is null or zero length, the input is
     *         returned.
     * @throws RuntimeException - if there is a problem.
     */
    public static byte[] compress (byte[] value) throws RuntimeException
    {
        try {
            if (value == null || value.length == 0) {
                return value;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GZIPOutputStream gzip;

            gzip = new GZIPOutputStream(out);
            gzip.write(value);
            gzip.close();
            return out.toByteArray();
        }
        catch (IOException e) {
            throw new IllegalStateException("cannot compress " +
                  Arrays.toString(value), e);
        }
    }

    /**
     * Decompresses an array of bytes.
     *
     * @param bytes -  the compressed bytes to decompress.
     * @return byte[] - the array of bytes containing the decompressed value.
     * @throws RuntimeException - if there is a decompression issue (rare).
     */
    public static byte[] decompress (byte[] bytes) throws RuntimeException
    {
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        try {
            InputStream in = getInputStream(bytes);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            while (in.available() > 0) {
                bos.write(in.read());
            }
            byte[] data = bos.toByteArray();
            return data;
        }
        catch (IOException e) {
            throw new IllegalStateException("cannot decompress ", e);
        }

    }

    /**
     * Returns input stream to decompress the data as read.
     *
     * @param data the compressed bytes
     * @return InputStream, never null.
     * @throws IOException
     */
    public static InputStream getInputStream (byte[] data) throws IOException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream retval = new GZIPInputStream(bis);
        return retval;
    }
}
