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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSingleton.java#10 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.MapUtil;
import java.util.Map;

public final class AWSingleton extends AWComponent
{
    public boolean _hasSubcomponent;

    protected void sleep ()
    {
        _hasSubcomponent = false;
    }

    private AWSingletonState singletonState (AWRequestContext requestContext)
    {
        Map singletonHashtable = (Map)requestContext.get("AWSingleton");
        if (singletonHashtable == null) {
            singletonHashtable = MapUtil.map();
            requestContext.put("AWSingleton", singletonHashtable);
        }
        // use the component reference of the current singleton as the key.
        // this allows for multiple singletons in the same component.
        Object singletonReference = key();
        AWSingletonState singletonState = (AWSingletonState)singletonHashtable.get(singletonReference);
        if (singletonState == null) {
            singletonState = new AWSingletonState();
            singletonHashtable.put(singletonReference, singletonState);
        }
        return singletonState;
    }

    private Object key ()
    {
        AWBinding keyBinding = bindingForName(AWBindingNames.key);
        return keyBinding != null ? valueForBinding(keyBinding) : componentReference();
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        AWSingletonState singletonState = singletonState(requestContext);
        _hasSubcomponent = singletonState.applyValues;
        singletonState.applyValues = true;
        super.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWSingletonState singletonState = singletonState(requestContext);
        _hasSubcomponent = singletonState.invokeAction;
        singletonState.invokeAction = true;
        return super.invokeAction(requestContext, component);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWSingletonState singletonState = singletonState(requestContext);
        _hasSubcomponent = singletonState.renderResponse;
        singletonState.renderResponse = true;
        super.renderResponse(requestContext, component);
    }
}

final class AWSingletonState extends AWBaseObject
{
    protected boolean applyValues = false;
    protected boolean invokeAction = false;
    protected boolean renderResponse = false;
}
