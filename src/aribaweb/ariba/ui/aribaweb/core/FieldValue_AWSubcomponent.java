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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/FieldValue_AWSubcomponent.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;

/**
    See AWIncludeBlock for big picture.

    This class extends the FieldValue_Object classExtension to cause fieldVlaue access for AWIncludeBlock to be forwarded to the target's parent component.  This way, when an AWBlock has $ bindings in it, those will actually be evaluated within the parent component (ie the component in which the AWBlock is defined).
*/
public final class FieldValue_AWSubcomponent extends FieldValue_Object
{
    public void setFieldValuePrimitive (Object target, FieldPath fieldPath, Object value)
    {
        super.setFieldValuePrimitive(((AWIncludeBlock)target).parent(), fieldPath, value);
    }

    public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
    {
        return super.getFieldValuePrimitive(((AWIncludeBlock)target).parent(), fieldPath);
    }
}
