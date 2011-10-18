/*
    Copyright 1996-2010 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/SetUtil.java#16 $
*/

package ariba.util.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.util.Collection;

/**
    Set Utilities. These are helper functions for dealing with
    sets.

    @aribaapi ariba
*/
public final class SetUtil
{
    //--------------------------------------------------------------------------
    // constants

    /**
        An immutable, empty <code>SortedSet</code>.
    */
    public static final SortedSet EmptySortedSet;

    //--------------------------------------------------------------------------
    // static initializer

    static {
        EmptySortedSet = new EmptySortedSetImpl();
    }

    // -------------------------------------------------------------------------
    // Constructor

    /*
        To prevent creating this class
    */
    private SetUtil ()
    {}


    // -------------------------------------------------------------------------
    // Utility methods to retrieve the default set implementation

    /**
        Constructs an empty optionally type-safe <code>Set</code>.<p>

        To construct a type-safe <code>Set</code>:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.set()</code>
        </ul>
        For a raw <code>Set</code>:<ul>
        <li><code>Set raw = SetUtil.set()</code>
        </ul>
        There will be no compile warnings either way.

        @return new empty Set
        @aribaapi ariba
    */
    public static <T> Set<T> set ()
    {
        return new HashSet<T>();
    }

    /**
        Constructs a Set capable of holding a specified number
        of elements. <p/>

        To construct a type-safe <code>Set</code>:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.set(10)</code>
        </ul>
        For a raw <code>Set</code>:<ul>
        <li><code>Set raw = SetUtil.set(10)</code>
        </ul>
        There will be no compile warnings either way.

        @param initialCapacity the number of elements this Set
        is capable of holding before needing to grow.
        @return new empty Set with given initial capacity
        @aribaapi ariba
    */
    public static <T> Set<T> set (int initialCapacity)
    {
        return new HashSet<T>(initialCapacity);
    }


    /**
        Creates new Set with the same contents as unique elements in the given Collection.
        <p/>

        Given that <code>setOfX</code> is a <code>Set&lt;X></code>,
        <code>setOfY</code> is a <code>Set&lt;Y></code> (and that <code>Y</code>
        extends <code>X</code>) and
        <code>raw</code> is a <code>Set</code>, the following
        all compile validly. <ul>
        <li><code>Set&lt;X> typesafe = SetUtil.set(setOfX)</code>
        <li><code>Set&lt;X> typesafe = SetUtil.&lt;X>set(setOfY)</code>
        <li><code>Set&lt;?> unknown = SetUtil.set(setOfX)</code>
        </ul>
        The following constructs compile with an unchecked warning:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.set(raw)</code>
        <li><code>Set raw = SetUtil.set(setOfX)</code>
        </ul>

        @param source source collection
        @return new Set with same contents as <code>source</code>
        @exception NullPointerException  if the specified Set is null.
        @aribaapi ariba
    */
    public static <T> Set<T> set (Collection<? extends T> source)
    {
        return new HashSet<T>(source);
    }

    /**
        Clones and returns the specified <code>set</code> if the actual
        dynamic type of <code>set</code> supports clone (that is, implements
        <code>Cloneable</code> and has a publicly available <code>clone()</code>
        method.) <p>

        If you just want a <code>java.util.Set</code>, then you're better off using
        {@link #set(java.util.Collection)}, since it's guaranteed to work and is probably
        just as fast on small sets. <p>

        @param set the <code>Set</code> to clone
        @return a new Set of the same type

        @aribaapi ariba
    */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> cloneSet (Set<? extends T> set)
    {
        return (Set<T>)ClassUtil.clone(set);
    }

    @SuppressWarnings("unchecked")
    public static final Class ImmutableSetClass =
        Collections.unmodifiableSet(set()).getClass();

