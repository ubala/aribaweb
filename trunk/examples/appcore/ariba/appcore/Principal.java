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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/Principal.java#5 $
*/
package ariba.appcore;

import org.compass.annotations.SearchableId;
import org.compass.annotations.SearchableProperty;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import ariba.ui.meta.annotations.Trait;
import ariba.util.core.ListUtil;

import java.util.List;

@Entity
public class Principal
{
    @Id @GeneratedValue @SearchableId
    private Long id;

    @Trait.LabelField @SearchableProperty
    String name;

    @ManyToMany
    List<Permission> permissions;

    @ManyToMany
    List<Group> memberOf;

    public Long getId ()
    {
        return id;
    }

    public List<Group> getMemberOf ()
    {
        return memberOf;
    }

    public void setMemberOf (List<Group> memberOf)
    {
        this.memberOf = memberOf;
    }

    @Transient PermissionSet permissionSet;

    public String getName ()
    {
        return name;
    }

    public void setName (String name)
    {
        this.name = name;
    }

    public List<Permission> getPermissions ()
    {
        return permissions;
    }

    public void setPermissions (List<Permission> permissions)
    {
        this.permissions = permissions;
    }

    public void addToPermissions (Permission p)
    {
        if (permissions == null) permissions = ListUtil.list();
        ListUtil.addElementIfAbsent(permissions, p);
    }

    public PermissionSet permissionSet ()
    {
        // Todo: should have cache by memberOf list (most entities have no local permission and share memberOf list)
        if (permissionSet == null || permissionSet.isStale()) {
            permissionSet = PermissionSet.permissionSetForGroups(memberOf);
            if (!ListUtil.nullOrEmptyList(permissions)) permissionSet = new PermissionSet(getPermissions(), permissionSet);
        }
        return permissionSet;
    }

    public boolean hasPermission (int id)
    {
        return permissionSet().hasPermission(id);
    }

    public boolean hasPermissions (List<Integer> ids, boolean all)
    {
        if (ListUtil.nullOrEmptyList(ids)) return true;
        for (Integer id : ids) {
            if (hasPermission(id)) {
                if (!all) return true;
            } else {
                if (all) return false;
            }
        }
        return all;
    }

    public boolean hasPermission (String name)
    {
        return permissionSet().hasPermission(Permission.idForPermissionName(name));
    }
}
