/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/ItemProperties.java#2 $
*/
package ariba.ui.meta.core;

import java.util.Map;

public class ItemProperties
{
    String _name;
    Map _properties;
    boolean _hidden;

    public ItemProperties(String name, Map properties, boolean hidden)
    {
        _name = name;
        _properties = properties;
        _hidden = hidden;
    }

    public String name() {
        return _name;
    }

    public Map properties() {
        return _properties;
    }

    public boolean isHidden()
    {
        return _hidden;
    }
}
