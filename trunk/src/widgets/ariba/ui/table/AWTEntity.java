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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTEntity.java#6 $
*/
package ariba.ui.table;

import java.util.List;

public abstract class AWTEntity
{
    public abstract List propertyKeys ();
    public abstract String defaultFormatterNameForKey (String key);
    public abstract String displayStringForKey (String key);
    public String defaultAlignmentForKey (String key) { return null; }
}
