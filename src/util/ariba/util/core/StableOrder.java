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

    $Id: //ariba/platform/util/core/ariba/util/core/StableOrder.java#5 $
*/

package ariba.util.core;

import java.util.List;
import java.util.Comparator;

/**
   This is a class that persists a particular order of elements. It will be able to
   ensure that new elements are only added at the end of the existing ones. The
   order is persisted between server startups. The persistence is realm-aware based
   on the current session's realm.
   
   @aribaapi ariba
*/
public abstract class StableOrder
{
    private static StringRepresentation defaultRepresentation =
            new StringRepresentation() {
                public String asString (Object object)
                {
                    return object == null ? null : object.toString();
                }
            }
            ;


    /**
        Stable order for lists of strings
        @see StableOrder#stableOrder(List, StringRepresentation)
        @aribaapi ariba
    */
    public List stableOrder (List/*<String>*/ original)
    {
        return stableOrder (original, defaultRepresentation);
    }

    /**
         Return the original list in the order (named by the orderKey)
         any subsequent call to this method with this name results in the same
         order. This method is expected to be slow. Any new elements in the list
         will be added to the end of the resultant list (and the persisted order)
         in the order they appear in the original list. So:
         stableOrder({"a", "d", "e"}) -> {"a","d","e"}
         stableOrder({"d", "a", "e"}) -> {"a","d","e"}
         stableOrder({"d", "b", "a", "c", "e"}) -> {"a", "d", "e", "b", "c"}

         Use this for small lists (hundreds) not large lists (thousands)

         Note: duplicate entries in the original list will be removed!

         @param original Original list
         @param representation Used to map objects to strings (uniquely) (or null to use toString)
         @return Sorted list
         @aribaapi ariba
    */
    public abstract List stableOrder (List original,
                                      StringRepresentation representation);


    /**
         Returns the list of ordered elements (the string representations)
         contained in this order.

         @return the list of string elements being ordered in their order
     */
    public abstract List/*<String>*/ getElements ();

    /**
        Re-set the order
        @aribaapi ariba
    */
    public abstract void clear ();

    /**
        Force the order to be the same order as the passed in List

        @param newOrder Order
        @aribaapi ariba
    */
    public void setOrder (List/*<String>*/ newOrder)
    {
        setOrder(newOrder, defaultRepresentation);
    }

    /**
        Force the order to be the same order as the passed in List

        @param newOrder Order
        @param representation a mapping to map the element objects to strings
        @aribaapi ariba
    */
    public abstract void setOrder (List newOrder,
                                   StringRepresentation representation);


    private static StableOrderFactory factory;

    public static void setFactory (StableOrderFactory theFactory)
    {
        factory = theFactory;
    }

    /**
        Get an instance of a stable order. The instance is
        realm-specific.
        @param name A name for the order
        @return An object that manages the named ordering
        @aribaapi ariba
    */
    public static StableOrder getOrder (String name)
    {
        return factory == null ? null : factory.getOrder(name);
    }

    /**
     * @aribaapi ariba
     */
    public static interface StringRepresentation
    {
        /**
            Return a string representation of the object. If the string representation
            of two objects are the same, they are assumed to be the same.

            @param object An object in the list
            @return string representation that is persisted in the database
         */
        public String asString (Object object);
    }

    /**
     * @aribaapi ariba
     */
    protected static interface StableOrderFactory
    {
        public StableOrder getOrder (String name);
    }
}
