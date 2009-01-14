package ariba.ui.meta.jpa;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.Session;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Iterator;

import ariba.ui.meta.persistence.ObjectContext;

public class HibernateInterceptor extends EmptyInterceptor
{
    public boolean onSave(Object o, Serializable serializable, Object[] objects, String[] strings, Type[] types)
    {
        System.out.println("*** onSave() " + o);
        return false;
    }

    public void postFlush(Iterator iterator)
    {
        JPAContext ctx = (JPAContext)ObjectContext.get();
        System.out.println("*** postFlush() ");
        while (iterator.hasNext()) {
            Object o = iterator.next();
            System.out.println("           obj: " + o
                + ", key = " + ctx.getPrimaryKey(o));
            ctx.recordObjectUpdate(o);
        }
    }

    public void afterTransactionCompletion(Transaction transaction)
    {
        System.out.println("*** afterTransactionCompletion() wasCommitted: " +
                transaction.wasCommitted());
        JPAContext ctx = (JPAContext)ObjectContext.get();
        ctx.recordTransactionDidComplete(transaction.wasCommitted());        
    }
}
