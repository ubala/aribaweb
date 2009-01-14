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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaSearch.java#2 $
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

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        String className = (String)MetaContext.currentContext(this).values().get(ObjectMeta.KeyClass);
        Class cls = (className != null) ? ClassUtil.classForName(className) : null;
        if (_displayGroup == null) {
            _displayGroup = new AWTDisplayGroup();
            _displayGroup.setDataSource(new ObjectContextDataSource(cls));
        } else {
            ((ObjectContextDataSource)_displayGroup.dataSource()).setEntityClass(cls);
        }

        super.renderResponse(requestContext, component);
    }

    public void setQuerySpecification (QuerySpecification spec)
    {
        ((ObjectContextDataSource)_displayGroup.dataSource()).setQuerySpecification(spec);
        _displayGroup.fetch();
    }
}