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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWErrorBucket.java#8 $
*/

package ariba.ui.aribaweb.core;

import java.util.List;

/**
    This interface allows uniform access to a bucket that holds
    one or more errors that share the same error keys.  This allows
    different implementations for the single error and multiple error cases.

    @aribaapi ariba
*/
public interface AWErrorBucket
{
    public boolean isSingleErrorBucket ();
    public int getDisplayOrder ();
    public int getUnnavigableDisplayOrder ();
    public int getRegistrationOrder ();
    public void setRegistrationOrder (int order);
    public Object getKey ();
    public Object[] getKeys ();
    public boolean keysEqual (Object[] theirKeys);
    public boolean isDuplicateError (AWErrorInfo error);
    public boolean hasErrorsWithSeverity (Boolean isWarning);
    public AWErrorInfo getFirstError (Boolean isWarning);
    public AWErrorBucket add (AWErrorInfo error);
    public int size ();
    public AWErrorInfo get (int i);
    public boolean hasDuplicate ();
    public AWComponent getAssociatedDataTable ();
    public Object getAssociatedTableItem ();
    public void setAssociatedTableItem (AWComponent table, Object item);

    /**
        Returns a list of all the <code>AWErrorInfos</code> in <code>this</code>.
        @aribaapi ariba
    */
    public List<AWErrorInfo> getErrorInfos();

    /**
        Returns a list of all the error infos in <code>this</code>
        that satisfy the condition
        {@link AWErrorInfo#isValidationError()} == <code>validationErrors</code>.
        All error infos are returned if <code>validationErrors == null</code>.
        @aribaapi ariba
    */
    public List<AWErrorInfo> getErrorInfos (Boolean validationErrors);
}
