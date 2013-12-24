/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BasicPageWrapper.java#37 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.html.AWImage;
import ariba.util.core.StringUtil;

public final class BasicPageWrapper extends PageWrapper
{
    static {
        FooterIncludes.registerInclude(MessageBanner.class.getName());
        FooterIncludes.registerInclude(PollDialog.class.getName());
        FooterIncludes.registerInclude(SpotlightFooter.class.getName());
    }

    public String  _formActionUrl;
    public String  _formName;
    public String  _formEncodingType;
    public boolean _hasForm;
    public Object _currentPanel;

    protected void awake ()
    {
        super.awake();
        _hasForm = booleanValueForBinding(BindingNames.hasForm);
        _formActionUrl = stringValueForBinding(BindingNames.formActionUrl);
        _formName = stringValueForBinding(BindingNames.formName);
        _formEncodingType = stringValueForBinding(BindingNames.formEncodingType);
        _hasForm = _hasForm
            || _formActionUrl  != null
            || _formName != null
            || _formEncodingType != null;
    }

    protected void sleep ()
    {
        super.sleep();
        _formActionUrl = null;
        _formName = null;
        _formEncodingType = null;
        _hasForm = false;
        _currentPanel = null;
    }

    public boolean omitFormTags ()
    {
        return !_hasForm;
    }

    public boolean omitWrapperBoilerplate ()
    {
        return session().omitWrapperFrame() ||
                booleanValueForBinding(BindingNames.omitWrapperFrame);
    }

    public String bodyClass ()
    {
        String bodyClass = stringValueForBinding(BindingNames.bodyClass);
        if (!omitWrapperBoilerplate() &&
            requestContext().isPrintMode()) {
            bodyClass = StringUtil.strcat(bodyClass, " hide");
        }
        return bodyClass;
    }

    /**
     * Gets the module name from the application.
     * @return module name.
     */
    public String applicationType ()
    {
        return application().getApplicationType();
    }

    public String favIconUrl ()
    {
        return AWImage.imageUrl(requestContext(), this, "favicon.ico");
    }

    public boolean alreadyInPageWrapper ()
    {
        return env().peek("PageWrapper") != null;
    }

    /**
        Displays a pane during automation testing runs that contains TA links.
     */
    private static String TestPaneComponent = null;
    public static void registerTestPaneComponent (String componentName)
    {
        TestPaneComponent = componentName;
    }
    
    public boolean hasTestPane ()
    {
        return TestPaneComponent != null;
    }

    public String getTestPane ()
    {
        return TestPaneComponent;
    }
}
