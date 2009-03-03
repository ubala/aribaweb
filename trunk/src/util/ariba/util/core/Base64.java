/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/Base64.java#5 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
    Base64.  Utilities for base 64 decoding rfc-1521: Base64 Alphabet

    See <TT>
    http://info.internet.isi.edu/in-notes/rfc/files/rfc1521.txt
    </TT>
    
    @aribaapi private
*/
public class Base64
{
    public static final String ClassName = "ariba.util.core.Base64";

    public static final char PadChar = '=';

    private static final char[] EncodeMapChar = {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
    };

    private static final byte[] EncodeMap = new byte[128];
    private static final byte[] DecodeMap = new byte[128];

    static {
        for (int i = 0; i < EncodeMapChar.length; i++) {
                      EncodeMap[i]  = (byte)EncodeMapChar[i];
            DecodeMap[EncodeMap[i]] = (byte)i;
        }
    }

    /**
        This method decodes the given byte[] using the base64-encoding
        specified in RFC-1521 (Section 5.2).

        See <TT>
        http://info.internet.isi.edu/in-notes/rfc/files/rfc1521.txt
        </TT>

        @param  string the string to decode.
        @return the decoded string or null if there was a problem decoding
        
    */
    public static String decode (String string)
    {
        byte[] in  = string.getBytes();
        byte[] out = decode(in, 0, in.length);
        if (out == null) {
            return null;
        }
        return new String(out);
    }

    /**
        This method decodes the given byte[] using the base64-encoding
        specified in RFC-1521 (Section 5.2).

        See <TT>
        http://info.internet.isi.edu/in-notes/rfc/files/rfc1521.txt
        </TT>

        @param  data the base64-encoded data.
        @param  start the position in the array to start.
        @param  len the number of bytes to decode.
        @return the decoded <var>data</var>.
    */
    public static final byte[] decode (byte[] data, int start, int len)
    {
            // check for corner cases
        if (data == null) {
            return  null;
        }

        if ((len % 4) != 0) {
            return null;
        }

        int tailPos = len;
        while (data [start + tailPos - 1] == PadChar) {
            tailPos--;
        }

        byte dest[] = new byte[tailPos - len/4];

            // convert from 64 letter alphabet to 0-63 number
        for (int i = start; i < len; i++) {
            data [i] = DecodeMap[data [i]];
        }

            // 4 byte to 3 byte conversion
        int sourceIndex = start;
        int destIndex   = 0;
        for (; destIndex < dest.length-2; sourceIndex += 4, destIndex += 3) {

                // 6 bytes from first char + 2 of next char
            dest[destIndex]   = (byte)(((data [sourceIndex] << 2) & 255) |
                                       ((data [sourceIndex+1] >>> 4) & 003));

                // last 4 bytes from second char + 4 of next char
            dest[destIndex+1] = (byte)(((data [sourceIndex+1] << 4) & 255) |
                                       ((data [sourceIndex+2] >>> 2) & 017));

                // last 2 bytes from second char + 6 of next char
            dest[destIndex+2] = (byte)(((data [sourceIndex+2] << 6) & 255) |
                                       (data [sourceIndex+3] & 077));
        }

        if (destIndex < dest.length) {
            dest[destIndex]   = (byte)(((data[sourceIndex] << 2) & 255) |
                                       ((data[sourceIndex+1] >>> 4) & 003));
        }
        if (++destIndex < dest.length) {
            dest[destIndex]   = (byte)(((data[sourceIndex+1] << 4) & 255) |
                                       ((data[sourceIndex+2] >>> 2) & 017));
        }

        return dest;
    }

    /**
        This method encodes the given String using the base64-encoding
        specified in RFC-1521 (Section 5.2). Makes no provisions for
        encoding more than a line's worth of data (i.e. '\n' chars are
        not inserted into the encode output so you should keep the
        (len-start) <= 57 bytes. (This results in a maximum of
        (57/3)*4 or 76 characters per output line.)

        See <TT>
        http://info.internet.isi.edu/in-notes/rfc/files/rfc1521.txt
        </TT>

        @param  string data to be base64-encoded.
        @return the encoded <var>data</var> or null if there was a
        problem encoding
    */
    public static String encode (String string)
    {
        byte[] in  = string.getBytes();
        byte[] out = encode(in, 0, in.length);
        if (out == null) {
            return null;
        }
        return new String(out);
    }

