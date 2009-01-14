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

    $Id: //ariba/platform/util/expr/ariba/util/expr/FieldValue_ConvertibleFieldValue.java#3 $
*/

package ariba.util.expr;

import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldPath;

/**

    @see ariba.util.fieldvalue.FieldValue_Object

    @aribaapi private
*/
public class FieldValue_ConvertibleFieldValue
    extends FieldValue_Object
{
    public final static String ClassName = "ariba.base.core.FieldValue_ConvertibleFieldValue";

    /**
     * This method retrieves the field value from the <code>target</code>
     * object.   If the field value is a <code>BaseId</code>, this method
     * will return the corresponding <code>BaseObject</code>.
     * @param target - the object from which to get the value of the field
     * identified by fieldPath
     * @param fieldPath - the fieldPath node which identifes the field to get.
     * @return the value obtained from the target using the fieldPath
     */
    public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
    {
        FieldPath newFieldPath = new FieldPath(fieldPath.fieldPathString());
        return newFieldPath.getFieldValue(target);
    }
}
