/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaBanner.java#1 $
*/

package ariba.ui.widgets;

public final class AribaBanner extends BrandingComponent
{
    private static String _globalBanner;

    public static void setGlobalBanner (String globalBanner)
    {
        setGlobalBanner(globalBanner, true);
    }

    public static void setGlobalBanner (String globalBanner, boolean override)
    {
        if (override || _globalBanner == null) _globalBanner = globalBanner;
    }

    public String banner ()
    {
        boolean overrideGlobalBanner =
            PageWrapper.instance(this).booleanValueForBinding(
                BindingNames.overrideGlobalBanner);
        
        if (_globalBanner != null && !overrideGlobalBanner) return _globalBanner;

        // Note this component is expected to be referenced from within a component
        // which itself is referenced from within a PageWrapper.
        return resolveTemplateOrComponentBasedInclude(BindingNames.banner);
    }
}
