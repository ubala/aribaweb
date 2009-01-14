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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWJSListRepetition.java#2 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWComponent;

public final class AWJSListRepetition extends AWComponent
{
    protected boolean _emittedItem;

    protected void awake() {
        super.awake();
        _emittedItem = false;
    }

    // emit a prefix comma for every item after the first (we use the call to this to track that an item was emitted)
    public String prefixComma ()
    {
        if (_emittedItem) return ",";
        _emittedItem = true;
        return null;
    }

    protected boolean allowComponentPathDebugging ()
    {
        return false;
    }
}
