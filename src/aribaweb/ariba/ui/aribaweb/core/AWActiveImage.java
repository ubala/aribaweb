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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWActiveImage.java#3 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.AWBaseImage;
import ariba.ui.aribaweb.util.AWUtil;

public final class AWActiveImage extends AWBaseImage
{
    private static final String[] AdditionalBindingNames = {
        AWBindingNames.action, AWBindingNames.submitForm
    };
    private static final String[] SupportedBindingNames =
            (String[])AWUtil.concatenateArrays(AWBaseImage.SupportedBindingNames, AdditionalBindingNames);

    // ** Thread Safety Considerations: see AWComponent.

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }
}
