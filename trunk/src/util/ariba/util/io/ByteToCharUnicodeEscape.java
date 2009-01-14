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

    $Id: //ariba/platform/util/core/ariba/util/io/ByteToCharUnicodeEscape.java#3 $
*/

package ariba.util.io;
import sun.io.ConversionBufferFullException;
import sun.io.MalformedInputException;
import sun.io.ByteToCharConverter;

/**
    Translates ASCII including \u0020 Unicode escapes to Unicode
    @aribaapi private
*/
class ByteToCharUnicodeEscape extends ByteToCharConverter
{
    /**
        UnicodeEscape is the name of my encoding use by Java sources
    */
    public String getCharacterEncoding ()
    {
        return "UnicodeEscape";
    }

    private static final int StateNormal    = 0;
    private static final int StateBackslash = 1;
    private static final int StateU         = 2;
    private static final int StateFirst     = 3;
    private static final int StateSecond    = 4;
    private static final int StateThird     = 5;

    private int state = StateNormal;

    private char escapedChar;
    
    /**
        Resets converter to its initial state.
    */
    public void reset ()
    {
        byteOff = 0;
        charOff = 0;
        state   = StateNormal;
    }

    /**
        Converts an array of bytes containing characters in an
        external encoding into an array of Unicode characters using
        Ariba's internal encoding . This method allows a buffer by
        buffer conversion of a data stream. The state of the
        conversion is saved between calls to convert. Among other
        things, this means multibyte input sequences can be split
        between calls. If a call to convert results in an exception,
        the conversion may be continued by calling convert again with
        suitably modified parameters. All conversions should be
        finished with a call to the flush method.

        @param bytes  byte array containing text to be converted. 
        @param bstart  begin conversion at this offset in input byte array. 
        @param bend  stop conversion at this offset in input array (exclusive). 
        @param chars  character array to receive conversion result. 
        @param cstart  start writing to output array at this offset. 
        @param cend  stop writing to output array at this offset (exclusive). 
        @return the number of bytes written to the output array. 

        @exception MalformedInputException if the input buffer
        contains any sequence of bytes that is illegal for the input
        character set.
        @exception ConversionBufferFullException if output array is
        filled prior to converting all the input.
    */
    public int convert (byte[] bytes,
                        int    bstart,
                        int    bend,
                        char[] chars,
                        int    cstart,
                        int    cend)
      throws MalformedInputException, ConversionBufferFullException
    {
        byteOff = bstart;
        charOff = cstart;
        int count = 0;
        for (; byteOff < bend ; byteOff++) {
            byte by = bytes[byteOff];
            switch (state) {
              case StateNormal: {
                  if (by == '\\') {
                      state = StateBackslash;
                      continue;
                  }
                  if (charOff >= cend) {
                      throw new ConversionBufferFullException();
                  }
                  chars[charOff++] = (char)by;
                  count++;
                  continue;
              }
              case StateBackslash: {
                  if (by == 'u') {
                      state = StateU;
                      continue;
                  }
                  if (charOff+1 >= cend) {
                      throw new ConversionBufferFullException();
                  }
                  chars[charOff++] = '\\';
                  chars[charOff++] = (char)by;
                  count+=2;
                  state = StateNormal;
                  break;
              }
              case StateU: {
                  int digit = Character.digit((char)by, 16);
                  if (digit == -1) {
                      throw new MalformedInputException();
                  }
                  escapedChar = (char)(digit << 12);
                  state = StateFirst;
                  break;
              } 
              case StateFirst: {
                  int digit = Character.digit((char)by, 16);
                  if (digit == -1) {
                      throw new MalformedInputException();
                  }
                  escapedChar |= digit << 8;
                  state = StateSecond;
                  break;
              }
              case StateSecond: {
                  int digit = Character.digit((char)by, 16);
                  if (digit == -1) {
                      throw new MalformedInputException();
                  }
                  escapedChar |= digit << 4;
                  state = StateThird;
                  break;
              }
              case StateThird: {
                  int digit = Character.digit((char)by, 16);
                  if (digit == -1) {
                      throw new MalformedInputException();
                  }
                  escapedChar |= digit << 0;

                  if (charOff >= cend) {
                      throw new ConversionBufferFullException();
                  }
                  chars[charOff++] = escapedChar;
                  count++;

                  state = StateNormal;
                  break;
              }
              default: {
                  throw new MalformedInputException();
              }
            }
        }
        return count;
    }

    /**
        Clear state of this converter.
    */
    public int flush (char[] chars, int cstart, int cend)
      throws MalformedInputException
    {
        if (state != StateNormal) {
            throw new MalformedInputException();
        }
        reset();
        return 0;
    }
}
