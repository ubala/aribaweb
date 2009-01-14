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

    $Id: //ariba/platform/util/core/ariba/util/io/CharToByteUnicodeEscape.java#3 $
*/

package ariba.util.io;
import sun.io.CharToByteConverter;
import sun.io.ConversionBufferFullException;

/**
    Translates non-printable ASCII to \u0020 Unicode escapes
    @aribaapi private
*/
class CharToByteUnicodeEscape extends CharToByteConverter
{
    /**
        UnicodeEscape is the name of my encoding use by Java sources
    */
    public String getCharacterEncoding ()
    {
        return "UnicodeEscape";
    }

    public void reset ()
    {
        byteOff = 0;
        charOff = 0;
    }

    /**
        For non-printable ASCII, can return \ u 1 2 3 4 = 6 characters
    */
    public static final int MaxBytesPerChar = 6;

    /**
        For non-printable ASCII, can return \ u 1 2 3 4 = 6 characters
    */
    public int getMaxBytesPerChar ()
    {
        return MaxBytesPerChar;
    }

    /**
        Converts an array of Unicode characters into an array of bytes
        in Ariba's internal character encoding
    */
    public int convert (char[] chars,
                        int    cstart,
                        int    cend,
                        byte[] bytes,
                        int    bstart,
                        int    bend)
      throws ConversionBufferFullException
    {
        charOff = cstart;
        byteOff = bstart;
        while (charOff < cend) {
            char ch = chars[charOff];
            if ((' ' <= ch && ch <= '~') ||
                (ch == '\t')             ||
                (ch == '\r')             ||
                (ch == '\n'))
            {
                if (byteOff + 1 > bend) {
                    throw new ConversionBufferFullException();
                }
                bytes[byteOff++] = (byte)ch;
                charOff++;
                continue;
            }
            if (byteOff + MaxBytesPerChar > bend) {
                throw new ConversionBufferFullException();
            }
            bytes[byteOff++] = (byte)'\\';
            bytes[byteOff++] = (byte)'u';
            bytes[byteOff++] = (byte)Character.forDigit((ch&0xF000)>>> 12, 16);
            bytes[byteOff++] = (byte)Character.forDigit((ch&0x0F00)>>>  8, 16);
            bytes[byteOff++] = (byte)Character.forDigit((ch&0x00F0)>>>  4, 16);
            bytes[byteOff++] = (byte)Character.forDigit((ch&0x000F)>>>  0, 16);
            charOff++;
        }
        return byteOff - bstart;
    }

    /**
        No fancy state for this converter
    */
    public int flush (byte[] bytes, int bstart, int bend)
    {
        return 0;
    }

    /**
        Like UTF8, we can handle anything
    */
    public boolean canConvert (char ch)
    {
        return true;
    }
}
