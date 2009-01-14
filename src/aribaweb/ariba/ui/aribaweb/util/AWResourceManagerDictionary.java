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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWResourceManagerDictionary.java#5 $
*/

package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWResourceManagerDictionary extends AWBaseObject
{
    Object[] _elements = {};

    public Object get (AWSingleLocaleResourceManager resourceManager)
    {
        // by grabbing a ref to _elements, it can be changed in another thread and we're still okay.
        Object[] elements = _elements;
        int index = resourceManager.index();
        return (index < elements.length) ? elements[index] : null;
    }

    public Object put (AWSingleLocaleResourceManager resourceManager, Object element)
    {
        // by grabbing a ref to _elements, it can be changed in another thread and we're still okay.
        Object[] elements = _elements;
        int index = resourceManager.index();
        Object previousElement = null;
        if (index >= elements.length) {
            elements = (Object[])AWUtil.realloc(elements, index + 1);
            _elements = elements;
        }
        else {
            previousElement = elements[index];
        }
        elements[index] = element;
        return previousElement;
    }
}
