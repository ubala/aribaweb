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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/ObjectContext.java#13 $
*/
package ariba.ui.meta.persistence;

import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.meta.core.UIMeta;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.Iterator;

public abstract class ObjectContext
{
    ObjectContext _parentContext;
    ContextGroup _contextGroup;
    Map _userMap;
    private static Map<String, EntityQueryFilterProvider> _queryFiltersMap =
    	new HashMap<String, EntityQueryFilterProvider>();

    protected abstract void persist(Object o);

    public static ObjectContext get()
    {
        ObjectContext ctx = (ObjectContext)threadLocal.get();
        Assert.that(ctx != null, "get() on thread with no ObjectContext bound");
        return ctx;
    }

    public static ObjectContext peek()
    {
        ObjectContext ctx = (ObjectContext)threadLocal.get();
        return ctx;
    }

    public <T> T create (String className)
    {
        // Would be nice to use AWUtil classForName for reloading support,
        // but hibernate doesn't seem to like us swapping the class...
        return (T)create(ClassUtil.classForName(className));
    }

    public <T> T create (Class<T> tClass)
    {
        tClass = PersistenceMeta.supercedingChildClass(tClass);
        T o = (T)ClassUtil.newInstance(tClass);
        Assert.that(o != null, "Unable to create instance of class: %s", tClass.getName());
        recordForInsert(o);
        return o;
    }

    /**
     * Records object for insert on next save().  Some contexts may persist here, others
     * may defer the persist until just pre-save, to give the newly created object a change
     * to be fully initialized prior to persist()
     * @param o object instance to be inserted
     */
    public void recordForInsert (Object o)
    {
        persist(o);
    }

    /**
        Map for storage of additional state associated with this context
     */
    public Map userMap ()
    {
        if (_userMap == null) _userMap = new HashMap();
        return _userMap;
    }

    public abstract <T> T merge(T t);

    public abstract void remove(Object o);

    public abstract <T> T find(java.lang.Class<T> tClass, Object primaryKey);

    public <T> T findOne (java.lang.Class<T> tClass, Map<String, Object>fieldValues)
    {
        List<T> results = ObjectContext.get().executeQuery(tClass, fieldValues);
        return ListUtil.nullOrEmptyList(results) ? null : results.get(0);
    }

    public abstract Object getPrimaryKey (Object o);

    // abstract <T> T getReference(java.lang.Class<T> tClass, Object o);

    public abstract void save();

    // void revert();

    public abstract void lock(Object o);

    public abstract boolean contains(Object o);

    public List executeQuery (QuerySpecification spec)
    {
        Predicate originalPredicate = spec.getPredicate();
        try {
            EntityQueryFilterProvider queryFilter = _queryFiltersMap.get(spec
                .getEntityName());
            if (queryFilter != null) {
                Predicate p = queryFilter.replacementPredicateForQuery(spec);
                if (p != null) {
                    spec.setPredicate(p);
                }
            }
            return processorForQuery(spec).executeQuery(this, spec);
        }
        finally {
            // Reset the original predicate
            spec.setPredicate(originalPredicate);
        }
    }

    public abstract List executeNamedQuery (java.lang.String s, Map <String, Object> params);

    public abstract Object getDelegate();

    public <T> List<T> executeQuery (java.lang.Class<T> tClass, Map<String, Object>fieldValues)
    {
        return executeQuery(new QuerySpecification(tClass.getName(), Predicate.fromKeyValueMap(fieldValues)));
    }

    public abstract QueryProcessor processorForQuery (QuerySpecification spec);

    public TypeProvider typeProvider (String entityName)
    {
        return new TypeProvider(UIMeta.getInstance(), entityName);
    }

    /*
     * Visibility related
     */
    public static void registerQueryFilterProviderForEntityClass(String entityClass,
    		EntityQueryFilterProvider filter) {
    	_queryFiltersMap.put(entityClass, filter);
    }

    public static void unregisterQueryFilterProviderForEntityClass(String entityClass) {
    	_queryFiltersMap.remove(entityClass);
    }

    /*
        ThreadLocal context binding
     */
    static ThreadLocal threadLocal = new ThreadLocal();

    public static void bind (ObjectContext ctx)
    {
        threadLocal.set(ctx);
    }

    public static void unbind ()
    {
        threadLocal.set(null);
    }

    static void bindContext (ObjectContext ctx, String groupName)
    {
        ctx._contextGroup = associateContextGroup(ctx, groupName);
        bind(ctx);
    }

    public static void bindNewContext (String groupName)
    {
        bindContext(createContext(), groupName);
    }

