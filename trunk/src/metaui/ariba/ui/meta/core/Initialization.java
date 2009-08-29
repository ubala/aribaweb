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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Initialization.java#13 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWNamespaceManager;
import ariba.ui.validation.AWVFormatterFactory;
import ariba.ui.widgets.AribaNavigationBar;
import ariba.ui.widgets.AribaCommandBar;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.AribaAction;
import ariba.ui.widgets.StringHandler;
import ariba.ui.widgets.PageWrapper;
import ariba.ui.widgets.HeaderIncludes;
import ariba.ui.meta.layouts.MetaHomePage;
import ariba.ui.meta.layouts.MetaNavTabBar;
import ariba.util.fieldvalue.FieldValue;

import java.util.Arrays;

public class Initialization
{
    private static boolean _DidInit = false;

    public static void preInit ()
    {
        UIMeta.getInstance();
    }

    public static void init ()
    {
        if (!_DidInit) {
            _DidInit = true;

            // register our resources with the AW
            AWConcreteApplication application = (AWConcreteApplication)AWConcreteServerApplication.sharedInstance();

            String resourceUrl = (String)FieldValue.getFieldValue(application, "resourceUrl");
            AWMultiLocaleResourceManager resourceManager = application.resourceManager();

            // lookups should check for files with extension ".oss"
            resourceManager.registerPackagedResourceExtension(".oss");

            // resourceManager.registerResourceDirectory("./ariba/ui/meta", resourceUrl+"ariba/ui/meta/");
            resourceManager.registerPackageName("ariba.ui.meta.core", true);
            resourceManager.registerPackageName("ariba.ui.meta.layouts", true);
            resourceManager.registerPackageName("ariba.ui.meta.editor", true);

            // Namespace Imports -----------------------------------------------
            AWNamespaceManager ns = AWNamespaceManager.instance();
            AWNamespaceManager.Resolver resolver;

            // extend ariba.ui.demoshell imports to include m:
            resolver = ns.resolverForPackage("ariba.ui.widgets");
            resolver.addIncludeToNamespace("m", new AWNamespaceManager.Import(
                    Arrays.asList("ariba.ui.meta"), // FIXME: remove when AWRecordPlayback placeholder eliminated
                    Arrays.asList("Meta")));

            // For ariba.ui.meta, use widgets imports + m: space
            resolver = new AWNamespaceManager.Resolver(ns.resolverForPackage("ariba.ui.widgets"));
            resolver.addIncludeToNamespace("m", new AWNamespaceManager.Import(
                    Arrays.asList("ariba.ui.meta"), // FIXME: remove when AWRecordPlayback placeholder eliminated
                    Arrays.asList("Meta")));
            ns.registerResolverForPackage("ariba.ui.meta", resolver);

            // register our formatter support
            AWVFormatterFactory.init();  // force registration of class extension

            AribaNavigationBar.setGlobalNavigationBar(ariba.ui.meta.layouts.MetaNavTabBar.class.getName(), false);
            AribaCommandBar.setGlobalCommandBar(ariba.ui.meta.layouts.MetaNavCommandBar.class.getName(), false);

            // ariba.ui.meta.core.Log.meta.setLevel(ariba.util.log.Log.DebugLevel);
            // ariba.ui.meta.core.Log.meta_detail.setLevel(ariba.util.log.Log.DebugLevel);
            // ariba.ui.meta.core.Log.meta_context.setLevel(ariba.util.log.Log.DebugLevel);

            application.registerDidInitCallback(new AWConcreteApplication.DidInitCallback() {
                public void applicationDidInit (AWConcreteApplication application) {
                    // Default the home page
                    if (application.resourceManager().packageResourceNamed(application.mainPageName()+".awl") == null) {
                        application.setMainPageName(MetaHomePage.class.getName());
                    }

                    if (ActionHandler.resolveHandler(AribaAction.HomeAction) == null) {
                        ActionHandler.setHandler(AribaAction.HomeAction, new ActionHandler() {
                            public AWResponseGenerating actionClicked(AWRequestContext requestContext) {
                                MetaNavTabBar.getState(requestContext.session()).gotoHomeModule(requestContext);
                                return requestContext.application().mainPage(requestContext);
                            }
                        });
                    }

                    if (StringHandler.resolveHandler(PageWrapper.ApplicationStringName, StringHandler.class) == null) {
                        StringHandler.setHandler(PageWrapper.ApplicationStringName, new StringHandler() {
                            public String getString (AWRequestContext requestContext)
                            {
                                String result = null;
                                Context ctx = MetaContext.peekContext(requestContext.getCurrentComponent());
                                if (ctx != null && ctx.values().get(UIMeta.KeyModule) != null) {
                                    ctx.push();
                                    ctx.setScopeKey(UIMeta.KeyModule);
                                    result = (String)ctx.propertyForKey("pageTitle");
                                    ctx.pop();
                                }
                                return result;
                            }
                        });
                    }

                    if (application.componentDefinitionForName("DocumentHeadContent") != null) {
                        HeaderIncludes.registerInclude("DocumentHeadContent");
                    }                    

                    UIMeta.getInstance().loadRuleFile("WidgetsRules.oss", true, Meta.SystemRulePriority);
                    if (!UIMeta.getInstance().loadRuleFile("Application.oss", false, Meta.LowRulePriority)) {
                        UIMeta.getInstance().loadRuleFromResourceNamed("Application.oss");
                    }
                }
            });
        }
    }
}