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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/UIMeta.java#12 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.util.AWClassLoader;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWNotificationCenter;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.validation.AWVIdentifierFormatter;
import ariba.ui.meta.annotations.Properties;
import ariba.ui.meta.annotations.Traits;
import ariba.ui.meta.annotations.Action;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValue;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

public class UIMeta extends Meta
{
    public final static String KeyOperation = "operation";
    public final static String KeyClass = "class";
    public final static String KeyField = "field";
    public final static String KeyModule = "module";
    public final static String KeyLayout = "layout";
    public final static String KeyArea = "area";
    public final static String KeyAction = "action";
    public final static String KeyActionCategory = "actionCategory";

    public final static String KeyObject = "object";
    public final static String KeyValue = "value";
    public final static String KeyType = "type";
    public final static String KeyElementType = "elementType";
    public final static String KeyTrait = "trait";
    public final static String KeyTraits = "traits";

    public final static String KeyVisible = "visible";
    public final static String KeyEditing = "editing";
    public final static String KeyEditable = "editable";
    public final static String KeyValid = "valid";

    public final static String KeyRank = "rank";
    public final static String KeyAfter = "after";
    public final static String KeyLabel = "label";
    public final static String KeyComponentName = "component";
    public final static String KeyBindings = "bindings";

    public final static String KeyHomePage = "homePage";


    static UIMeta _Instance;

    protected Map <AWResource, RuleSet> _loadedResources = new HashMap();
    protected List<String> _navModuleClasses = new ArrayList();
    Map<Class, List<AnnotationProcessor>> _annotationProcessors = new HashMap();

    public static UIMeta getInstance ()
    {
        if (_Instance == null) {
            _Instance = new UIMeta();
        }
        return _Instance;
    }

    /*
    public static UIMeta resetMeta ()
    {
        _Instance = new UIMeta();
        return _Instance;
    }
    */

