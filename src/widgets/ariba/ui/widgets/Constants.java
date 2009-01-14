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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/Constants.java#17 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWBindingNames;

public final class Constants extends AWConstants
{
    protected static final AWEncodedString OnMouseDown = AWEncodedString.sharedEncodedString("onMouseDown");
    protected static final AWEncodedString OnKeyDown = AWEncodedString.sharedEncodedString("onKeyDown");
    protected static final AWEncodedString Style = AWEncodedString.sharedEncodedString("style");
    protected static final AWEncodedString Check = AWEncodedString.sharedEncodedString("check");
    protected static final AWEncodedString Bullet = AWEncodedString.sharedEncodedString("bullet");
    protected static final AWEncodedString MenuClass = AWEncodedString.sharedEncodedString("mC");
    protected static final AWEncodedString DisabledMenuClass = AWEncodedString.sharedEncodedString("mCD");
    protected static final AWEncodedString ClassAttributeName = AWEncodedString.sharedEncodedString(AWBindingNames.classBinding);
}
