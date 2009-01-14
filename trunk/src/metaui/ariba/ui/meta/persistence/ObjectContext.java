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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/ObjectContext.java#2 $
*/
package ariba.ui.meta.persistence;

import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.ui.aribaweb.util.AWUtil;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.HashMap;
import java.util.Iterator;

public abstract class ObjectContext
{
    ContextGroup _contextGroup;

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

    public Object create (String className)
    {
        // Would be nice to use AWUtil classForName for reloading support,
        // but hibernate doesn't seem to like us swapping the class...
        return create(ClassUtil.classForName(className));
    }

    public Object create (Class tClass)
    {
        Object o = ClassUtil.newInstance(tClass);
        persist(o);
        return o;
    }
    
    public abstract <T> T merge(T t);

    public abstract void remove(Object o);

    public abstract <T> T find(java.lang.Class<T> tClass, Object primaryKey);

    public abstract Object getPrimaryKey (Object o);

    // abstract <T> T getReference(java.lang.Class<T> tClass, Object o);

    public abstract void save();

    // void revert();

    public abstract void lock(Object o);

    public abstract boolean contains(Object o);

    public abstract List executeQuery(QuerySpecification spec);

    public abstract List executeNamedQuery(java.lang.String s, Map <String, Object> params);

    public abstract Object getDelegate();

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

    public static void bindNewContext (String groupName)
    {
        ObjectContext ctx = createContext();
        ctx._contextGroup = associateContextGroup(ctx, groupName);
        bind(ctx);
    }

    public static void bindNewContext ()
    {
        ObjectContext ctx = peek();
        String groupName = (ctx != null) ? ctx._contextGroup._name : null;
        bindNewContext(groupName);
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
}