    /**
        Returns a version of the Set that is not modifiable
        If s is null, null will be returned. <p/>

        Given that <code>setOfX</code> is a <code>Set&lt;X></code>,
        <code>setOfY</code> is a <code>Set&lt;Y></code> (and that <code>Y</code>
        extends <code>X</code>) and
        <code>raw</code> is a <code>Set</code>, the following
        all compile validly. <ul>
        <li><code>Set&lt;X> typesafe = SetUtil.immutableSet(setOfX)</code>
        <li><code>Set&lt;X> typesafe = SetUtil.&lt;X>immutableSet(setOfY)</code>
        <li><code>Set&lt;?> unknown = SetUtil.immutableSet(setOfX)</code>
        </ul>
        The following constructs compile with an unchecked warning:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.immutableSet(raw)</code>
        <li><code>Set raw = SetUtil.immutableSet(setOfX)</code>
        </ul>

        @return new Set with same contents as <code>s</code> but is not
                modifiable
        @aribaapi ariba
    */
    public static <T> Set<T> immutableSet (Set<? extends T> s)
    {
            // Do this check to support general routines that protect
            // whatever set they are returning, such as return
            // Set.immutableSet(getFoo()); where the value being
            // passed may be null
        if (s == null) {
            return null;
        }

        if (s.getClass() == ImmutableSetClass) {
            /* 
               This is a safe cast, because the returned set is immutable.
               In general it would not be safe to cast a Set<U> to Set<T> where
               U extends T (as someone could add a T) but in this case it's okay.
             */
            return (Set<T>)s;
        }
        return Collections.unmodifiableSet(s);
    }

    /**
        Creates an empty SortedSet. <p/>

        To construct a type-safe <code>Set</code>:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.sortedSet()</code>
        </ul>
        For a raw <code>Set</code>:<ul>
        <li><code>Set raw = SetUtil.set()</code>
        </ul>
        There will be no compile warnings either way.

        @return new empty SortedSet
        @see java.util.SortedSet
        @aribaapi documented
    */
    public static <T> SortedSet<T> sortedSet ()
    {
        return new TreeSet<T>();
    }

    /**
        Creates an empty SortedSet <p/>

        Given that <code>compX</code> is a <code>Comparator&lt;X></code>,
        <code>compY</code> is a <code>Comparator&lt;Y></code> (and that <code>Y</code>
        extends <code>X</code>) and <code>comp</code> is a raw
        <code>Comparator</code>, the following all compile validly. <ul>
        <li><code>Set&lt;X> typesafe = SetUtil.sortedSet(compX)</code>
        <li><code>Set&lt;X> typesafe = SetUtil.&lt;X>sortedSet(compY)</code>
        <li><code>Set&lt;?> unknown = SetUtil.immutableSet(compX)</code>
        </ul>
        The following constructs compile with an unchecked warning:<ul>
        <li><code>Set&lt;X> typesafe = SetUtil.immutableSet(comp)</code>
        <li><code>Set raw = SetUtil.immutableSet(compX)</code>
        </ul>

        @param comparator the non-null comparator that will be used to
               sort the <code>Set</code>
        @return new empty SortedSet
        @see java.util.SortedSet
        @aribaapi documented
    */
    public static <T> SortedSet<T> sortedSet (Comparator<? super T> comparator)
    {
        return new TreeSet<T>(comparator);
    }

    /**
        Creates a new {@link SortedSet} with the same content as the given
        <code>SortedSet</code>. <p/>

        Given that <code>setOfX</code> is a <code>SortedSet&lt;X></code>,
        <code>setOfY</code> is a <code>SortedSet&lt;Y></code> (and that <code>Y</code>
        extends <code>X</code>) and <code>raw</code> is a raw
        <code>SortedSet</code>, the following all compile validly. <ul>
        <li><code>SortedSet&lt;X> typesafe = SetUtil.sortedSet(setOfX)</code>
        <li><code>SortedSet&lt;X> typesafe = SetUtil.&lt;X>sortedSet(setOfY)</code>
        <li><code>SortedSet&lt;?> unknown = SetUtil.sortedSet(setOfX)</code>
        </ul>
        The following constructs compile with an unchecked warning:<ul>
        <li><code>SortedSet&lt;X> typesafe = SetUtil.sortedSet(raw)</code>
        <li><code>SortedSet raw = SetUtil.sortedSet(setOfX)</code>
        </ul>

        @param source the non-null sorted set based on the given SortedSet
        @return SortedSet containing the elements of the source
        @see java.util.SortedSet
        @aribaapi documented
    */
    public static <T> SortedSet<T> sortedSet (SortedSet<? extends T> source)
    {
        return new TreeSet<T>(source);
    }

