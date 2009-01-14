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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Meta.java#3 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWChecksum;
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
import java.util.Set;
import java.util.HashSet;


public class Meta
{
    public static final String KeyAny = "*";
    public static final String KeyDeclare = "declare";
    static final int LowRulePriority = -100000;
    static final int SystemRulePriority = -200000;
    static final int ClassRulePriority = -100000;
    static final int MaxKeyDatas = 32;

    Map <String, KeyData> _keyData = new HashMap();
    KeyData[] _keyDatasById = new KeyData[MaxKeyDatas];
    int _nextKeyId = 0;
    List <Rule> _rules = new ArrayList();
    Map <MatchResult, PropertyMap> _MatchToPropsCache = new HashMap();
    Map<PropertyMap, PropertyMap> _PropertyMapUniquer = new HashMap();
    IdentityHashMap _identityCache = new IdentityHashMap();
    RuleSet _currentRuleSet;
    Map <String, PropertyManager> _managerForProperty = new HashMap();

    public Meta ()
    {

    }

    public void addRule (Rule rule)
    {
        List <Predicate> predicates =  rule._predicates;
        if (predicates.size() > 0 && predicates.get(predicates.size()-1)._isDecl) {
            addDecl(rule);
        }

        // we allow null to enable creation of a decl, but otherwise this rule has no effect
        if (rule._properties == null) return;

        int entryId = _rules.size();
        if (rule._rank == 0) rule._rank = allocateNextRuleRank();

        Log.meta_detail.debug("Add Rule, rank=%s: %s", Integer.toString(rule._rank), rule);

        // index it
        String lastScopeKey = null;
        int mask = 0;
        for (Predicate p : predicates) {
            KeyData data = keyData(p._key);
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
        }

        if (lastScopeKey != null) {
            rule._predicates = ListUtil.copyList(predicates);
            rule._predicates.add(new Predicate(lastScopeKey, true));
            KeyData data = keyData(lastScopeKey);
            data.addEntry(true, entryId);
            mask |= (1 << data._id);
        }

        rule._id = entryId;
        rule._keyMatchesMask = mask;
        _rules.add(rule);
    }

