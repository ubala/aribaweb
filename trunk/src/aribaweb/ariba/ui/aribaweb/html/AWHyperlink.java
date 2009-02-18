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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWHyperlink.java#31 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.ui.aribaweb.core.AWHtmlForm;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWStringDictionary;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.Assert;

// subclassed by network/service/apps/admin/ADPDirectoryHyperlink.java
public class AWHyperlink extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.pageName, BindingNames.action, BindingNames.onClick, BindingNames.omitTags,
        BindingNames.fragmentIdentifier, BindingNames.submitForm,
        BindingNames.target, BindingNames.scrollToVisible, BindingNames.href, BindingNames.senderId,
        BindingNames.onMouseDown, BindingNames.windowAttributes, "behavior"
    };

    // ** this is pushed by the AWGenericContainer
    public AWEncodedString _elementId;
    public AWEncodedString _senderId;
    public boolean _isSubmitForm;
    private boolean _scrollToVisible;
    private AWEncodedString _frameName = AWUtil.UndefinedEncodedString;
    private AWEncodedString _fragmentIdentifier = AWUtil.UndefinedEncodedString;
    // Bindings
    public AWBinding _actionBinding;
    public AWBinding _pageNameBinding;
    public AWBinding _onClickBinding;
    public AWBinding _omitTagsBinding;
    public AWBinding _fragmentIdentifierBinding;
    public AWBinding _targetBinding;
    public AWBinding _scrollToVisibleBinding;
    public AWBinding _senderIdBinding;
    public AWBinding _hrefBinding;
    public AWBinding _submitFormBinding;

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    protected void awake ()
    {
        _frameName = AWUtil.UndefinedEncodedString;
        _fragmentIdentifier = AWUtil.UndefinedEncodedString;
        AWHtmlForm currentForm = requestContext().currentForm();
        _isSubmitForm = hasBinding(_submitFormBinding) ?
            booleanValueForBinding(_submitFormBinding) :
            currentForm != null && ((AWForm)currentForm).submitFormDefault();
        Assert.that(_hrefBinding == null, "href no longer supported");
        Assert.that(_fragmentIdentifierBinding == null, "fragmentIdentifier no longer supported");
    }

    protected void sleep ()
    {
        _elementId = null;
        _senderId = null;
        _frameName = null;
        _fragmentIdentifier = null;
        _isSubmitForm = false;
        _scrollToVisible = false;
    }

    public AWEncodedString frameName ()
    {
        if (_frameName == AWUtil.UndefinedEncodedString) {
            _frameName = encodedStringValueForBinding(_targetBinding);
        }
        return _frameName;
    }

    private AWEncodedString fragmentIdentifier ()
    {
        if (_fragmentIdentifier == AWUtil.UndefinedEncodedString) {
            if (_scrollToVisible) {
                _fragmentIdentifier = senderId();
            }
            else {
                _fragmentIdentifier = encodedStringValueForBinding(_fragmentIdentifierBinding);
            }
        }
        return _fragmentIdentifier;
    }

    public AWStringDictionary otherBindingsValues ()
    {
        AWStringDictionary otherBindingsValues = super.otherBindingsValues();
        if (_scrollToVisible) {
            if (otherBindingsValues == null) {
                otherBindingsValues = page().otherBindingsValuesScratch();
            }
            otherBindingsValues.put(BindingNames.name, fragmentIdentifier());
        }
        return otherBindingsValues;
    }

    public AWEncodedString hrefString ()
    {
        return senderId();
    }

    public String remoteLinkUrl ()
    {
        AWEncodedString senderId = senderId();
        String url = AWComponentActionRequestHandler.SharedInstance.urlWithSenderId(requestContext(), senderId);
        return StringUtil.strcat(url, "&", AWComponentActionRequestHandler.SenderKey, "=", senderId.string());
    }

    public boolean isHiddenFieldSender ()
    {
        return AWGenericActionTag.isHiddenFieldSender(request(), senderId());
    }

    public AWResponseGenerating invokeAction ()
    {
        AWResponseGenerating actionResults = AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding);
        requestContext().setFrameName(frameName());
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _scrollToVisible = booleanValueForBinding(_scrollToVisibleBinding);
        super.renderResponse(requestContext, this);
        _scrollToVisible = false;
    }

    /////////////////
    // Conditionals
    /////////////////
    public AWEncodedString senderId ()
    {
        if (_senderId == null) {
            _senderId = encodedStringValueForBinding(_senderIdBinding);
            if (_senderId == null) {
                _senderId = _elementId;
            }
        }
        return _senderId;
    }

    public boolean isSender ()
    {
        return senderId().equals(requestContext().requestSenderId());
    }

        // recording & playback
        // provide a better semantic key
    protected AWBinding _debugPrimaryBinding ()
    {
        AWBinding primaryBinding = null;
        AWComponentReference componentRef = componentReference();

        if (componentRef != null) {
            AWComponent parent = parent();
            primaryBinding = componentRef.bindingForName(BindingNames.action, parent);
            if (primaryBinding == null) {
                primaryBinding = componentRef.bindingForName(BindingNames.pageName, parent);
            }
        }
        return primaryBinding;
    }

    public boolean staticOrOmit ()
    {
        return omitLink() || requestContext().isStaticGeneration();
    }

    public String staticUrlForActionResults ()
    {
        return requestContext().staticUrlForActionResults(AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding));
    }

    public boolean omitLink ()
    {
        return requestContext().isExportMode() || requestContext().isPrintMode();
    }
}
