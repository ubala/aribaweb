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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Widgets.java#69 $
*/

package ariba.ui.widgets;

import ariba.util.core.ClassUtil;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWTemplateParser;
import ariba.ui.aribaweb.core.AWXDebugResourceActions;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.table.AWTButtonArea;
import ariba.ui.table.AWTColumn;
import ariba.ui.table.AWTCSVData;
import ariba.ui.table.AWTRowDetail;
import ariba.ui.table.AWTHeadingArea;
import ariba.ui.table.AWTMetaColumn;
import ariba.ui.table.AWTMultiSelectColumn;
import ariba.ui.table.AWTSingleSelectColumn;
import ariba.ui.table.AWTMetaContent;
import ariba.ui.table.AWTDynamicColumns;
import ariba.ui.dev.AWDebugPane;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;

import java.util.Arrays;
import java.util.List;

/**
*/
public final class Widgets
{
    private static final String BrandingConfigResourcePath = "config/branding";
    private static final String BrandingAribaResourcePath = "ariba/branding";

    private static final String AWWebResourcePath = "ariba/ui/aribaweb";
    private static final String WidgResourcePath = "ariba/ui";

    private static WidgetsDelegate _delegate;

    private static boolean _initializeWithoutXMLNodeFieldValueClassExtension = false;
    private static boolean _DidInit = false;

    public static Object AWSessionManager = null;
    public static String AppDebug = null;
    public static String DashboardDebug = null;

    private static String TocBottomComponent = null;

    public static void initialize ()
    {
        if (_DidInit) return;
        _DidInit = true;

        setDelegate(new DefaultDelegate());
        registerComponentAliases();
        registerPackageNames(resourceManager());
        registerHandlers();
        AWApplication application = (AWApplication)AWConcreteApplication.sharedInstance();
        registerBrandingResources(application.resourceFilePath(), application.resourceURL());
        registerWidgetsResources("./", application.resourceFilePath(), application.resourceURL());
        initializeSubpackages();

        // force init of XML field value support
        if (!Widgets.initializeWithoutXMLNodeFieldValueClassExtension()) {
            XMLUtil.registerXMLNodeFieldValueClassExtension();
        }
    }

    protected static class DefaultDelegate implements WidgetsDelegate
    {
        public boolean hintMessagesVisible (AWRequestContext requestContext)
        {
            return true;
        }

        public String getUrlPrefixForBrand (String brand)
        {
            return null;
        }

        public String getDirectoryForBrand (String brand)
        {
            return "/";
        }

        public ActionHandler getHelpActionHandler (AWRequestContext requestContext,
                                                   String helpKey)
        {
            return null;
        }
    }

        // Note: ASN registers their own field value class extension for XML nodes. Because
        // this is incompatible with the one in FieldValue_XMLNode, this method was provided to disable
        // registering FieldValue_XMLNode.

    public static void setInitializeWithoutXMLNodeFieldValueClassExtension (boolean value)
    {
        _initializeWithoutXMLNodeFieldValueClassExtension = value;
    }

    public static boolean initializeWithoutXMLNodeFieldValueClassExtension ()
    {
        return _initializeWithoutXMLNodeFieldValueClassExtension;
    }

    public static void setAWSessionManagerConfig (Object manager)
    {
        AWSessionManager = manager;
        AWConcreteApplication.IsSessionManagementEnabled = AWSessionManager != null;
    }

    private static AWMultiLocaleResourceManager resourceManager ()
    {
        AWApplication application = AWConcreteApplication.defaultApplication();
        return application.resourceManager();
    }

