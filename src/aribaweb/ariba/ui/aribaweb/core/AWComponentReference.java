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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponentReference.java#65 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWRecyclePool;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import java.util.Map;
import ariba.util.core.StringUtil;
import java.util.List;
import java.util.LinkedHashMap;

import ariba.util.core.Assert;
import java.lang.reflect.Field;

// subclassed by toolkit/AJComponentReference.java
public class AWComponentReference extends AWContainerElement
{
    protected static final boolean LogComponentEvaluation = false;
    private static final AWBindingDictionary EmptyBindings = new AWBindingDictionary();
    private static final AWEncodedString OpenShowDebugTag =
            AWEncodedString.sharedEncodedString("<span onMouseOver=\"return ariba.Debug.showDebug(event);\" id=\"");
    private static final AWEncodedString CloseShowDebugTag =
            AWEncodedString.sharedEncodedString("</span>");
    private AWBinding _awbindingsDictionary;
    private AWBindingDictionary _bindings;
    private AWBindingDictionary _otherBindings;
    private final AWComponentDefinition _componentDefinition;
    private AWRecyclePool _sharedComponentPool;
    private AWConcreteXmlNode _rootXmlNode;
    private Object _userData;
    protected boolean _isStateless;
    private boolean _useLocalPool;
    private boolean _allowCascadedBindings = false;

    // ** Thread Safety Considerations: This is shared but ivars are immutable.

    public static AWComponentReference create (AWComponentDefinition componentDefinition)
    {
        AWComponentReference result = null;
        if (_CreatorInstance != null) result = _CreatorInstance.createComponentReference(componentDefinition);
        return (result != null) ? result : new AWComponentReference(componentDefinition);
    }

    public interface _Creator {
        public AWComponentReference createComponentReference (AWComponentDefinition definition);
    }

    protected static _Creator _CreatorInstance;
    protected static void _registerCreator (_Creator creator)
    {
        Assert.that(_CreatorInstance == null, "Can only register at most on Creator");
        _CreatorInstance = creator;
    }

    protected AWComponentReference (AWComponentDefinition componentDefinition)
    {
        _componentDefinition = componentDefinition;
        _bindings = EmptyBindings;
    }