    protected void addDecl (Rule rule)
    {
        /*
            @field=dyno { value:${ some expr} } becomes
            declare { field:dyno }
            field=dyno { field:dyno; value:${ some expr} }
        */
        // add rule for declaration
        List <Predicate> predicates =  rule._predicates;
        List <Predicate> prePreds = new ArrayList(predicates.subList(0, predicates.size()-1));
        Predicate declPred = predicates.get(predicates.size()-1);
        Map m = new HashMap();
        m.put(declPred._key, declPred._value);
        prePreds.add(new Predicate(declPred._key, KeyAny));
        prePreds.add(new Predicate(KeyDeclare, true));
        addRule(new Rule(prePreds, m));

        // Add property decl to main rule
        if (rule._properties == null) rule._properties = new HashMap();
        for (Predicate p : predicates) {
            rule._properties.put(p._key, p._value);
        }
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
        addRule(new Rule(predicateMap, propertyMap));
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
                predicates.add(new Predicate(key, entry.getValue()));
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

    protected void clearCaches ()
    {
        _MatchToPropsCache = new HashMap();
    }

    public class RuleSet
    {
        int _start, _end, _rank;

        public void disableRules()
        {
            for (int i=_start; i < _end; i++) {
                _rules.get(i).disable();
            }
            clearCaches();
        }
    }

    public void beginRuleSet ()
    {
        beginRuleSet(_rules.size());
    }

    public void beginRuleSet (int rank)
    {
        Assert.that(_currentRuleSet == null, "Can't start new rule set while one in progress");
        _currentRuleSet = new RuleSet();
        _currentRuleSet._start = _rules.size();
        _currentRuleSet._rank = rank;
    }

    public void beginReplacementRuleSet (RuleSet orig)
    {
        beginRuleSet();
        _currentRuleSet._rank = orig._rank - (orig._end - orig._start);
    }

    public RuleSet endRuleSet ()
    {
        Assert.that(_currentRuleSet != null, "No rule set progress");
        RuleSet result = _currentRuleSet;
        result._end = _rules.size();
        _currentRuleSet = null;
        return result;
    }

    public Context newContext()
    {
        return new Context(this);
    }

    // Touch a key/value to force pre-loading/registration of associated rule files
    public void touch (String key, Object value)
    {
        Context context = newContext();
        context.push();
        context.set(key, value);
        context.resolvedProperties();
        context.pop();
    }

    MatchResult match (String key, Object value, MatchResult intermediateResult)
    {
        KeyData keyData = _keyData.get(key);
        if (keyData == null) return intermediateResult;
        int keyMask = 1 << keyData._id;

        // get vec for this key/value -- if value is list, compute the union
        int[] newArr = null;
        if (value instanceof List) {
            for (Object v : (List)value) {
                int a[] = keyData.lookup(this, v);
                newArr = union(a, newArr);
            }
        }
        else {
            newArr = keyData.lookup(this, value);
        }

        if (intermediateResult == null || intermediateResult._matches == null) {
            return new MatchResult(newArr, keyMask);
        }

        if (newArr == null) {
            return new MatchResult(filter(intermediateResult._matches, keyMask), 
                    keyMask | intermediateResult._keysMatchedMask);
        }

        // Does our result already include this key?  Then no need to join again
        if ((intermediateResult._keysMatchedMask & keyMask) != 0) return intermediateResult;

        // Join
        int[] result = intersect(newArr, intermediateResult._matches,
                1 << keyData._id, intermediateResult._keysMatchedMask);
        return new MatchResult(result, keyMask | intermediateResult._keysMatchedMask);
    }

    // subclasses can override to provide specialized properties Map subclasses
    protected PropertyMap newPropertiesMap ()
    {
        return new PropertyMap();
    }

    public static class PropertyMap extends HashMap <String, Object>
    {
        List<PropertyManager> _contextPropertiesUpdated;

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

    PropertyMap propertiesForMatch (MatchResult matchResult)
    {

        PropertyMap properties = _MatchToPropsCache.get(matchResult);
        if (properties != null) return properties;


        properties = newPropertiesMap();

        int[] arr = filter(matchResult._matches, ~matchResult._keysMatchedMask);
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
        for (Rule r : rules) {
            modifiedMask |= r.apply(this, properties);
        }

        // Unique property maps
        PropertyMap matchingMap = _PropertyMapUniquer.get(properties);
        if (matchingMap != null) {
            properties = matchingMap;
        } else {
            _PropertyMapUniquer.put(properties, properties);
        }

        _MatchToPropsCache.put(matchResult, properties);
        return properties;
    }

    void _checkMatch (MatchResult matchResult, Map values)
    {
        int[] arr = filter(matchResult._matches, ~matchResult._keysMatchedMask);
        if (arr == null) return;
        // first entry is count
        int count = arr[0];
        for (int i=0; i < count; i++) {
            Rule r = _rules.get(arr[i+1]);
            _checkRule(r, values);
        }
    }

    void _checkRule (Rule r, Map values)
    {
        for (Predicate p : r._predicates) {
            Object contextValue = values.get(p._key);
            KeyData keyData = keyData(p._key);

            if (keyData._transformer != null) contextValue = keyData._transformer.tranformForMatch(contextValue);

            if (KeyAny.equals(p._value) || objectEquals(contextValue, p._value)
                    || ((p._value instanceof List) && ((List)p._value).contains(contextValue))
                    || ((contextValue instanceof List) && ((List)contextValue).contains(p._value)))
            {
                Log.meta_detail.debug("Possible bad rule match!  Rule: %s; predicate: %s, context val: %s",
                    r, p, contextValue);
            }
        }
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

    protected void registerKeyInitObserver (String key, ValueQueriedObserver o)
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

    // Word lists are int arrays with the first element holding the length
    static int[] addInt (int[] intArr, int val) {
        if (intArr == null) { int[] r = new int[4]; r[0]=1; r[1] = val; return r; }
        int newPos = intArr[0];
        if (intArr[newPos++] == val) return intArr;  // already here...
        if (newPos >= intArr.length) {
            int[] a = new int[newPos*2];
            System.arraycopy (intArr,0,a, 0, newPos);
            intArr = a;
        }
        intArr[newPos] = val;  intArr[0] = newPos;
        return intArr;
    }

    // only rules that use only the activated (queried) keys
    int[] filter (int[] arr, int queriedMask)
    {
        if (arr == null) return null;
        int[] result = null;
        int count = arr[0];
        for (int i=0; i < count; i++) {
            int r = arr[i+1];
            int ruleMatches = _rules.get(r)._keyMatchesMask;
            if ((ruleMatches & queriedMask) == 0) {
                result = addInt(result, r);
            }
        }
        return result;
    }

    int[] intersect (int[] a, int[] b, int aMask, int bMask) {
        if (a == null) return b;
        int[] result = null;
        int iA = 1, sizeA = a[0], iB = 1, sizeB = b[0];
        while (iA <= sizeA || iB <=sizeB) {
            int iAMask = (iA <= sizeA) ? _rules.get(a[iA])._keyMatchesMask : 0;
            int iBMask = (iB <= sizeB) ? _rules.get(b[iB])._keyMatchesMask : 0;
            int c = (iA > sizeA ? 1 : (iB > sizeB ? -1
                    : (a[iA] - b[iB])));
            if (c == 0) {
                result = addInt(result, a[iA]);
                iA++; iB++;
            } else if (c < 0) {
                // If A not in B, but A doesn't filter on B's mask, then add it
                if ((iAMask & bMask) == 0) {
                    result = addInt(result, a[iA]);
                }
                iA++;
            }
            else {
                if ((iBMask & aMask) == 0) {
                    result = addInt(result, b[iB]);
                }
                iB++;
            }
        }
        return result;
    }

    static int[] union (int[] a, int[] b) {
        if (a == null) return b;
        if (b == null) return a;
        int[] result = null;
        int iA = 1, sizeA = a[0], vA=a[1], iB = 1, sizeB = b[0], vB=b[1];
        while (iA <= sizeA || iB <=sizeB) {
            int c = vA - vB;
            result = addInt(result, ((c<=0) ? vA : vB));
            if (c <= 0) { iA++; vA = (iA <= sizeA) ? a[iA] : Integer.MAX_VALUE; }
            if (c >= 0) { iB++; vB = (iB <= sizeB) ? b[iB] : Integer.MAX_VALUE; }
        }
        return result;
    }

    public static boolean _arrayEq (int[] a, int[] b)
    {
        if (a == b) return true;
        if (a == null || b == null) return false;
        int count = a[0];
        if (count != b[0]) return false;
        for (int i=1; i <= count; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    protected class MatchResult
    {
        int _keysMatchedMask;
        int[] _matches;
        PropertyMap _properties;

        public MatchResult (int[] matches, int keysMatchedMask)
        {
            _matches = matches;
            _keysMatchedMask = keysMatchedMask;
        }

        // Hash implementation so we can cache properties by MatchResult
        public int hashCode() {
            long ret = _keysMatchedMask;
            if (_matches != null) {
                for (int i=0, c=_matches[0]; i<c; i++) {
                    ret = AWChecksum.crc32(ret, _matches[i+1]);
                }
            }
            return (int)ret;
        }

        public boolean equals(Object o) {
            MatchResult other = (MatchResult)o;
            return (_keysMatchedMask == other._keysMatchedMask) &&
                     _arrayEq(_matches, other._matches);
        }

        public PropertyMap properties ()
        {
            if (_properties == null) {
                _properties = propertiesForMatch(this);
            }
            return _properties;
        }
    }

    static interface ValueQueriedObserver
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

        private _ValueMatches get(Object value)
        {
            if (_transformer != null) value = _transformer.tranformForMatch(value);
            _ValueMatches matches = _ruleVecs.get(value);
            if (matches == null) {
                matches = new _ValueMatches();
                matches._parent = _any;
                _ruleVecs.put(value, matches);
            }
            return matches;
        }

        void addEntry (Object value, int id)
        {
            _ValueMatches matches = get(value);
            int[] before = matches._arr;
            int[] after = addInt(before, id);
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
                for (ValueQueriedObserver o : _observers) {
                    o.notify(owner, _key, value);
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
        boolean _read;
        int[] _arr;
        _ValueMatches _parent;
        int _parentSize;

        void checkParent ()
        {
            // Todo: performance: keep a rule set version # and only do this when
            // the rule set has reloaded
            if (_parent != null) {
                _parent.checkParent();
                if (_parent._arr != null && _parent._arr[0] != _parentSize) {
                    _arr = union(_arr, _parent._arr);
                    _parentSize = _parent._arr[0];
                }
            }
        }
    }

    public static final KeyValueTransformer Transformer_KeyPresent = new KeyValueTransformer()
    {
        public Object tranformForMatch(Object o) {
            return (o != null) ? Boolean.TRUE : Boolean.FALSE;
        }
    };

    static class Rule
    {
        int _id;
        int _keyMatchesMask;
        List <Predicate> _predicates;
        Map <String, Object> _properties;
        int _rank;

        public Rule (List predicates, Map properties, int rank)
        {
            _predicates = predicates;
            _properties = properties;
            _rank = rank;
        }

        public Rule (List predicates, Map properties)
        {
            this(predicates, properties, 0);
        }

        public Rule (Map predicateValues, Map properties)
        {
            _predicates = Predicate.fromMap(predicateValues);
            _properties = properties;
        }

        // returns context keys modified
        public int apply (Meta meta, PropertyMap properties)
        {
            int updatedMask = 0;
            if (_rank == Integer.MIN_VALUE) return 0;
            // MapUtil.mergeMapIntoMap(properties, _properties);

            /* Should use Property Meta to determine merge policy... */
            for (Map.Entry entry : _properties.entrySet()) {
                String key = (String)entry.getKey();
                Object value = entry.getValue();
                PropertyManager propManager = meta.managerForProperty(key);
                Object orig = properties.get(key);
                Object newVal = propManager.mergeProperty(key, orig, value);
                if (newVal != orig) {
                    properties.put(key, newVal);

                    KeyData keyData = propManager._keyDataToSet;
                    if (keyData != null) {
                        int keymask = (1 << keyData._id);
                        if ((keymask & updatedMask) == 0) {
                            updatedMask |= keymask;
                            properties.addContextKey(propManager);
                        }
                    }
                }
            }

            Log.meta_detail.debug("Evaluating Rule: %s ----> %s", this, properties);
            return updatedMask;
        }

        void disable ()
        {
            _rank = Integer.MIN_VALUE;
        }

        public String toString ()
        {
            return Fmt.S("<Rule %s -> %s>", _predicates.toString(), _properties.toString());
        }
    }

    static class Predicate
    {
        String _key;
        Object _value;
        boolean _isDecl;

        public Predicate (String key, Object value)
        {
            _key = key;
            _value = value;
        }

        public Predicate (String key, Object value, boolean isDecl)
        {
            this(key, value);
            _isDecl = isDecl;
        }

        static List fromMap (Map <String, Object> values)
        {
            List result = new ArrayList();
            for (Map.Entry <String, Object> entry : values.entrySet()) {
                result.add(new Predicate(entry.getKey(), entry.getValue()));
            }
            return result;
        }

        public String toString ()
        {
            return Fmt.S("%s=%s", _key, _value.toString());
        }
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

        Object mergeProperty (String propertyName, Object orig, Object override)
        {
            if (orig == null) return override;
            if (_merger == null) {
                // Perhaps should have a data-type-based merger registry?
                if (orig instanceof Map) {
                    if (override != null && override instanceof Map) {
                        // merge maps
//                        override = MapUtil.mergeMapIntoMapWithObjects(MapUtil.cloneMap((Map)orig), (Map)override, true);
                        override = MapUtil.mergeMapIntoMapWithObjects(MapUtil.cloneMap((Map)orig), (Map)override);
                    }
                }

                return override;
            }

            if (orig instanceof Context.DynamicPropertyValue || override instanceof Context.DynamicPropertyValue) {
                return new Context.DeferredOperationChain(_merger, orig, override);
            }

            return _merger.merge(orig, override);
        }
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
        keyData.setPropertyScopeKey(contextKey + "_p");
    }

    public interface PropertyMerger
    {
        Object merge (Object orig, Object override);
    }

    public void registerPropertyMerger (String propertyName, PropertyMerger merger)
    {
        PropertyManager manager = managerForProperty(propertyName);
        manager._merger = merger;
    }

    protected static class PropertyMerger_Overwrite implements PropertyMerger
    {
        public Object merge(Object orig, Object override) {
            return override;
        }
    }

    // (false trumps true) for visible and editable
    public static class PropertyMerger_And implements PropertyMerger
    {
        public Object merge(Object orig, Object override) {
            // null will reset (so that it can be overridden to true subsequently
            if (override == null) return null;
            return booleanValue(orig) && booleanValue(override);
        }
    }

    // Merge lists
    public static PropertyMerger PropertyMerger_List =  new PropertyMerger()
    {
        public Object merge(Object orig, Object override) {
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
