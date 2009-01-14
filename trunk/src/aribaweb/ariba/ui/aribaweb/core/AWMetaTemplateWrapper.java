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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWMetaTemplateWrapper.java#9 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;

/**
    Applications should subclass this to provide their own slave application metatemplate wrapper.
    No .awl file should be used.

    Subclasses should implement redirectUrlForPunchout() to manage the punchout operation
    and return a url to which this will redirect.
*/
abstract public class AWMetaTemplateWrapper extends AWComponent
{
    private static final String TemplateName = AWComponentDefinition.computeTemplateName(AWMetaTemplateWrapper.class);

    abstract protected String redirectUrlForPunchout (AWRequestContext requestContext, String templateString);

    public String templateName ()
    {
        return TemplateName;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        try {
            // render the page into a response and
            // convert that into a template string
            requestContext.setMetaTemplateMode(true);
            super.renderResponse(requestContext, component);
            requestContext.setMetaTemplateMode(false);
            String templateString = requestContext.response().generateStringContents();
            String slaveEntryPointUrl = redirectUrlForPunchout(requestContext, templateString);

            // create a fresh response which will redirect over to the slave app's entry url.
            // set allowIncrementalUpdateAppend to false here so the AW does not try
            // to use a fpr to get out of the current incremental update request.  Note
            // that the page component which encapsulates this AWMetaTemplateWrapper should
            // override isBoundary() to return true.
            requestContext.allowIncrementalUpdateApppend(false);
            AWRedirect.initiateRedirect(requestContext, slaveEntryPointUrl);
        }
        finally {
            requestContext.setMetaTemplateMode(false);
        }
    }
}
