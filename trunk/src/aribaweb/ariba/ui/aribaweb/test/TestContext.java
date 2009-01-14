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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestContext.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.MapUtil;

import java.util.Map;

public class TestContext {
    public static final String Name = "uiTestContext";
    
    private Map<Class, Object> _context = MapUtil.map();
    private String _username;

    static public TestContext getTestContext (AWRequestContext requestContext)
    {
        return (TestContext)requestContext.session().dict().get(TestContext.Name);
    }

    static public TestContext getTestContext (AWSession session)
    {
        return (TestContext)session.dict().get(TestContext.Name);
    }

    public Object get (Class type)
    {
        return _context.get(type);
    }

    public void put (Object object)
    {
        _context.put(object.getClass(), object); 
    }

    public void setUsername (String username)
    {
        _username = username;
    }

    public String getUsername ()
    {
        return _username;
    }

    public void clear ()
    {
        _context = MapUtil.map();
    }
}
