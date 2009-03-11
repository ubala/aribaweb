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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaSearch.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.meta.persistence.Predicate;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.ObjectContextDataSource;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.meta.core.Context;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class MetaSearch extends AWComponent
{
    public AWTDisplayGroup _displayGroup;
    public Map _searchMap = new HashMap();

    public boolean isStateless()
    {
        return false;
    }

    public static AWTDisplayGroup setupDisplayGroup (AWRequestContext requestContext)
    {
        UIMeta.UIContext ctx = MetaContext.currentContext(requestContext.getCurrentComponent());
        AWTDisplayGroup displayGroup = (AWTDisplayGroup)ctx.values().get("displayGroup");
        Assert.that(displayGroup != null, "MetaSearch used without displayGroup in Meta Context");
        String className = (String)ctx.values().get(ObjectMeta.KeyClass);
        Class cls = (className != null) ? ClassUtil.classForName(className) : null;
        ObjectContextDataSource dataSource = (ObjectContextDataSource)displayGroup.dataSource();
        if (dataSource == null) {
            dataSource = new ObjectContextDataSource(cls);
            displayGroup.setDataSource(dataSource);
        }
        else {
            dataSource.setEntityClass(cls);
        }

        return displayGroup;
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        _displayGroup = setupDisplayGroup(requestContext);

        super.renderResponse(requestContext, component);
    }

    public static void updateQuerySpecification (AWRequestContext requestContext, QuerySpecification spec)
    {
        AWTDisplayGroup displayGroup = setupDisplayGroup(requestContext);

        ((ObjectContextDataSource)displayGroup.dataSource()).setQuerySpecification(spec);
        displayGroup.fetch();
    }
}