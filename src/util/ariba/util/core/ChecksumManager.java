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

    $Id: //ariba/platform/util/core/ariba/util/core/ChecksumManager.java#12 $
*/

package ariba.util.core;

import ariba.util.log.Log;
import java.util.List;

/**
    Service that manages ComponentChecksums.  Used during the
    startup process to verify that all nodes within a group
    are configured the same way.

    Each component needs to decide what constitutes that component's
    configuration, and what sort of checksum will be adequate to catch
    inconsistencies.  This information is embodied in the ComponentChecksum
    abstract class.  The ComponentChecksum consists of a component name,
    which must be unique, and an arbitrary length byte array.  There
    are two concrete classes provided that extend ComponentChecksum:
    FileComponentChecksum, for handling components who's entire state is
    stored in files, and SimpleComponentChecksum that will accept a
    byte array as the checksum.

    Components will register specific ComponentChecksums with the
    manager.  The BaseServer will get all the checksums and compare
    them with checksums on remote machines to decide whether or not
    a node is configured the same as the cluster.

    See http://development.ariba.com/PSS/CoreServer/multiserver/Component Checksum.doc
    and http://development.ariba.com/PSS/CoreServer/multiserver/Group Restart.doc
    for details on how all this works.

    @aribaapi ariba
*/
public class ChecksumManager
{
    private static State state;

    /**
        Get the ChecksumManager service. Create the instance if one
        does not exist.  This follows the standard Ariba service pattern.

        @return the ChecksumManager
        @aribaapi ariba
    */
    public static ChecksumManager getChecksumManager ()
    {
        ChecksumManager serv = (ChecksumManager)getState().get();
        if (serv == null) {
            synchronized (ChecksumManager.class) {
                serv = (ChecksumManager)getState().get();
                if (serv == null) {
                    serv = new ChecksumManager();
                    getState().set(serv);
                }
            }
        }
        return serv;
    }

    /**
        Set the instance of the checksum manager.  Only called
        by getChecksumManger

        @param serv The instance
        @aribaapi private
    */
    public static void setChecksumManager (ChecksumManager serv)
    {
        synchronized (ChecksumManager.class) {
            getState().set(serv);
        }
    }


    /**
        Get the State in which to store the instance.  Create a new
        GlobalState if it does not exist.  This service is only saved
        as a Global State

        @return State variable
        @aribaapi private
    */
    public static State getState ()
    {
        if (state == null) {
            synchronized (ChecksumManager.class) {
                if (state == null) {
                    state = new GlobalState();
                }
            }
        }
        return state;
    }

    /**
        Set the state.  Only called from getState.

        @param s The state variable
        @aribaapi private
    */
    public static void setState (State s)
    {
        synchronized (ChecksumManager.class) {
            state = s;
        }
    }


    private GrowOnlyHashtable checksums;

    public ChecksumManager ()
    {
        checksums = new GrowOnlyHashtable();
    }

    /**
        register the component checksum with the manager.  Throws
        ComponentChecksumException if the component is already registered.

        @param cc the component checksum from some package
        @aribaapi ariba
    */
    public void register (ComponentChecksum cc)
      throws ComponentChecksumException
    {
        Log.startupUtil.debug("ChecksumManager: register %s", cc);
        String componentName = cc.getName();
        if (checksums.containsKey(componentName)) {
            throw new ComponentChecksumException(
                Fmt.S("Duplicate component name: %s", componentName));
        }
        synchronized(checksums) {
            if (checksums.containsKey(componentName)) {
                throw new ComponentChecksumException(
                    Fmt.S("Duplicate component name: %s", componentName));
            }
            checksums.put(componentName, cc);
        }

    }

    /**
        re-register the component checksum with the manager.  Replace the
        old value if the component already exists

        @param cc the component checksum from some package
        @aribaapi ariba
    */
    public void reRegister (ComponentChecksum cc)
      throws ComponentChecksumException
    {
        Log.startupUtil.debug("ChecksumManager: reRegister %s", cc);
        checksums.put(cc.getName(), cc);
    }

    /**
        return the List of all component checksums.  This vector will be
        sorted on increasing key value

        @return List of all registered ComponentChecsums
        @aribaapi ariba
    */
    public List getAll ()
    {
        List v;
        synchronized (checksums) {
                // get an array of the keys
            Object[] keys = checksums.keysList().toArray();
                // and sort them
            Sort.objects(keys, StringCompare.self);
            v = ListUtil.list();
            for (int i = 0; i < keys.length; i++) {
                v.add(checksums.get(keys[i]));
            }
        }
        return v;
    }

    /**
        check to see if the component checksums from another
        node match the current component checksums. Return
        true if everything matchs, false otherwise

        @aribaapi ariba
    */
    public boolean compareChecksums ()
    {
        List myChecksums = getAll();
            // easy checks first
        if (remoteChecksums == null) {
            Log.util.debug("remoteChecksum is null");
            return false;
        }
        if (myChecksums.size() != remoteChecksums.size()) {
            Log.util.warning(7030, myChecksums.size(), remoteChecksums.size());
            return false;
        }
            // Look at each element.  Make sure the names are the
            // same, and the values are the same
        for (int i = 0; i < myChecksums.size(); i++) {
            ComponentChecksum myElem =
                (ComponentChecksum)myChecksums.get(i);
            ComponentChecksum otherElem =
                (ComponentChecksum)remoteChecksums.get(i);
            Log.startupUtil.debug("verifying checksum for %s", myElem.getName());
            if (! myElem.getName().equals(otherElem.getName())) {
                Log.util.warning(7031,  myElem.getName(), otherElem.getName());
                return false;
            }
            if (! myElem.compareChecksum(otherElem.getChecksum())) {
                Log.util.warning(7032, myElem.getName(), myElem.getChecksum(),
                                     otherElem.getChecksum());
                return false;
            }
        }
        return true;
    }

    private List remoteChecksums = null;

    /**
        set the remote checksum in the Checksum manager, should be only be called by one thread
        @param checksums the List of ComponentChecksums from other nodes
        @aribaapi ariba
    */
    public void setRemoteChecksums (List checksums)
    {
        remoteChecksums = checksums;
    }

    /**
        get the remote checksum in the Checksum manager
        Return the List of ComponentChecksums from other nodes
        @aribaapi ariba
    */

    public List getRemoteChecksums ()
    {
        return remoteChecksums;
    }

}

