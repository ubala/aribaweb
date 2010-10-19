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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWErrorInfo.java#24 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;
import java.util.Collections;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.util.core.FastStringBuffer;
import ariba.util.log.Logger;
import ariba.ui.aribaweb.util.AWEncodedString;

/**
    This class encapsulates information about an error or warning.  Each
    error is identified by either a single key or a set of keys consist of
    value source, field path, and group name.  The key(s) are then used
    for looking up errors of interest.

    @aribaapi ariba
*/
public class AWErrorInfo implements AWErrorBucket
{
    public static final int NumKeys = 3;
    public static final int ValueSourceKeyIndex = 0;
    public static final int FieldPathKeyIndex = 1;
    public static final int GroupNameKeyIndex = 2;
    public static final int SingleKeyIndex = 0;
    public static final String NullKey = "^^^NullKey^^^";
    public static final int NotDisplayed = -999;

    protected Object[] _keys = new Object[NumKeys];
    protected String _message;
    protected Object _errantValue;
    protected boolean _isWarning;
    protected boolean _isValidationError;

    // This tracks validity condition where the error came from
    protected Object _errorSource;

    // track the datatable item that is associated with this error, if we know it
    AWComponent _datatable;
    Object _tableItem;
    boolean _wasTableAutoScrolled;

    // The order that errors are registered may not be the order that they
    // are rendered.  This member keeps track of the order this error is
    // rendered (as an error indicator).  If the same error is displayed
    // more than once, then we only track the first one.  This member should
    // be accessed or used only the error manager.
    private int _displayOrder = NotDisplayed;
    private int _unnavigableDisplayOrder = NotDisplayed;
    private int _registrationOrder = -1;

    // This id identifies the UI element that flags the error on the page.
    // We need this information to pop up the bubble to highlight the current
    // error asynchronously, long after the indicator has been appended.
    private AWEncodedString _indicatorId;

    private int _duplicateCount = 0;

    /**
        Constructor to build a single-key error object.

        @aribaapi ariba

        @param  key The object that identifies the error.
        @param  message The message that describes the error.
        @param  errantValue  The unparsable value that the user entered.  Since
                             the parsing failed, we cannot store this value in
                             the field.  We stash it away here so we can display
                             in the UI.
        @param  isWarning  Indicate that this error severity is a warning.
    */
    public AWErrorInfo (Object key, String message, Object errantValue, boolean isWarning)
    {
        Assert.that(key != null, "Cannot construction an AWErrorInfo with key=null");
        if (key instanceof Object[]) {
            Object[] keys = (Object[])key;
            for (int i = 0; i < NumKeys; i++) {
                _keys[i] = keys[i];
            }
        }
        else {
            for (int i = 0; i < NumKeys; i++) {
                _keys[i] = NullKey;
            }
            _keys[SingleKeyIndex] = key;
        }
        initialize(message, errantValue, isWarning);
        /* whether or not this is a true validation error (i.e. set as part of the
           running of the validation handlers. See isValidationError() */
        _isValidationError = false;
    }

    /**
        Constructor to build a multi-key error object.

        @aribaapi ariba

        @param  keys Expected to contain value source, field path, and group name.
        @param  message The message that describes the error.
        @param  errantValue  The unparsable value that the user entered.  Since
                             the parsing failed, we cannot store this value in
                             the field.  We stash it away here so we can display
                             in the UI.
        @param  isWarning  Indicate that this error severity is a warning.
    */
    public AWErrorInfo (Object[] keys, String message, Object errantValue,
                        boolean isWarning)
    {
        for (int i = 0; i < NumKeys; i++) {
            _keys[i] = keys[i];
        }
        initialize(message, errantValue, isWarning);
    }

    public AWErrorInfo (Object vs, String fieldPath, String groupName,
                        String message, Object errantValue, boolean isWarning)
    {
        _keys[ValueSourceKeyIndex] = vs;
        _keys[FieldPathKeyIndex] = (fieldPath != null) ? fieldPath : NullKey;
        _keys[GroupNameKeyIndex] = (groupName != null) ? groupName : NullKey;
        initialize(message, errantValue, isWarning);
    }

    private void initialize (String message, Object errantValue,
                             boolean isWarning)
    {
        _message = message;
        _errantValue = errantValue;
        _isWarning = isWarning;

        if (_message == null) {
            _message = "Unknown Error";
        }
    }

