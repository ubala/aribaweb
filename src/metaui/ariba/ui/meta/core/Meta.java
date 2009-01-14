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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Meta.java#20 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;

public class Meta
{
    public static final String KeyAny = "*";
    public static final String KeyDeclare = "declare";
    public static final int LowRulePriority = -100000;
    public static final int SystemRulePriority = -200000;
    public static final int ClassRulePriority = -100000;
    public static final int TemplateRulePriority = 100000;
    public static final int EditorRulePriority = 200000;
    static final int MaxKeyDatas = 32;
    static final Object NullMarker = new Object();
    protected static final String ScopeKeySuffix = "_p";

    Map <String, KeyData> _keyData = new HashMap();
    KeyData[] _keyDatasById = new KeyData[MaxKeyDatas];
    int _nextKeyId = 0;
    List <Rule> _rules = new ArrayList();
    int _ruleSetGeneration = 0;

    // Todo: NOT THREAD SAFE!
    Map <Match, PropertyMap> _MatchToPropsCache = new HashMap();
    Map<PropertyMap, PropertyMap> _PropertyMapUniquer = new HashMap();
    IdentityHashMap _identityCache = new IdentityHashMap();
    RuleSet _currentRuleSet;
    Map <String, PropertyManager> _managerForProperty = new HashMap();
    int _declareKeyMask;

    public Meta ()
    {
        _declareKeyMask = keyData(KeyDeclare).maskValue();
    }

    public void addRule (Rule rule)
    {
        List <Rule.Predicate> predicates =  rule._predicates;
        if (predicates.size() > 0 && predicates.get(predicates.size()-1)._isDecl) {
            Rule decl = rule.createDecl();
            addRule(decl);
        }

        // After we've captured the decl, do the collapse
        predicates = rule._predicates = rule.collapseKeyOverrides(rule._predicates);

        // we allow null to enable creation of a decl, but otherwise this rule has no effect
        if (rule._properties == null) return;

        int entryId = _rules.size();
        if (rule._rank == 0) rule._rank = allocateNextRuleRank();
        rule._ruleSet = _currentRuleSet;
        Log.meta_detail.debug("Add Rule, rank=%s: %s", Integer.toString(rule._rank), rule);

        // index it
        String lastScopeKey = null;
        int mask = 0, antiMask = 0;
        int count = predicates.size();
        for (int i=0; i < count; i++) {
            Rule.Predicate p = predicates.get(i);

            // See if overridded by same key later in predicate
            boolean overridden = false;
            for (int j=count-1; j>i; j--) {
                if (predicates.get(j)._key.equals(p._key)) { overridden=true; break; }
            }
            if (overridden) continue;

            KeyData data = keyData(p._key);
            if (p._value != NullMarker) {
                if (p._value instanceof List) {
                    for (Object v : (List)p._value) {
                        data.addEntry(v, entryId);
                    }
                }
                else {
                    data.addEntry(p._value, entryId);
                }
                mask |= (1 << data._id);

                String scopeKey = data.propertyScopeKey();
                if (scopeKey != null) lastScopeKey = scopeKey;
            } else {
                antiMask |= (1 << data._id);
            }
        }

        // all non-decl rules don't apply outside decl context
        int declMask = declareKeyMask();
        if ((mask & declMask) == 0) antiMask |= declMask;

        if (lastScopeKey != null) {
            rule._predicates = ListUtil.copyList(predicates);
            rule._predicates.add(new Rule.Predicate(lastScopeKey, true));
            KeyData data = keyData(lastScopeKey);
            data.addEntry(true, entryId);
            mask |= (1 << data._id);
        }

        rule._id = entryId;
        rule._keyMatchesMask = mask;
        rule._keyAntiMask = antiMask;
        _rules.add(rule);
    }

    protected int allocateNextRuleRank ()
    {
        if (_currentRuleSet != null) {
            return _currentRuleSet._rank++;
        }
        return _rules.size();
    }

    public void addRule (Map predicateMap, Map propertyMap)
    {
        addRule(predicateMap, propertyMap,  0);
    }

    public void addRule (Map predicateMap, Map propertyMap, int rank)
    {
        Rule rule = new Rule(predicateMap, propertyMap);
        if (rank != 0) rule._rank = rank;
        addRule(rule);
    }

