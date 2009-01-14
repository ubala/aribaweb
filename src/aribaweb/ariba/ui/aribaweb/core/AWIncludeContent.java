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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWIncludeContent.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import java.util.Map;
import java.lang.reflect.Field;

public final class AWIncludeContent extends AWBindableElement
{
    private static final AWEncodedString ComponentContentTag = AWEncodedString.sharedEncodedString(AWRemotePageWrapper.DynamicTagMarker + "<AWIncludeContent");
    private static final AWEncodedString CloseTag = AWEncodedString.sharedEncodedString("/>");
    private static final AWEncodedString TemplateNameEquals = AWEncodedString.sharedEncodedString(" templateName=\"");
    private static final AWEncodedString Quote = AWEncodedString.sharedEncodedString("\"");
    private AWBinding _templateName;
    private AWBinding _template;
    private AWBinding _required;
    private AWBinding _context;
    private boolean _isMeta;

    // ** Thread Safety Considerations: AWBinding is immutable so no locking required for this class.

    public void init (String tagName, Map bindingsHashtable)
    {
        _templateName = (AWBinding)bindingsHashtable.remove(AWBindingNames.templateName);
        if (_templateName == null) _templateName = (AWBinding)bindingsHashtable.remove(AWBindingNames.name);
        _required = (AWBinding)bindingsHashtable.remove(AWBindingNames.required);
        _template = (AWBinding)bindingsHashtable.remove(AWBindingNames.template);
        _context = (AWBinding)bindingsHashtable.remove(AWBindingNames.context);
        AWBinding isMetaBinding = (AWBinding)bindingsHashtable.remove(AWBindingNames.isMeta);
        _isMeta = isMetaBinding == null ? false : isMetaBinding.booleanValue(null);
        super.init(tagName, bindingsHashtable);
    }

    public AWBinding _templateName ()
    {
        return _templateName;
    }

    public AWBinding _template ()
    {
        return _template;
    }

