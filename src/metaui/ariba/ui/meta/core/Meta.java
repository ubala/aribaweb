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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Meta.java#39 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.GrowOnlyHashtable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

/**
    Meta is the core class in MetaUI.  An instance of meta represents a "Rule Base"
    (a repository rules), and this rule base is used to compute property maps
    based on a series of key/value constraints (typically based on the current values
    in a Context instance).

    Typically a single Meta instance is shared process wide, usually by way of multiple
    Context instances on various threads.

    Meta works in concert with Match.MatchResult to cache partial matches (match trees)
    with cached computed property maps.

    Meta is generally used by way of its subclasses ObjectMeta and UIMeta (which extend
    Meta with behaviors around auto-creating rules for references Java classes, auto-reading
    rule files associated with referenced java packages, and dynamic properties for
    field and layout zoning)
 */
public class Meta
{
    public static final String KeyAny = "*";
    public static final String KeyDeclare = "declare";
    public static final String KeyTrait = "trait";
    public static final int LowRulePriority = -100000;
    public static final int SystemRulePriority = -200000;
    public static final int ClassRulePriority = -100000;
    public static final int TemplateRulePriority = 100000;
    public static final int EditorRulePriority = 200000;
    static final int MaxKeyDatas = 64;
    static final Object NullMarker = new Object();
    protected static final String ScopeKey = "scopeKey";
    protected static final String DeclRule = "declRule";

    // PartialIndexing indexes each rule by a single (well chosen) key and evaluates
    // other parts of the selector on the index-filtered matches (generally this is a
    // win since may selectors are not selective, resulting in huge rule vectors)
    static boolean _UsePartialIndexing = true;
    static boolean _DebugDoubleCheckMatches = false;

    /**
        THREAD SAFETY Design
        A single instance of Meta is typically shared process-global, and is accessed
        concurrently by multiple threads (each with their own Context instances).
        Meta is thread-safe for reading (performing rule matches) as well as for
        incrementally adding RuleSet (as occurs, lazily, when classes / packages
        are first referenced).  Meta is *not* thread-safe for Rule Editing or Rule
        reload (rapid turnaround on .oss files) -- these activities are development-time
        only and should not involve concurrency.

        Thread safety for reads (rule matching) in the face of incremental rule additions
        is supported as follows:
          - The rule list and all rule indexes (i.e. KeyDatas, _ValueMatches) are *grow only*
                and support reading concurrent with appending
          - Updates to rules are serialized (single writer) via a lock on beginRuleSet --
            only a single RuleSet may be active at a time (and rules cannot be added without
            an active rule set)

        There are additional thread-safety considerations for Context.  See comments there
        for details.
     */

    // THREAD SAFETY: rules array is "grow only" and is replaced atomically
    // _ruleCount (size within the array) is updated upon endRuleSet();
    ReentrantLock _updateLock = new ReentrantLock();
    volatile Rule[] _rules = new Rule[128];
    volatile int _ruleCount = 0;
    RuleSet _currentRuleSet;
    int _nextKeyId = 0;
    int _ruleSetGeneration = 0;

    Map <String, KeyData> _keyData = new GrowOnlyHashtable();
    KeyData[] _keyDatasById = new KeyData[MaxKeyDatas];
    GrowOnlyHashtable<Match, PropertyMap> _MatchToPropsCache = new GrowOnlyHashtable();
    GrowOnlyHashtable<PropertyMap, PropertyMap> _PropertyMapUniquer = new GrowOnlyHashtable();
    GrowOnlyHashtable _identityCache = new GrowOnlyHashtable.IdentityMap();
    GrowOnlyHashtable <String, PropertyManager> _managerForProperty = new GrowOnlyHashtable();
    long _declareKeyMask;

    public Meta ()
    {
        _declareKeyMask = keyData(KeyDeclare).maskValue();
        registerPropertyMerger(KeyTrait, PropertyMerger_Traits);

        // Thread safety: add noop rule as rule 0 so a read into an unflushed _ValueMatches._arr entry
        // will get the noop rule
        Rule nooprule = new Rule(null, null, 0, 0);
        nooprule.disable();
        _rules[0] = nooprule;
        _ruleCount = 1;
    }

