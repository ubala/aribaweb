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

    $Id: //ariba/platform/util/core/ariba/util/core/IteratorUnion.java#3 $
*/

package ariba.util.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
    An enumeration that is the union of a list of enumerations.

    @aribaapi private
*/
public class IteratorUnion implements Iterator
{
    private Iterator[] _enumerations;
    private int _current = 0;

    /**
        Create an enumeration of e1 followed by e2.
    */
    public IteratorUnion (Iterator e1, Iterator e2)
    {
        _enumerations = new Iterator[2];
        _enumerations[0] = e1;
        _enumerations[1] = e2;
    }

    /**
        Create an enumeration of an array of enumeration in
        consecutive order, based on their index.
    */
    public IteratorUnion (Iterator[] enumerations)
    {
        if (enumerations == null) {
            enumerations = new Iterator[0];
        }

        _enumerations = (Iterator[])enumerations.clone();
    }

    public boolean hasNext ()
    {
        if (_enumerations.length == 0) {
            return false;
        }
        
        if (_enumerations[_current].hasNext()) {
            return true;
        }
        for (int i = _current + 1; i < _enumerations.length; i++) {
            if (_enumerations[i].hasNext()) {
                _current = i;
                return true;
            }
        }
        _current = _enumerations.length - 1;
        return false;
    }

    public Object next ()
    {
        if (_enumerations.length == 0) {
            throw new NoSuchElementException("IteratorUnion is empty");
        }
        hasNext();
        return _enumerations[_current].next();
    }

    public void remove ()
    {
        _enumerations[_current].remove();
    }
}

