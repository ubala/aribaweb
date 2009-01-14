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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponentInclude.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Constants;
import ariba.util.core.MapUtil;
import java.util.Map;
import java.util.Iterator;

public final class AWComponentInclude extends AWComponent
{
    private AWBinding _awcomponent;
    private int _nextSubpageComponentId = 0;
    private Map _subpageComponents = MapUtil.map();

    public void init ()
    {
        super.init();
        _awcomponent = bindingForName(AWBindingNames.awcomponent);
    }

    private AWComponent pullComponentInstance (AWRequestContext requestContext)
    {
        AWComponent subpageComponent = (AWComponent)valueForBinding(_awcomponent);
        if (subpageComponent != null) {
            AWPage page = requestContext.page();
            subpageComponent.setPage(page);
            int subpageInstanceId =  instanceIdForSubpageComponent(subpageComponent);
            requestContext.pushElementIdLevel(subpageInstanceId);
        }
        return subpageComponent;
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        AWComponent includedComponent = pullComponentInstance(requestContext);
        if (includedComponent != null) {
            includedComponent.applyValues(requestContext, component);
            requestContext.popElementIdLevel(0);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWComponent includedComponent = pullComponentInstance(requestContext);
        if (includedComponent != null) {
            actionResults = includedComponent.invokeAction(requestContext,
                                                                     component);
            requestContext.popElementIdLevel(0);
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWComponent includedComponent = pullComponentInstance(requestContext);
        if (includedComponent != null) {
            includedComponent.renderResponse(requestContext, component);
            requestContext.popElementIdLevel(0);
        }
    }

    public boolean isStateless ()
    {
        return false;
    }

    protected void sleep ()
    {
        super.sleep();
        if (!_subpageComponents.isEmpty()) {
            Iterator subcomponentIterator = _subpageComponents.keySet().iterator();
            while (subcomponentIterator.hasNext()) {
                AWComponent currentSubcomponent = (AWComponent)
                    subcomponentIterator.next();
                currentSubcomponent.ensureAsleep();
            }
        }
    }

    public int instanceIdForSubpageComponent (AWComponent subpageComponent)
    {
        Integer instanceId = (Integer)_subpageComponents.get(subpageComponent);
        if (instanceId == null) {
            instanceId = Constants.getInteger(_nextSubpageComponentId);
            _nextSubpageComponentId++;
            _subpageComponents.put(subpageComponent, instanceId);
        }
        return instanceId.intValue();
    }
}
