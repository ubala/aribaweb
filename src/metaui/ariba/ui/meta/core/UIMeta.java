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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/UIMeta.java#5 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWClassLoader;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWNotificationCenter;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.validation.AWVIdentifierFormatter;
import ariba.ui.meta.annotations.Properties;
import ariba.ui.meta.annotations.Traits;
import ariba.ui.meta.annotations.Action;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIMeta extends Meta
{
    public final static String KeyOperation = "operation";
    public final static String KeyClass = "class";
    public final static String KeyField = "field";
    public final static String KeyModule = "module";
    public final static String KeyLayout = "layout";
    public final static String KeyAction = "action";
    public final static String KeyActionCategory = "actionCategory";
    public final static String KeyLayoutProperties = "layoutProperties";
    public final static String KeyClassProperty = "classproperty";

    public final static String KeyObject = "object";
    public final static String KeyValue = "value";
    public final static String KeyType = "type";
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

    static UIMeta _Instance;

    protected Map <AWResource, RuleSet> _loadedResources = new HashMap();

    public static UIMeta getInstance ()
    {
        if (_Instance == null) {
            _Instance = new UIMeta();
        }
        _Instance.checkRuleFileChanges();
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
        registerKeyInitObserver(KeyClass, new IntrospectionMetaProvider());
        registerKeyInitObserver(KeyClass, new FileMetaProvider());
        registerKeyInitObserver(KeyModule, new FileMetaProvider());
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
        registerDefaultLabelGeneratorForKey(KeyModule);
        registerDefaultLabelGeneratorForKey(KeyAction);
        registerDefaultLabelGeneratorForKey(KeyActionCategory);

        // policies for chaining certain well known properties
        registerPropertyMerger(KeyVisible, new PropertyMerger_And());
        registerPropertyMerger(KeyVisible, new PropertyMerger_And());
        registerPropertyMerger(KeyEditable, new PropertyMerger_And());
        registerPropertyMerger(KeyValid, new PropertyMerger_Valid());
        registerPropertyMerger(KeyTraits, PropertyMerger_List);
        registerPropertyMerger(KeyField, PropertyMerger_List);
        registerPropertyMerger(KeyLayout, PropertyMerger_List);
        registerPropertyMerger(KeyModule, PropertyMerger_List);
        registerPropertyMerger(KeyAction, PropertyMerger_List);
        registerPropertyMerger(KeyActionCategory, PropertyMerger_List);

        mirrorPropertyToContext(KeyClass, KeyClass);
        mirrorPropertyToContext(KeyEditing, KeyEditing);
        mirrorPropertyToContext(KeyEditable, KeyEditable);
        mirrorPropertyToContext(KeyType, KeyType);
        mirrorPropertyToContext(KeyTraits, KeyTrait);

        registerValueTransformerForKey(KeyObject, Transformer_KeyPresent);
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
        } catch (Exception e) {
            endRuleSet().disableRules();
            throw new AWGenericException(Fmt.S("Error loading rule file: %s", resource.name()), e);
        }
        // Need to set *any* object on resource to get it's hasChanged() timestamp set
        resource.setObject(Boolean.TRUE);
        _loadedResources.put(resource, endRuleSet());
    }

    private long _lastCheckMillis = 0;
    protected void checkRuleFileChanges ()
    {
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

    static Context.DynamicPropertyValue defaultLabelGeneratorForKey (final String key)
    {
        return new Context.DynamicPropertyValue() {
            public Object evaluate(Context context) {
                Object fieldName = context.propertyForKey(key);
                return (fieldName != null && fieldName instanceof String)
                        ? AWVIdentifierFormatter.decamelize((String)fieldName)
                        : null;
            }
        };
    }

    public void registerDefaultLabelGeneratorForKey (String key)
    {
        Map m = new HashMap();
        m.put(KeyLabel, defaultLabelGeneratorForKey(key));
        addRule(new Rule(Arrays.asList(new Predicate(key, KeyAny)),
                          m, LowRulePriority));
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

    public Map<String, List<ItemProperties>> fieldsByZones (Context context)
    {
        return itemsByZones(context, KeyField, ZonesTLRB);
    }


    public List<String> itemNames (Context context, String key)
    {
        context.push();
        context.set(key, Meta.KeyAny);
        context.set(KeyDeclare, true);
        List <String> fieldNames = context.listPropertyForKey(key);
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
            result.add(new ItemProperties(itemName, context.resolvedProperties(),
                                        context.popActivation()));
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

    public Map<String, List<ItemProperties>> itemsByZones (Context context, String key, String[] zones)
    {
        Map<String, List> predecessors = _predecessorMap(context, key, zones[0]);
        Map<String, List<ItemProperties>> byZone = new HashMap();
        for (String zone : zones) {
            List<ItemProperties> list = new ArrayList();
            accumulatePrecessors(predecessors, zone, list);
            byZone.put(zone, list);
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
            Boolean visible = (Boolean) item.properties().get(KeyVisible);
            if (visible != null && visible.booleanValue()) result.add(item);
            accumulatePrecessors(predecessors, (String) item._name, result);
        }
    }

    public static String[] ActionZones = { "Main" };

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
        List catNames = AWUtil.collect(actionCategories, new AWUtil.ValueMapper () {
            public Object valueForObject(Object object)
            {
                return ((ItemProperties)object).name();
            }
        });
        context.set(KeyActionCategory, catNames);
        collectActionsByCategory(context, result);
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
            Boolean visible = (Boolean) actionInfo.properties().get(KeyVisible);
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

    public AWResponseGenerating fireAction (ItemProperties action, Context context,
                                            AWRequestContext requestContext)
    {
        context.push();
        context.set(KeyClass, action.properties().get(KeyClass));
        context.set(KeyActionCategory, action.properties().get(KeyActionCategory));
        context.restoreActivation(action.activation());
        context.set("requestContext", requestContext);
        Object resultWrapper = context.propertyForKey("actionResults");
        if (resultWrapper instanceof List) resultWrapper = ((List)resultWrapper).get(0);
        AWResponseGenerating result = (AWResponseGenerating)context.resolveValue(resultWrapper);
        Object pageBindings = context.propertyForKey("pageBindings");
        if (pageBindings != null) applyValues(result,  (Map)pageBindings, context);
        context.pop();
        return result;
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
            Log.meta.debug("IntrospectionMetaProvider notified of first use of class: %s " + value);
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
                    new Parser(UIMeta.this, propString).processRuleBody(predicateList);
                } catch (ParseException e) {
                    throw new AWGenericException(Fmt.S("Error parsing @Properties annotation \"%s\" on class %s",
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
                boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
                String name = method.getName();
                Action annotation;
                if ((annotation = method.getAnnotation(Action.class)) != null) {
                    if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                        Log.meta.debug("ERROR: can't declare non-public fields as Actions: %s:%s -- ignoring...",
                                key, name);
                        continue;
                    }
                    Map properties = new HashMap();
                    List <Predicate>predicates = Arrays.asList(new Predicate(KeyClass, key),
                                            new Predicate(KeyAction, name));
                    ListUtil.lastElement(predicates)._isDecl = true;
                    if (!isStatic) _addTraits(Arrays.asList("instance"), properties);
                    // properties.put(KeyVisible, true);

                    properties.put(KeyActionCategory, annotation.category());
                    _addTraits(Arrays.asList(annotation.ResponseType().name()), properties);

                    String message = annotation.message();
                    if (!StringUtil.nullOrEmptyString(message)) properties.put("message", message);

                    String pageName = annotation.pageName();
                    if (!StringUtil.nullOrEmptyString(pageName)) properties.put("pageName", pageName);
                    processAnnotations(method, predicates, properties);

                    Rule r = new Rule(predicates, properties, ClassRulePriority);
                    addRule(r);
                }
            }
        }

        void processAnnotations(AccessibleObject prop, List predicateList, Map map)
        {
            Traits fs = prop.getAnnotation(Traits.class);
            if (fs != null) {
                Object val = fs.value();
                List traits;
                if (val instanceof String) {
                    traits = ListUtil.arrayToList(((String)val).split(" "));
                } else {
                    traits = ListUtil.arrayToList((String[])val);
                }
                _addTraits(traits, map);
                Log.meta_detail.debug("---- annotation for field %s -- traits: %s", prop, traits);
            }


            Properties propInfo = prop.getAnnotation(Properties.class);
            if (propInfo != null) {
                String propString = propInfo.value();
                try {
                    new Parser(UIMeta.this, propString).processRuleBody(predicateList);
                } catch (ParseException e) {
                    throw new AWGenericException(Fmt.S("Error parsing @Properties annotation \"%s\" on %s:%s",
                            propString, prop.getClass().getName(), prop), e);
                }

                Log.meta_detail.debug("---- annotation for field %s -- @Properites: %s", prop, propString);
            }
        }

        void _addTraits (List traits, Map map)
        {
            List current = (List)map.get(KeyTraits);
            if (current == null) {
                map.put(KeyTraits, new ArrayList(traits));
            } else {
                current.addAll(traits);
            }
        }

        void _registerFieldsForClass (String key, Class cls)
        {
            Map<String, _IntrospectionFieldInfo> rankMap = new HashMap();
            int lastRank = _populateSuggestedFieldRankMap(cls, rankMap, 100);
            try {
                BeanInfo info = Introspector.getBeanInfo(cls);
                Predicate predicateDeclare = new Predicate(KeyDeclare, true);

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
                        /* Declare field */
                        Map decl = new HashMap();
                        decl.put(KeyField, name);
                        addRule(new Rule(Arrays.asList(new Predicate(KeyField, KeyAny), predicateDeclare), decl));

                        // add a rule for Class + Field -> type
                        Map m = new HashMap();
                        m.put(KeyType, type.getName());
                        m.put(KeyField, name);
                        m.put(KeyVisible, true);

                        if (p.getWriteMethod() == null) {
                            m.put(KeyEditable, false);
                            _addTraits(Arrays.asList("derived"), m);
                        }

                        List predicateList = Arrays.asList(new Predicate(KeyClass, key),
                                                        new Predicate(KeyField, name));
                        // Try to get natural rank
                        _IntrospectionFieldInfo finfo = rankMap.get(name);
                        int rank;
                        if (finfo != null) {
                            processAnnotations(finfo.field, predicateList, m);
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

    class FieldTypeIntrospectionMetaProvider implements Meta.ValueQueriedObserver
    {
        Map<String, RuleSet> _ruleSetsByClassName = new HashMap();

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
            /*
            beginRuleSet();
            Properties propInfo =  (Properties)cls.getAnnotation(Properties.class);
            if (propInfo != null) {
                String propString = propInfo.value();
                List predicateList = Arrays.asList(new Predicate(KeyClass, className));
                try {
                    new Parser(UIMeta.this, propString).processRuleBody(predicateList);
                } catch (ParseException e) {
                    throw new AWGenericException(Fmt.S("Error parsing @Properties annotation \"%s\" on class %s",
                            propString, className), e);
                }
                Log.meta_detail.debug("Annotations for class %s -- @Properites: %s", className, propString);
            }

            List all = new ArrayList();
            _registerRulesForClass(className, cls, all);
            RuleSet ruleSet = endRuleSet();
            _ruleSetsByClassName.put(className, ruleSet);
            */
        }

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
