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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWContent.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;
import ariba.ui.aribaweb.util.AWUtil;
import java.util.Map;
import java.lang.reflect.Field;

public final class AWContent extends AWContainerElement
{
    private AWBinding _templateName;
    private AWBinding _disabled;

    // ** Thread Safety Considerations: This is shared but all immutable ivars.

    public void init (String tagName, Map bindingsHashtable)
    {
        _templateName = (AWBinding)bindingsHashtable.remove(AWBindingNames.templateName);
        _disabled = (AWBinding)bindingsHashtable.remove(AWBindingNames.disabled);
        if (_templateName == null) _templateName = (AWBinding)bindingsHashtable.remove(AWBindingNames.name);
        Assert.that(_templateName != null,
                    "AWContent missing 'name' binding.");
        AWBinding parentTemplateNameBinding =
            (AWBinding)bindingsHashtable.remove(AWBindingNames.parentTemplateName);
        if (parentTemplateNameBinding != null) {
            //  if parentTemplateNameBinding is non-null if we have
            // <AWContent templateName="Foo" parentTemplateName="Bar"/>
            // which is a convenience mechanism for
            // <AWContent templateName="Foo">
            //     <AWIncludeContent templateName="Bar"/>
            // </AWContent>
            bindingsHashtable.put(AWBindingNames.templateName, parentTemplateNameBinding);
            AWIncludeContent includeContent = new AWIncludeContent();
            includeContent.init("AWIncludeContent", bindingsHashtable);
            setContentElement(includeContent);
        }
        super.init(tagName, bindingsHashtable);
    }

    public void setContentElement (AWElement contentElement)
    {
        Assert.that(contentElement() == null,
            "attempt to modify contentElement() -- probably AWContent container " +
            "with 'parentTemplateName' (must be non-container) %s:%s",
            templateName(), AWUtil.toString(lineNumber()));
        super.setContentElement(contentElement);
    }

    public String nameInComponent (AWComponent component)
    {
        return _templateName.stringValue(component);
    }

    public boolean enabled (AWComponent component)
    {
        return (_disabled == null) || !(component.booleanValueForBinding(_disabled));
    }

    protected Object getFieldValue (Field field)
      throws IllegalArgumentException, IllegalAccessException
    {
         try {
            return field.get(this);
        }
        catch (IllegalAccessException ex) {
            return super.getFieldValue(field);
        }
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        // AWContent is a nop for all these -- you must refer to the contentElement
        // to pass these along.  This is done this way to allow for AWNamedTemplates
        // within an unnamed content and have them be skipped.
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        // AWContent is a nop for all these -- you must refer to the contentElement
        // to pass these along.  This is done this way to allow for AWNamedTemplates
        // within an unnamed content and have them be skipped.
        return null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // AWContent is a nop for all these -- you must refer to the contentElement
        // to pass these along.  This is done this way to allow for AWNamedTemplates
        // within an unnamed content and have them be skipped.
    }

    protected void _applyValues(AWRequestContext requestContext,
                                           AWComponent component)
    {
        AWElement contentElement = contentElement();
        if (contentElement != null) {
            contentElement.applyValues(requestContext, component);
        }
    }

    protected AWResponseGenerating _invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        AWElement contentElement = contentElement();
        if (contentElement != null) {
            actionResults =
                contentElement.invokeAction(requestContext, component);
        }
        return actionResults;
    }

    protected void _renderResponse(AWRequestContext requestContext,
                                      AWComponent component)
    {
        AWElement contentElement = contentElement();
        if (contentElement != null) {
            contentElement.renderResponse(requestContext, component);
        }
    }

    protected boolean isNamedTemplate (AWComponent component, String templateName)
    {
        boolean hasNamedTemplate =
            _templateName.stringValue(component).equals(templateName);
        AWElement contentElement = contentElement();
        if (contentElement instanceof AWIncludeContent) {
            hasNamedTemplate =
                ((AWIncludeContent)contentElement).hasNamedTemplate(component);
        }
        // todo: need to add check for else case where contentElement is a template which
        // todo: contains a single AWIncludeContent with only whitespace around it.
        return hasNamedTemplate;
    }
}
