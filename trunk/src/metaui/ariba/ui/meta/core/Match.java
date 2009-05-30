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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Match.java#14 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWChecksum;
import ariba.util.core.Assert;

import java.util.List;
import java.util.Map;

/**
    Represents a set of matching rules resulting from looking up a set of key/values
    against the Meta rule base.

    Instances of the Match superclass are simply immutable snapshots of previous matches
    (used as keys in Match -> Properties lookup caches).
    The more meaty class is its static inner subclass, Match.MatchResult.
 */
class Match
{
    static final int[] EmptyMatchArray = {0};

    long _keysMatchedMask;
    int[] _matches;
    long _matchPathCRC = 0;

    protected Match () {}

    protected Match (int[] matches, long keysMatchedMask, long matchPathCRC)
    {
        _keysMatchedMask = keysMatchedMask;
        _matches = matches;
        _matchPathCRC = matchPathCRC;
    }

    // Hash implementation so we can cache properties by MatchResult
    public int hashCode() {
        long ret = _keysMatchedMask * 31 + _matchPathCRC;
        if (_matches != null) {
            for (int i=0, c=_matches[0]; i<c; i++) {
                ret = AWChecksum.crc32(ret, _matches[i+1]);
            }
        }
        return (int)ret;
    }

    public boolean equals(Object o) {
        Match other = (Match)o;
        return (_keysMatchedMask == other._keysMatchedMask) &&
                 _matchPathCRC == other._matchPathCRC &&
                 _arrayEq(_matches, other._matches);
    }

