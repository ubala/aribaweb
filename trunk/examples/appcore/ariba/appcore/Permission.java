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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/Permission.java#5 $
*/
package ariba.appcore;

import org.compass.annotations.SearchableId;
import org.compass.annotations.SearchableProperty;

import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Entity;

import ariba.ui.meta.annotations.Trait;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.Fmt;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Assert;

@Entity @NavModuleClass
public class Permission
{
    @Id @GeneratedValue @SearchableId
    private Integer id;

    @Trait.LabelField @SearchableProperty
    String name;

    public Integer getId ()
    {
        return id;
    }

    public String getName ()
    {
        return name;
    }

    public void setName (String name)
    {
        this.name = name;
    }

    static GrowOnlyHashtable _idsByName = new GrowOnlyHashtable();

    public static int idForPermissionName (String name)
    {
        Integer id = (Integer)_idsByName.get(name);
        if (id == null) {
            synchronized (PermissionSet.class) {
                Permission p = PermissionSet.getContext().findOne(Permission.class, AWUtil.map("name", name));
                Assert.that(p != null, "Lookup of unknown permission: %s", name);
                id = p.getId();
                _idsByName.put(name, id);
            }
        }
        return id.intValue();
    }

    public static Permission permissionForName (String name)
    {
        return ObjectContext.get().find(Permission.class, idForPermissionName(name));
    }

    public enum ClassOperation { create, edit, view, search }

    public static final ClassOperation[] AllClassOperations = { ClassOperation.create, ClassOperation.edit, ClassOperation.view, ClassOperation.search };

    public static String nameForClassOp (String className, ClassOperation op)
    {
        return Fmt.S("%s:%s", className, op.name());
    }

    public static Permission permissionForClassOp (String className, ClassOperation op)
    {
        return permissionForName(nameForClassOp(className, op));
    }
}