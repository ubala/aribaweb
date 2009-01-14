/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTSortOrdering.java#18 $
*/
package ariba.ui.table;

import java.util.List;

import ariba.util.core.ListUtil;
import ariba.util.core.Sort;
import ariba.util.core.Compare;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.fieldvalue.FieldPath;
import ariba.ui.aribaweb.util.AWUtil;

// subclassed by ARWTGroupSortOrdering
public class AWTSortOrdering
{
    public static final int CompareAscending = 0;
    public static final int CompareDescending = 1;
    public static final int CompareCaseInsensitiveAscending = 2;
    public static final int CompareCaseInsensitiveDescending = 3;

    protected String _key;
    protected FieldPath _fieldPath;
    protected int _selector;
    protected Compare _comparator;

    // params for serialize/deserialize
    private static final String ClassName = AWTSortOrdering.class.getName();
    protected static GrowOnlyHashtable SerializeHelperTable = new GrowOnlyHashtable();

    static {
        SerializeHelperTable.put(ClassName, new AWTSortOrderingSerializeHelper());
    }

    public AWTSortOrdering (String key, int selector)
    {
        _key = key;
        _fieldPath = FieldPath.sharedFieldPath(_key);
        _selector = selector;
    }

    // Just for subclasses who override getSortValue()
    protected AWTSortOrdering (int selector)
    {
        _selector = selector;
    }

    public String key ()
    {
        return _key;
    }

    public int selector ()
    {
        return _selector;
    }

    public void setSelector (int selector)
    {
        _selector = selector;
    }

    // Optional comparator to override sort behavior
    public void setComparator (Compare c)
    {
        _comparator = c;
    }

    public Compare comparator ()
    {
        return _comparator;
    }

    // equals -- really means matches key, not direction

    public boolean equals(Object other) {
        if (this.getClass() != other.getClass()) return false;
        String otherKey = ((AWTSortOrdering)other).key();
        return (otherKey == _key) || (_key != null && otherKey != null && _key.equals(otherKey));
    }

    //*********************************************************************************
    // Serialize / Deserialize sort orderings
    //*********************************************************************************

    public String serialize ()
    {
        return serializeHelper(getClassName()).serialize(this);
    }

    /**
     * Should be overridden by any subclasses which have specific requirements on
     * deserialization / AWTSortOrdering creation.  Otherwise, after the AWTSortOrdering
     * (sub)class is serialized, the value will be deserialized by the deserialize method
     * found below which creates a basic AWTSortOrdering.
     * @aribaapi private
     */
    protected String getClassName ()
    {
        return ClassName;
    }

    /**
     * Cache to map class name to deserialization method to avoid calls to reflection.
     * @param className
     * @return
     */
    private static AWTSortOrderingSerializeHelper serializeHelper (String className)
    {
        return (AWTSortOrderingSerializeHelper)SerializeHelperTable.get(className);
    }

    /**
     * Given a serialized AWTSortOrdering (or subclass) and an instance of the datatable
     * for which the serialized sort ordering is defined, this method will lookup the
     * the correct AWTSortOrdering (sub)class and invoke the
     * deserialize(List, AWTDataTable) and returning a AWTSortOrdering instance.
     *
     * @param serializedOrdering - serialized AWTSortOrdering (or subclass) as returned
     *        by serialize method.
     * @param dt - current AWTDataTable instance
     * @return AWTSortOrdering as appropriate for the serializedOrdering and the
     *         current datatable.  null if no valid sort ordering can be constructed from
     *         the serialized ordering String.
     * @aribaapi private
     */
    public static AWTSortOrdering deserialize (String serializedOrdering,
                                               AWTDataTable dt)
    {
        AWTSortOrdering ordering = null;
        String className =
            AWTSortOrderingSerializeHelper.getClassNameFromString(serializedOrdering);
        // invoke any static initialization
        Class cl = AWUtil.classForName(className);
        if (cl != null) {
            AWTSortOrderingSerializeHelper serializeHelper = serializeHelper(className);
            if (serializeHelper != null) {
                ordering = serializeHelper.deserialize(serializedOrdering,dt);
            }
        }

        return ordering;
    }


    //*********************************************************************************

    protected Object getSortValue (Object o)
    {
        return _fieldPath.getFieldValue(o);
    }

