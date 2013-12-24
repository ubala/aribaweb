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

    $Id: //ariba/platform/ui/metaui-jpa/ariba/ui/meta/jpa/CompassQueryProcessor.java#2 $
*/
package ariba.ui.meta.jpa;

import ariba.ui.meta.persistence.QueryProcessor;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.Predicate;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.PersistenceMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.ObjectMeta;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import org.compass.core.Compass;
import org.compass.core.CompassHits;
import org.compass.core.CompassHit;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.CompassQueryBuilder;
import org.compass.core.CompassQuery;
import org.compass.gps.device.hibernate.embedded.HibernateHelper;
import org.compass.annotations.Searchable;
import org.compass.annotations.SearchableProperty;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

class CompassQueryProcessor implements QueryProcessor
{
    private static boolean _DidInit;

    static {
        initialize();

    }

    public static void initialize ()
    {
        if (_DidInit) return;
        JPAContext.registerQueryProcessor(new CompassQueryProcessor());

        ObjectMeta meta = UIMeta.getInstance();

        meta.registerAnnotationListener(Searchable.class, new ObjectMeta.AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (!isAction) ObjectMeta.addTrait("Searchable", propertyMap);
            }
        });

        meta.registerAnnotationListener(SearchableProperty.class, new ObjectMeta.AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (!isAction) ObjectMeta.addTrait("SearchableProperty", propertyMap);                
            }
        });

        _DidInit = true;
    }


    public boolean isProcessorForQuery (QuerySpecification spec)
    {
        return spec.getPredicate() != null && spec.useTextIndex();
    }

    Compass getCompass (ObjectContext context)
    {
        // SessionFactory factory = ((SessionImpl)createEntityManager(_DefaultFactory).getDelegate()).getSessionFactory();
        return HibernateHelper.getCompass(((HibernateContext)context).getSession());
    }

    List processHits (ObjectContext context, CompassHits hits)
    {
        int count = Math.min(500, hits.length());
        List results = new ArrayList(count);
        for (int i=0; i < count; i++) {
            CompassHit hit = hits.hit(i);
            Object thinObj = hit.data();
            // ToDo:  PERFORMANCE!  This should be done in a batch fetch
            Object pk = context.getPrimaryKey(thinObj);
            Object fullObj = context.find(thinObj.getClass(), pk);
            results.add(fullObj);
        }
        return results;
    }

    protected CompassQuery queryForPredicate (CompassQueryBuilder builder, Predicate predicate)
    {
        if (predicate instanceof Predicate.KeyValue) {
            String key = ((Predicate.KeyValue)predicate).getKey();
            Object value = ((Predicate.KeyValue)predicate).getValue();
            return (key.equals(PersistenceMeta.KeywordsField))
                    ? builder.queryString((String)value).toQuery()
                    : builder.term(key, value);
        }
        else if (predicate instanceof Predicate.Junction) {
            CompassQueryBuilder.CompassBooleanQueryBuilder bool = builder.bool();
            boolean useMust = (predicate instanceof Predicate.And);
            for (Predicate p : ((Predicate.Junction)predicate).getPredicates()) {
                CompassQuery subquery = queryForPredicate(builder, p);
                if (useMust) {
                    bool.addMust(subquery);
                } else {
                    bool.addShould(subquery);
                }
            }
            return bool.toQuery();
        }

        Assert.that(false, "Unknown predicate type: %s", predicate.getClass());

        return null;
    }

    public List executeQuery (ObjectContext context, QuerySpecification spec)
    {
        CompassSession session = getCompass(context).openSession();
        CompassTransaction tr = session.beginTransaction();
        CompassQueryBuilder builder = session.queryBuilder();
        CompassQuery query = queryForPredicate(builder, spec.getPredicate());

        Class targetClass = ClassUtil.classForName(spec.getEntityName());

        //Check if we are not superceding this class
        Class currentClass = PersistenceMeta.supercedingChildClass(targetClass);
        query.setTypes(currentClass);

        // CompassHits hits = session.find(query);
        List results = processHits(context, query.hits());

        tr.commit();
        session.close();

        return results;
    }
}