    public void addRule (Rule rule)
    {
        List <Rule.Selector> selectors =  rule._selectors;
        if (selectors.size() > 0 && selectors.get(selectors.size()-1)._isDecl) {
            Rule decl = rule.createDecl();
            _addRule(decl, true);
        }

        // we allow null to enable creation of a decl, but otherwise this rule has no effect
        if (rule._properties != null) {
            // After we've captured the decl, do the collapse
            rule._selectors = rule.convertKeyOverrides(rule._selectors);

            _addRule(rule, true);
        }
    }

    synchronized void _addToRules(Rule rule, int pos)
    {
        if (pos == _rules.length) {
            Rule[] newArr = new Rule[pos * 2];
            System.arraycopy(_rules,  0, newArr, 0, pos);
            _rules = newArr;
        }
        _rules[pos] = rule;
    }

    public void _addRule (Rule rule, boolean checkPropScope)
    {
        Assert.that(_currentRuleSet != null, "Attempt to add rule without current RuleSet");
        List <Rule.Selector> selectors =  rule._selectors;

        int entryId = _currentRuleSet.allocateNextRuleEntry();
        rule._id = entryId;
        if (rule._rank == 0) rule._rank = _currentRuleSet._rank++;
        rule._ruleSet = _currentRuleSet;

        _addToRules(rule, entryId);

        Log.meta_detail.debug("Add Rule, rank=%s: %s", Integer.toString(rule._rank), rule);

        // index it
        KeyData lastScopeKeyData = null;
        String declKey = null;
        long declMask = declareKeyMask();
        long matchMask = 0, indexedMask = 0, antiMask = 0;
        int count = selectors.size();
        Rule.Selector indexOnlySelector = _UsePartialIndexing ? bestSelectorToIndex(selectors) : null;
        for (int i=count-1; i >= 0; i--) {
            Rule.Selector p = selectors.get(i);

            boolean shouldIndex = (indexOnlySelector == null || p == indexOnlySelector);
            KeyData data = keyData(p._key);
            long dataMask = data.maskValue();
            if (p._value != NullMarker) {
                if (shouldIndex || _DebugDoubleCheckMatches) {
                    if (p._value instanceof List) {
                        for (Object v : (List)p._value) {
                            data.addEntry(v, entryId);
                        }
                    }
                    else {
                        data.addEntry(p._value, entryId);
                    }
                    if (shouldIndex) indexedMask |= (1L << data._id);
                }
                if (!shouldIndex) {
                    // prepare selector for direct evaluation
                    p.bindToKeyData(data);
                }
                matchMask |= dataMask;

                if (data.isPropertyScope() && lastScopeKeyData == null) lastScopeKeyData = data;
                if ((dataMask & declMask) != 0) declKey = (String)p._value;

            } else {
                antiMask |= dataMask;
            }
        }

        // Decls that match on a non scope key need to scope
        boolean isDecl = (declKey != null);
        boolean nonScopeKeyDecl = declKey != null && !keyData(declKey).isPropertyScope();
        if (!isDecl || nonScopeKeyDecl) {
            // all non-decl rules don't apply outside decl context
            if (!isDecl) antiMask |= declMask;

            if (lastScopeKeyData != null  && checkPropScope) {
                Object traitVal = rule._properties.get(KeyTrait);
                if (traitVal != null) {
                    String traitKey = lastScopeKeyData._key + "_trait";
                    Rule traitRule = new Rule(rule._selectors, AWUtil.map(traitKey, traitVal), rule._rank, rule._lineNumber);
                    _addRule(traitRule, false);
                }
                rule._selectors = ListUtil.copyList(selectors);
                Rule.Selector scopeSel = new Rule.Selector(ScopeKey, lastScopeKeyData._key);
                rule._selectors.add(scopeSel);
                KeyData data = keyData(ScopeKey);
                if (!_UsePartialIndexing || _DebugDoubleCheckMatches) {
                    data.addEntry(lastScopeKeyData._key, entryId);
                    indexedMask |= (1L << data._id);
                }
                scopeSel.bindToKeyData(data);
                matchMask |= (1L << data._id);
            }
        }

        rule._keyMatchesMask = matchMask;
        rule._keyIndexedMask = indexedMask;
        rule._keyAntiMask = antiMask;
    }

