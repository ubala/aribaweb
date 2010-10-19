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

    $Id: //ariba/platform/util/core/ariba/util/formatter/Formatter.java#18 $
*/

package ariba.util.formatter;

import ariba.util.core.Compare;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Date;
import ariba.util.core.MapUtil;
import java.util.Map;
import ariba.util.core.ResourceService;
import java.text.ParseException;
import java.util.Locale;
import java.text.Format;
import java.util.ArrayList;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.Fmt;

/**
    <code>Formatter</code> and its subclasses (<code>StringFormatter</code>,
    <code>IntegerFormatter</code>, etc.) are responsible for formatting raw
    objects into strings and parsing strings back into objects.  They also
    provide methods for comparing pairs of objects for sorting purposes.
    <code>Formatter</code> instances can be used with the <code>Sort</code>
    class, since all <code>Formatters</code> implement the
    <code>Compare</code> interface.
    <p>
    The simplest way to use a formatter is in code like:
    <p>
    <blockquote>
    <code>
    String foo = Formatter.stringValue(object);
    Object bar = Formatter.parseString(string, type);
    </code>
    </blockquote>
    <p>
    Clients may also get a specific formatter for a given object or type by
    calling one of the static methods <code>formatterForObject</code> or
    <code>formatterForType</code>.
    <p>
    For improved runtime performance, this class maintains a cache of
    formatter instances, keyed by type.

    @aribaapi documented
*/
abstract public class Formatter implements StringParser, Compare
{
    private static final String UtilStringTable = "ariba.util.core";

    /*-----------------------------------------------------------------------
        Static Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a formatted string for the given object in the default locale.
        If a formatter instance for the given object can't be found, calls
        <code>toString</code> on the object and returns the resulting string.

        @param  object the object to format into a string
        @return        a string representation of the object
        @aribaapi public
    */
    public static String getStringValue (Object object)
    {
            // call the main implementation using the default locale
        return getStringValue(object, getDefaultLocale());
    }

    /**
        Returns a formatted string for the given object in the given locale.
        If a formatter instance for the given object can't be found, calls
        <code>toString</code> on the object and returns the resulting string.

        @param object the object to format into a string
        @param locale the <code>Locale</code> to use for formatting
        @return       a string representation of the object
        @aribaapi public
    */
    public static String getStringValue (Object object, Locale locale)
    {
            // can't get a formatter if the object is null
        if (object == null) {
            return "";
        }

            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

            // try to get a formatter and use it to string-ify the object
        Formatter fmt = getFormatterForObject(object);
        return (fmt == null) ? object.toString() : fmt.getFormat(object, locale);
    }


    /*-----------------------------------------------------------------------
        Static Parsing
      -----------------------------------------------------------------------*/

    /**
        Tries to parse a string to create an object of the given type using
        the default locale.  Returns the original string if a formatter
        instance for the given <code>type</code> cannot be created.

        @param  string the string to parse into an object
        @param  type   the Java type of the resulting object
        @return        an object of the given <code>type</code>
        @exception     ParseException if the string can't be parsed to create
                       an object of the given <code>type</code>
        @aribaapi public

    */
    public static Object parseString (String string, String type)
      throws ParseException
    {
        return parseString(string, type, getDefaultLocale());
    }