    /**
        This method encodes the given byte[] using the base64-encoding
        specified in RFC-1521 (Section 5.2). Makes no provisions for
        encoding more than a line's worth of data (i.e. '\n' chars are
        not inserted into the encode output so you should keep the
        (len-start) <= 57 bytes. (This results in a maximum of
        (57/3)*4 or 76 characters per output line.)

        See <TT>
        http://info.internet.isi.edu/in-notes/rfc/files/rfc1521.txt
        </TT>

        @param  data data to be base64-encoded.
        @param  start the position in the array to start.
        @param  len the number of bytes to encode.
        @return the encoded <var>data</var>.
    */
    public static final byte[] encode (byte[] data, int start, int len)
    {
        if (data == null) {
            return null;
        }

        int sourceIndex=start;
        int destIndex=0;

        byte dest[] = new byte[((len+2)/3)*4];


            // 3-byte to 4-byte conversion + 0-63 to ascii printable conversion
        for (; sourceIndex < len-2; sourceIndex += 3) {
            dest[destIndex++] = EncodeMap[(data[sourceIndex] >>> 2) & 077];
            dest[destIndex++] = EncodeMap[(data[sourceIndex+1] >>> 4) & 017 |
                                         (data[sourceIndex] << 4) & 077];
            dest[destIndex++] = EncodeMap[(data[sourceIndex+2] >>> 6) & 003 |
                                         (data[sourceIndex+1] << 2) & 077];
            dest[destIndex++] = EncodeMap[data[sourceIndex+2] & 077];
        }
        if (sourceIndex < start+len) {
            dest[destIndex++] = EncodeMap[(data[sourceIndex] >>> 2) & 077];
            if (sourceIndex < start+len-1) {
                dest[destIndex++] = EncodeMap[(data[sourceIndex+1] >>> 4) & 017 |
                                             (data[sourceIndex] << 4) & 077];
                dest[destIndex++] = EncodeMap[(data[sourceIndex+1] << 2) & 077];
            }
            else {
                dest[destIndex++] = EncodeMap[(data[sourceIndex] << 4) & 077];
            }
        }

            // add padding
        for (; destIndex < dest.length; destIndex++)
            dest[destIndex] = (byte)PadChar;

        return dest;
    }

    /**
        The methods you actually wanted ...

         @param contents
         @return base-64 encoded message.
     */
    public static String encodeToString (byte[] contents)
    {
        return StringUtil.getStringUTF8(Base64.encode(contents,0,contents.length));
    }


    /**
        The method you wanted to have all along!

        @param string base-64 encoded string
        @return base-64 decoded bytes from the string
     */
    public static byte[] decodeFromString (String string)
    {
        byte[] bytes = StringUtil.getBytesUTF8(string);

        return Base64.decode(bytes, 0, bytes.length);
    }


    /**
        This method will Base64 encode the contents of a file and output it to
        a specified OutputStream. Closing the outputstream will NOT be handled
        within this method.
    */
    public static final void encodeAndSend (File         file,
                                            OutputStream outputStream)
    {
        BufferedInputStream bin = null;
        try {
            bin = IOUtil.bufferedInputStream(file);
            encodeAndSend(bin, outputStream);
        }
        catch (FileNotFoundException e) {
            Log.util.error(2752, file);
        }
        catch (IOException e) {
            Log.util.error(2753, file);
        }
        finally {
            if (bin != null) {
                try {
                    bin.close();
                }
                catch (IOException e) {
                    Log.util.error(2754, file);
                }
            }
        }
    }

    public static final int LineSize = 57;

    public static final void encodeAndSend (InputStream  inputStream,
                                            OutputStream outputStream)
      throws IOException
    {
        byte unencoded[] = new byte[LineSize];

        int count = 0;
        while (true) {
            int n = inputStream.read(unencoded, count, unencoded.length-count);
            if (n == -1) {
                writeEncoded(outputStream, unencoded, count);
                break;
            }
            count += n;
            if (count != LineSize) {
                continue;
            }
            writeEncoded(outputStream, unencoded, LineSize);
            count = 0;
        }
        outputStream.flush();
    }

    private static void writeEncoded (OutputStream outputStream,
                                      byte[]       unencoded,
                                      int          length)
      throws IOException
    {
        byte[] encoded = encode(unencoded, 0, length);
        outputStream.write(encoded, 0, encoded.length);
        outputStream.write('\n');
    }

    /**
        For testing, but not moved to test.ariba.util.core.* because
        it can be useful for debugging in the field with support.

        @aribaapi private
    */
    public static void main (String[] args)
    {
        PrintWriter out = SystemUtil.out();
        PrintWriter err = SystemUtil.err();

        boolean file;
        if (args.length == 1) {
            file = true;
        }
        else if (args.length == 2) {
            file = false;
        }
        else {
            usage(err);
            return;
        }

        String option = args[0];
        boolean encode;
        if (option.equals("-encode")) {
            encode = true;
        }
        else if (option.equals("-decode")) {
            encode = false;
        }
        else {
            usage(err);
            return;
        }

        if (file) {
            try {
                if (encode) {
                    encodeAndSend(System.in, System.out);
                }
                else {
                    while (true) {
                        String line = IOUtil.readLine(System.in);
                        if (line == null) {
                            break;
                        }
                        byte[] encoded = line.getBytes();
                        byte[] decoded = decode(encoded, 0, encoded.length);
                        if (decoded == null) {
                            SystemUtil.exit(1);
                        }
                        System.out.write(decoded); // OK
                    }
                }
                SystemUtil.exit(0);
            }
            catch (IOException ioe) {
                SystemUtil.exit(1);
            }
        }
        
        String string = args[1];
        if (encode) {
            Fmt.F(out, "encode(%s)=%s\n", string, encode(string));
            SystemUtil.exit(0);
        }
        else {
            Fmt.F(out, "decode(%s)=%s\n", string, decode(string));
            SystemUtil.exit(0);
        }
    }

    private static final void usage (PrintWriter err)
    {
        Fmt.F(err, "usage: %s -encode|-decode [string]\n", ClassName);
        SystemUtil.exit(1);
    }
}
