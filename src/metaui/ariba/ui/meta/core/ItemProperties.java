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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/ItemProperties.java#1 $
*/
package ariba.ui.meta.core;

import java.util.Map;

public class ItemProperties
{
    String _name;
    Map _properties;
    Context.Activation _activation;

    public ItemProperties(String name, Map properties, Context.Activation activation)
    {
        _name = name;
        _properties = properties;
        _activation = activation;
    }

    public String name() {
        return _name;
    }

    public Map properties() {
        return _properties;
    }

    public Context.Activation activation() {
        return _activation;
    }
}
