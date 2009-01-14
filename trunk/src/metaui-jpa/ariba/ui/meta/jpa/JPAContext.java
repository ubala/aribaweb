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

    $Id: //ariba/platform/ui/metaui-jpa/ariba/ui/meta/jpa/JPAContext.java#1 $
*/
package ariba.ui.meta.jpa;

import org.hibernate.Session;
import org.hibernate.FlushMode;
import org.hibernate.EntityMode;
import org.hibernate.engine.EntityKey;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.impl.SessionImpl;
import org.hibernate.metadata.ClassMetadata;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.QueryGenerator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;

public class JPAContext extends ObjectContext
{
    EntityManager _entityManager;
    Map<Object, Object> _updatedObjects = new HashMap();

    // register us as the context provider
    private static boolean _DidInit = false;

    public static void initialize ()
    {
        if (_DidInit) return;
        _DidInit = true;
        JPAContext.setProvider(new Provider());
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

    /*
    public List executeQuery(java.lang.String s, Map <String, Object> params)
    {
        return executeQuery(_entityManager.createQuery(s), params);
    }
    */

    public List executeQuery(QuerySpecification spec)
    {
        QueryGenerator generator = new QueryGenerator(spec);
        String queryString = generator.generate();
        Map queryParams = generator.queryParams();

        return executeQuery(_entityManager.createQuery(queryString), queryParams);
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


    protected static class Provider implements ObjectContext.Provider
    {
        public ObjectContext create()
        {
            return new JPAContext(createEntityManager(_DefaultFactory));
        }
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


    /*
           Hibernate-specific
     */

    protected Session getSession ()
    {
        return (Session)_entityManager.getDelegate();
    }

    // Hibernate-specific
    public Object getPrimaryKey (Object o)
    {
        Session session = getSession();
        ClassMetadata classMeta = session.getSessionFactory().getClassMetadata(o.getClass());
        return classMeta.getIdentifier(o, EntityMode.POJO);
    }

    /*
        session change detection:
            http://www.hibernate.org/hib_docs/v3/api/org/hibernate/Interceptor.html

            http://www.hibernate.org/hib_docs/v3/api/org/hibernate/SessionFactory.html#openSession(org.hibernate.Interceptor)

            <property name="hibernate.ejb.interceptor" value="ariba.ui.meta.hibernate.Interceptor"/>
     */

    protected <T> T getIfAlreadyLoaded(java.lang.Class<T> tClass, Object primaryKey)
    {
        SessionImpl session = (SessionImpl)getSession();
        ClassMetadata classMeta = session.getSessionFactory().getClassMetadata(tClass);
        String entityName = classMeta.getEntityName();
        EntityPersister persister = session.getFactory().getEntityPersister(entityName);
        EntityKey entityKey = new EntityKey((Serializable)primaryKey, persister, EntityMode.POJO);

        return (T)((SessionImpl)getSession()).getPersistenceContext().getEntity(entityKey);
        //  return find(tClass, primaryKey);
        // return (T)getSession().get(tClass, (Serializable)primaryKey);
    }    

    protected static EntityManager createEntityManager (EntityManagerFactory factory)
    {
        EntityManager em = factory.createEntityManager();
        Session session = (Session)em.getDelegate();
        session.setFlushMode(FlushMode.MANUAL);
        return em;
    }
}