    /**
        Tries to parse a string to create an object of the given type using
        the given locale.  Returns the original string if a formatter instance
        for the given <code>type</code> cannot be created.

        @param  string the string to parse into an object
        @param  type   the Java type of the resulting object
        @param  locale the <code>Locale</code> to use for parsing
        @return        an object of the given <code>type</code>
        @exception     ParseException if the string can't be parsed to create
                       an object of the given <code>type</code>
        @aribaapi public
    */
    public static Object parseString (String string, String type, Locale locale)
      throws ParseException
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        Formatter fmt = getFormatterForType(type);
        return (fmt == null) ? string : fmt.parse(string, locale);
    }


    /*-----------------------------------------------------------------------
        Static Comparison
      -----------------------------------------------------------------------*/

    /**
        Returns true if and only if the two objects should be considered equal
        in the default locale.
        <p>
        If the first object is non-null, retrieves a formatter instance based
        on its type; otherwise, uses the second object.  If a formatter can't
        be found for either object, or the types of the two objects don't
        match, resorts to the <code>equals</code> method on the first non-null
        object.  If both objects are null, returns true.

        @param  o1 the first object to test for equality
        @param  o2 the second object to test for equality
        @return    <code>true</code> if the two objects are equal;
                   <code>false</code> otherwise.
        @aribaapi public
    */
    public static boolean objectsAreEqual (Object o1, Object o2)
    {
        return objectsAreEqual(o1, o2, getDefaultLocale());
    }

    /**
        Returns true if and only if the two objects should be considered equal
        in the given locale.
        <p>
        If the first object is non-null, retrieves a formatter instance based
        on its type; otherwise, uses the second object.  If a formatter can't
        be found for either object, or the types of the two objects don't
        match, resorts to the <code>equals</code> method on the first non-null
        object.  If both objects are null, returns true.

        @param  o1     the first object to test for equality
        @param  o2     the second object to test for equality
        @param  locale the <code>Locale</code> to use for equality testing
        @return        <code>true</code> if the two objects are equal;
                       <code>false</code> otherwise.
        @aribaapi public
    */
    public static boolean objectsAreEqual (Object o1, Object o2, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

            // short circuit (including both null)
        if (o1 == o2) {
            return true;
        }
            // check for one null but not the other
        else if (o1 == null || o2 == null) {
            return false;
        }

        String cls1 = (o1 == null) ? null : o1.getClass().getName();
        String cls2 = (o2 == null) ? null : o2.getClass().getName();

            // try to use a formatter for the first object
        if (o1 != null) {
            Formatter fmt = getFormatterForObject(o1);
            if (fmt != null && cls1.equals(cls2)) {
                return fmt.equal(o1, o2, locale);
            }
        }

            // try to use a formatter for the second object
        if (o2 != null) {
            Formatter fmt = getFormatterForObject(o2);
            if (fmt != null && cls2.equals(cls1)) {
                return fmt.equal(o1, o2, locale);
            }
        }

            // no formatter could be found for either object
        return (o1 != null) ? o1.equals(o2) : o2.equals(o1);
    }


    /*-----------------------------------------------------------------------
        Static Formatter Creation
      -----------------------------------------------------------------------*/

    /**
        Returns a formatter instance for the given object based on its type.

        @param  object the object for which a formatter is required
        @return        the appropriate formatter instance, or null if a
                       formatter instance can't be created for the given type
        @aribaapi documented
    */
    public static Formatter getFormatterForObject (Object object)
    {
        Assert.that(object != null, "object == null in formatterForObject()");

            // find a formatter for the type of the object
        return getFormatterForType(object.getClass().getName());
    }

    /**
        Returns a formatter instance for the given type.  Returns a cached
        instance if one exists for the given type.  Otherwise a new formatter
        instance is created and cached based on the given type.

        @param  type  the type for which a formatter is required
        @return       the appropriate formatter instance, or null if a
                      formatter instance can't be created for the given type
        @aribaapi documented
    */
    public static Formatter getFormatterForType (String type)
    {
        Assert.that(type != null, "type == null in formatterForType()");

            // check the formatter cache first
        Formatter fmt = getCachedFormatter(type);

            // if not found there, try to create one of the appropriate type
        if (fmt == null) {

                // determine the formatter class to use for this type
            String fmtClass = getFormatterClassForType(type);

            if (fmtClass != null) {
                    // create a new instance of the formatter class
                fmt = (Formatter)ClassUtil.newInstance(fmtClass, false);
                Assert.that(fmt != null, NewErrorMsg, fmtClass);

                    // cache the instance for later use
                cacheFormatter(fmt, type);
            }
        }

        return fmt;
    }

    /**
        Returns the formatter class to use for the given type.  We first check
        the class map for a perfect match with the type name.  If that fails,
        we then check the map against any interfaces implemented by the type.
        If that fails, we then call ourselves recursively with the super-type
        of the given type.  Returns null if a formatter class couldn't be
        determined for the given type, or if the type is invalid.
        @param type The fully qualified type name whose formatter we are finding
        @return The class name of the formatter, or null if there isn't any
        @aribaapi documented
    */
    public static String getFormatterClassForType (String type)
    {
        Class  typeClass = null;
        String fmtClass  = null;

            // check the class map for an exact match
        synchronized (map) {
            fmtClass = (String)map.get(type);
        }

            // check interfaces if the map check failed
        if (fmtClass == null) {
            typeClass = ClassUtil.classForName(type, false);
            if (typeClass != null) {
                Class[] interfaces = typeClass.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    synchronized (map) {
                        fmtClass = (String)map.get(interfaces[i].getName());
                    }
                    if (fmtClass != null) {
                        break;
                    }
                }
            }
        }

            // check the super-type if the interfaces check failed
        if (fmtClass == null && typeClass != null) {
            Class typeSuper = typeClass.getSuperclass();
            if (typeSuper != null) {
                    // call ourselves recursively on the super-type
                return getFormatterClassForType(typeSuper.getName());
            }
        }

        return fmtClass;
    }

    /**
        Registers the given formatter class name to be associated with the
        give type.  This allows clients of the formatter class to extend the
        set of formatters that are available.  For example, the procurement
        module registers procure-specific formatter classes for types that are
        specific to that module.

        @param  type      the object type for the given formatter class
        @param  className the class of formatter to use for the given type

        @aribaapi private
    */
    public static void registerFormatter (String type, String className)
    {
        synchronized (map) {
                // store the formatter class keyed by type
            map.put(type, className);
        }

        // if we have cached a formatter for this type - remove it so that
        // we'll get the new type
        synchronized (cache) {
            cache.remove(type);
        }
    }

    /*-----------------------------------------------------------------------
        Internationalization
      -----------------------------------------------------------------------*/

    /**
        Returns the default locale to use for formatting, parsing, etc. if no
        locale is given explicitly in one of the static methods above.  The
        locale from the current resource service is used as a default.

        @return Returns the default locale
        @aribaapi documented
    */
    protected static Locale getDefaultLocale ()
    {
        return ResourceService.getService().getLocale();
    }

    /**
     * Convenience routine to get a localized message for a ParseException.
     *
     * @param errorKey - the key to the error message
     *
     * @return a newly constructed localized String
     */
    public static String makeParseExceptionMessage (String errorKey)
    {
        ResourceService rs = ResourceService.getService();
        return rs.getLocalizedString(UtilStringTable, errorKey);
    }


    /**
     * Convenience routine to get a localized message for a ParseException.
     *
     * @param errorKey - the key to the error message
     * @param argument - an argument to the error key
     *
     * @return a newly constructed localized String
     */
    public static String makeParseExceptionMessage (String errorKey, String argument)
    {
        ResourceService rs = ResourceService.getService();
        return Fmt.Sil(rs.getLocale(), UtilStringTable, errorKey, argument);
    }

    /**
     * Convenience routine to get a ParseException with a localized message.
     * @param errorKey - the key to the error message
     * @param offset - the offset in the string being parsed
     * @return a newly constructed ParseException
     */
    public static ParseException makeParseException (String errorKey,
                                                     int offset)
    {
        String msg = makeParseExceptionMessage(errorKey);
        return new ParseException(msg, offset);
    }

    /**
     * Convenience routine to get a ParseException with a localized message.
     * @param errorKey - the key to the error message
     * @param argument - an argument to the error key
     * @param offset - the offset in the string being parsed
     * @return a newly constructed ParseException
     */
    public static ParseException makeParseException (String errorKey,
                                                     String argument,
                                                     int offset)
    {
        ResourceService rs = ResourceService.getService();
        return makeParseException(errorKey, argument, offset, rs.getLocale());
    }

    public static ParseException makeParseException (String errorKey,
                                                     String argument,
                                                     int offset,
                                                     Locale locale)
    {
        String msg = Fmt.Sil(locale, UtilStringTable, errorKey, argument);
        return new ParseException(msg, offset);
    }

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Creates a new <code>Formatter</code>.  Formatters are stateless and
        thus can be cached for use on multiple values of the same type.

        @aribaapi public
    */
    public Formatter ()
    {
    }


    /*-----------------------------------------------------------------------
        Locale
      -----------------------------------------------------------------------*/

    /**
        Returns the locale associated with this formatter.  Uses the locale
        from the current resource service by default.

        @return the locale associated with this formatter
        @aribaapi public
    */
    public Locale getLocale ()
    {
        return getDefaultLocale();
    }


    /*-----------------------------------------------------------------------
        Formatter information
      -----------------------------------------------------------------------*/

    /**
        Check whether this formatter guarantees text->object conversion.<br>
        All formatters can convert from object to string, but not all can
        do the reverse conversion.  This method checks whether this formatter
        does the reverse conversion.  By default we assume that the formatter
        can do the text -> object conversion, subclasses should override if
        this is not true.
        @return true if the formatter can do text to object conversion
        @aribaapi documented
    */
    public boolean isBidirectional ()
    {
        return true;
    }

    /**
        Check whether this formatter handles null values.<br>
        Most formatters do not handle null values, and so the user of the formatter
        is expected to handle the null value themselves.  Formatters which
        handle nulls should override this method, returning true, to indicate
        that they do handle null values.
        @return true if the formatter expectes to handle null values
        @aribaapi documented
    */
    public boolean canFormatNulls ()
    {
        return false;
    }

    /*-----------------------------------------------------------------------
        Formatting
      -----------------------------------------------------------------------*/

    /**
        Returns a string representation of the given object in the default
        locale.  Returns an empty string if the object is null.

        @param object the object to format as a string
        @return       a string representation of the object
        @aribaapi documented
    */
    public final String getFormat (Object object)
    {
        return getFormat(object, getLocale());
    }

    /**
        Returns a string representation of the given object in the given
        locale.  Returns an empty string if the object is null.

        @param object the object to format as a string
        @param locale the <code>Locale</code> to use for formatting
        @return       a string representation of the object
        @aribaapi public
    */
    public final String getFormat (Object object, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

            // call the internal protected implementation
        return (object != null || handlesNulls()) ? formatObject(object, locale) : "";
    }

    /**
        Check whether this class handles null values.
        @return true if this formatter class handles null values itself
        @aribaapi documented
    */
    public final boolean handlesNulls ()
    {
        /*
            Note: The reason that this method does an instanceof, rather than the
            better approach of calling a method (which some subclasses could override) is to be
            absolutely consistent with AWTextField's means of deciding which formatters handle
            nulls, which is with the marker interface.
        */
        return (this instanceof FormatterHandlesNulls);
    }

    /**
        Returns a string representation of the given object in the given
        locale.  The object is assumed to be non-null.
        <p>
        Subclasses must define this method to provide type-specific
        formatting.

        @param  object the object to format as a string
        @param  locale the <code>Locale</code> to use for formatting
        @return        a string representation of the object
        @aribaapi public
    */
    abstract protected String formatObject (Object object, Locale locale);


    /*-----------------------------------------------------------------------
        StringParser Interface
      -----------------------------------------------------------------------*/

    /**
        Tries to parse the given string into an object of the appropriate type
        for this formatter.  Uses the default locale to parse the string.

        @param  string the string to parse
        @return        an object of the appropriate type for this formatter
        @exception     ParseException if the string can't be parsed to create
                       an object the appropriate type
        @aribaapi documented
    */
    public final Object parse (String string)
      throws ParseException
    {
            // call protected parse method w/ trimmed string
        return parse(string, getLocale());
    }

    /**
        Tries to parse the given string into an object of the appropriate type
        for this formatter.  Uses the given locale to parse the string.

        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing
        @return        an object of the appropriate type for this formatter
        @exception     ParseException if the string can't be parsed to create
                       an object the appropriate type
        @aribaapi public
    */
    public final Object parse (String string, Locale locale)
      throws ParseException
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

            // short circuit if string is null
        if (string == null) {
            return null;
        }

            // call protected parse method w/ trimmed string
        return parseString(string.trim(), locale);
    }

    /**
        Parses the given string into an object of the appropriate type for
        this formatter.  The string is assumed to be non-null and trimmed of
        leading and trailing whitespace.
        <p>
        Subclasses must define this method to provide type-specific parsing.

        @param  string the string to parse
        @param  locale the <code>Locale</code> to use for parsing
        @return        an object of the appropriate type for this formatter
        @exception     ParseException if the string can't be parsed to create
                       an object the appropriate type
        @aribaapi public
    */
    abstract protected Object parseString (String string, Locale locale)
      throws ParseException;

    /**
        Returns an object of the appropriate type for this formatter derived
        from the given object.  The default locale is used for any conversion
        that may be done.
        <p>
        The type of the given object can by anything; it is up to each
        specific implementation to do the appropriate conversion, parsing,
        etc. to create a value of the appropriate type.
        <p>
        The return value may be null depending on the specific implementation
        for a given formatter.

        @param  object the object to convert to the type for this formatter
        @return        an object of the appropriate type for this formatter
        @aribaapi documented
    */
    public final Object getValue (Object object)
    {
        return getValue(object, getLocale());
    }

    /**
        Returns an object of the appropriate type for this formatter based on
        the given object.  The given locale is used for any conversion that
        may be done.
        <p>
        The type of the given object can by anything; it is up to each
        specific implementation to do the appropriate conversion, parsing,
        etc. to create a value of the appropriate type.
        <p>
        The return value may be null depending on the specific implementation
        for a given formatter.
        <p>
        Subclasses must define this method to provide type-specific
        conversion.

        @param  object the object to convert to the type for this formatter
        @param  locale the locale used when converting the object
        @return        an object of the appropriate type for this formatter
        @aribaapi public
    */
    abstract public Object getValue (Object object, Locale locale);


    /*-----------------------------------------------------------------------
        Equality Testing
      -----------------------------------------------------------------------*/

    /**
        Returns true if and only if the two objects should be considered equal
        in the default locale.  Returns true if the two objects are, in fact,
        the same object or both are null.  Otherwise, returns false if exactly
        one of the objects is null.  Otherwise, calls the protected method
        <code>objectsEqual</code> to determine if the two (non-null) objects
        are equal.

        @param  o1     the first object to test for equality
        @param  o2     the second object to test for equality
        @return        <code>true</code> if the two objects are equal;
                       <code>false</code> otherwise.
        @aribaapi documented
    */
    public final boolean equal (Object o1, Object o2)
    {
        return equal(o1, o2, getLocale());
    }

    /**
        Returns true if and only if the two objects should be considered equal
        in the given locale.  Returns true if the two objects are, in fact,
        the same object or both are null.  Otherwise, returns false if exactly
        one of the objects is null.  Otherwise, calls the protected method
        <code>objectsEqual</code> to determine if the two (non-null) objects
        are equal.

        @param  o1     the first object to test for equality
        @param  o2     the second object to test for equality
        @param  locale the <code>Locale</code> to use for equality testing
        @return        <code>true</code> if the two objects are equal;
                       <code>false</code> otherwise.
        @aribaapi public
    */
    public final boolean equal (Object o1, Object o2, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        if (o1 == o2) {
            return true;
        }
        else if (o1 == null || o2 == null) {
            return false;
        }
        else {
            return objectsEqual(o1, o2, locale);
        }
    }

    /**
        Returns true if and only if the two objects should be considered equal
        in the given locale.  The two values must be non-null and of the type
        appropriate for this formatter.
        <p>
        This default implementation just uses the compareObjects() method to
        test if the two objects compare equally.  Subclasses may override this
        method to provide more specific or strict equality testing.

        @param  o1     the first object to test for equality
        @param  o2     the second object to test for equality
        @param  locale the <code>Locale</code> to use for equality testing
        @return        <code>true</code> if the two objects are equal;
                       <code>false</code> otherwise.
        @aribaapi public
    */
    protected boolean objectsEqual (Object o1, Object o2, Locale locale)
    {
        return (compareObjects(o1, o2, locale) == 0);
    }


    /*-----------------------------------------------------------------------
        Compare Interface
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the default locale.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.
        <p>
        Returns zero if the two objects are, in fact, the same object or both
        are null.  Otherwise, arbitrarily returns -1 if the first object is
        null, or 1 if the second is null.  Otherwise, calls the protected
        method <code>compareObjects</code> to compare the two (non-null)
        objects.

        @param  o1     the first object to compare
        @param  o2     the second object to compare
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi documented
    */
    public final int compare (Object o1, Object o2)
    {
        return compare(o1, o2, getLocale());
    }

    /**
        Compares two objects for sorting purposes in the given locale.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.
        <p>
        Returns zero if the two objects are, in fact, the same object or both
        are null.  Otherwise, arbitrarily returns -1 if the first object is
        null, or 1 if the second is null.  Otherwise, calls the protected
        method <code>compareObjects</code> to compare the two (non-null)
        objects.

        @param  o1     the first object to compare
        @param  o2     the second object to compare
        @param  locale the <code>Locale</code> to use for comparison
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi public
    */
    public final int compare (Object o1, Object o2, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        if (o1 == o2) {
            return 0;
        }
        else if (o1 == null) {
            return -1;
        }
        else if (o2 == null) {
            return 1;
        }
        else {
            return compareObjects(o1, o2, locale);
        }
    }

    /**
        Compares two objects for sorting purposes in the given locale.
        Returns an <code>int</code> value which is less than, equal to, or
        greater than zero depending on whether the first object sorts before,
        the same, or after the second object.
        <p>
        Subclasses must define this method to provide type-specific
        comparison.

        @param  o1     the first object to compare
        @param  o2     the second object to compare
        @param  locale the <code>Locale</code> to use for comparison
        @return        <code>int</code> value which determines how the two
                       objects should be ordered
        @aribaapi public
    */
    abstract protected int compareObjects (Object o1, Object o2, Locale locale);


    /*-----------------------------------------------------------------------
        Quick Comparison
      -----------------------------------------------------------------------*/

    /**
        Compares two objects for sorting purposes in the default locale.
        <p>
        This method is a performance optimzation for sorting.  If comparing
        two values of a given type involves some conversion or processing that
        is too expensive to do for each comparison, this method and the
        <code>quickCompareValue</code> method below can be used to precompute
        the values to use for sorting.  Any code which needs to sort these
        types should call this method rather than <code>compare</code..

        @aribaapi private
    */
    public final int quickCompare (Object o1, Object o2)
    {
        return quickCompare(o1, o2, getLocale());
    }

    /**
        Compares two objects for sorting purposes in the given locale.
        <p>
        This method is a performance optimzation for sorting.  If comparing
        two values of a given type involves some conversion or processing that
        is too expensive to do for each comparison, this method and the
        <code>quickCompareValue</code> method below can be used to precompute
        the values to use for sorting.  Any code which needs to sort these
        types should call this method rather than <code>compare</code..

        @aribaapi private
    */
    public final int quickCompare (Object o1, Object o2, Locale locale)
    {
            // locale must be non-null
        Assert.that(locale != null, "invalid null Locale");

        if (o1 == o2) {
            return 0;
        }
        else if (o1 == null) {
            return -1;
        }
        else if (o2 == null) {
            return 1;
        }
        else {
            return quickCompareObjects(o1, o2, locale);
        }
    }

    /**
        Compares two values for sorting purposes, assuming they are values
        returned by the <code>quickCompareValue</code> method below.  The
        default locale is used for comparisons.
        <p>
        This default implementation is to just call the <code>compare</code>
        method.  Any formatter which overrides <code>quickCompareValue</code>
        should also override this method to do a quick comparison of the
        converted values.

        @aribaapi private
    */
    protected int quickCompareObjects (Object o1, Object o2)
    {
        return quickCompareObjects(o1, o2, getLocale());
    }

    /**
        Compares two values for sorting purposes, assuming they are values
        returned by the <code>quickCompareValue</code> method below.  The
        default locale is used for comparisons.
        <p>
        This default implementation is to just call the <code>compare</code>
        method.  Any formatter which overrides <code>quickCompareValue</code>
        should also override this method to do a quick comparison of the
        converted values.

        @aribaapi private
    */
    protected int quickCompareObjects (Object o1, Object o2, Locale locale)
    {
        return compare(o1, o2, locale);
    }

    /**
        Returns a converted value for the given object to be used by the
        <code>quickCompare</code> method above.  The previous object is the
        previous value that was passed to <code>quickCompareValue</code>
        (pre-converted).
        <p>
        The default implementation is to just return object itself.

        @aribaapi ariba
    */
    public Object quickCompareValue (Object object, Object previous)
    {
        return object;
    }


    /*-----------------------------------------------------------------------
        Formatter Cache
      -----------------------------------------------------------------------*/

    /**
        Retrieves a formatter for the given type from our cache.

        @param  type the type of <code>Formatter</code> to retrieve
        @return      a cached <code>Formatter</code> instance, or null if an
                     instance of the given <code>type</code> wasn't found in
                     the cache
        @aribaapi documented
    */
    protected static Formatter getCachedFormatter (String type)
    {
        synchronized (cache) {
            return (Formatter)cache.get(type);
        }
    }

    /**
        Stores formatter in our cache using the given key.

        @param formatter the <code>Formatter</code> instance to cache
        @param type      the type of the <code>Formatter</code>, used as the
                         key in the cache for later retrieval
        @aribaapi documented
    */
    protected static void cacheFormatter (Formatter formatter, String type)
    {
        // only stateless formatters can go in the cache
        if (!(formatter instanceof StatefulFormatter)) {
            synchronized (cache) {
                cache.put(type, formatter);
            }
        }
    }

    /*-----------------------------------------------------------------------
        Private Constants
      -----------------------------------------------------------------------*/

        // message strings for assertions
    private static final String NewErrorMsg =
        "couldn't create formatter of type '%s'.";


    /*-----------------------------------------------------------------------
        Private Statics
      -----------------------------------------------------------------------*/

        // the object type -> formatter type map
    private static final Map<String,String> map = MapUtil.map();

        // a cache of formatter instances, keyed by type (e.g. class)
        // only stateless formatters (not instances of StatefulFormatter) are in here
    private static final Map cache = MapUtil.map();


    /*-----------------------------------------------------------------------
        Static Initialization
      -----------------------------------------------------------------------*/

    static {
        map.put(Constants.StringType,           StringFormatter.ClassName);
        map.put(Constants.BooleanType,          BooleanFormatter.ClassName);
        map.put(Constants.IntegerType,          IntegerFormatter.ClassName);
        map.put(Constants.IntPrimitiveType,     IntegerFormatter.ClassName);
        map.put(Constants.LongType,             LongFormatter.ClassName);
        map.put(Constants.LongPrimitiveType,    LongFormatter.ClassName);
        map.put(Constants.DoubleType,           DoubleFormatter.ClassName);
        map.put(Constants.DoublePrimitiveType,  DoubleFormatter.ClassName);
        map.put(Constants.BigDecimalType,       BigDecimalFormatter.ClassName);
        map.put(Date.ClassName,                 DateFormatter.ClassName);
        map.put(Constants.IntegerArrayType,     IntegerArrayFormatter.ClassName);
        map.put(Constants.IntArrayType,         IntegerArrayFormatter.ClassName);
    }


    protected static final int StringFormatterType        = 0;
    protected static final int BooleanFormatterType       = 1;
    protected static final int IntegerFormatterType       = 2;
    protected static final int LongFormatterType          = 3;
    protected static final int DoubleFormatterType        = 4;
    protected static final int BigDecimalFormatterType    = 5;
    protected static final int CalendarDateFormatterType  = 6;
    protected static final int DateFormatterType          = 7;
    protected static final int IntegerArrayTFormatterType = 8;
    protected static final int IntArrayFormatterType      = 9;

    private static MultiKeyHashtable[] formatCache = new MultiKeyHashtable[]
        {
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
            new MultiKeyHashtable(2),
        };

    protected Format instantiateFormat (int type, Locale locale, String pattern)
    {
        Assert.that(false,
            "this method should be overridden by any formatters wanting caching");
        return null;
    }

    protected Format acquireFormat (int type, Locale locale, String pattern)
    {
        MultiKeyHashtable cache = formatCache[type];
        if (pattern == null) {
            pattern = "";
        }

        synchronized(cache)
        {
            ArrayList elements = (ArrayList)cache.get(locale,pattern);
            if (elements == null) {
                elements = new ArrayList();
                cache.put(locale,pattern,elements);
            }
            if (elements.isEmpty()) {
                return instantiateFormat(type,locale,pattern);
            }
            else {
                int i = elements.size()-1;
                return (Format)elements.remove(i);
            }
        }
    }

    protected void releaseFormat (Format format,int type, Locale locale, String pattern)
    {
        if (format == null) {
            return;
        }

        if (pattern == null) {
            pattern = "";
        }

        MultiKeyHashtable cache = formatCache[type];

        synchronized(cache)
        {
            // ToDo: should add a high water limit so that formatters
            // are not cached after the cache reaches a certain size.

            ArrayList elements = (ArrayList)cache.get(locale,pattern);
            Assert.that(elements != null, "no elements list. release without acquire?");
            elements.add(format);
        }
    }
}
