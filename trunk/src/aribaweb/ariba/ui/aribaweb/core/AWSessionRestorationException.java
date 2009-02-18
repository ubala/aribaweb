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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSessionRestorationException.java#6 $
*/

package ariba.ui.aribaweb.core;

public final class AWSessionRestorationException extends RuntimeException
{

    public enum State {Normal, IPChanged};
    public static final State IPChanged = State.IPChanged;
    public static final State Normal = State.Normal;

    private State _state;
    private String _oldIP;
    private String _newIP;

    public AWSessionRestorationException ()
    {
        super();
        _state = State.Normal;
    }

    public AWSessionRestorationException (String exceptionMessage)
    {
        this(exceptionMessage, State.Normal);
    }

    public AWSessionRestorationException (String exceptionMessage, State state)
    {
        super(exceptionMessage);
        _state = state;
    }

    public State getState ()
    {
        return _state;
    }

    public void setState (State state)
    {
        _state = state;
    }

    public String getOldIP ()
    {
        return _oldIP;
    }

    public void setOldIP (String oldIP)
    {
        _oldIP = oldIP;
    }

    public String getNewIP ()
    {
        return _newIP;
    }

    public void setNewIP (String newIP)
    {
        _newIP = newIP;
    }
}
