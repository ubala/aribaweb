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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaCommandBar.java#13 $
*/

package ariba.ui.widgets;

public final class AribaCommandBar extends BrandingComponent
{
    private static String _globalCommandBar;

    public static void setGlobalCommandBar (String globalCommandBar)
    {
        setGlobalCommandBar(globalCommandBar, true);
    }

    public static void setGlobalCommandBar (String globalCommandBar, boolean override)
    {
        if (override || _globalCommandBar == null) _globalCommandBar = globalCommandBar;
    }

    public String commands ()
    {
        boolean overrideGlobalCommandBar =
            PageWrapper.instance(this).booleanValueForBinding(
                BindingNames.overrideGlobalCommandBar);
        
        if (_globalCommandBar != null && !overrideGlobalCommandBar) return _globalCommandBar;

        // Note this component is expected to be referenced from within a component
        // which itself is referenced from within a PageWrapper.
        String commands = resolveTemplateOrComponentBasedInclude(BindingNames.commands);
        return commands != null ? commands : "DefaultCommandBar";
    }
}
