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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWStringLocalizer.java#4 $
*/
package ariba.ui.aribaweb.core;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWResourceManager;
import java.util.Map;

public interface AWStringLocalizer
{
    public Map getLocalizedAWLStrings (AWComponent component);

    public Map getLocalizedJavaStrings (AWComponent component);

    public Map getLocalizedStrings (String stringTable, String componentKey,
                                    AWResourceManager resourceManager);
}
