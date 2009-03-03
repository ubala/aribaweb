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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWCycleable.java#7 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWObject;

/**
    Defines the three critical request handling phase for "Component Actions".
    In the component action request handling cycle, a {@link AWPage page}, its root component,
    and the full tree of elements and subcomponents therein first go through {@link #renderResponse},
    and then, when the user performs an action on the page, will in the resulting follow-up request
    perform {@link #applyValues} and then {@link #invokeAction}.

    @aribaapi private
 */
public interface AWCycleable extends AWObject
{
    /**
     * The receiver should take any form values (or query string parameters) that it owns from the
     * {@link AWRequestContext#request()}.
     *
     * @param requestContext the context for the current request.
     * @param component the current parent component
     */
    public void applyValues(AWRequestContext requestContext, AWComponent component);

    /**
     * The receiver should determine if it is the intended recipient of the current action
     * (by checking the {@link AWRequestContext#request()} {@link AWRequest#senderId()}) and,
     * if so, handle the action and return the result.
     * @param requestContext the context for the current request.
     * @param component the current parent component
     * @return the response for the action
     */
    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component);

    /**
     * The receiver should render its content to the {@link AWRequestContext#response()}
     * @param requestContext the context for the current request.
     * @param component the current parent component
     */
    public void renderResponse(AWRequestContext requestContext, AWComponent component);
}
