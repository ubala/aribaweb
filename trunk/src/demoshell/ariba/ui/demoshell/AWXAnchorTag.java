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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXAnchorTag.java#4 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRedirect;

public class AWXAnchorTag extends AWComponent
{
 /*
 protected static final String[] SupportedBindingNames = {"href"};
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }
*/
    public AWResponseGenerating linkClicked ()
    {
        Object val = valueForBinding("href");
        if (val == null) {
            val = valueForBinding("HREF");
        }

        if (val instanceof AWResponseGenerating) {
            return (AWResponseGenerating)val;
        }

        if (val instanceof String) {
            String name = (String)val;
            if (name.startsWith("http://") || name.startsWith("https://")) {
                AWRedirect redirect = (AWRedirect)pageWithName(AWRedirect.class.getName());
                redirect.setUrl(name);
                return redirect;
            }

            if (val != null) {
                // try to create a new component for this file
                return AWXHTMLComponentFactory.sharedInstance().componentToReturnForRelativePath(name, parent());
            }
        }
        return null;
    }
}
