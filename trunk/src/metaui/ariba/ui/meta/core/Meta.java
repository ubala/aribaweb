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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Meta.java#11 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWChecksum;
import ariba.ui.aribaweb.util.AWDebugTrace;
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
import java.util.Iterator;


public class Meta
{
    public static final String KeyAny = "*";
    public static final String KeyDeclare = "declare";
    public static final int LowRulePriority = -100000;
    public static final int SystemRulePriority = -200000;
    public static final int ClassRulePriority = -100000;
    public static final int TemplateRulePriority = 100000;
    static final int MaxKeyDatas = 32;
    static final Object NullMarker = new Object();
    protected static final String ScopeKeySuffix = "_p";

    Map <String, KeyData> _keyData = new HashMap();
    KeyData[] _keyDatasById = new KeyData[MaxKeyDatas];
    int _nextKeyId = 0;
    List <Rule> _rules = new ArrayList();
    // Todo: NOT THREAD SAFE!
    Map <ImmutableMatchResult, PropertyMap> _MatchToPropsCache = new HashMap();
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
        List <Predicate> predicates =  rule._predicates;
        if (predicates.size() > 0 && predicates.get(predicates.size()-1)._isDecl) {
            addDecl(rule);
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
            Predicate p = predicates.get(i);

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
            rule._predicates.add(new Predicate(lastScopeKey, true));
            KeyData data = keyData(lastScopeKey);
            data.addEntry(true, entryId);
            mask |= (1 << data._id);
        }

        rule._id = entryId;
        rule._keyMatchesMask = mask;
        rule._keyAntiMask = antiMask;
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
        Predicate declPred = predicates.get(predicates.size()-1);

        // see if we have a colliding preceding contraint assignment
        Predicate matchPred = null;
        for (int i=predicates.size()-2; i >=0; i--) {
            Predicate p = predicates.get(i);
            if (p._key.equals(declPred._key)) {
                matchPred = p;
                break;
            }
        }
        if (matchPred == null) matchPred = new Predicate(declPred._key, KeyAny);

        // Mutate the predicates list to scope overrides
        predicates = rule.collapseKeyOverrides(predicates);
        List <Predicate> prePreds = new ArrayList(predicates.subList(0, predicates.size()-1));
        declPred = predicates.get(predicates.size()-1);

