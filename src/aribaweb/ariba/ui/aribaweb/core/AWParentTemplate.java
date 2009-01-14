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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWParentTemplate.java#6 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWResource;

import java.util.Map;
import java.io.InputStream;

/**
 * This element serves to include the template defined by the parent component
 * at the point of reference.  This allows for a component subclass' template to
 * look as follows:
 *
 *     :
 *     preamble
 *     <AWParentTemplate/>
 *     postamble
 *     :
 */
public final class AWParentTemplate extends AWBaseElement
{
    private AWTemplate _parentTemplate;

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        setTemplateName(templateName);
        setLineNumber(lineNumber);
        return this;
    }

    private AWTemplate parentTemplate (AWComponent component)
    {
        if (_parentTemplate == null || AWConcreteApplication.IsRapidTurnaroundEnabled) {
            // Note: we must reparse the template for each occurrence of AWParentTemplate
            // to avoid certain caching issues with AWComponentReference.userData (see AWIncludeBlock's usage)
            // As such, we cannot use the resource.object() to store the template, although we can
            // use the hasChanged().
            AWResource resource = component.parentTemplateResource();
            if (_parentTemplate == null || resource.hasChanged()) {
                InputStream inputStream = resource.inputStream();
                _parentTemplate = component.parseTemplate(inputStream);
            }
        }
        return _parentTemplate;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        AWTemplate template = parentTemplate(component);
        template.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWTemplate template = parentTemplate(component);
        return template.invokeAction(requestContext, component);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        AWTemplate template = parentTemplate(component);
        template.renderResponse(requestContext, component);
    }
}