    public void addRules (Map <String, Object> ruleSet, List predicates)
    {
        // Special keys:  "props, "rules".  Everthing else is a predicate
        Map props = null;
        List <Map <String, Object>> rules = null;
        predicates = (predicates == null) ? new ArrayList() : new ArrayList(predicates);
        for (Map.Entry <String, Object> entry : ruleSet.entrySet()) {
            String key = entry.getKey();
            if (key.equals("props")) {
                props = (Map)entry.getValue();
            }
            else if (key.equals("rules")) {
                rules = (List)entry.getValue();
            }
            else {
                predicates.add(new Rule.Predicate(key, entry.getValue()));
            }
        }
        if (props != null) {
            addRule(new Rule(predicates, props));
        }
        if (rules != null) {
            for (Map <String, Object> r : rules) {
                addRules(r, predicates);
            }
        }
    }

    public void addRules (Map <String, Object> ruleSet)
    {
        addRules(ruleSet, null);
    }

    public void loadRules (String filename, InputStream inputStream, int rank, boolean editable)
    {
        beginRuleSet(rank, filename);
        _loadRules(filename, inputStream, editable);
    }

    public static final String RuleFileDelimeter = "/*!--- Editor Generated Rules -- Content below this line will be overwritten ---*/\n\n";
    public static final String RuleFileDelimeterStart = "/*!--- ";
    boolean _RuleEditingEnabled = true;

    protected void _loadRules (String filename, InputStream inputStream, boolean editable)
    {
        Log.meta.debug("Loading rule file: %s", filename);
        try {
            String editRules = null, userRules = AWUtil.stringWithContentsOfInputStream(inputStream);
            // Read ruls
            int sepStart = userRules.indexOf(RuleFileDelimeterStart);
            if (sepStart != -1) {
                int sepEnd = userRules.indexOf("*/", sepStart);
                if (sepEnd != -1) {
                    editRules = userRules.substring(sepEnd + 2);
                    userRules = userRules.substring(0, sepStart);
                }
            }

            // read user rules
            new Parser(this, userRules).addRules();

            if (editable &&_RuleEditingEnabled) {
                _currentRuleSet._editableStart = _rules.size();
                // boost rank for editor rules
                _currentRuleSet._rank = EditorRulePriority;

                if (editRules != null) {
                    new Parser(this, editRules).addRules();
                }
            }
        } catch (Error er) {
            endRuleSet().disableRules();
            throw new AWGenericException(Fmt.S("Error loading rule file: %s -- %s", filename, er));
        } catch (Exception e) {
            endRuleSet().disableRules();
            throw new AWGenericException(Fmt.S("Exception loading rule file: %s", filename), e);
        }
    }

    public String parsePropertyAssignment (String propString, Map propertyMap)
    {
        String error = null;
        try {
            new Parser(this, propString).processProperty(propertyMap);
        } catch (Error e) {
            error = e.getMessage();
        } catch (ParseException e) {
            error = e.getMessage();
        }
        return error;
    }

    protected void clearCaches ()
    {
        _MatchToPropsCache = new HashMap();
        _PropertyMapUniquer = new HashMap();
        _identityCache = new IdentityHashMap();
    }

    public class RuleSet
    {
        String _filePath;
        int _start, _end, _editableStart = -1; 
        int _rank;

        public void disableRules()
        {
            for (int i=_start; i < _end; i++) {
                _rules.get(i).disable();
            }
            clearCaches();
        }

        public String filePath()
        {
            return _filePath;
        }

        public List<Rule> rules (boolean editableOnly)
        {
            List<Rule> result = ListUtil.list();
            int i = (editableOnly) ? (_editableStart == -1 ? _end : _editableStart) 
                    : _start;
            for ( ; i<_end; i++) {
                Rule r = _rules.get(i);
                if (!r.disabled()) result.add(r);
            }
            return result;
        }
    }

    public void beginRuleSet (String filePath)
    {
        beginRuleSet(_rules.size(), filePath);
    }

    public void beginRuleSet (int rank, String filePath)
    {
        Assert.that(_currentRuleSet == null, "Can't start new rule set while one in progress");
        _currentRuleSet = new RuleSet();
        _currentRuleSet._start = _rules.size();
        _currentRuleSet._rank = rank;
        _currentRuleSet._filePath = filePath;
    }

    public void beginReplacementRuleSet (RuleSet orig)
    {
        beginRuleSet(orig.filePath());
        _currentRuleSet._rank = orig._rank - (orig._end - orig._start);
    }

    public RuleSet endRuleSet ()
    {
        Assert.that(_currentRuleSet != null, "No rule set progress");
        RuleSet result = _currentRuleSet;
        result._end = _rules.size();
        _currentRuleSet = null;
        _ruleSetGeneration++;
        return result;
    }

