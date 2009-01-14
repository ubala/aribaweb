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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWTdContainer.java#9 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTaggedContainer;
import ariba.ui.aribaweb.core.AWTaggedElement;
import java.util.Map;

public final class AWTdContainer extends AWTaggedContainer
{
    static {
        registerTdContainer();
    }

    public static void registerTdContainer ()
    {
        AWComponent.defaultTemplateParser().registerContainerClassForTagName("td", AWTdContainer.class);
        AWComponent.defaultTemplateParser().registerContainerClassForTagName("TD", AWTdContainer.class);
        AWComponent.defaultTemplateParser().registerContainerClassForTagName("Td", AWTdContainer.class);
        AWComponent.defaultTemplateParser().registerContainerClassForTagName("tD", AWTdContainer.class);
    }

    protected AWTaggedElement initTaggedElement (String tagName, Map bindingsHashtable)
    {
        AWTaggedElement tdElement = new AWTdElement();
        tdElement.init(tagName, bindingsHashtable);
        return tdElement;
    }
}
