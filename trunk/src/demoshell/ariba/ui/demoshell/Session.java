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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/Session.java#7 $
*/

package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWSessionValidationException;
import ariba.util.fieldvalue.Extensible;
import ariba.util.core.MapUtil;

import java.util.Map;
import ariba.ui.aribaweb.util.AWResourceManager;

import java.util.Map;

public class Session extends AWSession
        implements Extensible  // so we have a convenient bag of properties
{
    public String name;
    protected Map _extras;
    private boolean _isAuthenticated = false;

    // implements Extensible -- now pages can do arbitrary bindings,
    // like "$session.myField" and that field will automatically be stored
    // in the hashtable
    // XXX: craigf -- doesn't AWSession already have a hashtable?!?
    public Map extendedFields ()
    {
        if (_extras == null) {
            _extras = MapUtil.map();
        }
        return _extras;
    }

    public void init ()
    {
        AWResourceManager resourceManager = DemoShell.sharedInstance().resourceManagerForSession(this);
        if (resourceManager != null) {
            setResourceManager(resourceManager);
        }
    }

    // for testing sso -- serves the same purpose as the existence of a user object
    // in a "real" application.
    public void setAuthenticated (boolean isAuthenticated)
    {
        _isAuthenticated = isAuthenticated;
    }

    public boolean isAuthenticated ()
    {
        return _isAuthenticated;
    }

    public void assertAuthenticated ()
    {
        if (!isAuthenticated()) throw new AWSessionValidationException();
    }

}
