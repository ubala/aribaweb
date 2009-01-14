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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/SiteActionHandler.java#1 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import java.util.List;
import java.util.Collections;

/**
    @aribaapi
*/
public class SiteActionHandler extends ActionHandler
{
    
    public List<String> getSites (AWRequestContext requestContext)
    {
            // delegate down to subclass to get list of realms
        return null;
    }
    
    public String onClick (AWRequestContext requestContext)
    {
            // delegate to subclass to construct url from site name in requestContext
        return null;
    }
    
    public boolean isEnabled (AWRequestContext requestContext)
    {
        return false;
    }
}
