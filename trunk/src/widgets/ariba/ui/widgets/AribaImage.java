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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/AribaImage.java#13 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;

public final class AribaImage extends AWComponent
{
        // If the user supplied a key, then attempt to get the filename from the components
        // string manager. If none is found, use the default file name which is specified
        // by the parents filename binding.

    public String filename ()
    {
        String returnValue = null;
        String name = stringValueForBinding(BindingNames.name);
        if (name != null) {
            StringHandler handler = StringHandler.resolveHandlerInComponent(name, this.parent());
            String imageFileName = handler == null ? null : handler.getString(requestContext());
            if (imageFileName != null) {
                returnValue = imageFileName;
            }
        }
        if (returnValue == null) {
            returnValue = stringValueForBinding(BindingNames.filename);
        }
        return returnValue;
    }
}
