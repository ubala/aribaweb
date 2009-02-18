/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/MetaIncludeComponent.java#13 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWBindable;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWIncludeComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWElementIdPath;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWConcreteTemplate;
import ariba.ui.aribaweb.core.AWContent;
import ariba.ui.aribaweb.core.AWElementContaining;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldPath;

import java.util.Map;
import java.util.List;

/**
    MetaIncludeComponent is (along with MetaContext) the key element for binding MetaUI into
    AribaWeb user interfaces.

    MetaIncludeComponent dynamically switches in an AWComponent (or other AWElement) based on
    the current MetaContext's 'component' property and sets its bindings from the 'bindings' property.
    This alone enables almost any existing AW widget to be specified for use for a particular field
    or layout using rules -- without any additional glue code or "adaptor components".

    MetaIncludeComponent support additional, more sophisticated, component bindings:  wrapping the main
    component using 'wrapperComponent' and 'wrapperBindings', binding component content using the bindings
    'awcontent' and 'awcontentElement', and event binding named Content templates using an 'awcontentLayouts'
    map binding.
 */
public class MetaIncludeComponent extends AWIncludeComponent
{
    private AWBinding _awpropertyMap;

    public void init (String tagName, Map bindingsHashtable)
    {
        _awpropertyMap = (AWBinding)bindingsHashtable.remove("awpropertyMap");
        super.init(tagName, bindingsHashtable);
    }


    protected void assertValidBindings ()
    {
        // override parent check to no-op
    }

    protected Meta.PropertyMap properties (AWComponent component)
    {
        Meta.PropertyMap map = (_awpropertyMap != null)
                ? (Meta.PropertyMap)_awpropertyMap.value(component) : null;
        if (map == null) {
            map = MetaContext.currentContext(component).allProperties();
        }
        return map;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // if in debugTracing mode, push context info
        if (requestContext.componentPathDebuggingEnabled()) {
            Context context = MetaContext.currentContext(component);
            requestContext.debugTrace().pushMetadata(null, context.debugTracePropertyProvider(), true);
        }

        try {
            super.renderResponse(requestContext, component);
        }
        catch (Exception e) {
            throw AWGenericException.augmentedExceptionWithMessage(Fmt.S("Exception during MetaInclude with context: %s",
                    MetaContext.currentContext(component)),
                    e);
        }
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        AWResponseGenerating actionResults =  super.invokeAction(requestContext, component);

        // ComponentPath inspection
        if (actionResults != null && requestContext.isPathDebugRequest()) {
            Context context = MetaContext.currentContext(component);
            requestContext.debugTrace().pushMetadata(null, context.debugTracePropertyProvider(), true);
        }

        return actionResults;
    }    

    protected String componentName (AWComponent component)
    {
        String name = componentName(component, UIMeta.KeyComponentName);
        if (name == null && AWConcreteApplication.IsDebuggingEnabled) {
            return "MetaNoComponent";
        }
        return name;
    }

    protected String componentName (AWComponent component, String propertyKey)
    {
        Object name = properties(component).get(propertyKey);
        return (name == null) ? null
            : (name instanceof String)
                ? (String)name
                : ((String)MetaContext.currentContext(component).resolveValue(name));
    }

    protected Map bindingsForNewReference (AWComponent component)
    {
        Map <String, Object> more = (Map)properties(component).get(UIMeta.KeyBindings);
        return addBindingsForNewReference(component,
                super.bindingsForNewReference(component), more);
    }

    protected Map addBindingsForNewReference (AWComponent component, Map result, Map <String, Object> more)
    {
        if (more != null) {
            for (Map.Entry <String, Object> entry : more.entrySet()) {
                AWBinding binding;
                Object value = entry.getValue();
                if (value instanceof FieldPath) {
                    if (((FieldPath)value).car().equals("component")) {
                        binding = AWBinding.bindingWithNameAndKeyPath(entry.getKey(),
                                ((FieldPath)value).cdr().fieldPathString());
                    }
                    else {
                        binding = new ContextFieldPathBinding();
                        ((ContextFieldPathBinding)binding).init(entry.getKey(), (FieldPath)value);
                    }
                }
                else if (value instanceof PropertyValue.Dynamic) {
                    binding = new DynamicValueBinding();
                    ((DynamicValueBinding)binding).init(entry.getKey(), (PropertyValue.Dynamic)value);
                }
                // Todo: ack!  Need a more formal packaging of deferred bindings...
                else if (value instanceof List && ((List)value).size() == 1 && (((List)value).get(0) instanceof PropertyValue.Dynamic)) {
                    binding = new DynamicValueBinding();
                    ((DynamicValueBinding)binding).init(entry.getKey(), (PropertyValue.Dynamic)((List)value).get(0));
                }
                else {
                    binding = AWBinding.bindingWithNameAndConstant(entry.getKey(), value);
                }
                result.put(entry.getKey(), binding);
            }
        }
        return result;
    }

    private static final String ComponentReferenceMapKey = "MSComMap";

    protected Map componentReferenceMap (AWComponent component)
    {
        Meta meta = MetaContext.currentContext(component).meta();
        return meta.identityCache();
    }

    protected AWBindable lookupElementReference(AWComponent component)
    {
        // we use the Property Map (identity) as our key
        Map properties = properties(component);
        return (AWBindable)componentReferenceMap(component).get(properties);
    }