    public void init (String tagName, Map bindingsHashtable)
    {
        _awbindingsDictionary = (AWBinding)bindingsHashtable.remove(AWBindingNames.awbindingsDictionary);
        String supportedBindingNames[] = _componentDefinition.supportedBindingNames();
        if (supportedBindingNames != null) {
            Map otherBindings = bindingsHashtable;
            bindingsHashtable = new LinkedHashMap();
            int supportedBindingNamesCount = supportedBindingNames.length;

            // special case for otherBindings
            String currentBindingName = AWBindingNames.otherBindings;
            AWBinding supportedBinding = (AWBinding)otherBindings.remove(currentBindingName);
            if (supportedBinding != null) {
                bindingsHashtable.put(currentBindingName, supportedBinding);
            }

            // special case for namePrefix
            currentBindingName = AWBindingNames.semanticKeyBindingName();
            supportedBinding = (AWBinding)otherBindings.remove(currentBindingName);
            if (supportedBinding != null) {
                bindingsHashtable.put(currentBindingName, supportedBinding);
            }
            otherBindings.remove(AWBindingNames.namePrefix);
            otherBindings.remove(AWBindingNames.awname);

            currentBindingName = AWBindingNames._awname;
            supportedBinding = (AWBinding)otherBindings.remove(currentBindingName);
            if (supportedBinding != null) {
                bindingsHashtable.put(currentBindingName, supportedBinding);
            }
            otherBindings.remove(AWBindingNames._awname);

            for (int index = 0; index < supportedBindingNamesCount; index++) {
                currentBindingName = supportedBindingNames[index];
                supportedBinding = (AWBinding)otherBindings.remove(currentBindingName);
                if (supportedBinding != null) {
                    bindingsHashtable.put(currentBindingName, supportedBinding);
                }
            }
            if (!otherBindings.isEmpty()) {
                _otherBindings = AWBinding.bindingsDictionary(otherBindings);
            }
        }
        _isStateless = _componentDefinition.isStateless();
        _bindings = AWBinding.bindingsDictionary(bindingsHashtable);
        _useLocalPool = false;
        if (_isStateless && _componentDefinition.sharedComponentInstance().useLocalPool()) {
            _useLocalPool = true;
            _sharedComponentPool = AWRecyclePool.newPool(16, AWConcreteServerApplication.AllowsConcurrentRequestHandling,
                    ((AWConcreteApplication)AWConcreteApplication.SharedInstance).isStateValidationEnabled());
        }
        super.init(tagName, null);
    }

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        AWBinding binding = (AWBinding)bindingsHashtable.remove("^^^");
        if (binding != null) {
            enableCascadingBindingLookup();
        }
        return super.determineInstance(elementName, bindingsHashtable, templateName, lineNumber);
    }

    protected void enableCascadingBindingLookup ()
    {
        _allowCascadedBindings = true;
    }

    public AWComponentDefinition componentDefinition ()
    {
        return _componentDefinition;
    }

    public AWBinding bindingForName (String bindingName, AWComponent component)
    {
        AWBinding binding = _bindings.get(bindingName);
        if (binding == null) {
            if (_allowCascadedBindings) {
                // constructs a binding of the form foo="$^foo"
                String caratBinding = StringUtil.strcat("^", bindingName);
                binding = AWBinding.bindingWithNameAndKeyPath(bindingName, caratBinding);
                _bindings._put(bindingName, binding);
            }
            if (binding == null && _awbindingsDictionary != null && component != null) {
                AWBindingDictionary dynamicBindingDictionary = (AWBindingDictionary)_awbindingsDictionary.value(component);
                if (dynamicBindingDictionary != null) binding = dynamicBindingDictionary.get(bindingName);
            }
        }
        return binding;
    }

    public AWBinding bindingForName (String bindingName)
    {
        return bindingForName(bindingName, null);
    }

    public AWBindingDictionary bindings ()
    {
        // Note: this doesn't allow access to the bindings which are passed in as awbindingsDictionary
        return _bindings;
    }

    public AWBindingDictionary otherBindings ()
    {
        return _otherBindings;
    }

    public AWBinding[] allBindings ()
    {
        AWBinding[] superBindings = super.allBindings();
        List bindingVector = ListUtil.list();
        if (_bindings != null) {
            for (int index = _bindings.size() - 1; index >= 0; index--) {
                AWBinding currentBinding = _bindings.elementAt(index);
                AWUtil.addElement(bindingVector, currentBinding);
            }
        }
        if (_otherBindings != null) {
            for (int index = _otherBindings.size() - 1; index >= 0; index--) {
                AWBinding currentBinding = _otherBindings.elementAt(index);
                AWUtil.addElement(bindingVector, currentBinding);
            }
        }
        AWBinding[] myBindings = new AWBinding[bindingVector.size()];
        bindingVector.toArray(myBindings);
        AWBinding[] allBindings = (AWBinding[])AWUtil.concatenateArrays(superBindings, myBindings, AWBinding.class);
        return allBindings;
    }

    public Object userData ()
    {
        return _userData;
    }

    public void setUserData (Object userData)
    {
        _userData = userData;
    }

    ///////////////////
    // "xml" support
    ///////////////////
    protected AWXmlNode xmlNode ()
    {
        if (_rootXmlNode == null) {
            _rootXmlNode = new AWConcreteXmlNode();
            _rootXmlNode.init("rootXmlNode", null);
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWElement[] elementArray = ((AWTemplate)contentElement).elementArray();
                int elementCount = elementArray.length;
                for (int index = 0; index < elementCount; index++) {
                    AWElement currentElement = elementArray[index];
                    if (currentElement instanceof AWXmlNode) {
                        _rootXmlNode.add(currentElement);
                    }
                }
            }
            else {
                _rootXmlNode.add(contentElement);
            }
        }
        return _rootXmlNode;
    }

    public boolean isKindOfClass (Class targetClass)
    {
        return targetClass.isAssignableFrom(_componentDefinition.componentClass());
    }

    public boolean isStateless ()
    {
        return _isStateless;
    }

    private AWComponent lookupStatefulComponent (AWElementIdPath elementIdPath,
                                                 AWComponent parentComponent,
                                                 boolean isRenderPhase)
    {
        AWPage page = parentComponent.page();
        AWComponent subcomponentInstance = parentComponent.requestContext().getStatefulComponent(elementIdPath);

        if (subcomponentInstance != null) {
                // we have an existing instance for this class
            if (AWConcreteApplication.IsRapidTurnaroundEnabled && isRenderPhase) {
                subcomponentInstance = AWComponentReference.refreshedComponent(
                    _componentDefinition, parentComponent,
                    subcomponentInstance, elementIdPath);
            }
            subcomponentInstance.setupForNextCycle(this, parentComponent, page);
            subcomponentInstance.ensureAwake(page);
        }
        return subcomponentInstance;
    }

    protected AWComponent statefulSubcomponentInstanceInComponent (AWRequestContext requestContext,
                                                                 AWComponent parentComponent)
    {
        AWElementIdPath statefulSubcomponentId = statefulSubcomponentId(requestContext);
        AWPage page = parentComponent.page();
        AWComponent subcomponentInstance = lookupStatefulComponent(statefulSubcomponentId, parentComponent, true);

        if (subcomponentInstance == null) {
            subcomponentInstance = _componentDefinition.createComponent(this, parentComponent, requestContext);
            // Always allow stateful components to takeBindings, if they like.
            subcomponentInstance.takeBindings(_bindings);
            subcomponentInstance.saveInPage(statefulSubcomponentId);
            subcomponentInstance.ensureAwake(page);
        }
        return subcomponentInstance;
    }

    protected AWComponent rendezvousWithStatefulComponent (AWRequestContext requestContext, AWComponent parentComponent)
    {
        AWElementIdPath elementIdPath = statefulSubcomponentId(requestContext);
        AWComponent subcomponentInstance = lookupStatefulComponent(elementIdPath, parentComponent, false);
        if (subcomponentInstance == null && !requestContext.pageRequiresPreGlidCompatibility()) {
            AWComponentDefinition componentDefinition = componentDefinition();
            String componentNamePath = componentDefinition == null ?
                    "no component definition" : componentDefinition.componentNamePath();
            if(AWConcreteApplication.IsDebuggingEnabled){
                Assert.that(requestContext.allowFailedComponentRendezvous(),
                        "Cannot rendezvous with stateful subcomponent: %s\nelement id trace %s\ncomponentNamePath: %s\nstateful components: %s",
                        AWElementIdPath.debugElementIdPath(elementIdPath),
                        requestContext.currentElementIdTrace(), componentNamePath,
                        parentComponent.page()._debugSubcomponentString(componentNamePath));
            }

            String msg = ariba.util.i18n.LocalizedJavaString.getLocalizedString(
                AWComponentReference.class.getName(), 1,
                "An error has occurred while processing your request. Refresh the page and try again.",
                parentComponent.preferredLocale());

            parentComponent.recordValidationError(AWErrorManager.GeneralErrorKey, msg, "");
        }
        return subcomponentInstance;
    }

    /**
        Only called in debug mode
    */
    protected static AWComponent refreshedComponent (AWComponentDefinition componentDefinition,
            AWComponent parentComponent, AWComponent subcomponentInstance, AWElementIdPath elementIdPath)
    {
            // dynamic class reloading support...
            // This call could potentially do a recompile of the class.
        Class upToDateClass = componentDefinition.componentClass();
        Class myClass = subcomponentInstance.getClass();

        if (!(myClass == upToDateClass)) {
                // warn stale usage of class
            AWComponent replacement = subcomponentInstance.replacementInstance(parentComponent);
            if (replacement == null) {
                // we can't replace this page-level component, so we're bail out and
                // return to the front door
                Log.aribaweb.debug(
                        "Dynamic swapping of page-component class %s requires new object instance.  Redirecting to home page...",
                        myClass.getName());
                throw new AWSessionValidationException();
                /*
                String warningString = Fmt.S("Warning: Stateful component %s is using "+
                                             "a stale class.  Your changes are not being picked up yet.",
                                             componentDefinition.componentName());
                componentDefinition.logReloadString(warningString);
                return subcomponentInstance;
                */
            } else {
                    // force creation of a new component
                subcomponentInstance = replacement;
                subcomponentInstance.requestContext().page()._clearSubcomponentsWithParentPath(elementIdPath, true);
                if (parentComponent != null) {
                    subcomponentInstance.saveInPage(elementIdPath);
                }
                String warningString = Fmt.S("Note: An instance of stateful component %s "+
                                             "has been replaced with a new instance based on your class changes.",
                                             componentDefinition.componentName());
                componentDefinition.logReloadString(warningString);
                return replacement;
            }
        }
        return subcomponentInstance;
    }

    protected AWElementIdPath statefulSubcomponentId (AWRequestContext requestContext)
    {
        // requestContext.pushElementIdLevel();
        // return requestContext.currentElementIdPath();
        return requestContext.nextElementIdPath();
    }

    public AWComponent _statelessSubcomponentInstanceInComponent (AWRequestContext requestContext, AWComponent parentComponent)
    {
        AWComponent sharedComponentInstance = null;
        if (_useLocalPool) {
            _componentDefinition.componentClass();  // for debug -- force AWReloading if necessary
            sharedComponentInstance = (AWComponent)_sharedComponentPool.checkout();
        }
        else {
            sharedComponentInstance = _componentDefinition.sharedComponentInstance();
        }
        if (sharedComponentInstance == null) {
            sharedComponentInstance = _componentDefinition.newComponentInstance();
            if (_useLocalPool) {
                // Only allow stateless components to takeBindings if they useLocalPool.

                // need to force this before take bindings (takeBindingsViaReflection
                // can get at the reflection fields on the ComponentDefinition)
                sharedComponentInstance._componentReference = this;

                sharedComponentInstance.takeBindings(_bindings);
            }
        }
        AWPage page = null;
        if (requestContext != null) {
            page = requestContext.page();
        }
        else {
            page = parentComponent.page();
        }
        sharedComponentInstance.setupForNextCycle(this, parentComponent, page);
        sharedComponentInstance.ensureAwake(page);
        return sharedComponentInstance;
    }

    /*
        NOTE:  This code is copied VERBATIM to AWXComponentReference.
        THE TWO MUST BE KEPT IN SYNC!
     */
    public void _checkInSharedComponentInstance (AWComponent subcomponentInstance)
    {
        subcomponentInstance.flushState();
        subcomponentInstance.ensureAsleep();
        if (_useLocalPool) {
            // We need the _componentReference to restore the binding variables in AWComponent
            // during ensureFieldValuesClear.  Its okay to leave this set for useLocalPool()
            // components since the _componentRef doesn't change.
            subcomponentInstance.setupForNextCycle(this, null, null);
            _sharedComponentPool.checkin(subcomponentInstance);
        }
        else {
            subcomponentInstance.setupForNextCycle(null, null, null);
            if (AWConcreteServerApplication.AllowsConcurrentRequestHandling) {
                synchronized (_componentDefinition) {
                    _componentDefinition.checkInSharedComponentInstance(subcomponentInstance);
                }
            }
            else {
                _componentDefinition.checkInSharedComponentInstance(subcomponentInstance);
            }
        }
    }

    /*
        NOTE:  This code is copied VERBATIM to AWXComponentReference.
        THE TWO MUST BE KEPT IN SYNC!
     */
    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        if (LogComponentEvaluation) {
            logPhase(requestContext, "applyValues", true);
        }

        AWComponent subcomponentInstance = _isStateless ?
                _statelessSubcomponentInstanceInComponent(requestContext, component) :
                rendezvousWithStatefulComponent(requestContext, component);

        // if the subcomponent instance is null, then we must be in pre-glid compatibility
        // mode and the page structure has changed between phases.  In this case, just
        // skip the rest of this subtree
        if (subcomponentInstance == null)
            return;

        requestContext.pushCurrentComponent(subcomponentInstance);
        try {
            subcomponentInstance.applyValues(requestContext, component);

        }
        catch (AWGenericException ag) {
            throwException(ag);
        }
        catch (Throwable re) {
            throwException(re);
        }
                requestContext.popCurrentComponent(component);
        if (_isStateless) {
            _checkInSharedComponentInstance(subcomponentInstance);
        } else {
            // requestContext.popElementIdLevel();
        }

        if (LogComponentEvaluation) {
            logPhase(requestContext, "applyValues", false);
        }
    }

    /*
        NOTE:  This code is copied VERBATIM to AWXComponentReference.
        THE TWO MUST BE KEPT IN SYNC!
     */
    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        if (LogComponentEvaluation) {
            logPhase(requestContext, "invokeAction", true);
        }

        AWResponseGenerating actionResults = null;
        AWComponent subcomponentInstance = _isStateless ?
                _statelessSubcomponentInstanceInComponent(requestContext, component) :
                rendezvousWithStatefulComponent(requestContext, component);

        // if the subcomponent instance is null, then we must be in pre-glid compatibility
        // mode and the page structure has changed between phases.  In this case, just
        // skip the rest of this subtree
        if (subcomponentInstance == null)
            return null;

        requestContext.pushCurrentComponent(subcomponentInstance);
        AWEncodedString elementId = AWConcreteApplication.IsDebuggingEnabled
            && requestContext.isPathDebugRequest() ?
            requestContext.currentElementId() : null;
        try {
            actionResults = subcomponentInstance.invokeAction(requestContext, component);
        }
        catch (AWGenericException ag) {
            throwException(ag);
        }
        catch (Throwable re) {
            throwException(re);
        }

        requestContext.popCurrentComponent(component);
        if (_isStateless) {
            _checkInSharedComponentInstance(subcomponentInstance);
        } else {
            // requestContext.popElementIdLevel();
        }

        if (LogComponentEvaluation) {
            logPhase(requestContext, "invokeAction", false);
        }
        if (actionResults != null && requestContext.isPathDebugRequest()) {
            requestContext.debugTrace().pushComponentPathEntry(this, elementId);
        }

        return actionResults;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (LogComponentEvaluation) {
            logPhase(requestContext, "renderResponse", true);
        }

        AWComponent subcomponentInstance = _isStateless ?
                _statelessSubcomponentInstanceInComponent(requestContext, component) :
                statefulSubcomponentInstanceInComponent(requestContext, component);
        requestContext.pushCurrentComponent(subcomponentInstance);

        // Disable old-style component inspector support in favor of new alt-click debug trace mechanism
        /*
        if (requestContext.componentPathDebuggingEnabled() && subcomponentInstance.allowComponentPathDebugging()) {
            AWResponse response = requestContext.response();
            response.appendContent(OpenShowDebugTag);
            response.appendContent(templateName());
            response.appendContent(AWConstants.Colon);
            response.appendContent(AWUtil.toString(lineNumber()));
            response.appendContent(AWConstants.Quote);
            response.appendContent(AWConstants.RightAngle);
            subcomponentInstance.renderResponse(requestContext, component);
            response.appendContent(CloseShowDebugTag);
        } else {
            subcomponentInstance.renderResponse(requestContext, component);
        }
        */

        try {
            subcomponentInstance.renderResponse(requestContext, component);
        }
        catch (AWGenericException ag) {
            throwException(ag);
        }
        catch (Throwable re) {
            throwException(re);
        }

        requestContext.popCurrentComponent(component);
        if (_isStateless) {
            _checkInSharedComponentInstance(subcomponentInstance);
        } else {
            // requestContext.popElementIdLevel();
        }

        if (LogComponentEvaluation) {
            logPhase(requestContext, "renderResponse", false);
        }
        }

    private void throwException (Throwable re)
    {
        throwException(new AWGenericException(re));
    }

    private void throwException (AWGenericException ag)
    {
        ag.addReferenceElement(this);
        throw ag;
    }

    public void validate (AWValidationContext validationContext, AWComponent component)
    {
        // set the current element to this.  Effectively associating this element
        // with the current container's component.
        AWElement origElement = component.currentTemplateElement();
        component.setCurrentTemplateElement(this);

        // load the template of the referenced component -- sets the componentAPI (if
        // it exists, into the componentDefinition.
        AWComponent currentComponent = componentDefinition().sharedComponentInstance();
        currentComponent.setupForNextCycle(this, component, component.page());
        currentComponent.componentApi();

        componentDefinition().addReferencedBy(component);

        // validate bindings -- left side and right side validation.

        // required bindings validation
        validateRequiredBindings(validationContext, component);

        // make sure tags match suported bindings
        validateSupportedBindings(validationContext, component);

        // binding validation (right side).
        validateBindings(validationContext, component);

        validateNamedTemplates(component);

        super.validate(validationContext, component);
        component.setCurrentTemplateElement(origElement);
    }

    private void validateNamedTemplates (AWComponent component)
    {
        AWApi componentApi = _componentDefinition.componentApi();
        if (componentApi != null) {
            // Handle case where we're missing a required named template
            AWContentApi[] contentApi = componentApi.contentApis();
            for (int index = 0, length = contentApi.length; index < length; index++) {
                AWContentApi acontentApi = contentApi[index];
                if (acontentApi._required) {
                    String templateName = acontentApi._name;
                    if (!hasNamedTemplate(component, templateName)) {
                        _componentDefinition.addMissingRequiredNamedTemplate(component, templateName);
                    }
                }
            }
            // Handle opposite case where we have a named template, but its not declared.
            AWElement contentElement = contentElement();
            if (contentElement instanceof AWTemplate) {
                AWTemplate template = (AWTemplate)contentElement;
                AWContent[] contents =
                        (AWContent[])template.extractElementsOfClass(AWContent.class);
                for (int index = 0, length = contents.length; index < length; index++) {
                    AWContent content = contents[index];
                    validateNamedTemplateIsDeclared(content, contentApi, component);
                }
            }
            else if (contentElement instanceof AWContent) {
                validateNamedTemplateIsDeclared((AWContent)contentElement, contentApi, component);
            }
        }
    }

    private void validateNamedTemplateIsDeclared (AWContent content,
                                                  AWContentApi[] contentApi, AWComponent component)
    {
        String name = content.nameInComponent(component);
        // todo: this is disabled until we can make Buyer use standard api for
        // todo: "toc" and others and not use "leftPanel", for example.
        if (false && !hasNamedTemplateDeclaration(contentApi, name)) {
            _componentDefinition.addMissingNamedTemplateDeclaration(component, name);
        }
    }

    private boolean hasNamedTemplateDeclaration (AWContentApi[] contentApi, String targetName)
    {
        for (int index = 0, length = contentApi.length; index < length; index++) {
            AWContentApi acontentApi = contentApi[index];
            if (acontentApi._name.equals(targetName)) {
                return true;
            }
        }
        return false;
    }

    // left side validation
    private void validateRequiredBindings (AWValidationContext validationContext, AWComponent component)
    {
        AWApi componentApi = _componentDefinition.componentApi();
        if (componentApi != null) {
            componentApi.validateRequiredBindings(validationContext, component, _bindings);
        }
    }

    // left side validation
    private void validateSupportedBindings (AWValidationContext validationContext, AWComponent component)
    {
        AWApi componentApi = _componentDefinition.componentApi();
        if (componentApi != null) {
            // we have a component Api so check _bindings

            // componentDefinition is the componentDefinition of the component in whose template this
            // component reference occurs
            AWComponentDefinition componentDefinition = component.componentDefinition();
            for (int index = _bindings.size() - 1; index > -1; index--) {
                String bindingName = _bindings.keyAt(index);
                AWBindingApi bindingApi = componentApi.getBindingApi(bindingName);
                if (bindingApi != null) {
                    // track found bindings
                    _componentDefinition.addBindingReference(component, bindingName);
                }
                else {
                    // binding not defined in AWApi
                    // check if supported bindings defined
                    if (componentApi.allowsPassThrough()) {
                        // track bindings that will be passed through
                        componentDefinition.addPassThroughBinding(validationContext, component, bindingName);
                    }
                    else {
                        componentDefinition.addUnsupportedBinding(validationContext, component, bindingName);
                    }
                }
            }

            if (_otherBindings != null) {
                for (int index = _otherBindings.size() - 1; index > -1; index --) {
                    componentDefinition.addPassThroughBinding(validationContext, component, _otherBindings.keyAt(index));
                }
            }
        }
        else {
            // no AWApi so just collect all the bindings for reporting
            for (int index = _bindings.size() - 1; index > -1; index--) {
                _componentDefinition.addEmpiricalBinding(_bindings.keyAt(index), component);
            }
        }
    }

    // right side validation
    private void validateBindings (AWValidationContext validationContext, AWComponent component)
    {
        // validate supported bindings
        for (int index = _bindings.size() - 1; index > -1; index--) {
            AWBinding binding = _bindings.elementAt(index);
            binding.validate(validationContext, component, _componentDefinition);
        }

        // now validate other bindings
        if (_otherBindings != null) {
            for (int index = _otherBindings.size() - 1; index > -1; index --) {
                AWBinding binding = _otherBindings.elementAt(index);
                binding.validate(validationContext, component, _componentDefinition);
            }
        }
    }

    public String toString ()
    {
        return StringUtil.strcat(super.toString(), ":", _componentDefinition.toString());
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

    // Uncomment this at the code at buttom of method below to do unbalanced elementId checking
    // private AWElementIdPath _debug_elementIdPath;

    public void logPhase (AWRequestContext requestContext, String phase, boolean entering)
    {
        if (requestContext.session(false) == null) return;

        boolean logRequestBanner = requestContext.get("hasLoggedBanner") == null;
        if (logRequestBanner) {
            requestContext.put("hasLoggedBanner", Boolean.TRUE);
            Integer count = (Integer)requestContext.httpSession().getAttribute("RequestCount");
            if (count == null) {
                count = Constants.getInteger(1);
            } else {
                count = Constants.getInteger(count.intValue() + 1);
            }
            requestContext.httpSession().setAttribute("RequestCount", count);

            logString("######## Request " + count + " ##############################");
        }

        Integer indent = (Integer)requestContext.get(phase);

        if (indent == null) {
            indent = Constants.getInteger(0);
            logString(phase);
        }

        if (entering) {
            indent = Constants.getInteger(1 + indent.intValue());
        }

        for (int i = indent.intValue(); i > 0; i--) {
            logString("  ");
        }

        if (!entering) {
            indent = Constants.getInteger(-1 + indent.intValue());
        }

        requestContext.put(phase, indent);

        logString(entering ? "> " : "< ");
        String componentDesc = _componentDefinition.componentName();
        int lastDot = componentDesc.lastIndexOf(".");
        if (lastDot != -1) {
            componentDesc = componentDesc.substring(lastDot + 1);
        }
        logString(componentDesc);

        /*
        if (entering) {
            _debug_elementIdPath = requestContext.nextElementIdPath();
        } else {
            AWElementIdPath afterPath = requestContext.nextElementIdPath();
            if (!_isStateless && _debug_elementIdPath.privatePath().length != afterPath.privatePath().length) {
                logString("MISMATCHED ELEMENT ID LENGTHS ON ENTRY/EXIT");
            }
        }
        */
    }

    protected boolean hasNamedTemplate (AWComponent component, String templateName)
    {
        boolean hasNamedTemplate = false;
        AWElement contentElement = contentElement();
        if (contentElement instanceof AWConcreteTemplate) {
            hasNamedTemplate =
                ((AWConcreteTemplate)contentElement).hasNamedTemplate(component,
                                                                      templateName);
        }
        else if (contentElement instanceof AWContent) {
            hasNamedTemplate =
                ((AWContent)contentElement).isNamedTemplate(component,
                                                                  templateName);
        }
        return hasNamedTemplate;
    }

    protected boolean isBoundReference ()
    {
        return false;
    }

    // used by AWInstanceInclude
    public AWComponentReference createBoundReference (AWComponent instance)
    {
        return new BoundReference(instance);
    }

    protected static class BoundReference extends AWComponentReference
    {
        protected AWComponent _instance;

        public BoundReference (AWComponent instance)
        {
            super(instance.componentDefinition());
            _instance = instance;
            init(componentDefinition().componentName(), EmptyHashtable);
            _isStateless = false;
        }

        protected AWComponent statefulSubcomponentInstanceInComponent (AWRequestContext requestContext,
                                                                       AWComponent parentComponent)
        {
            // force ID push to balance with susequent pop by parent class
            statefulSubcomponentId(requestContext);

            AWPage page = parentComponent.page();
            _instance.setupForNextCycle(this, parentComponent, page);
            _instance.ensureAwake(page);

            return _instance;
        }

        protected AWComponent rendezvousWithStatefulComponent (AWRequestContext requestContext, AWComponent parentComponent)
        {
            // force ID push to balance with susequent pop by parent class
            statefulSubcomponentId(requestContext);

            return _instance;
        }

        protected boolean isBoundReference ()
        {
            return true;
        }
    }

}
