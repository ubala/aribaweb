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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWInstanceInclude.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;

public class AWInstanceInclude extends AWComponent
{
    public AWComponent _instance = null;
    protected boolean _clearInstance = false;
    public final static String InstanceBinding = "instance";
    public final static String autoClear = "autoClear";

    public boolean isStateless ()
    {
        return false;
    }

    public static void closePanel (AWComponent component)
    {
        AWInstanceInclude instance = (AWInstanceInclude)component.env().peek("awInstanceInclude");
        Assert.that(instance != null, "closePanel called outside of environment scope");
        instance._clearInstance = true;
    }

    public void checkClear ()
    {
        if (_clearInstance) {
            setValueForBinding(null, InstanceBinding);
            _clearInstance = false;
        }
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWComponent instance = (AWComponent)valueForBinding(InstanceBinding);
        if ((_instance != null) && (instance == _instance) && booleanValueForBinding(autoClear)) {
            // we've already rendered this one -- clear it
            _instance = null;
            setValueForBinding(null, InstanceBinding);
        } else {
            _instance = instance;
        }
        super.renderResponse(requestContext, component);
    }

    public AWComponentReference instanceReference ()
    {
        AWComponentReference reference = _instance.componentReference();
        return (reference.isBoundReference()) ? reference
                : reference.createBoundReference(_instance);
    }
}
