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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaMarkdown.java#4 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWString;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.widgets.Markdown;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.util.core.Assert;

import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MetaMarkdown extends AWComponent
{
    public String _actionUrl;
    AWResponseGenerating _actionResults;
    String _actionTarget;

    protected void sleep ()
    {
        _actionUrl = null;
        _actionResults = null;
        _actionTarget = null;
    }

    public String markdownValue ()
    {
        String value = null;
        String resourcePath = stringValueForBinding("resourcePath");
        if (resourcePath != null) {
            AWResource resource = resourceManager().resourceNamed(resourcePath);
            Assert.that(resource != null, "Unable to find resourcePath: %s", resourcePath);
            value = AWUtil.stringWithContentsOfInputStream(resource.inputStream());
        }

        if (value == null) value = stringValueForBinding(AWBindingNames.value);

        if (value == null) return value;

        String markdown = Markdown.translateMarkdown(value);
        return markdown;
    }

    static final Pattern ActionPattern = Pattern.compile("^/action/(\\w+)(?:\\?(.+))?");
    static final Pattern GotoPattern = Pattern.compile("^/goto/([\\w\\-\\./_]+)(?:\\?(.+))?");
    static final Pattern ImplicitGotoPattern = Pattern.compile("^/?([\\w\\-\\./_]+)(?:\\?(.+))?");

    public AWResponseGenerating action ()
    {
        prepare();
        return _actionResults;
    }

    public String actionTarget ()
    {
        prepare();
        return _actionTarget;
    }

    void prepare ()
    {
        String action = null;
        String page = null;
        Matcher m = ActionPattern.matcher(_actionUrl);
        if (m.matches()) {
            action = m.group(1);
        } else {
            m = GotoPattern.matcher(_actionUrl);
            if ((m = GotoPattern.matcher(_actionUrl)).matches()
                    || (m = ImplicitGotoPattern.matcher(_actionUrl)).matches()) {
                action="goto";
                page = m.group(1);

                String basePath = stringValueForBinding("resourcePath");
                if (basePath != null) page = resourceRelative(basePath, page, requestContext());
            }
        }

        if (action != null) {
            Context ctx = MetaContext.currentContext(this);
            ctx.push();
            String queryString = m.group(2);
            if (queryString != null) {
                String deescaped = queryString.replace("&amp;","&");
                Map<String, String[]> queryParams = AWUtil.parseQueryString(deescaped);
                for (Map.Entry<String,String[]> e : queryParams.entrySet()) {
                    ctx.set(e.getKey(), e.getValue()[0]);
                }
            }
            ctx.set(UIMeta.KeyAction, action);
            if (page != null) ctx.set("page", page);
            _actionResults = ((UIMeta)ctx.meta()).fireAction(ctx, requestContext());
            _actionTarget = (String)ctx.propertyForKey("linkTarget");
            ctx.pop();
        } else {
            // static URL -- initialize action target
            Context ctx = MetaContext.currentContext(this);
            ctx.push();
            ctx.set("page", _actionUrl);
            _actionTarget = (String)ctx.propertyForKey("linkTarget");
            ctx.pop();

            _actionResults = AWRedirect.getRedirect(requestContext(), _actionUrl);
        }
    }

    /*
        Silly general purpose utilities that might better be placed in UIMeta...
     */
    public static String resourceRelative (String baseResourcePath, String relativePath, AWRequestContext requestContext)
    {
        AWResourceManager resourceManager = requestContext.getCurrentComponent().resourceManager();
        AWResource resource = null;
        if (baseResourcePath != null) {
            AWResource baseResource = resourceManager.resourceNamed(baseResourcePath);
            if (baseResource != null) {
                resource = baseResource.relativeResource(relativePath, resourceManager);
            }
        }

        if (resource == null) resource = resourceManager.resourceNamed(relativePath);

        return resource != null ? resource.relativePath() : null;
    }

    public static String moduleForResource (String resourcePath, UIMeta.UIContext context)
    {
        List<UIMeta.ModuleProperties> modules = MetaNavTabBar.getState(context.requestContext().session()).moduleInfo().modules;
        UIMeta.ModuleProperties module = null;
        if (resourcePath != null) {
            if (resourcePath.startsWith("/")) resourcePath = resourcePath.substring(1);
            int sepIndex = resourcePath.indexOf('/');
            String moduleName = (sepIndex != -1) ? resourcePath.substring(0, sepIndex) : resourcePath;
            for (UIMeta.ModuleProperties m : modules) {
                if (m.name().equals(moduleName)) {
                    module = m;
                    break;
                }
            }
        }
        if (module == null) module = modules.get(0);
        return module.name();
    }
}