   /**
        Creates a <code>Set</code> from the objects in the given array.<p/>

        Note, the elements in the array are NOT copied.<p>

        @param array to use as the initial contents of the <code>Set</code>; may
                not be <code>null</code>
        @param assertIfDuplicateElementFound if <code>true</code> the method
               asserts if an duplicated element is found in the input array
               otherwise it will be ignored
        @return Set containing objects from the specified array

        @aribaapi ariba
    */
    public static <T> Set<T> arrayToSet (
        T[] array,
        boolean  assertIfDuplicateElementFound
    )
    {
        Set<T> result = SetUtil.set(array.length);
        for (int i=0; i<array.length; i++) {
            T e = array[i];
            boolean isNotDuplicate = result.add(e);
            if (assertIfDuplicateElementFound) {
                Assert.that(isNotDuplicate, "Object %s is duplicated in array", e);
            }
        }
        return result;
    }

    /**
        Returns new Set with all the items in source Collection added to it. Combines
        SetUtil.list() and Set addAll(Collection). Throws NullPointerException if source
        is null.
        @aribaapi ariba
    */
    public static <T> Set<T> setAddAll (Collection<T> source)
    {
        Set<T> set = SetUtil.set(source.size());
        set.addAll(source);
        return set;
    }

    // -------------------------------------------------------------------------
    // Public Methods

    /**
        Determine if a Set is null or empty.<p>

        @param set object to check
        @return <b>true</b> if <B>set</B> is null or empty,
        <b>false</b> otherwise
        @aribaapi ariba
    */
    public static boolean nullOrEmptySet (Set<?> set)
    {
        return (set == null) || set.isEmpty();
    }

   /**
        Retrieve a random element from set.<p>

        Note that the set can contain null element (at most one).<p>

        @param set object from which to retrieve an element
        @return any element of <code>set</code> if it's not null or empty,
                assert otherwise.
        @aribaapi ariba
    */
    public static <T> T getAnyElement (Set<T> set)
    {
        Assert.that(!SetUtil.nullOrEmptySet(set), "The set is empty");
        return set.iterator().next();
    }

    // -------------------------------------------------------------------------
    // Operations on sets

    /**
        Perform the operation intersect on two given sets. The operation is
        nondestructive.<p>

        If one of the sets is null or empty the method returns an empty set.<p>

        @param set1 input set
        @param set2 input set
        @return the intersect of the given sets.
        @aribaapi ariba
    */
    public static <T> Set<T> intersect (Set<? extends T> set1, Set<? extends T> set2)
    {
        Set<T> s = set();
        intersect(set1, set2, s);
        return s;
    }

    /**
        Perform the operation intersect on two given sets. The operation is
        nondestructive.<p>

        If one of the sets is null or empty, nothing would be added to the
        result set.<p>

        @param set1 input set
        @param set2 input set
        @param result result set
        @throw IllegalArgumentException when result is <code>null</code>.
        @aribaapi ariba
     */
    public static <T> void intersect (
            Set<? extends T> set1,
            Set<? extends T> set2,
            Set<T> result)
    {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");            
        }

        if (set1 == null || set2 == null ||
            set1.isEmpty() || set2.isEmpty())
        {
            return;
        }

        Set<? extends T> iterateThru = null;
        Set<? extends T> check       = null;
        if (set1.size() > set2.size()) {
            iterateThru = set2;
            check       = set1;
        }
        else {
            iterateThru = set1;
            check       = set2;
        }

