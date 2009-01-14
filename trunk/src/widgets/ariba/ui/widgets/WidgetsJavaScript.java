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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/WidgetsJavaScript.java#8 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;

public final class WidgetsJavaScript extends AWComponent
{
    public boolean includeIndividualJSFiles ()
    {
        // if debugging and the individual files can be found (i.e. the
        // ARIBA_AW_SEARCH_PATH has been set to include <aribaweb_src_dir/>/resource/webserver)
        // the include individual files; otherwise use the compressed combo file
        return AWConcreteApplication.IsDebuggingEnabled
                && resourceManager().resourceNamed("widg/Widgets.js") != null;
    }
}