    Rule.Selector bestSelectorToIndex (List<Rule.Selector> selectors)
    {
        Rule.Selector best = null;
        int bestRank = Integer.MIN_VALUE;
        int pos = 0;
        for (Rule.Selector sel : selectors) {
            int rank = selectivityRank(sel) + pos++;
            if (rank > bestRank) {
                best = sel;
                bestRank = rank;
            }
        }
        return best;
    }


    int selectivityRank (Rule.Selector selector)
    {
        // Score selectors: good if property scope, key != "*" or bool
        // "*" is particularly bad, since these are inherited by all others
        int score = 1;
        Object value = selector.getValue();
        if (value != null && !KeyAny.equals(value)) score += ((value instanceof Boolean) ? 1 : 9);
        KeyData keyData = keyData(selector.getKey());
        if (keyData.isPropertyScope()) score *= 5;
        // Todo: we could score based on # of entries in KeyData
        return score;
    }
/*
    int selectivityRank_dynamic (Rule.Selector selector)
    {
        KeyData keyData = keyData(selector.getKey());
        _ValueMatches m = keyData.get(selector.getValue());
        int count = m._arr != null ? m._arr[0] : 0;
        return Integer.MAX_VALUE / (count + 1);
    }
*/
    // if addition of this rule results in addition of extra rules, those are returned
    // (null otherwise)

    int _editingRuleEnd ()
    {
        return Math.max(_currentRuleSet._end, _ruleCount);
    }

    public List<Rule> _addRuleAndReturnExtras (Rule rule)
    {
        int start = _editingRuleEnd();
        List<Rule> extras = null;

        addRule(rule);

        // Return any extra rules created by addition of this one
        for (int i=start, c=_editingRuleEnd(); i < c; i++) {
            Rule r = _rules[i];
            if (r != rule) {
                if (extras == null) extras = ListUtil.list();
                extras.add(r);
            }
        }

        return extras;
    }

    // Icky method to replace an exited rule in place
    public List<Rule> _updateEditedRule (Rule rule, List<Rule>extras)
    {
        // in place replace existing rule with NoOp
        Rule nooprule = new Rule(null, null, 0, 0);
        nooprule.disable();
        _rules[rule._id] = nooprule;

        if (extras != null) {
            for (Rule r : extras) r.disable();
        }

        // Since this rule has already been mutated (the first time it was added) we need to reverse the
        // addition of the scopeKey
        List<Rule.Selector> preds = rule.getSelectors();
        if (!ListUtil.nullOrEmptyList(preds) && ListUtil.lastElement(preds).getKey().equals(ScopeKey)) {
            ListUtil.removeLastElement(preds);
        }
        
        // now (re)-add it and invalidate
        extras = _addRuleAndReturnExtras(rule);
        invalidateRules();
        return extras;
    }

    public String scopeKeyForSelector (List<Rule.Selector> preds)
    {
        for (int i=preds.size()-1; i>=0; i--) {
            Rule.Selector pred = preds.get(i);
            KeyData data = keyData(pred._key);
            if (data.isPropertyScope()) return pred._key;
        }
        return null;
    }

    public void addRule (Map selectorMap, Map propertyMap)
    {
        addRule(selectorMap, propertyMap,  0);
    }

    public void addRule (Map selectorMap, Map propertyMap, int rank)
    {
        Rule rule = new Rule(selectorMap, propertyMap);
        if (rank != 0) rule._rank = rank;
        addRule(rule);
    }

