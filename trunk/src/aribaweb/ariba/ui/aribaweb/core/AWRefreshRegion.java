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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRefreshRegion.java#22 $
*/
package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;

public final class AWRefreshRegion extends AWComponent
{
    private static final AWEncodedString DEFAULT_CLASS = AWEncodedString.sharedEncodedString("rr");
    public static final AWBinding NullActionBinding =
            AWBinding.bindingWithNameAndKeyPath(AWBindingNames.action, AWBinding.NullKey);

    private static final String[] SupportedBindingNames = {
        AWBindingNames.tagName, AWBindingNames.style,
        AWBindingNames.classBinding,
        AWBindingNames.alwaysRender, AWBindingNames.isScope,
        AWBindingNames.elementId,
        AWBindingNames.useId,
        AWBindingNames.disabled,
        AWBindingNames.ignore,
        AWBindingNames.forceRefreshOnChange,
        AWDragContainer.DragActionBinding,
        AWDropContainer.DropActionBinding,
    };

    private boolean _isDisabled;
    public boolean _dragEnabled;
    public AWEncodedString _elementId;

    public AWBinding _styleBinding;
    public AWBinding _tagNameBinding;
    public AWBinding _classBinding;
    public boolean _hasValueBinding;
    public boolean _hasRequiresRefreshBinding;

    ///////////////////
    // Bindings Support
    ///////////////////
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _isDisabled = booleanValueForBinding(AWBindingNames.disabled);
        _dragEnabled = hasBinding(AWDragContainer.DragActionBinding);
        String useId = stringValueForBinding("useId");
        if (useId != null) {
            _elementId = AWEncodedString.sharedEncodedString(useId);
        } else {
            _elementId = requestContext().nextElementId();
        }
        setValueForBinding(_elementId, AWBindingNames.elementId);
        _styleBinding = bindingForName(AWBindingNames.style);
        _classBinding = bindingForName(AWBindingNames.classBinding);
        _tagNameBinding = bindingForName(AWBindingNames.tagName);
        // use false here to avoid recursion.
        // We only want to know what usage pattern
        // is desired -- not if it ultimately is bound.
        _hasValueBinding = bindingForName(AWBindingNames.value, false) != null;
        if (_hasValueBinding) {
            _hasRequiresRefreshBinding = false;
        }
        else {
            _hasRequiresRefreshBinding = bindingForName(AWBindingNames.requiresRefresh, false) != null;
        }
        super.awake();
    }

    public void sleep ()
    {
        _elementId = null;
        _isDisabled = false;
        _dragEnabled = false;
        _styleBinding = null;
        _classBinding = null;
        _tagNameBinding = null;
        _hasValueBinding = false;
        _hasRequiresRefreshBinding = false;
        super.sleep();
    }

    public void pushBuffer ()
    {
        if (!_isDisabled) {
            boolean isScope = booleanValueForBinding(AWBindingNames.isScope);
            boolean alwaysRender = booleanValueForBinding(AWBindingNames.alwaysRender);
            ((AWBaseResponse)response()).pushBuffer(_elementId, isScope, alwaysRender);
        }
    }

    public void popBuffer ()
    {
        if (!_isDisabled) {
            boolean forceRefreshOnChange = booleanValueForBinding(AWBindingNames.forceRefreshOnChange);
            AWBaseResponse response = (AWBaseResponse)response();
            response.popBuffer(forceRefreshOnChange);
        }
    }

    public boolean isSender ()
    {
        return requestContext().requestSenderId().equals(_elementId.string());
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        super.renderResponse(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        // We need to handle invokeAction ourselves because the element id of our
        // AWGenericContainer is retrieved in awake. This is necessary for the
        // pushBuffer() call which sets up the refresh buffers.  The AWIf in our
        // template ends up skipping the AWGenericContainer since it's element id is less
        // than the element id of the AWIf so the action binding of the
        // AWGenericContainer is never fired.
        if (isSender()) {
            if (requestContext.isPathDebugRequest()) return requestContext.pageComponent();
            return AWDragContainer.processAction(this, _elementId);
        }
        return super.invokeAction(requestContext, component);
    }

    public Object getStyle ()
    {
        AWEncodedString style = null;
        if (hasBinding(_styleBinding)) {
            style = encodedStringValueForBinding(_styleBinding);
        }

        if (AWConcreteServerApplication.IsDebuggingEnabled) {
            AWSession session = session(false);
            if (session != null &&
                Boolean.TRUE.equals(session.dict().get("RefreshRegionsVisible"))) {
                return StringUtil.strcat("border:1px red dotted;",
                                         style != null ? style.toString() : null);
            }
        }
        return style;
    }

    public Object getClassName ()
    {
        Object className = DEFAULT_CLASS;
        if (hasBinding(_classBinding)) {
            className = stringValueForBinding(_classBinding);
        }
        else if (hasBinding(_tagNameBinding)) {
            String tagName = stringValueForBinding(_tagNameBinding);
            if (!"div".equals(tagName)) {
                className = null;
            }
        }
        return className;
    }

    public boolean isClassicRefreshRegion ()
    {
        return !(_hasValueBinding || _hasRequiresRefreshBinding);
    }
}
