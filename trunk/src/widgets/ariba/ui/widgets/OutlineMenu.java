/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/OutlineMenu.java#2 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWOrderedList;
import ariba.util.core.Fmt;

public class OutlineMenu extends AWComponent
{
    public Object _list;
    public int _index;
    private static final int toppleLimit = 8;

    @Override
    protected void awake ()
    {
        _list = valueForBinding(BindingNames.list);
    }

    @Override
    protected void sleep ()
    {
        _list = null;
    }

    public String currLinkStyle ()
    {
        return Fmt.S("z-index:%s;", 30 - _index);
    }

    public boolean shouldDropUp ()
    {
        boolean menuDropUp = booleanValueForBinding("menuDropUp");        
        return (_index > toppleLimit) || menuDropUp;
    }
    
    public String childrenClass ()
    {
        return "olmChildren " + (shouldDropUp()? "olmDropUp" : "");
    }

}
