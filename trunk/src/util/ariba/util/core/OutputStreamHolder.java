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

    $Id: //ariba/platform/util/core/ariba/util/core/OutputStreamHolder.java#2 $
*/
package ariba.util.core;

import java.io.OutputStream;
import java.io.IOException;

/**
    Represents the capability to hold an <code>OutputStream</code>. <p>

    @aribaapi ariba
*/
public interface OutputStreamHolder
{
    /**
        Returns an <code>OutputStream</code>. <p/>

        No guarantee is offered as to whether this is a new stream on
        whatever the underlying resource is or whether the same stream handle
        may be returned from multiple calls to this method. However, a typical
        scenario would be that an instance of this interface would only be
        available to one client and that client would use it once.
        The semantics are left intentionally weak to allow different types of
        use of this interface. <p/>

        <b>Important note:</b> The obligation is on the caller to close the
        returned stream when he is finished with it. <p/>

        @throws IOException if an exception occurs while creating or opening
                the stream
        @aribaapi ariba
    */
    public OutputStream getOutputStream () throws IOException;

}
