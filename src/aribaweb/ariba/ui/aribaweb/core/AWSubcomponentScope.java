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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSubcomponentScope.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;

import java.util.Map;

/*
    Used as a scope for reseting (discarding) stateful subcomponent instances.
    For instance, when a DataTable's column layout changes, it's stateful subcomponents
    will no longer align with the elementIds.  By tripping the latch here these stateful
    subcomponents can be discarded.
 */
public class AWSubcomponentScope extends AWContainerElement
{
    private AWBinding _resetLatch;

    public void init (String tagName, Map bindingsHashtable)
    {
        _resetLatch = (AWBinding)bindingsHashtable.remove("resetLatch");
        Assert.that(_resetLatch != null, "AWSubcomponentScope must have resetLatch binding");
        super.init(tagName, bindingsHashtable);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component) {
        requestContext.pushElementIdLevel();
        super.applyValues(requestContext, component);
        requestContext.popElementIdLevel();
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component) {
        requestContext.pushElementIdLevel();
        AWResponseGenerating actionResults = super.invokeAction(requestContext, component);
        requestContext.popElementIdLevel();
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        requestContext.pushElementIdLevel();
        AWElementIdPath path = requestContext.currentElementIdPath();
        boolean reset = _resetLatch.booleanValue(component);
        if (reset) {
            requestContext.page()._clearSubcomponentsWithParentPath(path, false);
            _resetLatch.setValue(false, component);
        }
        super.renderResponse(requestContext, component);
        requestContext.popElementIdLevel();
    }
}
