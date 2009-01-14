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

    $Id: //ariba/platform/util/core/ariba/util/core/VectorEnumerator.java#7 $
*/

package ariba.util.core;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
    @aribaapi private
*/
class VectorEnumerator implements Enumeration
{
    Vector vector;
    int    index;

    VectorEnumerator(Vector vector) {
        this.vector = vector;
        index = 0;
    }

    VectorEnumerator(Vector vector, int index) {
        this.vector = vector;
        this.index = index;
    }

    public boolean hasMoreElements ()
    {
        return index < vector.size();
    }

    public Object nextElement ()
    {
        if (index >= vector.size()) {
            throw new NoSuchElementException();
        }
        return vector.get(index++);
    }
}