    public AWContent _namedSubtemplate (AWComponent subcomponent, AWComponent context, AWElement contentElement, AWRequestContext requestContext)
    {
        AWContent namedSubtemplate = null;
        int indexOfNamedSubtemplate = 0;
        String templateName = _templateName.stringValue(subcomponent);
        if (contentElement instanceof AWTemplate) {
            AWTemplate contentTemplate = ((AWTemplate)contentElement);
            indexOfNamedSubtemplate = contentTemplate.indexOfNamedSubtemplate(templateName, context);
            if (indexOfNamedSubtemplate != -1) {
                namedSubtemplate = contentTemplate.subtemplateAt(indexOfNamedSubtemplate);
            }
        }
        else if (contentElement instanceof AWContent) {
            String namedTemplateName = ((AWContent)contentElement).nameInComponent(subcomponent);
            if ((templateName == namedTemplateName) || templateName.equals(namedTemplateName)) {
                namedSubtemplate = (AWContent)contentElement;
            }
        }

        if (namedSubtemplate != null) {
            if (requestContext != null) {
                requestContext.pushElementIdLevel(indexOfNamedSubtemplate);
            }
        }
        else {
            boolean isRequired = (_required == null) ? true : _required.booleanValue(subcomponent);
            if (isRequired) {
                throw new AWGenericException(getClass().getName() + ": reference to Named Content not found: \"" + templateName + "\"");
            }
        }
        return namedSubtemplate;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent subcomponent)
    {
        AWComponent contextComponent = contextComponent(subcomponent);
        AWComponent parentComponent = contextComponent.parent();
        if (_template != null) {
            AWElement templateElement = (AWElement)_template.value(subcomponent);
            if (templateElement != null) {
                requestContext.pushElementIdLevel();
                templateElement.applyValues(requestContext, parentComponent);
                requestContext.popElementIdLevel();
            }
        }
        else {
            AWElement contentElement = contextComponent.componentReference().contentElement();
            if (contentElement != null) {
                if (_templateName != null) {
                    AWContent namedSubtemplate = _namedSubtemplate(subcomponent, contextComponent, contentElement, requestContext);
                    if (namedSubtemplate != null) {
                        try {
                            // namedSubtemplate does the pushElementIdLevel if
                            // necessary (this needs some rework for symmetry
                            // purposes)
                            namedSubtemplate._applyValues(requestContext, parentComponent);
                        }
                        catch (AWGenericException ag) {
                            throwException(ag, namedSubtemplate);
                        }
                        catch (Throwable t) {
                            throwException(t, namedSubtemplate);
                        }
                        requestContext.popElementIdLevel(0);
                    }
                }
                else {
                    contentElement.applyValues(requestContext, parentComponent);
                }
            }
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent subcomponent)
    {
        AWResponseGenerating actionResults = null;
        AWComponent contextComponent = contextComponent(subcomponent);
        AWComponent parentComponent = contextComponent.parent();
        if (_template != null) {
            AWElement templateElement = (AWElement)_template.value(subcomponent);
            if (templateElement != null) {
                requestContext.pushElementIdLevel();
                try {
                    actionResults = templateElement.invokeAction(
                        requestContext,
                        parentComponent);
                }
                catch (AWGenericException ag) {
                    throwException(ag, templateElement);
                }
                catch (Throwable t) {
                    throwException(t, templateElement);
                }
                requestContext.popElementIdLevel();
            }
        }
        else {
            AWElement contentElement = contextComponent.componentReference().contentElement();
            if (contentElement != null) {
                if (_templateName != null) {
                    AWContent namedSubtemplate = _namedSubtemplate(subcomponent, contextComponent, contentElement, requestContext);
                    if (namedSubtemplate != null) {
                        // namedSubtemplate does the pushElementIdLevel if necessary (this needs some rework for symmetry purposes)
                        try {
                            // namedSubtemplate does the pushElementIdLevel if
                            // necessary (this needs some rework for symmetry
                            // purposes)
                            actionResults = namedSubtemplate._invokeAction(
                                requestContext,
                                parentComponent);
                        }
                        catch (AWGenericException ag) {
                            throwException(ag, namedSubtemplate);
                        }
                        catch (Throwable t) {
                            throwException(t, namedSubtemplate);
                        }
                        requestContext.popElementIdLevel(0);
                    }
                }
                else {
                    try {
                        actionResults = contentElement.invokeAction(
                            requestContext,
                            parentComponent);
                    }
                    catch (AWGenericException ag) {
                        throwException(ag, contentElement);
                    }
                    catch (Throwable t) {
                        throwException(t, contentElement);
                    }
                }
            }
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent subcomponent)
    {
        if (_isMeta && requestContext.isMetaTemplateMode()) {
            AWResponse response = requestContext.response();
            response.appendContent(ComponentContentTag);
            if (_templateName != null) {
                AWEncodedString templateName = _templateName.encodedStringValue(subcomponent);
                response.appendContent(TemplateNameEquals);
                response.appendContent(templateName);
                response.appendContent(Quote);
            }
            response.appendContent(CloseTag);
            return;
        }

        AWComponent contextComponent = contextComponent(subcomponent);
        AWComponent parentComponent = contextComponent.parent();
        if (_template != null) {
            AWElement templateElement = (AWElement)_template.value(subcomponent);
            if (templateElement != null) {
                requestContext.pushElementIdLevel();
                try {
                    templateElement.renderResponse(requestContext, parentComponent);
                }
                catch (AWGenericException ag) {
                    throwException(ag, templateElement);
                }
                catch (Throwable t) {
                    throwException(t, templateElement);
                }
                requestContext.popElementIdLevel();
            }
        }
        else {
            AWElement contentElement = contextComponent.componentReference().contentElement();
            if (contentElement != null) {
                if (_templateName != null) {
                    AWContent namedSubtemplate = _namedSubtemplate(subcomponent, contextComponent, contentElement, requestContext);
                    if (namedSubtemplate != null) {
                        try {
                            // namedSubtemplate does the pushElementIdLevel if
                            // necessary (this needs some rework for symmetry
                            // purposes)
                            namedSubtemplate._renderResponse(
                                requestContext,
                                parentComponent);
                        }
                        catch (AWGenericException ag) {
                            throwException(ag, namedSubtemplate);
                        }
                        catch (Throwable t) {
                            throwException(t, namedSubtemplate);
                        }
                        requestContext.popElementIdLevel(0);
                    }
                }
                else {
                    try {
                        contentElement.renderResponse(requestContext, parentComponent);
                    }
                    catch (AWGenericException ag) {
                        throwException(ag, contentElement);
                    }
                    catch(Throwable t) {
                        throwException(t, contentElement);
                    }
                }
            }
        }
    }

    private void throwException (Throwable t, AWElement element)
    {
        throwException(new AWGenericException(t), element);
    }

    private void throwException (AWGenericException ag, AWElement element)
    {
        AWBaseElement bodyRef = (element instanceof AWBaseElement)
                ? ((AWBaseElement)element) : this;
        ag.addReferenceElement(bodyRef);
        throw ag;
    }
    
    private AWComponent contextComponent (AWComponent subcomponent)
    {
        AWComponent contextComponent;
        if (_context != null) {
            contextComponent = (AWComponent)_context.value(subcomponent);
            Assert.that(contextComponent != null,
                        Fmt.S("Context component cannot be null.  Binding: %s, sub component: %s",
                              _context.bindingDescription(),
                              subcomponent));
        }
        else {
            contextComponent = subcomponent;
        }
        return contextComponent;
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

    protected boolean hasNamedTemplate (AWComponent component)
    {
        boolean hasNamedTemplate = false;
        if (_templateName != null) {
            String templateName = _templateName.stringValue(component);
            hasNamedTemplate = component.doesReferenceHaveNamedTemplate(templateName);
        }
        return hasNamedTemplate;
    }
}
