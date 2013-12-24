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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWBody.java#9 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWBody extends AWComponent
{
    private static final String[] SupportedBindingNames = {
        BindingNames.filename, BindingNames.onLoad, BindingNames.background
    };
    // Wait indicator for desktop browsers
    private static final String ProgressBar = "anxProgressBar.gif";
    // Wait indicator for iPad
    private static final String Spinner = "anxWaitSpinner.gif";

    protected static final String JavaScriptLessThanSymbol = "<";

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public String backgroundUrl ()
    {
        String imageUrl = null;
        String filename = (String)valueForBinding(BindingNames.filename);
        if (filename != null) {
            imageUrl = urlForResourceNamed(filename);
            if (imageUrl == null) {
                imageUrl = AWUtil.formatErrorUrl(filename);
            }
        }
        return imageUrl;
    }

    public String waitImg ()
    {
        if (request() !=null && request().isIPad()) {
            return Spinner;
        }
        else {
            return ProgressBar;
        }
    }
}
