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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldValueAccessor.java#2 $
*/

package ariba.util.fieldvalue;

/**
The FieldValueAccessor class is a base class that serves as the base for
all FieldValueAccessors.  FieldValueAccessors are used to set/get values on/from
 a target.  The details of how to execute these operations is left to the
subclasses and depends upon the type of the target object.  For the most part,
the subclass ReflectionFieldValueAccessor will do the job, but there may be cases
where a more specific approach will yield more efficient, or more desired, results.

Instances of FieldValueAccessor hold the (public) forClass variable so the
FieldValue dispatching mechanism may quickly determine if the target's class and
the FieldValueAccessor in question are compatible.
*/
public interface FieldValueAccessor
{
    /**
    Returns the Class for which the accessor applies.
    */
    public Class forClass ();

    /**
    Returns the fieldName which the accessor represents.
    This is primarily for debugging.

    @return the fieldName string
    */
    public String getFieldName ();

    /**
    This method is called when determining if a cached FieldValueAccessor
    is applicable for the target object. Special subclasses should override
    this if they need additional tests for applicability.  The default test
    is to check if the target.getClass() == accessor.forClass and, if so,
    then this method is called for further checking.  If this method returns
    false, the cached accessor is ignored and a new one will be obtained by
    performaing a more expensive lookup.

    @param target the object on which the receiver will operate
    (for either the get or set operation) if this method returns true.
    @return This method always returns true.  Subclasses of FieldValueAccessor
    which implement different accessors for different instances of the same
    class will need to override this and return the approiate value.
    */
    public boolean isApplicable (Object target);
}
