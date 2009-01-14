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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/ComplexRepetition.java#2 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.fieldvalue.OrderedList;
import ariba.ui.aribaweb.core.AWBinding;

public final class ComplexRepetition extends AWComponent
{
    public int _maxCount;
    public int _count;
    public int _index;
    public Object _list;   // OrderedList
    protected AWBinding _indexBinding;

    public boolean isStateless ()
    {
        return true;
    }

    protected void awake ()
    {
        super.awake();
        _list = valueForBinding("list");
        _count = OrderedList.get(_list).size(_list);
        _indexBinding = bindingForName("index");

        AWBinding binding = bindingForName("count");
        _maxCount = (binding == null) ? _count : intValueForBinding(binding);
        if (_maxCount > _count) _maxCount = _count;
    }

    protected void sleep ()
    {
        _list = null;
        _indexBinding = null;
    }

    public void setIndex (int index)
    {
        _index = index;
        // push it through
        if (_indexBinding != null) {
            setValueForBinding(index, _indexBinding);
        }
    }

    public boolean isLast ()
    {
        return (_index == (_maxCount-1));
    }

    public boolean didTruncateList ()
    {
        return _maxCount < _count;
    }

    public boolean isEmpty ()
    {
        return _count == 0;
    }
}
