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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWBacktrackState.java#6 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.StringUtil;

public final class AWBacktrackState extends AWBaseObject
{
    private AWBacktrackState _nextBacktrackState;
    private AWBacktrackState _previousBacktrackState;
    protected AWComponent component;
    public Object userState;

    public AWBacktrackState (AWComponent componentValue, Object userStateValue)
    {
        super();
        component = componentValue;
        userState = userStateValue;
    }

    public void setNextBacktrackState (AWBacktrackState backtrackState)
    {
        _nextBacktrackState = backtrackState;
        if (backtrackState != null) {
            backtrackState.setPreviousBacktrackState(this);            
        }
    }

    public AWBacktrackState nextBacktrackState ()
    {
        return _nextBacktrackState;
    }

    public void setPreviousBacktrackState (AWBacktrackState backtrackState)
    {
        _previousBacktrackState = backtrackState;
    }

    public AWBacktrackState previousBacktrackState ()
    {
        return _previousBacktrackState;
    }

    private String thisString ()
    {
        return StringUtil.strcat(component.toString(), ": ", userState.toString());
    }

    private String tailString ()
    {
        String tailString = thisString();
        if (_nextBacktrackState != null) {
            tailString = StringUtil.strcat(tailString, " -> ", _nextBacktrackState.tailString());
        }
        return tailString;
    }

    private String headString ()
    {
        String headString = thisString();
        if (_previousBacktrackState != null) {
            headString = StringUtil.strcat(_previousBacktrackState.headString(), " -> ", headString);
        }
        return headString;
    }

    public String toString ()
    {
        String stringValue = StringUtil.strcat("*", thisString(), "*");
        if (_previousBacktrackState != null) {
            String headString = _previousBacktrackState.headString();
            stringValue = StringUtil.strcat(headString, " -> ", stringValue);
        }
        if (_nextBacktrackState != null) {
            String tailString = _nextBacktrackState.tailString();
            stringValue = StringUtil.strcat(stringValue, " -> ", tailString);
        }
        return stringValue;
    }
}
