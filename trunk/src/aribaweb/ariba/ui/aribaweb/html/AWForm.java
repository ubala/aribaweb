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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWForm.java#38 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWElementIdPath;
import ariba.ui.aribaweb.core.AWHiddenFormValueHandler;
import ariba.ui.aribaweb.core.AWHtmlForm;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWArrayManager;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.StringUtil;
import java.util.List;

// subclassed for validation
public class AWForm extends AWComponent implements AWHtmlForm
{
    private static final String[] SupportedBindingNames = {
        BindingNames.action, BindingNames.method,  BindingNames.onSubmit,
        BindingNames.target, BindingNames.fragmentIdentifier, BindingNames.omitTags,
        BindingNames.submitFormDefault, // unused?
        BindingNames.autocomplete,
        BindingNames.id, BindingNames.name, BindingNames.onKeyPress}; // deprecated?

    public AWEncodedString _formElementId;
    public AWEncodedString _hiddenFieldElementId;
    private AWEncodedString _frameName = AWUtil.UndefinedEncodedString;
    public boolean _emitTags;
    public boolean _submitFormDefault;

    // ** Thread Safety Considerations: see AWComponent.

    // Note: The 'name' attribute is not really supported (ie developers cannot assign
    // their own names).

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void awake ()
    {
        _frameName = AWUtil.UndefinedEncodedString;
        _emitTags = !booleanValueForBinding(BindingNames.omitTags);
        _submitFormDefault = booleanValueForBinding(BindingNames.submitFormDefault, true);
    }

    protected void sleep ()
    {
        _formElementId = null;
        _hiddenFieldElementId = null;
        _frameName = null;
        _emitTags = false;
        _submitFormDefault = false;
        curHiddenValueHandler = null;
    }

    public AWEncodedString formName ()
    {
        if (hasBinding(BindingNames.name)) {
            Object formName = valueForBinding(BindingNames.name);
            if (formName != null) {
                if (formName instanceof AWEncodedString) {
                    return (AWEncodedString)formName;
                }
                else {
                    return AWEncodedString.sharedEncodedString(formName);
                }
            }
        }
        return formId();
    }

    public AWEncodedString formId ()
    {
        return _formElementId;
    }

    public AWEncodedString frameName ()
    {
        if (_frameName == AWUtil.UndefinedEncodedString) {
            _frameName = encodedStringValueForBinding(BindingNames.target);
        }
        return _frameName;
    }

    public boolean submitFormDefault ()
    {
        return _submitFormDefault;
    }

    public AWEncodedString formActionUrl ()
    {
        AWEncodedString formActionUrl =  (requestContext().isStaticGeneration())
                ? new AWEncodedString(requestContext().staticUrlForActionResults((AWResponseGenerating)valueForBinding(AWBindingNames.action)))
                : AWComponentActionRequestHandler.SharedInstance.requestHandlerUrlEncoded(request());
        String fragmentIdentifier = stringValueForBinding(BindingNames.fragmentIdentifier);
        if (fragmentIdentifier != null) {
            String url = StringUtil.strcat(formActionUrl.string(), "#", fragmentIdentifier);
            formActionUrl = AWEncodedString.sharedEncodedString(url);
        }

        return formActionUrl;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (_emitTags) {
            _formElementId = requestContext.nextElementId();
            try {
                requestContext.pushElementIdLevel();
                String formSenderId = requestContext.request().formValueForKey(AWComponentActionRequestHandler.FormSenderKey);
                if (_formElementId.equals(formSenderId)) {
                    requestContext.setCurrentForm(this);
                    super.applyValues(requestContext, component);
                }
            }
            finally {
                requestContext.setCurrentForm(null);
                requestContext.popElementIdLevel();
                requestContext.incrementFormIndex();
            }
        }
        else {
            super.applyValues(requestContext, component);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        if (_emitTags) {
            _formElementId = requestContext.nextElementId();
            try {
                requestContext.pushElementIdLevel();
                requestContext.setCurrentForm(this);
                actionResults = super.invokeAction(requestContext, component);
            }
            finally {
                requestContext.setCurrentForm(null);
                requestContext.popElementIdLevel();
                requestContext.incrementFormIndex();
            }
            if (actionResults != null) {
                requestContext.setFrameName(frameName());
            }
        }
        else {
            actionResults = super.invokeAction(requestContext, component);
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_emitTags) {
            _formElementId = requestContext.nextElementId();
            try {
                requestContext.pushElementIdLevel();
                requestContext.setCurrentForm(this);
                if (!requestContext.isPrintMode()) {
                    requestContext.setFormInputIds(formInputIds(_formElementId));
                }
                super.renderResponse(requestContext, component);
                requestContext.setFormInputIds(null);
            }
            finally {
                requestContext.setCurrentForm(null);
                requestContext.popElementIdLevel();
                requestContext.incrementFormIndex();
            }
        }
        else {
            super.renderResponse(requestContext, component);
        }
    }

    private AWArrayManager formInputIds (AWEncodedString formElementId)
    {
        String formElementIdString = formElementId.string();
        AWPage page = page();
        AWArrayManager formInputIds = page.getFormIds(formElementIdString);
        if (formInputIds == null) {
            formInputIds = new AWArrayManager(AWElementIdPath.class);
            page.putFormIds(formElementIdString, formInputIds);
        }
        else {
            formInputIds.clear();
        }

        // add all "global" form input ids
        // note that this takes care of all forms that occur after the recording of
        // the global form input id.  All forms that occur before the global element id
        // is recorded are taken care of by AWPage.addGlobalFormInputIdPath().
        List globalPaths = requestContext().getGlobalFormInputIdPaths();
        if (globalPaths != null) {
            for (int i=0, size=globalPaths.size(); i < size; i++) {
                formInputIds.addElement(globalPaths.get(i));
            }
        }
        return formInputIds;
    }

    public boolean isFormSender ()
    {
        String requestSenderId = requestContext().requestSenderId();
        return _hiddenFieldElementId.equals(requestSenderId);
    }

    public Boolean hasFormAction ()
    {
        return hasBinding(BindingNames.action) ? Boolean.TRUE : null;
    }

    // record & playback
    protected boolean _debugSemanticKeyInteresting ()
    {
        return false;
    }

    public AWHiddenFormValueHandler curHiddenValueHandler = null;

    public List getHiddenValueHandlers ()
    {
        return page().hiddenFormValueManager().getHiddenValueHandlers();
    }
}
