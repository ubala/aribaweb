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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWFormatting_AWFormatter.java#5 $
*/

package ariba.ui.aribaweb.util;

import java.text.ParseException;

public final class AWFormatting_AWFormatter extends AWFormatting
{
    // ** Thread Safety Considerations: no state here -- no locking required.

    public Object parseObject (Object receiver, String stringToParse)
    {
        Object parsedObject = null;
        try {
            parsedObject = ((AWFormatter)receiver).parseObject(stringToParse);
        }
        catch (ParseException parseException) {
            throw new AWGenericException(parseException.getMessage());
        }
        return parsedObject;
    }

    public String format (Object receiver, Object objectToFormat)
    {
        return ((AWFormatter)receiver).format(objectToFormat);
    }
}
