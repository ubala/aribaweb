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

    $Id: //ariba/platform/ui/widgets/ariba/ui/outline/OutlineInnerRepetition.java#2 $
*/
package ariba.ui.outline;

import ariba.ui.aribaweb.core.AWComponent;

public final class OutlineInnerRepetition extends AWComponent
{
    public OutlineRepetition _outlineRepetition;
    private int _index = -1;

    public void awake ()
    {
        _outlineRepetition = OutlineRepetition.currentInstance(this);
        _index = -1;
    }

    public void sleep ()
    {
        _outlineRepetition = null;
        _index = 0;
    }

    public void setIndex (int index)
    {
        // the AWFor can
        // iteratively incremented and set the index (iterating)
        // set the index to a particular value value (glid skipping)
        while (_index < index) {
            _outlineRepetition.incrementOutlineIndex();
            _index++;
        }
    }
}
