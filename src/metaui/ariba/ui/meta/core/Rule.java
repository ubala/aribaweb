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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Rule.java#15 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
    A Rule defines a map of properties that should apply in the event that a set of Selectors
    are matched.  Given a rule base (Meta) and a set of asserted values (Context) a list of matching
    rules can be computed (by matching their selectors against the values) and by successively (in
    rank / priority order) applying (merging) their property maps a set of effective properties can
    be computed.
 */
public class Rule
{
    int _id;
    long _keyMatchesMask;
    long _keyIndexedMask;
    long _keyAntiMask;
    List<Selector> _selectors;
    Map<String, Object> _properties;
    int _rank;
    Meta.RuleSet _ruleSet;
    int _lineNumber;

    public Rule (List<Selector> selectors, Map<String, Object> properties, int rank, int lineNumber)
    {
        _selectors = selectors; // scopeNestedContraints(selectors);
        _properties = properties;

        // Todo: Temporary transition support (some Test Automation stagers are using the old "traits" property).  Will remove...
        if (_properties != null && _properties.get("traits") != null) {
            _properties.put("trait", _properties.get("traits"));
            _properties.remove("traits");
        }

        _rank = rank;
        _lineNumber = lineNumber;
    }

    public Rule (List<Selector> selectors, Map<String, Object> properties, int rank)
    {
        this(selectors, properties, rank, -1);
    }

    public Rule (List<Selector> selectors, Map<String, Object> properties)
    {
        this(selectors, properties, 0, -1);
    }

    public Rule (Map selectorValues, Map properties)
    {
        this(Selector.fromMap(selectorValues), properties, 0, -1);
    }

    public boolean matches (Meta.MatchValue[] matchArray)
    {
        for (Selector sel : _selectors) {
            if (!sel.matches(matchArray)) return false;
        }
        return true;
    }

    // returns context keys modified
    public long apply (Meta meta, Meta.PropertyMap properties,
                      String declareKey, AWDebugTrace.AssignmentRecorder recorder)
    {
        if (_rank == Integer.MIN_VALUE) return 0;

        Log.meta_detail.debug("Evaluating Rule: %s", this);
        return merge(meta, _properties, properties, declareKey, recorder);
    }

    public static long  merge (Meta meta, Map<String, Object> src, Map<String, Object> dest,
                      String declareKey, AWDebugTrace.AssignmentRecorder recorder)
    {
        long updatedMask = 0;

        /* Should use Property Meta to determine merge policy... */
        for (Map.Entry entry : src.entrySet()) {
            String key = (String)entry.getKey();
            Object value = entry.getValue();
            Meta.PropertyManager propManager = meta.managerForProperty(key);
            Object orig = dest.get(key);
            boolean isDeclare = (declareKey != null && key.equals(declareKey));
            Object newVal = propManager.mergeProperty(key, orig, value, isDeclare);

            if (recorder != null) recorder.registerAssignment(key, newVal);

            if (newVal != orig) {
                dest.put(key, newVal);
                Meta.KeyData keyData = propManager._keyDataToSet;
                if (keyData != null) {
                    long keymask = (1L << keyData._id);
                    if ((keymask & updatedMask) == 0 && (dest instanceof Meta.PropertyMap)) {
                        updatedMask |= keymask;
                        ((Meta.PropertyMap)dest).addContextKey(propManager);
                    }
                }
            }
        }

        return updatedMask;
    }

    public void disable ()
    {
        _rank = Integer.MIN_VALUE;
    }

