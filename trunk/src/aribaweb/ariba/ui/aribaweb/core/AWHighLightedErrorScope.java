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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWHighLightedErrorScope.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.util.core.Constants;

public class AWHighLightedErrorScope extends AWComponent
{
    public static final String InScopeEnvVar = "isInHighLightedErrorScope";

    private Object _key;

    protected void sleep() {
        super.sleep();
        _key = null;
    }

    public Object errorKey ()
    {
        _key = AWErrorManager.getErrorKeyFromBindingsOnly(this);
        if (_key == null) {
            _key = requestContext().nextElementId();
        }
        return _key;
    }

    public Boolean isInHighLightedErrorScope ()
    {
        if (skipHighLight()) {
            return Constants.getBoolean(false);
        }
        Boolean inScope = Constants.getBoolean(false);
        inScope = Constants.getBoolean(
            errorManager().isHighLightedError(_key));

        return inScope;
    }

    public static boolean isInHighLightedErrorScope (AWEnvironmentStack env)
    {
        Boolean inScope = (Boolean)env.peek(AWHighLightedErrorScope.InScopeEnvVar);
        return (inScope != null) ? inScope.booleanValue() : false;
    }

    private boolean skipHighLight ()
    {
        return booleanValueForBinding(AWBindingNames.omitTags);
    }
}