    public String debugString ()
    {
        return super.toString();
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

    /**
        An Match which includes a UnionMatchResult part (which is used by Context to
        represent the set of overridden key/values up the stack)
     */
    static class MatchWithUnion extends Match
    {
        UnionMatchResult _unionMatch;

        protected MatchWithUnion() { }

        protected MatchWithUnion(int[] matches, long keysMatchedMask, long matchPathCRC,
                                      UnionMatchResult over)
        {
            super(matches,  keysMatchedMask, matchPathCRC);
            _unionMatch = over;
        }

        public int hashCode()
        {
            return super.hashCode() ^ ((_unionMatch != null) ? _unionMatch.hashCode() : 0);
        }

        public boolean equals(Object o) {
            return super.equals(o)
                    && ((_unionMatch == ((MatchWithUnion)o)._unionMatch)
                        || ((_unionMatch != null) && ((MatchWithUnion)o)._unionMatch != null
                              && _unionMatch.equals(((MatchWithUnion)o)._unionMatch)));
        }
    }


    /**
        MatchResult represents the result of computing the set of matching rules
        based on the key/value on this instance, and the other key/value pairs
        on its predecessor chain.  I.e. to find the matching rules for the context keys
           {operation=edit; layout=Inspect; class=Foo}, first a MatchResult is created for
        "operation=edit" and passed as the "prev" to the creation of another for "layout=Inspect",
        and so on.  In this way the MatchResults form a *(sharable) partial-match tree.*
        The ability to result previous partial match "paths" is an important optimization:
        the primary client of MatchResult (and of rule matching in general) is the Context, when each
        assignment pushes a record on a stack that (roughly) extends the Match from the previous
        assignment.  By caching MatchResult instances on the _Assignment records, matching is limited
        to the *incremental* matching on just the new assignment, not a full match on all keys in the
        context.

        Further, a MatchResult caches the *property map* resulting from the application of the rules
        that it matches.  By caching MatchResult objects (and caching the map from
        Rule vector (AKA Match) -> MatchResult -> PropertyMap), redudant rule application (and creation of
        additional property maps) is avoided.
     */
    static class MatchResult extends MatchWithUnion
    {
        Meta _meta;
        Meta.KeyData _keyData;
        Object _value;
        Meta.PropertyMap _properties;
        MatchResult _prevMatch;
        int _metaGeneration;

        public MatchResult (Meta meta, Meta.KeyData keyData, Object value, MatchResult prev)
        {
            _meta = meta;
            _keyData = keyData;
            _value = value;
            _prevMatch = prev;
            _unionMatch = (_prevMatch != null) ? _prevMatch._unionMatch : null;
            _initMatch();
        }

        void setOverridesMatch (UnionMatchResult over)
        {
            _unionMatch = over;
        }
        
        public int[] matches ()
        {
            _invalidateIfStale();
            if (_matches == null) _initMatch();
            return _matches;
        }

        int [] filter ()
        {
            return filter(_meta._rules, _meta._ruleCount, matches(), _keysMatchedMask, null);
        }

        /**
         * Fill in matchArray with MatchValues to use in Selector matching
         * @param matchArray
         */
        void initMatchValues (Meta.MatchValue[] matchArray)
        {
            if (_prevMatch != null) _prevMatch.initMatchValues(matchArray);
            if (_unionMatch != null) _unionMatch.initMatchValues(matchArray);
            _meta.matchArrayAssign(matchArray, _keyData, _keyData.matchValue(_value));
        }

        public int [] filteredMatches ()
        {
            // shouldn't this be cached?!?
            int[] matches = matches();
            long keysMatchedMask = _keysMatchedMask | (_unionMatch != null ? _unionMatch._keysMatchedMask : 0);
            int [] overrideMatches;
            if (_unionMatch != null && ((overrideMatches = _unionMatch.matches()) != null)) {
                if (matches == null) {
                    matches = overrideMatches;
                }
                else {
                    matches = intersect(_meta._rules, matches, overrideMatches,
                            _keysMatchedMask, _unionMatch._keysMatchedMask);
                }
            }

            Meta.MatchValue[] matchArray = null;
            if (Meta._UsePartialIndexing) {
                matchArray = _meta.newMatchArray();
                initMatchValues(matchArray);
            }
            
            return filter(_meta._rules, _meta._ruleCount, matches, keysMatchedMask, matchArray);
        }

        public Object valueForKey (String key)
        {
            return (_keyData._key.equals(key)) ? _value
                    : (_prevMatch != null ? _prevMatch.valueForKey(key) : null);
        }

        public Match immutableCopy ()
        {
            _invalidateIfStale();
            return new MatchWithUnion(matches(), _keysMatchedMask, _matchPathCRC, _unionMatch);
        }

        void _invalidateIfStale ()
        {
            if (_metaGeneration < _meta.ruleSetGeneration()) {
                _initMatch();
            }
        }

        protected int[] join (int[] a, int[] b, long aMask, long bMask)
        {
            return intersect(_meta._rules, a, b,
                        aMask, bMask);
        }

        protected void _initMatch ()
        {
            long keyMask = 1L << _keyData._id;

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
            _keysMatchedMask =  (_prevMatch == null) ? keyMask : (keyMask | _prevMatch._keysMatchedMask);
            if (prevMatches == null) {
                _matches = newArr;
                // Todo: not clear why this is needed, but without it we end up failing to filter
                // certain matches that should be filtered (resulting in bad matches)
                if (!Meta._UsePartialIndexing) _keysMatchedMask = keyMask;
            }
            else {
                if (newArr == null) {
                    newArr = EmptyMatchArray;
                }
                // Join
                _matches = join(newArr, prevMatches, keyMask, _prevMatch._keysMatchedMask);
            }

            // compute path CRC
            _matchPathCRC = -1;
            for (MatchResult mr = this; mr != null; mr = mr._prevMatch) {
                _matchPathCRC = AWChecksum.crc32(_matchPathCRC, mr._keyData._key);
                if (mr._value != null) _matchPathCRC = AWChecksum.crc32(_matchPathCRC, mr._value.hashCode());
            }
            if (_matchPathCRC == 0) _matchPathCRC = 1;
            _metaGeneration = _meta.ruleSetGeneration();
            _properties = null;
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
                    Log.meta.debug("  -- Only in A: " + _meta._rules[a[iA]]);
                    iA++;
                }
                else {
                    Log.meta.debug("  -- Only in B: " + _meta._rules[b[iB]]);
                    iB++;
                }
            }
        }

        public Meta.PropertyMap properties ()
        {
            _invalidateIfStale();
            if (_properties == null) {
                _properties = _meta.propertiesForMatch(this, null);
            }
            return _properties;
        }

        public String debugString ()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Match Result path: \n");
            _appendPrevPath(buf);

            if (_unionMatch != null) {
                buf.append("\nOverrides path: ");
                _unionMatch._appendPrevPath(buf);
            }
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

