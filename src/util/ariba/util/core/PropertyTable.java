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

    $Id: //ariba/platform/util/core/ariba/util/core/PropertyTable.java#10 $
*/

package ariba.util.core;

import ariba.util.formatter.BooleanFormatter;
import ariba.util.formatter.DoubleFormatter;
import ariba.util.formatter.IntegerFormatter;
import java.util.Iterator;
import java.util.Map;

/**
    <code>PropertyTable</code> is an extension of a <code>Map</code>
    which provides additional convenience methods and some minor semantic
    changes.  For example, calling the <code>setPropertyForKey</code> method
    with a null value removes the value for the given key from the
    <code>PropertyTable</code>.  There are also convenience methods for
    returning a property as a particular type; if the value stored in the
    <code>PropertyTable</code> is of a different type, a reasonable conversion
    is done.  Finally, it is possible to store a null in a property table
    using a special object to represent that.

    @aribaapi public
*/
public class PropertyTable implements DebugState
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    /**
        A special object that represent a null value stored in a
        <code>PropertyTable</code>.
        @aribaapi documented
    */
    public static final String NullValueMarker = "<<Null value marker>>";

    /**
        Our <code>Map</code> of property values.
        @aribaapi private
    */
    protected Map properties;


    /*-----------------------------------------------------------------------
        Constructors
      -----------------------------------------------------------------------*/

    /**
        Creates a new empty <code>PropertyTable</code>.
        @aribaapi documented
    */
    public PropertyTable ()
    {
        this(null);
    }

    /**
        Creates a new <code>PropertyTable</code> from the given
        <code>Map</code> of properties.

        @param properties the properties to store in this table
        @aribaapi documented
    */
    public PropertyTable (Map properties)
    {
        if (properties == null) {
            properties = MapUtil.map();
        }
        this.properties = properties;
    }


    /*-----------------------------------------------------------------------
        Public Methods
      -----------------------------------------------------------------------*/

    /**
        Returns the properties stored in this table as a <code>Map</code>.

        @return the <code>Map</code> of properties
        @aribaapi documented
    */
    public Map getProperties ()
    {
        return properties;
    }

    /**
        Return an <code>Iterator</code> of all the properties stored in
        this table.

        @return an <code>Iterator</code> of the properties in this table
        @aribaapi documented
    */
    public Iterator getAllProperties ()
    {
        return properties.keySet().iterator();
    }

    /**
        Return the property for the given key.  Returns null if the given key
        is null, or if a null was specially inserted into the table using the
        <code>NullValueMarker</code>.

        @param key the key for the property to retrieve
        @return    the property associated with the given key
        @aribaapi public
    */
    public Object getPropertyForKey (String key)
    {
        Object result = (key == null) ? null : properties.get(key);
        return (result == NullValueMarker) ? null : result;
    }

    /**
        Sets a property value for the given key.  If the value is null, the current
        property for the key is removed.

        @param key   the key under which to store the value
        @param value the new value, or null to remove the current value
        @aribaapi public
    */
    public void setPropertyForKey (String key, Object value)
    {
        if (key != null) {
            if (value == null) {
                properties.remove(key);
            }
            else {
                properties.put(key, value);
            }
        }
    }

    /**
        Return the property for the given key as a string.  If the current
        value for the property is not a string, the <code>toString</code>
        method is called on the value.  Returns null if the given key is null.

        @param key the key for the property to retrieve
        @return    the string property associated with the given key
        @aribaapi public
    */
    public String stringPropertyForKey (String key)
    {
        Object property = getPropertyForKey(key);
        return (property != null && property != NullValueMarker) ?
                    property.toString() :
                    null;
    }

    /**
        Return the property for the given key as a <code>boolean</code>.  If
        the current value for the property is not a boolean, the value is
        converted using the <code>booleanValue</code> method from the
        <code>Util</code> class.  Returns null if the given key is null.

        @param key the key for the property to retrieve
        @return    the boolean property associated with the given key
        @aribaapi public
    */
    public boolean booleanPropertyForKey (String key)
    {
        return booleanValue(getPropertyForKey(key));
    }

    /**
        Return the property for the given key as an <code>int</code>.  If the
        current value for the property is not an integer, the value is
        converted using the <code>intValue</code> method from the
        <code>Util</code> class.  Returns null if the given key is null.

        @param key the key for the property to retrieve
        @return    the integer property associated with the given key
        @aribaapi public
    */
    public int integerPropertyForKey (String key)
    {
        return intValue(getPropertyForKey(key));
    }

    /**
        Return the property for the given key as a <code>double</code>.  If
        the current value for the property is not a double, the value is
        converted using the <code>doubleValue</code> method from the
        <code>Util</code> class.  Returns null if the given key is null.

        @param key the key for the property to retrieve
        @return    the double property associated with the given key
        @aribaapi public
    */
    public double doublePropertyForKey (String key)
    {
        return doubleValue(getPropertyForKey(key));
    }

    /**
        Return the number of properties in the table.

        @return    the number of properties
        @aribaapi public
    */
    public int getCount ()
    {
        return properties.size();
    }

    /**
        Returns whether the table contains a value for the given key.  If a null
        was inserted as the value using <code>NullValueMarker</code>, this method
        will return true.

        @param key the key for the property to test
        @return    whether the table contains a value for the given key
        @aribaapi public
    */
    public boolean containsKey (String key)
    {
        return key == null ? false : properties.containsKey(key);
    }

    /**
        Returns a string reprsentation of this table of properties.

        @aribaapi private
    */
    public String toString ()
    {
        return properties.toString();
    }

    /**
        Returns an object that will be toStringed when the debug
        information needs to be printed.

        @return an object that encapsulates the state of the current
            application

        @aribaapi documented
    */
    public Object debugState ()
    {
        return properties;
    }

    /*-----------------------------------------------------------------------
        Protected Static Methods
      -----------------------------------------------------------------------*/

    /**
        Returns the boolean represented by the given value.

        @param value
        @return boolean represented by value
        @aribaapi private
    */
    protected static boolean booleanValue (Object value)
    {
        return BooleanFormatter.getBooleanValue(value);
    }

    /**
        Returns the int represented by the given value.

        @param value
        @return int represented by value
        @aribaapi private
    */
    protected static int intValue (Object value)
    {
        return IntegerFormatter.getIntValue(value);
    }

    /**
        Returns the double represented by the given value.

        @param value
        @return double represented by value
        @aribaapi private
    */
    protected static double doubleValue (Object value)
    {
        return DoubleFormatter.getDoubleValue(value);
    }
}
