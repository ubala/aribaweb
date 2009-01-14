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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConcreteTemplate.java#21 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import java.util.List;
import java.lang.reflect.Array;

public final class AWConcreteTemplate extends AWTemplate implements AWElementContaining
{
    private static final AWElement[] EmptyCycleableArray = {};
    private static final int GrowthIncrement = 4;
    private AWElement[] _elements = EmptyCycleableArray;
    private int _elementCount = 0;
    private boolean _validated = false;

    // ** Thread Safety Considerations:  Although instances of this class are shared by many threads, no locking is required because, once the parsing is complete and the _elements array is established, it is immutable.

    public AWElement[] _elements ()
    {
        return _elements;
    }

    public int _elementCount ()
    {
        return _elementCount;
    }

    public void add (AWElement element)
    {
        if ((element instanceof AWBareString) && (_elementCount > 0)) {
            AWElement previouslyAddedElement = _elements[_elementCount - 1];
            if (previouslyAddedElement instanceof AWBareString) {
                String previousString = ((AWBareString)previouslyAddedElement).encodedString().string();
                String currentString = ((AWBareString)element).encodedString().string();
                String coalescedString = StringUtil.strcat(previousString, currentString);
                AWBareString newBareString = AWBareString.getInstance(coalescedString);
                element = newBareString;
                _elementCount--;
            }
        }
        if (_elementCount >= _elements.length) {
            _elements = (AWElement[])AWUtil.realloc(_elements, (_elements.length + GrowthIncrement));
        }
        _elements[_elementCount] = element;
        _elementCount++;
    }

    ////////////////////////
    // AWTemplate abstract methods
    ///////////////////////

    public boolean hasElements ()
    {
        return (_elements.length > 0);
    }

    public AWElement[] elementArray ()
    {
        if (_elements.length != _elementCount) {
            AWElement[] elementArray = new AWElement[_elementCount];
            System.arraycopy(_elements, 0, elementArray, 0, _elementCount);
            _elements = elementArray;
        }
        return _elements;
    }

    public AWElement[] extractElementsOfClass (Class targetClass)
    {
        List elementVector = ListUtil.list();
        AWElement[] elementArray = _elements;
        int elementCount = elementArray.length;
        for (int index = 0; index < elementCount; index++) {
            AWElement currentElement = elementArray[index];
            if ((currentElement != null) && targetClass.isAssignableFrom(currentElement.getClass())) {
                elementVector.add(currentElement);
            }
        }
        AWElement[] extractedElements = (AWElement[])Array.newInstance(targetClass, elementVector.size());
        elementVector.toArray(extractedElements);
        return extractedElements;
    }

    public int indexOfNamedSubtemplate (String templateName, AWComponent component)
    {
        int indexOfNamedSubtemplate = -1;
        for (int index = 0; index < _elementCount; index++) {
            AWElement currentCycleable = _elements[index];
            if (currentCycleable instanceof AWContent) {
                AWContent currentContent = (AWContent)currentCycleable;
                String currentTemplateName = currentContent.nameInComponent(component);
                if ((currentTemplateName == templateName) || currentTemplateName.equals(templateName)) {
                    indexOfNamedSubtemplate = index;
                    break;
                }
            }
        }
        return indexOfNamedSubtemplate;
    }

    public AWContent subtemplateAt (int index)
    {
        return (AWContent)_elements[index];
    }

    public boolean validated ()
    {
        return _validated;
    }


