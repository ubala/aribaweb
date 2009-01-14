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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/Extensible.java#2 $
*/

package ariba.util.fieldvalue;

import java.util.Map;

/**
    The Extensible interface can be implemented when a given class
    wants to provide dynamically added fields.  Once this is implemented
    to return a Map, the FieldValue system will be able to look in
    the Map to see if the desired field exists.

    One restriction applies for extensible fields -- you must set the value
    of the field before attempting to access itr, else an exception will
    be thrown.

    @aribaapi private

*/
public interface Extensible
{
    /**
    Returns the Map in which the dynamically added fields reside.
    */
    public Map extendedFields ();
}
