/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/DemoShell.java#44 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.*;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.servletadaptor.AWServletApplication;
import ariba.ui.servletadaptor.AWServletRequest;
import ariba.ui.widgets.AribaCommandBar;
import ariba.ui.widgets.AribaNavigationBar;
import ariba.ui.meta.core.UIMeta;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.TableUtil;
import ariba.util.core.ClassUtil;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldValueException;
import org.apache.oro.text.perl.Perl5Util;
import org.apache.oro.text.regex.MatchResult;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Arrays;

public class DemoShell
{
    protected boolean _startPageIsMain = true;
    protected String _startPage;
    private static DemoShell _sharedInstance = null;
    protected String _uriPrefix = "";
    protected AWMultiLocaleResourceManager _brandResourceManager;

    public static DemoShell sharedInstance ()
    {
        if (_sharedInstance == null) {
            _sharedInstance = new DemoShell(null);
        }
        return _sharedInstance;
    }

    public static void init (String startPage)
    {
        /* if we have a shared instance just return */
        if (_sharedInstance != null) {
            return;
        }
        else {
            _sharedInstance = new DemoShell(startPage);
        }
    }

    protected static AWApplication application()
    {
        return (AWApplication)AWConcreteApplication.sharedInstance();
    }

    public DemoShell (String startPage)
    {
        _startPage = startPage;
        if (_startPage != null) {
            _startPageIsMain = false;
        }

        _sharedInstance = this; // questionable...  but nestedreport relies on this side-effect

        AWApplication application = application();

        ariba.ui.widgets.Widgets.initialize();

        // force wizard initialization
        ariba.ui.wizard.core.Wizard.class.getName();
        
        initDemoRoot();
        application.resourceManager().registerPackageName("ariba.ui.demoshell", true);
        String resourceUrl = (String)FieldValue.getFieldValue(application, "resourceUrl");
        application.resourceManager().registerResourceDirectory("./internal", resourceUrl+"/internal");
        application.resourceManager().registerResourceDirectory("./internal/ariba/ui/demoshell", null, false);

        // register namespaces for our components
        AWNamespaceManager ns = AWNamespaceManager.instance();
        AWNamespaceManager.Resolver resolver = ns.resolverForPackage("ariba.ui.widgets");
        resolver = new AWNamespaceManager.Resolver(resolver);
        resolver.addIncludeToNamespace("x", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.demoshell", "ariba.ui.scratch"),
                Arrays.asList("AWX")));
        ns.registerResolverForPackage("ariba.ui.demoshell", resolver);

        // only init if present...
        String cls = "ariba.ui.scratch.Initialization";
        if (ClassUtil.classForName(cls, false) != null) {
            ClassUtil.invokeStaticMethod(cls, "init",
                new Class[]{},
                new Object[]{});
            ns.registerResolverForPackage("ariba.ui.scratch", resolver);
        }

        ariba.ui.meta.core.Initialization.init();

        /*
        // FIXME - should be factored out to example app
        if (_startPageIsMain) {
            application.resourceManager().registerPackageName("example.ui.app", true);

            // Hack: force load of the global nav "module" definitions
            UIMeta.getInstance().touch(UIMeta.KeyModule, "example.ui.app.Main");
        }
        */
    }

    private class DefaultMultiLocaleResourceManager extends AWMultiLocaleResourceManager
    {
        public AWResourceManager getRealm (String realmName, String version, boolean shouldCreate)
        {
            throw new AWGenericException("DefaultMultiLocaleResourceManager getRealm not supported.");
        }

        public AWMultiLocaleResourceManager createRealmResourceManager(String realmName, String version)
        {
            throw new AWGenericException("DefaultMultiLocaleResourceManager createRealmResourceManager not supported.");
        }
    }

    private static Perl5Util perlUtil = new Perl5Util();
    Map _Args = null;

    protected Map getCommandlineArgs ()
    {
        if (_Args == null) {
            _Args = MapUtil.map();
            String cmdLine = System.getProperty("Ariba.CommandLineArgs", "");
            StringTokenizer st = new StringTokenizer(cmdLine, " ");
            while (st.hasMoreTokens()) {
                String arg = st.nextToken();
                if (perlUtil.match("/-D(\\w*)=(.*)/", arg)) {
                    MatchResult res = perlUtil.getMatch();
                    _Args.put(res.group(1), res.group(2));
                }
            }
        }
        return _Args;
    }

    protected String argForKey (String key)
    {
        Map args = getCommandlineArgs();
        String result = (String)args.get(key);
        if (result == null) {
            result = System.getProperty(key);
        }
        return result;
    }