    public AWContent subtemplateForName (String templateName, AWComponent component)
    {
        int indexOfNamedSubtemplate = indexOfNamedSubtemplate(templateName, component);
        return subtemplateAt(indexOfNamedSubtemplate);
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (_elementCount > 0) {
            final boolean allowsSkipping = requestContext.allowsSkipping();
            if (AWConcreteApplication.IsDebuggingEnabled) {
                for (int index = 0; index < _elementCount; index++) {
                    AWElement originalElement = component.currentTemplateElement();
                    AWElement currentElement = _elements[index];
                    component.setCurrentTemplateElement(currentElement);
                    currentElement.applyValues(requestContext, component);
                    component.setCurrentTemplateElement(originalElement);
                    if (allowsSkipping && requestContext.targetFormIdPath() == null) {
                        // If in here, there are no more targets to "take"
                        // so break out of loop
                        break;
                    }
                }
            }
            else {
                for (int index = 0; index < _elementCount; index++) {
                    _elements[index].applyValues(requestContext, component);
                    if (allowsSkipping && requestContext.targetFormIdPath() == null) {
                        // If in here, there are no more targets to "take"
                        // so break out of loop
                        break;
                    }
                }
            }
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults = null;
        if (_elementCount > 0) {
            if (AWConcreteApplication.IsDebuggingEnabled) {
                for (int index = 0; index < _elementCount; index++) {
                    AWElement originalElement = component.currentTemplateElement();
                    component.setCurrentTemplateElement(_elements[index]);
                    actionResults = _elements[index].invokeAction(requestContext, component);
                    component.setCurrentTemplateElement(originalElement);
                    if (actionResults != null) {
                        break;
                    }
                }
            }
            else {
                for (int index = 0; index < _elementCount; index++) {
                    actionResults = _elements[index].invokeAction(requestContext, component);
                    if (actionResults != null) {
                        break;
                    }
                }
            }
        }
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_elementCount > 0) {
            if (AWConcreteApplication.IsDebuggingEnabled) {
                for (int index = 0; index < _elementCount; index++) {
                    AWElement originalElement = component.currentTemplateElement();
                    AWElement currentElement = _elements[index];
                    component.setCurrentTemplateElement(currentElement);
                    currentElement.renderResponse(requestContext, component);
                    component.setCurrentTemplateElement(originalElement);
                }
            }
            else {
                for (int index = 0; index < _elementCount; index++) {
                    _elements[index].renderResponse(requestContext, component);
                }
            }
        }
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        _validated = true;

        for (int index = 0; index < _elementCount; index++) {
            AWElement element = _elements[index];
            element.validate(validationContext, component);
        }
    }

    public AWApi removeApiTag ()
    {
        AWApi apiElement = null;
        // Fix this: assumes api is at 0.
        AWElement zerothElement = _elementCount > 0 ? _elements[0] : null;
        if (zerothElement instanceof AWApi) {
            apiElement = (AWApi)zerothElement;
            _elements = (AWElement[])AWUtil.subarray(_elements, 1, _elementCount - 1);
            _elementCount--;
        }
        return apiElement;
    }

    /**
     * Append a representation of this template to the buffer.  The representation
     * should match the original template parsed except where ordering of
     * constructs is not required (ie, ordering of bindings).
     *
     * @param buffer
     */
    public void appendTo (StringBuffer buffer)
    {
        AWElement[] elements = elementArray();
        for (int i=0; i < elements.length; i++) {
            elements[i].appendTo(buffer);
        }
    }

    ////////////////////////
    // Visitable Interface
    ///////////////////////
    public void startVisit (AWVisitor visitor)
    {
        if (_elementCount > 0) {
            for (int index = 0; index < _elementCount; index++) {
                _elements[index].startVisit(visitor);
            }
        }
    }

    ////////////////////////
    // Debugging
    ///////////////////////
    public String toString ()
    {
        return StringUtil.strcat(getClass().getName(),
                                 ": ",
                                 (ListUtil.arrayToList(_elements)).toString());
    }

    protected boolean hasNamedTemplate (AWComponent component, String templateName)
    {
        if (_elementCount > 0) {
            for (int index = 0; index < _elementCount; index++) {
                AWElement element = _elements[index];
                if (element instanceof AWContent) {
                    AWContent content = (AWContent)element;
                    if (content.isNamedTemplate(component, templateName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
