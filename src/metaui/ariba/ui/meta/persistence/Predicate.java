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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/Predicate.java#5 $
*/
package ariba.ui.meta.persistence;

import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;

abstract public class Predicate implements QueryGenerator.Visitor
{
    public enum Operator { Eq, Neq, Gt, Gte, Lt, Lte };

    public static Predicate fromKeyValueMap (Map<String, Object> toMatch)
    {
        if (toMatch == null) return null;
        List <Predicate> preds = new ArrayList();
        for (Map.Entry<String,Object> e : toMatch.entrySet()) {
            Object val = e.getValue();
            if (val instanceof RangeValue) {
                addKeyValueToPreds(e.getKey(), ((RangeValue)val).from, Operator.Gte, preds);
                addKeyValueToPreds(e.getKey(), ((RangeValue)val).to, Operator.Lte, preds);
            } else {
                addKeyValueToPreds(e.getKey(), val, Operator.Eq, preds);
            }
        }
        return (preds.size() == 0) ? null
                : (preds.size() == 1)
                    ? preds.get(0) : new And(preds);
    }

    static void addKeyValueToPreds (String key, Object val, Operator op, List <Predicate> preds)
    {
        if (val != null
                && (!(val instanceof String) || !StringUtil.nullOrEmptyOrBlankString((String)val))
                && (!(val instanceof Collection) || !((Collection)val).isEmpty())) {
            Predicate p = new KeyValue(key, val, op);
            preds.add(p);
        }
    }

    public static class KeyValue extends Predicate
    {
        String _key;
        Object _value;
        Operator _operator;

        public KeyValue (String key, Object value, Operator op)
        {
            _key = key;
            _value = value;
            _operator = op;
        }

        public KeyValue (String key, Object value)
        {
            this(key, value, Operator.Eq);
        }

        public void generate(QueryGenerator generator)
        {
            generator.add(_key, _value, _operator);
        }

        public String getKey ()
        {
            return _key;
        }

        public Object getValue ()
        {
            return _value;
        }
    }

    abstract public static class Junction extends Predicate
    {
        List<Predicate> _predicates;

        public Junction (List<Predicate> predicates)
        {
            _predicates = predicates;
        }

        public Junction (Predicate ...predicates)
        {
            _predicates = ListUtil.arrayToList(predicates);
        }

        public void generate(QueryGenerator generator)
        {
            int count = _predicates.size();
            if (count != 0) generator.appendToWhere("(");
            for (int i=0; i < count; i++) {
                Predicate p = _predicates.get(i);
                p.generate(generator);
                if (count > 1 && (i != count - 1)) generator.appendToWhere(operatorString());
            }
            if (count != 0) generator.appendToWhere(")");
        }

        protected abstract String operatorString ();

        public List<Predicate> getPredicates ()
        {
            return _predicates;
        }
    }

    public static class And extends Junction
    {
        List<Predicate> _predicates;

        public And (List<Predicate> predicates)
        {
            super(predicates);
        }

        public And (Predicate ...predicates)
        {
            super(predicates);
        }

        protected String operatorString()
        {
            return " AND ";
        }
    }

    public static class Or extends Junction
    {
        List<Predicate> _predicates;

        public Or (List<Predicate> predicates)
        {
            super(predicates);
        }

        public Or (Predicate ...predicates)
        {
            super(predicates);
        }

        protected String operatorString()
        {
            return " OR ";
        }
    }

    /**
        When used as a value in map for fromKeyValueMap, results in > < used in predicate
     */
    public static class RangeValue
    {
        public Object from, to;
    }
}
