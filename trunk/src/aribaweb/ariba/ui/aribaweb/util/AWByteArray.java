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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWByteArray.java#7 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import java.io.OutputStream;
import java.io.IOException;

public final class AWByteArray extends AWBaseObject
{
    private byte[] _byteArray;
    public int inUse;

    public AWByteArray ()
    {
        this(8);
    }

    public AWByteArray (int initialSize)
    {
        super();
        _byteArray = new byte[initialSize];
        inUse = 0;
    }

    public byte[] array ()
    {
        return _byteArray;
    }

    public byte[] toByteArray ()
    {
        byte[] bytes = new byte[inUse];
        System.arraycopy(_byteArray, 0, bytes, 0, inUse);
        return bytes;
    }

    private void grow (int newLength)
    {
        byte[] oldByteArray = _byteArray;
        int oldArraylength = oldByteArray.length;
        _byteArray = new byte[newLength];
        System.arraycopy(oldByteArray, 0, _byteArray, 0, oldArraylength);
    }

    private void grow ()
    {
        grow(_byteArray.length * 2);
    }

    public void addElement (byte byteValue)
    {
        if (inUse >= _byteArray.length) {
            grow();
        }
        _byteArray[inUse] = byteValue;
        inUse++;
    }

    public void append (byte[] bytes, int offset, int length)
    {
        int newLength = inUse + length;
        if (newLength > _byteArray.length) {
            grow(newLength + inUse);
        }
        System.arraycopy(bytes, offset, _byteArray, inUse, length);
        inUse += length;
    }

    public byte removeLastElement ()
    {
        inUse--;
        return _byteArray[inUse];
    }

    public byte removeElementAt (int index)
    {
        if (index >= inUse) {
            throw new ArrayIndexOutOfBoundsException(Fmt.S("%s >= %s", Constants.getInteger(index), Constants.getInteger(inUse)));
        }
        byte byteValue = _byteArray[index];
        int copyCount = inUse - index - 1;
        if (copyCount > 0) {
            System.arraycopy(_byteArray, index + 1, _byteArray, index, copyCount);
        }
        inUse--;
        return byteValue;
    }

    public boolean endsWith (byte[] targetEnding)
    {
        boolean endsWith = false;
        int targetLength = targetEnding.length;
        if (inUse >= targetLength) {
            int index = targetLength;
            int byteArrayOffset = inUse - targetLength;
            while (index > 0) {
                index--;
                byte currentTargetByte = targetEnding[index];
                byte currentArrayByte = _byteArray[byteArrayOffset + index];
                if (currentArrayByte != currentTargetByte) {
                    return false;
                }
            }
            endsWith = true;
        }
        return endsWith;
    }

    public void writeTo (OutputStream outputStream)
    {
        try {
            outputStream.write(_byteArray, 0, inUse);
        }
        catch (IOException ioexception) {
            throw new AWGenericException(ioexception);
        }
    }

    /*
        Shift the byte array numberOfElements "leftward" and truncate the byte array.
    */

    public void leftShiftElements (int numberOfElements)
    {
        if (numberOfElements > inUse) {
            numberOfElements = inUse;
        }
        inUse -= numberOfElements;
        System.arraycopy(_byteArray, numberOfElements, _byteArray, 0, inUse);
    }
}
