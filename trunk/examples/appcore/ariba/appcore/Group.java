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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/Group.java#4 $
*/
package ariba.appcore;

import ariba.ui.meta.annotations.*;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.List;
import ariba.util.core.*;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.aribaweb.util.AWUtil;
import org.compass.annotations.*;

@Entity @NavModuleClass @Searchable
public class Group extends Principal
{
    public enum DefaultGroup { AdminUsers, AnonymousUsers, DefaultUsers }
}