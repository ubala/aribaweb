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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestContext.java#10 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWRecordingManager;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.Assert;
import ariba.util.core.Base64;
import ariba.util.core.MapUtil;
import ariba.util.core.StringUtil;
import ariba.util.i18n.I18NUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
    private static Map<String, TestContextObjectFactory> _factoriesById = MapUtil.map();
    private static Map<Class, String> _factoryIdsByClass = MapUtil.map();

    static
    {
        // register the playback monitor with AWRecordingManager
        // here we assume if we have TestContext it means we're in the playback mode
        AWRecordingManager.registerPlaybackMonitor(new AWRecordingManager.PlaybackMonitor() {
            public boolean isInPlaybackMode(AWRequestContext requestContext)
            {
                return (getTestContext(requestContext) != null);
            }
        } );
    }


    public TestContext ()
    {
        _id = String.valueOf(System.currentTimeMillis());
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

    public Object getInternalParam (String key)
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

    public static void registerTestContextObjectFactory (
        TestContextObjectFactory factory,
        Class c,
        String factoryId
        )
    {
        Assert.that(factory != null, "factory cannot be null");
        Assert.that(c != null, "class cannot be null");
        Assert.that(!StringUtil.nullOrEmptyOrBlankString(factoryId),
                    "factoryId cannot be null or blank or empty");

        Assert.that(_factoriesById.get(factoryId) == null, 
                    "Cannot reregister factory ID %s", factoryId);
        _factoriesById.put(factoryId, factory);

        Assert.that(_factoryIdsByClass.get(c) == null,
                    "Cannot reregister class %s", c.getName());
        _factoryIdsByClass.put(c, factoryId);
    }

    TestContextObjectFactory factoryForId (String id)
    {
        return _factoriesById.get(id);
    }

    String factoryIdForClass (Class c)
    {
        // TODO: should return factory of superclass?
        return _factoryIdsByClass.get(c);
    }

    private static final String Pipe = "|";
    public static final String SemiColon = ";";

    public String getSuiteData ()
    {
        StringBuffer buf = new StringBuffer();
        for (Object key : keys()) {
            Class c = (Class)key;
            String factoryId = factoryIdForClass(c);
            if (factoryId != null) {
                TestContextObjectFactory f = factoryForId(factoryId);
                Object obj = get(c);
                String objId = f.getSharedID(obj);
                if (buf.length() > 0) {
                    buf.append(SemiColon);
                }
                try {
                    buf.append(URLEncoder.encode(factoryId, I18NUtil.EncodingUTF_8));
                    buf.append(Pipe);
                    buf.append(URLEncoder.encode(objId, I18NUtil.EncodingUTF_8));
                }
                catch (UnsupportedEncodingException e) {
                    Assert.that(false, "UTF-8 must be supported");
                }
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
            String[] pairs = suiteData.split(SemiColon);
            for (String pair : pairs) {
                String[] kv = pair.split("\\|");
                Assert.that(kv.length == 2,
                            "Suite data key-value pair had %s " +
                            "members instead of 2", kv.length);
                try {
                    String factoryId = URLDecoder.decode(
                        kv[0],
                        I18NUtil.EncodingUTF_8
                        );
                    String objId = URLDecoder.decode(
                        kv[1],
                        I18NUtil.EncodingUTF_8
                        );
                    TestContextObjectFactory f = factoryForId(factoryId);
                    Assert.that(f != null,
                                "No factory found for ID %s", factoryId);
                    Object obj = f.reconstituteObject(requestContext, objId);
                    if (obj != null) {
                        put(obj);
                    }
                }
                catch (UnsupportedEncodingException e) {
                    Assert.that(false, "UTF-8 must be supported");
                }
            }
        }
    }

    public static String getEncodedReturnUrl (String displayName, String url)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(displayName);
        buf.append(SemiColon);
        buf.append(url);
        return Base64.encode(buf.toString());

    }

    public String getEncodedReturnUrl ()
    {
        return getEncodedReturnUrl(
                getReturnUrlName(),
                getReturnUrl()
                );
    }

    public String getReturnUrlName ()
    {
        return _returnUrl != null ? _returnUrl[0] : null;
    }

    public String getReturnUrl ()
    {
        return _returnUrl != null ? _returnUrl[1] : null;
    }
}
