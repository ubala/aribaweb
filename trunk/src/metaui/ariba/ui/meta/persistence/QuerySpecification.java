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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/QuerySpecification.java#3 $
*/
package ariba.ui.meta.persistence;

import java.util.List;

public class QuerySpecification
{
    String _entityName;
    Predicate _predicate;
    List<SortOrdering> _sortOrderings;
    boolean _useTextIndex;

    public QuerySpecification (String rootEntity)
    {
        this(rootEntity, null);
    }

    public QuerySpecification (String rootEntity, Predicate predicate)
    {
        _entityName = rootEntity;
        _predicate = predicate;
    }

    public String getEntityName()
    {
        return _entityName;
    }

    public Predicate getPredicate()
    {
        return _predicate;
    }

    public void setPredicate(Predicate predicate)
    {
        _predicate = predicate;
    }

    public List<SortOrdering> getSortOrderings ()
    {
        return _sortOrderings;
    }

    public void setSortOrderings (List<SortOrdering> sortOrderings)
    {
        _sortOrderings = sortOrderings;
    }

    public boolean useTextIndex ()
    {
        return _useTextIndex;
    }

    public void setUseTextIndex (boolean useTextIndex)
    {
        _useTextIndex = useTextIndex;
    }
}
