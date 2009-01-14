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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaSearchForm.java#1 $
*/

package ariba.ui.meta.layouts;

import ariba.ui.meta.persistence.Predicate;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWRequestContext;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class MetaSearchForm extends AWComponent
{
    public Map _searchMap = new HashMap();
    ObjectContext.ChangeWatch _changeWatch;

    public boolean isStateless()
    {
        return false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // Issue search on first time, or upon changes in our group
        boolean firstTime = false;
        if (_changeWatch == null) {
            _changeWatch = ObjectContext.get().createChangeWatch();
            firstTime = true;
        }
        if (firstTime || _changeWatch.hasChanged()) search();

        super.renderResponse(requestContext, component);
    }

    public void search ()
    {
        Predicate pred = Predicate.fromKeyValueMap(_searchMap);
        String className = (String)MetaContext.currentContext(this).values().get(UIMeta.KeyClass);
        List result =
            ObjectContext.get().executeQuery(new QuerySpecification(className, pred));
        setValueForBinding(result, AWBindingNames.list);
    }
}