    public boolean isSingleKey ()
    {
        for (int i = 1; i < NumKeys; i++) {
            if (_keys[i] != NullKey) {
                return false;
            }
        }
        return true;
    }

    public static int getNumberOfKeys (Object key)
    {
        if (key instanceof Object[]) {
            Object[] keys = (Object[])key;
            int numKeys = 0;
            for (int i = 0; i < NumKeys; i++) {
                if (keys[i] != NullKey) {
                    numKeys++;
                }
            }
            return numKeys;
        }
        return 1;
    }

    public static Object[] makeKeyArray (Object key)
    {
        if (key instanceof Object[]) {
            return (Object[])key;
        }
        Object[] keys = new Object[NumKeys];
        keys[SingleKeyIndex] = key;
        for (int i = 1; i < NumKeys; i++) {
            keys[i] = NullKey;
        }
        return keys;
    }

    public static Object[] makeKeyArray (Object vs, Object field, Object group)
    {
        Object[] keys = new Object[NumKeys];
        keys[ValueSourceKeyIndex] = (vs == null) ? NullKey : vs;
        keys[FieldPathKeyIndex] = (field == null) ? NullKey : field;
        keys[GroupNameKeyIndex] = (group == null) ? NullKey : group;

        return keys;
    }

    public boolean isWarning ()
    {
        return _isWarning;
    }

    public int getDisplayOrder ()
    {
        return _displayOrder;
    }

    public void setDisplayOrder (int order)
    {
        Assert.that(order == _displayOrder || _displayOrder == NotDisplayed,
            "This error has already been assigned a display order");

        _displayOrder = order;
    }

    public int getUnnavigableDisplayOrder ()
    {
        return _unnavigableDisplayOrder;
    }

    public void setUnnavigableDisplayOrder (int order)
    {
        Assert.that(order == _unnavigableDisplayOrder || _unnavigableDisplayOrder == NotDisplayed,
            "This error has already been assigned a display order");

        _unnavigableDisplayOrder = order;
    }

    public int getRegistrationOrder ()
    {
        return _registrationOrder;
    }

    public void setRegistrationOrder (int order)
    {
        _registrationOrder = order;
    }

    /**
        Returns <code>true</code> if this error is a validation error in the sense
        that it was added to the error manager during the invocation of the
        validation handlers. Returns <code>false</code> otherwise.
     
        @aribaapi ariba
    */
    public boolean isValidationError ()
    {
        return _isValidationError;   
    }

    public List<AWErrorInfo> getErrorInfos (Boolean validationErrors)
    {
        return (validationErrors == null || validationErrors == _isValidationError)
                    ? getErrorInfos()
                    : Collections.<AWErrorInfo>emptyList();
    }

    /**
        Sets whether or not this is a validation error. See {@link #isValidationError()}.
        @aribaapi ariba
    */
    public void setValidationError (boolean value)
    {
        _isValidationError = value;
    }

    public Object getKey ()
    {
        Assert.that(isSingleKey(), "getKey() cannot be called on a multikey error");
        return _keys[SingleKeyIndex];
    }

    public Object[] getKeys ()
    {
        return _keys;
    }

    public String getMessage ()
    {
        return _message;
    }

    public Object getErrantValue ()
    {
        return _errantValue;
    }

    public Object getValueSource ()
    {
        return _keys[ValueSourceKeyIndex];
    }

    public Object getGroupName ()
    {
        return _keys[GroupNameKeyIndex];
    }

    public Object getFieldPath ()
    {
        return _keys[FieldPathKeyIndex];
    }

    public Object getErrorSource ()
    {
        return _errorSource;
    }

    public void setErrorSource (Object source)
    {
        _errorSource = source;
    }

    public boolean keysEqual (Object[] theirKeys)
    {
        Object[] myKeys = getKeys();
        for (int i = 0; i < NumKeys; i++) {
            if (!myKeys[i].equals(theirKeys[i])) {
                return false;
            }
        }

        return true;
    }

    public boolean keysEqualLoosely (Object[] theirKeys)
    {
        Object[] myKeys = getKeys();
        for (int i = 0; i < NumKeys; i++) {
            if (myKeys[i] == NullKey || theirKeys[i] == NullKey) {
                // partial compare is done
                break;
            }
            if (!myKeys[i].equals(theirKeys[i])) {
                return false;
            }
        }

        return true;
    }

