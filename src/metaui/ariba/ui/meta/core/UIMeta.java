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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/UIMeta.java#60 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWBindingDictionary;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWValidationContext;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWStringLocalizer;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWResourceManagerDictionary;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.validation.AWVIdentifierFormatter;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.ui.meta.annotations.Localized;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldValue;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.TimeZone;

public class UIMeta extends ObjectMeta
{
    public final static String KeyOperation = "operation";
    public final static String KeyModule = "module";
    public final static String KeyLayout = "layout";
    public final static String KeyArea = "area";
    public final static String KeyEditing = "editing";
    public final static String KeyAfter = "after";
    public final static String KeyHidden = "hidden";
    public final static String KeyLabel = "label";
    public final static String KeyComponentName = "component";
    public final static String KeyBindings = "bindings";
    public final static String KeyHomePage = "homePage";
    public final static String KeyZonePath = "zonePath";
    public final static String PropFieldsByZone = "fieldsByZone";
    public final static String PropActionsByCategory = "actionsByCategory";
    public final static String PropActionCategories = "actionCategories";
    public final static String PropFieldPropertyList = "fieldPropertyList";
    public final static String PropLayoutsByZone = "layoutsByZone";

    static UIMeta _Instance;

    protected Map <AWResource, RuleSet> _loadedResources = new HashMap();
    protected static List<String> _NavModuleClasses = new ArrayList();
    protected static Set<String> _LocalizedClasses = new HashSet();

    public static UIMeta getInstance ()
    {
        if (_Instance == null) {
            _Instance = new UIMeta();
        }
        return _Instance;
    }

    static protected void initialize () {}
    
