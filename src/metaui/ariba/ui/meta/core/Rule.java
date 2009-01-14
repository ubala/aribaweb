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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Rule.java#2 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class Rule
{
    int _id;
    int _keyMatchesMask;
    int _keyAntiMask;
    List<Predicate> _predicates;
    Map<String, Object> _properties;
    int _rank;
    Meta.RuleSet _ruleSet;
    int _lineNumber;

    public Rule (List<Predicate> predicates, Map<String, Object> properties, int rank, int lineNumber)
    {
        _predicates = predicates; // scopeNestedContraints(predicates);
        _properties = properties;
        _rank = rank;
        _lineNumber = lineNumber;
    }

    public Rule (List<Predicate> predicates, Map<String, Object> properties, int rank)
    {
        this(predicates, properties, rank, -1);
    }

    public Rule (List<Predicate> predicates, Map<String, Object> properties)
    {
        this(predicates, properties, 0, -1);
    }

    public Rule (Map predicateValues, Map properties)
    {
        this(Predicate.fromMap(predicateValues), properties, 0, -1);
    }

    // returns context keys modified
    public int apply (Meta meta, Meta.PropertyMap properties,
                      boolean isDeclare, AWDebugTrace.AssignmentRecorder recorder)
    {
        if (_rank == Integer.MIN_VALUE) return 0;

        Log.meta_detail.debug("Evaluating Rule: %s", this);
        return merge(meta, _properties, properties, isDeclare, recorder);
    }

    public static int merge (Meta meta, Map<String, Object> src, Map<String, Object> dest,
                      boolean isDeclare, AWDebugTrace.AssignmentRecorder recorder)
    {
        int updatedMask = 0;

        /* Should use Property Meta to determine merge policy... */
        for (Map.Entry entry : src.entrySet()) {
            String key = (String)entry.getKey();
            Object value = entry.getValue();
            Meta.PropertyManager propManager = meta.managerForProperty(key);
            Object orig = dest.get(key);
            Object newVal = propManager.mergeProperty(key, orig, value, isDeclare);

            if (recorder != null) recorder.registerAssignment(key, newVal);

            if (newVal != orig) {
                dest.put(key, newVal);
                Meta.KeyData keyData = propManager._keyDataToSet;
                if (keyData != null) {
                    int keymask = (1 << keyData._id);
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

    public List<Predicate> getPredicates ()
    {
        return _predicates;
    }

    public void setPredicates (List<Predicate> preds)
    {
        _predicates = preds;
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

    protected Rule createDecl ()
    {
        /*
            @field=dyno { value:${ some expr} } becomes
            declare { field:dyno }
            field=dyno { field:dyno; value:${ some expr} }
        */
        // add rule for declaration
        List <Predicate> predicates =  _predicates;
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
        if (matchPred == null) matchPred = new Predicate(declPred._key, Meta.KeyAny);

        // Mutate the predicates list to scope overrides
        predicates = collapseKeyOverrides(predicates);
        List <Predicate> prePreds = new ArrayList(predicates.subList(0, predicates.size()-1));
        declPred = predicates.get(predicates.size()-1);

        // Add property decl to main rule
        if (_properties == null) _properties = new HashMap();
        for (Predicate p : predicates) {
            _properties.put(p._key, p._value);
        }

        // The decl rule...
        prePreds.add(matchPred);
        prePreds.add(new Predicate(Meta.KeyDeclare, declPred._key));
        Map m = new HashMap();
        m.put(declPred._key, declPred._value);
        return new Rule(prePreds, m);
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
                        newVal = newVal.equals(Meta.KeyAny)
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

    void _checkRule (Map values, Meta meta)
    {
        for (Predicate p : _predicates) {
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
                Log.meta_detail.debug("Possible bad rule match!  Rule: %s; predicate: %s, context val: %s",
                        this, p, contextValue);
            }
        }
    }

    public static class Predicate
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

        public String getKey ()
        {
            return _key;
        }

        public Object getValue ()
        {
            return _value;
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

            // for description, use predicate list, minus any propertyScope (_p) key
            _description = (Meta.isPropertyScopeKey(ListUtil.lastElement(r._predicates)._key))
                    ? r._predicates.subList(0, r._predicates.size()-1).toString()
                    : r._predicates.toString();

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
    }
}