        // The decl rule...
        prePreds.add(matchPred);
        prePreds.add(new Predicate(KeyDeclare, declPred._key));
        Map m = new HashMap();
        m.put(declPred._key, declPred._value);
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
        _PropertyMapUniquer = new HashMap();
        _identityCache = new IdentityHashMap();
    }

    public class RuleSet
    {
        String _filePath;
        int _start, _end, _rank;

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
        return result;
    }

    // Can be compared to detect if rule set has been updated (due to incremental loading)
    public int ruleSetGeneration ()
    {
        return _rules.size();
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

    MatchResult match (String key, Object value, MatchResult intermediateResult)
    {
        KeyData keyData = _keyData.get(key);
        if (keyData == null) return intermediateResult;
        int keyMask = 1 << keyData._id;

        // Does our result already include this key?  Then no need to join again
        if (intermediateResult != null && (intermediateResult._keysMatchedMask & keyMask) != 0) return intermediateResult;

        return new MatchResult(this, keyData, value, intermediateResult);
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

    PropertyMap propertiesForMatch (MatchResult matchResult, AWDebugTrace.AssignmentRecorder recorder)
    {

        PropertyMap properties = _MatchToPropsCache.get(matchResult);
        if (properties != null && recorder == null) return properties;


        properties = newPropertiesMap();

        int[] arr = filter(matchResult.matches(), ~matchResult._keysMatchedMask);
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
            if (recorder != null) {
                // for description, use predicate list, minus any propertyScope (_p) key
                String desc = (ListUtil.lastElement(r._predicates)._key.endsWith(ScopeKeySuffix))
                        ? r._predicates.subList(0, r._predicates.size()-1).toString()
                        : r._predicates.toString();
                recorder.setCurrentSource(r._rank, desc, r.location());
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

    void _checkMatch (MatchResult matchResult, Map values)
    {
        int[] arr = filter(matchResult.matches(), ~matchResult._keysMatchedMask);
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

            if (contextValue != null &&
                       ((KeyAny.equals(p._value) && !Boolean.FALSE.equals(contextValue))
                    || objectEquals(contextValue, p._value)
                    || ((p._value instanceof List) && ((List)p._value).contains(contextValue))
                    || ((contextValue instanceof List) && ((List)contextValue).contains(p._value))))
            {
                // okay
            } else {
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
    int[] filter (int[] arr, int notQueriedMask)
    {
        if (arr == null) return null;
        int[] result = null;
        int count = arr[0];
        for (int i=0; i < count; i++) {
            int r = arr[i+1];
            Rule rule = _rules.get(r);
            if ((rule._keyMatchesMask & notQueriedMask) == 0
                    && (rule._keyAntiMask & ~notQueriedMask) == 0 ) {
                result = addInt(result, r);
            }
        }
        return result;
    }

    // only rules that use only the activated (queried) keys
    int[] filterMustUse (int[] arr, int usesMask)
    {
        if (arr == null) return null;
        int[] result = null;
        int count = arr[0];
        for (int i=0; i < count; i++) {
            int r = arr[i+1];
            Rule rule = _rules.get(r);
            if ((rule._keyMatchesMask & usesMask) != 0) {
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

    protected static class ImmutableMatchResult
    {
        int _keysMatchedMask;
        int[] _matches;

        protected ImmutableMatchResult () {}

        public ImmutableMatchResult (int[] matches, int keysMatchedMask)
        {
            _keysMatchedMask = keysMatchedMask;
            _matches = matches;
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
            ImmutableMatchResult other = (ImmutableMatchResult)o;
            return (_keysMatchedMask == other._keysMatchedMask) &&
                     _arrayEq(_matches, other._matches);
        }
    }

    static final int[] EmptyMatchArray = {0};
    
    protected static class MatchResult extends ImmutableMatchResult
    {
        Meta _meta;
        KeyData _keyData;
        Object _value;
        PropertyMap _properties;
        MatchResult _prevMatch;
        int _metaGeneration;

        public MatchResult (Meta meta, KeyData keyData, Object value, MatchResult prev)
        {
            _meta = meta;
            _keyData = keyData;
            _value = value;
            _prevMatch = prev;
            _initMatch();
        }

        public int[] matches ()
        {
            _invalidateIfStale();
            if (_matches == null) _initMatch();
            return _matches;
        }

        public ImmutableMatchResult immutableCopy ()
        {
            _invalidateIfStale();
            return new ImmutableMatchResult(matches(), _keysMatchedMask);
        }

        void _invalidateIfStale ()
        {
            if (_metaGeneration < _meta.ruleSetGeneration()) {
                _initMatch();
                _metaGeneration = _meta.ruleSetGeneration();
                _properties = null;
            }
        }

        protected void _initMatch ()
        {
            int keyMask = 1 << _keyData._id;

            // get vec for this key/value -- if value is list, compute the union
            int[] newArr = null;
            if (_value instanceof List) {
                for (Object v : (List)_value) {
                    int a[] = _keyData.lookup(_meta, v);
                    newArr = union(a, newArr);
                }
            }
            else {
                newArr = _keyData.lookup(_meta, _value);
            }

            int[] prevMatches = (_prevMatch == null) ? null : _prevMatch.matches();
            if (prevMatches == null) {
                _matches = newArr;
                _keysMatchedMask = keyMask;
            }
            else {
                if (newArr == null) {
                    newArr = EmptyMatchArray;
                }
                // Join
                _matches = _meta.intersect(newArr, prevMatches,
                        keyMask, _prevMatch._keysMatchedMask);

                /* NOT NEEDED: now we use the property key as the *value* in the declare predicate
                // if this is a Declare match, then force match on the last property
                if (keyMask == _meta.declareKeyMask()) {
                    MatchResult prev = _prevMatch;
                    // first nearest property scope key
                    while (prev != null && prev._keyData._propertyScopeKey == null) prev = prev._prevMatch;
                    if (prev != null) {
                        int[] filtered = _meta.filterMustUse(_matches, prev._keyData.maskValue());
                        if (filtered[0] != _matches[0]) {
                            System.out.println("*** Filtered decl rules for must use: "
                                + prev._keyData._key + "  " + debugString());
                            _logMatchDiff(filtered, _matches);
                            _matches = filtered;
                        }
                    }
                }
                */
                _keysMatchedMask =  keyMask | _prevMatch._keysMatchedMask;
            }
        }

        void _logMatchDiff(int[] a, int[] b)
        {
            int iA = 1, sizeA = a[0], iB = 1, sizeB = b[0];
            while (iA <= sizeA || iB <=sizeB) {
                int c = (iA > sizeA ? 1 : (iB > sizeB ? -1
                        : (a[iA] - b[iB])));
                if (c == 0) {
                    iA++; iB++;
                } else if (c < 0) {
                    // If A not in B, but A doesn't filter on B's mask, then add it
                    System.out.println("  -- Only in A: " + _meta._rules.get(a[iA]));
                    iA++;
                }
                else {
                    System.out.println("  -- Only in B: " + _meta._rules.get(b[iB]));
                    iB++;
                }
            }
        }

        public PropertyMap properties ()
        {
            if (_properties == null) {
                _properties = _meta.propertiesForMatch(this, null);
            }
            return _properties;
        }

        public String debugString ()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Match Result path: ");
            _appendPrevPath(buf);
            return buf.toString();
        }

        void _appendPrevPath (StringBuffer buf) {
            if (_prevMatch != null) {
                _prevMatch._appendPrevPath(buf);
                buf.append(" -> ");
            }
            buf.append(_keyData._key);
            buf.append("=");
            buf.append(_value);
        }
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
                    _arr = union(_arr, _parent._arr);
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

    static class Rule
    {
        int _id;
        int _keyMatchesMask;
        int _keyAntiMask;
        List <Predicate> _predicates;
        Map <String, Object> _properties;
        int _rank;
        RuleSet _ruleSet;
        int _lineNumber;

        public Rule (List predicates, Map properties, int rank, int lineNumber)
        {
            _predicates = predicates; // scopeNestedContraints(predicates);
            _properties = properties;
            _rank = rank;
            _lineNumber = lineNumber;
        }

        public Rule (List predicates, Map properties, int rank)
        {
            this(predicates, properties, rank, -1);
        }

        public Rule (List predicates, Map properties)
        {
            this(predicates, properties, 0, -1);
        }

        public Rule (Map predicateValues, Map properties)
        {
            this(Predicate.fromMap(predicateValues), properties, 0, -1);
        }

        // returns context keys modified
        public int apply (Meta meta, PropertyMap properties,
                          boolean isDeclare, AWDebugTrace.AssignmentRecorder recorder)
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
                Object newVal = propManager.mergeProperty(key, orig, value, isDeclare);
                if (newVal != orig) {
                    properties.put(key, newVal);

                    if (recorder != null) recorder.registerAssignment(key, newVal);

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

            Log.meta_detail.debug("Evaluating Rule: %s", this);
            return updatedMask;
        }

        void disable ()
        {
            _rank = Integer.MIN_VALUE;
        }

        public int getLineNumber ()
        {
            return _lineNumber;
        }

        public void setLineNumber (int lineNumber)
        {
            _lineNumber = lineNumber;
        }

        String location ()
        {
            String path = (_ruleSet != null) ? _ruleSet.filePath() : "Unknown";
            return (_lineNumber >= 0) ? Fmt.S("%s:%s", path, _lineNumber) : path;
        }

        // rewrite any predicate of the form "layout=l1, class=c, layout=l2" to
        // "class=c, layout=l1_l2"
        List <Predicate> collapseKeyOverrides (List<Predicate> orig)
        {
            List result = orig;
            int count = orig.size();
            int collapsed = 0;
            for (int i=0; i < count; i++) {
                Predicate p = orig.get(i);
                boolean hide = false;
                // See if overridded by same key later in predicate                
                for (int j = i + 1; j < count; j++) {
                    Predicate pNext = orig.get(j);
                    if (pNext._key.equals(p._key)) {
                        // if we're overridden, we drop ours, and replace the next collision
                        // with one with our prefix

                        // make a copy if we haven't already
                        if (result == orig) result = new ArrayList(orig.subList(0,i));
                        hide = true;
                        break;
                    }
                }
                if (result != orig && !hide) result.add(p);
            }

            return result;
        }

        // Alias values scoped by a parent assignment on the same key
        //  E.g. "layout=l1, class=c, layout=l2" to
        // "layout=l1, class=c, layout=l1_l2"
        List <Predicate> scopeNestedContraints (List<Predicate> orig)
        {
            List result = orig;
            int count = orig.size();
            for (int i=count-1; i > 0; i--) {
                Predicate p = orig.get(i);
                if (p._value instanceof String) {
                    String newVal = (String)p._value;
                    // See if overridded by same key later in predicate
                    for (int j = i - 1; j >= 0; j--) {
                        Predicate pPrev = orig.get(j);
                        if (pPrev._key.equals(p._key) && pPrev._value instanceof String) {
                            newVal = newVal.equals(KeyAny)
                                ? (String)pPrev._value
                                : pPrev._value + "_" + newVal;
                        }
                    }
                    if (newVal != p._value) {
                        // make a copy if we haven't already
                        if (result == orig) result = new ArrayList(orig);
                        Predicate pReplacement = new Predicate(p._key, newVal, p._isDecl);
                        result.set(i, pReplacement);
                    }
                }
            }

            return result;
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

    public static class OverrideValue
    {
        Object _value;
        public OverrideValue (Object value) { _value = value; }
        public String toString () { return _value.toString() + "(!)"; }
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
                 (orig instanceof Context.DynamicPropertyValue || newValue instanceof Context.DynamicPropertyValue)) {
                return new Context.DeferredOperationChain(_merger, orig, newValue);
            }

            return _merger.merge(orig, newValue, isDeclare);
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
        keyData.setPropertyScopeKey(contextKey.concat(ScopeKeySuffix));
    }

    public interface PropertyMerger
    {
        Object merge (Object orig, Object override, boolean isDeclare);
    }

    // marker interface for PropertyMerges that can handle dynamic values
    public interface PropertyMergerDynamic extends PropertyMerger { }

    public void registerPropertyMerger (String propertyName, PropertyMerger merger)
    {
        PropertyManager manager = managerForProperty(propertyName);
        manager._merger = merger;
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
    public static class PropertyMerger_And implements PropertyMerger
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            // null will reset (so that it can be overridden to true subsequently
            if (override == null) return null;
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
