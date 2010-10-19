/*
    Copyright 2009 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/Externalize.java#5 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWStaticSiteGenerator;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.meta.layouts.MetaNavTabBar;
import ariba.util.core.MapUtil;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

/**
    Used by bin/localize.groovy to externalize localizable labels to CSVs
 */
public class Externalize
{
    UIMeta _meta;

    static UIMeta initialize ()
    {
        UIMeta.initialize();
        // rough app startup
        final AWConcreteApplication application = (AWConcreteApplication)AWConcreteApplication.createApplication(
                AWStaticSiteGenerator.ExtendedDefaultApplication.class.getName(), AWStaticSiteGenerator.ExtendedDefaultApplication.class);

        return UIMeta.getInstance();
    }

    public Externalize (UIMeta meta)
    {
        _meta = meta;
    }

    /*
        Returns Map of strings for @Localized (or @NavModule, or @Entity) classes within the
        given list of packages.
        Returned Map is PackageName -> FileName -> Key -> List(string, string, comment)
     */

    Map<String, Map<String, List>> byPackage = MapUtil.map();
    List _filterPackages;

    Map mapForFileKey (String packageName, String fileKey)
    {
        Map byFile = byPackage.get(packageName);
        if (byFile == null) {
            byFile = MapUtil.map();
            byPackage.put(packageName, byFile);
        }
        Map byKey = (Map)byFile.get(fileKey);
        if (byKey == null) {
            byKey = MapUtil.map();
            byFile.put(fileKey, byKey);
        }
        return byKey;
    }

    public Map stringsForPackages (List packages)
    {
        _filterPackages = packages;

        Context ctx = _meta.newContext();

        // First, actions and fields on all localizedClasses
        Log.meta.debug("Localized MetaUI classes: " + _meta.localizedClasses());
        for (String className : _meta.localizedClasses()) {
            registerFieldsForClass(ctx, className);
        }

        // Next, All top-level modules
        UIMeta.ModuleInfo moduleInfo = _meta.computeModuleInfo (ctx, false);
        for (String moduleName : moduleInfo.moduleNames) {
            registerLabelsForModule(ctx, moduleName);
        }

        return byPackage;
    }

    void recordLabel (Context ctx)
    {
        Object visible = ctx.staticallyResolveValue(ctx.allProperties().get(UIMeta.KeyVisible));
        boolean isHidden = (visible != null) && ((visible instanceof Boolean) && !((Boolean)visible).booleanValue());
        Object rawLabel = ctx.staticallyResolveValue(ctx.allProperties().get(UIMeta.KeyLabel));
        // Log.meta_detail.debug("Raw label: " + rawLabel + " - " + rawLabel.getClass());
        if (!isHidden && rawLabel instanceof UIMeta.AutoLocalized) {
            // Must eval label before called packageName()!
            String label = (String)ctx.propertyForKey(UIMeta.KeyLabel);
            String packageName = ((UIMeta.AutoLocalized)rawLabel).packageName();
            if (!_filterPackages.contains(packageName)) return;
            String fileKey = ((UIMeta.AutoLocalized)rawLabel).fileKey();
            String key = ((UIMeta.AutoLocalized)rawLabel).key();
            mapForFileKey(packageName, fileKey).put(key, AWUtil.list(label, label, ""));
        }
    }

    void registerFieldsForClass (Context ctx, String className)
    {
        ctx.push();
        // Ick...  Pushing bogus "instance" so rules that check for "object" will match
        // Could cause class-cast exception if any dynamic properties are evaluated
        ctx.set(UIMeta.KeyObject, "Placeholder for " + className);
        ctx.set(UIMeta.KeyClass, className);
        recordLabel(ctx);
        
        List<String> fields = _meta.itemNames(ctx, UIMeta.KeyField);
        for (String field : fields) {
            ctx.push();
            ctx.set(UIMeta.KeyField, field);
            recordLabel(ctx);
            ctx.pop();
        }

        registerActions(ctx, false);

        ctx.pop();
    }

    void registerLabelsForModule(Context ctx, String moduleName)
    {
        ctx.push();
        ctx.set(UIMeta.KeyModule, moduleName);
        recordLabel(ctx);

        // layouts
        for (String layout : _meta.itemNames(ctx, UIMeta.KeyLayout)) {
            ctx.push();
            ctx.set(UIMeta.KeyLayout, layout);
            recordLabel(ctx);
            ctx.pop();
        }

        registerActions(ctx, true);
    }

    void registerActions (Context ctx, boolean global)
    {
        // action categories and actions
        String[] zones = (global) ? UIMeta.ModuleActionZones : UIMeta.ActionZones;
        Map<String, List<ItemProperties>> actionsByCategory = new HashMap();
        List<ItemProperties> actionCategories = _meta.actionsByCategory(ctx, actionsByCategory, zones);
        for (ItemProperties cat : actionCategories) {
            ctx.push();
            String catName = cat.name();
            ctx.set(UIMeta.KeyActionCategory, catName);
            if (global) recordLabel(ctx);
            List<ItemProperties> actions = actionsByCategory.get(catName);
            if (actions != null) {
                for (ItemProperties action : actions) {
                    ctx.push();
                    ctx.set(UIMeta.KeyAction, action.name());
                    recordLabel(ctx);
                    ctx.pop();
                }
            }
            ctx.pop();
        }
    }
}