/*
    protected String argForKey (String key)
    {
        return System.getProperty(key);
    }
*/
    void initDemoRoot ()
    {
        AWApplication application = application();
        AWXHTMLComponentFactory factory = AWXHTMLComponentFactory.sharedInstance();

        try {
           setUriPrefix((String)FieldValue.getFieldValue(application, "fullUrlPrefix"));
        }
        catch (FieldValueException ex) {
            Log.demoshell.debug("URI prefix not set!");
        }

        // Get info from application
        String docroot = (String)FieldValue.getFieldValue(application, "resourceUrl");

        // if we had a start page then just move on
        if (_startPage == null) {
            // register based on command line line param
            _startPage = argForKey("startPage");
        }

        if (_startPage == null) _startPage = System.getenv().get("ARIBA_DEMOSHELL_HOME");

        if (_startPage == null) {
            Map params = TableUtil.loadMap(new File("config/Parameters.table"), true);
            if (params != null) {
                _startPage = (String)params.get("startPage");
            }
        }

        if (_startPage == null) {
            System.out.println("***  Application started without startPage -- Launch app with -DstartPage=<path>");
            _startPage = "./docroot/internal/demoshell/Home.htm";
            System.out.println("     Using " + _startPage + " for site root ...");
        }

        if (_startPage != null) {
            File startPageFile = new File(_startPage);
            if (!startPageFile.exists()) {
                throw new AWGenericException("StartPage: '" + _startPage + "' does not exist.  Launch app with -DstartPage=<path>");
            }

            String siteRoot = (startPageFile.isDirectory()) ? startPageFile.getPath() : startPageFile.getParent();
            if (startPageFile.isDirectory()) {
                _startPage = null;
                startPageFile = null;
            }

            // See if they have an alternate URL for the siteRoot
            String siteRootUrl = argForKey("siteRootUrl");
            if (!StringUtil.nullOrEmptyString(siteRootUrl))
            {
                System.out.println("***  Registering demo docroot at alternate location: ");
                System.out.println("          dir: " + siteRoot);
                System.out.println("          url: " + siteRootUrl);

                factory.setDocRoot(siteRoot, siteRootUrl);
            } else {
                factory.setDocRoot("./docroot", docroot);
            }

            factory.setSiteRoot(siteRoot);

            // Make the start page into Main?
            if (_startPageIsMain && startPageFile != null) {
                factory.registerComponentAlias("Main", "/"+startPageFile.getName());
            }

            // register the demo root as a site root
            File siteRootDirectory = new File(siteRoot);
            String siteUrl = factory.siteRelativeUrlForFile(siteRootDirectory);
            if (new File(siteRootDirectory, "resources").exists()) {
                application.resourceManager().registerPackageName("resources");
            }
            Log.demoshell.debug("Registering site directory: %s -> %s", siteRoot, siteUrl);
            application.resourceManager().registerResourceDirectory(siteRoot, siteUrl, true);

            // if they have a brand directory, register it
            File brandDir = new File(siteRoot, "branding");
            if (brandDir.exists()) {
                String brandDirPath = brandDir.getAbsolutePath();
                // note: the {} below is necessary because AWMultiLocaleResourceManager is abstract...
                _brandResourceManager = new DefaultMultiLocaleResourceManager();
                _brandResourceManager.setNextResourceManager(application.resourceManager());
                String brandUrl = factory.siteRelativeUrlForFile(brandDir);
                Log.demoshell.debug("Registering brand directory: %s -> %s", brandDirPath, brandUrl);
                _brandResourceManager.registerResourceDirectory(brandDirPath, brandUrl, false);
            }
        } else {
            Log.demoshell.debug("** No startPage specified.  To launch with external startPage, launch with -DstartPage=<path> on the launch line");
        }
    }

    public AWResourceManager resourceManagerForSession (AWSession session)
    {
        if (_brandResourceManager != null) {
            return _brandResourceManager.resourceManagerForLocale(Locale.ENGLISH);
        }
        return null;
    }

    public void setUriPrefix (String prefix)
    {
        _uriPrefix = prefix;
    }

    public String startPage ()
    {
        return _startPage;
    }

    public String redirectForRequestURI (HttpServletRequest servletRequest)
    {
        String requestURI = servletRequest.getRequestURI();
        String url = null;
        AWServletApplication app = (AWServletApplication)AWConcreteApplication.sharedInstance();
        if (app.getClass() != Application.class) {
            // a request is coming to our servlet rather than the one associated with
            // the app.  We need to redirect to the app's main servlet
            AWServletRequest awRequest = (AWServletRequest)app.createRequest(servletRequest);
            AWRequestContext requestContext =  app.createRequestContext(awRequest);
            url = AWDirectActionUrl.fullUrlForDirectAction("showPage/DemoShellActions", requestContext, "page", "Main");
        }
        return url;
    }

    public AWResponseGenerating mainPage (AWRequestContext requestContext)
    {
        AWXHTMLComponentFactory factory = AWXHTMLComponentFactory.sharedInstance();

        // weird, but see handleSessionRestorationError() below...
        String page = DemoShell.sharedInstance().startPage();
        String uri = requestContext.request().uri();
        Log.demoshell.debug("Start page URI = %s", uri);

        String prefix = _uriPrefix + "/";
        if ((uri != null) && uri.startsWith(prefix) && (uri.length() > prefix.length())) {
            // chop off prefix
            File f = new File(factory.siteRoot(), uri.substring(prefix.length()));
            // if (!f.exists()) return Application.sharedInstance().createResponse();

            page =  (f.exists()) ? f.getAbsolutePath() : null;
        }

        if (page != null) {
            Log.demoshell.debug("Start page path = %s", page);
            return factory.createComponentForAbsolutePath(page, requestContext);
        }
        Log.demoshell.debug("No startPage specified.  Returning Main");
        return requestContext.pageWithName("MetaHomePage");
    }
}
