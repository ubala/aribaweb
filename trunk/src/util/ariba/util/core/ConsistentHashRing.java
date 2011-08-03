/*
    Copyright (c) 2010-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ConsistentHashRing.java#4 $

    Responsible: cwwilkinson
*/
package ariba.util.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The Consistent Hash Ring is a data structure that is used to
 * load balance resources across several server nodes.  This set of
 * nodes can change over time, so by using a consistent hashing function, the
 * resources can be balanced and as node membership changes, the location of resources
 * does not need to be recomputed.
 * The "ring" aspect of this structure is to divide a circle in to n arcs, one arc per
 * node instance.  Resources are then hashed into the arcs.  As the number of arcs changes,
 * the position on the ring of a resource does not change, just the node that owns the arc
 * containing the resource.
 * <p/>
 * The nodes are replicated multiple times on the ring, so that there is a more uniform
 * distribution of resources per node, and also have a uniform distribution as nodes are
 * removed from the ring.
 * <p/>
 * The selection of the hashing function is important, and should be stable for each key
 * (meaning re-hashing of the same key always returns the same hash value).  The hash values
 * calculated should also be uniformly distributed.
 *
 * @param <T>  - The type of nodes to maintain on the ring.
 */
public class ConsistentHashRing<T>
{

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Long, T> ring = new TreeMap<Long, T>();


    /**
     * The HashFunction interface is used to implement the hashing function for the keys.
     */
    public interface HashFunction
    {
        public long hash (Object key);
    }

    /**
     * Constructs a new hash ring using MD5 hash function.
     *
     * @param numberOfReplicas - the number of times each server should be hashed onto the
     *                         ring.
     * @param nodes            - the initial collection of nodes to hash onto the ring.
     */
    public ConsistentHashRing (int numberOfReplicas, Collection<T> nodes)
    {
        this(DefaultHashFunction, numberOfReplicas, nodes);
    }

    /**
     * Constructs a new hash ring.
     *
     * @param hashFunction     - the hashing function for the keys
     * @param numberOfReplicas - the number of times each server should be hashed onto the
     *                         ring.
     * @param nodes            - the initial collection of nodes to hash onto the ring.
     */
    public ConsistentHashRing (HashFunction hashFunction, int numberOfReplicas,
                               Collection<T> nodes)
    {

        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        if (nodes != null) {
            for (T node : nodes) {
                add(node);
            }
        }
    }

    /**
     * Adds a node to the ring.
     *
     * @param node
     */
    public void add (T node)
    {
        long hcode;

        hcode = hashFunction.hash(node.toString());
        synchronized (ring) {
            ring.put(hcode, node);
        }
        for (int i = 0; i < numberOfReplicas; i++) {
            // serialize access to the ring, but allow access to this class itself,
            //since calculating the hash can happen concurrently.
            hcode = hashFunction.hash(node.toString() + i);
            synchronized (ring) {
                ring.put(hcode, node);
            }
        }
    }

    /**
     * Removes a node from the ring
     *
     * @param node
     */
    public void remove (T node)
    {
        long hcode;

        if (node == null) {
            return;
        }
        hcode = hashFunction.hash(node.toString());
        synchronized (ring) {
            ring.remove(hcode);
        }
        for (int i = 0; i < numberOfReplicas; i++) {
            // serialize access to the ring, but allow access to this class itself,
            //since calculating the hash can happen concurrently.
            hcode = hashFunction.hash(node.toString() + i);
            synchronized (ring) {
                ring.remove(hcode);
            }
        }
    }

    /**
     * Returns the node that is responsible for the given resource key.
     *
     * @param key - the key of the resource to map onto the ring.
     * @return the node which owns the resource, or null if the ring is empty.
     */
    public T get (Object key)
    {
        T retval = null;

        if (!ring.isEmpty() && hashFunction != null) {

            long hash = hashFunction.hash(key);

            synchronized (ring) {
                if (!ring.containsKey(hash)) {
                    // get the keys greater or equal to hash
                    SortedMap<Long, T> tailMap = ring.tailMap(hash);
                    // return the first greatest, or the first of the entire map, if we
                    // need to wrap.
                    hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
                }
                retval = ring.get(hash);
            }
        }
        return retval;
    }

    /**
     * Returns the collection of values in the ring.
     *
     * @return Collection of T
     */
    public Collection<T> values ()
    {
        return ring.values();
    }

    private static final HashFunction DefaultHashFunction = new MD5HashFunction();

    /**
     * A MD5 hash function which is a good alternative for uniform hashing.
     */
    public static class MD5HashFunction implements HashFunction
    {
        public long hash (Object key)
        {
            long val = 0;
            if (key != null) {
                try {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] bytes = md.digest(key.toString().getBytes());
                    // taking the hi bytes in reverse order gives a good distribution
                    for (int i = 15; i >= 8; i--) {
                        val = (val << 8) | (0x0ff & bytes[i]);
                    }
                }
                catch (NoSuchAlgorithmException e) {
                    Assert.fail(
                          e.getMessage() == null ?
                                e.getClass().getName() : e.getMessage());
                }
            }
            return val;
        }
    }
}

