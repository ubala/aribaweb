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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWByteArrayOutputStream.java#5 $
*/

package ariba.ui.aribaweb.util;

import java.io.OutputStream;

public final class AWByteArrayOutputStream extends OutputStream
{
    private AWByteArray _byteArray;

    public AWByteArrayOutputStream (AWByteArray byteArray)
    {
        super();
        _byteArray = byteArray;
    }

    public AWByteArrayOutputStream (int initialSize)
    {
        this(new AWByteArray(initialSize));
    }

    public AWByteArrayOutputStream ()
    {
        this(new AWByteArray());
    }
    
    public void write (int byteValue)
    {
        // do nothing
    }

    public void write (byte[] bytes)
    {
        _byteArray.append(bytes, 0, bytes.length);
    }

    public void write (byte[] bytes, int offset, int length)
    {
        _byteArray.append(bytes, offset, length);
    }

    public byte[] toByteArray ()
    {
        return _byteArray.toByteArray();
    }

    public AWByteArray byteArray ()
    {
        return _byteArray;
    }
}
