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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/FieldPath.java#4 $
*/

package ariba.util.fieldvalue;

import ariba.util.core.StringUtil;
import ariba.util.core.GrowOnlyHashtable;

/**
    The FieldPath class is an object representation of a dotted fieldPath.
    A String such as "foo.bar.baz" can be used to access a value on a target
    object using the FieldValue interface.  Such a string might be the equivalent
    of the following Java code:  target.getFoo().getBar().getBaz();.  In order
    to avoid reparsing this string each time its used, the FieldPath object
    provides a way to store the parsed version of this string as a linked list
    of three nodes "foo"->"bar"->"baz".
    <p>
    In addition to avoiding the need to reparse the dotted fieldPath each time,
    we also use the FieldPath object as a place to store the previous
    FieldValueAccessor to which the node's fieldName resolved.  Of course, the
    class for which the previousAccessor applies my not apply for the next usage,
    but in the common case it does, and so the performance of the dispatch
    operation is improved by avoiding a more costly hash lookup.  Finally, the
    underlying Accessor (e.g. {@link ReflectionFieldAccessor} or
    {@link ReflectionMethodAccessor}) may itself cache a byte-code compiled accesor
    for the target, thereby turning field access into a no-lookup, no-reflection
    direct access.
*/
public class FieldPath extends Object
{
    private static final ReflectionFieldAccessor DummyAccessor =
        ReflectionFieldAccessor.newInstance(FieldPath.class, "_fieldName");
    private static final FieldValue DummyClassExtension = FieldValue.get(Object.class);
    private static final GrowOnlyHashtable FieldPathHashtable = new GrowOnlyHashtable();
    private static final String FieldPathSeparator = ".";
    private String _fieldPathString;
    public final String _fieldName;
    public final FieldPath _nextFieldPath;
    // By initializing, avoid need to check for null
    public FieldValueSetter _previousSetter = DummyAccessor;
    public FieldValueGetter _previousGetter = DummyAccessor;
    private FieldValue _previousClassExtension = DummyClassExtension;

    /**
    Constructs a new FieldPath from fieldPathString.  If fieldPathString
    is a dotted fieldPath, this becomes a recursive operation and thus
    will create an entire linked list of FieldPath nodes until there
    are no more dotted fieldPath components.

    @param fieldPathString the string which contains a fieldName or
    series or fieldNames separated by a FieldPathSeparator.
    */
    public FieldPath (String fieldPathString)
    {
        String fieldPathPrefix = FieldPath.prefixForFieldPathString(fieldPathString);
        _fieldName = fieldPathPrefix.intern();
        String fieldPathSuffix = FieldPath.suffixForFieldPathString(fieldPathString);
        _nextFieldPath = (fieldPathSuffix != null) ? new FieldPath(fieldPathSuffix)
            : null;
    }

    /**
    Extracts the substring of fieldPathString up to the first dot (FieldPathSeparator).

    @param fieldPathString the string which contains a fieldName or series or
    fieldNames separated by a FieldPathSeparator.
    @return the first component of the dotted fieldPath string.  If
    fieldPathString contains no dots, the this returns fieldPathString.
    */
    private static String prefixForFieldPathString (String fieldPathString)
    {
        String carString = null;
        int indexOfDot = fieldPathString.indexOf(FieldPathSeparator);
        if (indexOfDot == -1) {
            carString = fieldPathString;
        }
        else {
            carString = fieldPathString.substring(0, indexOfDot);
        }
        return carString;
    }

    /**
    Extracts the substring of fieldPathString after the first dot (FieldPathSeparator).

     @param fieldPathString the string which contains a fieldName or
     series or fieldNames separated by a FieldPathSeparator.
     @return the remainder of fieldPathString after the first dot or
     null if there are no dots.
    */
    private static String suffixForFieldPathString (String fieldPathString)
    {
        String cdrString = null;
        int indexOfDot = fieldPathString.indexOf(FieldPathSeparator);
        if (indexOfDot != -1) {
            cdrString = fieldPathString.substring(indexOfDot + 1);
        }
        return cdrString;
    }

