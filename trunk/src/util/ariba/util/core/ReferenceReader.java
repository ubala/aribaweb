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

    $Id: //ariba/platform/util/core/ariba/util/core/ReferenceReader.java#5 $
*/

package ariba.util.core;

import java.io.IOException;
import java.util.List;

/**
    Interface class for reading value of a parameter reference.

    @aribaapi ariba
*/
public interface ReferenceReader
{
    /**
        Initialize this class

        @param args input arguments, must be called first and only once after the implementing class
        is instantiated.

        @exception IOException if an exception is raised due to I/O error
        @exception ReferenceReaderException  raised when any errors other than I/O is encountered.

        @aribaapi ariba
    */
    public void init (List args)
      throws ReferenceReaderException, IOException;

    /**
        read the value of a given key

        @param key the key whose value is to be read. Must be non-null.

        @return the value read. <b>null</b> if the key does not exist.

        @exception ReferenceReaderException  raised when any errors is encountered during this operation.

        @aribaapi ariba
    */
    public Object read (Object key)
      throws ReferenceReaderException;
}
