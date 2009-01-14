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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWEditableRegion.java#2 $
*/

package ariba.ui.aribaweb.core;

public class AWEditableRegion extends AWComponent
{

    public static boolean disabled (AWRequestContext requestContext)
    {
        return editability(requestContext, "disabled");
    }

    public static boolean only (AWRequestContext requestContext)
    {
        return editability(requestContext, "readonly");
    }

    private static boolean editability (AWRequestContext requestContext, String flag)
    {
        Boolean editability = (Boolean)requestContext.page().env().peek(flag);
        return editability != null ? editability.booleanValue() : false;
    }

}
