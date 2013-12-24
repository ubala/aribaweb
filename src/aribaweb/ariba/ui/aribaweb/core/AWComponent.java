/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponent.java#129 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.GrowOnlyHashSet;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWFastStringBuffer;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWStringDictionary;
import ariba.ui.aribaweb.util.AWStringsThunk;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWParameters;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.aribaweb.html.AWPrivateScript;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import java.util.ArrayList;
import java.util.Map;
import ariba.util.i18n.I18NUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpSession;

import static ariba.ui.aribaweb.core.AWComponent.RenderingListener.InterestLevel;

/**
    AWComponent is the key class for template/based interactive content.  All "pages",
    page sub-sections, "wrappers", as well as most controls / widgets, and even control
    contructs are implemented as subclasses of AWComponent.
     <p/>

    Components typically have a {@link AWTemplate template} (.awl file) containing "bare
    html text" mixed with {@link AWComponentReference references} to other embedded
    AWComponents (or low level {@link ariba.ui.aribaweb.core.AWBindableElement elements}).
    These, in turn, have {@link AWBinding bindings} that refer back to the java instance
    paired with the template (often using {@link ariba.util.fieldvalue.FieldPath fieldpaths}
    to dynamically push/pull values and invoke actions.
    <p/>
    Components may be either {@link #isStateless() stateless} (pooled) or stateful (bound to their
    page instance and stored in the session.  (Typically pages, page sub-sections, and
    particular rich components are stateful, while simple controls are stateless and
    simply push/pull their needed state from their parent component via bindings).
    <p/>
    Simple subclasses of AWComponent typically declare instance variables (possible public
    for use in bindings) as well as action methods (which return {@link AWResponseGenerating reponses}
    which are usually just other page-level AWComponent instances for the next page (or null
    to rerender the current page while reflecting any updated state).
    <p/>
    Components participate in the {@link AWCycleable} request handling lifecycle.  In addition to
    {@link #renderResponse(AWRequestContext, AWComponent)}, {@link #applyValues(AWRequestContext, AWComponent)},
    and {@link #invokeAction(AWRequestContext, AWComponent)}, an AWComponent also experiences
    {@link #init()}, {@link #awake()}, {@link #sleep()}, and even possibly {@link #hibernate()}.  
  @aribaapi private
 */
