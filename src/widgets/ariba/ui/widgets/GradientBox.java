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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/GradientBox.java#3 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.StringUtil;

public class GradientBox extends AWComponent
{
    private static final String[] SupportedBindingNames =
        { BindingNames.classBinding };


    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public String cssClass ()
    {
        String cssClass = (String)valueForBinding(BindingNames.classBinding);
        return cssClass != null ?
            StringUtil.strcat("gradBox ", cssClass) : "gradBox";
    }
}
