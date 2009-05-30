package ariba.ui.meta.jpa;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.Session;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Iterator;

import ariba.ui.meta.persistence.ObjectContext;

public class HibernateInterceptor extends EmptyInterceptor
{
    public boolean onSave(Object o, Serializable serializable, Object[] objects, String[] strings, Type[] types)
    {
        Log.metajpa.debug("*** onSave() " + o);
        return false;
    }

    public void postFlush(Iterator iterator)
    {
        JPAContext ctx = (JPAContext)ObjectContext.get();
        Log.metajpa.debug("*** postFlush() ");
        while (iterator.hasNext()) {
            Object o = iterator.next();
            Log.metajpa.debug("           obj: " + o
                + ", key = " + ctx.getPrimaryKey(o));
            ctx.recordObjectUpdate(o);
        }
    }

    public void afterTransactionCompletion(Transaction transaction)
    {
        Log.metajpa.debug("*** afterTransactionCompletion() wasCommitted: " +
                transaction.wasCommitted());
        JPAContext ctx = (JPAContext)ObjectContext.get();
        ctx.recordTransactionDidComplete(transaction.wasCommitted());        
    }

    public Object getEntity(String entityName, Serializable id) throws CallbackException
    {
        // let the context potential provide an instance (e.g. a child context could merge and instance from its parent)
        JPAContext ctx = (JPAContext)ObjectContext.get();
        Object o =  ctx.overrideEntityInstance(entityName, id);
        // Log.metajpa.debug("*** getEntity(%s, %s) --> %s\n", entityName, id, o);
        return o;
    }

    /*
    public Boolean isTransient (Object o)
    {
        JPAContext ctx = (JPAContext)ObjectContext.get();
        return ctx.isTransient(o);
    }
    */
}
