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

    $Id: //ariba/platform/ui/metaui-jpa/ariba/ui/meta/jpa/JPAContext.java#2 $
*/
package ariba.ui.meta.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.QueryGenerator;
import ariba.ui.meta.persistence.QueryProcessor;
import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

abstract public class JPAContext extends ObjectContext
{
    static List<QueryProcessor> _queryProcessors = ListUtil.list();
    EntityManager _entityManager;
    Map<Object, Object> _updatedObjects = new HashMap();

    // register us as the context provider
    private static boolean _DidInit = false;

    public static void initialize ()
    {
        if (_DidInit) return;
        _DidInit = true;

        // if hibernate is present then init the Hibernate Context
        if (ClassUtil.classForName("org.hibernate.Session") != null) {
            ClassUtil.classTouch("ariba.ui.meta.jpa.HibernateContext");

            // And Compass...
            if (ClassUtil.classForName("org.compass.core.Compass") != null) {
                ClassUtil.classTouch("ariba.ui.meta.jpa.CompassQueryProcessor");
            }
        }

        registerQueryProcessor(new JPAQueryProcessor());
    }

    static EntityManagerFactory _DefaultFactory;

    public static EntityManagerFactory getDefaultFactory()
    {
        return _DefaultFactory;
    }

    public static void setDefaultFactory(EntityManagerFactory defaultFactory)
    {
        _DefaultFactory = defaultFactory;
    }

    static class JPAQueryProcessor implements QueryProcessor
    {
        public boolean isProcessorForQuery (QuerySpecification spec)
        {
            return spec.getPredicate() == null || !spec.useTextIndex();
        }

        public List executeQuery (ObjectContext context, QuerySpecification spec)
        {
            QueryGenerator generator = new QueryGenerator(spec);
            String queryString = generator.generate();
            Map queryParams = generator.queryParams();

            JPAContext jpa = (JPAContext)context;
            return jpa.executeQuery(jpa._entityManager.createQuery(queryString), queryParams);
        }
    }

    public static void registerQueryProcessor (QueryProcessor processor)
    {
        _queryProcessors.add(processor);
    }

    public QueryProcessor processorForQuery (QuerySpecification spec)
    {
        for (QueryProcessor p : _queryProcessors) {
            if (p.isProcessorForQuery(spec)) return p;
        }
        return null;
    }

    protected JPAContext (EntityManager entityManager)
    {
        _entityManager = entityManager;
    }

    protected void persist(Object o)
    {
        _entityManager.persist(o);
    }

    public <T> T merge(T t)
    {
        return _entityManager.merge(t);
    }

    public void remove(Object o)
    {
        _entityManager.remove(o);
    }

    public <T> T find(java.lang.Class<T> tClass, Object primaryKey)
    {
        return _entityManager.find(tClass, primaryKey);
    }

    // <T> T getReference(java.lang.Class<T> tClass, Object o);

    public void save()
    {
        EntityTransaction tx = _entityManager.getTransaction();
        tx.begin();
        _entityManager.flush();
        tx.commit();
        notifyGroupOfSave();
    }

    // void revert();

    public void lock(Object o)
    {
        _entityManager.lock(o, LockModeType.WRITE);
    }

    public boolean contains(Object o)
    {
        return _entityManager.contains(o);
    }

    List executeQuery (Query q, Map <String, Object> params)
    {
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                q.setParameter(e.getKey(), e.getValue());
            }
        }
        return q.getResultList();
    }

    public List executeNamedQuery(java.lang.String s, Map <String, Object> params)
    {
        return executeQuery(_entityManager.createNamedQuery(s), params);
    }

    public Object getDelegate()
    {
        return _entityManager;
    }

    /*
        Called by external interceptors
     */
    protected void recordObjectUpdate (Object o)
    {
        _updatedObjects.put(o, getPrimaryKey(o));
    }

    protected <T> T getIfAlreadyLoaded(java.lang.Class<T> tClass, Object primaryKey)
    {
        // Todo: how do we do this in pure JPA?  (This will *always* FETCH)
        return find(tClass, primaryKey);
    }

    protected void objectUpdatedInPeerContext (Object key, Object o)
    {
        if (getIfAlreadyLoaded(o.getClass(), key) != null) {
            System.out.println(this + ": Object updated by peer: " + o);

            // refresh?
            merge(o);
        }
    }

    protected void recordTransactionDidComplete (boolean wasCommitted)
    {
        if (wasCommitted && !_updatedObjects.isEmpty()) {
            Iterator<ObjectContext> peerIter = groupMembersIterator();
            while (peerIter.hasNext()) {
                ObjectContext peer = peerIter.next();
                if (peer != null && peer != this) {
                    for (Map.Entry<Object, Object> e : _updatedObjects.entrySet()) {
                        ((JPAContext)peer).objectUpdatedInPeerContext(e.getValue(), e.getKey());
                    }
                }
            }
        }
        _updatedObjects.clear();
    }
}
