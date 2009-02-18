package ariba.ui.meta.core;

import ariba.util.core.Assert;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.HashMap;

/**
    A map that masks on top of an (immutable) parent map
 */
public class NestedMap<K, V> extends AbstractMap<K, V>
{
    Map<K, V>_parent;
    Map<K, Object> _map;
    int _overrideCount;

    static final Object _NullMarker = new Object();

    public NestedMap (Map<K,V> parentMap)
    {
        _parent = parentMap;
        _map = new HashMap();
    }

    private NestedMap (Map<K,V> parentMap, Map<K,Object> map)
    {
        _parent = parentMap;
        _map = map;
    }

    // You seriously need to know what you're doing before calling this...
    // In particular, the new parent should have the exact same keys present as the previous one
    public NestedMap reparentedMap (Map newParent)
    {
        NestedMap newMap = new NestedMap(newParent, _map);
        newMap._overrideCount = _overrideCount;
        return newMap;
    }

    // Essential override from AbstractMap
    public Set<Map.Entry<K,V>> entrySet ()
    {
        return new EntrySet(this);
    }

    public int size()
    {
        return _parent.size() + _map.size() - _overrideCount;
    }

    public V get(Object key)
    {
        Object val = _map.containsKey(key) ? _map.get(key) : _parent.get(key);
        return (val == _NullMarker) ? null : (V)val;
    }

    public V put(K key, V value)
    {
        Object orig = _map.get(key);
        if ((orig == _NullMarker || orig == null) && _parent.containsKey(key)) {
            _overrideCount += (orig == _NullMarker ? -1: 1);
        }
        _map.put(key, value);
        return (V)orig;
    }

    public V remove(Object key)
    {
        Object orig = null;
        if (_map.containsKey(key)) {
            orig = _map.remove(key);
            if (_parent.containsKey(key)) {
                _map.put((K)key, _NullMarker);
                // _overrideCount--;
                _overrideCount++;
            }
        }
        else if (_parent.containsKey(key)) {
            // we're "removing" a value we don't have (but that our parent does)
            // we need to store a null override
            orig = _parent.get(key);
            _map.put((K)key, _NullMarker);
            _overrideCount += 2;
        }
        return (V)orig;
    }

    public boolean containsKey (Object value)
    {
        return _map.containsKey(value) ? (_map.get(value) != _NullMarker) : _parent.containsKey(value);
    }

    public Map dup ()
    {
        NestedMap dup = new NestedMap(_parent);
        dup._map = new HashMap(_map);
        dup._overrideCount = _overrideCount;
        return dup;
    }

    private static class EntrySet extends AbstractSet
    {
        private final NestedMap _nestedMap;

        EntrySet (NestedMap map)
        {
            _nestedMap = map;
        }

        public Iterator iterator ()
        {
            return new EntryIterator(_nestedMap);
        }

        public int size ()
        {
            return _nestedMap.size();
        }
    }


    private static class EntryIterator implements Iterator
    {
        private final NestedMap _nestedMap;
        Iterator<Map.Entry> _nestedIterator;
        Iterator<Map.Entry> _parentIterator;
        Map.Entry _currentEntry;
        Map.Entry _nextEntry;
        boolean _fromNested;

        public EntryIterator (NestedMap map)
        {
            _nestedMap = map;
            _nestedIterator = _nestedMap._map.entrySet().iterator();
            _parentIterator = _nestedMap._parent.entrySet().iterator();
            advanceToNext();
        }

        void advanceToNext()
        {
            _fromNested = false;
            // Note: we need to skip nulls (masked values)
            while (!_fromNested && _nestedIterator.hasNext()) {
                _nextEntry = _nestedIterator.next();
                if (_nextEntry.getValue() != _NullMarker) _fromNested = true;
            }

            if (!_fromNested) {
                while (_parentIterator.hasNext()) {
                    _nextEntry = _parentIterator.next();
                    if (!_nestedMap._map.containsKey(_nextEntry.getKey())) return;
                }
                _nextEntry = null;
            }
        }

        public boolean hasNext()
        {
            return _nextEntry != null;
        }

        public Object next()
        {
            if (_nextEntry == null) throw new NoSuchElementException("next() when no more elements");
            _currentEntry = _nextEntry;
            advanceToNext();
            return _currentEntry;
        }

        public void remove()
        {
            Assert.that (_currentEntry != null, "Can't remove parent items from a nested map");
            _nestedMap.remove(_currentEntry.getKey());
        }
    }
}
