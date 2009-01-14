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

    $Id: //ariba/platform/util/core/ariba/util/core/MIMESource.java#3 $
*/

package ariba.util.core;

import java.io.IOException;
import java.io.InputStream;

/**
    This interface provides the abstraction of MIME data source.
    It provides a content type for that data source as well as access to it 
    in the form of InputStream.

    @aribaapi ariba
*/
public interface MIMESource
{
    /**
        This method returns the MIME type of the data in the form of a string.
        It is suggested that getContentType could return 
        MIME.ContentTypeApplicationOctetStream 
        ("application/octet-stream") if the MIMESource 
        implementation can not determine the data type.

        @return the MIME Type

        @aribaapi ariba
    */
    public String getContentType ();


    /**
        This method returns an InputStream representing the the data and 
        will throw the appropriate exception if it cannot do so.

        @throws  IOException
        @return an InputStream

        @aribaapi ariba
    */
    public InputStream getInputStream () throws IOException;
}
