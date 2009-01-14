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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWContainerElement.java#16 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;

import java.lang.reflect.Field;

abstract public class AWContainerElement extends AWBindableElement implements AWElementContaining
{
    private AWElement _contentElement;

    // ** Thread Safety Considerations:  Although instances of this class are shared by many threads, no locking is required because, once the parsing is complete and the _elements array is established, it is immutable.

    public void add (AWElement element)
    {
        if (_contentElement == null) {
            _contentElement = element;
        }
        else if (_contentElement instanceof AWTemplate) {
            ((AWConcreteTemplate)_contentElement).add(element);
        }
        else {
            AWConcreteTemplate contentTemplate = new AWConcreteTemplate();
            contentTemplate.init();
            contentTemplate.setTemplateName(templateName());
            contentTemplate.setLineNumber(lineNumber());
            contentTemplate.add(_contentElement);
            contentTemplate.add(element);
            _contentElement = contentTemplate;
        }
    }

    public void setContentElement (AWElement contentElement)
    {
        _contentElement = contentElement;
    }

    public AWElement contentElement ()
    {
        return _contentElement;
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (_contentElement != null) {
            try {
                if (AWConcreteApplication.IsDebuggingEnabled &&
                    !(_contentElement instanceof AWConcreteTemplate)) {
                    AWElement origElement = component.currentTemplateElement();
                    component.setCurrentTemplateElement(_contentElement);
                    _contentElement.applyValues(requestContext, component);
                    component.setCurrentTemplateElement(origElement);
                }
                else {
                    _contentElement.applyValues(requestContext, component);
                }
            }
            catch (AWGenericException ge) {
                throw ge;
            }
            catch (Throwable t) {
                throwException(t, component);
            }
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        if (_contentElement != null) {
            try {
                if (AWConcreteApplication.IsDebuggingEnabled &&
                    !(_contentElement instanceof AWConcreteTemplate)) {
                    AWElement origElement = component.currentTemplateElement();
                    component.setCurrentTemplateElement(_contentElement);
                    actionResults = _contentElement.invokeAction(requestContext, component);
                    component.setCurrentTemplateElement(origElement);
                }
                else {
                    actionResults = _contentElement.invokeAction(requestContext, component);
                }
            }
            catch (AWGenericException e) {
                if (e.additionalMessage() == null) {
                    String message = component.componentPath("\n").toString();
                    e.addMessage(message);
                }
                throw e;
            }
            catch (Throwable t) {
                throwException(t, component);
            }
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_contentElement != null) {
            try {
                if (AWConcreteApplication.IsDebuggingEnabled &&
                    !(_contentElement instanceof AWConcreteTemplate)) {
                    AWElement origElement = component.currentTemplateElement();
                    component.setCurrentTemplateElement(_contentElement);
                    _contentElement.renderResponse(requestContext, component);
                    component.setCurrentTemplateElement(origElement);
                }
                else {
                    _contentElement.renderResponse(requestContext, component);
                }
            }
            catch (AWGenericException ge) {
                throw ge;
            }
            catch (Throwable t) {
                throwException(t, component);
            }
        }
    }

    private void throwException (Throwable t, AWComponent component)
    {
        String message = component.componentPath("\n").toString();
        throw new AWGenericException(message, t);
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        if (_contentElement != null) {
            _contentElement.validate(validationContext, component);
        }
    }

    public void appendTo (StringBuffer buffer)
    {
        super.appendTo(buffer);
        if (_contentElement != null) {
            _contentElement.appendTo(buffer);
            buffer.append("</" + tagName() + ">");
        }
    }

    protected void closeString (StringBuffer buffer)
    {
        if (_contentElement == null) {
            super.closeString(buffer);
        }
        else {
            buffer.append(">");
        }
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

    ////////////////////////
    // Visitable Interface
    ///////////////////////
    public void continueVisit (AWVisitor visitor)
    {
        if (_contentElement != null) {
            _contentElement.startVisit(visitor);
        }
    }
}
