/*
    Copyright (c) 1996-2008 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/DatabaseBaseId.java#10 $
    Responsible: bluo
*/

package ariba.util.core;

/**
    This is a stripped down <code>BaseId</code>, for use in tools
    (such as migration) below the level of <code>ObjectServer</code>.

    This is used by ariba.base.core.BaseId

    @aribaapi documented
*/
abstract public class DatabaseBaseId
{
    /*-----------------------------------------------------------------------
        Fields
      -----------------------------------------------------------------------*/

    /**
        @aribaapi private
    */
    public long  typeCode = 0;
    
    /**
        @aribaapi private
    */
    public long value    = 0;

    /**
        @aribaapi private
    */
    public static final int OptimizedBaseIdWidth = 25;

    /*-----------------------------------------------------------------------
        Constructor
      -----------------------------------------------------------------------*/

    /**
        Constructs a new DatabaseBaseId.
        
        @aribaapi private
    */
    public DatabaseBaseId ()
    {
    }


    /**
        Checks if this BaseId is temporary.  Temporary BaseIds cannot be
        stored in the database.

        @return <b>true</b> is the baseId is temporary, <b>false</b> otherwise.
        @aribaapi documented
    */
    public boolean isTemp ()
    {
        return value == 0;
    }

    /*-----------------------------------------------------------------------
        Conversion
      -----------------------------------------------------------------------*/
     
    /**
        Converts the type code and value to a string.

        @return a String representation of the typecode and value
        @aribaapi documented
    */
    abstract public String toDBString ();

    /**
        Converts the type code to a string.

        @return a String representation of a type code.
        @aribaapi documented
    */
    abstract public String getTypeCodeString ();

}
