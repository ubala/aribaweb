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

    $Id: //ariba/platform/util/core/ariba/util/io/ObjectInputStream.java#4 $
*/

package ariba.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.util.Locale;

/**
    @aribaapi private
*/
public class ObjectInputStream extends java.io.ObjectInputStream
{
    public ObjectInputStream (InputStream in)

      throws IOException
    {
        super(in);
    }

    protected void readStreamHeader ()
      throws IOException, StreamCorruptedException
    {
            // intentionally left empty to override the superclass's method
            // which will cause a deadlock for streams attached to networks.
    }

    public Locale readLocale () throws IOException
    {
        return SerializeUtil.readLocale(this);
    }
}