    public boolean isSameError (AWErrorInfo error)
    {
        Object src1 = getErrorSource();
        Object src2 = error.getErrorSource();
        if (src1 != null && src2 != null && src1.equals(src2)) {
            // Considered same error if came from the same validity condition
            return true;
        }
        if (getMessage().equals(error.getMessage())) {
            // Considered same error if the error messages are the same
            return true;
        }
        return false;
    }

    public String toString ()
    {
        return Fmt.S("[displayOrder=%s | regOrder=%s | msg='%s' | keys=%s]",
            Integer.toString(getDisplayOrder()),
            Integer.toString(getRegistrationOrder()),
            getMessage(),
            getKeysAsString());
    }

    // Logging should call this method to avoid excessive processing
    public String toString (Logger log)
    {
        if (log.isDebugEnabled()) {
            return toString();
        }
        else {
            return "";
        }
    }


    public String getKeysAsString ()
    {
        Object[] keys = getKeys();
        return getKeysAsString(keys);
    }

    public static String getKeysAsString (Object[] keys)
    {
        FastStringBuffer fsb = new FastStringBuffer();
        fsb.append("[");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                fsb.append(", ");
            }
            Object key = keys[i];
            if (key instanceof String && key.equals(NullKey)) {
                fsb.append("");
            }
            else if (key instanceof String) {
                if (((String)key).length() < 20) {
                    fsb.append(key);
                }
                else {
                    fsb.append(Fmt.S("%s...@%s",
                        ((String)key).substring(0, 20),
                        Integer.toString(System.identityHashCode(key))));
                }
            }
            else {
                fsb.append(Fmt.S("%s@%s", key.getClass().getName(),
                    Integer.toString(System.identityHashCode(key))));
            }
        }
        fsb.append("]");

        return fsb.toString();
    }

    public void incrementDuplicateCount ()
    {
        _duplicateCount++;
    }

    public void decrementDuplicateCount ()
    {
        if (_duplicateCount > 0) {
            _duplicateCount--;
        }
    }

    public int getDuplicateCount ()
    {
        return _duplicateCount;
    }

    /*-----------------------------------------------------------------------
        Implementation of AWErrorBucket
      -----------------------------------------------------------------------*/

    public boolean isSingleErrorBucket ()
    {
        return true;
    }

    public AWErrorBucket add (AWErrorInfo error)
    {
        AWErrorBucket newBucket = new AWErrorManager.MultiErrorBucket(this);
        newBucket.add(error);
        return newBucket;
    }

    public boolean isDuplicateError (AWErrorInfo error)
    {
        return this.isSameError(error);
    }

    public boolean hasErrorsWithSeverity (Boolean isWarning)
    {
        return isWarning == null || isWarning() == isWarning.booleanValue();
    }

    public AWErrorInfo getFirstError (Boolean isWarning)
    {
        if (isWarning == null) {
            return this;
        }
        else {
            return (isWarning() == isWarning.booleanValue()) ? this : null;
        }
    }

    public AWErrorInfo get (int i)
    {
        Assert.that(i == 0,
            "AWErrorInfo is a single-error bucket. Cannot get error at index %s",
            Integer.toString(i));
        return this;
    }

    public int size ()
    {
        return 1;
    }

    public List<AWErrorInfo> getErrorInfos ()
    {
        return Collections.singletonList(this);
    }

    public boolean hasDuplicate ()
    {
        return _duplicateCount > 0;
    }

    public Object getAssociatedTableItem ()
    {
        return _tableItem;
    }

    public AWComponent getAssociatedDataTable ()
    {
        return _datatable;
    }

    public void setAssociatedTableItem (AWComponent table, Object item)
    {
        _datatable = table;
        _tableItem = item;
    }

    /*-----------------------------------------------------------------------
        End Implementation of AWErrorBucket
      -----------------------------------------------------------------------*/

    public AWEncodedString getIndicatorId ()
    {
        return _indicatorId;
    }

    public void setIndicatorId (AWEncodedString id)
    {
        _indicatorId = id;
    }

    public boolean getWasTableAutoScrolled ()
    {
        return _wasTableAutoScrolled;
    }


    public void setWasTableAutoScrolled (boolean scrolled)
    {
        _wasTableAutoScrolled = scrolled;
    }
}
