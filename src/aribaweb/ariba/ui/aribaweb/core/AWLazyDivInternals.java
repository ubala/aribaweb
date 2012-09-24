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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWLazyDivInternals.java#5 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;

public final class AWLazyDivInternals extends AWComponent
{
    private static final AWIncludeContent SharedComponentContentElement;
    public static final String DebugSemanticKey = "LazyDiv";
    public AWEncodedString _elementId;

    static {
        SharedComponentContentElement = new AWIncludeContent();
        SharedComponentContentElement.setTemplateName(ariba.ui.aribaweb.core.AWLazyDiv.class.getName());
    }

    public boolean isStateless ()
    {
        return true;
    }

    protected void sleep ()
    {
        _elementId = null;
    }

    public void setElementId (AWEncodedString elementId)
    {
        _elementId = elementId;
        setValueForBinding(_elementId, AWBindingNames.id);
    }

    private void setHasRendered (boolean flag)
    {
        page().put(_elementId, flag ? Boolean.TRUE : null);
    }

    private boolean hasRendered ()
    {
        Boolean flag = (Boolean)page().get(_elementId);
        return flag == null ? false : flag.booleanValue();
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        setElementId(requestContext.nextElementId());
        requestContext.pushElementIdLevel();
        if (hasRendered()) {
            SharedComponentContentElement.applyValues(requestContext, this);
        }
        requestContext.popElementIdLevel();
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating responseGenerating = null;
        setElementId(requestContext.nextElementId());
        requestContext.pushElementIdLevel();
        if (hasRendered()) {
            responseGenerating = SharedComponentContentElement.invokeAction(requestContext, this);
        }
        else {
            String requestSenderId = requestContext.requestSenderId();
            boolean isSender = _elementId.equals(requestSenderId);
            if (isSender) {
                AWResponse response = application().createResponse();
                responseGenerating = response;
                requestContext.setXHRRCompatibleResponse(response);
                if (AWConcreteApplication.IsDebuggingEnabled) {
                    AWRecordingManager.mergeSemanticKeys(
                        page().getPreviousResponse(), response, requestContext);
                }
                SharedComponentContentElement.renderResponse(requestContext, this);

                setHasRendered(true);
            }
        }
        requestContext.popElementIdLevel();
        return responseGenerating;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        setElementId(requestContext.nextElementId());
        requestContext.pushElementIdLevel();
        if (hasRendered()) {
            SharedComponentContentElement.renderResponse(requestContext, this);
        }
        else {
            super.renderResponse(requestContext, component);
        }
        requestContext.popElementIdLevel();
    }

    protected String _debugSemanticKey ()
    {
        return DebugSemanticKey;
    }

}