    public UIMeta()
    {
        AWJarWalker.registerAnnotationListener(NavModuleClass.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _navModuleClasses.add(className);
                    }
                });
        registerKeyInitObserver(KeyClass, new IntrospectionMetaProvider());
        registerKeyInitObserver(KeyClass, new FileMetaProvider());
        registerKeyInitObserver(KeyModule, new ModuleMetaProvider());
        registerKeyInitObserver(KeyType, new FieldTypeIntrospectionMetaProvider());

        // These keys define scopes for their properties
        defineKeyAsPropertyScope(KeyField);
        defineKeyAsPropertyScope(KeyLayout);
        defineKeyAsPropertyScope(KeyModule);
        defineKeyAsPropertyScope(KeyAction);
        defineKeyAsPropertyScope(KeyActionCategory);
        defineKeyAsPropertyScope(KeyClass);

        // Default rule for converting field name to label
        registerDefaultLabelGeneratorForKey(KeyField);
        registerDefaultLabelGeneratorForKey(KeyLayout);
        registerDefaultLabelGeneratorForKey(KeyClass);
        registerDefaultLabelGeneratorForKey(KeyModule);
        registerDefaultLabelGeneratorForKey(KeyAction);
        registerDefaultLabelGeneratorForKey(KeyActionCategory);

        // policies for chaining certain well known properties
        registerPropertyMerger(KeyVisible, new PropertyMerger_And());
        registerPropertyMerger(KeyVisible, new PropertyMerger_And());
        registerPropertyMerger(KeyEditable, new PropertyMerger_And());
        registerPropertyMerger(KeyValid, new PropertyMerger_Valid());
        registerPropertyMerger(KeyTraits, PropertyMerger_List);
        registerPropertyMerger(KeyField, Context.PropertyMerger_DeclareList);
        registerPropertyMerger(KeyLayout, Context.PropertyMerger_DeclareList);
        registerPropertyMerger(KeyModule, Context.PropertyMerger_DeclareList);
        registerPropertyMerger(KeyAction, Context.PropertyMerger_DeclareList);
        registerPropertyMerger(KeyActionCategory, Context.PropertyMerger_DeclareList);

        mirrorPropertyToContext(KeyClass, KeyClass);
        mirrorPropertyToContext(KeyEditing, KeyEditing);
        mirrorPropertyToContext(KeyEditable, KeyEditable);
        mirrorPropertyToContext(KeyType, KeyType);
        mirrorPropertyToContext(KeyElementType, KeyElementType);
        mirrorPropertyToContext(KeyTraits, KeyTrait);
        mirrorPropertyToContext(KeyLayout, KeyLayout);

        registerValueTransformerForKey(KeyObject, Transformer_KeyPresent);
        registerValueTransformerForKey("requestContext", Transformer_KeyPresent);
        registerValueTransformerForKey("displayGroup", Transformer_KeyPresent);

        registerDerivedValue("fieldsByZone", new Context.StaticDynamicWrapper(new Context.StaticallyResolvable() {
                public Object evaluate(Context context) {
                    Map m = ((UIMeta)context.meta()).itemNamesByZones(context, KeyField);
                    String zonePath = (String)context.propertyForKey("zonePath");
                    return (zonePath == null) ? m : FieldValue.getFieldValue(m, zonePath);
                }
            }), KeyClass, "*");
        
        registerDerivedValue("layoutsByZone", new Context.StaticDynamicWrapper(new Context.StaticallyResolvable() {
                public Object evaluate(Context context) {
                    return ((UIMeta)context.meta()).itemNamesByZones(context, KeyLayout);
                }
            }), KeyLayout, "*");

        registerAnnotationListener(Traits.class, new AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AccessibleObject prop, List predicateList, Map propertyMap, boolean isAction)
            {
                processTraitsAnnotation((Traits)annotation, prop, propertyMap);
            }
        });

        registerAnnotationListener(Properties.class, new AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AccessibleObject prop, List predicateList, Map propertyMap, boolean isAction)
            {
                processPropertiesAnnotation((Properties)annotation, prop, predicateList);
            }
        });

        registerAnnotationListener(Action.class, new AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AccessibleObject prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (isAction) processActionAnnotation((Action)annotation, prop, predicateList);
            }
        });

        AWPage.registerLifecycleListener(new AWPage.LifecycleListener() {
            // Listen for new page activations and check for rule file changes
            public void pageWillRender(AWPage page)
            {
               checkRuleFileChanges();
            }
            
            public void pageWillAwake (AWPage page) { }
            public void pageWillSleep (AWPage page) { }
        });
    }

    public static abstract class AnnotationProcessor
    {
        abstract public void processAnnotation(Annotation annotation, AccessibleObject prop,
                                               List predicateList, Map propertyMap, boolean isAction);
    }

    public void registerAnnotationListener (Class annotationClass, AnnotationProcessor listener)
    {
        List<AnnotationProcessor> listeners = _annotationProcessors.get(annotationClass);
        if (listeners == null) {
            listeners = new ArrayList();
            _annotationProcessors.put(annotationClass, listeners);
        }
        listeners.add(listener);
    }

    void invokeAnnotationListeners (Annotation annotation, AccessibleObject prop, List predicateList, Map propertyMap, boolean isAction)
    {
        List<AnnotationProcessor> listeners = _annotationProcessors.get(annotation.annotationType());
        if (listeners != null) {
            for (AnnotationProcessor l : listeners) {
                l.processAnnotation(annotation, prop, predicateList, propertyMap, isAction);
            }
        }
    }
    
    // Use a special map subsclass for our Properties
    protected PropertyMap newPropertiesMap ()
    {
        return new PropertyMap();
    }

    private static final FieldPath _FieldPathNullMarker = new FieldPath("null");

    protected static class PropertyMap extends Meta.PropertyMap
    {
        FieldPath _fieldPath;

        public FieldPath fieldPath() {
            if (_fieldPath == null) {
                Object value = get(KeyValue);
                String fieldName = (String)get(KeyField);
                _fieldPath = (fieldName != null && value == null)
                        ? new FieldPath(fieldName)
                        : _FieldPathNullMarker;
            }
            return _fieldPath == _FieldPathNullMarker ? null : _fieldPath;
        }
    }
    /*
        Provide subclass context with conveniences for getting object field values
     */
    public Context newContext()
    {
        return new UIContext(this);
    }

    public static class UIContext extends Context
    {
        AWRequestContext _requestContext;

        public UIContext(UIMeta meta)
        {
            super(meta);
        }

        public Object value ()
        {
            Object object = object();
            // Assert.that(object != null, "Call to value() with no current object");
            if (object == null) return null;
            FieldPath fieldPath = fieldPath();
            return (fieldPath != null) ? fieldPath.getFieldValue(object)
                                       : propertyForKey("value");
        }

        public void setValue (Object val)
        {
            Object object = object();
            Assert.that(object != null, "Call to value() with no current object");
            FieldPath fieldPath = fieldPath();
            Assert.that(fieldPath != null, "Can't set derived property (yet)");
            fieldPath.setFieldValue(object, val);
        }

        public FieldPath fieldPath()
        {
            return ((PropertyMap)allProperties()).fieldPath();
        }

        // need to override these -- Extensible gives error if value is queried before being set 
        public Object object ()
        {
            return values().get(KeyObject);
        }

        public void setObject (Object object)
        {
            values().put(KeyObject, object);
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
            beginRuleSet(rank);
            _loadRuleFile(resource);
            return true;
        }
        return false;
    }

    protected void _loadRuleFile (AWResource resource)
    {
        Log.meta.debug("Loading rule file: %s", resource.name());
        try {
            new Parser(this, new InputStreamReader(resource.inputStream())).addRules();
        } catch (Error er) {
            endRuleSet().disableRules();
            throw new AWGenericException(Fmt.S("Error loading rule file: %s -- %s", resource.name(), er));
        } catch (Exception e) {
            endRuleSet().disableRules();
            throw new AWGenericException(Fmt.S("Exception loading rule file: %s", resource.name()), e);
        }
        // Need to set *any* object on resource to get it's hasChanged() timestamp set
        resource.setObject(Boolean.TRUE);
        _loadedResources.put(resource, endRuleSet());
    }

    private long _lastCheckMillis = 0;
    protected void checkRuleFileChanges ()
    {
        if (!AWConcreteServerApplication.IsRapidTurnaroundEnabled) return;
        // Only stat every 2 seconds
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - _lastCheckMillis > 2000) {
            _lastCheckMillis = currentTimeMillis;
            for (Map.Entry<AWResource, RuleSet> entry : _loadedResources.entrySet()) {
                AWResource resource = entry.getKey();
                if (resource.hasChanged()) {
                    Log.meta.debug("Reloading modified rule file: %s", resource.name());
                    Meta.RuleSet ruleSet = entry.getValue();
                    ruleSet.disableRules();
                    beginReplacementRuleSet(ruleSet);
                    _loadRuleFile(resource);
                }
            }
        }
    }

    static class _DefaultLabelGenerator implements Context.StaticallyResolvable
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

    static Context.DynamicPropertyValue defaultLabelGeneratorForKey (final String key)
    {
        return new _DefaultLabelGenerator(key);
    }

    protected static Object defaultLabelForIdentifier (String fieldName)
    {
        int lastDot = fieldName.lastIndexOf('.');
        if (lastDot != -1 && lastDot != fieldName.length() -1) fieldName = fieldName.substring(lastDot+1);
        return AWVIdentifierFormatter.decamelize(fieldName);
    }

    protected void registerDerivedValue (String propKey, Context.DynamicPropertyValue dynamicValue,
                                         String contextKey, String contextValue)
    {
        Map m = new HashMap();
        m.put(propKey, dynamicValue);
        addRule(new Rule(Arrays.asList(new Predicate(contextKey, contextValue)),
                          m, SystemRulePriority));
    }

    public void registerDefaultLabelGeneratorForKey (String key)
    {
        registerDerivedValue(KeyLabel, defaultLabelGeneratorForKey(key),
                key, KeyAny);
    }

    /* Test API
    public Map<String, ItemInfo> fieldsPropertiesForClass (String className)
    {
        return fieldsPropertiesForClass(className, null);
    }

    public Map<String, ItemInfo> fieldsPropertiesForClass (String className, String operation)
    {
        Context context = newContext();
        context.set(KeyClass, className);
        context.set(KeyOperation, operation);
        return fieldsProperties(context);
    }

    public Map<String, ItemInfo> fieldsProperties (Context context)
    {
        return itemProperties(context, KeyField);
    }

    */
    public List<String> fieldNames (Context context)
    {
        return itemNames(context, KeyField);
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

    public List<String> itemNames (Context context, String key)
    {
        context.push();
        String contextVal = (String)context.values().get(key);
        if (contextVal == null) contextVal = Meta.KeyAny; 
        context.set(key, contextVal);
        context.set(KeyDeclare, true);
        List <String> fieldNames = context.listPropertyForKey(key);
        fieldNames.remove(contextVal);
        context.pop();
        return fieldNames;
    }

    public List<ItemProperties> itemProperties (Context context, String key)
    {
        List<ItemProperties> result = new ArrayList();
        List <String> names = itemNames(context, key);
        for (String itemName : names) {
            context.push();
            context.set(key, itemName);

            // only hidden at this stage if *statically* resolvable to hidden
            Object visible = context.staticallyResolveValue(context.allProperties().get(KeyVisible));
            boolean isHidden = (visible == null) || ((visible instanceof Boolean) && !((Boolean)visible).booleanValue());

            result.add(new ItemProperties(itemName, context.allProperties(), isHidden));
                // context.resolvedProperties()
            context.pop();
        }
        return result;
    }

    private static String RootPredecessorKey = "_root";
    public static final String ZoneTop = "zTop";
    public static final String ZoneLeft = "zLeft";
    public static final String ZoneRight = "zRight";
    public static final String ZoneBottom = "zBottom";

    public static String[] ZonesTLRB = {ZoneLeft, ZoneRight, ZoneTop, ZoneBottom };

    private Map<String, List> _predecessorMap(Context context, String key, final String defaultPredecessor) {
        List<ItemProperties> fieldInfos = itemProperties(context, key);
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
        Map<String, List> predecessors = _predecessorMap(context, key, zones[0]);
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
        Map<String, List> predecessors = _predecessorMap(context, property, zones[0]);
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
        Context context = newContext();
        context.set(KeyClass, className);
        return (String)context.propertyForKey("displayKey");
    }

    public static String[] ActionZones = { "Main" };

    public enum ModuleMatch { AsHome, AsShow, NoMatch };

    public static class ModuleProperties extends ItemProperties
    {
        List<String> _homeForTypes;
        List<String> _usableForTypes;

        public ModuleProperties(String name, Map properties, boolean hidden,
                                List<String> homeForTypes, List<String> usableForTypes)
        {
            super(name, properties, hidden);
            _homeForTypes = homeForTypes;
            _usableForTypes = usableForTypes;
        }

        public List<String> getHomeForTypes()
        {
            return _homeForTypes;
        }

        public List<String> getUsableForTypes()
        {
            return _usableForTypes;
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

    public List<ModuleProperties> modules (Context context)
    {
        List<ItemProperties> items = itemList(context, KeyModule, ActionZones);
        List<ModuleProperties> modules = new ArrayList();
        for (ItemProperties item : items) {
            context.push();
            context.set(KeyModule, item.name());

            context.push();
            context.set("homeForClasses", true);
            List<String> homeClasses = itemNames(context, KeyClass);
            context.pop();

            context.push();
            context.set("showsClasses", true);
            List<String> showsClasses = itemNames(context, KeyClass);
            context.pop();

            modules.add(new ModuleProperties(item.name(), item.properties(), item.isHidden(),
                    homeClasses, showsClasses));
        }
        return modules;
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

    public Map <String, List<ItemProperties>> navActionsByCategory (Context context)
    {
        // assume "module" has been asserted in context, now get classes
        List<String> homeClasses = context.listPropertyForKey("homeForClasses");
        List<String> showClasses = context.listPropertyForKey("showsClasses");
        List <String> allClasses = new ArrayList();
        allClasses.addAll(homeClasses);
        allClasses.addAll(showClasses);
        context.push();
        context.set(KeyClass, allClasses);
        Map<String, List<ItemProperties>> result = new HashMap();
        actionsByCategory(context, result);
        context.pop();
        return result;
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
        /*
        context.set(KeyActionCategory, catNames);
        collectActionsByCategory(context, result);
        */
        for (String cat : catNames) {
            context.push();
            context.set(KeyActionCategory, cat);
            collectActionsByCategory(context, result);
            context.pop();
        }
        return actionCategories;
    }

    /*
        public Map <String, List<ItemProperties>> navActionsByCategory (Context context)
        {
            // assume "module" has been asserted in context, now get classes
            List<String> homeClasses = context.listPropertyForKey("homeForClasses");
            List<String> showClasses = context.listPropertyForKey("showsClasses");
            List <String> allClasses = new ArrayList();
            allClasses.addAll(homeClasses);
            allClasses.addAll(showClasses);
            Map <String, List<ItemProperties>> result = new HashMap();
            for (String className : allClasses) {
                context.push();
                context.set(KeyClass, className);
                collectActionsByCategory(context, result);
                context.pop();
            }
            return result;
        }

    */
    public void collectActionsByCategory (Context context, Map <String, List<ItemProperties>> result)
    {
        // assume "module" has been asserted in context, now get classes
        List<ItemProperties> actionInfos = itemProperties(context, KeyAction);
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

    /*
        "Valid" property support
            valid prop should return false or an error string if error, true or null otherwise
    */
    public static String validationError (Context context)
    {
        Object error = context.propertyForKey(UIMeta.KeyValid);
        if (error == null) return null;
        if (error instanceof Boolean) {
            return ((Boolean)error) ? null : "Invalid entry";
        }
        return error.toString();
    }

    static class PropertyMerger_Valid implements PropertyMerger
    {
        public Object merge(Object orig, Object override) {
            // if first is error (error message or false, it wins), otherwise second
            return (override instanceof String
                    || (override instanceof Boolean && !((Boolean)override).booleanValue()))
                ? override
                : orig;
        }
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
                beginRuleSet();
                _loadRuleFile(resource);
            }
        }
    }

    /*
        Generate rules on-demand for classes based on introspection
     */
    static class _IntrospectionFieldInfo {
        int rank;
        Field field;
    }

    boolean _didDeclareModules = false;

    void declareModulesForClasses (List<String> moduleClasses)
    {
        if (moduleClasses.size() == 0) return;
        Log.meta.debug("Auto declaring modules for classes: %s ", moduleClasses);
        beginRuleSet();
        for (String className : moduleClasses) {
            List <Predicate>predicates = Arrays.asList(new Predicate(KeyModule, className));
            ListUtil.lastElement(predicates)._isDecl = true;

            Map properties = new HashMap();
            addTrait("ModuleClassPage", properties);
            properties.put("moduleClassName", className);
            Rule r = new Rule(predicates, properties, ClassRulePriority);

            addRule(r);

            // Add decl rule for this module being home for this class
            addRule(new Rule(
                    Arrays.asList(new Predicate(KeyModule, className),
                          new Predicate("homeForClasses", true),
                          new Predicate(KeyClass, className, true)),
                    new HashMap(),
                    ClassRulePriority));
        }
        endRuleSet();
    }

    public static void addTraits(List traits, Map map)
    {
        List current = (List)map.get(KeyTraits);
        if (current == null) {
            map.put(KeyTraits, new ArrayList(traits));
        } else {
            current.addAll(traits);
        }
    }
    
    public static void addTrait(String trait, Map map)
    {
        List current = (List)map.get(KeyTraits);
        if (current == null) {
            map.put(KeyTraits, Arrays.asList(trait));
        } else {
            current.add(trait);
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

    public void processTraitsAnnotation (Traits fs, AccessibleObject prop, Map propertyMap)
    {
        Object val = fs.value();
        List traits;
        if (val instanceof String) {
            traits = ListUtil.arrayToList(((String)val).split(" "));
        } else {
            traits = ListUtil.arrayToList((String[])val);
        }
        addTraits(traits, propertyMap);
        Log.meta_detail.debug("---- annotation for field %s -- traits: %s", prop, traits);
    }

    public void processPropertiesAnnotation (Properties propInfo, AccessibleObject prop, List predicateList)
    {
        String propString = propInfo.value();
        try {
            new Parser(UIMeta.this, propString).processRuleBody(predicateList, null);
        } catch (Error e) {
            throw new AWGenericException(Fmt.S("Error parsing @Properties annotation \"%s\" on %s:%s -- %s",
                    propString, prop.getClass().getName(), prop, e));
        } catch (ParseException e) {
            throw new AWGenericException(Fmt.S("Exception parsing @Properties annotation \"%s\" on %s:%s",
                    propString, prop.getClass().getName(), prop), e);
        }
        Log.meta_detail.debug("---- annotation for field %s -- @Properites: %s", prop, propString);
    }

    public void processActionAnnotation (Action annotation, AccessibleObject prop, List predicateList)
    {
        Method method = (Method)prop;
        boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
        String name = method.getName();
        if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
            Log.meta.debug("ERROR: can't declare non-public fields as Actions: (Class):%s -- ignoring...",
                    name);
            return;
        }
        Map properties = new HashMap();

        List predicates = new ArrayList(predicateList);
        predicates.add(predicateList.size()-1, new Predicate(KeyActionCategory, annotation.category()));
        properties.put(KeyActionCategory, annotation.category());

        if (!isStatic) addTraits(Arrays.asList("instance"), properties);

        // Todo:  Category part of predicate!        
        addTraits(Arrays.asList(annotation.ResponseType().name()), properties);

        String message = annotation.message();
        if (!StringUtil.nullOrEmptyString(message)) properties.put("message", message);

        String pageName = annotation.pageName();
        if (!StringUtil.nullOrEmptyString(pageName)) properties.put("pageName", pageName);
        addRule(new Rule(predicates, properties, ClassRulePriority));
    }

    class IntrospectionMetaProvider implements Meta.ValueQueriedObserver, AWNotificationCenter.Observer
    {
        Map<String, RuleSet> _ruleSetsByClassName = new HashMap();

        public IntrospectionMetaProvider ()
        {
            // register for AWReload notification
            AWNotificationCenter.addObserver(AWClassLoader.ClassReloadedTopic, this);
        }

        public void onNotification(String topic, Object data)
        {
            List<String> classNames = (List)data;
            for (String className : classNames) {
                Class cls = ClassUtil.classForName(className);
                RuleSet ruleSet = _ruleSetsByClassName.get(className);
                if (ruleSet != null && cls != null) {
                    Log.meta.debug("***** reprocessing reloaded class %s", className);
                    ruleSet.disableRules();
                    registerRulesForClass(cls);
                }
            }
        }

        public void notify(Meta meta, String key, Object value)
        {
            Log.meta.debug("IntrospectionMetaProvider notified of first use of class: %s ", value);
            Class cls = ClassUtil.classForName((String)value, Object.class, false);
            if (cls != null) {
                registerRulesForClass(cls);
            }
        }

        void registerRulesForClass (Class cls)
        {
            String className = cls.getName();
            Class sc = cls.getSuperclass();
            if (sc != null) {
                keyData(KeyClass).setParent(className, sc.getName());
            }

            beginRuleSet();
            Properties propInfo =  (Properties)cls.getAnnotation(ariba.ui.meta.annotations.Properties.class);
            if (propInfo != null) {
                String propString = propInfo.value();
                List predicateList = Arrays.asList(new Predicate(KeyClass, className));
                try {
                    new Parser(UIMeta.this, propString).processRuleBody(predicateList, null);
                } catch (Error e) {
                    throw new AWGenericException(Fmt.S("Error parsing @Properties annotation \"%s\" on class %s -- %s",
                            propString, className, e));
                } catch (ParseException e) {
                    throw new AWGenericException(Fmt.S("Exception parsing @Properties annotation \"%s\" on class %s",
                            propString, className), e);
                }

                Log.meta_detail.debug("Annotations for class %s -- @Properites: %s", className, propString);
            }

            _registerActionsForClass(className, cls);
            _registerFieldsForClass(className, cls);
            RuleSet ruleSet = endRuleSet();
            _ruleSetsByClassName.put(className, ruleSet);
        }

        void _registerActionsForClass (String key, Class cls)
        {
            for (Method method : cls.getDeclaredMethods()) {
                Annotation[] annotations = method.getAnnotations();
                if (annotations != null && annotations.length > 0) {
                    String name = method.getName();
                    Map properties = new HashMap();
                    List <Predicate>predicates = Arrays.asList(new Predicate(KeyClass, key),
                                                new Predicate(KeyAction, name));
                    ListUtil.lastElement(predicates)._isDecl = true;
                    processAnnotations(method, predicates, properties, true);
                    if (properties.size() > 0) {
                        Rule r = new Rule(predicates, properties, ClassRulePriority);
                        addRule(r);
                    }
                }
            }
        }

        void processAnnotations(AccessibleObject prop, List predicateList, Map map, boolean isAction)
        {
            Annotation[] annotations = prop.getAnnotations();
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    invokeAnnotationListeners(annotation,  prop,  predicateList, map, isAction);
                }
            }
        }

        void _registerFieldsForClass (String key, Class cls)
        {
            Map<String, _IntrospectionFieldInfo> rankMap = new HashMap();
            int lastRank = _populateSuggestedFieldRankMap(cls, rankMap, 100);
            try {
                BeanInfo info = Introspector.getBeanInfo(cls);

                for (PropertyDescriptor p :  info.getPropertyDescriptors()) {
                    String name = p.getName();
                    Class type = p.getPropertyType();

                    // Java beans plays games with "is" methods that FieldValue doesn't
                    // so for now we need to recover the "is"
                    if (p.getReadMethod() != null) {
                        String readMethodName = p.getReadMethod().getName();
                        if (readMethodName.startsWith("is")) name = readMethodName;
                    }

                    if (type != null && !"class".equals(name)) {
                        // add a rule for Class + Field -> type
                        Map m = new HashMap();
                        m.put(KeyType, type.getName());

                        // List with parameterized type?
                        String collectionElementType = _collectionElementType(cls, p);
                        if (collectionElementType != null) {
                            m.put(KeyElementType, collectionElementType);
                        }
                        m.put(KeyField, name);
                        m.put(KeyVisible, true);

                        if (p.getWriteMethod() == null) {
                            m.put(KeyEditable, false);
                            addTraits(Arrays.asList("derived"), m);
                        }

                        List<Predicate> predicateList = Arrays.asList(new Predicate(KeyClass, key),
                                                        new Predicate(KeyField, name));
                        ListUtil.lastElement(predicateList)._isDecl = true;
                        
                        // Try to get natural rank
                        _IntrospectionFieldInfo finfo = rankMap.get(name);
                        int rank;
                        if (finfo != null) {
                            processAnnotations(finfo.field, predicateList, m, false);
                            rank = finfo.rank;
                        } else {
                            lastRank += 10;
                            rank = lastRank;
                        }
                        m.put(KeyRank, rank);

                        Rule r = new Rule(predicateList, m, ClassRulePriority);
                        addRule(r);
                    }
                }
            } catch (IntrospectionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        // Default field order to the order of declaration of the instance variables
        private int _populateSuggestedFieldRankMap (Class cls, Map<String, _IntrospectionFieldInfo> map, int startRank)
        {
            Class scls = cls.getSuperclass();
            if (scls != null) {
                startRank = _populateSuggestedFieldRankMap(scls, map, startRank);
            }

            Field[] fields = cls.getDeclaredFields();

            for (int index = 0, length = fields.length; index < length; index++) {
                Field field = fields[index];

                if ((field.getModifiers() & Modifier.STATIC) == 0) {
                    String name = field.getName();
                    if (name.startsWith("_")) name = name.substring(1);
                    _IntrospectionFieldInfo fi = new _IntrospectionFieldInfo();
                    fi.rank = startRank;
                    fi.field = field;
                    map.put(name, fi);
                    startRank += 10;
                }
            }
            return startRank;
        }
    }

    static protected ParameterizedType _parameterizedTypeForProperty (Class src, PropertyDescriptor p)
    {
        Type type;
        Method readMethod = p.getReadMethod();
        if (readMethod != null
                && ((type = readMethod.getGenericReturnType()) != null)
                && (type instanceof ParameterizedType))
            return (ParameterizedType)type;

        try {
            Field field = src.getDeclaredField(p.getName());
            if (field != null
                    && ((type = field.getGenericType()) != null)
                    && (type instanceof ParameterizedType))
                return (ParameterizedType)type;
        } catch (NoSuchFieldException e) {
            // ignore
        }

        Method writeMethod = p.getWriteMethod();
        Type types[];
        if (writeMethod != null
                && ((types = writeMethod.getGenericParameterTypes()) != null)
                && (types.length ==1) && (types[0] instanceof ParameterizedType))
            return (ParameterizedType)types[0];

        return null;
    }

    static protected String _collectionElementType(Class src, PropertyDescriptor p)
    {
        ParameterizedType pType = _parameterizedTypeForProperty(src, p);
        if (pType != null && (pType.getRawType() instanceof Class)
            && Collection.class.isAssignableFrom((Class)pType.getRawType()))
        {
            Type[] ta = pType.getActualTypeArguments();
            if (ta.length == 1 && ta[0] instanceof Class) {
                return ((Class)ta[0]).getName();
            }
        }
        return null;
    }

    class FieldTypeIntrospectionMetaProvider implements Meta.ValueQueriedObserver
    {
        public void notify(Meta meta, String key, Object value)
        {
            Log.meta.debug("FieldTypeIntrospectionMetaProvider notified of first use of field type: %s ", value);
            Class cls = ClassUtil.classForName((String)value, Object.class, false);
            if (cls != null) {
                registerRulesForClass(cls);
            }
        }

        void registerRulesForClass (Class cls)
        {
            String className = cls.getName();
            Class sc = cls.getSuperclass();
            if (sc != null) {
                keyData(KeyType).setParent(className, sc.getName());
            }

            if (cls.isEnum()) {
                registerChoicesForEnum(cls);
            }

            // Do something for non-primitives -- add default trait?
            if (!cls.isPrimitive()) {
                Log.meta.debug("Registering non-primitive field type: %s ", cls);
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

    protected void registerChoicesForEnum (Class cls)
    {
        Log.meta.debug("Registering enum choices for: %s ", cls);
        Field[] flds = cls.getDeclaredFields();
        for (Field f : flds) {
            if (f.isEnumConstant()) {
                Log.meta.debug("enum field: %s ", f);
            }
        }
        Object[] enumConstants = cls.getEnumConstants();
        Log.meta.debug("enum constants: %s " + enumConstants);
    }
}
