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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/ObjectContextDataSource.java#2 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.table.AWTDataSource;
import ariba.ui.table.AWTEntity;
import ariba.ui.table.AWTSortOrdering;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.ObjectMeta;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.ArrayList;

public class ObjectContextDataSource extends AWTDataSource
{
    Class _entityClass;
    QuerySpecification _querySpecification;
    boolean _doSortInQuery = true;
    ObjectContext.ChangeWatch _changeWatch;

    public ObjectContextDataSource (Class entityClass)
    {
        setEntityClass(entityClass);
    }

    public List fetchObjects()
    {
        return (_querySpecification != null) ? ObjectContext.get().executeQuery(_querySpecification) : null;
    }

    public void setSortOrderings (List<AWTSortOrdering> orderings)
    {
        _querySpecification.setSortOrderings((orderings == null || !_doSortInQuery) ? null : createSortOrderings(orderings));
    }

    public boolean doSortInQuery ()
    {
        return _doSortInQuery;
    }

    public void setDoSortInQuery (boolean doSortInQuery)
    {
        _doSortInQuery = doSortInQuery;
    }

    public boolean dataSourceDoesSort ()
    {
        return _doSortInQuery;
    }

    public Object insert()
    {
        if (_entityClass == null) return null;
        return ObjectContext.get().create(_entityClass);
    }

    public void delete(Object object)
    {
        if (_entityClass == null) return;
        ObjectContext.get().remove(object);
    }

    public boolean hasChanges ()
    {
        if (_changeWatch == null) {
            _changeWatch = ObjectContext.get().createChangeWatch();
        }
        return _changeWatch.hasChanged();
    }

    public Class getEntityClass ()
    {
        return _entityClass;
    }

    public void setEntityClass (Class entityClass)
    {
        if (entityClass != _entityClass) {
            _entityClass = entityClass;
            _querySpecification = (_entityClass != null) ? new QuerySpecification(_entityClass.getName()) : null;
        }
    }

    public QuerySpecification getQuerySpecification ()
    {
        return _querySpecification;
    }

    public void setQuerySpecification (QuerySpecification querySpecification)
    {
        _querySpecification = querySpecification;
    }

    public static SortOrdering createSortOrdering (AWTSortOrdering ordering)
    {
        return new SortOrdering(ordering.key(), SortOrdering.Direction.values()[ordering.selector()]);
    }

    public static List<SortOrdering>createSortOrderings (List<AWTSortOrdering> orderings)
    {
        List<SortOrdering> result = ListUtil.list(orderings.size());
        for (AWTSortOrdering ordering : orderings) {
            result.add(createSortOrdering(ordering));
        }
        return result;
    }
}
