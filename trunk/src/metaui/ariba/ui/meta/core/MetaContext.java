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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/MetaContext.java#2 $
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
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Assert;
import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.util.core.Fmt;
import java.util.Map;

public class MetaContext extends AWContainerElement
{
    protected static final String EnvKey = "MetaContext";
    AWBindingDictionary _bindings;
    AWBinding _restoreActivation;
    int _hasMetaRuleChildren;

    static {
        ComponentClassExtension.init();
    }
    
    public static Context currentContext (AWComponent component)
    {
        Context context = peekContext(component);
        Assert.that(context != null, "Meta context not available on environment");        
        return context;
    }

    public static Context peekContext (AWComponent component)
    {
        AWEnvironmentStack env = component.env();
        return (Context)env.peek(EnvKey);
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _restoreActivation = (AWBinding)bindingsHashtable.remove("restoreActivation");
        _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        super.init(tagName, null);
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] superBindings = super.allBindings();
        java.util.List bindingVector = _bindings.elementsVector();
        AWBinding[] myBindings = new AWBinding[bindingVector.size()];
        bindingVector.toArray(myBindings);
        return (AWBinding[])(AWUtil.concatenateArrays(superBindings, myBindings));
    }

    protected boolean pushPop (boolean isPush, boolean needCleanup,
                               AWComponent component)
    {
        boolean didCreate = false;
        AWEnvironmentStack env = component.env();
        Context context = (Context)env.peek(EnvKey);
        if (context == null) {
            Assert.that(isPush, "Should always have context on pop");
            UIMeta meta = UIMeta.getInstance();
            context = meta.newContext();
            env.push(EnvKey, context);
            didCreate = true;
        }

        AWBindingDictionary bindings = _bindings;
        if (isPush) {
            Context.Activation activation = (_restoreActivation != null)
                    ? (Context.Activation)_restoreActivation.value(component)
                    : null;
            if (activation != null) {
                context.restoreActivation(activation);
            } else {
                context.push();
            }
            for (int index = bindings.size() - 1; index >= 0; index--) {
                AWBinding currentBinding = bindings.elementAt(index);
                Object value = currentBinding.value(component);
                String key = bindings.keyAt(index);
                context.set(key, value);
            }
            // if we haven't checked, look for and init embedded MetaRules tags
            if (_hasMetaRuleChildren == 0) {
                _hasMetaRuleChildren = initEmbeddedMetaRules(component, context) ? 1 : -1;
            }
            // If we have children, push our template on as context
            if (_hasMetaRuleChildren == 1) {
                context.merge(MetaRules.TemplateId, component.templateName());
            }
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
        try {
            super.renderResponse(requestContext, component);
        }
        catch (Exception e) {
            throw new AWGenericException(Fmt.S("Meta Context: %s", component.env().peek(EnvKey)), e);
        }
        finally {
            pushPop(false, needCleanup, component);
        }
    }

    public void applyValues (AWRequestContext requestContext, AWComponent component)
    {
        boolean needCleanup = pushPop(true, false, component);
        try {
            super.applyValues(requestContext, component);
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
            actionResults = super.invokeAction(requestContext, component);
        }
        finally {
            pushPop(false, needCleanup, component);
        }
        return actionResults;
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
