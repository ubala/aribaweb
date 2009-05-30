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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/Initialization.java#12 $
*/
package ariba.appcore;

import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.persistence.PersistenceMeta;
import ariba.ui.meta.persistence.Loader;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.PropertyValue;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWClasspathResourceDirectory;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.core.AWLocalLoginSessionHandler;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWSessionValidationException;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWLocal;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.AribaAction;
import ariba.ui.widgets.ConditionHandler;
import ariba.ui.widgets.StringHandler;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ClassUtil;
import ariba.util.core.Assert;
import ariba.util.core.SetUtil;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Collection;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import ariba.appcore.util.LoginPage;
import ariba.appcore.annotations.DatabaseInitLoader;
import ariba.appcore.annotations.DefaultAccess;
import ariba.appcore.annotations.AnonymousAccess;


public class Initialization
{
    static Set<String> _InitLoaderClasses = SetUtil.set();
    static Set<String> _AccessAnnotatedClasses = SetUtil.set();
    public static final Class[] NoClasses = new Class[] {};

    // Called via pre-initializer (early enough for annotation listeners to participate in jar scan)
    public static void initialize ()
    {
        registerAnnotationListeners();

        // Post application init initialization
        AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.sharedInstance();
        application.registerDidInitCallback(new AWConcreteApplication.DidInitCallback() {
            public void applicationDidInit (AWConcreteApplication application) {
                metaInitialize(UIMeta.getInstance());

                if (!PersistenceMeta.doNotConnect()) checkDataInitLoaders(_InitLoaderClasses);

                User.initializeSessionBinder();

                setupStringHandlers();

                // Scan  aribaweb.properties for 'allow-access-without-login' setting and initialize login handler
                boolean allowAccessWithoutLogin = true;
                for (String name : AWClasspathResourceDirectory.awJarUrlsByName().keySet()) {
                    Properties props = AWClasspathResourceDirectory.aribawebPropertiesForName(name);
                    Object val = props.get("allow-access-without-login");
                    if (val != null && !Boolean.parseBoolean((String)val)) {
                        allowAccessWithoutLogin = false;
                        break;
                    }
                }
                setupSessionValidator(allowAccessWithoutLogin);
            }
        });
    }

