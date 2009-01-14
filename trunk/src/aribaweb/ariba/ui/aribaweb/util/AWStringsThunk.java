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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWStringsThunk.java#6 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;

public class AWStringsThunk
{
    private static AWStringsFieldValueClassExtension StringsThunkClassExtension =
        new AWStringsFieldValueClassExtension();
    protected AWResourceManager _resourceManager;

    static {
        FieldValue.registerClassExtension(AWStringsThunk.class, StringsThunkClassExtension);
    }

    public AWStringsThunk (AWResourceManager resourceManager)
    {
        super();
        _resourceManager = resourceManager;
    }

    public void setFieldValuePrimitive (FieldPath fieldPath, Object value)
    {
        throw new AWGenericException(getClass().getName() + ": cannot set values.");
    }

    public Object getFieldValuePrimitive (FieldPath fieldPath)
    {
        throw new AWGenericException(getClass().getName() + ": Requires fieldPath");
    }

    public Object getFieldValue (FieldPath fieldPath)
    {
        return StringsThunkClassExtension.getFieldValue(this, fieldPath);
    }

    public Object getFieldValue (String fieldPathString)
    {
        return StringsThunkClassExtension.getFieldValue(this, fieldPathString);
    }
}