    public void _setCurrentRuleSet (RuleSet ruleSet)
    {
        _currentRuleSet = ruleSet;
    }

    // Can be compared to detect if rule set has been updated (due to incremental loading)
    public int ruleSetGeneration ()
    {
        return _ruleSetGeneration;
    }

    public void invalidateRules ()
    {
        _ruleSetGeneration++;
        clearCaches();
    }

    public Context newContext()
    {
        return new Context(this);
    }

    int declareKeyMask ()
    {
        return _declareKeyMask;
    }

    // Touch a key/value to force pre-loading/registration of associated rule files
    public void touch (String key, Object value)
    {
        Context context = newContext();
        context.push();
        context.set(key, value);
        context.allProperties();
        context.pop();
    }

    Object transformValue (String key, Object value)
    {
        KeyData keyData = _keyData.get(key);
        if (keyData != null && keyData._transformer != null) value = keyData._transformer.tranformForMatch(value);
        return value;
    }

    Match.MatchResult match (String key, Object value, Match.MatchResult intermediateResult)
    {
        KeyData keyData = _keyData.get(key);
        if (keyData == null) return intermediateResult;
        int keyMask = 1 << keyData._id;

        // Does our result already include this key?  Then no need to join again
        if (intermediateResult != null && (intermediateResult._keysMatchedMask & keyMask) != 0) return intermediateResult;

        return new Match.MatchResult(this, keyData, value, intermediateResult);
    }

    // subclasses can override to provide specialized properties Map subclasses
    protected PropertyMap newPropertiesMap ()
    {
        return new PropertyMap();
    }

    public interface PropertyMapAwaking
    {
        Object awakeForPropertyMap (PropertyMap map);
    }

    public static class PropertyMap extends HashMap <String, Object>
    {
        List<PropertyManager> _contextPropertiesUpdated;

        void awakeProperties ()
        {
            for (Map.Entry<String, Object> e : entrySet()) {
                Object value = e.getValue();
                if (value instanceof PropertyMapAwaking) {
                    Object newValue = ((PropertyMapAwaking)value).awakeForPropertyMap(this);
                    if (newValue != value) put(e.getKey(), newValue);
                }
            }
        }

        void addContextKey (PropertyManager key)
        {
            if (_contextPropertiesUpdated == null) _contextPropertiesUpdated = new ArrayList();
            _contextPropertiesUpdated.add(key);
        }

        List<PropertyManager> contextKeysUpdated ()
        {
            return _contextPropertiesUpdated;
        }
    }

    PropertyMap propertiesForMatch (Match.MatchResult matchResult, AWDebugTrace.AssignmentRecorder recorder)
    {

        PropertyMap properties = _MatchToPropsCache.get(matchResult);
        if (properties != null && recorder == null) return properties;


        properties = newPropertiesMap();

        int[] arr = matchResult.filter();
        if (arr == null) return properties;
        // first entry is count
        int count = arr[0];
        Rule[] rules = new Rule[count];
        for (int i=0; i < count; i++) {
            rules[i] = _rules.get(arr[i+1]);
        }

        // sort by rank
        Arrays.sort(rules, new Comparator <Rule>() {
            public int compare(Rule o1, Rule o2) {
                // ascending
                return o1._rank - o2._rank;
            }
        });

        int modifiedMask = 0;
        boolean isDeclare = (_declareKeyMask & matchResult._keysMatchedMask) != 0;
        for (Rule r : rules) {
            if (recorder != null && r._rank != Integer.MIN_VALUE) {
                recorder.setCurrentSource(new Rule.AssignmentSource(r));
            }

            modifiedMask |= r.apply(this, properties, isDeclare, recorder);
        }

        properties.awakeProperties();

/* Uniquer doesn't interact well with DynamicStaticProperties
        // Unique property maps
        PropertyMap matchingMap = _PropertyMapUniquer.get(properties);
        if (matchingMap != null) {
            properties = matchingMap;
        } else {
            _PropertyMapUniquer.put(properties, properties);
        }
*/
        if (recorder == null) _MatchToPropsCache.put(matchResult.immutableCopy(), properties);
        return properties;
    }

    KeyData keyData (String key)
    {
        KeyData data = _keyData.get(key);
        if (data == null) {
            int id = _nextKeyId;
            Assert.that(id < MaxKeyDatas-1, "Exceeded maximum number of context keys");
            _nextKeyId++;
            data = new KeyData(key, id);
            _keyDatasById[id] = data;
            _keyData.put(key, data);
        }
        return data;
    }

