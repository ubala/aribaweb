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

    $Id: //ariba/platform/util/core/ariba/util/core/ComponentChecksum.java#7 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.io.Serializable;
import java.util.List;

/**
    The abstract class to support checking the that
    the runtime configuration of components are consistent
    within a cluster.

    @aribaapi ariba
*/
public abstract class ComponentChecksum implements Serializable
{
    protected String name;

    public ComponentChecksum (String name)
    {
        this.name = name;
    }

    public ComponentChecksum ()
    {
    }

    /**
        Return the name of the component

        @return the name
        @aribaapi ariba
    */
    public String getName ()
    {
        return name;
    }

    /**
        Return the checksum of the component.  The checksum is an
        aribitrary byte array.  It is up the component implementing
        this class to determine what a sensible checksum consists of.

        @return the component's checksum as a byte array
        @aribaapi ariba
    */
    public abstract byte[] getChecksum ();

    /**
        compare the current checksum with another one.
        
        @return false if they aren't the same, true if they are
        @param otherChecksum checksum to compare
        @aribaapi ariba
    */
    public boolean compareChecksum (byte[] otherChecksum)
    {
            // Easy comparison first
        byte[] myChecksum = getChecksum();
        if (myChecksum.length != otherChecksum.length) {
            return false;
        }
        for (int i = 0; i < myChecksum.length; i++) {
            if (myChecksum[i] != otherChecksum[i]) {
                printChecksums(myChecksum, otherChecksum);
                return false;
            }
        }
        return true;
    }

    /**
        @aribaapi private
    */
    private void printChecksums (byte []mine, byte []theirs)
    {
        if (!Log.startupUtil.isDebugEnabled()) {
            return;
        }
        List mineList = makeListOfBytes(mine);
        List theirsList = makeListOfBytes(theirs);
        Log.startupUtil.debug("ComponentChecksum for component %s " +
                              " mine = %s, theirs = %s, equal = %s",
                              name,
                              mineList,
                              theirsList,
                              Constants.getBoolean(mineList.equals(theirsList)));
                              
    }

    /**
        @aribaapi private
    */
    public static List makeListOfBytes (byte []b)
    {
        List v = ListUtil.list(b.length);
        for (int i=0; i<b.length; i++) {
            v.add(Constants.getInteger(b[i]));
        }
        return v;
    }

    /**
        @return a human readable version of this checksum
        @aribaapi ariba
    */
    public String toString ()
    {
        FastStringBuffer str = new FastStringBuffer(getName());
        str.append(": (");
        byte[] b = getChecksum();
        for (int i = 0; i < b.length; i++) {
            str.append(Byte.toString(b[i]));
            str.append(" ");
        }
        str.append(")");
        return str.toString();
    }
    
}          
    
    
    