    protected void storeElementReference(AWElement componentReference,
                                                            String componentName, AWComponent component)
    {
        Map properties = properties(component);
        componentReferenceMap(component).put(properties, componentReference);
    }

    protected int computeIndexForElement (AWBindable element, AWComponent component)
    {
        // for stability, our id is based on the contents of our property map
        int index = properties(component).hashCode() % AWElementIdPath.LevelMaxSize;
        if (index < 0) index *= -1;
        return index;
    }

    protected AWBindable _createComponentReference (String componentName, AWComponent component,
                                                    AWApplication application)
    {
        AWBindable element = super._createComponentReference(componentName, component, application);

        String wrapperName = componentName(component, "wrapperComponent");
        if (wrapperName != null) {
            Map wrapperBindings = addBindingsForNewReference(component, MapUtil.map(),
                                                    (Map)properties(component).get("wrapperBindings"));
            AWBindable wrapperElement = createElement(wrapperName, component, application, wrapperBindings);
            Assert.that((wrapperElement instanceof AWElementContaining),
                    "Wrapper component not instance of AWElementContaining: %s", wrapperName);
            ((AWElementContaining)wrapperElement).add(element);
            element = wrapperElement;
        }

        if (element instanceof AWComponentReference) {
            ((AWComponentReference)element).setUserData(properties(component));
        }
        return element;
    }

    protected AWElement createContentElement (AWComponent component, Map newBindingsHashtable,
                                              AWApplication application)
    {
        AWBinding contentLayoutsBinding = (AWBinding)newBindingsHashtable.remove("awcontentLayouts");
        if (contentLayoutsBinding != null) {
            Map<String, String> contentToLayout = (Map)contentLayoutsBinding.value(component);
            if (!MapUtil.nullOrEmptyMap(contentToLayout)) {
                String name;
                if (contentToLayout.size() == 1 && ((name = contentToLayout.get("_main")) != null)) {
                    return createLayoutInclude(name);
                } else {
                    // Template -*> AWContent (name:key) --> MetaContext (layout:value) --> MetaInclude
                    AWConcreteTemplate concreteTemplate = new AWConcreteTemplate();
                    concreteTemplate.init();
                    for (Map.Entry e : contentToLayout.entrySet()) {
                        AWContent content = new AWContent();
                        content.init("AWContent", AWUtil.map(AWBindingNames.name,
                                AWBinding.bindingWithNameAndConstant(AWBindingNames.name, e.getKey())));
                        content.setTemplateName(templateName());
                        content.add(createLayoutInclude((String)e.getValue()));
                        concreteTemplate.add(content);
                    }
                    return concreteTemplate;
                }
            }
        }
        return super.createContentElement(component, newBindingsHashtable, application);
    }

    private AWElement createLayoutInclude(String layoutName) {
        MetaContext metaContext = new MetaContext();
        metaContext.init("MetaContext", AWUtil.map(UIMeta.KeyLayout,
                AWBinding.bindingWithNameAndConstant(UIMeta.KeyLayout, layoutName)));
        metaContext.setTemplateName(templateName());
        MetaIncludeComponent metaInclude = new MetaIncludeComponent();
        metaInclude.setTemplateName(templateName());
        metaInclude.init("MetaIncludeComponent", MapUtil.map());
        metaContext.add(metaInclude);
        return metaContext;
    }


    protected boolean _elementsAreCompatible (AWComponent component, AWBindable orig, AWBindable element)
    {
        // stash away our properties map for use in _elementsAreCompatible check
        if (element instanceof AWComponentReference) {
            ((AWComponentReference)element).setUserData(properties(component));
        }

        if (element instanceof AWComponentReference) {
            Map origProperties = (Map)((AWComponentReference)element).userData();
            Map properties = properties(component);
            if (origProperties != null && origProperties.equals(properties)) {
                return true;
            }
        }
        return false;
    }

    static class DynamicValueBinding extends AWBinding
    {
        private PropertyValue.Dynamic _dynamicValue;

        public void init (String bindingName, PropertyValue.Dynamic value)
        {
            this.init(bindingName);
            _dynamicValue = value;

        }

        public boolean isConstantValue ()
        {
            return false;
        }

        public boolean isSettableInComponent (Object object)
        {
            return false;
        }

        public void setValue (Object value, Object object)
        {
            Assert.that(false, "Can't set through a Dynamic Value");
        }

        public Object value (Object object)
        {
            Context context = MetaContext.currentContext((AWComponent)object);
            return _dynamicValue.evaluate(context);
        }

        protected String bindingDescription ()
        {
            return "DynamicValue:" + _dynamicValue.toString();
        }
    }

    static class ContextFieldPathBinding extends AWBinding
    {
        private FieldPath _fieldPath;

        public void init (String bindingName, FieldPath fieldPath)
        {
            this.init(bindingName);
            _fieldPath = fieldPath;

        }

        public boolean isConstantValue ()
        {
            return false;
        }

        public boolean isSettableInComponent (Object object)
        {
            return true;
        }

        public void setValue (Object value, Object object)
        {
            Context context = MetaContext.currentContext((AWComponent)object);
            _fieldPath.setFieldValue(context, value);
        }

        public Object value (Object object)
        {
            Context context = MetaContext.currentContext((AWComponent)object);
            return _fieldPath.getFieldValue(context);
        }

        protected String bindingDescription ()
        {
            return "FieldPath:" + _fieldPath.toString();
        }
    }
}