    public void registerKeyInitObserver (String key, ValueQueriedObserver o)
    {
        keyData(key).addObserver(o);
    }

    public void registerValueTransformerForKey (String key, KeyValueTransformer transformer)
    {
        keyData(key)._transformer = transformer;
    }

    protected IdentityHashMap identityCache ()
    {
        return _identityCache;
    }

    public static interface ValueQueriedObserver
    {
        void notify (Meta meta, String key, Object value);
    }

    // Used to transform values into the version they should be indexed / searched under
    // For instance, "object" may be indexed as true/false (present or not)
    interface KeyValueTransformer {
        public Object tranformForMatch (Object o);
    }

    static class KeyData
    {
        String _key;
        int _id;
        Map <Object, _ValueMatches> _ruleVecs;
        List <ValueQueriedObserver> _observers;
        _ValueMatches _any;
        KeyValueTransformer _transformer;
        String _propertyScopeKey;

        public KeyData (String key, int id)
        {
            _key = key;
            _id = id;
            _ruleVecs = new HashMap();
            _any = get(KeyAny);

        }

        public int maskValue ()
        {
            return 1 << _id;
        }

        private _ValueMatches get (Object value)
        {
            if (_transformer != null) value = _transformer.tranformForMatch(value);
            _ValueMatches matches = _ruleVecs.get(value);
            if (matches == null) {
                matches = new _ValueMatches(value);
                if (value != null && !Boolean.FALSE.equals(value)) matches._parent = _any;
                _ruleVecs.put(value, matches);
            }
            return matches;
        }

        void addEntry (Object value, int id)
        {
            _ValueMatches matches = get(value);
            int[] before = matches._arr;
            int[] after = Match.addInt(before, id);
            if (before != after) {
                matches._arr = after;
            }
        }

        int[] lookup (Meta owner, Object value)
        {
            _ValueMatches matches = get(value);
            if (!matches._read && _observers != null) {
                // notify
                matches._read = true;
                if (value != null) {
                    for (ValueQueriedObserver o : _observers) {
                        o.notify(owner, _key, value);
                    }
                }
            }
            // check if parent has changed and need to union in parent data
            matches.checkParent();
            return matches._arr;
        }

        void setParent (Object value, Object parentValue)
        {
            _ValueMatches parent = get(parentValue);
            _ValueMatches child = get(value);
            child._parent = parent;
        }

        Object parent (Object value)
        {
            _ValueMatches child = get(value);
            return child._parent._value;
        }

        void addObserver (ValueQueriedObserver o)
        {
            if (_observers == null) {
                _observers = new ArrayList();
            }
            _observers.add(o);
        }

        // If this key defines a scope for properties (e.g. field, class, action)
        // this this returns the name of the predicate key for those properties
        // (e.g. field_p, class_p)
        String propertyScopeKey ()
        {
            return _propertyScopeKey;
        }

        public void setPropertyScopeKey (String propertyScopeKey)
        {
            _propertyScopeKey = propertyScopeKey;
        }
    }

    static private class _ValueMatches {
        Object _value;
        boolean _read;
        int[] _arr;
        _ValueMatches _parent;
        int _parentSize;

        private _ValueMatches(Object value)
        {
            _value = value;
        }

        void checkParent ()
        {
            // Todo: performance: keep a rule set version # and only do this when
            // the rule set has reloaded
            if (_parent != null) {
                _parent.checkParent();
                if (_parent._arr != null && _parent._arr[0] != _parentSize) {
                    _arr = Match.union(_arr, _parent._arr);
                    _parentSize = _parent._arr[0];
                }
            }
        }
    }

    public static final KeyValueTransformer Transformer_KeyPresent = new KeyValueTransformer()
    {
        public Object tranformForMatch(Object o) {
            return (o != null && !(o.equals(Boolean.FALSE))) ? Boolean.TRUE : Boolean.FALSE;
        }
    };

    public static class OverrideValue
    {
        Object _value;
        public OverrideValue (Object value) { _value = value; }
        public Object value() { return _value; }
        public String toString () { return (_value != null ? _value.toString() : "null") + "!"; }
    }

    public class PropertyManager
    {
        String _name;
        PropertyMerger _merger;
        KeyData _keyDataToSet;

        public PropertyManager (String propertyName)
        {
            _name = propertyName;
        }

