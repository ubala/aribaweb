/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/PermissionSet.java#4 $
*/
package ariba.appcore;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Collection;
import java.util.Collections;

public class PermissionSet
{
    int _version;
    HashSet<Integer> _permissions;

    static int _GroupVersion = 0;

    public PermissionSet (Collection<Permission> permissionList, PermissionSet base)
    {
        _version = _GroupVersion;
        _permissions = new HashSet();
        if (base != null) _permissions.addAll(base._permissions);

        if (permissionList != null) {
            for (Permission p : permissionList) {
                _permissions.add(p.getId());
            }
        }
    }

    public PermissionSet (Collection<PermissionSet> permissionSets)
    {
        _version = _GroupVersion;
        _permissions = new HashSet();

        for (PermissionSet s : permissionSets) {
            _permissions.addAll(s._permissions);
        }
    }

    public boolean hasPermission (int id)
    {
        return _permissions != null && _permissions.contains(id);
    }

    public boolean isStale ()
    {
        return _version < _GroupVersion;
    }

    public static PermissionSet permissionSetForGroups (List<Group> groupList)
    {
        if (groupList == null) groupList = Collections.EMPTY_LIST;
        PermissionSet ps = (PermissionSet)_permissionSetByGroupList.get(groupList);
        if (ps == null) {
            List<PermissionSet> inherited = AWUtil.collect(groupList, new AWUtil.ValueMapper() {
                    public Object valueForObject (Object group)
                    {
                        return ((Principal)group).permissionSet();
                    }
                });
            ps = new PermissionSet(inherited);
            // Todo: should store with list of *ids* not actual Groups
            _permissionSetByGroupList.put(groupList, ps);
        }
        return ps;
    }

    // ToDo: listen for notification on edit to Groups and bump _GroupVersion

    static ObjectContext _ObjectContext;
    static GroupListGrowOnlyHashtable _permissionSetByGroupList = new GroupListGrowOnlyHashtable();

    static ObjectContext getContext()
    {
        if (_ObjectContext == null) {
            _ObjectContext = ObjectContext.createContext();
        }
        return _ObjectContext;
    }

    static class GroupListGrowOnlyHashtable extends GrowOnlyHashtable
    {
        protected int getHashValueForObject (Object o)
        {
            int h = 7;
            for (Group g : ((List<Group>)o)) {
                h = h*33 + g.getId().intValue();
            }
            return h;
        }

        protected boolean objectsAreEqualEnough (Object obj1, Object obj2)
        {
            return obj1 == obj2 || _groupListsEqual((List<Group>)obj1, (List<Group>)obj2);
        }

        boolean _groupListsEqual (List<Group> l1, List<Group> l2)
        {
            int c = l1.size();
            if (c != l2.size()) return false;
            for (int i=0; i<c; i++) {
                if (!l1.get(i).getId().equals(l2.get(i).getId())) return false;
            }
            return true;
        }
    }
}