        for (Iterator<? extends T> it=iterateThru.iterator(); it.hasNext();) {
            T o = it.next();
            if (check.contains(o)) {
                result.add(o);
            }
        }
    }

    /**
        Performs the operation subtract set2 from set1. The operation
        is nondestructive.<p>

        @param set1 input set
        @param set2 input set
        @return set containing the elements of the operation set1 - set2. If
                set1 is null or empty the method returns an empty set. If on
                the other hand set2 is null or empty the method returns a copy
                of set1.
        @aribaapi ariba
    */
    public static <T> Set<T> subtract (Set<? extends T> set1, Set<? extends T> set2)
    {
        Set<T> s = set();
        subtract(set1, set2, s);
        return s;
    }

    /**                                  
        Performs the operation subtract set2 from set1. The operation
        is nondestructive.<p>

        @param set1 input set
        @param set2 input set
        @param result result set (containing the elements of the operation
               set1 - set2.  If set1 is null or empty, no element would be added
               to it. If on the other hand set2 is null or empty, all the elements
               in set1 would be added to it).
        @throw IllegalArgumentException when result is <code>null</code>.
        @aribaapi ariba
    */
    public static <T> void subtract (
            Set<? extends T> set1,
            Set<? extends T> set2,
            Set<T> result)
    {
        if (result == null) {
            throw new IllegalArgumentException("result must not be null");
        }

        if (set1 == null || set1.isEmpty()) {
            return;
        }

        if (set2 == null || set2.isEmpty()) {
            result.addAll(set1);
            return;
        }

        for (T t : set1) {
            if (!set2.contains(t)) {
                result.add(t);
            }
        }
    }

    /**
        Returns a new Set containing all the elements from two sets

        @param set1 input set
        @param set2 input set
        @return the union of the given sets.
        @aribaapi ariba
    */
    public static <T> Set<T> union (Set<? extends T> set1, Set<? extends T> set2)
    {
        if (set1 == null) {
            return SetUtil.set(set2);
        }

        if (set2 == null) {
            return SetUtil.set(set1);
        }

        Set<T> s = set(set1.size() + set2.size());
        s.addAll(set1);
        s.addAll(set2);
        return s;
    }

    @SuppressWarnings("unchecked")
    public static <T> SortedSet<T> emptySortedSet ()
    {
        return (SortedSet<T>)EmptySortedSet;
    }

    //--------------------------------------------------------------------------
    // nested class

    /**
        Private implementation of an empty <code>SortedSet</code>.
        @aribaapi private
    */
    private static class EmptySortedSetImpl<T>
    extends AbstractSet<T>
    implements SortedSet<T>
    {
        //----------------------------------------------------------------------
        // data members

        private Comparator<T> _comparator;

        //----------------------------------------------------------------------
        // constructors

        /**
            @aribaapi private
        */
        public EmptySortedSetImpl (Comparator<T> comparator)
        {
            _comparator = comparator;
        }

        /**
            @aribaapi private
        */
        public EmptySortedSetImpl ()
        {
            this(null);
        }

        //----------------------------------------------------------------------
        // public methods

        /**
            @aribaapi private
        */
        public Iterator<T> iterator ()
        {
            return Collections.<T>emptySet().iterator();
        }

        /**
            @aribaapi private
        */
        public int size ()
        {
            return 0;
        }

        /**
            @aribaapi private
        */
        public Comparator<T> comparator ()
        {
            return _comparator;
        }

        /**
            @aribaapi private
        */
        public SortedSet<T> subSet (Object fromElement, Object toElement)
        {
            return emptySortedSet();
        }

        /**
            @aribaapi private
        */
        public SortedSet<T> headSet (Object toElement)
        {
            return emptySortedSet();
        }

        /**
            @aribaapi private
        */
        public SortedSet<T> tailSet (Object fromElement)
        {
            return emptySortedSet();
        }

        /**
            @aribaapi private
        */
        public T first ()
        {
            throw new NoSuchElementException();
        }

        /**
            @aribaapi private
        */
        public T last ()
        {
            throw new NoSuchElementException();
        }
    }
}