    public void addRules (Map <String, Object> ruleSet, List selectors)
    {
        // Special keys:  "props, "rules".  Everthing else is a selector
        Map props = null;
        List <Map <String, Object>> rules = null;
        selectors = (selectors == null) ? new ArrayList() : new ArrayList(selectors);
        for (Map.Entry <String, Object> entry : ruleSet.entrySet()) {
            String key = entry.getKey();
            if (key.equals("props")) {
                props = (Map)entry.getValue();
            }
            else if (key.equals("rules")) {
                rules = (List)entry.getValue();
            }
            else {
                selectors.add(new Rule.Selector(key, entry.getValue()));
            }
        }
        if (props != null) {
            addRule(new Rule(selectors, props));
        }
        if (rules != null) {
            for (Map <String, Object> r : rules) {
                addRules(r, selectors);
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

    public void loadRules (String ruleText)
    {
        loadRules("StringLiteral", new ByteArrayInputStream(ruleText.getBytes()), 0, true);
        endRuleSet();
    }

    public static final String RuleFileDelimeter = "/*!--- Editor Generated Rules -- Content below this line will be overwritten ---*/\n\n";
    public static final String RuleFileDelimeterStart = "/*!--- ";
    boolean _RuleEditingEnabled = true;

    static class RuleLoadingException extends AWGenericException
    {
        RuleLoadingException (String message, Throwable exception)
        {
            super(message, exception);
        }
    }

    protected void _loadRules (String filename, InputStream inputStream, boolean editable)
    {
        Log.meta_detail.debug("Loading rule file: %s", filename);
        try {
            // String editRules = null, userRules = AWUtil.stringWithContentsOfInputStream(inputStream);
            String editRules = null, userRules;
            try {
                InputStreamReader isr = new InputStreamReader(inputStream, AWCharacterEncoding.UTF8.name);
                userRules = AWUtil.getString(isr);
            }
            catch (UnsupportedEncodingException unsupportedEncodingException) {
                throw new AWGenericException(unsupportedEncodingException);
            }

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
                _currentRuleSet._editableStart = _currentRuleSet._end;
                // boost rank for editor rules
                _currentRuleSet._rank = EditorRulePriority;

                if (editRules != null) {
                    new Parser(this, editRules).addRules();
                }
            }
        } catch (Error er) {
            endRuleSet().disableRules();
            throw new RuleLoadingException(Fmt.S("Error loading rule file: %s -- %s", filename, er), null);
        } catch (Exception e) {
            endRuleSet().disableRules();
            throw new RuleLoadingException(Fmt.S("Exception loading rule file: %s -- %s", filename, e.getMessage()), e);
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
        _MatchToPropsCache = new GrowOnlyHashtable();
        _PropertyMapUniquer = new GrowOnlyHashtable();
        _identityCache = new GrowOnlyHashtable.IdentityMap();
    }

    boolean isTraitExportRule (Rule rule)
    {
        if (MapUtil.nullOrEmptyMap(rule._properties) || rule._properties.size() == 1) {
            return ((String)rule._properties.keySet().toArray()[0]).endsWith("_trait");
        }
        return false;
    }

    /**
        A group of rules originating from a common source.
        All rules must be added to the rule base as part of a RuleSet.
     */
    public class RuleSet
    {
        String _filePath;
        int _start, _end, _editableStart = -1; 
        int _rank;

        public void disableRules()
        {
            for (int i=_start; i < _end; i++) {
                _rules[i].disable();
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
                Rule r = _rules[i];
                if (!r.disabled() && !isTraitExportRule(r)) {
                    result.add(r);
                }
            }
            return result;
        }

        public int startRank ()
        {
            return (_start < _ruleCount)
                    ? _rules[_start]._rank
                    : _rank - (_end - _start); 
        }

        protected int allocateNextRuleEntry ()
        {
            return (_ruleCount > _end) ? _ruleCount++ : _end++;
        }
    }

    public void beginRuleSet (String filePath)
    {
        beginRuleSet(_ruleCount, filePath);
    }

    public void beginRuleSet (int rank, String filePath)
    {
        _updateLock.lock();
        try {
            Assert.that(_currentRuleSet == null, "Can't start new rule set while one in progress");
            _currentRuleSet = new RuleSet();
            _currentRuleSet._start = _ruleCount;
            _currentRuleSet._end = _ruleCount;
            _currentRuleSet._rank = rank;
            _currentRuleSet._filePath = filePath;
        } catch (RuntimeException e) {
            _updateLock.unlock();
            throw e;
        }
    }

    public void beginReplacementRuleSet (RuleSet orig)
    {
        int origRank = orig.startRank();
        beginRuleSet(orig.filePath());
        _currentRuleSet._rank = origRank;
    }

    public RuleSet endRuleSet ()
    {
        Assert.that(_currentRuleSet != null, "No rule set progress");
        RuleSet result = _currentRuleSet;
        if (_ruleCount < result._end) _ruleCount = result._end;
        _currentRuleSet = null;
        _ruleSetGeneration++;

        _updateLock.unlock();
        return result;
    }

    // not thread safe, but only used in editor
    public void _resumeEditingRuleSet(RuleSet ruleSet)
    {
        _updateLock.lock();
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

    long declareKeyMask ()
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
        long keyMask = 1L << keyData._id;

        // Does our result already include this key?  Then no need to join again
        // if (intermediateResult != null && (intermediateResult._keysMatchedMask & keyMask) != 0) return intermediateResult;

        return new Match.MatchResult(this, keyData, value, intermediateResult);
    }

    Match.UnionMatchResult unionOverrideMatch (String key, Object value,
                             Match.UnionMatchResult intermediateResult)
    {
        KeyData keyData = _keyData.get(overrideKeyForKey(key));
        if (keyData == null) return intermediateResult;
        return new Match.UnionMatchResult(this, keyData, value, intermediateResult);
    }

    // subclasses can override to provide specialized properties Map subclasses
    protected PropertyMap newPropertiesMap ()
    {
        return new PropertyMap();
    }

    /**
        Called on implementing values to allow statically resolvable (but dynamic) values
        to evaluate/copy themselves for inclusion in a new map (to ensure that a value that
        derived its value based on a different context doesn't get reused in another)
     */
    public interface PropertyMapAwaking
    {
        Object awakeForPropertyMap (PropertyMap map);
    }

    /**
        The Map type used to accumulate the effective properties through the successive application
        of rules.
     */
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

        int[] arr = matchResult.filteredMatches();
        if (arr == null) return properties;
        // first entry is count
        int count = arr[0];
        Rule[] rules = new Rule[count];
        for (int i=0; i < count; i++) {
            rules[i] = _rules[arr[i+1]];
        }

        // sort by rank
        Arrays.sort(rules, new Comparator <Rule>() {
            public int compare(Rule o1, Rule o2) {
                // ascending
                return o1._rank - o2._rank;
            }
        });

        long modifiedMask = 0;
        String declareKey = ((_declareKeyMask & matchResult._keysMatchedMask) != 0)
                ? (String)matchResult.valueForKey(KeyDeclare) : null;
        for (Rule r : rules) {
            if (recorder != null && r._rank != Integer.MIN_VALUE) {
                recorder.setCurrentSource(new Rule.AssignmentSource(r));
            }

            modifiedMask |= r.apply(this, properties, declareKey, recorder);
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

    List<String> _keysInMask (long mask)
    {
        List<String>matches = ListUtil.list();
        int pos = 0;
        while (mask != 0) {
            if ((mask & 1) != 0) {
                matches.add(_keyDatasById[pos]._key);
            }
            pos++;
            mask >>= 1;
        }
        return matches;
    }

    public void registerKeyInitObserver (String key, ValueQueriedObserver o)
    {
        keyData(key).addObserver(o);
    }

    public void registerValueTransformerForKey (String key, KeyValueTransformer transformer)
    {
        keyData(key)._transformer = transformer;
    }

    protected GrowOnlyHashtable identityCache ()
    {
        return _identityCache;
    }

    /**
        Implemented by observers for notification of when a paricular key/value has been referenced in
        the rule base for the first time.
        Most commonly used to lazilly register additional rules upon first reference to a class.
     */
    public static interface ValueQueriedObserver
    {
        void notify (Meta meta, String key, Object value);
    }

    /**
        Used to transform values into the (static) version they should be indexed / searched under
        For instance, "object" may be indexed as true/false (present or not)
      */
    interface KeyValueTransformer {
        public Object tranformForMatch (Object o);
    }

    /**
        KeyData is the primary structure for representing information about context keys
        (e.g. "class", "layout", "operation", "field", ...), including an index of rules
        that match on particular values of that key (_ValueMatches).

        Note that every context key has a small integer ID (0-63) and these are uses in
        (long) masks for certain rule matching operations.
     */
    static class KeyData
    {
        String _key;
        int _id;
        // Thread safety: beginRuleSet lock guarentees single writer (and multiple readers)
        GrowOnlyHashtable <Object, _ValueMatches> _ruleVecs;
        List <ValueQueriedObserver> _observers;
        _ValueMatches _any;
        KeyValueTransformer _transformer;
        boolean _isPropertyScope;

        public KeyData (String key, int id)
        {
            _key = key;
            _id = id;
            _ruleVecs = new GrowOnlyHashtable();
            _any = get(KeyAny);

        }

        public long maskValue ()
        {
            return 1L << _id;
        }

        private _ValueMatches get (Object value)
        {
            if (value == null) value = NullMarker;
            else if (_transformer != null) value = _transformer.tranformForMatch(value);
            _ValueMatches matches = _ruleVecs.get(value);
            if (matches == null) {
                matches = new _ValueMatches(value);
                if (value != null && !Boolean.FALSE.equals(value)) matches._parent = _any;
                _ruleVecs.put(value, matches);
            }
            return matches;
        }

        MatchValue matchValue (Object value)
        {
            if (value instanceof List) {
                List list = (List)value;
                if (list.size() == 1) return get(list.get(0));
                Meta.MultiMatchValue multi = new Meta.MultiMatchValue();
                for (Object v : list) {
                    multi.add(get(v));
                }
                return multi;
            } else {
                return get(value);
            }
        }

        void addEntry (Object value, int id)
        {
            // Thread safety: beginRuleSet lock guarentees single writer (and multiple readers)
            // -- ops in our add are carefully sequenced work during a read
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
                // double-check logging is for turkeys, but I just can't help myself
                owner._updateLock.lock();
                try {
                    if (!matches._read) {
                        // notify
                        if (value != null) {
                            for (ValueQueriedObserver o : _observers) {
                                o.notify(owner, _key, value);
                            }
                        }
                    }
                    matches._read = true;
                } finally {
                    owner._updateLock.unlock();
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
        // this this returns the name of the selector key for those properties
        // (e.g. field_p, class_p)
        boolean isPropertyScope ()
        {
            return _isPropertyScope;
        }

        public void setIsPropertyScope (boolean yn)
        {
            _isPropertyScope = yn;
        }
    }

    // Key used to record overriden (i.e. context parent) rules
    static String overrideKeyForKey (String key)
    {
        return key + "_o";
    }

    /**
        Abstraction for values (or sets of values) that can be matched against others
        (in the context of Selector key/value) matching.  Subtypes take advantage of
        the fact that _ValueMatches instances globally uniquely represent key/value pairs
        to enable efficient matching entirely through identity comparison.
     */
    interface MatchValue {
        boolean matches (MatchValue other);
        MatchValue updateByAdding (MatchValue other);
    }

    /**
        Uniquely represents a particular key/value in the Meta scope, and indexes all rules
        with (indexed) Selectors matching that key/value.

        _ValueMatches also models *inheritance* by allowing one key/value to have another
        as its "parent" and thereby match on any Selector (and rule) that its parent would.
        For instance, this enables a rule on class=Number to apply to class=Integer and
        class=BigDecimal, and one on class=* to apply to any.
        The utility of 'parent' is not limited, of course, to the key "class": all keys
        take advantage of the parent "*" to support unqualified matches on that key, and
        keys like 'operation' define a value hierarchy ( "inspect" -> {"view", "search"},
        "search" -> {"keywordSearch", "textSearch"})
     */
    static private class _ValueMatches implements MatchValue
    {
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
                int[] parentArr = _parent._arr;
                if (parentArr != null && parentArr[0] != _parentSize) {
                    _arr = Match.union(_arr, parentArr);
                    _parentSize = parentArr[0];
                }
            }
        }

        public boolean matches (MatchValue other)
        {
            if (!(other instanceof _ValueMatches)) return other.matches(this);
            // we recurse up parent chain to do superclass matches
            return (other == this)
                || (_parent != null && _parent.matches(other));
        }

        public MatchValue updateByAdding (MatchValue other)
        {
            MultiMatchValue multi = new MultiMatchValue();
            multi.add(this);
            return multi.updateByAdding(other);
        }
    }

    static class MultiMatchValue extends ArrayList<MatchValue> implements MatchValue
    {
        public boolean matches (MatchValue other)
        {
            if (other instanceof MultiMatchValue) {
                // list / list comparison: any combo can match
                for (MatchValue v : this) {
                    if (other.matches(v)) return true;
                }
            }
            else {
                // single value against array: one must match
                for (MatchValue v: this) {
                    if (v.matches(other)) return true;
                }
            }
            return false;
        }

        public MatchValue updateByAdding (MatchValue other)
        {
            if (other instanceof MultiMatchValue) {
                addAll((MultiMatchValue)other);
            } else {
                add(other);
            }
            return this;
        }
    }

    /**
     * Allocate new matchArray to be used in matching against rule Selectors
     * @return uninitialized array
     */
    MatchValue[] newMatchArray ()
    {
        return new MatchValue[_nextKeyId];
    }

    void matchArrayAssign(MatchValue[] array, KeyData keyData, MatchValue matchValue)
    {
        int idx = keyData._id;
        MatchValue curr = array[idx];
        if (curr != null) {
            matchValue = curr.updateByAdding(matchValue);
        }
        array[idx] = matchValue;
    }

    public static final KeyValueTransformer Transformer_KeyPresent = new KeyValueTransformer()
    {
        public Object tranformForMatch(Object o) {
            return (o != null && !(o.equals(Boolean.FALSE))) ? Boolean.TRUE : Boolean.FALSE;
        }
    };

    /**
        Wrapper for a value that should, in rule application, override any previous value for its
        property.  This can be used to override default property value merge policy, for instance
        allowing the "visible" property to be forced from false to true.
     */
    public static class OverrideValue
    {
        Object _value;
        public OverrideValue (Object value) { _value = value; }
        public Object value() { return _value; }
        public String toString () { return (_value != null ? _value.toString() : "null") + "!"; }
    }

    /**
        Store of policy information for particular properties -- most significantly, how
        successive values of this property are to be *merged* during rule application.
        (See Meta.registerPropertyMerger).  E.g. 'visible', 'trait', and 'valid' all have unique
        merge policies.
     */
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
        keyData.setIsPropertyScope(true);

        String traitKey = contextKey + "_trait";
        mirrorPropertyToContext(traitKey, traitKey);
        registerPropertyMerger(traitKey, PropertyMerger_Traits);
    }

    public static boolean isPropertyScopeKey (String key)
    {
        return ScopeKey.equals(key);
    }

    /**
        Define policy for merging a property value assigned by one rule
        to a subsequent value from a higher ranked rule.
     */
    public interface PropertyMerger
    {
        /**
         * Called during rule application to merge an earlier (lower ranked) value with a newer one.
         * @param orig the previous value accumulated in the property map
         * @param override the new value from the higher ranked rule
         * @param isDeclare whether we are currently accumulating matched for declarations of the property/value
         * @return the new property value to be put in the property map
         */
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

    /**
        PropertyMerger implementing AND semantics -- i.e. false trumps true.
        (Used, for instance, for the properties 'visible' and 'editable')
     */
    public static class PropertyMerger_And implements PropertyMerger, PropertyMergerDynamic, PropertyMergerIsChaining
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            // null will reset (so that it can be overridden to true subsequently
            if (override == null) return null;

            // If we can evaluate statically, do it now
            if (((orig instanceof Boolean) && !((Boolean)orig).booleanValue())
                || ((override instanceof Boolean) && !((Boolean)override).booleanValue()))
                    return false;

            // ANDing with true is a noop -- return new value
            if ((orig instanceof Boolean) && ((Boolean)orig)) {
                return (override instanceof PropertyValue.Dynamic) ? override : booleanValue(override);
            }
            if ((override instanceof Boolean) && ((Boolean)override)) {
                return (orig instanceof PropertyValue.Dynamic) ? orig : booleanValue(orig);
            }

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

    /**
        PropertyMerger for properties the should be unioned as lists
     */
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

    public static Meta.PropertyMerger PropertyMerger_DeclareList =  new PropertyMergerDeclareList();

    /**
        PropertyMerger for properties the should override normally, but return lists when
        in declare mode (e.g. "class", "field", "layout", ...)
     */
    public static class PropertyMergerDeclareList implements PropertyMergerDynamic
    {
        public Object merge (Object orig, Object override, boolean isDeclare) {
            if (!isDeclare) return override;

            // if we're override a single element with itself, don't go List...
            if (!(orig instanceof List) && !(override instanceof List)
                    && Meta.objectEquals(orig, override)) {
                return orig;
            }

            List result = new ArrayList();
            ListUtil.addElementsIfAbsent(result, Meta.toList(orig));
            ListUtil.addElementsIfAbsent(result, Meta.toList(override));
            return result;
        }
    };
    
    /**
        PropertyMerger for the 'trait' property.  Generally, traits are unioned, except for traits
        from the same "traitGroup", which override (i.e. only one trait from each traitGroup should
        survive).
     */
    public PropertyMerger PropertyMerger_Traits =  new PropertyMergerDeclareList()
    {
        public Object merge(Object orig, Object override, boolean isDeclare) {
            if (isDeclare) return super.merge(orig, override, isDeclare);

            // if we're override a single element with itself, don't go List...
            if (!(orig instanceof List) && !(override instanceof List)
                    && objectEquals(orig, override)) {
                return orig;
            }

            List<Object> origL = toList(orig);
            List<Object> overrideL = toList(override);
            List result = ListUtil.list();
            for (Object trait : origL) {
                if (trait instanceof OverrideValue) trait = ((OverrideValue)trait).value();
                boolean canAdd = true;
                String group = groupForTrait((String)trait);
                if (group != null) {
                    for (Object overrideTrait: overrideL) {
                        if (overrideTrait instanceof OverrideValue) overrideTrait = ((OverrideValue)overrideTrait).value();
                        if (group.equals(groupForTrait((String)overrideTrait))) {
                            canAdd = false;
                            break;
                        }
                    }
                }
                if (canAdd) result.add(trait);
            }
            ListUtil.addElementsIfAbsent(result, overrideL);
            return result;
        }
    };

    // overridden by ObjectMeta
    String groupForTrait (String trait)
    {
        return "default";
    }

    public static void addTraits(List traits, Map map)
    {
        List current = (List)map.get(KeyTrait);
        if (current == null) {
            map.put(KeyTrait, new ArrayList(traits));
        } else {
            current.addAll(traits);
        }
    }

    public static void addTrait(String trait, Map map)
    {
        List current = (List)map.get(KeyTrait);
        if (current == null) {
            map.put(KeyTrait, AWUtil.list(trait));
        } else {
            current.add(trait);
        }
    }

    static class _KeyValueCount { String key; Object value; int count; }

    public void _logRuleStats ()
    {
        StringWriter strWriter = new StringWriter();
        PrintWriter out = new PrintWriter(strWriter);
        List<_KeyValueCount> counts = ListUtil.list();
        int total = 0;
        for (KeyData keyData : _keyData.values()) {
            for (_ValueMatches vm : keyData._ruleVecs.values()) {
                _KeyValueCount kvc = new _KeyValueCount();
                kvc.key = keyData._key;
                kvc.value = vm._value;
                kvc.count = (vm._arr != null) ? vm._arr[0] : 0;
                total += kvc.count;
                counts.add(kvc);
            }
        }

        Collections.sort(counts, new Comparator<_KeyValueCount>() {
            public int compare(_KeyValueCount o1, _KeyValueCount o2) {
                return o2.count - o1.count;
            }
        });

        int c = Math.min(10, counts.size());
        out.printf("Total index entries comparisons performed: %d\n", Match._Debug_ElementProcessCount);
        out.printf("Total index entries: %d\n\n", total);
        out.printf("Top %d keys/values\n", c);
        for (int i=0; i<c; i++) {
            _KeyValueCount kvc = counts.get(i);
            out.printf("     %s = %s : %d entries\n", kvc.key,  kvc.value, kvc.count);
        }
        Log.meta_detail.debug(strWriter.toString());
    }
    
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
