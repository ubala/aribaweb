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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/UIMeta.java#32 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.validation.AWVIdentifierFormatter;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.fieldvalue.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIMeta extends ObjectMeta
{
    public final static String KeyOperation = "operation";
    public final static String KeyModule = "module";
    public final static String KeyLayout = "layout";
    public final static String KeyArea = "area";
    public final static String KeyEditing = "editing";
    public final static String KeyAfter = "after";
    public final static String KeyLabel = "label";
    public final static String KeyComponentName = "component";
    public final static String KeyBindings = "bindings";
    public final static String KeyHomePage = "homePage";
    public final static String KeyZonePath = "zonePath";

    static UIMeta _Instance;

    protected Map <AWResource, RuleSet> _loadedResources = new HashMap();
    protected List<String> _navModuleClasses = new ArrayList();

    public static UIMeta getInstance ()
    {
        if (_Instance == null) {
            _Instance = new UIMeta();
        }
        return _Instance;
    }

    public UIMeta()
    {
        AWJarWalker.registerAnnotationListener(NavModuleClass.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _navModuleClasses.add(className);
                    }
                });
        registerKeyInitObserver(KeyClass, new FileMetaProvider());

        registerKeyInitObserver(KeyModule, new ModuleMetaProvider());

        // These keys define scopes for their properties
        defineKeyAsPropertyScope(KeyLayout);
        defineKeyAsPropertyScope(KeyModule);

        // Default rule for converting field name to label
        registerDefaultLabelGeneratorForKey(KeyField);
        registerDefaultLabelGeneratorForKey(KeyLayout);
        registerDefaultLabelGeneratorForKey(KeyClass);
        registerDefaultLabelGeneratorForKey(KeyModule);
        registerDefaultLabelGeneratorForKey(KeyAction);
        registerDefaultLabelGeneratorForKey(KeyActionCategory);

        // policies for chaining certain well known properties
        registerPropertyMerger(KeyLayout, Context.PropertyMerger_DeclareList);
        registerPropertyMerger(KeyModule, Context.PropertyMerger_DeclareList);

        mirrorPropertyToContext(KeyEditing, KeyEditing);
        mirrorPropertyToContext(KeyLayout, KeyLayout);
        mirrorPropertyToContext(KeyComponentName, KeyComponentName);

        registerValueTransformerForKey("requestContext", Transformer_KeyPresent);
        registerValueTransformerForKey("displayGroup", Transformer_KeyPresent);

        registerDerivedValue("fieldsByZone", new PropertyValue.StaticDynamicWrapper(new PropertyValue.StaticallyResolvable() {
                public Object evaluate(Context context) {
                    Map m = ((UIMeta)context.meta()).itemNamesByZones(context, KeyField);
                    String zonePath = zonePath(context);
                    return (zonePath == null) ? m : FieldValue.getFieldValue(m, zonePath);
                }
            }), KeyClass, "*");
        
        registerDerivedValue("fieldPropertyList", new PropertyValue.StaticDynamicWrapper(new PropertyValue.StaticallyResolvable() {
                public Object evaluate(Context context) {
                    return ((UIMeta)context.meta()).fieldList(context);
                }
            }), KeyClass, "*");

        registerDerivedValue("layoutsByZone", new PropertyValue.StaticDynamicWrapper(new PropertyValue.StaticallyResolvable() {
                public Object evaluate(Context context) {
                    return ((UIMeta)context.meta()).itemNamesByZones(context, KeyLayout);
                }
            }), KeyLayout, "*");

        AWPage.registerLifecycleListener(new AWPage.LifecycleListener() {
            // Listen for new page activations and check for rule file changes
            public void pageWillRender(AWPage page)
            {
               checkRuleFileChanges(false);
            }
            
            public void pageWillAwake (AWPage page) { }
            public void pageWillSleep (AWPage page) { }
        });
    }

    public String zonePath (Context context)
    {
        String zonePath = null;
        if (context.values().get(KeyLayout) != null) {
            context.push();
            context.setContextKey(KeyLayout);
            zonePath = (String)context.propertyForKey(KeyZonePath);
            context.pop();
        }
        return zonePath;
    }

    public Context newContext()
    {
        return new UIContext(this);
    }

    public static class UIContext extends ObjectContext
    {
        AWRequestContext _requestContext;

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
        }

        public AWComponent getComponent ()
        {
            return _requestContext.getCurrentComponent();
        }
    }

    public boolean loadRuleFile (String filename, boolean required, int rank)
    {
        AWResourceManager resourceManager = AWConcreteServerApplication.sharedInstance().resourceManager();
        AWResource resource = resourceManager.packageResourceNamed(filename);
        Assert.that(!required || resource != null, "Rule file not found in resource search path: %s", filename);
        if (resource != null) {
            beginRuleSet(rank, filename);
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
        _loadRules(resource.name(), resource.inputStream(), (resource instanceof AWFileResource));

        // Need to set *any* object on resource to get it's hasChanged() timestamp set
        resource.setObject(Boolean.TRUE);
        _loadedResources.put(resource, endRuleSet());
    }

    private long _lastCheckMillis = 0;
    public void checkRuleFileChanges (boolean force)
    {
        if (!AWConcreteServerApplication.IsRapidTurnaroundEnabled) return;
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
        ruleSet.disableRules();
        beginReplacementRuleSet(ruleSet);
        _loadRuleFile(resource);
    }

    static class _DefaultLabelGenerator implements PropertyValue.StaticallyResolvable
    {
        String _key;
        public _DefaultLabelGenerator (String key) { _key = key; }

        public Object evaluate(Context context) {
            Object fieldName = context.propertyForKey(_key);
            return (fieldName != null && fieldName instanceof String)
                    ? defaultLabelForIdentifier((String)fieldName)
                    : null;
        }
    }

    static PropertyValue.Dynamic defaultLabelGeneratorForKey (final String key)
    {
        return new _DefaultLabelGenerator(key);
    }

    protected static Object defaultLabelForIdentifier (String fieldName)
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
        addRule(new Rule(Arrays.asList(new Rule.Predicate(contextKey, contextValue)),
                          m, SystemRulePriority));
    }

    public void registerDefaultLabelGeneratorForKey (String key)
    {
        registerDerivedValue(KeyLabel, defaultLabelGeneratorForKey(key),
                key, KeyAny);
    }

    public List<ItemProperties> fieldList (Context context)
    {
        return itemList(context, KeyField, ZonesTLRB);
    }

    public Map<String, Object> fieldsByZones (Context context)
    {
        return itemsByZones(context, KeyField, ZonesTLRB);
    }

    public Map<String, Object> itemNamesByZones (Context context, String key)
    {
        List<String> zones = (List)context.propertyForKey("zones");
        if (zones == null) zones = Arrays.asList("main");
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
    public static final String ZoneTop = "zTop";
    public static final String ZoneLeft = "zLeft";
    public static final String ZoneRight = "zRight";
    public static final String ZoneBottom = "zBottom";

    public static String[] ZonesTLRB = {ZoneLeft, ZoneRight, ZoneTop, ZoneBottom };

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

    public String displayKeyForClass (String className)
    {
        // performance: should use registerDerivedValue("fieldsByZone", new Context.StaticDynamicWrapper
        // to get cached resolution here...
        Context context = newContext();
        context.set(KeyClass, className);
        context.set(KeyLayout, "LabelField");
        List<ItemProperties> fields = itemProperties(context, KeyField, true);

        return fields.isEmpty() ? null : fields.get(0).name();
    }


    public static String[] ActionZones = { "Main" };

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

    public ModuleInfo computeModuleInfo (Context context)
    {
        ModuleInfo moduleInfo = new ModuleInfo();
        // List<ItemProperties> items = itemList(context, KeyModule, ActionZones);
        moduleInfo.modules = ListUtil.list();
        Set<String> classesSet = new HashSet();
        List<String> allModuleNames = itemNames(context, KeyModule);
        moduleInfo.moduleNames = ListUtil.list();

        for (String moduleName : allModuleNames) {
            context.push();
            context.set(KeyModule, moduleName);

            if (!context.booleanPropertyForKey(KeyVisible, true)) continue;

            moduleInfo.moduleNames.add(moduleName);

            context.push();
            context.set("homeForClasses", true);
            List<String> homeClasses = itemNames(context, KeyClass);
            context.pop();

            context.push();
            context.set("showsClasses", true);
            List<String> showsClasses = itemNames(context, KeyClass);
            context.pop();

            moduleInfo.modules.add(new ModuleProperties(moduleName, context.allProperties(), false,
                    homeClasses, showsClasses));

            classesSet.addAll(homeClasses);
            classesSet.addAll(showsClasses);
        }
        moduleInfo.classNames = ListUtil.list();
        moduleInfo.classNames.addAll(classesSet);

        context.push();
        context.set(KeyModule, moduleInfo.moduleNames);
        context.set(KeyClass, moduleInfo.classNames);
        moduleInfo.actionsByCategory = MapUtil.map();
        moduleInfo.actionCategories = actionsByCategory(context, moduleInfo.actionsByCategory);
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
            return ((NavContextProvider)pageComponent).currentNavContext();
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
        context.set(KeyAction, KeyAny);
        context.set(KeyDeclare, KeyAction);
        List <String> categoryNames = context.listPropertyForKey(KeyActionCategory);
        context.pop();
        return categoryNames;
    }

    // caller must push/pop!
    public List<ItemProperties> actionsByCategory(Context context, Map<String, List<ItemProperties>> result)
    {
        List actionCategories = itemList(context, KeyActionCategory, ActionZones);
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
            context.set(KeyActionCategory, cat);
            collectActionsByCategory(context, result);
            context.pop();
        }
    }

    public void collectActionsByCategory (Context context, Map <String, List<ItemProperties>> result)
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
        Object resultWrapper = context.propertyForKey("actionResults");
        AWResponseGenerating result = (AWResponseGenerating)context.resolveValue(resultWrapper);
        preparePage(context, result);
        return result;
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

    void declareModulesForClasses (List<String> moduleClasses)
    {
        if (moduleClasses.size() == 0) return;
        Log.meta.debug("Auto declaring modules for classes: %s ", moduleClasses);
        for (String className : moduleClasses) {
            beginRuleSet(className);
            List <Rule.Predicate>predicates = Arrays.asList(new Rule.Predicate(KeyModule, className));
            ListUtil.lastElement(predicates)._isDecl = true;

            Map properties = new HashMap();
            addTrait("ModuleClassPage", properties);
            properties.put("moduleClassName", className);
            Rule r = new Rule(predicates, properties, ClassRulePriority);

            addRule(r);

            // Add decl rule for this module being home for this class
            addRule(new Rule(
                    Arrays.asList(new Rule.Predicate(KeyModule, className),
                          new Rule.Predicate("homeForClasses", true),
                          new Rule.Predicate(KeyClass, className, true)),
                    new HashMap(),
                    ClassRulePriority));
            endRuleSet();
        }
    }

    class ModuleMetaProvider implements Meta.ValueQueriedObserver
    {
        public void notify(Meta meta, String key, Object value)
        {
            String moduleName = ((String)value);
            Log.meta.debug("ModuleMetaProvider notified of first use of module: %s ", moduleName);
            if (!_didDeclareModules) declareModulesForClasses(_navModuleClasses);
            _didDeclareModules = true;
        }
    }

    class FileMetaProvider implements Meta.ValueQueriedObserver
    {

        public void notify(Meta meta, String key, Object value)
        {
            Log.meta.debug("FileMetaProvider notified of first use of class: %s ", value);
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

}
