/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/WriteResistantMap.java#1 $

    Responsible: jshultis
*/

package ariba.util.core;

import java.util.Map;

/**
 * Similar to a ReadOnlyMap, but with a method <code>getMap</code> enabling
 * the caller to get direct access to the underlying Map, which may then
 * be modified. This "hole in the firewall" should be used only with
 * extreme caution, and in circumstances where an optimized way of modifying
 * the state is jusified.
 *
 * @aribaapi private
 * @param <K> Key class
 * @param <V> Value class
 */
public class WriteResistantMap<K,V> extends ReadOnlyMap<K,V>
{
    public WriteResistantMap (Map<K,V> aMap)
    {
        super(aMap);
    }

    public static WriteResistantMap emptyInstance ()
    {
        return new WriteResistantMap(MapUtil.map());
    }

    public V get (K key)
    {
        V value = map.get(key);
        if (value instanceof Map) {
            value = (V)new WriteResistantMap((Map)value);
        }
        return value;
    }

    /**
     * Do not call this method unless you know exactly what you are doing.
     * @return The wrapped map
     * @aribaapi private
     */
    public Map<K,V> getMap ()
    {
        return map;
    }
}
