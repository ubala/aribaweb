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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWPrivateHyperlink.java#2 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWHtmlForm;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;

public final class AWPrivateHyperlink extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.action, BindingNames.pageName, BindingNames.submitForm,
        BindingNames.omitTags, BindingNames.target};

    public AWBinding _actionBinding;
    public AWBinding _pageNameBinding;
    public AWBinding _submitFormBinding;
    public AWBinding _targetBinding;
    public AWBinding _omitTagsBinding;
    public AWBinding _namePrefixBinding;

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public boolean isStateless ()
    {
        return true;
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    public boolean submitForm ()
    {
        boolean submitForm = false;
        AWHtmlForm currentForm = requestContext().currentForm();
        if (currentForm != null) {
            submitForm = hasBinding(_submitFormBinding) ?
                booleanValueForBinding(_submitFormBinding) :
                true;
        }
        return submitForm;
    }

    public AWResponseGenerating linkClicked ()
    {
        return AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding);
    }

    public boolean omitTags ()
    {
        return (requestContext().isExportMode() || requestContext().isPrintMode()) ||
                ((_omitTagsBinding != null) && this.booleanValueForBinding(_omitTagsBinding));
    }
}
