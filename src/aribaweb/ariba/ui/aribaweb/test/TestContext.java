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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestContext.java#2 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.MapUtil;

import java.util.Map;
import java.util.Set;

public class TestContext
{
    public static final String Name = "uiTestContext";
    public static final String ID = "testContextId";

    private Map<String, Object> _internalContext = MapUtil.map();
    
    private Map<Object, Object> _context = MapUtil.map();
    private String _username;
    private TestContextDataProvider _dataProvider;
    private String _id;

    private static Map<String, TestContext> _savedTestContext = MapUtil.map();

    public TestContext ()
    {
        _id = String.valueOf(System.currentTimeMillis()) ;
    }

    public String getId ()
    {
        return _id;
    }
    
    public static TestContext getSavedTestContext (AWRequestContext requestContext)
    {
        TestContext tc = null;
        String tcId = requestContext.formValueForKey(ID);
        if (tcId != null) {
            tc = _savedTestContext.get(tcId);
        }
        return tc;
    }

    public static void removeSavedTestContext (AWRequestContext requestContext)
    {
        String tcId = requestContext.formValueForKey(ID);
        _savedTestContext.remove(tcId);
    }

    public void saveTestContext ()
    {
        _savedTestContext.put(_id, this);
    }

    public void addInternalParam (String key, Object value)
    {
        _internalContext.put(key, value);
    }
    
    public Object getInternalParam(String key)
    {
        return _internalContext.get(key);
    }

    public void setDataProvider (TestContextDataProvider dataProvider)
    {
        _dataProvider = dataProvider;
    }
    static public TestContext getTestContext (AWRequestContext requestContext)
    {
        return (TestContext)requestContext.session().dict().get(TestContext.Name);
    }

    static public TestContext getTestContext (AWSession session)
    {
        return (TestContext)session.dict().get(TestContext.Name);
    }

    public Set keys ()
    {
        return _context.keySet();
    }
    
    public Object get (Class type)
    {
        Object obj = _context.get(type);
        if (_dataProvider != null) {
            obj = _dataProvider.resolve(this, obj);
        }
        return obj;
    }

    public void put (Object object)
    {
        _context.put(object.getClass(), object); 
    }
    
    public void put (Object key, Object value)
    {
        _context.put(key, value); 
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
