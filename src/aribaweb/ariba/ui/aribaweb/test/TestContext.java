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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestContext.java#8 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.util.core.Base64;
import ariba.util.core.ClassUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.test.SuiteShare;

import java.util.Map;
import java.util.Set;

public class TestContext
{
    public static final String Name = "uiTestContext";
    public static final String ID = "testContextId";
    public static final String TestAutomationMode = "taMode";
    public static final String SuiteDataParam = "suiteData";
    public static final String ReturnUrlParam = "returnUrl";

    private Map<String, Object> _internalContext = MapUtil.map();
    
    private Map<Object, Object> _context = MapUtil.map();
    private String _username;
    private TestContextDataProvider _dataProvider;
    private String _id;
    private String[] _returnUrl;
    
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
        if (value != null) {
            _internalContext.put(key, value);
        }
    }

    public Object getInternalParam(String key)
    {
        return _internalContext.get(key);
    }

    public void setDataProvider (TestContextDataProvider dataProvider)
    {
        _dataProvider = dataProvider;
    }

    static public boolean isTestAutomationMode (AWRequestContext requestContext)
    {
        return AWConcreteServerApplication.IsDebuggingEnabled && 
                (getTestContext(requestContext) != null ||
                getSavedTestContext(requestContext) != null ||
                !StringUtil.nullOrEmptyOrBlankString(requestContext.formValueForKey(TestAutomationMode)));
    }
    
    static public TestContext getTestContext (AWRequestContext requestContext)
    {
        AWSession session = requestContext.session(false);
        return getTestContext(session);
    }

    static public TestContext getTestContext (AWSession session)
    {
        return session != null ? (TestContext)session.dict().get(TestContext.Name) : null;
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

    private static final String OpenParen = "(";
    private static final String CloseParen = ")";
    public static final String SemiColon = ";";

    public String getSuiteData ()
    {
        StringBuffer buf = new StringBuffer();
        Set keys = keys();
        for (Object key : keys) {
            Object obj = get((Class)key);
            if (obj instanceof SuiteShare) {
                SuiteShare share = (SuiteShare)obj;
                buf.append(obj.getClass().getName());
                buf.append(OpenParen);
                buf.append(share.getObjectIdentifier());
                buf.append(CloseParen);
                buf.append(SemiColon);
            }
        }
        return Base64.encode(buf.toString());
    }

    public void initializeSuiteData (AWRequestContext requestContext)
    {
        String returnUrl = requestContext.formValueForKey(ReturnUrlParam);
        if (!StringUtil.nullOrEmptyOrBlankString(returnUrl)) {
            returnUrl = Base64.decode(returnUrl);
            _returnUrl =  returnUrl.split(SemiColon);

        }
        String suiteData = requestContext.formValueForKey(SuiteDataParam);
        if (!StringUtil.nullOrEmptyOrBlankString(suiteData)) {
            suiteData = Base64.decode(suiteData);
            String[] objects = suiteData.split(SemiColon);
            for (String object : objects) {
                int index1 = object.indexOf(OpenParen);
                int index2 = object.indexOf(CloseParen);
                String identifier = object.substring(index1, index2);
                String className = object.substring(0, index1);
                SuiteShare  sharedObject = (SuiteShare)ClassUtil.newInstance(className);
                sharedObject.initialize(identifier);
                put(sharedObject);
            }
        }
    }

    public String getReturnUrlName ()
    {
       return _returnUrl != null ? _returnUrl[1] : null;
    }

    public String getReturnUrl ()
    {
        return _returnUrl != null ? _returnUrl[0] : null;
    }
}
