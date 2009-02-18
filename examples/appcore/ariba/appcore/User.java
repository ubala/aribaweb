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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/User.java#7 $
*/
package ariba.appcore;

import ariba.ui.meta.annotations.*;

import javax.persistence.Entity;
import java.util.List;
import ariba.util.core.*;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWSession;
import org.compass.annotations.*;

@Entity @NavModuleClass @Searchable
public class User extends Principal
{
    @SearchableProperty
    String userName;
    @Trait.Secret String password;
    String email;

    public String getUserName ()
    {
        return userName;
    }

    public void setUserName (String userName)
    {
        this.userName = userName;
    }

    public String getPassword ()
    {
        return password;
    }

    public void setPassword (String password)
    {
        // todo: needs to be replaced with code that applies one-way hash to stored passwords
        this.password = password;
    }

    public boolean matchingPassword (String candidate)
    {
        // Todo: password should be one-way hashed!
        return (candidate != null && candidate.equals(password));
    }

    public String getEmail ()
    {
        return email;
    }

    public void setEmail (String email)
    {
        this.email = email;
    }

    public static User find (String email)
    {
        ObjectContext context = ObjectContext.get();
        List<User> results = context.executeQuery(User.class, AWUtil.map("email", email));
        return ListUtil.nullOrEmptyList(results) ? null : results.get(0);
    }

    public static User create (String email, String fullName)
    {
        ObjectContext context = ObjectContext.get();
        User user = context.create(User.class);
        user.email = email;
        user.name = fullName;
        return user;
    }

    public static User findOrCreate (String email, String fullName)
    {
        User user = find(email);
        return  (user != null) ? user : create(email, fullName);
    }


    static ThreadLocal _ThreadLocalUserId = new ThreadLocal();
    static Long _AnonymousUID;
    public static final String AnonymousName = "anonymous";
    public static final String AdminName = "admin";
    
    static User getAnonymous ()
    {
        User anon;
        if (ObjectContext.peek() == null) return null;
        if (_AnonymousUID == null) {
            anon = ObjectContext.get().findOne(User.class, AWUtil.map("name", AnonymousName));
            if (anon != null) _AnonymousUID = anon.getId();
        } else {
            anon = ObjectContext.get().find(User.class, _AnonymousUID);
        }
        return anon;
    }


    static boolean _DidReg = false;
    static final String _SessionUIDKey = "_OC_USER_ID";
    static final String _SessionContextKey = "_OC_Context_ID";

    /**
        Gets current established user (or Anonymous)
     */
    public static User currentUser ()
    {
        Long id = (Long)_ThreadLocalUserId.get();
        return (id != null) ? ObjectContext.get().find(User.class, id) : getAnonymous();
    }

    public static boolean isLoggedIn ()
    {
        User user =  currentUser();
        return user != null && user != getAnonymous();
    }
    
    public static void initializeSessionBinder ()
    {
        if (!_DidReg) {
            _DidReg = true;
            AWSession.registerLifecycleListener(new UserBinder());
        }
    }
    /**
        Once bound, currentUser() will return this user for requests on this session
     */
    public static void bindUserToSession (User user, AWSession session)
    {
        session.dict().put(_SessionUIDKey, user.getId());
    }

    static class UserBinder implements AWSession.LifecycleListener
    {
        public void sessionWillAwake (AWSession session)
        {
            _ThreadLocalUserId.set(session.dict().get(_SessionUIDKey));
            if (ObjectContext.peek() == null) {
                ObjectContext ctx = (ObjectContext)session.dict().get(_SessionContextKey);
                if (ctx == null) {
                    ObjectContext.bindNewContext(session.sessionId());
                    ctx = ObjectContext.get();
                    session.dict().put(_SessionContextKey, ctx);
                } else {
                    ObjectContext.bind(ctx);
                }
            }
        }

        public void sessionWillSleep (AWSession session)
        {
            _ThreadLocalUserId.set(null);
            ObjectContext.unbind();
        }
    }
}