    /**
    Attempts to locate a shared instance of a FieldPath object that
    corresponds to fieldPathString.  If none is found, a new FieldPath
    instance is created and cached for the next time this may be called.
    It is not recommend that this method be called to obtain FieldPath
    instances if you plan on caching the FieldPath yourself as sharing
    FieldPaths will result in greater cache invalidation with respect
    to the previousAccessor cache on each FieldPath node.

    @param fieldPathString the string which contains a fieldName or
    series or fieldNames separated by a FieldPathSeparator.
    @return a FieldPath object corresponding to fieldPathString.
    This FieldPath object may have been in use by other clients of
    this method and, thus, may have previousAccessors established.
    You must check the validity of any such accessors before using them.
    */
    public static FieldPath sharedFieldPath (String fieldPathString)
    {
        FieldPath fieldPath = (FieldPath)FieldPathHashtable.get(fieldPathString);
        if (fieldPath == null) {
            fieldPath = new FieldPath(fieldPathString);
            FieldPathHashtable.put(fieldPathString, fieldPath);
        }
        return fieldPath;
    }

    /**
    Returns the fieldName value of the first node of a linked list of FieldPaths.

    @return the fieldName for the receiving node.
    */
    public String car ()
    {
        return _fieldName;
    }

    /**
    Returns the remainder of the linked list of FieldPaths.

    @return the next FieldPath node or null if there is none.
    */
    public FieldPath cdr ()
    {
        return _nextFieldPath;
    }

    /**
    Returns the last node in the linked list of FieldPath nodes.

    @return the last FieldPath node or the receiver if there are no more nodes.
    */
    public FieldPath tail ()
    {
        return (_nextFieldPath == null) ? this : _nextFieldPath.tail();
    }

    /**
    Reconstitutes the dotted fieldPath starting at the current node
    and proceding to the end of the list.  The result is cached the
    first time this is called to minimize memory consumption if
    this is never called.

    @return the dotted fieldPath as it appears from the current node
    to the end of the linked list of FieldPaths.
    */
    public String fieldPathString ()
    {
        if (_fieldPathString == null) {
            if (_nextFieldPath == null) {
                _fieldPathString = _fieldName;
            }
            else {
                _fieldPathString = StringUtil.strcat(_fieldName, FieldPathSeparator,
                                              _nextFieldPath.toString());
                _fieldPathString = _fieldPathString.intern();
            }
        }
        return _fieldPathString;
    }

    public String toString ()
    {
        return fieldPathString();
    }

    /**
    Convenience that avoids the use of the FieldValue class.

    @param target see the FieldPath version of this method
    @param value see the FieldPath version of this method
    */
    public void setFieldValue (Object target, Object value)
    {
        FieldValue fieldValueClassExtension = _previousClassExtension;
        // See Javadoc for FieldValue_BaseObjectProxyImpl to understand why
        // getClass rather than getRealClass is used here even for ClassProxy.
        Class targetClass = target.getClass();
        if (fieldValueClassExtension.forClass != targetClass) {
            fieldValueClassExtension = (FieldValue)
            FieldValue.FieldValueClassExtensionRegistry.get(targetClass);
            _previousClassExtension = fieldValueClassExtension;
        }
        fieldValueClassExtension.setFieldValue(target, this, value);
    }

    /**
    Looks up the FieldValue classExtension for target and forwards to
    the corresponding method in that classExtension.

    @param target see the FieldPath version of this method
    @return see the FieldPath version of this method
    */
    public Object getFieldValue (Object target)
    {
        FieldValue fieldValueClassExtension = _previousClassExtension;
        // See Javadoc for FieldValue_BaseObjectProxyImpl to understand why
        // getClass rather than getRealClass is used here even for ClassProxy.
        Class targetClass = target.getClass();
        if (fieldValueClassExtension.forClass != targetClass) {
            fieldValueClassExtension = (FieldValue)
                FieldValue.FieldValueClassExtensionRegistry.get(targetClass);
            _previousClassExtension = fieldValueClassExtension;
        }
        return fieldValueClassExtension.getFieldValue(target, this);
    }
}
