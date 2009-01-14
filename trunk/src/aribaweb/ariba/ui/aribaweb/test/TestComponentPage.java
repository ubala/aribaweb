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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/test/TestComponentPage.java#1 $
*/

package ariba.ui.aribaweb.test;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.FastStringBuffer;
import java.util.Map;
import java.util.Set;

public class TestComponentPage extends AWComponent
{
    @TestLink
    void createBusinessObject ()
    {
        TestContext tc = TestContext.getTestContext(requestContext());
        tc.put(new String("MyBusinessObject"));
    }

    @TestLink
    AWComponent showBusinessObjectwWindow (String businessObject)
    {
        return null;
    }

    @TestLink
    AWComponent showNewBusinessObjectWindow ()
    {
        TestContext tc = TestContext.getTestContext(requestContext());
        tc.put(new String("MyBusinessObject"));
        return null;
    }

    @TestLink
    void deleteBusinessObject (String businessObject)
    {
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
