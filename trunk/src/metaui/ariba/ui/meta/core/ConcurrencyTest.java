/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/core/ConcurrencyTest.java#2 $
*/
package ariba.ui.meta.core;

import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.util.core.Date;
import ariba.util.core.ListUtil;
import ariba.util.core.Assert;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.math.BigDecimal;

public class ConcurrencyTest {
    static ObjectMeta _meta;

    static Map<String, String> classFields = AWUtil.map(
            "firstName", String.class.getName(),
            "lastName", String.class.getName(),
            "birthDay", Date.class.getName(),
            "rating", Integer.class.getName(),
            "budget", BigDecimal.class.getName());

    static String ruleTextTemplate = "class=&CLASS& {" +
                "field=firstName&SFX& { " +
                    "valid:${value != null && value.length() > 0}; label:FN;" +
                    "operation=edit { label:FN2; }" +
                "}" +
            "}";

    // register a "class" in response to a notification
    static class PseudoIntrospectionProvider implements Meta.ValueQueriedObserver {
        public void notify(Meta meta, String key, Object value)
        {
            String className = (String)value;
            int dashIdx = className.indexOf("_");
            String fieldSuffix = (dashIdx != -1) ? className.substring(dashIdx+1) : "";

            meta.beginRuleSet(className);
            try {
                int rank = 1;
                for (Map.Entry<String, String> e : classFields.entrySet()) {
                    String name = e.getKey() + fieldSuffix;
                    Map m = new HashMap();
                    m.put(ObjectMeta.KeyType, e.getValue());
                    m.put(ObjectMeta.KeyField, name);
                    m.put(ObjectMeta.KeyVisible, true);
                    m.put(ObjectMeta.KeyRank, (rank++) * 10);

                    List<Rule.Selector> selectorList = Arrays.asList(new Rule.Selector(ObjectMeta.KeyClass, className),
                                                    new Rule.Selector(ObjectMeta.KeyField, name));
                    ListUtil.lastElement(selectorList)._isDecl = true;

                    Rule r = new Rule(selectorList, m, ObjectMeta.ClassRulePriority);
                    meta.addRule(r);
                }
            } finally {
                meta.endRuleSet();
            }

            String ruleText = ruleTextTemplate.replace("&CLASS&", className).replace("&SFX&", fieldSuffix);
            _meta.loadRules(ruleText);

            // System.out.printf("** Registered rules for class %s\n", className);
        }
    }

    static int _Iterations = 0;
    static int _ClassesCreated = 0;
    static int _FinishedThreads = 0;

    static class TestState implements Runnable {
        String _className;
        int _startClass, _classCount;
        int _repetitions;

        public TestState (String className, int startClass, int classCount, int repetitions) {
            _className = className;
            _startClass = startClass;
            _classCount = classCount;
            _repetitions = repetitions;
        }

        public void run() {
            try {
                for (int cnum = _startClass; cnum < _startClass + _classCount; cnum++) {
                    String suffix = Integer.toString(cnum);
                    String className = _className.concat("_").concat(suffix);

                    for (int i=0; i < _repetitions; i++) {
                        Context ctx = _meta.newContext();
                        ctx.push();
                        ctx.set("layout", "Inspect");
                        ctx.set(ObjectMeta.KeyClass, className);

                        List<String>fields = _meta.itemNames(ctx, ObjectMeta.KeyField);
                        Assert.that(fields.size() == classFields.size(), "Mismatched fields list: %s for class:%s", fields, className);

                        ctx.push();
                        ctx.set(ObjectMeta.KeyField, "firstName" + suffix);
                        String label = (String) ctx.propertyForKey("label");
                        Assert.that("FN".equals(label), "label mismatch: %s", label);
                        ctx.push();
                        ctx.set("operation", "edit");
                        String label2 = (String) ctx.propertyForKey("label");
                        Assert.that("FN2".equals(label2), "label2 mismatch: %s", label);
                        ctx.pop();
                        ctx.pop();

                        ctx.push();
                        ctx.set(ObjectMeta.KeyField, "rating" + suffix);
                        String nameProp = (String)ctx.propertyForKey("field");
                        Assert.that(("rating"+suffix).equals(nameProp), "field name mismatch: %s", nameProp);
                        ctx.pop();

                        // System.out.printf("Passed run for class %s, rep %d\n", className, i);
                        ctx.pop();
                    }
                    synchronized (ConcurrencyTest.class) {
                        _Iterations += _repetitions;
                        _ClassesCreated++;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } finally {
                synchronized (ConcurrencyTest.class) {
                    _FinishedThreads++;
                }
            }
        }
    }
    public static void main (String argv[])
    {
        int NumIterations = 5000000;
        // int NumClasses = 1;
        // int NumThreads = 1;
        int NumClasses = 1000;
        int NumThreads = 8;
        int ThreadOverlap = 4;  // Num Threads concurrently hitting same classes

        boolean useUIMeta = true;

        if (useUIMeta) {
            AWConcreteApplication.defaultApplication();
            _meta = UIMeta.getInstance();
        } else {
            _meta = new ObjectMeta();
        }

        _meta.registerKeyInitObserver(ObjectMeta.KeyClass, new PseudoIntrospectionProvider());

        int classesPerThread = NumClasses / (NumThreads / ThreadOverlap);
        int itersPerClass = (NumIterations / NumClasses) / ThreadOverlap;

        // Single threaded run
        for (int i=0; i<NumThreads/ThreadOverlap; i++) {
            for (int j=0; j<ThreadOverlap; j++) {
                try {
                    new Thread(new TestState("TestClass", i*classesPerThread, classesPerThread, itersPerClass)).start();
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        int finishedThreads = 0;
        long lastMillis = System.currentTimeMillis(), startMillis = lastMillis;
        int lastIters = 0, lastClasses = 0;
        while (finishedThreads < NumThreads) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            int nowClasses, nowIters;
            long nowMillis;
            synchronized (ConcurrencyTest.class) {
                finishedThreads = _FinishedThreads;
                nowMillis = System.currentTimeMillis();
                nowClasses = _ClassesCreated;
                nowIters = _Iterations;
            }
            long lapMillis = nowMillis - lastMillis;
            System.out.printf(" %.2f (+ %.2f): Classes: %d (+%,d), Iterations: %,d (+%,d) -- %,.2f / sec\n",
                    ((double)nowMillis - startMillis)/1000, ((double)lapMillis)/1000,
                    nowClasses, (nowClasses - lastClasses),
                    nowIters, (nowIters - lastIters),
                    ((double)(nowIters - lastIters))/((double)lapMillis/1000));
            lastClasses = nowClasses;
            lastIters = nowIters;
            lastMillis = nowMillis;            
        }
        System.out.printf("Tests COMPLETED.  Classes Created: %d, Iterations: %d\n",
                _ClassesCreated, _Iterations);
        System.out.printf("   Rule index entries processed: %d\n", Match._Debug_ElementProcessCount);
    }
}
