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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/MetaContext.java#11 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingDictionary;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWContainerElement;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.util.core.Assert;
import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.Fmt;

import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
    AW Element for manipulating a MetaUI Context as part of AribaWeb component processing.
    See AWApi for more info... 
 */
public class MetaContext extends AWContainerElement
{
    protected static final String EnvKey = "MetaContext";
    protected static final String ValueMapBindingKey = "valueMap";
    protected static final String scopeKeyBindingKey = "scopeKey";
    protected static final String PushNewContextBindingKey = "pushNewContext";
    AWBindingDictionary _bindings;
    AWBinding _scopeKeyBinding;
    AWBinding _valueMapBinding;
    AWBinding _pushNewContextBinding;
    int _hasMetaRuleChildren;

    static {
        ComponentClassExtension.init();
    }
    
    public static UIMeta.UIContext currentContext (AWComponent component)
    {
        UIMeta.UIContext context = peekContext(component);
        Assert.that(context != null, "Meta context not available on environment");        
        return context;
    }

    public static UIMeta.UIContext peekContext (AWComponent component)
    {
        AWEnvironmentStack env = component.env();
        return (UIMeta.UIContext)env.peek(EnvKey);
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _valueMapBinding = (AWBinding)bindingsHashtable.remove(ValueMapBindingKey);
        _scopeKeyBinding = (AWBinding)bindingsHashtable.remove(scopeKeyBindingKey);
        _pushNewContextBinding = (AWBinding)bindingsHashtable.remove(PushNewContextBindingKey);
        _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        super.init(tagName, null);
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] superBindings = super.allBindings();
        java.util.List bindingVector = _bindings.elementsVector();
        AWBinding[] myBindings = new AWBinding[bindingVector.size()];
        // Todo: add back _valueMapBinding
        bindingVector.toArray(myBindings);
        return (AWBinding[])(AWUtil.concatenateArrays(superBindings, myBindings));
    }

    protected boolean pushPop (boolean isPush, boolean needCleanup,
                               AWComponent component)
    {
        boolean didCreate = false;
        AWEnvironmentStack env = component.env();
        Context context = (Context)env.peek(EnvKey);
        Assert.that(isPush || context != null, "Should always have context on pop");
        boolean forceCreate = isPush && (_pushNewContextBinding != null && _pushNewContextBinding.booleanValue(component) );
        if (context == null || forceCreate) {
            UIMeta meta = UIMeta.getInstance();
            context = meta.newContext();
            ((UIMeta.UIContext)context).setRequestContext(component.requestContext());
            env.push(EnvKey, context);
            didCreate = true;
        }

        if (component.requestContext().currentPhase() == AWRequestContext.Phase_Render) {
            // if we haven't checked, look for and init embedded MetaRules tags
            if (_hasMetaRuleChildren == 0) {
                _hasMetaRuleChildren = initEmbeddedMetaRules(component, context) ? 1 : -1;
            }
        }

        // If we have children, push our template on as context
        if (_hasMetaRuleChildren == 1) {
            context.merge(MetaRules.TemplateId, component.templateName());
        }

        AWBindingDictionary bindings = _bindings;
        if (isPush) {
            context.push();
            String scopeKey = (_scopeKeyBinding != null)
                                    ? (String)_scopeKeyBinding.value(component) : null;
            Map <String, Object> values;
            if (_valueMapBinding != null && ((values = (Map)_valueMapBinding.value(component)) != null)) {
                // ToDo: sort based on Meta (KeyData) defined rank (e.g. module -> class -> operation ...)
                // ToDo: cache sort?
                List<String> sortedKeys = new ArrayList(values.keySet());
                Collections.sort(sortedKeys);
                for (String key : sortedKeys) {
                    if (key.equals(scopeKeyBindingKey)) {
                        scopeKey = (String)values.get(key);
                    } else {
                        context.set(key, values.get(key));
                    }
                }
            }

            for (int index = bindings.size() - 1; index >= 0; index--) {
                AWBinding currentBinding = bindings.elementAt(index);
                Object value = currentBinding.value(component);
                String key = bindings.keyAt(index);
                context.set(key, value);
            }

            if (scopeKey != null) context.setScopeKey(scopeKey);
        } else {
            context.pop();
        }
        if (needCleanup) {
            env.pop(EnvKey);
        }
        return didCreate;
    }

    private boolean initEmbeddedMetaRules(AWComponent component, Context context)
    {
        boolean hasOne = false;
        if (contentElement() instanceof AWTemplate) {
            AWTemplate template = (AWTemplate)contentElement();

            AWElement[] elements = template.elementArray();
            for (int i=0; i < elements.length; i++) {
                AWElement currElement = elements[i];
                if (currElement instanceof MetaRules) {
                    ((MetaRules)currElement).initWithContext(context, component);
                    hasOne = true;
                }
            }
        }
        return hasOne;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        boolean needCleanup = pushPop(true, false, component);
        boolean didPushTrace = false;
        if (requestContext.componentPathDebuggingEnabled()) {
            Context context = MetaContext.currentContext(component);
            Object provider = context.debugTracePropertyProvider();
            if (provider != null) {
                AWDebugTrace debugTrace = requestContext.debugTrace();
                debugTrace.pushMetadata(null, provider, true);
                debugTrace.pushTraceNode(this);
                didPushTrace = true;
            }
        }

        boolean shouldPushSemanticKeyPrefix =
                (AWConcreteApplication.IsAutomationTestModeEnabled &&
                requestContext.isDebuggingEnabled() &&
                !AWBindingNames.UseNamePrefixBinding) ||
                requestContext._debugShouldRecord();
        if (shouldPushSemanticKeyPrefix) {
            requestContext._debugPushSemanticKeyPrefix();
            Context context = MetaContext.currentContext(component);
            String key = context._currentPropertyScopeKey();
            if (key != null) {
                Object value = context.values().get(key);
                if (value != null) {
                    requestContext._debugSetSemanticKeyPrefix(key.concat("=").concat(value.toString()));
                }
            }
        }
        
        try {
            super.renderResponse(requestContext, component);
        }
        catch (Exception e) {
            throwAugmentedException(e, component);
        }
        finally {
            if (didPushTrace) {
                requestContext.debugTrace().popTraceNode();
                requestContext.debugTrace().popMetadata();
            }
            
            pushPop(false, needCleanup, component);

            if (shouldPushSemanticKeyPrefix) {
                requestContext._debugPopSemanticKeyPrefix();
            }
        }
    }

    public void applyValues (AWRequestContext requestContext, AWComponent component)
    {
        boolean needCleanup = pushPop(true, false, component);
        try {
            super.applyValues(requestContext, component);
        }
        catch (Exception e) {
            throwAugmentedException(e, component);
        }
        finally {
            pushPop(false, needCleanup, component);
        }
    }

    public AWResponseGenerating invokeAction (AWRequestContext requestContext, AWComponent component)
    {
        boolean needCleanup = pushPop(true, false, component);
        AWResponseGenerating actionResults = null;
        try {
            AWEncodedString elementId = AWConcreteApplication.IsDebuggingEnabled
                && requestContext.isPathDebugRequest() ? 
                    requestContext.currentElementId() : null;
            actionResults = super.invokeAction(requestContext, component);

            if (actionResults != null && requestContext.isPathDebugRequest()) {
                Context context = MetaContext.currentContext(component);
                Object provider = context.debugTracePropertyProvider();
                if (provider != null) {
                    requestContext.debugTrace().pushComponentPathEntry(this, elementId);
                    requestContext.debugTrace().pushMetadata(null, provider, true);
                }
            }

        }
        catch (Exception e) {
            throwAugmentedException(e, component);
        }
        finally {
            pushPop(false, needCleanup, component);
        }
        return actionResults;
    }

    void throwAugmentedException (Exception e, AWComponent component)
    {
        // don't add our context if a deeper context already did
        if ((e instanceof AWGenericException)
                && (((AWGenericException)e).additionalMessage() != null)
                && ((AWGenericException)e).additionalMessage().contains("-- Meta Context:")) {
            throw (AWGenericException)e;
        }
        throw AWGenericException.augmentedExceptionWithMessage(
                Fmt.S("-- Meta Context: %s", ((Context)component.env().peek(EnvKey)).debugString()), e);
    }
    
    // Register the accessor "metaContext" on AWComponent
    public static class ComponentClassExtension extends ClassExtension
    {
        protected static final ClassExtensionRegistry
            _ClassExtensionRegistry = new ClassExtensionRegistry();

        public static ComponentClassExtension get (Object target)
        {
            return (ComponentClassExtension)_ClassExtensionRegistry.get(target);
        }

        public static void init () {
            _ClassExtensionRegistry.registerClassExtension(AWComponent.class,
                                              new ComponentClassExtension());
        }

        public Object metaContext (Object target)
        {
            return currentContext(((AWComponent)target));
        }
    }
}
