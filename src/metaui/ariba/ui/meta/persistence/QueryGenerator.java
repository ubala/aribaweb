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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/QueryGenerator.java#5 $
*/
package ariba.ui.meta.persistence;

import ariba.util.core.Fmt;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import ariba.ui.meta.persistence.Predicate.Operator;

public class QueryGenerator
{
    QuerySpecification _spec;
    Map<String, String> _aliases = new HashMap();
    int _nextEntityId = 0;
    Map<String, Object> _values = new HashMap();
    int _nextValueId = 0;
    StringBuffer _whereClause = new StringBuffer();
    TypeProvider _typeProvider;

    public interface Visitor
    {
        void generate (QueryGenerator generator);
    }

    public QueryGenerator (QuerySpecification spec, TypeProvider typeProvider)
    {
        _spec = spec;
        _typeProvider = typeProvider;
    }

    public String generate ()
    {
        if (_spec.getPredicate() != null) _spec.getPredicate().generate(this);

        StringBuffer sb = new StringBuffer();

        sb.append("SELECT ");
        sb.append(rootAlias());
        sb.append(" FROM ");

        for (Map.Entry<String,String> e : _aliases.entrySet()) {
            sb.append(e.getKey());
            sb.append(" ");
            sb.append(e.getValue());
        }

        if (_whereClause.length() > 0) {
            sb.append(" WHERE ");
            sb.append(_whereClause);
        }

        if (_spec.getSortOrderings() != null) generateOrderBy(_spec.getSortOrderings(), sb);

        return sb.toString();
    }

    public Map queryParams()
    {
        return _values;
    }

    void appendToWhere (String string)
    {
        _whereClause.append(string);
    }

    String formatKeyPath (String keyPath)
    {
        return Fmt.S("%s.%s", rootAlias(), keyPath);
    }

    private String rootAlias()
    {
        return aliasForEntity(_spec.getEntityName());
    }

    String formatOperator (Operator op)
    {
        if (Operator.Eq == op) return "=";
        else if (Operator.Neq == op) return "<>";
        else if (Operator.Gt == op) return ">";
        else if (Operator.Gte == op) return ">=";
        else if (Operator.Lt == op) return "<";
        else if (Operator.Lte == op) return "<=";
        Assert.that(false, "Unsupported operator: %s", op);
        return null;
    }

    String formatValue (Object value)
    {
        String id = "v" + _nextValueId++;
        _values.put(id, value);
        return ":".concat(id);
    }

    void add (String keyPath, Object value, Predicate.Operator operator)
    {
        if (operator == Operator.Eq && (value instanceof String) && ((String)value).endsWith("*")) {
            // Todo: proper escaping (of "%" and "_")
            String likePattern = ((String)value).replace("*", "%");
            appendToWhere(Fmt.S("%s LIKE %s",
                    formatKeyPath(keyPath),
                    formatValue(likePattern)));
        } else {
            appendToWhere(Fmt.S("%s %s %s",
                    formatKeyPath(keyPath),
                    formatOperator(operator),
                    formatValue(value)));
        }
    }

    void generateOrderBy (List<SortOrdering> orderings, StringBuffer buf)
    {
        boolean first = true;
        for (SortOrdering ordering : orderings) {
            if (first) {
                buf.append(" order by ");
                first = false;
            } else {
                buf.append(", ");
            }
            TypeProvider.Info type = _typeProvider.infoForKeyPath(ordering.getKey());
            Class typeClass = type != null ? type.typeClass() : null;
            boolean useCaseInsensitive = ordering.isCaseInsensitive()
                                                && typeClass != null
                                                && ClassUtil.instanceOf(typeClass, String.class);
            buf.append(Fmt.S((useCaseInsensitive ? "LOWER(%s)" : "%s"), formatKeyPath(ordering.getKey())));
            buf.append(ordering.isAscending() ? " asc" : " desc");
        }
    }

    protected String aliasForEntity (String name)
    {
        String alias = _aliases.get(name);
        if (alias == null) {
            alias = "e" + _nextEntityId++;
            _aliases.put(name, alias);
        }
        return alias;
    }
}
