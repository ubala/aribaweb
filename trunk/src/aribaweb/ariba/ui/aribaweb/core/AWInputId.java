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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWInputId.java#3 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.AWLabel;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.Fmt;
import ariba.util.core.Assert;

public final class AWInputId
{
    private AWEncodedString _id;
    private boolean _hasBeenConsumed = false;

    private AWInputId ()
    {
    }
    
    private AWInputId (Object id)
    {
        if (id instanceof String) {
            _id = new AWEncodedString((String)id);
        }
        else if (id instanceof AWEncodedString) {
            _id = (AWEncodedString)id;
        }
        else {
            throw new AWGenericException(Fmt.S("AWInputId must be initialized with a String or AWEncodedString.  Received: %s", id.getClass().getName()));
        }
    }

    // Note: we assume single threaded access to AWInputId
    public AWEncodedString getId ()
    {
        if (_hasBeenConsumed) {
            return null;
        }
        _hasBeenConsumed = true;
        return _id;
    }
    // support for $env.awinputId
    public String toString ()
    {
        if (_hasBeenConsumed) {
            return null;
        }
        _hasBeenConsumed = true;
        return _id.toString();
    }

    public static AWInputId getAWInputId (Object id)
    {
        if (id == null) {
            return null;
        }
        return new AWInputId(id);
    }

    public static AWEncodedString getAWInputId (AWRequestContext requestContext)
    {
        AWPage page = requestContext.page();
        if (page == null)
            return null;

        AWEnvironmentStack env = page.env();

        Object awInputId = env.peek(AWLabel.awinputId);
        if (awInputId == null) {
            return null;
        }

        Assert.that((awInputId instanceof AWInputId), "Expected ariba.ui.aribaweb.core.AWInputId for awinputId. Received: %s", awInputId.getClass().getName());

        return ((AWInputId)awInputId).getId();
    }
}