        Object mergeProperty (String propertyName, Object orig, Object newValue, boolean isDeclare)
        {
            if (orig == null) return newValue;
            if (newValue instanceof OverrideValue) return ((OverrideValue) newValue)._value;
            if (_merger == null) {
                // Perhaps should have a data-type-based merger registry?
                if (orig instanceof Map) {
                    if (newValue != null && newValue instanceof Map) {
                        // merge maps
                        newValue = MapUtil.mergeMapIntoMapWithObjects(MapUtil.cloneMap((Map)orig), (Map) newValue, true);
                    }
                }

                return newValue;
            }

            if (!(_merger instanceof PropertyMergerDynamic) &&
                 (orig instanceof PropertyValue.Dynamic || newValue instanceof PropertyValue.Dynamic)) {
                return new PropertyValue.DeferredOperationChain(_merger, orig, newValue);
            }

            return _merger.merge(orig, newValue, isDeclare);
        }
    }

    public boolean propertyWillDoMerge (String propertyName, Object origValue)
    {
        PropertyMerger merger = mergerForProperty(propertyName);
        return (merger instanceof PropertyMergerIsChaining)
                || ((origValue != null) && (origValue instanceof Map));
    }

    protected PropertyManager managerForProperty (String name)
    {
        PropertyManager manager = _managerForProperty.get(name);
        if (manager == null) {
            manager = new PropertyManager(name);
            _managerForProperty.put(name, manager);
        }
        return manager;
    }

    public void mirrorPropertyToContext(String propertyName, String contextKey)
    {
        KeyData keyData = keyData(contextKey);
        PropertyManager manager = managerForProperty(propertyName);
        manager._keyDataToSet = keyData;
    }

    public void defineKeyAsPropertyScope (String contextKey)
    {
        KeyData keyData = keyData(contextKey);
        keyData.setPropertyScopeKey(contextKey.concat(ScopeKeySuffix));
    }

    public static boolean isPropertyScopeKey (String key)
    {
        return key.endsWith(ScopeKeySuffix);
    }

    public interface PropertyMerger
    {
        Object merge (Object orig, Object override, boolean isDeclare);
    }

    // Marker interface
    public interface PropertyMergerIsChaining {}

    // marker interface for PropertyMerges that can handle dynamic values
    public interface PropertyMergerDynamic extends PropertyMerger { }

    public void registerPropertyMerger (String propertyName, PropertyMerger merger)
    {
        PropertyManager manager = managerForProperty(propertyName);
        manager._merger = merger;
    }

    public PropertyMerger mergerForProperty (String propertyName)
    {
        PropertyManager manager = managerForProperty(propertyName);
        return manager._merger;
    }

    protected static class PropertyMerger_Overwrite implements PropertyMerger
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            return override;
        }

        public String toString()
        {
            return "OVERWRITE";
        }
    }

    // (false trumps true) for visible and editable
    public static class PropertyMerger_And implements PropertyMerger, PropertyMergerDynamic, PropertyMergerIsChaining
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            // null will reset (so that it can be overridden to true subsequently
            if (override == null) return null;

            // If we can evaluate statically, do it now
            if (((orig instanceof Boolean) && !((Boolean)orig).booleanValue())
                || ((override instanceof Boolean) && !((Boolean)override).booleanValue()))
                    return false;

            // if one of our values is dynamic, defer
            if ((orig instanceof PropertyValue.Dynamic || override instanceof PropertyValue.Dynamic)) {
                return new PropertyValue.DeferredOperationChain(this, orig, override);
            }

            return booleanValue(orig) && booleanValue(override);
        }

        public String toString()
        {
            return "AND";
        }
    }

    // Merge lists
    public static PropertyMerger PropertyMerger_List =  new PropertyMerger()
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            // if we're override a single element with itself, don't go List...
            if (!(orig instanceof List) && !(override instanceof List)
                    && objectEquals(orig, override)) {
                return orig;
            }

            List l1 = toList(orig);
            List l2 = toList(override);
            List result = ListUtil.copyList(l1);
            ListUtil.addElementsIfAbsent(result, l2);
            return result;
        }
    };

    /*
        A few handy utilities (for which we probably already have superior versions elsewhere)
     */
    protected static boolean booleanValue (Object value)
    {
        return (value != null && (!(value instanceof Boolean) || ((Boolean)value).booleanValue()));
    }

    protected static List toList (Object value)
    {
        return (value instanceof List) ? ((List)value) : ListUtil.list(value);
    }

    static boolean objectEquals (Object one, Object two)
    {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return one.equals(two);
    }
}