    public static void bindNewContext ()
    {
        ObjectContext ctx = peek();
        String groupName = (ctx != null) ? ctx._contextGroup._name : null;
        bindNewContext(groupName);
    }

    public static void bindNestedContext ()
    {
        ObjectContext ctx = peek();
        assert ctx != null : "Can't bind nested context when no current context";
        bindContext(ctx.createNestedContext(), ctx._contextGroup._name);
    }

    public static ObjectContext createContext ()
    {
        return _Provider.create();
    }

    public ChangeWatch createChangeWatch ()
    {
        return new ChangeWatch(_contextGroup);
    }

    // called by subclasses upon save
    protected void notifyGroupOfSave ()
    {
        if (_contextGroup != null) _contextGroup._saveCount++;
    }

    public String groupName_debug ()
    {
        return _contextGroup != null ? _contextGroup._name : null;
    }

    /*
        External provider for new contexts
     */
    protected static Provider _Provider;

    public interface Provider
    {
        ObjectContext create();
    }

    public static Provider getProvider()
    {
        return _Provider;
    }

    public static void setProvider(Provider provider)
    {
        _Provider = provider;
    }

    /**
     * Create a nested ObjectContext.  Nested contexts have visibility to their parent's uncommited
     * changes, and commit to their parent's in-memory objects rather than to to database.
     * Nested Contexts are useful for modal UI flows, where an operation may be cancelled and
     * doing so should leave the parent's objects unaffected -- by "sandboxing" these changes in
     * copies of objects in a nested context, the Cancel is handled by simply discarding the child context,
     * and Okay by doing a save().
     *
     * NOTE: not all ObjectContext implementations can support Nested Contexts.  (e.g. such support is
     * not part of the JPA EntityManager specification)
     * @return the new child context instance
     */
    public ObjectContext createNestedContext ()
    {
        ObjectContext child = _Provider.create();
        child._parentContext = this;
        return child;
    }

    /**
     * Returns parent context for a nested child context
     * @return the parent context, or null if not nested.
     */
    public ObjectContext getParentContext ()
    {
        return _parentContext;
    }

    public static class ChangeWatch
    {
        ContextGroup _contextGroup;
        int _lastSaveCount;

        protected ChangeWatch (ContextGroup group)
        {
            _contextGroup = group;
            _lastSaveCount = group._saveCount;
        }

        public boolean hasChanged ()
        {
            int newCount = _contextGroup._saveCount;
            boolean changed = newCount != _lastSaveCount;
            _lastSaveCount = newCount;
            return changed;
        }
    }

    protected Iterator <ObjectContext> groupMembersIterator()
    {
        return _contextGroup.memberIterator();
    }


    static class ContextGroup
    {
        String _name;
        int _saveCount;
        WeakHashMap <ObjectContext, Object> _members = new WeakHashMap();

        ContextGroup (String name) { _name = name; }

        void registerMember (ObjectContext member)
        {
            _members.put(member, true);
        }

        Iterator <ObjectContext> memberIterator()
        {
            return _members.keySet().iterator();
        }
    }

    static Map <String, ContextGroup> _ContextGroups = new HashMap();
    static ContextGroup associateContextGroup (ObjectContext ctx, String name)
    {
        // take this opportunity to clean out any empty groups
        // todo: do this on a timer instead?
        gcEmptyContextGroups();

        if (name == null) return new ContextGroup(null);

        synchronized (_ContextGroups) {
            ContextGroup group = (ContextGroup) _ContextGroups.get(name);
            if (group == null) {
                group = new ContextGroup(name);
                _ContextGroups.put(name, group);
            }
            group.registerMember(ctx);
            return group;
        }
    }

    // clean out empty groups (they will empty because their (weak) ObjectContext keys
    // will get collected
    static void gcEmptyContextGroups ()
    {
        synchronized (_ContextGroups) {
            Iterator <Map.Entry <String, ContextGroup>> iter  = _ContextGroups.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry <String, ContextGroup> e = iter.next();
                if (e.getValue()._members.size() == 0) iter.remove();
            }
        }
    }

    /**
     * Implement this interface if you want to add additional filtering on
     * queries linked to an entity.
     * <p>
     * The implemented class should be registered on the ObjectContext using
     * {@link ObjectContext#registerQueryFilterProviderForEntityClass(String, EntityQueryFilterProvider)}
     */
    public interface EntityQueryFilterProvider {

        public Predicate replacementPredicateForQuery(QuerySpecification qs);

    }

}
