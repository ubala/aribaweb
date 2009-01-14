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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFastStringBuffer.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.StringArray;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class AWFastStringBuffer extends AWBaseObject
{
    private final int MaxEntries = 32 * 1024;
    private final StringArray _stringArray;
    private int _totalLength;

    public AWFastStringBuffer ()
    {
        super();
        _stringArray = new StringArray();
        _stringArray.makeRoomFor(128);
    }

    public AWFastStringBuffer (int initialCapacity)
    {
        super();
        _stringArray = new StringArray();
        _stringArray.makeRoomFor(initialCapacity);
    }

    public int length ()
    {
        return _totalLength;
    }

    public void clear ()
    {
        AWUtil.clear(_stringArray);
        _totalLength = 0;
    }

    public void append (String string)
    {
        if (string != null) {
            _stringArray.add(string);
            _totalLength += string.length();
            if (_stringArray.inUse() >= MaxEntries) {
                toString();
            }
        }
    }

    public void append (int intValue)
    {
        String intString = AWUtil.toString(intValue);
        append(intString);
    }

    private boolean equalsContents (String[] otherStringArray, int otherStringCount)
    {
        boolean equalsContents = false;
        int stringCount = _stringArray.inUse();
        if (stringCount == otherStringCount) {
            equalsContents = true;
            String[] stringArray = _stringArray.array();
            for (int index = 0; index < stringCount; index++) {
                String currentString = stringArray[index];
                String otherString = otherStringArray[index];
                if (!((currentString == otherString) || currentString.equals(otherString))) {
                    equalsContents = false;
                    break;
                }
            }
        }
        return equalsContents;
    }

    public boolean equals (Object otherStringBuffer)
    {
        boolean isEqual = true;
        if (otherStringBuffer != this) {
            if (otherStringBuffer instanceof AWFastStringBuffer) {
                isEqual = ((AWFastStringBuffer)otherStringBuffer).equalsContents(_stringArray.array(), _stringArray.inUse());
            }
            else {
                isEqual = false;
            }
        }
        return isEqual;
    }

    public char[] getChars ()
    {
        char[] charBuffer = new char[_totalLength];
        int currentBufferOffset = 0;
        String[] stringsArray = _stringArray.array();
        int stringCount = _stringArray.inUse();
        for (int index = 0; index < stringCount; index++) {
            String currentString = stringsArray[index];
            int currentStringLength = currentString.length();
            currentString.getChars(0, currentStringLength, charBuffer, currentBufferOffset);
            currentBufferOffset += currentStringLength;
        }
        return charBuffer;
    }

    public String toString ()
    {
        char[] charBuffer = getChars();
        String composedString = new String(charBuffer);
        AWUtil.clear(_stringArray);
        _stringArray.add(composedString);
        return composedString;
    }

    /* ----------------------
        Writing
        --------------------- */
    public void write (Writer writer)
    {
        String[] stringsArray = _stringArray.array();
        int stringCount = _stringArray.inUse();
        try {
            for (int index = 0; index < stringCount; index++) {
                String currentString = stringsArray[index];
                writer.write(currentString, 0, currentString.length());
            }
        }
        catch (IOException exception) {
            throw new AWGenericException(exception);
        }
    }

    public void writeTo (OutputStream outputStream)
    {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        write(outputStreamWriter);
        try {
            outputStreamWriter.close();
        }
        catch (IOException exception) {
            throw new AWGenericException(exception);
        }
    }
}
