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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWLabel.java#7 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWInputId;
import ariba.ui.aribaweb.util.AWEncodedString;

public final class AWLabel extends AWComponent
{
    public static final String awinputId = "awinputId";
    public AWEncodedString _elementId;

    protected void sleep ()
    {
        _elementId = null;
    }

    public AWInputId getInputId ()
    {
        return AWInputId.getAWInputId(_elementId);
    }
}
