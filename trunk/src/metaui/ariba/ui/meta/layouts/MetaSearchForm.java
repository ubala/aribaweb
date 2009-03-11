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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaSearchForm.java#7 $
*/

package ariba.ui.meta.layouts;

import ariba.ui.meta.persistence.Predicate;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.PersistenceMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Context;
import ariba.ui.aribaweb.core.AWComponent;

import java.util.Map;

public class MetaSearchForm extends AWComponent
{
    public Map _searchMap = new PersistenceMeta.SearchMap();
    String _searchOperation;
    public boolean _supportsTextSearch;
    public Object _mid;

    public boolean isStateless()
    {
        return false;
    }

    public String searchOperation ()
    {
        if (_searchOperation == null) {
            Context context = MetaContext.currentContext(this);
            _searchOperation = (String)context.propertyForKey("searchOperation");
            _supportsTextSearch = context.booleanPropertyForKey("textSearchSupported", false);
        }
        return _searchOperation;
    }

    public void setSearchOperation (String op)
    {
        _searchOperation = op;
    }

    public void init ()
    {
        super.init();
        MetaSearch.setupDisplayGroup(requestContext());
    }

    public boolean showSearchTypeChooser ()
    {
        return _supportsTextSearch;
    }

    public void search ()
    {
        if (errorManager().checkErrorsAndEnableDisplay()) return;
        Predicate pred = Predicate.fromKeyValueMap(_searchMap);
        Context context = MetaContext.currentContext(this);
        String className = (String)context.values().get(UIMeta.KeyClass);
        QuerySpecification spec = new QuerySpecification(className, pred);

        context.push();
        context.setScopeKey(UIMeta.KeyClass);
        spec.setUseTextIndex(pred != null
                && context.booleanPropertyForKey(PersistenceMeta.PropUseTextSearch, false));
        context.pop();

        MetaSearch.updateQuerySpecification(requestContext(), spec);
    }
}
