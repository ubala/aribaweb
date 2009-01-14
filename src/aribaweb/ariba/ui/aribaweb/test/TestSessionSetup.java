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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestSessionSetup.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWSession;

import java.util.Set;

public interface TestSessionSetup
{
    public boolean activateTestLinks (AWRequestContext requestContext);

    public void initializeSession (AWRequestContext requestContext, TestContext testContext);
    public void initializeTestContext (AWRequestContext requestContext);
    public void registerTestContextDataProvider (TestContext testContext, AWSession session);
    public Set<String> resolveSuperType (String classname);

    public String getObjectDisplayName (Object obj);
}