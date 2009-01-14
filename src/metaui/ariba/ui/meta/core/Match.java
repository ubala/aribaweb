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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Match.java#5 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWChecksum;

import java.util.List;
import java.util.Map;

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

    static class MatchResult extends Match
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
            _initMatch();
        }

        public int[] matches ()
        {
            _invalidateIfStale();
            if (_matches == null) _initMatch();
            return _matches;
        }

        int [] filter ()
        {
            return filter(_meta._rules, matches(), ~_keysMatchedMask);
        }

        public Match immutableCopy ()
        {
            _invalidateIfStale();
            return new Match(matches(), _keysMatchedMask, _matchPathCRC);
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
                _matches = intersect(_meta._rules, newArr, prevMatches,
                        keyMask, _prevMatch._keysMatchedMask);

                /* NOT NEEDED: now we use the property key as the *value* in the declare selector
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

            // compute path CRC
            _matchPathCRC = -1;
            for (MatchResult mr = this; mr != null; mr = mr._prevMatch) {
                _matchPathCRC = AWChecksum.crc32(_matchPathCRC, mr._keyData._key);
                if (mr._value != null) _matchPathCRC = AWChecksum.crc32(_matchPathCRC, mr._value.hashCode());
            }
            if (_matchPathCRC == 0) _matchPathCRC = 1;
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

        void _checkMatch (Map values, Meta meta)
        {
            int[] arr = filter();
            if (arr == null) return;
            // first entry is count
            int count = arr[0];
            for (int i=0; i < count; i++) {
                Rule r = meta._rules.get(arr[i+1]);
                r._checkRule(values, meta);
            }
        }
    }

    // only rules that use only the activated (queried) keys
    static int[] filter (List<Rule> rules, int[] arr, long notQueriedMask)
    {
        if (arr == null) return null;
        int[] result = null;
        int count = arr[0];
        for (int i=0; i < count; i++) {
            int r = arr[i+1];
            Rule rule = rules.get(r);
            if ((rule._keyMatchesMask & notQueriedMask) == 0
                    && (rule._keyAntiMask & ~notQueriedMask) == 0
                    && !rule.disabled()) {
                result = addInt(result, r);
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

    static int[] intersect (List<Rule> rules, int[] a, int[] b, long aMask, long bMask) {
        if (a == null) return b;
        int[] result = null;
        int iA = 1, sizeA = a[0], iB = 1, sizeB = b[0];
        while (iA <= sizeA || iB <=sizeB) {
            long iAMask = (iA <= sizeA) ? rules.get(a[iA])._keyMatchesMask : 0;
            long iBMask = (iB <= sizeB) ? rules.get(b[iB])._keyMatchesMask : 0;
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
}
