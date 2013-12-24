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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWIncludeComponent.java#12 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.MapUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AWIncludeComponent extends AWContainerElement
{
    private AWBinding _awcomponentName;
    private AWBinding _awcomponentReference;
    private AWBinding _awcomponent;
    private Map _bindingsHashtable;
    private final GrowOnlyHashtable _elementReferences = new GrowOnlyHashtable();
    private final GrowOnlyHashtable _elementReferenceIds = new GrowOnlyHashtable();
    // For collision detection
    private final GrowOnlyHashtable _elementReferencesForId = new GrowOnlyHashtable();

    public void init (String tagName, Map bindingsHashtable)
    {
        _awcomponentName = (AWBinding)bindingsHashtable.remove(AWBindingNames.awcomponentName);
        if (_awcomponentName == null) _awcomponentName = (AWBinding)bindingsHashtable.remove(AWBindingNames.name);
        _awcomponentReference = (AWBinding)bindingsHashtable.remove(AWBindingNames.awcomponentReference);
        assertValidBindings();
        if (_awcomponentName == null) {
            _awcomponent = (AWBinding)bindingsHashtable.remove(AWBindingNames.awcomponent);
        }
        _bindingsHashtable = bindingsHashtable;
        super.init(tagName, bindingsHashtable);
    }

    protected void assertValidBindings ()
    {
        Assert.that(_awcomponentName != null || _awcomponentReference != null,
                "%s: must have binding named 'awcomponentName' or 'awcomponentReference'.",
                getClass().getName());
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] myBindings = (AWBinding[])AWUtil.elements(_bindingsHashtable, AWBinding.class);
        AWBinding[] superBindings = super.allBindings();
        return (AWBinding[])(AWUtil.concatenateArrays(superBindings, myBindings));
    }

    public boolean isKindOfClass (Class targetClass)
    {
        throw new AWGenericException("AWIncludeComponent.isKindOfClass not yet implemented.");
    }

    private Map deepCopyBindingsHashtable (Map bindingsHashtable)
    {
        Map newHashtable = MapUtil.map();
        if (!bindingsHashtable.isEmpty()) {
            Iterator keyEnumerator = bindingsHashtable.keySet().iterator();
            while (keyEnumerator.hasNext()) {
                String currentKey = (String)keyEnumerator.next();
                AWBinding currentBinding = (AWBinding)bindingsHashtable.get(currentKey);
                AWBinding clonedBinding = (AWBinding)currentBinding.clone();
                newHashtable.put(currentKey, clonedBinding);
            }
        }
        return newHashtable;
    }

    protected Map bindingsForNewReference (AWComponent component)
    {
        return deepCopyBindingsHashtable(_bindingsHashtable);
    }

    protected AWBindable lookupElementReference(AWComponent component)
    {
        String componentName = componentName(component);
        if (componentName == null) {
            throw new AWGenericException("AWIncludeComponent awcomponentName must not evaluate to null: " + _awcomponentName + " at component path: " + component.componentPath());
        }

        return (AWBindable) _elementReferences.get(componentName);
    }

    protected void storeElementReference(AWElement componentReference,
                                                            String componentName, AWComponent component)
    {
        _elementReferences.put(componentName, componentReference);
    }

    protected String componentName (AWComponent component)
    {
        return _awcomponentName.stringValue(component);
    }

    public AWBindable _currentComponentReference (AWApplication application, AWComponent component)
    {
        if (_awcomponentReference != null) {
            return (AWComponentReference)_awcomponentReference.value(component);
        }
        AWBindable element = lookupElementReference(component);
        if (element == null) {
            String componentName = componentName(component);
            Assert.that(componentName != null, "Null component name not allowed for AWIncludeComponent");
            element = _createComponentReference(componentName, component, application);
            storeElementReference(element, componentName, component);
        }
        return element;
    }

    protected AWBindable _createComponentReference (String componentName, AWComponent component,
                                                    AWApplication application)
    {
        AWBindable element;
        Map newBindingsHashtable = bindingsForNewReference(component);

        // create content first so it can remove bindings
        AWElement contentElement = createContentElement(component, newBindingsHashtable, application);

        element = createElement(componentName, component, application, newBindingsHashtable);
        if (element instanceof AWElementContaining && contentElement != null) {
            ((AWElementContaining)element).add(contentElement);
        }
        return element;
    }

    protected AWElement createContentElement (AWComponent component, Map newBindingsHashtable,
                                              AWApplication application)
    {
        AWElement contentElement = contentElement();
        AWBinding awContentBinding = (AWBinding)newBindingsHashtable.remove(AWBindingNames.awcontent);
        AWBinding awContentElementBinding = (AWBinding)newBindingsHashtable.remove(AWBindingNames.awcontentElement);
        // see if we have a binding for our component content
        if (awContentBinding != null || awContentElementBinding != null) {
            Map valueBindingMap = new HashMap();
            if (awContentBinding != null) valueBindingMap.put("value", awContentBinding);
            String elementName = (awContentElementBinding != null)
                    ? awContentElementBinding.stringValue(component) : null;
            if (elementName == null) elementName = "AWString";
            contentElement = createElement(elementName, component, application, valueBindingMap);
        }
        return contentElement;
    }

    protected AWBindable createElement (String componentName, AWComponent component,
                                      AWApplication application, Map newBindingsHashtable)
    {
        AWBindable element;
        AWComponentDefinition componentDefinition = ((AWConcreteApplication)application)._componentDefinitionForName(componentName, component);
        if (componentDefinition != null) {
            element = AWComponentReference.create(componentDefinition);
            element.init(componentName, newBindingsHashtable);
            ((AWBaseElement)element).setTemplateName(templateName());
            ((AWBaseElement)element).setLineNumber(lineNumber());
        } else {
            Class elementClass = application.resourceManager().classForName(componentName);
            Assert.that(elementClass != null, "AWIncludeComponent cannot locate component named '%s'", componentName);
            Assert.that(AWUtil.classImplementsInterface(elementClass, AWBindable.class),
                    "Can't switch in component / Element that's not bindable: %s", componentName);
            try {
                element = (AWBindable)elementClass.newInstance();
                element = (AWBindable)element.determineInstance(componentName, elementClass.getName(),
                                newBindingsHashtable, templateName(), lineNumber());
            }
            catch (IllegalAccessException illegalAccessException) {
                throw new AWGenericException(illegalAccessException);
            }
            catch (InstantiationException instantiationException) {
                String errorMessage = Fmt.S("Problem creating new instance of \"%s\" (%s) templateName: \"%s\" lineNumber: %s",
                                            componentName, elementClass.getName(),
                                            templateName(), String.valueOf(lineNumber()));
                throw new AWGenericException(errorMessage, instantiationException);
            }
        }
        return element;
    }

    private AWComponent contextComponent (AWComponent component)
    {
        return _awcomponent == null ? component : (AWComponent)_awcomponent.value(component);
    }

    ///////////////
    // Cycling
    ///////////////
    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        final boolean allowsSkipping = requestContext.allowsSkipping();
        if (allowsSkipping) {
            // This block serves to skip the entire For
            AWElementIdPath targetFormIdPath = requestContext.targetFormIdPath();
            boolean prefixMatches = requestContext.nextPrefixMatches(targetFormIdPath);
            if (!prefixMatches) {
                // Since we bail out here, we must incementElementId to balance the
                // pushElementIdLevel we would have done otherwise (which increments before pushing)
                requestContext.incrementElementId();
                return;
            }
        }
        // Important! each switched-in subcomponent class must have its own elementId or else,
        // if the switched in component is stateful, the first component registered with the _subcomponents
        // cache on the root page would be the winner, and the switching effect wouldn't take place.
        AWBindable currentElement = _currentComponentReference(requestContext.application(), component);
        int id = elementReferenceId(currentElement, component, false);
        requestContext.pushElementIdLevel(id);
        currentElement.applyValues(requestContext, contextComponent(component));
        requestContext.popElementIdLevel(0);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        final boolean allowsSkipping = requestContext.allowsSkipping();
        if (allowsSkipping) {
            // This block serves to skip the entire For
            AWElementIdPath requestSenderIdPath = requestContext.requestSenderIdPath();
            boolean prefixMatches = requestContext.nextPrefixMatches(requestSenderIdPath);
            if (!prefixMatches) {
                // Since we bail out here, we must incementElementId to balance the
                // pushElementIdLevel we would have done otherwise (which increments before pushing)
                requestContext.incrementElementId();
                return null;
            }
        }
        // Important! see comment above re elementId.
        // Note: for backward compatibility always evaluate the binding for componentRef
        // even if requiresGlidStateRecording == true.
        AWBindable currentComponentReference = _currentComponentReference(requestContext.application(), component);
        int id = elementReferenceId(currentComponentReference, component, false);
        requestContext.pushElementIdLevel(id);
        AWResponseGenerating actionResults =
            currentComponentReference.invokeAction(requestContext, contextComponent(component));
        requestContext.popElementIdLevel(0);
        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // Important! see comment above re elementId.
        AWBindable currentComponentReference =
            _currentComponentReference(requestContext.application(), component);
        int id = elementReferenceId(currentComponentReference, component, true);
        requestContext.pushElementIdLevel(id);
        currentComponentReference.renderResponse(requestContext, contextComponent(component));
        requestContext.popElementIdLevel(0);
    }

    protected int elementReferenceId (AWBindable element, AWComponent component, boolean canCreate)
    {
        Assert.that(element != null, "ComponentReference passed to " +
                                                "AWIncludeComponent must be non-null.");
        if (_awcomponentReference != null) {
            return pageScopedReferenceId(element, component);
        }

        Integer index = (Integer)_elementReferenceIds.get(element);
        if (index != null) {
            return index;
        }
        boolean result = canCreate || component.requestContext().allowFailedComponentRendezvous();
        if(AWConcreteServerApplication.IsDebuggingEnabled && !result)
        {
            Iterator<Integer> iterator1 = _elementReferencesForId.keySet().iterator();  
            Log.aribaweb.debug("[AWIncludeComponent] - elementReferenceId() - Start Printing values for " +
                "_elementReferencesForId: ");
            while (iterator1.hasNext()) {  
                int iindex = iterator1.next().intValue();
                AWBindable elem  = (AWBindable) _elementReferencesForId.get(iindex);  
                Log.aribaweb.debug("[AWIncludeComponent] - elementReferenceId() - Index: " + iindex + " - " 
                    + " Element Tagname: " + elem.tagName());  
            }
            Log.aribaweb.debug("[AWIncludeComponent] - elementReferenceId() - End Printing Values for " +
                "_elementReferenceIds: "); 
            Iterator<AWBindable> iterator2 = _elementReferenceIds.keySet().iterator();
            Log.aribaweb.debug("[AWIncludeComponent] - - elementReferenceId() - Start Printing values for " +
                "_elementReferenceIds: ");
            while (iterator2.hasNext()) {  
                AWBindable elem = iterator2.next();
                int iindex = (Integer) _elementReferenceIds.get(elem);  
                Log.aribaweb.debug("[AWIncludeComponent] - elementReferenceId() - Element Tagname: " 
                    + elem.tagName() + " -  " + " Index: " + iindex);
            }
            Log.aribaweb.debug("[AWIncludeComponent] - - elementReferenceId() - End Printing values for " +
                "_elementReferenceIds: ");
        }
        Assert.that(canCreate || component.requestContext().allowFailedComponentRendezvous(),
             "IncludeComponent failed to rendezvous with existing element " 
              + "-- likely cause: illegal change between phases that shouldn't:  New ComponentReference: %s",
              element);

        index = computeIndexForElement(element, component);
        AWBindable existingElement = (AWBindable) _elementReferencesForId.get(index);
        while (existingElement != null && !_elementsAreCompatible(component, existingElement, element)) {
            // collision
            index++;
            Assert.that(index >= 0 && index < AWElementIdPath.LevelMaxSize, "elementReferenceId out of range:" + index);
            existingElement = (AWBindable) _elementReferencesForId.get(index);
        }
        _elementReferencesForId.put(index, element);
        _elementReferenceIds.put(element, index);
        return index;
    }

    protected boolean _elementsAreCompatible (AWComponent component, AWBindable orig, AWBindable element)
    {
        return false;
    }

    protected int computeIndexForElement (AWBindable element, AWComponent component)
    {
        int index = element.tagName().hashCode() % AWElementIdPath.LevelMaxSize;
        if (index < 0) index *= -1;
        return index;
    }

    protected int pageScopedReferenceId (AWBindable element, AWComponent component)
    {
        // Leak-prevention: use a page-level table for possibly dynamic component references
        Integer index;
        AWPage page = component.page();
        index = (Integer)page.get(element);
        if (index == null) {
            Integer sz = (Integer)page.get("_AWSCNextId");
            int size = (sz != null) ? sz.intValue() + 1 : 0;
            index = Constants.getInteger(size);
            page.put("_AWSCNextId", index);
            page.put(element, index);
        }
        return index.intValue();
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
}
