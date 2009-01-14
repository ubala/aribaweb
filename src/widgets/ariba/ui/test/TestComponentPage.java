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

    $Id: //ariba/platform/ui/widgets/ariba/ui/test/TestComponentPage.java#1 $
*/

package ariba.ui.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.test.TestContext;
import ariba.ui.aribaweb.test.TestLinkManager;
import ariba.ui.aribaweb.test.TestInspectorLink;
import ariba.util.core.FastStringBuffer;
import ariba.util.test.TestValidationParameter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestComponentPage extends AWComponent
{
    void createBusinessObject ()
    {
        TestContext tc = TestContext.getTestContext(requestContext());
        tc.put(new String("MyBusinessObject"));
        tc.put(new Integer(20));
        tc.put(this);

        Set keys = tc.keys();
        for (Object key : keys) {
            boolean has = TestLinkManager.instance().hasObjectInspectors(key);
            if (has) {
                List<TestInspectorLink> inspectors =
                        TestLinkManager.instance().getObjectInspectors(key);
                for (TestInspectorLink i :inspectors) {
                    i.invoke(requestContext());
                }
            }
        }
    }

    AWComponent showBusinessObjectwWindow (String businessObject)
    {
        return null;
    }

    AWComponent showNewBusinessObjectWindow ()
    {
        TestContext tc = TestContext.getTestContext(requestContext());
        tc.put(new String("MyBusinessObject"));
        return null;
    }

    void deleteBusinessObject (String businessObject)
    {
    }

    void testInspector1 ()
    {
    }

    List<TestValidationParameter> testInspector2 ()
    {
        return null;
    }

    List<TestValidationParameter> testInspector3 (Integer value)
    {
        return null;
    }

    public static String logExceptions ()
    {
        Map <Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        Set<Thread> keys = map.keySet();
        FastStringBuffer sb = new FastStringBuffer();
        for (Thread t : keys) {
            StackTraceElement[] stacktaces = map.get(t);
            if (stacktaces != null) {
                sb.append("Current stack for long running thread: *************************" + t.toString());
                for (StackTraceElement line: stacktaces) {
                    sb.append("\n\t");
                    sb.append(line);
                }
            }
        }
        System.out.println(sb.toString());
        return sb.toString();
    }
}