public class AWComponent extends AWBaseObject implements AWCycleable, AWCycleableReference, AWResponseGenerating,
        AWResponseGenerating.ResponseSubstitution
{                  
    public static final String ComponentTemplateFileExtension = ".awl";
    public static final Class ClassObject = AWComponent.class;
    private static AWTemplateParser DefaultTemplateParser = null;
    protected static final String BindingSuffix = "Binding";
    private static AWResourceManager _templateResourceManager = null;

    protected AWComponentReference _componentReference;

    private AWPage _page;
    private AWComponent _parent;
    private AWElement _currentTemplateElement;
    private AWTemplate _uniqueTemplate;
    private Map _extendedFields;
    private AWBinding _otherBindingsBinding;
    private boolean _isAwake = false;

    // to control wacky feature of allowing templates to specify their own encoding:
    private static boolean AllowTemplateEncoding = true;

    // ** Thread Safety Considerations: Stateful components are never shared
    // between threads.  Stateless components are often used by different
    // threads, however, they are only used by one thread at a time as managed
    // by the check-out/in scheme in AWComponentDefinition, so we can consider
    // all components to be single threaded.
    public AWElement determineInstance (String elementName,
                                        String translatedClassName,
                                        Map bindingsHashtable,
                                        String templateName,
                                        int lineNumber)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        // Must search for elementName first since componentName will always succeed (eg AWComponent)
        // so we don't ever try the elementName (which is used for classless components).
        AWComponentDefinition componentDefinition = application.componentDefinitionForName(translatedClassName);
        if (componentDefinition == null) {
            String componentName = ClassUtil.stripPackageFromClassName(getClass().getName());
            componentDefinition = application.componentDefinitionForName(componentName);
        }
        AWComponentReference componentReference = createComponentReference(componentDefinition);
        return componentReference.determineInstance(translatedClassName, bindingsHashtable, templateName, lineNumber);
    }

    protected AWComponentReference createComponentReference (AWComponentDefinition componentDefinition)
    {
        return AWComponentReference.create(componentDefinition);
    }

    public AWElement determineInstance (String elementName, Map bindingsHashtable, String templateName, int lineNumber)
    {
        Assert.assertNonFatal(true, "Do not call determineInstance(String, Map, String, int)");
        return determineInstance(elementName, elementName, bindingsHashtable, null, -1);
    }

    protected boolean useLocalPool ()
    {
        return false;
    }

    /**
     * Pseudo-private: used only for initing validation testing version of components
     * (i.e. for AWConcreteApplication.preinstantiateAllComponents()
     */
    protected void _setup (AWComponentReference componentReference, AWPage page)
    {
        _componentReference = componentReference;
        setPage(page);
    }

    public void init (AWComponentReference componentReference, AWComponent parentComponent, AWPage page)
    {
        _componentReference = componentReference;
        _parent = parentComponent;
        setPage(page);
        this.init();
    }

    public void init ()
    {
        super.init();

        // If we are truly stateless (isStateless == true and we aren't a page level
        // component) make sure the component doesn't override init.  It doesn't make
        // sense for stateless components to override init because init will only be
        // called once (technically, once per shared instance).
        if (isStatelessSubComponent()) {
            try {
                Class cls = getClass();
                while (cls != null && cls != AWComponent.ClassObject) {
                    Method initMethod = cls.getDeclaredMethod("init");
                    Assert.that(initMethod == null, "Stateless component %s cannot override init().", getClass().getName());
                    cls = cls.getSuperclass();
                }
            }
            catch (NoSuchMethodException e) {
                // Will never happen
                e.printStackTrace();
            }
        }
    }

    protected void setParent (AWComponent parent)
    {
        _parent = parent;
    }

    public AWComponent parent ()
    {
        return _parent;
    }

    protected void setPage (AWPage page)
    {
        _page = page;
    }

    private boolean isStatelessSubComponent ()
    {
        return isStateless() && parent() != null;
    }

    protected void setCurrentTemplateElement (AWElement element)
    {
        _currentTemplateElement = element;
    }

    protected AWElement currentTemplateElement ()
    {
        return _currentTemplateElement;
    }

    /**
        This method allows for access to the current component fromthe .awl via "$this"
    */
    public AWComponent getThis ()
    {
        return this;
    }

    public void setupForNextCycle (AWComponentReference componentReference, AWComponent parentComponent, AWPage page)
    {
        // This allows a component to be stateless -- these three things change each time a component is reused.
        _parent = parentComponent;
        _componentReference = componentReference;
        setPage(page);
    }

    public AWComponentDefinition componentDefinition ()
    {
        return _componentReference.componentDefinition();
    }

    public boolean hasMultipleTemplates ()
    {
        return false;
    }

    public static void setDefaultTemplateParser (AWTemplateParser templateParser)
    {
        DefaultTemplateParser = templateParser;
    }

    public static AWTemplateParser defaultTemplateParser ()
    {
        if (DefaultTemplateParser == null) {
            DefaultTemplateParser = new AWHtmlTemplateParser();
            DefaultTemplateParser.init((AWApplication)AWConcreteApplication.sharedInstance());

            // the AWApi doc tags...
            DefaultTemplateParser.registerContainerClassForTagName("Binding", AWBindingApi.class);
            DefaultTemplateParser.registerContainerClassForTagName("NamedContent", AWContentApi.class);
            DefaultTemplateParser.registerContainerClassForTagName("Copyright", AWHtmlTemplateParser.LiteralContainer.class);
            DefaultTemplateParser.registerContainerClassForTagName("Responsible", AWHtmlTemplateParser.LiteralContainer.class);
            DefaultTemplateParser.registerContainerClassForTagName("Todo", AWHtmlTemplateParser.LiteralContainer.class);
            DefaultTemplateParser.registerContainerClassForTagName("Overview", AWHtmlTemplateParser.LiteralContainer.class);
            DefaultTemplateParser.registerContainerClassForTagName("Example", AWExampleApi.class);
            DefaultTemplateParser.registerContainerClassForTagName("IncludeExample", AWIncludeExample.class);

            DefaultTemplateParser.registerContainerClassForTagName("script", AWPrivateScript.class);
            DefaultTemplateParser.registerContainerClassForTagName("Script", AWPrivateScript.class);
            DefaultTemplateParser.registerContainerClassForTagName("SCRIPT", AWPrivateScript.class);
        }
        return DefaultTemplateParser;
    }

    protected static void initializeAllowedGlobalTags (AWNamespaceManager.AllowedGlobalsResolver resolver)
    {
        resolver.addAllowedGlobal("Binding");
        resolver.addAllowedGlobal("NamedContent");
        resolver.addAllowedGlobal("Copyright");
        resolver.addAllowedGlobal("Responsible");
        resolver.addAllowedGlobal("Todo");
        resolver.addAllowedGlobal("Overview");
        resolver.addAllowedGlobal("Example");
        resolver.addAllowedGlobal("IncludeExample");
        resolver.addAllowedGlobal("Script");
    }
    
    public AWTemplateParser templateParser ()
    {
        return _page.templateParser();
    }

    public void setTemplateParser (AWTemplateParser templateParser)
    {
        _page.setTemplateParser(templateParser);
    }

    public AWComponentReference componentReference ()
    {
        return _componentReference;
    }

    public static AWComponent createPageWithName (String pageName)
    {
        AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.sharedInstance();
        AWRequestContext requestContext = application.createRequestContext(null);
        return application.createPageWithName(pageName, requestContext);
    }

    protected void takeBindings (AWBindingDictionary bindings)
    {
        if (isStateless()) {
            takeBindingsViaReflection(bindings);
        }
        _otherBindingsBinding = bindings.get(AWBindingNames.otherBindings);
    }

    private void takeBindingsViaReflection (AWBindingDictionary bindings)
    {
        Map fields = componentDefinition().bindingFields();
        try {
            int i = bindings.size();
            while (i-- > 0) {
                String key = bindings.keyAt(i);
                Field field = (Field)fields.get(key);
                if (field != null) {
                    AWBinding binding = bindings.elementAt(i);
                    if (binding != null) {
                        // Run this check only in debug mode
                        if (((AWConcreteApplication)AWConcreteApplication.SharedInstance).isStateValidationEnabled()) {
                            Object existingValue = field.get(this);
                            Assert.that(existingValue == null || existingValue == AWBinding.DummyBinding,
                                        "AWBinding fields must not be initialized: %s %s contains %s",
                                        getClass(), field, existingValue);
                        }
                        field.set(this, binding);
                    }
                }
            }
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
    }

    private void clearBindingIvars ()
    {
        Map fields = componentDefinition().bindingFields();
        try {
            Iterator iter = fields.values().iterator();
            while (iter.hasNext()) {
                Field field = (Field)iter.next();
                field.set(this, null);
            }
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
    }

    // Called when checking a stateless component back into its pool
    protected boolean isFieldRequiredClear (Field field) {
        // we don't require AWBindings to be cleared for locally pooled components (because they're pooled on the
        // AWComponentReference where the bindings stay constant across uses
        return super.isFieldRequiredClear(field) &&
                 (!useLocalPool() || !AWBinding.class.isAssignableFrom(field.getType()))
                && !(field.getName().equals("_uniqueTemplate") || field.getName().equals("_extendedFields"));
    }

    public void ensureFieldValuesClear ()
    {
        // Stop the check at AWComponent -- i.e. just check ivars introduced in the
        // subclasses
        ensureFieldValuesClear(getClass(), AWComponent.class);
    }

    protected Field lookupBindingField (String fieldName, Class classObject)
    {
        Field field = null;
        try {
            field = classObject.getDeclaredField(fieldName);
            if (field == null) {
                field = recurseForLookupBindingField(fieldName, classObject);
            }
        }
        catch (NoSuchFieldException noSuchFieldException) {
            field = recurseForLookupBindingField(fieldName, classObject);
        }
        catch (SecurityException securityException) {
            securityException = null;
        }
        return field;
    }

    private Field recurseForLookupBindingField (String fieldName, Class classObject)
    {
        Class superclassObject = classObject.getSuperclass();
        return (superclassObject == null || superclassObject == AWComponent.ClassObject.getSuperclass()) ?
            null :
            lookupBindingField(fieldName, superclassObject);
    }

    protected Field lookupBindingField (String key)
    {
        String fieldName = StringUtil.strcat("_", key, BindingSuffix);
        return lookupBindingField(fieldName, getClass());
    }

    /////////////////////////////
    // Request Context Accessors
    /////////////////////////////
    public AWRequestContext requestContext ()
    {
        return _page.requestContext();
    }

    public AWApplication application ()
    {
        return _page.requestContext().application();
    }

    public AWPage page ()
    {
        return _page;
    }

    public AWComponent pageComponent ()
    {
        return _page.pageComponent();
    }

    /**
     * Note: Should be overwritten in sub-classing page-level components.
     * @return The driving business logic.
     */
    public Object getDrivingBusinessObject ()
    {
        return null;
    }

    /**
     * Note: Can be overwritten by sub-class if page is always rendered in a
     * certain version.
     * @return The rendering version of this page.
     */
    public UIRenderMeta.RenderVersion getPageRenderVersion ()
    {
        AWComponent renderingComponent = pageComponent();

        // if we can't find the renderingComponent, give up
        if (renderingComponent == null) {
            return UIRenderMeta.RenderVersion.AW5;
        }

        // if this is the renderingComponent, then proceed
        if (this.equals(renderingComponent)) {
            // only in debug mode?
            UIRenderMeta.RenderVersion renderVersion = requestContext()
                    .getQueryRenderVersion();

            if (renderVersion != null) {
                return renderVersion;
            }

            Object businessObject = renderingComponent
                    .getDrivingBusinessObject();

            if (businessObject != null) {
                UIRenderMeta uiRenderMeta = UIRenderMeta.get(businessObject);

                if (uiRenderMeta != null) {
                    return uiRenderMeta.getRenderVersion(businessObject);
                }
            }

            return UIRenderMeta.RenderVersion.AW5;
        }
        // delegate work to the renderingComponent
        else {
            return renderingComponent.getPageRenderVersion();
        }
    }

    public boolean isRenderAW5 ()
    {
        return UIRenderMeta.RenderVersion.AW5.equals(getPageRenderVersion());
    }

    public AWSession session ()
    {
        return _page.session(true);
    }

    public AWSession session (boolean required)
    {
        return _page.session(required);
    }

    public HttpSession httpSession ()
    {
        return _page.httpSession();
    }

    public AWRequest request ()
    {
        return _page.requestContext().request();
    }

    public AWResponse response ()
    {
        return _page.requestContext().response();
    }

    public boolean isBrowserMicrosoft ()
    {
        return _page.isBrowserMicrosoft;
    }

    public boolean isMacintosh ()
    {
        return _page.isMacintosh;
    }

    public String browserMinWidth ()
    {
        return _page.requestContext().browserMinWidth();
    }

    public String browserMaxWidth ()
    {
        return _page.requestContext().browserMaxWidth();
    }

    public AWResourceManager resourceManager ()
    {
        return _page.resourceManager();
    }

    public void setResourceManager (AWResourceManager resourceManager)
    {
        _page.setResourceManager(resourceManager);
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        _page.setCharacterEncoding(characterEncoding);
    }

    public AWCharacterEncoding characterEncoding ()
    {
        return _page.characterEncoding();
    }

    public void setClientTimeZone (TimeZone timeZone)
    {
        _page.setClientTimeZone(timeZone);
    }

    public TimeZone clientTimeZone ()
    {
        return _page.clientTimeZone();
    }

    public void setEnv (AWEnvironmentStack environmentStack)
    {
        _page.setEnv(environmentStack);
    }

    public AWEnvironmentStack env ()
    {
        return _page.env();
    }

    public void setPreferredLocale (Locale locale)
    {
        _page.setPreferredLocale(locale);
    }

    public Locale preferredLocale ()
    {
        return _page.preferredLocale();
    }

    public String name ()
    {
        return componentDefinition().componentName();
    }

    public String namePath ()
    {
        return componentDefinition().componentNamePath();
    }

    public String templateName ()
    {
        return componentDefinition().templateName();
    }

    /**
     * Get the name of the class where we get our resources from.
     * This is the class in which we resolve AWLocal references.
     * @return the full class name of the resource class
     * @aribaapi ariba
     */
    public String resourceClassName ()
    {
        if (!hasMultipleTemplates()) {
            // in the normal case, we use the one-time calculated resourceClassName
            return componentDefinition().resourceClassName(this);
        }
        else {
            // in the multiple personalities case, get the template name and derive
            // a resource class from that.
            String templateName = templateName();
            return AWComponentDefinition.computeClassNameFromTemplate(templateName);
        }
    }

    public AWTemplate template ()
    {
        AWTemplate template = _uniqueTemplate;
        if (template == null) {
            template = loadTemplate();
            if (!hasMultipleTemplates() && !AWConcreteApplication.IsRapidTurnaroundEnabled) {
                _uniqueTemplate = template;
            }
        }
        return template;
    }

    public boolean isClientPanel ()
    {
        return dict("clientPanel") != null;
    }

    public void setClientPanel (boolean yn)
    {
        if (yn) {
            dict("clientPanel", Boolean.TRUE);
        } else {
            dict().remove("clientPanel");
        }
    }

    // AWResponseGenerating.ResponseSubstitution
    // See if we're a modal panel...
    public AWResponseGenerating replacementResponse () {
        AWPage requestPage;
        if (requestContext() != null  && (requestPage = requestContext().requestPage()) != null
                && isClientPanel()) {
            requestPage.addModalPanel(this);
            return requestPage.pageComponent();
        }
        return this;
    }

    protected AWApi componentApi ()
    {
        // make sure the template is loaded
        if (hasTemplate()) {
            template();
            return componentDefinition().componentApi();
        }
        return null;
    }

    ////////////////////
    // Validation
    ////////////////////

    protected void addValidationError (AWRequestContext requestContext)
    {
//        AWValidationContext validationContext = requestContext.validationContext();
//        validationContext.addComponentWithError(componentDefinition());
    }

    protected void validate (AWValidationContext validationContext)
    {
        AWTemplate template = template();
        if (template != null && !template.validated()) {
            template.validate(validationContext,this);
        }
        if (componentDefinition().hasValidationErrors() && isValidationEnabled()) {
            validationContext.addComponentWithError(componentDefinition());
        }
    }

    protected String getCurrentPackageName ()
    {
        return componentDefinition().componentPackageName();
    }

    // Override this method to bypass the package level check
    public boolean isValidationEnabled ()
    {
        return isPackageLevelFlagEnabled(getCurrentPackageName(), AWConcreteApplication.TemplateValidationFlag);
    }

    // Override this method to bypass the package level check
    public boolean isStrictTagNaming ()
    {
        return isPackageLevelFlagEnabled(getCurrentPackageName(), AWConcreteApplication.StrictTagNamingFlag);
    }

    protected boolean isPackageLevelFlagEnabled (String packageName, int flag)
    {
        return ((AWConcreteApplication)AWConcreteApplication.sharedInstance()).isPackageLevelFlagEnabled(packageName, flag);
    }

    ////////////////////
    // Template parsing
    ////////////////////
    public boolean allowsWhitespaceCompression ()
    {
        return !AWConcreteApplication.IsDebuggingEnabled;
    }


    private void throwComponentNotFoundException (String templateName)
    {
        throw new AWGenericException("*** Error: " + getClass().getName() + ": unable to locate file named \"" + templateName + "\"");
    }

    protected AWResource safeTemplateResource ()
    {
        AWResourceManager resourceManager = templateResourceManager();
        String templateName = templateName();
        AWResource resource = resourceManager.resourceNamed(templateName);
        if (resource == null) {
            resource = parentTemplateResource();
        }
        return resource;
    }

    protected AWResource parentTemplateResource ()
    {
        // Recurse up the superclass chain to locate a template.  We used to require subclasses
        // to explicitly provide the templateName of the superclass, but now we iteratively
        // locate the template resource as follows:
        AWResource resource = null;
        AWResourceManager resourceManager = templateResourceManager();
        Class superclass = getClass().getSuperclass();
        while (resource == null && AWComponent.ClassObject.isAssignableFrom(superclass)) {
            String superclassName = superclass.getName();
            AWComponentDefinition componentDefinition = application().componentDefinitionForName(superclassName);
            String templateName = componentDefinition.templateName();
            resource = resourceManager.resourceNamed(templateName);
            superclass = superclass.getSuperclass();
        }
        return resource;
    }

    public AWResource templateResource ()
    {
        AWResource resource = safeTemplateResource();
        if (resource == null) {
            throwComponentNotFoundException(templateName());
        }
        return resource;
    }


    public static AWResourceManager templateResourceManager ()
    {
        return _templateResourceManager;
    }

    /**
     * We need to use the same resource manager for loading templates.
     * Otherwise, each component instances can potential get templates with
     * a different structure (ie, AWIncludeComponent maps)
     */
    public static void initTemplateResourceManager (AWResourceManager manager)
    {
        _templateResourceManager = manager;
    }

    protected boolean hasTemplate ()
    {
        return safeTemplateResource() != null;
    }

    protected boolean templateHasEncoding ()
    {
        return false;
    }

    public boolean hasContentNamed (String name)
    {
        AWElement contentElement = componentReference().contentElement();
        if (name == null) return (contentElement != null);
        
        if (contentElement instanceof AWTemplate) {
            AWTemplate template = (AWTemplate)contentElement;
            int index = template.indexOfNamedSubtemplate(name, this);
            if (index != -1) {
                AWContent content = template.subtemplateAt(index);
                return content.enabled(this);
            }
        }
        return false;
    }

    /**
     * @deprecated  Use hasContentNamed()
     */
    public boolean hasSubTemplateNamed (String templateName)
    {
        return hasContentNamed(templateName);
    }

    public boolean hasContentForTagName (String tagName)
    {
        AWElement contentElement = componentReference().contentElement();
        if (contentElement != null) {
            if (contentElement instanceof AWTemplate) {
                AWTemplate elementsTemplate = (AWTemplate)contentElement;
                AWElement[] elementArray = elementsTemplate.elementArray();
                int elementCount = elementArray.length;
                for (int index = 0; index < elementCount; index++) {
                    AWElement currentElement = elementArray[index];
                    if (currentElement != null &&
                        currentElement instanceof AWBindableElement) {
                        AWBindableElement bindingElement = (AWBindableElement)currentElement;
                        if (tagName.equals(bindingElement.tagName())) {
                            return true;
                        }
                    }
                }
            }
            else if (contentElement instanceof AWBindableElement) {
                AWBindableElement bindingElement = (AWBindableElement)contentElement;
                if (tagName.equals(bindingElement.tagName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean doesReferenceHaveNamedTemplate (String templateName)
    {
        AWComponentReference componentReference = componentReference();
        return componentReference == null ? false : componentReference.hasNamedTemplate(parent(), templateName);
    }

    protected static String readTemplateString (InputStream inputStream)
    {
        byte[] bytes = AWUtil.getBytes(inputStream);
        String templateString = AWCharacterEncoding.ISO8859_1.newString(bytes);
        if (AllowTemplateEncoding && templateString.startsWith("<!--")) {
            int indexOfClosingComment = templateString.indexOf("-->");
            if (indexOfClosingComment != -1) {
                String firstLine = templateString.substring(0, indexOfClosingComment);
                String encodingToken = "encoding:";
                int indexOfEncoding = AWUtil.indexOf(firstLine, encodingToken, 0, true);
                if (indexOfEncoding != -1) {
                    String encodingName = firstLine.substring(indexOfEncoding + encodingToken.length(), indexOfClosingComment);
                    encodingName = encodingName.trim();
                    try {
                        templateString = new String(bytes, encodingName);
                    }
                    catch (UnsupportedEncodingException unsupportedEncodingException) {
                        throw new AWGenericException(unsupportedEncodingException);
                    }
                }
            }
        }
        return templateString;
    }

    protected InputStream templateInputStream ()
    {
        AWResource templateResource = templateResource();
        return templateResource.inputStream();
    }

    protected AWTemplate parseTemplate (InputStream fileInputStream)
    {
        AWTemplate template = null;
        AWTemplateParser templateParser = templateParser();
        try {
            String templateString = null;
            boolean shouldExpectEncoding = templateHasEncoding();
            if (shouldExpectEncoding) {
                templateString = AWUtil.stringWithContentsOfInputStream(fileInputStream, shouldExpectEncoding);
            }
            else {
                templateString = readTemplateString(fileInputStream);
            }
            if (allowsWhitespaceCompression()) {
                templateString = AWUtil.leftJustify(templateString);
            }
            if (templateParser instanceof AWHtmlTemplateParser) {
                template = ((AWHtmlTemplateParser)templateParser).templateFromString(templateString, templateName(), this);
            }
            else {
                template = templateParser.templateFromString(templateString, templateName());
            }
        }
        finally {
            try {
                fileInputStream.close();
            }
            catch (IOException ioexception) {
                String message = Fmt.S("Error closing file: \"%s\"", templateName());
                throw new AWGenericException(message, ioexception);
            }
        }
        return template;
    }

    protected AWTemplate parseTemplate ()
    {
        InputStream fileInputStream = templateInputStream();
        return parseTemplate(fileInputStream);
    }

    public AWTemplate loadTemplate ()
    {
        AWTemplate template = null;
        AWResource resource = templateResource();
        if (resource != null) {
            template = (AWTemplate)resource.object();
            if ((template == null) ||
                (AWConcreteApplication.IsRapidTurnaroundEnabled
                        && requestContext().currentPhase() == AWRequestContext.Phase_Render
                        && resource.hasChanged())) {

                // reset needs to be called before template parser since the template parser
                // can append validation errors
                componentDefinition().resetValidationData();

                template = parseTemplate();

                if (template != null) {
                    AWApi componentApi  = ((AWConcreteTemplate)template).removeApiTag();

                    if (AWConcreteApplication.IsDebuggingEnabled) {
                        componentDefinition().setComponentApi(componentApi);

                        // validate the componentAPI.  Needed since componentAPI
                        // validation initializes supported binding information.

                        if (componentApi != null && requestContext() != null) {
                            // if there is a component api, then validate it (metadata validation)
                            componentApi.validate(requestContext().validationContext(), this);
                        }
                    }

                    resource.setObject(template);
                }
            }
        }
        return template;
    }

    ////////////////////
    // Parser callbacks
    ////////////////////
    public boolean allowEmbeddedKeyPaths ()
    {
        return true;
    }

    /**
       Wacky: this method is called when this (stateful) component instance that is about
       to be activate is using an out of date class (do to dynamic reloading).  If a non-null
       instance is returned, it is used in place of this instance.  If null is returned,
       the stale instance is used.

       This is only called in dubug mode (i.e. IsRapidTurnaroundEnabled == true)
    */
    protected AWComponent replacementInstance (AWComponent parentComponent)
    {
        AWComponent replacement = null;

        // by default, stateful *sub*-components are replaced, but pageComponents are not
        if (parentComponent != null) {
            replacement = componentDefinition().createComponent(componentReference(), parentComponent, requestContext());
            replacement.ensureAwake(page());
        }
        return replacement;
    }


    /**
        Overridden by AWComponent subclasses to indicate whether component instances
        should be preserved with the page/session (i.e. are "stateful") or can be
        pooled and reused for each phase of request processing (i.e. are stateless)

        Default is to be stateless unless the component is used as the top-level
        (page) component.
     */
    public boolean isStateless ()
    {
        // default is true (and by default components aren't client panels...)
        return !isClientPanel();
    }

    protected void saveInPage (AWElementIdPath elementIdPath)
    {
        requestContext().putStatefulComponent(elementIdPath, this);
    }

    ///////////////
    // AWCycleable
    ///////////////
    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        template().applyValues(requestContext, this);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        return template().invokeAction(requestContext, this);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // the interest level code is an optimization so we do not try (more than once) to find
        // community context for a component that does not have it
        InterestLevel interestLevel = getInterestLevel(this);
        InterestLevel origLevel = interestLevel;

        if (AWConcreteApplication.IsDebuggingEnabled) {
            validate(requestContext.validationContext());
        }

        if (!_componentReference.isStateless()) {
            AWBacktrackState backtrackState = requestContext.backtrackState();
            if ((backtrackState != null) && (backtrackState.component == this)) {
                Object userBacktrackState = backtrackState.userState;
                Object existingUserBacktrackState = restoreFromBacktrackState(userBacktrackState);
                _page.swapBacktrackStates(existingUserBacktrackState);
            }
        }
        // call rendering cycle start and end events
        if (interestLevel == InterestLevel.Interested) {
            interestLevel = componentWillRender(requestContext, this, component);
        }
        template().renderResponse(requestContext, this);
        if (interestLevel == InterestLevel.Interested) {
            interestLevel = componentFinishedRender(requestContext, this, component);
        }

        if (interestLevel == InterestLevel.NeverInterested && interestLevel != origLevel) {
            markNeverInterested(this);
        }
    }

    // these should be called when not calling via ComponentReference
    public void _topLevelApplyValues (AWRequestContext requestContext, AWComponent component)
    {
        applyValues(requestContext, component);
    }

    public AWResponseGenerating _topLevelInvokeAction(AWRequestContext requestContext, AWComponent component)
    {
        return invokeAction(requestContext, this);
    }

    public void _topLevelRenderResponse(AWRequestContext requestContext, AWComponent component)
    {
        renderResponse(requestContext, this);
    }


    public AWEncodedString escapeAttribute (AWEncodedString attributeValue)
    {
        return useXmlEscaping() ? attributeValue.xmlEscapedString() : attributeValue.htmlAttributeString();
    }

    public AWEncodedString escapeString (Object object)
    {
        return useXmlEscaping() ? AWUtil.escapeXml(object) : AWUtil.escapeHtml(object);
    }

    public AWEncodedString escapeUnsafeString (Object object)
    {
        return useXmlEscaping() ? AWUtil.escapeXml(object) :
                                  AWUtil.escapeUnsafeHtml(object);
    }

    public boolean shouldCloseElements ()
    {
        return useXmlEscaping();
    }

    public boolean useXmlEscaping ()
    {
        return false;
    }

    //////////////////////////
    // Page Cache Management
    //////////////////////////
    public void recordBacktrackState (Object userBacktrackState)
    {
        if (_componentReference.isStateless()) {
            throw new AWGenericException("Attempt to recordBacktrackState(Object) from stateless component.");
        }
        _page.recordBacktrackState(this, userBacktrackState);
    }

    public void recordBacktrackState (int userBacktrackState)
    {
        recordBacktrackState(Constants.getInteger(userBacktrackState));
    }

    public void removeBacktrackState ()
    {
        _page.removeBacktrackState();
    }

    public void truncateBacktrackState ()
    {
        _page.truncateBacktrackState();
    }

    public void truncateBacktrackState (AWBacktrackState backtrackStateMark)
    {
        _page.truncateBacktrackState(backtrackStateMark);
    }

    public AWBacktrackState markBacktrackState ()
    {
        return _page.markBacktrackState();
    }

    public Object restoreFromBacktrackState (Object userBacktrackState)
    {
        throw new AWGenericException(getClass().getName() + ": backtrack state was found, but component did not implement takeValuesFromBacktrackState(Object userState)");
    }

    protected boolean shouldRedirect ()
    {
        return true;
    }

    public boolean shouldCachePage ()
    {
        return true;
    }

    ////////////////////////
    // AWResponseGenerating
    ////////////////////////
    public AWResponse generateResponse (AWResponse response, AWRequestContext requestContext)
    {
        _page.ensureAwake(requestContext);
        requestContext.setPage(_page);
        return requestContext.generateResponse(response);
    }

    public AWResponse generateResponse (AWResponse response)
    {
        AWRequestContext existingRequestContext = requestContext();
        AWRequest request = existingRequestContext == null ? null : existingRequestContext.request();
        AWApplication application = (AWApplication)AWConcreteApplication.sharedInstance();
        AWRequestContext requestContext = application.createRequestContext(request);
        if (existingRequestContext != null) {
            HttpSession existingHttpSession = existingRequestContext.existingHttpSession();
            if (existingHttpSession != null) {
                requestContext.setHttpSession(existingHttpSession);
            }
        }
        return generateResponse(response, requestContext);
    }

    public AWResponse generateResponse ()
    {
        return generateResponse(null);
    }

    public String generateStringContents ()
    {
        AWRequestContext newRequestContext = null;
        AWApplication application = (AWApplication)AWConcreteApplication.sharedInstance();
        AWRequestContext existingRequestContext = requestContext();
        if (existingRequestContext != null) {
            newRequestContext = application.createRequestContext(existingRequestContext.request());
            HttpSession existingHttpSession =
                existingRequestContext.existingHttpSession();
            newRequestContext.setHttpSession(existingHttpSession);
        }
        else {
            newRequestContext = application.createRequestContext(null);
        }

        // disable incremental update since we're trying to get the string contents
        newRequestContext.isContentGeneration(true);

        AWResponse response = generateResponse(null, newRequestContext);
        return response.generateStringContents();
    }

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        // Default is to do nothing.  Users can override, but are not required to call super.
    }

    protected void flushState ()
    {
        clearDict(this);
    }

    public void ensureAwake (AWPage page)
    {
        if (!_isAwake) {
            setPage(page);
            awake();
            _isAwake = true;
        }
    }

    protected void sleep ()
    {
        // Default is to do nothing.  Users can override, but are not required to call super.
    }

    protected void ensureAsleep ()
    {
        if (_isAwake) {
            _isAwake = false;
            sleep();
            _currentTemplateElement = null;
            if (isStatelessSubComponent() && !useLocalPool()) {
                _otherBindingsBinding = null;
            }
            // note: _parent, _componentReference, and _page are set to null in AWComponentRef (but only for stateless);
        }
    }

    protected void hibernate ()
    {
        // Default is to do nothing.  Users can override, but are not required to call super.
    }

    /**
     * Called when the page this component is associated with is no longer on the top
     * of the page cache (ie, the user has "moved off" the page).  Note that this is
     * only called on the "page component" (ie, the component that is the root of the
     * component hierarchy for this page).
     * @aribaapi private
     */
    protected void exit ()
    {
        //Log.aribaweb_request.debug("Exiting: %s",componentPath().toString());
    }

    ///////////////////
    // Bindings Support
    ///////////////////
    public String[] supportedBindingNames ()
    {
        return null;
    }

    public AWBindingDictionary bindings ()
    {
        return _componentReference.bindings();
    }

    public AWBindingDictionary otherBindings ()
    {
        return _componentReference.otherBindings();
    }

    public AWStringDictionary otherBindingsValues ()
    {
            // This merges otherBindings from the parent with other
            // bindings bound to this component giving preference to the
            // parent's bindings (allow parent to override child).
            // Is this *always* what we want, or do we need to allow
            // developers to decide how to merge things?
        AWStringDictionary otherBindingsValues = (AWStringDictionary)
            ((_otherBindingsBinding == null) ?
            valueForBinding(AWBindingNames.otherBindings) :
            valueForBinding(_otherBindingsBinding));
        AWBindingDictionary bindingDictionary = _componentReference.otherBindings();
        if (bindingDictionary != null) {
            if (otherBindingsValues == null) {
                otherBindingsValues = _page.otherBindingsValuesScratch();
            }
            for (int index = (bindingDictionary.size() - 1); index >= 0;index--) {
                AWEncodedString currentBindingName = bindingDictionary.nameAt(index);
                AWBinding currentBinding = bindingDictionary.elementAt(index);
                AWEncodedString currentBindingValue = encodedStringValueForBinding(currentBinding);
                if (currentBindingValue != null) {
                    otherBindingsValues.putIfIdenticalKeyAbsent(currentBindingName, currentBindingValue);
                }
            }
        }
        return otherBindingsValues;
    }

    public AWBinding bindingForName (String bindingName)
    {
        // This is provided for backward compatibility -- not recommended for use.
        return bindingForName(bindingName, true);
    }

    //public static AWMultiKeyHashtable BindingCounts = new AWMultiKeyHashtable(2);
    //public static int BindingCountTotal = 0;

    public AWBinding bindingForName (String bindingName, boolean recursive)
    {
        /*
        BindingCountTotal++;
        Integer bindingCountObj = (Integer)BindingCounts.get(bindingName, componentDefinition());
        int bindingCount = bindingCountObj == null ? 1 : bindingCountObj.intValue() + 1;
        BindingCounts.put(bindingName, componentDefinition(), Constants.getInteger(bindingCount));
        if (BindingCountTotal % 5000 == 0) {
            System.out.println("***** BindingCountTotal: " + BindingCountTotal);
            BindingCounts.printf();
        }
        */
        AWBinding localBinding = _componentReference.bindingForName(bindingName, _parent);
        if (recursive && !hasBinding(localBinding)) {
            localBinding = null;
        }
        return localBinding;
    }

    public boolean hasBinding (String bindingName)
    {
        return bindingForName(bindingName, true) != null;
    }

    public boolean hasBinding (AWBinding binding)
    {
        return binding == null ? false : binding.bindingExistsInParentForSubcomponent(this);
    }

    // Int
    public double doubleValueForBinding (AWBinding binding)
    {
        return (binding != null) ? binding.doubleValue(_parent) : 0.0;
    }

    public double doubleValueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return doubleValueForBinding(binding);
    }

    public double doubleValueForBinding (String bindingName, double defaultValue)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return hasBinding(binding) ? doubleValueForBinding(binding) : defaultValue;
    }

    // Int
    public int intValueForBinding (AWBinding binding)
    {
        int intValue = 0;
        if (binding != null) {
            intValue = binding.intValue(_parent);
        }
        return intValue;
    }

    public int intValueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return intValueForBinding(binding);
    }

    public int intValueForBinding (String bindingName, int defaultValue)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return hasBinding(binding) ? intValueForBinding(binding) : defaultValue;
    }

    // Boolean
    public boolean booleanValueForBinding (AWBinding binding)
    {
        return (binding != null) && binding.booleanValue(_parent);
    }

    public boolean booleanValueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return booleanValueForBinding(binding);
    }

    public boolean booleanValueForBinding (AWBinding binding, boolean defaultValue)
    {
        return hasBinding(binding) ? booleanValueForBinding(binding) : defaultValue;
    }

    public boolean booleanValueForBinding (String bindingName, boolean defaultValue)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return booleanValueForBinding(binding, defaultValue);
    }

    // Object
    public Object valueForBinding (AWBinding binding)
    {
        Object objectValue = null;
        if (binding != null) {
            objectValue = binding.value(_parent);
        }
        return objectValue;
    }

    public Object valueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return valueForBinding(binding);
    }

    public Object valueForBinding (String bindingName, Object defaultValue)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return hasBinding(binding) ? valueForBinding(binding) : defaultValue;
    }

    // String
    public String stringValueForBinding (AWBinding binding)
    {
        String stringValue = null;
        if (binding != null) {
            stringValue = binding.stringValue(_parent);
        }
        return stringValue;
    }

    public String stringValueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return stringValueForBinding(binding);
    }

    public String stringValueForBinding (String bindingName, String defaultValue)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return hasBinding(binding) ? stringValueForBinding(binding) : defaultValue;
    }

    // AWEncodedString
    public AWEncodedString encodedStringValueForBinding (AWBinding binding)
    {
        AWEncodedString encodedString = null;
        if (binding != null) {
            encodedString = binding.encodedStringValue(_parent);
        }
        return encodedString;
    }

    public AWEncodedString encodedStringValueForBinding (String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        return encodedStringValueForBinding(binding);
    }

    // Object
    public void setValueForBinding (Object objectValue, AWBinding binding)
    {
        if (binding != null) {
            binding.setValue(objectValue, _parent);
        }
    }

    public void setValueForBinding (Object objectValue, String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        setValueForBinding(objectValue, binding);
    }

    // ** Int
    public void setValueForBinding (int intValue, AWBinding binding)
    {
        if (binding != null) {
            binding.setValue(intValue, _parent);
        }
    }

    public void setValueForBinding (int intValue, String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        setValueForBinding(intValue, binding);
    }

    // ** Booelan
    public void setValueForBinding (boolean booleanValue, AWBinding binding)
    {
        if (binding != null) {
            binding.setValue(booleanValue, _parent);
        }
    }

    public void setValueForBinding (boolean booleanValue, String bindingName)
    {
        AWBinding binding = bindingForName(bindingName, false);
        setValueForBinding(booleanValue, binding);
    }

    ///////////////////
    // "xml" support
    ///////////////////
    public static void registerXmlNodeWithName (String xmlNodeName)
    {
        defaultTemplateParser().registerContainerClassForTagName(xmlNodeName, AWConcreteXmlNode.class);
        defaultTemplateParser().registerElementClassForTagName(xmlNodeName, AWConcreteXmlNode.class);
    }

    public AWXmlNode xml ()
    {
        // Perf: recycling opportunity?
        return new AWXmlContext(_componentReference.xmlNode(), _parent);
    }

    /////////////////
    // Convenience
    /////////////////
    public AWComponent pageWithName (String pageName)
    {
        return _page.requestContext().pageWithName(pageName);
    }

    public AWComponent pageWithName (String pageName, Map<String, Object>assignments)
    {
        return _page.requestContext().pageWithName(pageName, assignments);
    }

    public <T> T pageWithClass (Class<T> tClass)
    {
        return (T)pageWithName(tClass.getName());
    }

    public <T> T pageWithClass (Class<T> tClass, Map<String, Object>assignments)
    {
        return (T)pageWithName(tClass.getName(), assignments);
    }

    public Object componentConfiguration(String configName)
    {
        return _page.componentConfiguration(this, configName);
    }

    public void setComponentConfiguration(String configName, Object config)
    {
        _page.setComponentConfiguration(this, configName, config);
    }

    ///////////////////
    // Dict Support
    ///////////////////
    public Map extendedFields ()
    {
        if (_extendedFields == null) {
            _extendedFields = MapUtil.map();
        }
        return _extendedFields;
    }

    protected void clearDict (AWComponent component)
    {
        if (_extendedFields != null) {
            _extendedFields.clear();
        }
    }

    public Map dict ()
    {
        // don't call extendedFields() so subclasses can implement Extensible
        // without rerouting dict()
        if (_extendedFields == null) {
            _extendedFields = MapUtil.map();
        }
        return _extendedFields;
    }

    public void dict (String key, Object value)
    {
        dict().put(key, value);
    }

    public Object dict (String key)
    {
        return (_extendedFields == null) ? null : dict().get(key);
    }

    protected Map pageDict ()
    {
        return _page.pageComponent().dict();
    }

    public AWPageRedirect redirectToPage (AWComponent destinationPage)
    {
            // this should be an AWUtil static method ??
        if (destinationPage == null) {
            destinationPage = _page.pageComponent();
        }
        AWPageRedirect pageRedirect = (AWPageRedirect)pageWithName(AWPageRedirect.PageName);
        pageRedirect.setPage(destinationPage);
        return pageRedirect;
    }

    /////////////////
    // Debugging
    /////////////////
    public AWComponent awcyclePageAndLog ()
    {
        debugString("*** awcyclePageAndLog");
        return null;
    }

    /**
        Create a handy identifier for this component.
        This method creates an identifier composed of the containing template
        location and the class name.
        @return the identifier
        @aribaapi private
    */
    protected final String debugIdentifier ()
    {
        String classname = this.getClass().getName();
        String template = parent().fullTemplateResourceUrl();
        AWBaseElement elem = (AWBaseElement)parent().currentTemplateElement();
        int lineNumber = elem.lineNumber();

        return Fmt.S("%s:%s.%s", classname, template,
                     Constants.getInteger(lineNumber));
    }

    protected String fullTemplateResourceUrl ()
    {
        return templateResource().fullUrl();
    }

    public AWFastStringBuffer componentPath (String separatorString)
    {
        AWFastStringBuffer stringBuffer = new AWFastStringBuffer();
        AWComponent currentComponent = this;
        while (currentComponent != null) {
            stringBuffer.append("        ");
            if (currentComponent.getClass() == AWComponent.ClassObject) {
                AWResource resource = currentComponent.templateResource();
                if (resource != null) {
                    stringBuffer.append(resource.fullUrl());
                }
                else {
                    stringBuffer.append(currentComponent.namePath());
                    stringBuffer.append(".awl");
                }
                AWBaseElement elem = (AWBaseElement)currentComponent.currentTemplateElement();
                if (elem != null) {
                    stringBuffer.append(":");
                    stringBuffer.append(elem.lineNumber());
                }
            }
            else {
                AWBaseElement elem = (AWBaseElement)currentComponent.currentTemplateElement();
                if (elem instanceof AWBindableElement) {
                    AWBindableElement bindable = (AWBindableElement)elem;
                    stringBuffer.append(bindable.tagName());
                }
                else {
                    // needed by toolkit / non-bindeable element based code
                    stringBuffer.append(currentComponent.toString());
                }
                if (elem != null) {
                    stringBuffer.append("(");
                    stringBuffer.append(currentComponent.fullTemplateResourceUrl().replaceAll("^file\\:/", "/"));
                    stringBuffer.append(":");
                    stringBuffer.append(elem.lineNumber());
                    stringBuffer.append(")");
                }
            }

            stringBuffer.append(separatorString);
            currentComponent = currentComponent.parent();
        }
        return stringBuffer;
    }

    protected AWFastStringBuffer componentPath ()
    {
        return componentPath(": ");
    }

    protected String operationIdentifierForKeyPath (String keyPath)
    {
        return null;
    }

    ////////////////////////
    // Formatter Validation
    ////////////////////////
    /**
     * Retrieve the error manager for the page.
     * @return error manager
     */
    public AWErrorManager errorManager ()
    {
        return _page.errorManager();
    }

    public AWFormValueManager formValueManager ()
    {
        return _page.formValueManager();
    }

    /**
     * Record an error to the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the setErrorMessageAndValue().
     * @param errorKey The object that identifies the error.  This is typically a string.
     * @param errorMessage The message that describes the error.
     * @param errantValue The unparsable value that the user entered.  Since
                          the parsing failed, we cannot store this value in
                          the field.  We stash it away here so we can display
                          in the UI later.
     * @aribaapi ariba
     */
    public void recordValidationError (Object errorKey,
                                       String errorMessage,
                                       Object errantValue)
    {
        Assert.that(errorKey != null, "errorKey cannot be null");
        AWErrorInfo error = new AWErrorInfo(
            errorKey, errorMessage, errantValue, false);
        recordValidationError(error);
    }

    /**
     * Record an error to the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the setErrorMessageAndValue().
     * @param error The error object.
     * @aribaapi ariba
     */
    public void recordValidationError (AWErrorInfo error)
    {
        Assert.that(error != null, "Cannot record an error with a NULL AWErrorInfo");
        AWErrorManager.AWNewErrorManager errorManager =
            (AWErrorManager.AWNewErrorManager)_page.errorManager();
        errorManager.setErrorMessageAndValue(error);
    }

    /**
     * Record multiple error to the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the setErrorMessageAndValue().
     * @param errors a list of AWErrorInfo objects
     * @aribaapi ariba
     */
    public void recordValidationErrors (List errors)
    {
        for (int i = 0; i < errors.size(); i++) {
            recordValidationError((AWErrorInfo)errors.get(i));
        }
    }

    /**
     * Record an error to the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the setErrorMessageAndValue().
     * @param exception
     * @param errorKey The object that identifies the error.  This is typically a string.
     * @param errantValue The unparsable value that the user entered.  Since
                          the parsing failed, we cannot store this value in
                          the field.  We stash it away here so we can display
                          in the UI later.
     * @aribaapi ariba
     */
    public void recordValidationError (Throwable exception, Object errorKey,
                                       Object errantValue)
    {
        Assert.that(errorKey != null, "errorKey cannot be null");
        String  errorMessage = exception.getLocalizedMessage();
        AWErrorInfo error = new AWErrorInfo(
            errorKey, errorMessage, errantValue, false);
        recordValidationError(error);
    }

    /**
     * Record an error to the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the setErrorMessageAndValue().
     * @param errorKey The object that identifies the error.  This is typically a string.
     * @param warningMessage The message that describes the warning.
     * @aribaapi ariba
     */
    public void recordValidationWarning (Object errorKey, String warningMessage)
    {
        Assert.that(errorKey != null, "errorKey cannot be null");
        AWErrorInfo error = new AWErrorInfo(
            errorKey, warningMessage, null, true);
        recordValidationError(error);
    }

    /**
     * Clear an error in the error manager.
     * To avoid confusion between the current and new ErrorManagers,
     * this is the only way to access the clearErrorForKey().
     * @param errorKey
     * @aribaapi ariba
     */
    public void clearValidationError (Object errorKey)
    {
        if (errorKey == null) {
            return;
        }
        AWErrorManager.AWNewErrorManager errorManager =
            (AWErrorManager.AWNewErrorManager)_page.errorManager();
        errorManager.clearErrorForKey(errorKey);
    }

    //////////////////////////////////
    // Localized Java Strings support
    //////////////////////////////////
    public String localizedJavaString (int stringId, String originalString)
    {
        return _componentReference.componentDefinition().localizedJavaString(stringId, originalString, this, _page.resourceManager());
    }

    public AWStringsThunk strings ()
    {
        return resourceManager().strings();
    }

    public String urlForResourceNamed (String resourceName)
    {
        AWRequestContext requestContext = requestContext();
        boolean isMetaTemplateMode = requestContext.isMetaTemplateMode();
        return urlForResourceNamed(resourceName, isMetaTemplateMode);
    }
        
    public String urlForResourceNamed (String resourceName, boolean useFullUrl)
    {
        return urlForResourceNamed (resourceName, useFullUrl, false);
    }

    public String urlForResourceNamed (String resourceName, boolean useFullUrl, boolean isVersioned)
    {
        AWRequestContext requestContext = requestContext();
        boolean isSecure = useFullUrl ? requestContext.request() != null && requestContext.request().isSecureScheme() : false;
        String url = resourceManager().urlForResourceNamed(resourceName, useFullUrl, isSecure, isVersioned);
        return url;
    }


    /*-----------------------------------------------------------------------
        Localizaton convenience methods
      -----------------------------------------------------------------------*/

    public boolean isBidirectional ()
    {
        return I18NUtil.isBidirectional(preferredLocale());
    }

    public String languageDirection ()
    {
        return I18NUtil.languageDirection(preferredLocale());
    }

    public String languageRight ()
    {
        return I18NUtil.languageRight(preferredLocale());
    }

    public String languageLeft ()
    {
        return I18NUtil.languageLeft(preferredLocale());
    }

    /*----------------------------------------------------------------------
        record and playback
    --------------------------------------------------------------------*/
    /**
     * If any component wants to provide a semantic key different from the
     * default, it should override _debugSemanticKeyInteresting() and _debugSemanticKey()
     * @return whether key is interesting
     */
    protected boolean _debugSemanticKeyInteresting ()
    {
        return _debugPrimaryBinding() == null;
    }

     protected String _debugSemanticKey ()
     {
         String semanticKey = null;
         if (semanticKey == null) {
             AWBinding binding = _debugPrimaryBinding();
             if (binding != null) {
                 semanticKey = AWRecordingManager.actionEffectiveKeyPathInComponent(binding, parent());
             }
             else if (semanticKey == null) {
                 semanticKey = ClassUtil.stripPackageFromClassName(name());
             }
         }
         return semanticKey;
     }

    protected AWBinding _debugPrimaryBinding ()
    {
        AWBinding primaryBinding = null;
        AWComponentReference componentRef = componentReference();

        if (componentRef != null) {
            primaryBinding = bindingForName(AWBindingNames._awname, false);
            if (primaryBinding == null) {
                primaryBinding = bindingForName(AWBindingNames.action, false);
                if (primaryBinding == null) {
                    primaryBinding = bindingForName(AWBindingNames.value, false);
                }
            }
        }
        return primaryBinding;
    }

    /**
     * enhanced implmentation for record & playback:
     * component returns a semantic key based on the following algorithm:
     * If the component reference is interesting (ie, not widgets), then use the primary
     * binding key path such as action or value as the semantic key.  Otherwise, ask if
     * the parent is interesting, if yes, return the parent semantic key :: my semantic key.
     * Otherwise, recursively ask the parent to return its semantic key
     */
    protected String _debugCompositeSemanticKey (String bestKeySoFar)
    {
        if (_debugSemanticKeyInteresting() || pageComponent() == this) {
            String myKey = _debugSemanticKey();
            return bestKeySoFar == null ? myKey :
                    StringUtil.strcat(_debugSemanticKey(), ":", bestKeySoFar);
        }
        if (bestKeySoFar == null) {
            bestKeySoFar = _debugSemanticKey();
        }
        return parent()._debugCompositeSemanticKey(bestKeySoFar);
    }

    /**
     * Sometimes, the semantic key logging in AWGenericElement is not enough or doesn't work
     * for this particular component. The component can override _debugRecordMapping
     * and return false for _debugRecordingMappingInGenericElement
     * @param requestContext
     * @param component
     */
    protected void _debugRecordMapping (AWRequestContext requestContext, AWComponent component)
    {

    }

    protected boolean _debugRecordMappingInGenericElement ()
    {
        return true;
    }

    /**
     * Override this method to disable wrapping of components in spans when doing componentPathDebugging
     * @return whether component path debugging is allowed for this component
     */
    protected boolean allowComponentPathDebugging ()
    {
        return true;
    }

    /**
     * Override this method to restore the behavior in this page to that of aribaweb-7.
     * This only applies to page-level components
     */
    public boolean requiresPreGlidCompatibility ()
    {
        // all modules are now using modern approach
        return false;
    }

    /**
     * Allows AWSessionValidator to evaluate when the page.ensureAwake is called.
     * Should be overridden by page components that do not require session validation.
     *
     * @return true by default
     */
    protected boolean shouldValidateSession ()
    {
        return true;
    }

    protected void validateSession (AWRequestContext requestContext)
    {
        requestContext.application().assertValidSession(requestContext);
    }

    /**
         Provides verification on the request before its handled.
         One of the items that are being checked for component action
         requests is to protected against "CSRF" attack, this is done
         by placing on session identifier on all component actions
         requests and validating against the session on the server.

         @return true by default
     */
    protected boolean shouldValidateRequest ()
    {
        return true;
    }

    protected void validateRequest (AWRequestContext requestContext)
    {
        requestContext.application().validateRequest(requestContext);
    }

    /**
         Provides verification on the request before it's handled.
                           
         @return true by default
     */
    protected boolean shouldValidateRemoteHost ()
    {
        return true;
    }
    
    /**
        Method called after applyValues phase is completed.
        This method is highly dangerous.  Note that whenever this method is called no bindings
        may be evaluated because all components are out of band.
        @deprecated
    */
    public void postTakeValueActions ()
    {
        // Only implemented for page components. Called after all of the values have been
        // pushed from a request.
    }

    protected boolean allowDeferredResponse ()
    {
        return true;
    }

    protected boolean isBoundary ()
    {
        return false;
    }

    protected boolean actionTracingEnabled ()
    {
        return false;
    }

    /**
     * Allows for parameter field value syntax in AWL files.
     * See AWParameters for more information.
     *
     * example: $AWParameters.System.UI.Table.MinHeight
     * @aribaapi private
     */
    public AWParameters getAWParameter ()
    {
        return application().getConfigParameters();
    }

    /**
     * Only applicable to pageComponents.  Called by AWPage when AWPage receives a
     * change notification.  See AWPage.notifyChange()
     * @aribaapi private
     */
    public void notifyChange ()
    {
        // Default is to do nothing.  Users can override,
        // but are not required to call super.
    }
	
	/**
	* Sets the destination page for performance monitoring.  This method is defined to
    * allow component subclasses to control the setting of the perf. logging fields.
    */
    public void setPerfDestinationInfo()
    {
        if (PerformanceState.threadStateEnabled()) {
             PerformanceState.getThisThreadHashtable().setDestinationPage(namePath());
        }
	}

    /**
     * Provide ability to turn off community features at the application level
     * @return
     */
    public boolean isUserCommunityEnabled()
    {
        return AWConcreteApplication.isUserCommunityEnabled();
    }

    /**
     * Get window size on which In Situ pane will fold. 
     * @return int
     */
    public int getFoldInSituOnWindowSizeParam ()
    {
        return AWConcreteApplication.getFoldInSituWindowSize();
    }
    /**
     * Implementers of this interface can be used in conjunction with the rendering listener
     * interfaces below, or anywhere to model a boolean predicate on the 3 rendering argus
     */
    public static interface RenderingFilter {

        public boolean isSatisfiedBy (AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent);
    }

    /**
     * Allow listeners to monitor rendering 'lifecycle' events
     */
    public static interface RenderingListener
    {
        /**
         * The APIs return this enum to indicate whether further calls should be made.
         * The common case is that during render we will call componentWillRender which
         * might return NeverInterested if there was no community context for this component.
         * This value should be cached in platform and further calls will not be made
         * for this component.
         *
         * For another component the call might return NotInterested in which case there is
         * not reason to call any later APIS (componentClosingTag or componentFinishedRender)
         * in the context of the rendering of this particular component.  Next time this same
         * component is rendered the result might be different.
         *
         * If Interested is returned, then later calls will be made
         */
        static public enum InterestLevel {
            NeverInterested,
            NotInterested,
            Interested,
        }
        // called before render
        InterestLevel componentWillRender (AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent);

        // called after rendering content, but before closing tag (this is a point
        // at which content can be injected into the rendered result).
        // Note that this call will only be generated by some container components
        InterestLevel componentClosingTag (AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent);

        // called after render
        InterestLevel componentFinishedRender (AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent);
    }

    private static InterestLevel maxInterest(InterestLevel m0, InterestLevel m1)
    {
        return (m0.compareTo(m1) > 0) ? m0 : m1;
    }

    /**
     * Give listener chance to clean up any state now that this rendering is finished.
     *
     *
     * @param requestContext
     * @param thisElem
     * @param parentComponent
     * @return InterestLevel of Interested to continue with next API in this session,
     *                           NeverInterested to make no more calls to these APIs for this component again
     *                           NotInterested to make no more calls to these APIs for this call to component renderResponse
     *
     * Here is the explantion of InterestedLevel complication.
     * If any of the calls to the listener return Interested, then this method must return Interested.
     * Else if any of the calls to the listener return NotInterested, then this method must return NotInterested.
     * Else (by default) return NeverInterested
     *
     * So for any particular call:
     * We implement this by keeping track of the maximum inmtrest level
     * (where NeverIntersted < NotInterested < Interested
     *
     */
    protected InterestLevel componentWillRender(AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent)
    {
        if (_RenderingListeners == null || _RenderingListeners.size() == 0) {
            return InterestLevel.NeverInterested;
        }
        InterestLevel retInterestLevel = InterestLevel.NeverInterested;

        for (RenderingListener l : _RenderingListeners) {
            InterestLevel interestLevel =
                    l.componentWillRender(requestContext, thisElem, parentComponent);
            
            retInterestLevel = maxInterest(retInterestLevel, interestLevel);
        }

        return retInterestLevel;
    }

    /**
     * Give listener chance to add any content before the closing tag is emitted.  The listener must ensure
     * that any generated HTML will be valid.
     *
     *
     * @param requestContext
     * @param thisElem
     * @param parentComponent
     * @return InterestLevel of Interested to continue with next API in this session,
     *                           NeverInterested to make no more calls to these APIs for this component again
     *                           NotInterested to make no more calls to these APIs for this call to component renderResponse
     *
     * @see AWComponent#componentWillRender for explantion of InterestedLevel complication.
     * 
     */
    protected InterestLevel componentClosingTag(AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent)
    {
        if (_RenderingListeners == null || _RenderingListeners.size() == 0) {
            return InterestLevel.NeverInterested;
        }
        InterestLevel retInterestLevel = InterestLevel.NeverInterested;

        for (RenderingListener l : _RenderingListeners) {
            InterestLevel interestLevel =
                    l.componentClosingTag(requestContext, thisElem, parentComponent);

            retInterestLevel = maxInterest(retInterestLevel, interestLevel);
        }

        return retInterestLevel;
    }

    /**
     * Give listener chance to clean up any state now that this rendering is finished.
     *     *
     * @param requestContext
     * @param thisElem
     * @param parentComponent
     * @return InterestLevel of Interested to continue with next API in this session,
     *                           NeverInterested to make no more calls to these APIs for this component again
     *                           NotInterested to make no more calls to these APIs for this call to component renderResponse
     *
     * @see AWComponent#componentWillRender for explantion of InterestedLevel complication.
     * 
     */
    protected InterestLevel componentFinishedRender(AWRequestContext requestContext, AWCycleable thisElem, AWComponent parentComponent)
    {

        if (_RenderingListeners == null || _RenderingListeners.size() == 0) {
            return InterestLevel.NeverInterested;
        }

        InterestLevel retInterestLevel = InterestLevel.NeverInterested;

        for (RenderingListener l : _RenderingListeners) {
            InterestLevel interestLevel =
                    l.componentFinishedRender(requestContext, thisElem, parentComponent);

            retInterestLevel = maxInterest(retInterestLevel, interestLevel);
        }

        return retInterestLevel;
    }

    private static List<RenderingListener> _RenderingListeners = null;

    public static void registerRenderingListener(RenderingListener listener)
    {
        if (_RenderingListeners == null) _RenderingListeners = new ArrayList();
        _RenderingListeners.add(listener);
    }

    /**
     * If a component (by name) shows up in this set then we are not interested in it, ever!
     * We will then skip calling the RenderingListener APIs for that component.
     */
    private static GrowOnlyHashSet _CachedNeverInterestedSet =
            new GrowOnlyHashSet();

    /**
     * This method will assume interest unless the component name shows up in the never interested set.
     *
     * @param comp
     * @return
     */
    static InterestLevel getInterestLevel(AWComponent comp)
    {
        return (_CachedNeverInterestedSet.contains(comp.name()))
                ? InterestLevel.NeverInterested
                : InterestLevel.Interested;
    }

    static void markNeverInterested(AWComponent comp)
    {
        _CachedNeverInterestedSet.add(comp.name());
    }
}

/*
final class AWMultiKeyHashtable extends MultiKeyHashtable
{
    public AWMultiKeyHashtable (int keyCount)
    {
        super(keyCount);
    }

    public void printf ()
    {
        super.printf();
    }
}
*/
