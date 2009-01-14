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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValueGetter.java#2 $
*/

package ariba.util.fieldvalue;

/**
The FieldValueGetter class is an abstract class that serves as the base for
all FieldValueGetters.  FieldValueGetters are used to get values from
a target.  The details of how to execute these operations is left to the
subclasses and depends upon the type of the target object.  For the most part,
the subclass ReflectionGetter will do the job, but there may be cases
where a more specific approach will yield more efficient, or more desired, results.
*/
public interface FieldValueGetter extends FieldValueAccessor
{
    /**
    Executes the underlying 'get' operation (which is masked by this
    abstract implementation) upon the target object.

    @param target the object to which the receiver will perform the
    get operation.
    @return The result of the underlying 'get' operation.
    */
    abstract public Object getValue (Object target);
}
