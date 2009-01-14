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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWStringsFieldValueClassExtension.java#8 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.core.GrowOnlyHashtable;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.util.List;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue_Object;
import java.io.InputStream;

public class AWStringsFieldValueClassExtension extends FieldValue_Object
{
    private static GrowOnlyHashtable ExtendedFilenames = new GrowOnlyHashtable();

    public void setFieldValuePrimitive (Object target, FieldPath fieldPath, Object value)
    {
        throw new AWGenericException(getClass().getName() + ": cannot set values.");
    }

    public Object getFieldValuePrimitive (Object target, FieldPath fieldPath)
    {
        throw new AWGenericException(getClass().getName() + ": Requires fieldPath");
    }

    public void setFieldValuePrimitive (Object target, String key, Object value)
    {
        throw new AWGenericException(getClass().getName() + ": cannot set values.");
    }

    public Object getFieldValuePrimitive (Object target, String key)
    {
        throw new AWGenericException(getClass().getName() + ": Requires fieldPath");
    }

    public Object getFieldValue (Object target, FieldPath fieldPath)
    {
        Object value = null;
        String filename = fieldPath.car();
        String stringsFilename = (String)ExtendedFilenames.get(filename);
        if (stringsFilename == null) {
            stringsFilename = StringUtil.strcat(filename, ".strings");
            ExtendedFilenames.put(filename, stringsFilename);
        }
        AWResourceManager resourceManager = ((AWStringsThunk)target)._resourceManager;
        AWResource resource = resourceManager.resourceNamed(stringsFilename);
        if (resource != null) {
            Map stringsTable = (Map)resource.object();
            if (stringsTable == null) {
                synchronized (resource) {
                    stringsTable = (Map)resource.object();
                    if (stringsTable == null) {
                        stringsTable = (Map)resource.object();
                        if ((stringsTable == null) || (AWUtil.IsRapidTurnaroundEnabled && resource.hasChanged())) {
                            InputStream inputStream = resource.inputStream();
                            List lines = AWUtil.parseCsvStream(inputStream);
                            AWUtil.close(inputStream);
                            if (lines != null) {
                                stringsTable = AWUtil.convertToLocalizedStringsTable(lines);
                                resource.setObject(stringsTable);

                                AWUtil.internKeysAndValues(stringsTable);
                            }
                            else {
                                stringsTable = MapUtil.map();
                            }

                        }
                    }
                }
            }

            stringsTable = (Map)stringsTable.get(filename);
            FieldPath cdr = fieldPath.cdr();
            if (cdr == null) {
                value = stringsTable;
            }
            else {
                String stringKey = cdr.car();
                value = stringsTable.get(stringKey);
            }
        }
        else {
            value = Fmt.S("Undefined string with key path: \"%s\"", fieldPath.toString());
        }
        return value;
    }

    public void setFieldValue (Object target, FieldPath fieldPath, Object value)
    {
        throw new AWGenericException(getClass().getName() + ": cannot set values.");
    }
}
