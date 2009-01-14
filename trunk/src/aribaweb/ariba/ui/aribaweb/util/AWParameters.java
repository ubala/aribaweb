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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWParameters.java#5 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValue_Object;
import ariba.util.fieldvalue.FieldValueAccessor;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.Fmt;

/**
 * Note that an instance of this class is created by default
 * for AW apps that do not override AWConcreteApplication.initConfigParameters, but will
 * effectively noop as the AWParameterFieldValueClassExtension will effectively always
 * return null.
 */
public class AWParameters
{
    static {
        FieldValue.registerClassExtension(
            AWParameters.class,
            new AWParametersFieldValueClassExtension());
    }

    /**
     * Stubbed out.  This method should be overridden by subclasses to return proper
     * parameter values.
     *
     * @param paramkey
     * @aribaapi private
     */
    public String getParameterValue (String paramkey)
    {
        return null;
    }

    public String getParameterValue (String paramkey, String defaultValue)
    {
        String value = getParameterValue(paramkey);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public int getIntParameterValue (String paramkey)
    {
        String value = getParameterValue(paramkey, "0");
        return Integer.parseInt(value);
    }

    public long getLongParameterValue (String paramkey, long defaultValue)
    {
        String value = getParameterValue(paramkey, Long.toString(defaultValue));
        return Long.parseLong(value);
    }

    static final class AWParametersFieldValueClassExtension extends FieldValue_Object
    {
        public FieldValueAccessor createAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(
                Fmt.S("%s: createAccessor() not suported", receiver.getClass()));
        }

        public FieldValueAccessor getAccessor (Object receiver, String fieldName)
        {
            throw new AWGenericException(
                Fmt.S("%s: getAccessor() not suported", receiver.getClass()));
        }

        public void setFieldValuePrimitive (Object receiver,
                                            FieldPath fieldPath,
                                            Object value)
        {
            throw new AWGenericException(
                Fmt.S("%s: setFieldValuePrimitive() not suported", receiver.getClass()));
        }

        public Object getFieldValue (Object target, FieldPath fieldPath)
        {
            // simple FieldValue accessor that returns "everything else" as the key
            // so Parameters.foo.bar would return "foo.bar" here as the key
            return getFieldValuePrimitive(target, fieldPath.fieldPathString());
        }

        public Object getFieldValuePrimitive (Object receiver, String key)
        {
            Object objectValue = ((AWParameters)receiver).getParameterValue(key);
            return objectValue;
        }
    }
}