    private static void registerAnnotationListeners ()
    {
        AWJarWalker.registerAnnotationListener(DatabaseInitLoader.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _InitLoaderClasses.add(className);
                    }
                });

        AWJarWalker.registerAnnotationListener(DefaultAccess.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _AccessAnnotatedClasses.add(className);
                    }
                });

        AWJarWalker.registerAnnotationListener(AnonymousAccess.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        _AccessAnnotatedClasses.add(className);
                    }
                });
    }

    static final String _DataLoadGlobalKey = Initialization.class.getName();
    static void checkDataInitLoaders (Set<String>loaderClasses)
    {
        ObjectContext.bindNewContext();

        // Todo: check if schema has already been loaded
        Global global = Global.find(_DataLoadGlobalKey);
        if (global != null) {
            System.out.println("Dataloads already performed.  Skipping.");
            return;
        }

        // mark that we've done init
        Global.create(_DataLoadGlobalKey).setLongValue(1L);
        ObjectContext.get().save();

        Map<Annotation, AnnotatedElement> loaderAnnotations = AWJarWalker.annotationsForClasses(loaderClasses,
                new Class[] {DatabaseInitLoader.class});
        List<DatabaseInitLoader>loaders = _orderedLoaders(loaderAnnotations);
        for (DatabaseInitLoader loader : loaders) {
            try {
                ((Method)loaderAnnotations.get(loader)).invoke(null);
            } catch (IllegalAccessException e) {
                throw new AWGenericException(loader.toString(), e);
            } catch (InvocationTargetException e) {
                throw new AWGenericException(loader.toString(), e);
            }
        }
    }

    // Order loader annotations taking in
    static List<DatabaseInitLoader> _orderedLoaders (final Map<Annotation, AnnotatedElement>annotationToRef)
    {
        Map<Class, List<Annotation>> classToAnnotations = AWUtil.groupBy(annotationToRef.keySet(), new AWUtil.ValueMapper() {
            public Object valueForObject (Object object)
            {
                return ((Member)annotationToRef.get(object)).getDeclaringClass();
            }
        });

        List<DatabaseInitLoader>ordered = new ArrayList();
        for (Annotation a : annotationToRef.keySet()) {
            Class[] preds = ((DatabaseInitLoader)a).predecessors();
            for (Class cls : preds) {
                for (Annotation pred : classToAnnotations.get(cls)) {
                    ListUtil.addElementIfAbsent(ordered, (DatabaseInitLoader)pred);
                }
            }
            ListUtil.addElementIfAbsent(ordered, (DatabaseInitLoader)a);
        }
        return ordered;
    }

    @DatabaseInitLoader
    public static void loadDefaultUsersAndGroups ()
    {
        createPermissions();
        createDefaultUsers();
        processAccessAnnotations(_AccessAnnotatedClasses);

        AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.SharedInstance;
        Loader loader = new Loader();
        loader.prepareAllLoads(application.resourceManager(), "dataloads/");
        loader.runLoads();
    }

    public static final String RequiredPermissions = "requiredPermissionIds";

    static void metaInitialize(final UIMeta meta)
    {
        // force load of ariba.appcore/rules.oss (User / Permission rules)
        meta.touch(UIMeta.KeyClass, Permission.class.getName());
    }

    // Conditional Module visibility
    public static List moduleClassViewPermissions (Context context)
    {
        HashSet permIds = new HashSet();
        String [] types = {"homeForClasses", "showsClasses" };
        for (String type : types) {
            context.push();
            context.set(type, true);
            List<String> classNames = ((UIMeta)context.meta()).itemNames(context, UIMeta.KeyClass);
            if (!ListUtil.nullOrEmptyList(classNames)) {
                AWUtil.collect(classNames, permIds, new AWUtil.ValueMapper() {
                    public Object valueForObject (Object object)
                    {
                        return Permission.idForPermissionName(
                                Permission.nameForClassOp((String)object, Permission.ClassOperation.view));
                    }
                });
            }
            context.pop();
        }

        return new ArrayList(permIds);
    }

    public static Object userClassOperationPermissionCheck (Context context, String operation)
    {
        if (operation == null) return true;
        Permission.ClassOperation op = Permission.ClassOperation.valueOf(operation);
        Object classes = context.values().get(ObjectMeta.KeyClass);
        String cls = null;

        // Clumsy support for contexts with multiple classes: allowed if user has *any* permissions
        if (classes != null && classes instanceof List) {
            int size = ((List)classes).size();
            if (size == 0) return true;
            if (size == 1) {
                cls = (String)((List)classes).get(0);
            } else {
                // multi-class path
                final List<Integer>permissions = ListUtil.list();
                for (String cl : (List<String>)classes) {
                    permissions.add(Permission.idForPermissionName(Permission.nameForClassOp(cl, op)));
                }
                
                return new PropertyValue.Dynamic() {
                    public Object evaluate (Context context)
                    {
                        for (int p : permissions) {
                            if (User.currentUser().hasPermission(p)) return true;
                        }
                        return false;
                    }
                };
            }
        } else {
            cls = (String)classes;
        }
        
        if (cls == null) return true;
        // FIXME: map unknown ops to parent ops?  (e.g. keywordSearch to search)
        if (op == null) return true;
        final int id = Permission.idForPermissionName(Permission.nameForClassOp(cls, op));
        return new PropertyValue.Dynamic() {
            public Object evaluate (Context context)
            {
                return User.currentUser().hasPermission(id);
            }
        };
    }

    public static void setupSessionValidator (final boolean allowAccessWithoutLogin)
    {
        AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.sharedInstance();
        application.setSessionValidator(new AWLocalLoginSessionHandler() {
            protected AWResponseGenerating showLoginPage (AWRequestContext requestContext,
                                                          CompletionCallback callback)
            {
                LoginPage loginPage = (LoginPage)requestContext.pageWithName(LoginPage.class.getName());
                loginPage.init(callback);
                return loginPage;
            }

            protected boolean requireSessionValidationForAllComponentActions ()
            {
                return !allowAccessWithoutLogin;
            }

            protected boolean validateSession (AWRequestContext requestContext)
            {
                return User.isLoggedIn();
            }
        });

        ActionHandler.setHandler(AribaAction.LogoutAction, new ActionHandler() {
            public AWResponseGenerating actionClicked (AWRequestContext requestContext)
            {
                // Force user to anonymous and kill session
                User.bindUserToSession(User.getAnonymous(), requestContext.session());
                // MetaNavTabBar.invalidateState(requestContext.session());
                requestContext.session().terminate();
                return AWComponentActionRequestHandler.SharedInstance.processFrontDoorRequest(requestContext);
            }
        });

        ConditionHandler.setHandler("disableLogoutAction", new ConditionHandler() {
            public boolean evaluateCondition (AWRequestContext requestContext)
            {
                return !User.isLoggedIn();
            }
        });

        if (allowAccessWithoutLogin) {
            ConditionHandler.setHandler("showLoginAction", new ConditionHandler() {
                public boolean evaluateCondition (AWRequestContext requestContext)
                {
                    return !User.isLoggedIn();
                }
            });

            ActionHandler.setHandler("login", new ActionHandler() {
                public AWResponseGenerating actionClicked (AWRequestContext requestContext)
                {
                    // force a login
                    if (!User.isLoggedIn()) throw new AWSessionValidationException();
                    return null;
                }

                public boolean submitFormToComponentAction ()
                {
                    // we're going to change the structure of the page, so we don't want form vals for the replay
                    return false;
                }
            });
        }
    }

    public static class DefaultStringHandler extends StringHandler
    {
        public String getString (AWRequestContext requestContext)
        {
            String name = name();
            if ("applicationName".equals(name)) {
                return AWLocal.localizedJavaString(1, "AribaWeb Demonstration" /*  */, Initialization.class, requestContext);
            }
            if ("Login".equals(name)) {
                return AWLocal.localizedJavaString(2, "Sign in" /*  */, Initialization.class, requestContext);
            }
            if (Home.equals(name)) {
                return AWLocal.localizedJavaString(5, "Home" /*  */, Initialization.class, requestContext);
            }
            if (Logout.equals(name)) {
                return AWLocal.localizedJavaString(6, "Logout" /*  */, Initialization.class, requestContext);
            }
            if (Logout.equals(name)) {
                return AWLocal.localizedJavaString(7, "Logout" /*  */, Initialization.class, requestContext);
            }
            if (Preferences.equals(name)) {
                return AWLocal.localizedJavaString(8, "Preferences" /*  */, Initialization.class, requestContext);
            }
            return null;
        }
    }

    static void setupStringHandlers ()
    {
        StringHandler.setDefaultHandler(new DefaultStringHandler());

        StringHandler.setHandler(StringHandler.UserGreeting, new StringHandler() {
            public String getString (AWRequestContext requestContext)
            {
                return User.isLoggedIn() ? Fmt.S(AWLocal.localizedJavaString(3, "Welcome %s" /*  */, Initialization.class, requestContext), User.currentUser().getName())
                                    : AWLocal.localizedJavaString(4, "(Not logged in)" /*  */, Initialization.class, requestContext);
            }
        });
    }

    static void createPermissions ()
    {
        ObjectContext ctx = ObjectContext.get();

        // Permissions for every Entity
        Map<String, Set<Permission>> permissionsByPackage = MapUtil.map();
        for (String className : PersistenceMeta.getEntityClasses()) {
            // get list for package
            String pkg = AWUtil.stripLastComponent(className, '.');
            Set<Permission> permissionsForClass = permissionsByPackage.get(pkg);
            if (permissionsForClass == null) {
                permissionsForClass = SetUtil.set();
                permissionsByPackage.put(pkg,permissionsForClass);
            }

            for (Permission.ClassOperation op : Permission.ClassOperation.values()) {
                String permName = Permission.nameForClassOp(className, op);
                Permission permission = ctx.create(Permission.class);
                permission.setName(permName);
                permissionsForClass.add(permission);
            }
            ctx.save();
        }

        // Groups for every package
        List<Group>pkgGroups = ListUtil.list();
        for (Map.Entry<String, Set<Permission>> e : permissionsByPackage.entrySet()) {
            Group group = ctx.create(Group.class);
            group.setName(Fmt.S("%s:ALL", e.getKey()));
            group.setPermissions(e.getValue());
            pkgGroups.add(group);
        }

        // Superuser group
        createGroup(ctx, Group.DefaultGroup.AdminUsers.name(), pkgGroups);

        // Anonymous -> *no* permissions by default -- should be overridden by app
        Group anon = createGroup(ctx, Group.DefaultGroup.AnonymousUsers.name(), null);

        // DefaultUser -> *no* permissions by default -- should be overridden by app
        createGroup(ctx, Group.DefaultGroup.DefaultUsers.name(), AWUtil.list(anon));

        ctx.save();
    }

    static Group createGroup (ObjectContext ctx, String name, List<Group>memberOf)
    {
        Group group = ctx.create(Group.class);
        group.setName(name);
        group.setMemberOf(memberOf);
        return group;
    }

    public static Group getDefaultGroup (Group.DefaultGroup group)
    {
        return ObjectContext.get().findOne(Group.class, AWUtil.map("name", group.name()));
    }


    static void createDefaultUsers ()
    {
        ObjectContext ctx = ObjectContext.get();
        User admin = ctx.create(User.class);
        admin.setName(User.AdminName);
        admin.setPassword("ariba");
        Group adminGroup = getDefaultGroup(Group.DefaultGroup.AdminUsers);
        admin.setMemberOf(AWUtil.list(adminGroup));

        User anonymous = ctx.create(User.class);
        anonymous.setName(User.AnonymousName);
        Group anonGroup = getDefaultGroup(Group.DefaultGroup.AnonymousUsers);
        anonymous.setMemberOf(AWUtil.list(anonGroup));
        anonymous.setPassword("");

        ctx.save();
    }

    static void processAccessAnnotations(Collection<String> classes)
    {
        ObjectContext ctx = ObjectContext.get();
        Group anonGroup = getDefaultGroup(Group.DefaultGroup.AnonymousUsers);
        Group defaultGroup = getDefaultGroup(Group.DefaultGroup.DefaultUsers);
        for (String className : classes) {
            Class cls = ClassUtil.classForName(className);
            Assert.that(cls != null, "Unable to find class: %s", className);
            for (Annotation annotation:  cls.getAnnotations()) {
                if (annotation instanceof DefaultAccess) {
                    addPermissions(defaultGroup, cls, ((DefaultAccess)annotation).value());
                }
                else if (annotation instanceof AnonymousAccess) {
                    addPermissions(anonGroup, cls, ((AnonymousAccess)annotation).value());
                }
            }
        }
        ctx.save();
    }

    static void addPermissions (Group group, Class cls, Permission.ClassOperation[] operations)
    {
        if (operations == null || operations.length == 0) operations = Permission.AllClassOperations;
        for (Permission.ClassOperation op : operations) {
            Permission permission = Permission.permissionForClassOp(cls.getName(), op);
            group.addToPermissions(permission);
        }
    }
}