    static {
        AWJarWalker.registerAnnotationListener(NavModuleClass.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _NavModuleClasses.add(className);
                        _LocalizedClasses.add(className);
                    }
                });
        AWJarWalker.registerAnnotationListener(Localized.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _LocalizedClasses.add(className);
                    }
                });        
    }

    public UIMeta()
    {
        beginRuleSet(UIMeta.class.getName());
        try {
            registerKeyInitObserver(KeyClass, new FileMetaProvider());

            // These keys define scopes for their properties
            // defineKeyAsPropertyScope(KeyArea);
            defineKeyAsPropertyScope(KeyLayout);
            defineKeyAsPropertyScope(KeyModule);

            // Default rule for converting field name to label
            registerDefaultLabelGeneratorForKey(KeyClass);
            registerDefaultLabelGeneratorForKey(KeyField);
            registerDefaultLabelGeneratorForKey(KeyLayout);
            registerDefaultLabelGeneratorForKey(KeyModule);
            registerDefaultLabelGeneratorForKey(KeyAction);
            registerDefaultLabelGeneratorForKey(KeyActionCategory);

            // policies for chaining certain well known properties
            registerPropertyMerger(KeyArea, PropertyMerger_DeclareList);
            registerPropertyMerger(KeyLayout, PropertyMerger_DeclareList);
            registerPropertyMerger(KeyModule, PropertyMerger_DeclareList);

            mirrorPropertyToContext(KeyEditing, KeyEditing);
            mirrorPropertyToContext(KeyLayout, KeyLayout);
            mirrorPropertyToContext(KeyComponentName, KeyComponentName);

            registerPropertyMerger(KeyEditing, new PropertyMerger_And());

            registerValueTransformerForKey("requestContext", Transformer_KeyPresent);
            registerValueTransformerForKey("displayGroup", Transformer_KeyPresent);

            // define operation hierarchy
            keyData(KeyOperation).setParent("view", "inspect");
            keyData(KeyOperation).setParent("print", "view");
            keyData(KeyOperation).setParent("edit", "inspect");
            keyData(KeyOperation).setParent("search", "inspect");
            keyData(KeyOperation).setParent("keywordSearch", "search");
            keyData(KeyOperation).setParent("textSearch", "keywordSearch");

            registerStaticallyResolvable(PropFieldsByZone, new PropertyValue.StaticallyResolvable() {
                    public Object evaluate(Context context) {
                        Map m = ((UIMeta)context.meta()).itemNamesByZones(context, KeyField, zones(context));
                        String zonePath = zonePath(context);
                        if (zonePath != null) {
                            m = (Map)FieldValue.getFieldValue(m, zonePath);
                            if (m == null) m = Collections.EMPTY_MAP;
                        }
                        return m;
                    }
                }, KeyClass);

            registerStaticallyResolvable(PropFieldPropertyList, new PropertyValue.StaticallyResolvable() {
                    public Object evaluate(Context context) {
                        return ((UIMeta)context.meta()).fieldList(context);
                    }
                }, KeyClass);

            registerStaticallyResolvable(PropLayoutsByZone, new PropertyValue.StaticallyResolvable() {
                    public Object evaluate(Context context) {
                        return ((UIMeta)context.meta()).itemNamesByZones(context, KeyLayout, zones(context));
                    }
                }, KeyLayout);

            /* actions by category caching visibility.  need to be fixed before reeneabling this  
            // Register cached derived properties for actionCategories (list) and actionsByCategory (map)
            for (String key : new String[] { KeyModule, KeyLayout, KeyClass, KeyField }) {
                registerStaticallyResolvable(PropActionsByCategory, new PropertyValue.StaticallyResolvable() {
                        public Object evaluate(Context context) {
                            Map<String, List<ItemProperties>> result = MapUtil.map();
                            ((UIMeta)context.meta()).actionsByCategory(context, result, ActionZones);
                            return result;
                        }
                    }, key);
                registerStaticallyResolvable(PropActionCategories, new PropertyValue.StaticallyResolvable() {
                        public Object evaluate(Context context) {
                            return ((UIMeta)context.meta()).itemList(context, KeyActionCategory, ActionZones);
                        }
                    }, key);
            }
            */

            PropertyValue.StaticallyResolvable dyn = new PropertyValue.StaticallyResolvable() {
                    public Object evaluate(Context context) {
                        return bindingDictionaryForValueMap((Map)context.propertyForKey("bindings"));
                    }
                };
            registerStaticallyResolvable("bindingsDictionary", dyn, KeyField);
            registerStaticallyResolvable("bindingsDictionary", dyn, KeyLayout);
            registerStaticallyResolvable("bindingsDictionary", dyn, KeyClass);
            registerStaticallyResolvable("bindingsDictionary", dyn, KeyModule);
        } finally {
            endRuleSet();
        }

        if (AWConcreteServerApplication.IsRapidTurnaroundEnabled) {
            AWPage.registerLifecycleListener(new AWPage.LifecycleListener() {
                // Listen for new page activations and check for rule file changes
                public void pageWillRender(AWPage page)
                {
                    AWValidationContext validationContext = page.validationContext();
                    validationContext.clearGeneralErrors("MetaUI");
                    try {
                        checkRuleFileChanges(false);
                    } catch (RuleLoadingException exception) {
                        validationContext.addGeneralError("MetaUI", exception.getMessage(), exception);
                    }
                }

                public void pageWillAwake (AWPage page) { }
                public void pageWillSleep (AWPage page) { }
            });
        }
    }

    public List<String> zones (Context context)
    {
        List<String> zones = (List)context.propertyForKey("zones");
        return (zones == null) ? Arrays.asList(ZoneMain) : zones;
    }

    public String zonePath (Context context)
    {
        String zonePath = null;
        if (context.values().get(KeyLayout) != null) {
            context.push();
            context.setScopeKey(KeyLayout);
            zonePath = (String)context.propertyForKey(KeyZonePath);
            context.pop();
        }
        return zonePath;
    }

    public Context newContext()
    {
        return new UIContext(this);
    }

    public static class UIContext extends ObjectMetaContext
    {
        AWRequestContext _requestContext;
        AWResourceManager _resourceManager;
        TimeZone _timeZone;

        public UIContext(UIMeta meta)
        {
            super(meta);
        }

        public AWRequestContext requestContext()
        {
            return _requestContext;
        }

        public void setRequestContext (AWRequestContext requestContext)
        {
            _requestContext = requestContext;
            _resourceManager = null;
            _timeZone = null;
        }

        public AWComponent getComponent ()
        {
            return _requestContext.getCurrentComponent();
        }

        public AWResourceManager resourceManager ()
        {
            if (_resourceManager == null) {
                HttpSession existingHttpSession = (_requestContext != null) ? _requestContext.existingHttpSession() : null;
                if (existingHttpSession != null) {
                    _resourceManager = AWSession.session(existingHttpSession).resourceManager();
                }
                if (_resourceManager == null) {
                    _resourceManager = AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
                }
            }
            return _resourceManager;
        }

        public Locale locale ()
        {
            return resourceManager().locale();
        }

        public TimeZone timezone () {
            if (_timeZone == null) {
                HttpSession existingHttpSession = (_requestContext != null) ? _requestContext.existingHttpSession() : null;
                if (existingHttpSession != null) {
                    _timeZone = AWSession.session(existingHttpSession).clientTimeZone();
                }
                if (_timeZone == null) {
                    _timeZone = TimeZone.getDefault();
                }
            }
            return _timeZone;            
        }

        public UIMeta uiMeta ()
        {
            return (UIMeta)_meta;
        }
    }

    public boolean loadRuleFile (String filename, boolean required, int rank)
    {
        AWResourceManager resourceManager = AWConcreteServerApplication.sharedInstance().resourceManager();
        AWResource resource = resourceManager.packageResourceNamed(filename);
        Assert.that(!required || resource != null, "Rule file not found in resource search path: %s", filename);
        if (resource != null) {
            beginRuleSet(rank, resource.relativePath());
            _loadRuleFile(resource);
            return true;
        }
        return false;
    }

    public Map<AWResource, Meta.RuleSet> loadedRuleSets ()
    {
        return _loadedResources;
    }

    protected void _loadRuleFile (AWResource resource)
    {
        try {
            String resourceName = resource.name();
            boolean editable = resourceName.endsWith("rules.oss");
            _loadRules(resource.name(), resource.inputStream(), editable);
        } finally {
            // Need to set *any* object on resource to get it's hasChanged() timestamp set
            resource.setObject(Boolean.TRUE);
        }

        _loadedResources.put(resource, endRuleSet());
    }

    private long _lastCheckMillis = 0;
    public void checkRuleFileChanges (boolean force)
    {
        // Only stat every 2 seconds
        long currentTimeMillis = System.currentTimeMillis();
        if (force || currentTimeMillis - _lastCheckMillis > 2000) {
            _lastCheckMillis = currentTimeMillis;
            for (AWResource resource : _loadedResources.keySet()) {
                if (resource.hasChanged()) {
                    reloadRuleFile(resource);
                }
            }
        }
    }
    
    public void reloadRuleFile (AWResource resource)
    {
        Meta.RuleSet ruleSet = _loadedResources.get(resource);
        Assert.that(ruleSet != null, "Attempt to reload not previously loaded resource");
        Log.meta.debug("Reloading modified rule file: %s", resource.name());
        beginReplacementRuleSet(ruleSet);
        _loadRuleFile(resource);
        ruleSet.disableRules();
        invalidateRules();
    }

    static class _DefaultLabelGenerator implements PropertyValue.StaticallyResolvable
    {
        String _key;
        public _DefaultLabelGenerator (String key) { _key = key; }

        public Object evaluate(Context context) {
            Object fieldName = context.values().get(_key);
            return (fieldName != null && fieldName instanceof String)
                    ? defaultLabelForIdentifier((String)fieldName)
                    : null;
        }
    }

    static public PropertyValue.Dynamic defaultLabelGeneratorForKey (final String key)
    {
        return new _DefaultLabelGenerator(key);
    }

    public static String defaultLabelForIdentifier (String fieldName)
    {
        int lastDot = fieldName.lastIndexOf('.');
        if (lastDot != -1 && lastDot != fieldName.length() -1) fieldName = fieldName.substring(lastDot+1);
        return AWVIdentifierFormatter.decamelize(fieldName);
    }

    protected void registerDerivedValue (String propKey, PropertyValue.Dynamic dynamicValue,
                                         String contextKey, String contextValue)
    {
        Map m = new HashMap();
        m.put(propKey, dynamicValue);
        addRule(new Rule(Arrays.asList(new Rule.Selector(contextKey, contextValue)),
                          m, SystemRulePriority));
    }

    public void registerStaticallyResolvable (String propKey,
                                              PropertyValue.StaticallyResolvable dynamicValue,
                                              String contextKey)
    {
        registerDerivedValue (propKey, new PropertyValue.StaticDynamicWrapper(dynamicValue),
                                         contextKey, KeyAny);
    }

    public void registerDefaultLabelGeneratorForKey (String key)
    {
        registerDerivedValue(KeyLabel, new LocalizedLabelString(), key, KeyAny);
    }

    public List<ItemProperties> fieldList (Context context)
    {
        return itemList(context, KeyField, ZonesTLRB);
    }

    public Map<String, Object> fieldsByZones (Context context)
    {
        return itemsByZones(context, KeyField, ZonesTLRB);
    }

    public Map<String, Object> itemNamesByZones (Context context, String key, List<String> zones)
    {
        Map<String, Object>itemsByZones = itemsByZones(context, key, zones.toArray(new String[zones.size()]));
        return mapItemPropsToNames(itemsByZones);
    }

    private Map<String, Object> mapItemPropsToNames(Map<String, Object> itemsByZones)
    {
        Map<String, Object> namesByZones = new HashMap();
        for (Map.Entry<String, Object> e : itemsByZones.entrySet()) {
            Object value = e.getValue();
            if (value instanceof List) {
                List<String> names = new ArrayList();
                for (Object item : (List)value) {
                    if (item instanceof ItemProperties) names.add(((ItemProperties)item)._name);
                }
                namesByZones.put(e.getKey(), names);
            }
            else if (value instanceof Map) {
                namesByZones.put(e.getKey(), mapItemPropsToNames((Map<String, Object>)value));
            }
        }
        return namesByZones;
    }

    private static String RootPredecessorKey = "_root";
    public static final String ZoneMain = "zMain";
    public static final String ZoneTop = "zTop";
    public static final String ZoneLeft = "zLeft";
    public static final String ZoneRight = "zRight";
    public static final String ZoneBottom = "zBottom";
    public static final String ZoneDetail = "zDetail";

    public static String[] ZonesTLRB = {ZoneTop, ZoneLeft, ZoneRight, ZoneBottom };
    public static String[] ZonesMTLRB = {ZoneMain, ZoneTop, ZoneLeft, ZoneRight, ZoneBottom };
    public static String[] ZonesDetail = {ZoneDetail};

    public Map<String, List> predecessorMap (Context context, String key, final String defaultPredecessor)
    {
        List<ItemProperties> fieldInfos = itemProperties(context, key, false);
        Map<String, List> predecessors = AWUtil.groupBy(fieldInfos,
                new AWUtil.ValueMapper() {
                    public Object valueForObject(Object o) {
                        Object pred = ((ItemProperties)o).properties().get(KeyAfter);
                        return pred != null ? pred : defaultPredecessor;
                    }
                });
        return predecessors;
    }

    public List<ItemProperties> itemList (Context context, String key, String[] zones)
    {
        Map<String, List> predecessors = predecessorMap(context, key, zones[0]);
        List<ItemProperties> result = new ArrayList();
        for (String zone : zones) {
            accumulatePrecessors(predecessors, zone, result);
        }
        return result;
    }

    boolean isZoneReference (String key)
    {
        // keys of the form "z<Name>" and "foo.bar.z<Name>" are considered zone keys
        int lastDot = key.lastIndexOf(".");
        String suffix = (lastDot == -1) ? key : key.substring(lastDot+1);
        return (suffix.length() > 1) && (suffix.charAt(0) == 'z') && (Character.isUpperCase(suffix.charAt(1)));
    }

    public Map<String, Object> itemsByZones (Context context, String property, String[] zones)
    {
        Map<String, List> predecessors = predecessorMap(context, property, zones[0]);
        Map<String, Object> byZone = new HashMap();
/*
        for (String zone : zones) {
            List<ItemProperties> list = new ArrayList();
            accumulatePrecessors(predecessors, zone, list);
            byZone.put(zone, list);
        }
*/
        for (Map.Entry<String, List> e : predecessors.entrySet()) {
            String zone = e.getKey();
            if (isZoneReference(zone)) {
                List<ItemProperties> list = new ArrayList();
                accumulatePrecessors(predecessors, zone, list);

                // use field value for assignment so keys of form "a.b.c" will
                // go in nested Maps
                FieldValue.setFieldValue(byZone, zone, list);
            }
        }
        return byZone;
    }


    // recursive decent of predecessor tree...
    void accumulatePrecessors (Map<String, List> predecessors, String key, List result)
    {
        List<ItemProperties> items = predecessors.get(key);
        if (items == null) return;
        // Rank sort at this level
        Collections.sort(items, new Comparator<ItemProperties>() {
            public int compare(ItemProperties o1, ItemProperties o2) {
                Integer r1 = (Integer)o1.properties().get(KeyRank);
                Integer r2 = (Integer)o2.properties().get(KeyRank);
                if (r1 == null) r1 = 100;
                if (r2 == null) r2 = 100;
                return (r1 == r2) ? 0
                        : (r1 == null) ? 1
                            : (r2 == null) ? -1
                                : (r1 - r2);

            }
        });
        // add each field and those inserted as its predecessor
        for (ItemProperties item : items) {
            if (!item.isHidden()) result.add(item);
            accumulatePrecessors(predecessors, item._name, result);
        }
    }

    // Called by Parser to handle decls like "zLeft => lastName#required"
    public Rule addPredecessorRule(String itemName, List<Rule.Selector>contextPreds,
                                   String predecessor, Object traits, int lineNumber)
    {
        if (predecessor == null && traits == null) return null;

        // Determine key being used.  If selector scope key is "class" use "field"
        String key = scopeKeyForSelector(contextPreds);
        if (key == null || key.equals(KeyClass)) key = KeyField;
        List<Rule.Selector> selector = new ArrayList(contextPreds);
        selector.add(new Rule.Selector(key, itemName));
        Map props = MapUtil.map();
        if (predecessor != null) props.put(KeyAfter, predecessor);
        if (traits != null) props.put(KeyTrait, traits);
        Rule rule = new Rule(selector, props, 0, lineNumber);
        addRule(rule);
        return rule;
    }

    public List<String>flattenVisible (Map<String, List> fieldsByZones, String[] zoneList,
                                       String key, Context context)
    {
        List<String>result = ListUtil.list();
        if (fieldsByZones != null) {
            for (String zone : zoneList) {
                List<String>fields = fieldsByZones.get(zone);
                if (fields == null) continue;
                for (String field : fields) {
                    context.push();
                    context.set(key, field);
                    if (context.booleanPropertyForKey(KeyVisible, false)) result.add(field);
                    context.pop();
                }
            }
        }
        return result;
    }


    public String displayKeyForClass (String className)
    {
        // performance: should use registerDerivedValue("...", new Context.StaticDynamicWrapper
        // to get cached resolution here...
        Context context = newContext();
        context.set(KeyLayout, "LabelField");
        context.set(KeyClass, className);
        List<ItemProperties> fields = itemProperties(context, KeyField, true);

        return fields.isEmpty() ? "toString" : fields.get(0).name();
    }


    public static String[] ModuleActionZones = { "zNav", "zGlobal" };
    public static String[] ActionZones = { "zGlobal", "zMain", "zGeneral" };

    public enum ModuleMatch { AsHome, AsShow, NoMatch };

    public static class ModuleProperties extends ItemProperties
    {
        List<String> _homeForTypes;
        List<String> _usableForTypes;
        List<String> _allTypes;


        public ModuleProperties(String name, Map properties, boolean hidden,
                                List<String> homeForTypes, List<String> usableForTypes)
        {
            super(name, properties, hidden);
            _homeForTypes = homeForTypes;
            _usableForTypes = usableForTypes;
            _allTypes = ListUtil.copyList(homeForTypes);
            _allTypes.addAll(_usableForTypes);
        }

        public List<String> getHomeForTypes()
        {
            return _homeForTypes;
        }

        public List<String> getUsableForTypes()
        {
            return _usableForTypes;
        }

        public List<String> getAllTypes()
        {
            return _allTypes;
        }

        ModuleMatch matches (Map <String, String> matchContext)
        {
            String moduleName = matchContext.get(KeyModule);
            if (moduleName != null && moduleName.equals(name())) {
                return ModuleMatch.AsHome;
            }

            String homePage = matchContext.get(KeyHomePage);
            if (homePage != null && homePage.equals(properties().get(KeyHomePage))) {
                return ModuleMatch.AsHome;
            }

            String className = matchContext.get(KeyClass);
            return (className == null) ? ModuleMatch.NoMatch
                    : (_homeForTypes.contains(className)
                            ? ModuleMatch.AsHome
                            : (_usableForTypes.contains(className)
                                ? ModuleMatch.AsShow : ModuleMatch.NoMatch));
        }
    }

    public static class ModuleInfo
    {
        public List<ModuleProperties> modules;
        public List<String> moduleNames;
        public List<String> classNames;
        public List<ItemProperties> actionCategories;
        public Map<String, List<ItemProperties>> actionsByCategory;
    }

    public ModuleInfo computeModuleInfo (Context context, boolean checkVisibility)
    {
        ensureDidDeclareModules();
        ModuleInfo moduleInfo = new ModuleInfo();
        // List<ItemProperties> items = itemList(context, KeyModule, ActionZones);
        moduleInfo.modules = ListUtil.list();
        Set<String> classesSet = new HashSet();
        List<ItemProperties> allModuleProps = itemList(context, KeyModule, ActionZones);
        moduleInfo.moduleNames = ListUtil.list();

        for (ItemProperties module : allModuleProps) {
            context.push();
            context.set(KeyModule, module.name());

            if (checkVisibility && !context.booleanPropertyForKey(KeyVisible, true)) {
            	context.pop();
            	continue;
            }

            moduleInfo.moduleNames.add(module.name());

            context.push();
            context.set("homeForClasses", true);
            List<String> homeClasses = itemNames(context, KeyClass);
            context.pop();

            context.push();
            context.set("showsClasses", true);
            List<String> showsClasses = itemNames(context, KeyClass);
            context.pop();

            moduleInfo.modules.add(new ModuleProperties(module.name(), context.allProperties(), false,
                    homeClasses, showsClasses));

            classesSet.addAll(homeClasses);
            classesSet.addAll(showsClasses);
            context.pop();
        }
        moduleInfo.classNames = ListUtil.list();
        moduleInfo.classNames.addAll(classesSet);

        context.push();
        context.set(KeyModule, moduleInfo.moduleNames);
        context.set(KeyClass, moduleInfo.classNames);
        moduleInfo.actionsByCategory = MapUtil.map();
        moduleInfo.actionCategories = actionsByCategory(context, moduleInfo.actionsByCategory, ModuleActionZones);
        context.pop();

        return moduleInfo;
    }

    // Implemented by pages to express what they're displaying
    public interface NavContextProvider
    {
        // Should return map of the form [class:example.app.SomeClass]
        Map<String, String> currentNavContext ();
    }

    // get nav context for current page
    public Map<String, String> contextForPage (AWComponent pageComponent)
    {
        if (pageComponent instanceof NavContextProvider) {
            Map<String, String> navContext = ((NavContextProvider)pageComponent).currentNavContext();
            if (navContext != null) return navContext;
        }
        return Collections.singletonMap(UIMeta.KeyHomePage, pageComponent.componentDefinition().componentName());
    }

    // find which of the given modules are the best fit for the given page
    public ModuleProperties matchForPage (List<ModuleProperties> modules,
                                          AWComponent pageComponent,
                                          ModuleProperties currentlySelected)
    {
        Map<String, String> navContext = contextForPage(pageComponent);
        ModuleProperties homeMatch = null, showMatch = null;
        for (ModuleProperties m : modules) {
            ModuleMatch match = m.matches(navContext);
            if (match == ModuleMatch.AsHome
                    && (homeMatch == null || m == currentlySelected)) {
                homeMatch = m;
            }
            if (match == ModuleMatch.AsShow
                    && (showMatch == null || m == currentlySelected)) {
                showMatch = m;
            }
        }
        return (homeMatch != null) ? homeMatch
                : ((showMatch != null) ? showMatch
                    : ((currentlySelected != null) ? currentlySelected
                            : (modules.isEmpty()) ? null : modules.get(0)));
    }

    public List<String> actionCategories (Context context)
    {
        context.push();
        // context.set(KeyAction, KeyAny);
        context.set(KeyDeclare, KeyAction);
        List <String> categoryNames = context.listPropertyForKey(KeyActionCategory);
        context.pop();
        return categoryNames;
    }

    // caller must push/pop!
    public List<ItemProperties> actionsByCategory(Context context, Map<String, List<ItemProperties>> result, String[] zones)
    {
        List actionCategories = itemList(context, KeyActionCategory, zones);
        List<String> catNames = AWUtil.collect(actionCategories, new AWUtil.ValueMapper () {
            public Object valueForObject(Object object)
            {
                return ((ItemProperties)object).name();
            }
        });

        addActionsForCategories(context, result, catNames);
        return actionCategories;
    }

    private void addActionsForCategories (Context context, Map<String, List<ItemProperties>> result, List<String> catNames)
    {
        for (String cat : catNames) {
            context.push();
            if (!cat.equals(DefaultActionCategory)) context.set(KeyActionCategory, cat);
            collectActionsByCategory(context, result, cat);
            context.pop();
        }
    }

    public void collectActionsByCategory (Context context, Map <String, List<ItemProperties>> result, String targetCat)
    {
        // assume "module" has been asserted in context, now get classes
        List<ItemProperties> actionInfos = itemProperties(context, KeyAction, true);
        for (ItemProperties actionInfo : actionInfos) {
            context.push();
            context.set(KeyAction, actionInfo.name());
            Boolean visible = context.booleanPropertyForKey(KeyVisible, true);
            context.pop();
            if (visible != null && visible.booleanValue()) {
                String category = (String)actionInfo.properties().get(KeyActionCategory);
                if (category == null) category = DefaultActionCategory;
                if (!targetCat.equals(category)) continue;
                List<ItemProperties> forCategory = result.get(category);
                if (forCategory == null) {
                    forCategory = new ArrayList();
                    result.put(category, forCategory);
                }
                forCategory.add(actionInfo);
            }
        }
    }

    private AWResponseGenerating _fireAction(Context context, AWRequestContext requestContext)
    {
        Object result = context.propertyForKey("actionResults");

        // deal permissively with action scripts that return non-AWResponseGenerating results
        if (!(result instanceof AWResponseGenerating)) return null;

        preparePage(context, (AWResponseGenerating)result);
        return (AWResponseGenerating)result;
    }

    public AWResponseGenerating fireAction (ItemProperties action, Context context,
                                            AWRequestContext requestContext)
    {
        context.push();
        context.set(KeyActionCategory, action.properties().get(KeyActionCategory));
        context.set(KeyAction, action.name());
        AWResponseGenerating result = _fireAction(context, requestContext);
        context.pop();
        return result;
    }

    public AWResponseGenerating fireAction (Context context,
                                            AWRequestContext requestContext)
    {
        context.push();
        AWResponseGenerating result = _fireAction(context, requestContext);
        context.pop();
        return result;
    }

    public void preparePage (Context context, AWResponseGenerating result)
    {
        if (result instanceof AWComponent) {
            Object pageBindings = context.propertyForKey("pageBindings");
            if (pageBindings != null) applyValues(result,  (Map)pageBindings, context);
        }
    }

    public static void applyValues (Object target, Map <String, Object> values, Context context)
    {
        for (Map.Entry<String, Object> e : values.entrySet()) {
            FieldValue.setFieldValue(target, e.getKey(), context.resolveValue(e.getValue()));
        }
    }

    public static String beautifyClassName (String className)
    {
        int dot = className.lastIndexOf('.');
        if (dot != -1) className = className.substring(dot+1);
        return AWVIdentifierFormatter.decamelize(className);
    }

    public static String beautifyFileName (String path)
    {
        return AWVIdentifierFormatter.decamelize(AWUtil.pathToLastComponent(AWUtil.lastComponent(path, "/"), "."));
    }

    AWBindingDictionary bindingDictionaryForValueMap (Map<String, Object> map)
    {
        if (MapUtil.nullOrEmptyMap(map)) return null;
        Map <String, AWBinding> bindingMap = new HashMap(map.size());
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String name = e.getKey();
            Object val = e.getValue();
            AWBinding binding = null;
            if (val instanceof PropertyValue.Dynamic) {
                binding = new MetaIncludeComponent.DynamicValueBinding();
                ((MetaIncludeComponent.DynamicValueBinding)binding).init(name, (PropertyValue.Dynamic)val);
            } else {
                binding = AWBinding.bindingWithNameAndConstant(name, val);
            }
            bindingMap.put(name, binding);
        }
        return new AWBindingDictionary(bindingMap);
    }
    
    Set<String> _loadedNames = new HashSet();

    public void loadRuleFromResourceNamed (String name)
    {
        if (!name.endsWith(".oss")) name += ".oss";
        if (!_loadedNames.contains(name)) {
            _loadedNames.add(name);
            AWResourceManager resourceManager = AWConcreteServerApplication.sharedInstance().resourceManager();
            AWResource resource = resourceManager.resourceNamed(name);
            if (resource != null) {
                beginRuleSet(resource.relativePath());
                _loadRuleFile(resource);
            }
        }
    }

    /*
        Generate rules on-demand for classes based on introspection
     */
    boolean _didDeclareModules = false;

    void ensureDidDeclareModules()
    {
        if (_didDeclareModules) return;
        _didDeclareModules = true;
        declareModulesForClasses(_NavModuleClasses);
    }

    void declareModulesForClasses (List<String> moduleClasses)
    {
        if (moduleClasses.size() == 0) return;
        Log.meta.debug("Auto declaring modules for classes: %s ", moduleClasses);
        for (String className : moduleClasses) {
            String classFileName = className.replace(".", "/") + ".java";
            beginRuleSet(classFileName);
            try {
                List <Rule.Selector> selectors = Arrays.asList(new Rule.Selector(KeyModule, className));
                ListUtil.lastElement(selectors)._isDecl = true;

                Map properties = new HashMap();
                addTrait("ModuleClassPage", properties);
                properties.put("moduleClassName", className);
                Rule r = new Rule(selectors, properties, ClassRulePriority);

                addRule(r);

                // Add decl rule for this module being home for this class
                addRule(new Rule(
                        Arrays.asList(new Rule.Selector(KeyModule, className),
                              new Rule.Selector("homeForClasses", true),
                              new Rule.Selector(KeyClass, className, true)),
                        new HashMap(),
                        ClassRulePriority));
            } finally {
                endRuleSet();
            }
        }
    }

    class FileMetaProvider implements Meta.ValueQueriedObserver
    {

        public void notify(Meta meta, String key, Object value)
        {
            Log.meta_detail.debug("FileMetaProvider notified of first use of class: %s ", value);
            String className = ((String)value);
            int dot = className.lastIndexOf(".");
            if (dot != -1) {
                String pkg = className.substring(0, dot);
                String pkgRuleFile = pkg.replace(".", "/").concat("/rules.oss");
                loadRuleFromResourceNamed(pkgRuleFile);
                // loadName(className);
            }
        }
    }

    // Marker interface
    public interface AutoLocalized
    {
        String packageName ();
        String fileKey ();
        String key ();
    }

    LocalizedString createLocalizedString (String key, String defaultValue)
    {
        Assert.that(_currentRuleSet != null, "Attempt to create localized string without currentRuleSet in place");
        return new LocalizedString(_currentRuleSet.filePath(), key, defaultValue);
    }

    public static class LocalizedStringCache
    {
        private AWResourceManagerDictionary _localizedStringsHashtable = new AWResourceManagerDictionary();

        AWResourceManager resourceManager (Context context)
        {
            AWResourceManager resourceManager = null;
            if (context instanceof UIContext) {
                resourceManager = ((UIContext)context).resourceManager();
            }
            return (resourceManager != null) ? resourceManager
                    : AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
        }


        public String cacheLookup (Context context, String key)
        {
            AWSingleLocaleResourceManager resourceManager = (AWSingleLocaleResourceManager)resourceManager(context);
            return (String)_localizedStringsHashtable.get(resourceManager);
        }

        public String fullLookup (Context context, String key, String stringTable, String fileKey, String defaultString)
        {
            AWSingleLocaleResourceManager resourceManager = (AWSingleLocaleResourceManager)resourceManager(context);
            synchronized (this) {
                String localizedString = (String)_localizedStringsHashtable.get(resourceManager);
                if (localizedString == null) {
                    AWStringLocalizer localizer = AWConcreteApplication.SharedInstance.getStringLocalizer();

                    Map localizedStringsHashtable =  localizer.getLocalizedStrings (stringTable, fileKey, resourceManager);
                    if (localizedStringsHashtable != null) {
                        localizedString = (String)localizedStringsHashtable.get(key);
                    }
                    if (localizedString == null) {
                        localizedString = resourceManager.pseudoLocalizeUnKeyed(defaultString);
                    }
                    if (!AWConcreteApplication.IsRapidTurnaroundEnabled) {
                        _localizedStringsHashtable.put(resourceManager, localizedString);
                    }
                }
                return localizedString;
            }
        }
    }

    public static class LocalizedString extends LocalizedStringCache implements PropertyValue.Dynamic
    {
        String _filePath;
        String _key;
        String _defaultString;

        public LocalizedString (String filePath, String key, String defaultValue)
        {
            _filePath = filePath;
            _key = key;
            _defaultString = defaultValue;
        }

        public Object evaluate (Context context)
        {
            String localizedString = cacheLookup(context, _key);
            if (localizedString == null) {
                String stringTable = AWUtil.fileNameToJavaPackage(_filePath);
                String fileKey = AWUtil.stripToBaseFilename(_filePath);
                localizedString = fullLookup(context, _key, stringTable, fileKey, _defaultString);
            }
            return localizedString;
        }
    }

    public void throwSampleException ()
    {
        try {
            _throwSampleException();
        } catch (Exception t) {
            throw new AWGenericException("Exception called _throwSample", t);
        }
    }

    void _throwSampleException ()
    {
        throw new AWGenericException("_throwSampleException always throws!");
    }

    public static void registerLocalizedClass (String className)
    {
        _LocalizedClasses.add(className);
    }

    public static Set<String> localizedClasses ()
    {
        return _LocalizedClasses;
    }

    public static class LocalizedLabelString extends LocalizedString
            implements Meta.PropertyMapAwaking, AutoLocalized
    {
        public LocalizedLabelString ()
        {
            super(null, null, null);
        }

        public Object evaluate (Context context)
        {
            if (_key == null) {
                // our contextKey scope determines our declaration location (file / package)
                Rule.Wrapper wrapper = (Rule.Wrapper)context.propertyForKey(DeclRule);
                if (wrapper != null) {
                    _filePath = wrapper.rule.getRuleSet().filePath();
                } else {
                    Log.meta.debug("Mising rule wrapper for localized string!");
                    context.debug();
                }
                // if declaration was in java file, then key is just field/method name
                boolean isJava = (_filePath != null && _filePath.endsWith(".java"));

                String scopeKey = (String)context.values().get(Meta.ScopeKey);
                String scopeVal = (String)context.values().get(scopeKey);
                if (UIMeta.KeyClass.equals(scopeKey) || UIMeta.KeyModule.equals(scopeKey)) {
                    scopeVal = AWUtil.lastComponent(scopeVal, ".");
                }
                _defaultString = defaultLabelForIdentifier(scopeVal);
                _key = isJava ? scopeVal : (scopeKey + "_" + scopeVal);

            }
            return _filePath != null ? super.evaluate(context) : _defaultString;
        }

        public Object awakeForPropertyMap (Meta.PropertyMap map)
        {
            return new LocalizedLabelString();
        }

        public String packageName ()
        {
            return AWUtil.fileNameToJavaPackage(_filePath);
        }

        public String fileKey ()
        {
            return AWUtil.stripToBaseFilename(_filePath);
        }

        public String key ()
        {
            return _key;
        }
    }
}
