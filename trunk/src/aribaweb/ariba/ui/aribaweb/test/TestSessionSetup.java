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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestSessionSetup.java#9 $
    Responsible: ksaleh
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.test.TestValidationParameter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface TestSessionSetup
{
    public String getTestUserSetupComponentName ();

    public boolean activateTestLinks (AWRequestContext requestContext);

    public void initializeTestContext (AWRequestContext requestContext);
    public void registerTestContextDataProvider (TestContext testContext,
                                                 AWSession session);
    public Set<String> resolveSuperType (String classname);

    public String getObjectDisplayName (Object obj);

    // the session state will be saved before data stagers are called
    // use getSessionState, and then a call to restore the session state
    // is made after the stager is invoked.
    // The restore method can check if the stager modified the state
    // and restore if it needs.
    // This is being added so we can support reuse of junit tests as
    // data stagers.  Some of these unit tests replace the context
    // they run with and they don't restore.
    public Object getSessionState ();
    public void restoreSessionStateIfNeeded (Object obj);

    public List<TestInspectorLink> getApplicationValidators (Class c);
    public List<TestValidationParameter> invokeValidator (TestInspectorLink validator,
                                                     AWRequestContext requestContext);
    public String getTestCentralLinkList (AWRequestContext requestContext);
    public String getCurrentTestCentralPageInfo (AWRequestContext requestContext);
    public String getRemoteRealmName ();

    public void executeStager(Runnable stagerRunnable);
    
    public void saveTestContext (String testId, TestContext tc) throws IOException;
    public AWResponseGenerating restoreTestContext (String testId, AWRequestContext rq) throws IOException, ClassNotFoundException;
}
