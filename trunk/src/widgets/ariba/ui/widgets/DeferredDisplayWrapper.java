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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/DeferredDisplayWrapper.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;

/**
    Component to display had deferred-evaluation header.
    This component allows a heading element to appear before the body, but be evaluated after.  Handy for things
    like header page errors that can't be calculated until the whole page is calculated, but appear at
    the top.
    @aribaapi ariba
*/
public class DeferredDisplayWrapper extends AWComponent
{
    // bindings
    public AWEncodedString _destinationElementId;
    public AWEncodedString _sourceElementId;

    protected void sleep ()
    {
        _destinationElementId = null;
        _sourceElementId = null;
        super.sleep();
    }

}