    public int compare (Object o1, Object o2)
    {
        // we need a proper, extensible (class extension based) mechanism, but for now
        // here's a hard-coded hack...
        Object v1 = getSortValue(o1);
        Object v2 = getSortValue(o2);
        return compareValues(v1, v2);
    }

    public static int basicCompare (Object v1, Object v2)
    {
        int order = 0;
        if (v1 == v2) {
            order = 0;
        }
        else if (v1 == null) {
            order = -1;
        }
        else if (v2 == null) {
            order = 1;
        }
        else if (v1 instanceof String) {
            order = ((String)v1).compareToIgnoreCase((String)v2);
        }
        else if (v1 instanceof Number) {
            double d1 = ((Number)v1).doubleValue(), d2 = ((Number)v2).doubleValue();
            order = (d1 == d2) ? 0 : ((d1 < d2) ? -1 : 1);
        }
        // covers Date, most numbers, ...
        else if (v1 instanceof Comparable) {
            order = ((Comparable)v1).compareTo(v2);
        }
        else if (v1 instanceof Boolean) {
            boolean b1 = ((Boolean)v1).booleanValue();
            boolean b2 = ((Boolean)v2).booleanValue();
            order = (b1 == b2) ? 0 : ((b1 == true) ? 1 : -1);
        }
        else {
            // we don't know how to sort these, so lets at least group "==" objects together
            int h1 = v1.hashCode(), h2 = v2.hashCode();
            order = (h1 == h2) ? 0 : ((h1 < h2) ? -1 : 1);
        }

        return order;
    }

    public int compareValues (Object v1, Object v2)
    {
        int order = (_comparator != null) ? _comparator.compare(v1, v2) : basicCompare(v1, v2);
        return handleOrdering(order);
    }

    protected int handleOrdering (int order)
    {
        if ((_selector == CompareDescending) ||
            (_selector == CompareCaseInsensitiveDescending))
        {
            order = (order < 0) ? 1 : ((order > 0) ? -1 : 0);
        }
        return order;
    }

    public static AWTSortOrdering sortOrderingWithKey (String key, int selector)
    {
        return new AWTSortOrdering(key, selector);
    }

    public static List sortedArrayUsingKeyOrderArray (List array,
                                                      List sortOrderings)
    {
        Object[] objects = array.toArray();
        Sort.objects(objects, new Comparator(sortOrderings));

        // Create result vector (but don't copy the array)
        List result = ListUtil.arrayToList(objects, false);

        return result;
    }

    protected static class Comparator implements Compare
    {
        List _orderings;

        public Comparator (List orderings)
        {
            _orderings = orderings;
        }

        public int compare (Object o1, Object o2)
        {
            int result = 0;
            for (int i=0, count=_orderings.size(); i<count; i++) {
                result = ((AWTSortOrdering)_orderings.get(i)).compare(o1, o2);
                if (result != 0) {
                    break;
                }
            }
            return result;
        }
    }

    static protected class AWTSortOrderingSerializeHelper
    {
        protected static final String SerializeDelimiter = "|";
        protected static final int ClassPos = 0;
        protected static final int KeyPos = 1;
        protected static final int SelectorPos = 2;

        /**
         * Format is pipe delimited | values.  First value should be the fully qualified
         * class name of the AWTSortOrdering (sub)class.
         * @aribaapi private
         */
        public String serialize (AWTSortOrdering so)
        {
            return Fmt.S("%s|%s|%s",
                         so.getClassName(), so.key(), String.valueOf(so.selector()));
        }

        /**
         * Returns an instance of AWTSortOrdering.
         * @param serializedSortOrdering
         * @param dt
         * @return AWTSortOrdering
         * @aribaapi private
         */
        public AWTSortOrdering deserialize (String serializedSortOrdering,
                                            AWTDataTable dt)
        {
            List skeys =
                StringUtil.stringToStringsListUsingBreakChars(serializedSortOrdering,
                                                              SerializeDelimiter);

            String key = (String)skeys.get(KeyPos);
            int selector = Integer.parseInt((String)skeys.get(SelectorPos));
            return new AWTSortOrdering(key,selector);
        }

        public static String getClassNameFromString (String serializedSortOrdering)
        {
            List skeys =
                StringUtil.stringToStringsListUsingBreakChars(serializedSortOrdering,
                                                              SerializeDelimiter);

            // backward compatibility with existing sort orderings -- just ignore
            if (skeys.size() < 3)
                return null;

            return (String)skeys.get(ClassPos);
        }
    }
}

