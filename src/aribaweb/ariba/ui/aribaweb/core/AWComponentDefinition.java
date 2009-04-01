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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponentDefinition.java#47 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AW2DVector;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWRecyclePool;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.util.core.FormatBuffer;
import ariba.util.core.Fmt;
import java.util.Map;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import java.util.List;
import java.io.File;
import java.util.Iterator;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AWComponentDefinition extends AWBaseObject
{
    private static int ComponentDefinitionId = 0;
    private String _componentName;
    private String _componentNamePath;
    private String _templateName;
    private Class _componentClass;
    private String _resourceClassName;
    private AWRecyclePool _componentPool;
    private boolean _isStatelessComponent;
    private boolean _isClassless;
    private String[] _supportedBindingNames;
    private AWComponentReference _sharedComponentReference;
    private int _componentDefinitionId = -1;
    private AW2DVector _localizedStrings;
    private boolean _isReloadable;
    private AWApi _componentApi;

    protected static final int UnsupportedBindingDefinition = 0;
    protected static final int MissingSupportedBindingDefinition = 1;

    //////////////////
    // Validation
    //////////////////
    // erroneous bindings used in the template(s) associated with this definition
    private List _bindingErrorList = null;

    // Reference tracking
    private List _referencedBy = null;
    private List _referencedByLocations = null;

    private boolean _isPageLevel = false;

    // bindings to set via reflection
    protected Map _bindingFields;

    // AWApi errors
    private List _unsupportedBindingDefinitions = null;
    private List _missingSupportedBindingDefinitions = null;
    private List _invalidBindingAlternates = null;
    private List _mismatchedBindingAlternates = null;

    // template parsing errors
    private List _templateParsingErrors = null;

    private int _componentApiErrorCount = 0;

    // Inferred bindings
    private Map _empiricalApiTable =  null;

    // AWApi binding references
    private Map _bindingReferenceTable = null;

    public class EmpiricalApiData
    {
        public String _bindingName;
        public List _referencedBy;
        public List _locationInReference;

        public String printReferencedBy ()
        {
            FormatBuffer sbReturn = new FormatBuffer();
            for (int i = 0, size = _referencedBy.size(); i < size; i++) {
                if (i!=0) {
                    sbReturn.append(",");
                }
                sbReturn.append(_referencedBy.get(i) + ":" + _locationInReference.get(i));
            }
            return sbReturn.toString();
        }
    }

    public class ComponentReferenceLocation
    {
        private String _componentName;
        private List _lineNumbers;

        public ComponentReferenceLocation (String componentName, String position)
        {
            _componentName = componentName;
            _lineNumbers = ListUtil.list();
            _lineNumbers.add(position);
        }

        public String componentName ()
        {
            return _componentName;
        }

        public void addLineNumber (String lineNumber)
        {
            _lineNumbers.add(lineNumber);
        }

        public String lineNumbers ()
        {
            String sReturn = (String)ListUtil.firstElement(_lineNumbers);
            if (_lineNumbers.size() != 1) {
                sReturn = sReturn + ", ...";
            }
            return sReturn;
        }
    }

    public interface ScriptClassProvider extends AWBindable {
        // public Class componentSubclass (String name, AWTemplate template);
        public Class componentSubclass (String packageName, String filename, String body, Class superclass);
    }

    static Map<String, ScriptClassProvider> _ScriptProviders = MapUtil.map();

    static public void registerScriptProvider (String tagName, ScriptClassProvider provider)
    {
        AWComponent.defaultTemplateParser().registerContainerClassForTagName(tagName, provider.getClass());
        _ScriptProviders.put("<" + tagName, provider);
    }

    Class classFromProvider (AWResource templateResource)
    {
        if (_ScriptProviders.isEmpty()) return null;
        String templateString = AWUtil.stringWithContentsOfInputStream(templateResource.inputStream(), false);
        for (String tagName : _ScriptProviders.keySet()) {
            int index = templateString.indexOf(tagName);
            if (index != -1) {
                int closeIndex = templateString.indexOf("</" + tagName.substring(1) + ">", index);
                if (closeIndex != -1) {
                    int beginTagEnd = templateString.indexOf(">", index);
                    String body = templateString.substring(beginTagEnd + 1, closeIndex);
                    String className = componentName().replace('.', '_').replace('-', '_'); // templateName().replace('.', '_').replace('-', '_');                    
                    Class cls = _ScriptProviders.get(tagName).componentSubclass(componentPackageName(), className, body, null);
                    if (cls != null) return cls;
                }
            }
        }
        return null;
    }

    /* Thread Safety Considerations: Instances of this class are shared by many threads.  Some ivars are immutable and some require locking.  The mutable ones are:

        _sharedComponent
        _componentPool (often non-existent)
    */

    public static String computeTemplateName (String className, String extension)
    {
        className = className.replace('.', '/');
        return StringUtil.strcat(className, extension);
    }

    public static String computeTemplateName (Class componentClass, String extension)
    {
        return computeTemplateName(componentClass.getName(), extension);
    }

    public static String computeTemplateName (Class componentClass)
    {
        return computeTemplateName(componentClass, AWComponent.ComponentTemplateFileExtension);
    }

    /**
     * Given a template name, compute the associated class name.
     * @param templateName - a template name, calculated according to computeTemplateName
     * @return a fully qualified class name
     * @aribaapi ariba
     */
    public static String computeClassNameFromTemplate (String templateName)
    {
        // remove the extension
        String bareTemplate = templateName.substring(0, templateName.length() -
            AWComponent.ComponentTemplateFileExtension.length());
        // replace the /s
        return bareTemplate.replace('/', '.');
    }

    private String initComponentNamePath (String componentName)
    {
        StringArray components = AWUtil.componentsSeparatedByString(componentName, ".");
        return AWUtil.componentsJoinedByString(components, File.separator);
    }

    public void init (String componentName, Class componentClass)
    {
        this.init();
        _componentName = componentName.intern();
        _componentNamePath = (componentClass == AWComponent.ClassObject)
                 ? initComponentNamePath(_componentName).intern()
                 : initComponentNamePath(componentClass.getName()).intern();
        _componentClass = componentClass;
        _isClassless = _componentClass == AWComponent.class;        
        String origComponentName =
            AWUtil.getClassLoader().getComponentNameForClass(
                componentClass.getName());
        String origClassName = origComponentName.replace('.','/');
        _templateName = StringUtil.strcat(origClassName,
            AWComponent.ComponentTemplateFileExtension);
        initFromComponent();
        _isReloadable = AWUtil.getClassLoader().isReloadable(origComponentName);
    }

    public void setTemplateName (String templateName)
    {
        _templateName = templateName;
        flushCacheIfClassChanged();
    }

    public boolean isStateless ()
    {
        return _isStatelessComponent;
    }

    public String componentName ()
    {
        return _componentName;
    }

    public String componentNamePath ()
    {
        return _componentNamePath;
    }

    /**
     * Check whether this component is classless.
     * A classless component only has an awl file - no java class.
     * @return true if this component is classless
     * @aribaapi ariba
     */
    public boolean isClassless ()
    {
        return _isClassless;
    }

    /*
        Only called from newComponentInstance, this method
        refreshes the class if we are debugging.
    */
    long _lastClassScan = -1;

    public Class componentClass ()
    {
        // Todo: Perf - throttle checking -- maybe once per second per component defn
        if (_isReloadable || _isClassless) {
            flushCacheIfClassChanged();
        }
        return _componentClass;
    }

    public String templateName ()
    {
        return _templateName;
    }

    public String componentPackageName ()
    {
        String currPackage = null;
        if (_isClassless) {
            // we've got a generic component so get the package of
            // the template
            currPackage = AWUtil.fileNameToJavaPackage(templateName());
        }
        else {
            Class componentClass = componentClass();
            Package pkg = componentClass.getPackage();
            currPackage = (pkg != null) ? pkg.getName() : "";
        }

        return currPackage;
    }
    
    /**
     * Get the name of the class where we get our resources from.
     * This is the class in which we resolve AWLocal references.
     * @param componentInstance - the component whose resource class we're fetching
     * @return the full class name of the resource class
     * @aribaapi ariba
     */
    public String resourceClassName (AWComponent componentInstance)
    {
        if (_resourceClassName == null) {
            // do the work to initialized the resource class name
            _resourceClassName =
                computeClassNameFromTemplate(componentInstance.templateName());
        }
        return _resourceClassName;
    }

    public String[] supportedBindingNames ()
    {
        return _supportedBindingNames;
    }

    public AWComponentReference sharedComponentReference ()
    {
        // This is used as the component referernce for all components that are page-level components.
        if (_sharedComponentReference == null) {
            _sharedComponentReference = AWComponentReference.create(this);
            _sharedComponentReference.init(componentName(), EmptyHashtable);
        }
        return _sharedComponentReference;
    }

    public int componentDefinitionId ()
    {
        if (_componentDefinitionId == -1) {
            _componentDefinitionId = ComponentDefinitionId;
            ComponentDefinitionId++;
        }
        return _componentDefinitionId;
    }

    protected void setComponentApi (AWApi componentApi)
    {
        _componentApi = componentApi;
        if (_componentApi != null) {
            AWComponentApiManager.sharedInstance().registerComponentApi(this);
        }
        else {
            AWComponentApiManager.sharedInstance().addMissingAWApi(this);
        }
    }

    public AWApi componentApi ()
    {
        return _componentApi;
    }

    //////////////////////
    // Component Creation
    //////////////////////
    protected AWComponent newComponentInstance ()
    {
        AWComponent componentInstance = null;
        try {
                // changed psheill.  Get latest class, or possibly subclass
            componentInstance = (AWComponent)componentClass().newInstance();
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new AWGenericException(illegalAccessException);
        }
        catch (InstantiationException exception) {
            String message = Fmt.S("Error: Unable to instantiate new instance of component class named \"%s\"", _componentClass.getName());
            throw new AWGenericException(message, exception);
        }

        return componentInstance;
    }

    public AWComponent createComponent (AWComponentReference componentReference, AWComponent parent, AWRequestContext requestContext)
    {
        AWComponent componentInstance = newComponentInstance();
        AWPage page = (parent == null) ? new AWPage(componentInstance, requestContext) : parent.page();
        componentInstance.init(componentReference, parent, page);

        if (parent == null && componentInstance.isValidationEnabled()) {
            setIsPageLevel(true);
        }

        return componentInstance;
    }

    public synchronized AWComponent sharedComponentInstance ()
    {
        if (_isReloadable) {
            flushCacheIfClassChanged();
        }
        // This lazily creates an instance if one is not available.
        AWComponent componentInstance = null;
        if (_componentPool != null) {
            componentInstance = (AWComponent) _componentPool.checkout();
        }
        if (componentInstance == null) {
            componentInstance = newComponentInstance();
        }
        return componentInstance;
    }

    /**
        Only called in debug mode.
    */
    protected void flushCacheIfClassChanged ()
    {
        if (!_isReloadable && !_isClassless) return;         
        Class componentClass = AWUtil.getClassLoader().checkReloadClass(_componentClass);
        if (componentClass != null) setComponentClass(componentClass);

        // Handle templates with embedded script tags (e.g. <groovy></groovy>
        if (_isClassless) {
            AWResourceManager resourceManager = AWComponent.templateResourceManager();
            String templateName = templateName();
            AWResource resource = resourceManager.resourceNamed(templateName);
            checkForEmbeddedScriptChanges(resource);
        }
    }

    protected void checkForEmbeddedScriptChanges (AWResource resource)
    {
        if (resource != null && resource.lastModified() > _lastClassScan) {
            _lastClassScan = System.currentTimeMillis();
            Class newClass = classFromProvider(resource);
            if (newClass != null && newClass != _componentClass) {
                setComponentClass(newClass);
                initFromComponent();
            }
        }
    }

    protected void setComponentClass(Class componentClass)
    {
        if (componentClass != _componentClass) {
            _componentClass = componentClass;
            if (_componentPool != null) {
                initComponentPool();
            }
        }
    }

    public void checkInSharedComponentInstance (AWComponent componentInstance)
    {
        // Users must synchronize before calling this, if multithreaded
        _componentPool.checkin(componentInstance);
    }

    /**
     * Perform some initializations based on a prototype component instance.
     * @aribaapi private
     */
    private void initFromComponent ()
    {
        // This needn't be synchronized since its during initialization and only one thread has visibility to an instance during initialization.
        AWComponent componentInstance = newComponentInstance();
        _isStatelessComponent = componentInstance.isStateless();
        _supportedBindingNames = componentInstance.supportedBindingNames();
        if (_isStatelessComponent && !componentInstance.useLocalPool()) {
            initComponentPool();
        } else {
            _componentPool = null;
        }
    }

    private void initComponentPool ()
    {
        // locking is done externally for AWComponentDefinition -- no need to have AWRecyclePool do locking.
        _componentPool = AWRecyclePool.newPool(32, false,
                ((AWConcreteApplication)AWConcreteApplication.SharedInstance).isStateValidationEnabled());
    }

    public String toString ()
    {
        return StringUtil.strcat(super.toString(), ":\"", _componentName, "\"");
    }

    public AWTemplate defaultTemplate ()
    {
        AWTemplate defaultTemplate = null;
        synchronized (this) {
            AWComponent componentInstance = (_isClassless) ? new AWComponent() : newComponentInstance();
            AWPage page = new AWPage(componentInstance, null);
            componentInstance.setupForNextCycle(sharedComponentReference(), null, page);
            defaultTemplate = componentInstance.template();
            componentInstance.setupForNextCycle(null, null, null);
        }
        return defaultTemplate;
    }

    public Map bindingFields ()
    {
        // thread safety -- okay -- worst case, compute extra array
        if (_bindingFields == null) {
            Map fields = MapUtil.map();
            addBindingFields(_componentClass, fields);
            _bindingFields = fields;
        }
        return _bindingFields;
    }

    private void addBindingFields (Class classObject, Map resultMap)
    {
        Class AWBindingClass = AWBinding.class;
        Field[] fields = classObject.getDeclaredFields();
        for (int index = 0, length = fields.length; index < length; index++) {
            Field field = fields[index];
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) ||
                Modifier.isFinal(modifiers)) {
                continue;
            }
            Class fieldType = field.getType();
            if (AWBindingClass.isAssignableFrom(fieldType)) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (fieldName.startsWith("_") && fieldName.endsWith(AWComponent.BindingSuffix)) {
                    int suffixIndex = fieldName.lastIndexOf(AWComponent.BindingSuffix);
                    String bindingName = fieldName.substring(1, suffixIndex);
                    resultMap.put(bindingName, field);
                }
            }
        }
        // recurse
        Class superclass = classObject.getSuperclass();
        if (superclass != AWComponent.class.getSuperclass()) {
            addBindingFields(superclass, resultMap);
        }
    }

    ///////////////////////
    // Localization Support
    ///////////////////////
    protected String localizedJavaString (int stringId, String originalString, AWComponent component, AWSingleLocaleResourceManager resourceManager)
    {
        // Todo: consider caching jstrings's csv results on the resource to allow for sharing strings across personalities.
        String localizedString = null;
        if (_localizedStrings == null) {
            _localizedStrings = new AW2DVector();
        }
        int resourceManagerIndex = resourceManager.index();
        localizedString = (String)_localizedStrings.elementAt(resourceManagerIndex, stringId);
        if (localizedString == null) {
            synchronized (this) {
                localizedString = (String)_localizedStrings.elementAt(resourceManagerIndex, stringId);
                if (localizedString == null) {
                    Map localizedStringsHashtable = AWLocal.loadLocalizedJavaStrings(component);
                    if (localizedStringsHashtable != null) {
                        AW2DVector localizedStringsCopy = (AW2DVector)_localizedStrings.clone();
                        Iterator keyEnumerator = localizedStringsHashtable.keySet().iterator();
                        while (keyEnumerator.hasNext()) {
                            String currentStringId = (String)keyEnumerator.next();
                            // Note: an application might choose to merge awl strings and java strings into one single string
                            // file, so we need to check for the integer key. all the awl strings will start with a letter such as
                            // "a001".
                            char firstCharacter = currentStringId.charAt(0);
                            if (firstCharacter >= '0' && firstCharacter <= '9') {
                                String currentLocalizedString = (String)localizedStringsHashtable.get(currentStringId);
                                localizedStringsCopy.setElementAt(currentLocalizedString, resourceManagerIndex, Integer.parseInt(currentStringId));
                            }
                        }
                        localizedString = (String)localizedStringsCopy.elementAt(resourceManagerIndex, stringId);
                        if (localizedString == null) {
                            if (AWLocal.IsDebuggingEnabled) {
                                localizedString =
                                    AWUtil.addEmbeddedContextForDefaultString(stringId, originalString, component.namePath());
                            }
                            else {
                                localizedString = originalString;
                            }
                            localizedStringsCopy.setElementAt(localizedString, resourceManagerIndex, stringId);
                        }
                        _localizedStrings = localizedStringsCopy;
                    }
                    else {
                        localizedString = originalString;
                    }
                }
            }
        }
        return localizedString;
    }

    public void logReloadString (String s)
    {
        System.out.println(s);
    }

    ///////////////////////
    // Validation Support
    ///////////////////////
    // todo: scope errors by template for multi-template case

    public void resetValidationData ()
    {
        _bindingErrorList = null;

        _unsupportedBindingDefinitions = null;
        _missingSupportedBindingDefinitions = null;
        _invalidBindingAlternates = null;
        _mismatchedBindingAlternates = null;

        _templateParsingErrors = null;
        _componentApiErrorCount = 0;
    }

    public List bindingErrorList ()
    {
        return _bindingErrorList;
    }

    private List _bindingErrorList ()
    {
        if (_bindingErrorList == null) {
            _bindingErrorList = ListUtil.list();
        }
        return _bindingErrorList;
    }

    public boolean hasValidationErrors ()
    {
        return (_componentApiErrorCount > 0) || (validationErrorCount() > 0);
    }

    public Map empiricalApiTable ()
    {
        return _empiricalApiTable;
    }

    public int validationErrorCount ()
    {
        return (_bindingErrorList == null) ? 0 : _bindingErrorList.size();
    }

    public int componentApiErrorCount ()
    {
        return _componentApiErrorCount;
    }

    private static ComponentReferenceLocation findComponentReferenceLocation (
        List list, String componentName)
    {
        ComponentReferenceLocation ref = null;
        boolean found = false;
        for (int i=0,size=list.size(); i<size && !found; i++) {
            ref = (ComponentReferenceLocation)list.get(i);
            if (ref.componentName().equals(componentName)) {
                found = true;
            }
        }

        if (found == false) {
            ref = null;
        }
        return ref;
    }

    /**
        Track the components that reference this component.
        @param component
    */
    synchronized public void addReferencedBy (AWComponent component)
    {
        if (_referencedBy == null) {
            _referencedBy = ListUtil.list();
            _referencedByLocations = ListUtil.list();
        }
        // Todo: currently no way to clear this out if
        // referencing component is modified in rapid turn around
        String componentName = component.name();
        int pos = _referencedBy.indexOf(componentName);
        if (pos == -1) {
            _referencedBy.add(componentName);
            _referencedByLocations.add(new ComponentReferenceLocation(componentName, currentTemplateLocation(component)));
        }
        else {
            ComponentReferenceLocation location = (ComponentReferenceLocation)_referencedByLocations.get(pos);
            location.addLineNumber(currentTemplateLocation(component));
        }
    }

    public List referencedBy ()
    {
        return _referencedBy;
    }
    public List referencedByLocations ()
    {
        return _referencedByLocations;
    }

    public void setIsPageLevel (boolean flag)
    {
        _isPageLevel = flag;
    }

    public boolean isPageLevel ()
    {
        return _isPageLevel;
    }

    //////////////
    // AWApi errors
    //////////////

    public List unsupportedBindingDefinitions ()
    {
        return _unsupportedBindingDefinitions;
    }

    public List missingSupportedBindingDefinitions ()
    {
        return _missingSupportedBindingDefinitions;
    }

    public void addInvalidComponentApiBindingDefinition (AWValidationContext validationContext, String key, int type)
    {
        if (type == UnsupportedBindingDefinition) {
            // binding found in AWApi but not found in supported binding list
            if (_unsupportedBindingDefinitions == null) {
                _unsupportedBindingDefinitions = ListUtil.list();
            }
            _unsupportedBindingDefinitions.add(key);
            _componentApiErrorCount++;
        }
        else if (type == MissingSupportedBindingDefinition) {
            // binding found in supported binding list but not found in AWApi
            if (_missingSupportedBindingDefinitions == null) {
                _missingSupportedBindingDefinitions = ListUtil.list();
            }
            _missingSupportedBindingDefinitions.add(key);
            _componentApiErrorCount++;
        }
    }

    public List invalidComponentBindingApiAlternates ()
    {
        return _invalidBindingAlternates;
    }

    public List mismatchedComponentBindingApiAlternates ()
    {
        return _mismatchedBindingAlternates;
    }

    /**
        Called when a binding Api has an alternate specified and the
        alternate does not have an Api defined.
        @param validationContext
        @param bindingName           binding on which an alternate is defined
        @param alternateBindingName  alternate binding name
    */
    public void addInvalidComponentApiAlternate (AWValidationContext validationContext, String bindingName, String alternateBindingName)
    {
        if (_invalidBindingAlternates == null) {
            _invalidBindingAlternates = ListUtil.list();
        }
        String[] bindingPair = {bindingName, alternateBindingName};
        _invalidBindingAlternates.add(bindingPair);
        _componentApiErrorCount++;
    }

    /**
        Called when a binding Api has an alternate specified and the
        alternate binding Api does not have the same alternate list.
        @param validationContext
        @param bindingName           binding on which an alternate is defined
        @param alternateBindingName  alternate binding name
    */
    public void addMismatchedComponentApiAlternates (AWValidationContext validationContext, String bindingName, String alternateBindingName)
    {
        if (_mismatchedBindingAlternates == null) {
            _mismatchedBindingAlternates = ListUtil.list();
        }
        String[] bindingPair = {bindingName, alternateBindingName};
        _mismatchedBindingAlternates.add(bindingPair);
        _componentApiErrorCount++;
    }

    /**
        Template Parsing errors
    */
    public List templateParsingErrors ()
    {
        return _templateParsingErrors;
    }

    /**
        Strict tag naming is enabled and a tag was found whose name starts with
        an uppercase character and no corresponding component was found.

        @param validationContext
        @param tagName
        @param templateName
        @param currentLine
    */
    public void addUnknownTag (AWValidationContext validationContext,
                               String              tagName,
                               String              templateName,
                               int                 currentLine)
    {
        if (_templateParsingErrors == null) {
            _templateParsingErrors = ListUtil.list();
        }

        _templateParsingErrors.add("Unable to find definition for <" + tagName + "> found in "
                                   +templateName+":"+currentLine+")" + " Rendered literally to HTML.");
        _componentApiErrorCount++;
    }

    //////////////
    // runtime validation errors
    //////////////

    // Used but not defined in the AWApi (and pass through not allowed)
    public void addUnsupportedBinding (AWValidationContext validationContext, AWComponent component, String bindingName)
    {
        List bindingErrorList = _bindingErrorList();
        if (!(AWBindingNames.namePrefix.equals(bindingName) || AWBindingNames.awname.equals(bindingName))) {
            bindingErrorList.add("Unsupported binding. " +
                                  formatBinding(component, bindingName) + "(" + formatComponent(component) + ")");
        }
    }

    // right side of binding is invalid
    public void addInvalidValueForBinding (AWValidationContext validationContext, AWComponent component, String bindingName, String errorMessage)
    {
        List bindingErrorList = _bindingErrorList();
        bindingErrorList.add("Invalid value for binding. " +
                              formatBinding(component, bindingName) + "(" + formatComponent(component) + ")" +
                              " " + errorMessage);
    }

    public void addMissingRequiredBinding (AWValidationContext validationContext, AWComponent component, String bindingName)
    {
        List bindingErrorList = _bindingErrorList();
        bindingErrorList.add("Missing required binding. " +
                              formatBinding(component, bindingName) + "(" + formatComponent(component) + ")");
    }

    protected void addMissingRequiredNamedTemplate (AWComponent component, String templateName)
    {
        List bindingErrorList = _bindingErrorList();
        AWComponentReference componentReference = (AWComponentReference)component.currentTemplateElement();
        String referencedComponentName = componentReference.componentDefinition().componentName();
        bindingErrorList.add("Reference to: " + referencedComponentName +
                " is missing required &lt;AWContent templateName=\"" + templateName +
                "\"&gt; ( in: " + formatComponent(component) + ")");
    }

    protected void addMissingNamedTemplateDeclaration (AWComponent component, String name)
    {
        List bindingErrorList = _bindingErrorList();
        bindingErrorList.add("NamedTemplate is orphaned (no declaration) &lt;AWContent templateName=\"" + name +
                "\"&gt; ( in: " + formatComponent(component) + ")");
    }

    /**
        AWApi not defined for element so collect all bindings
        @param bindingName   bindind used
        @param parent        component in which the binding is used
    */
    public void addEmpiricalBinding (String bindingName, AWComponent parent)
    {
        if (_empiricalApiTable == null) {
            _empiricalApiTable = MapUtil.map();
        }

        EmpiricalApiData apiData = (EmpiricalApiData)_empiricalApiTable.get(bindingName);
        if (apiData == null) {
            apiData = new EmpiricalApiData();
            apiData._bindingName = bindingName;
            _empiricalApiTable.put(bindingName,apiData);
        }

        List referenceList = apiData._referencedBy;
        List locationList = apiData._locationInReference;
        if (referenceList == null) {
            referenceList = ListUtil.list();
            locationList = ListUtil.list();
            apiData._referencedBy = referenceList;
            apiData._locationInReference = locationList;
        }

        int pos = referenceList.indexOf(parent.name());
        if (pos == -1) {
            referenceList.add(parent.name());
            locationList.add(currentTemplateLocation(parent));
        }
        // else more than one so just ignore ...
    }

    //////////////
    // warnings
    //////////////
    public void addPassThroughBinding (AWValidationContext validationContext, AWComponent component, String bindingName)
    {
        // todo: log
//        System.out.println(" iii pass through binding found: "+formatComponent(component) +
//                           " " + formatBinding(bindingName));
    }

    ///////////////////
    // AWApi reference
    ///////////////////
    public void addBindingReference (AWComponent component, String bindingName)
    {
        // bindingName --> component name : line#
        if (_bindingReferenceTable == null) {
            _bindingReferenceTable = MapUtil.map();
        }

        List componentReferenceList = (List)_bindingReferenceTable.get(bindingName);
        if (componentReferenceList == null) {
            componentReferenceList = ListUtil.list();
            _bindingReferenceTable.put(bindingName,componentReferenceList);
        }
        String componentName = component.name();
        ComponentReferenceLocation componentRef = findComponentReferenceLocation(componentReferenceList, componentName);
        if (componentRef == null) {
            componentRef = new ComponentReferenceLocation(componentName,currentTemplateLocation(component));
            componentReferenceList.add(componentRef);
        }
        else {
            // already referenced once so just add line number
            componentRef.addLineNumber(currentTemplateLocation(component));
        }
    }

    //////////////
    // logging
    //////////////
    private String currentTemplateLocation (AWComponent component)
    {
        String sReturn = null;
        AWBaseElement element = (AWBaseElement)component.currentTemplateElement();
        if (element != null) {
            sReturn = String.valueOf(element.lineNumber());
        }
        return sReturn;
    }

    private String formatComponent (AWComponent component)
    {
        FormatBuffer ret = new FormatBuffer();
        AWBaseElement element = (AWBaseElement)component.currentTemplateElement();
        ret.append(component.name());
        if (element != null) {
            ret.append(":");
            ret.append(element.lineNumber());
        }
        return ret.toString();
    }

    private String formatBinding (AWComponent component, String bindingName)
    {
        AWBindableElement element = (AWBindableElement)component.currentTemplateElement();
        if (element == null) {
            return Fmt.S("binding: '%s'", component.name(), bindingName);
        }
        else {
            return Fmt.S(" tag: '%s' binding: '%s'",
                         element.tagName(),
                         bindingName);
        }
    }

    /////////
    // debug
    /////////
    private void printList (FormatBuffer output, List list)
    {
        if (list == null) {
            output.append("\n");
            return;
        }

        for (int i = 0, listsize = list.size(); i < listsize; i++) {
            String key = (String)list.get(i);
            output.append("\t");
            output.append(key);
            output.append("\n");
        }
    }

    public String printComponentApiErrors ()
    {
        FormatBuffer sbReturn = new FormatBuffer();
        if (_unsupportedBindingDefinitions != null) {
            sbReturn.append("Unsupported bindings.  Binding found in AWApi, but not defined in supported binding list in the component.\n");
            printList(sbReturn, _unsupportedBindingDefinitions);
        }

        if (_missingSupportedBindingDefinitions != null) {
            sbReturn.append("Missing supported bindings.  Binding defined in supported binding list in the component but not found in AWApi.\n");
            printList(sbReturn, _missingSupportedBindingDefinitions);
        }

        if (_invalidBindingAlternates != null || _mismatchedBindingAlternates != null) {
            sbReturn.append("Invalid alternates defined for bindings\n");

            if (_invalidBindingAlternates != null) {
                for (int i = 0, size = _invalidBindingAlternates.size(); i < size; i++) {
                    String[] bindingError = (String[])_invalidBindingAlternates.get(i);
                    sbReturn.append("\tUnable to find definition for binding '" + bindingError[1] +
                                    "' specified in alternates for bindings: '" + bindingError[0] + "'.\n");
                }
            }
            if (_mismatchedBindingAlternates != null) {
                for (int i = 0, size = _mismatchedBindingAlternates.size(); i < size; i++) {
                    String[] bindingError = (String[])_mismatchedBindingAlternates.get(i);
                    sbReturn.append("\tMismatched alternates defined for bindings: '" + bindingError[0] +
                                    "','" + bindingError[1] + "'\n");
                }
            }
        }

        if (_templateParsingErrors != null) {
            sbReturn.append("Unrecognized tags found.  Rendered directly to output.\n");
            printList(sbReturn, _templateParsingErrors);
        }

        return sbReturn.toString();
    }

    /**
        Weird method to check if the class has overriden templateName() to return a different
        template than <ClassName>.awl.  (e.g. AWVTextField).  In this case, we don't force full
        AWApi documentation.
    */
    public boolean usesOwnTemplate (AWComponent component)
    {
        return component.templateName().equals(computeTemplateName(_componentClass));
    }
}
