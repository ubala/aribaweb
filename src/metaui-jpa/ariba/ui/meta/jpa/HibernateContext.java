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

    $Id: //ariba/platform/ui/metaui-jpa/ariba/ui/meta/jpa/HibernateContext.java#3 $
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

import ariba.ui.meta.persistence.ObjectContext;
import java.io.Serializable;
import java.util.Map;

public class HibernateContext extends JPAContext
{
    static {
        JPAContext.setProvider(new Provider());
    }
    
    protected HibernateContext (EntityManager entityManager)
    {
        super(entityManager);
    }

    protected static class Provider implements ObjectContext.Provider
    {
        public ObjectContext create()
        {
            return new HibernateContext(createEntityManager(getDefaultFactory()));
        }
    }

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

    protected Map getPotentiallyModifiedObjects ()
    {
        return ((SessionImpl)getSession()).getPersistenceContext().getEntitiesByKey();
    }
    
    protected static EntityManager createEntityManager (EntityManagerFactory factory)
    {
        EntityManager em = factory.createEntityManager();
        Session session = (Session)em.getDelegate();
        session.setFlushMode(FlushMode.MANUAL);
        return em;
    }
}