    public boolean disabled ()
    {
        return _rank == Integer.MIN_VALUE;
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

    public List<Selector> getSelectors ()
    {
        return _selectors;
    }

    public void setSelectors (List<Selector> preds)
    {
        _selectors = preds;
    }

    public Map<String, Object> getProperties ()
    {
        return _properties;
    }

    public int getRank ()
    {
        return _rank;
    }

    public Meta.RuleSet getRuleSet ()
    {
        return _ruleSet;
    }

    public boolean isEditable ()
    {
        return (_ruleSet != null) && (_ruleSet._editableStart > 0)
                && (_id >= _ruleSet._editableStart);
    }

    // here so logging of property map doesn't infinitely recurse
    public static class Wrapper
    {
        Rule rule;

        public Wrapper (Rule rule)
        {
            this.rule = rule;
        }
    }

    protected Rule createDecl ()
    {
        /*
            @field=dyno { value:${ some expr} } becomes
            declare { field:dyno }
            field=dyno { field:dyno; value:${ some expr} }
        */
        // add rule for declaration
        List <Selector> selectors = _selectors;
        Selector declPred = selectors.get(selectors.size()-1);
        List <Selector> prePreds = convertKeyOverrides(new ArrayList(selectors.subList(0, selectors.size()-1)));

        // We give main rule each of its selectors as properties.  (This is probably a bad idea...)
        if (_properties == null) _properties = new HashMap();
        for (Selector p : selectors) {
            if (!(p._value instanceof  List)) _properties.put(p._key, p._value);
        }
        // Flag the declaring rule as a property
        _properties.put(Meta.DeclRule, new Wrapper(this));

        // check for override scope
        boolean hasOverrideScope = false;
        for (Selector p : prePreds) {
            if (p._key.equals(declPred._key)) hasOverrideScope = true;
        }

        // if decl key isn't scoped, then select on no scope
        if (!hasOverrideScope) {
            String overrideKey = Meta.overrideKeyForKey(declPred._key);
            prePreds.add(0, new Selector(overrideKey, Meta.NullMarker));
        }

        // The decl rule...
        prePreds.add(new Selector(Meta.KeyDeclare, declPred._key));
        Map m = new HashMap();
        m.put(declPred._key, declPred._value);
        return new Rule(prePreds, m);
    }
    
    // rewrite any selector of the form "layout=L1, class=c, layout=L2" to
    // "layout_o=L1 class=c, layout=L2"
    List <Selector> convertKeyOverrides (List<Selector> orig)
    {
        List result = orig;
        int count = orig.size();
        for (int i=0; i < count; i++) {
            Selector p = orig.get(i);
            // See if overridded by same key later in selector
            for (int j = i + 1; j < count; j++) {
                Selector pNext = orig.get(j);
                if (pNext._key.equals(p._key)) {
                    // if we're overridden, we drop ours, and replace the next collision
                    // with one with our prefix

                    // make a copy if we haven't already
                    if (result == orig) result = new ArrayList(orig.subList(0,i));
                    p = new Selector(Meta.overrideKeyForKey(p._key), p._value);
                    break;
                }
            }
            if (result != orig) result.add(p);
        }

        return result;
    }

    public String toString ()
    {
        return Fmt.S("<Rule [%s] %s -> %s>", _rank, _selectors.toString(), _properties.toString());
    }

    void _checkRule (Map values, Meta meta)
    {
        for (Selector p : _selectors) {
            Object contextValue = values.get(p._key);
            Meta.KeyData keyData = meta.keyData(p._key);

            if (keyData._transformer != null) contextValue = keyData._transformer.tranformForMatch(contextValue);

            if (contextValue != null &&
                       ((Meta.KeyAny.equals(p._value) && !Boolean.FALSE.equals(contextValue))
                    || Meta.objectEquals(contextValue, p._value)
                    || ((p._value instanceof List) && ((List)p._value).contains(contextValue))
                    || ((contextValue instanceof List) && ((List)contextValue).contains(p._value))))
            {
                // okay
            } else {
                Log.meta_detail.debug("Possible bad rule match!  Rule: %s; selector: %s, context val: %s",
                        this, p, contextValue);
            }
        }
    }

    /**
        A Selector defines a sort of key/value predicate that must be satisfied for a
        rule to apply.
     */
    public static class Selector
    {
        String _key;
        Object _value;
        boolean _isDecl;

        public Selector (String key, Object value)
        {
            _key = key;
            _value = value;
        }

        public Selector (String key, Object value, boolean isDecl)
        {
            this(key, value);
            _isDecl = isDecl;
        }

        static List fromMap (Map <String, Object> values)
        {
            List result = new ArrayList();
            for (Map.Entry <String, Object> entry : values.entrySet()) {
                result.add(new Selector(entry.getKey(), entry.getValue()));
            }
            return result;
        }

        public String getKey ()
        {
            return _key;
        }

        public Object getValue ()
        {
            return _value;
        }

        int _matchArrayIdx;
        Meta.MatchValue _matchValue;

        void bindToKeyData (Meta.KeyData keyData)
        {
            _matchArrayIdx = keyData._id;
            _matchValue = keyData.matchValue(_value);
        }

        public boolean matches (Meta.MatchValue[] matchArray)
        {
            // If we haven't been initialized with a matchValue, then we were indexed and don't need to match
            if (_matchValue == null) return true;
            Meta.MatchValue other = matchArray[_matchArrayIdx];
            return (other != null) ? other.matches(_matchValue) : false;
        }

        public String toString ()
        {
            return Fmt.S("%s=%s", _key, _value.toString());
        }
    }

    public static class AssignmentSource extends AWDebugTrace.AssignmentSource
    {
        Rule _rule;
        String _description;

        public AssignmentSource (Rule r)
        {
            _rule = r;

            // for description, use selector list, minus any propertyScope (_p) key
            _description = (Meta.isPropertyScopeKey(ListUtil.lastElement(r._selectors)._key))
                    ? r._selectors.subList(0, r._selectors.size()-1).toString()
                    : r._selectors.toString();

        }

        public Rule getRule ()
        {
            return _rule;
        }

        public int getRank ()
        {
            return _rule._rank;
        }

        public String getDescription ()
        {
            return _description;
        }

        public String getLocation ()
        {
            return _rule.location();  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String toString ()
        {
            return Fmt.S("Rule[%s: %s]", _rule._rank, _rule.getSelectors());
        }
    }
}