    private static void registerComponentAliases ()
    {
        AWTemplateParser templateParser = AWComponent.defaultTemplateParser();
        templateParser.registerElementClassForTagName("Include", WidgetInclude.class);

        // register .awl namespaces for our components
        AWNamespaceManager ns = AWNamespaceManager.instance();
        AWNamespaceManager.AllowedGlobalsResolver globals
                = new AWNamespaceManager.AllowedGlobalsResolver(ns.resolverForPackage("ariba.ui.aribaweb"));
        globals.addAllowedGlobalPrefix("Ariba");
        globals.addAllowedGlobal("Include");
        AWNamespaceManager.Resolver resolver = new AWNamespaceManager.Resolver(globals);
        resolver.addIncludeToNamespace("a", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.dev"),
                Arrays.asList("AW")));
        resolver.addIncludeToNamespace("w", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.widgets", "ariba.ui.wizard",
                        "ariba.ui.outline", "ariba.ui.validation", "ariba.ui.richtext"),
                Arrays.asList("")));
        resolver.addIncludeToNamespace("t", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.table"),
                Arrays.asList("AWT")));
        resolver.addIncludeToNamespace("v", new AWNamespaceManager.Import(
                Arrays.asList("ariba.ui.validation"),
                Arrays.asList("AWV")));

        ns.registerResolverForPackage("ariba.ui.widgets", resolver);
        ns.registerResolverForPackage("ariba.ui.dev", resolver);
        ns.registerResolverForPackage("ariba.ui.table", resolver);
        ns.registerResolverForPackage("ariba.ui.outline", resolver);
        ns.registerResolverForPackage("ariba.ui.validation", resolver);
        ns.registerResolverForPackage("ariba.ui.wizard", resolver);
        ns.registerResolverForPackage("ariba.ui.richtext", resolver);
    }

    private static void registerPackageNames (AWMultiLocaleResourceManager resourceManager)
    {
        resourceManager.registerPackageName(
            ClassUtil.stripClassFromClassName(Widgets.class.getName()), true);
        resourceManager.registerPackageName(
            ClassUtil.stripClassFromClassName(AWDebugPane.class.getName()), true);

        resourceManager.registerClass(AWTButtonArea.class);
        resourceManager.registerClass(AWTColumn.class);
        resourceManager.registerClass(AWTCSVData.class);
        resourceManager.registerClass(AWTDynamicColumns.class);
        resourceManager.registerClass(AWTHeadingArea.class);
        resourceManager.registerClass(AWTMetaColumn.class);
        resourceManager.registerClass(AWTMetaContent.class);
        resourceManager.registerClass(AWTMultiSelectColumn.class);
        resourceManager.registerClass(AWTSingleSelectColumn.class);
        resourceManager.registerClass(AWTRowDetail.class);
    }

    private static void registerHandlers ()
    {
        ActionHandler.setHandler(ToggleSidebarActionHandler.ToggleSideBarActionName,
                                 new ToggleSidebarActionHandler());
        ConditionHandler.setHandler(BindingNames.isSidebarVisible,
                                    new IsSidebarVisibleConditionHandler());
        ConditionHandler.setHandler(BindingNames.hasSidebarNotch,
                                    new HasSidebarNotchConditionHandler());
        ConditionHandler.setHandler("hasSidebar",
                                    new HasSidebarConditionHandler());
        ConditionHandler.setHandler(BindingNames.disableHomeAction,
                                    new DisableHomeActionConditionHandler());
        ConditionHandler.setHandler("disableHelpAction",
                                    new DisableHelpActionConditionHandler());
        ConditionHandler.setHandler("disableLogoutAction",
                                    new DisableLogoutActionConditionHandler());
        ConditionHandler.setHandler("isAccessibilityEnabled",
                                    new IsAccessibilityEnabledConditionHandler());
        ConditionHandler.setHandler("disableAboutBox",
                                    new DisableAboutBoxConditionHandler());
        ConditionHandler.setHandler("needsWrapperActionDelimiter",
                                    new WrapperActionDelimiterConditionHandler());
        ConditionHandler.setHandler("needsDelimiter",
                                    new NeedsDelimiterConditionHandler());
        ConditionHandler.setHandler(BindingNames.isNavigationBarVisible,
                                    new IsNavigatorBarVisibleConditionHandler());
        ConditionHandler.setHandler("displayProductImageInBannerArea",
                                    new DisplayProductImageInBannerAreaConditionHandler());
        ConditionHandler.setHandler("hideBannerImage",
                                    new HideBannerImageConditionHandler());
    }

    private static void initializeSubpackages ()
    {
        ariba.ui.validation.Initialization.init();
        ariba.ui.outline.Initialization.init();
        ariba.ui.table.Initialization.init();
        ariba.ui.richtext.Initialization.init();
    }

    public static void setDelegate (WidgetsDelegate delegate)
    {
        _delegate = delegate;
    }

    public static WidgetsDelegate getDelegate ()
    {
        return _delegate;
    }

    /**
        Adds a resource directory to the search path that the jsptoolkit uses
        to resolve resources.  ResourcePath would be a directory which has
        under it a set of locale subdirectories (e.g. "en_US", "fr_FR").
        The urlPrefix is used if a url needs to be emitted into html for that
        resource, and would represent the base url corresponding to those
        resources in the docroot.

        @param resourcePath The base directory of a set of resources.
        @param urlPrefix    The base url for those resources (can be null).

        @see ariba.ui.aribaweb.util.AWResourceManager#pathForResourceNamed
        @see ariba.ui.aribaweb.util.AWResourceManager#urlForResourceNamed

        @aribaapi ariba
    */
    public static synchronized void registerResourceDirectory (String resourcePath, String urlPrefix)
    {
        AWMultiLocaleResourceManager resourceManager = resourceManager();
        resourceManager.registerResourceDirectory(resourcePath, urlPrefix);
    }

        // resourceFilePath is the pathname of the docroot
    public static void registerBrandingResources (String resourceFilePath, String resourceURL)
    {
        registerResourceDirectory(StringUtil.strcat(resourceFilePath, BrandingConfigResourcePath),
                                  StringUtil.strcat(resourceURL, BrandingConfigResourcePath));

        registerResourceDirectory(StringUtil.strcat(resourceFilePath, BrandingAribaResourcePath),
                                  StringUtil.strcat(resourceURL, BrandingAribaResourcePath));

    }

		// resourceFilePath is the pathname of the docroot
		// resourceURL is the URL for the docroot resources
	private static void registerWidgetsResources (String packageRootPath, String resourceFilePath, String resourceURL)
	{
        registerResourceDirectory(StringUtil.strcat(resourceFilePath, AWWebResourcePath),
                                  StringUtil.strcat(resourceURL, AWWebResourcePath));

        registerResourceDirectory(StringUtil.strcat(resourceFilePath, WidgResourcePath),
                                  StringUtil.strcat(resourceURL, WidgResourcePath));

        registerResourceDirectory(packageRootPath,
                                  null);

        registerResourceDirectory(StringUtil.strcat(packageRootPath, "ariba/resource"),
                                  StringUtil.strcat(resourceURL, "ariba/resource"));
	}

    // Used for AribaUI to register RecordPlayback control to appear
    // below TOC on login and other pages

    public static String getTocBottomComponent()
    {
        return TocBottomComponent;
    }

    public static void setTocBottomComponent(String tocBottomComponent)
    {
        TocBottomComponent = tocBottomComponent;
    }

    /** Style sheet registration **/
    private static List<StyleSheetInfo> StyleSheets = ListUtil.list();

    static {
        registerStyleSheet("widgets.css");
        registerStyleSheet("print.css", "print");
    }

    public static void registerStyleSheet (String filename)
    {
        registerStyleSheet(filename, null);
    }

    public static void registerStyleSheet (String filename, String media)
    {
        StyleSheetInfo styleSheetInfo = new StyleSheetInfo();
        styleSheetInfo._filename = filename;
        styleSheetInfo._media = media;
        StyleSheets.add(styleSheetInfo);
    }

    public static List<StyleSheetInfo> styleSheets ()
    {
        return StyleSheets;
    }

    public static class StyleSheetInfo
    {
        public String _filename;
        public String _media;
    }
}
