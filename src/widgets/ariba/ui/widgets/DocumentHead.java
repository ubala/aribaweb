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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/DocumentHead.java#8 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.html.AWImage;

import java.util.List;

public final class DocumentHead extends PageWrapper
{
    public Widgets.StyleSheetInfo _currentStyleSheet;

    protected void sleep()
    {
        super.sleep();
        _currentStyleSheet = null;
    }

    public String favIconUrl ()
    {
        return AWImage.imageUrl(requestContext(), this, "favicon.ico", false);
    }

    /**
     * @return A list of the static stylesheets for the current rendering
     *  version.
     */
    public List<Widgets.StyleSheetInfo> styleSheets ()
    {
        return isRenderAW5() ? Widgets.styleSheetsAW5() :
                Widgets.styleSheetsAW6();
    }

    public boolean hasCustomCSS ()
    {
        return resourceManager().resourceNamed("custom.css") != null;
    }

    public boolean hasApplicationCSS ()
    {
        return resourceManager().resourceNamed("application.css") != null;
    }
}
