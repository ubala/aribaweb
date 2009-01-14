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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWContentApi.java#2 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import java.util.Map;

public final class AWContentApi extends AWApiDeclaration
{
    public String _name;
    public boolean _required;

    public void init (String tagName, Map bindingsHashtable)
    {
        super.init();
        AWBinding nameBinding = (AWBinding)bindingsHashtable.get(AWBindingNames.name);
        if (nameBinding == null) {
            throw new AWGenericException("AWContentApi requires 'name' specification: ");
        }
        _name = nameBinding.stringValue(null);
        AWBinding isRequiredBinding = (AWBinding)bindingsHashtable.get(AWBindingNames.required);
        if (isRequiredBinding == null) {
            throw new AWGenericException("AWContentApi requires 'required' specification: ");
        }
        _required = isRequiredBinding.booleanValue(null);
    }
}
