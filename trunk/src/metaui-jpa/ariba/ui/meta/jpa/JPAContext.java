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

    $Id: //ariba/platform/ui/metaui-jpa/ariba/ui/meta/jpa/JPAContext.java#9 $
*/
package ariba.ui.meta.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Persistence;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.PersistenceMeta;
import ariba.ui.meta.persistence.QuerySpecification;
import ariba.ui.meta.persistence.QueryGenerator;
import ariba.ui.meta.persistence.QueryProcessor;
import ariba.util.core.ClassUtil;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.IdentityHashMap;
import java.io.Serializable;

import org.hibernate.CallbackException;

abstract public class JPAContext extends ObjectContext
{
    static List<QueryProcessor> _queryProcessors = ListUtil.list();
    EntityManager _entityManager;
    Map<Object, Object> _updatedObjects = new HashMap();
    Map<Object, Boolean> _pendingPersists = new IdentityHashMap<Object, Boolean>();
    boolean _deferPersists = true;

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
        if (_DefaultFactory == null) {
            // if none, use default persistence factory
            EntityManagerFactory emf =
                    Persistence.createEntityManagerFactory("Main");
            JPAContext.setDefaultFactory(emf);
        }
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

            QueryGenerator generator = new QueryGenerator(spec, context.typeProvider(spec.getEntityName()));
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
        tClass = PersistenceMeta.supercedingChildClass(tClass);
        return _entityManager.find(tClass, primaryKey);
    }

    // <T> T getReference(java.lang.Class<T> tClass, Object o);

    public void recordForInsert (Object o)
    {
        if (_deferPersists) {
            _pendingPersists.put(o, true);
        }
        else {
            persist(o);
        }
    }

    public void save()
    {
        _validateChanges();

        if (getParentContext() != null) {
            saveToParentContext();
        } else {
            _flushPendingPersists();
            EntityTransaction tx = _entityManager.getTransaction();
            tx.begin();
            _entityManager.flush();
            tx.commit();
            notifyGroupOfSave();
        }
    }

    /**
     * NOTE: this implementation is currently BROKEN!  The JPA EntityManager API does not include support
     * for nested EntityManagers, and this attempt to emulate from the outside (including using Hibernate
     * interceptor APIs) does not work completely (do to issues with applying merge() when saving back
     * unsaved objects from the child back to the parent). 
     * @return the new child context instance
     */
    public ObjectContext createNestedContext ()
    {
        ObjectContext child = super.createNestedContext();

        // Need to flush our persists and merge them up so they same copy is in the child
        Object[] toMerge = _pendingPersists.keySet().toArray();

        _flushPendingPersists();

        // We expect an interceptor hook to catch get() calls in the child, and merge such
        // entities on-demand into the child context

        return child;
    }

    protected void saveToParentContext ()
    {
        /*
        // FIXME!  How do we do a *merge* to an not-yet persisted object?

        for (Object obj :_pendingPersists.keySet()) {
            Object parentCopy = obj;
            parent.recordForInsert(parentCopy);
        }
        */
        // we don't want to do this until the final save, but right now we may not have a choice...
        _flushPendingPersists();

        JPAContext parent = (JPAContext)getParentContext();
        Map objectsById = getPotentiallyModifiedObjects();
        for (Object obj : objectsById.values()) {
            parent.merge(obj);
        }

    }

    public Boolean isTransient (Object o)
    {
        return _pendingPersists.containsKey(o);
    }


    /**
     * Get list of objects that have unsaved changes (or, if that's not possible, all objects in the context)
     * @return Map from id to object instance
     */
    abstract protected Map getPotentiallyModifiedObjects ();


    void _validateChanges ()
    {

    }

    void _flushPendingPersists ()
    {
        Iterator iter = _pendingPersists.keySet().iterator();
        // If we throw on a particular object, it and the remainder of unpersisted objects will remain
        while (iter.hasNext()) {
            Object o = iter.next();
            persist(o);
            iter.remove();
        }
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

    public EntityManager getJPAEntityManager ()
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
            Log.metajpa.debug(this + ": Object updated by peer: " + o);

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

    /**
     * Used to provide a child context with object instances that include modifications from the parent context.
     * @param entityName
     * @param id
     * @return a merged instance from parent context, or null if none
     * @throws CallbackException
     */

    Object _mergeId;

    protected Object overrideEntityInstance(String entityName, Serializable id)
    {
        Object override = null;
        JPAContext parent = (JPAContext)getParentContext();
        if (parent != null && !id.equals(_mergeId)) {
            Class entityClass = ClassUtil.classForName(entityName);
            Object parentObject = parent.getIfAlreadyLoaded(entityClass, id);
            if (parentObject != null) {
                Object prevMergeId = _mergeId;
                _mergeId = id;
                try {
                    override = merge(parentObject);
                } finally {
                    _mergeId = prevMergeId;
                }
            }
        }
        return override;
    }
}