        void _checkMatch (Map values, Meta meta)
        {
            int[] arr = filter();
            if (arr == null) return;
            // first entry is count
            int count = arr[0];
            for (int i=0; i < count; i++) {
                Rule r = meta._rules[arr[i+1]];
                r._checkRule(values, meta);
            }
        }
    }

    static class UnionMatchResult extends MatchResult
    {
        public UnionMatchResult (Meta meta, Meta.KeyData keyData, Object value, MatchResult prev)
        {
            super(meta, keyData, value, prev);
        }

        protected int[] join (int[] a, int[] b, long aMask, long bMask)
        {
            return union(a, b);
        }
    }

    /**
        Filter a partially matched set of rules down to the actual matches.
        The input set of rules, matchesArr, is based on a *partial* match, and so includes rules that were
        touched by some of the queried keys, but that may also require *additional* keys that we
        have not matched on -- these must now be removed.
        Also, when "partial indexing", rules are indexed on a subset of their keys, so matchesArr
        will contain rules that need to be evaluated against those MatchValues upon which they
        were not indexed (and therefore not intersected / filtered on in the lookup process).
    */
    int[] filter (Rule[] allRules, int maxRule, int[] matchesArr, long queriedMask, Meta.MatchValue[] matchArray)
    {
        if (matchesArr == null) return null;
        int[] result = null;
        int count = matchesArr[0];
        for (int i=0; i < count; i++) {
            int r = matchesArr[i+1];
            if (r >= maxRule) continue;
            Rule rule = allRules[r];
            if (rule.disabled() || (rule._keyAntiMask & queriedMask) != 0) continue;
            // Must have matched on (activate) all match keys for this rule, *and*
            // if have any non-indexed rules, need to check match on those
            if (((rule._keyMatchesMask & ~queriedMask) == 0)
                    && ((rule._keyMatchesMask == rule._keyIndexedMask)
                        || (matchArray != null && rule.matches(matchArray))))
            {
                if (Meta._DebugDoubleCheckMatches && !(matchArray != null && rule.matches(matchArray))) {
                    Assert.that(false, "Inconsistent (negative) match on rule: %s", rule);
                }
                result = addInt(result, r);
            } else if (Meta._DebugDoubleCheckMatches && (matchArray != null && rule.matches(matchArray))) {
                    // Assert.that(false, "Inconsistent (positive) match on rule: %s", rule);
            }
        }
        return result;
    }

    // only rules that use only the activated (queried) keys
    static int[] filterMustUse (List<Rule> rules, int[] arr, int usesMask)
    {
        if (arr == null) return null;
        int[] result = null;
        int count = arr[0];
        for (int i=0; i < count; i++) {
            int r = arr[i+1];
            Rule rule = rules.get(r);
            if ((rule._keyMatchesMask & usesMask) != 0) {
                result = addInt(result, r);
            }
        }
        return result;
    }

    public static int _Debug_ElementProcessCount = 0;

    /**
     * Intersects two rulevecs.  This is not a traditional intersection where only items in both
     * inputs are included in the result: we only intersect rules that match on common keys;
     * others are unioned.
     *
     * For instance, say we have the following inputs:
     *      a:  [matched on: class, layout]  (class=Foo, layout=Inspect)
     *          1) class=Foo layout=Inspect { ... }
     *          2) class=Foo operation=edit { ... }
     *          3) layout=Inspect operation=view { ... }
     *
     *      b:  [matched on: operation]  (operation=view)
     *          3) layout=Inspect operation=view { ... }
     *          4) operation=view type=String { ... }
     *          5) operation=view layout=Tabs { ... }
     *
     * The result should be: 1, 3, 4
     * I.e.: items that appear in both (#3 above) are included, as are items that appear in just one,
     * *but don't match on the keys in the other* (#1 and #4 above).
     *
     * @param allRules the full rule base
     * @param a first vector of rule indexes
     * @param b second vector of rule indexes
     * @param aMask mask indicating the keys against which the first rule vectors items have already been matched
     * @param bMask mask indicating the keys against which the second rule vectors items have already been matched
     * @return rule vector for the matches
     */
    static int[] intersect (Rule[] allRules, int[] a, int[] b, long aMask, long bMask) {
        if (a == null) return b;
        int[] result = null;
        int iA = 1, sizeA = a[0], iB = 1, sizeB = b[0];
        _Debug_ElementProcessCount += sizeA + sizeB;

        while (iA <= sizeA || iB <=sizeB) {
            long iAMask = (iA <= sizeA) ? allRules[a[iA]]._keyIndexedMask : 0;
            long iBMask = (iB <= sizeB) ? allRules[b[iB]]._keyIndexedMask : 0;
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
        int sizeA = a[0], sizeB = b[0];
        if (sizeA == 0) return b;
        if (sizeB == 0) return a;
        _Debug_ElementProcessCount += sizeA + sizeB;

        int[] result = null;
        int iA = 1, vA=a[1], iB = 1, vB=b[1];
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
}
