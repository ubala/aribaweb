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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaNavigationBar.java#4 $
*/

package ariba.ui.widgets;

public final class AribaNavigationBar extends BrandingComponent
{
    private static String _globalNavigationBar;

    public static void setGlobalNavigationBar (String globalNavigationBar)
    {
        setGlobalNavigationBar(globalNavigationBar, true);
    }

    public static void setGlobalNavigationBar (String globalNavigationBar, boolean override)
    {
        if (override || _globalNavigationBar == null) _globalNavigationBar = globalNavigationBar;
    }

    public String content ()
    {
        // Note this component is expected to be referenced from within a component
        // which itself is referenced from within a PageWrapper.
        String content = resolveTemplateOrComponentBasedInclude("globalNavArea");

        if (content == null && _globalNavigationBar != null) content = _globalNavigationBar;
                
        // using "DefaultCommandBar" as a blank
        return content != null ? content : "DefaultCommandBar";
    }
}
