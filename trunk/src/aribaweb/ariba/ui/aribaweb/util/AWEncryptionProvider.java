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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWEncryptionProvider.java#2 $
*/
package ariba.ui.aribaweb.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
    Provides encryption services to the AW package.

    THe implementation is provided by the AWApplication

 @aribaapi
*/

public abstract class AWEncryptionProvider
{
    private static AWEncryptionProvider provider;

    public static AWEncryptionProvider getProvider()
    {
        return provider;
    }

    public static void setProvider (AWEncryptionProvider p)
    {
        provider = p;
    }

    /**
          input stream wrapper that returns cleartext given a previously
          encrypted input stream.

          @param cipherStream input stream returning encrypted bytes
          @return input stream returning cleartext
    */
    abstract public InputStream getCleartextInputStream(InputStream cipherStream)
            throws IOException;

    /**
          Output stream wrapper that encrypts data before it is passed on to
          the wrapped cipherStream

          @param cipherStream output stream which received encrypted data
          @return output stream which will convert cleartext data into encrypted data
     */
    abstract public OutputStream getCipherOutputStream(OutputStream cipherStream)
            throws IOException;
}
