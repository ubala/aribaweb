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

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadDebugState.java#10 $
*/

package ariba.util.core;

/**
    This class maintains debug state for the current thread. The class
    is intended to allow callers to add and remove arbitrary debug
    information as processing of a transation continues. This way when
    there is an error or a hang this information can be retrieved. It
    is very cheap to add and remove items as there is no
    synchronization. All static methods are safe to call.

    <p>

    Non static calls should be made with extreme caution - as
    that code will have to be able to handle all types of exceptions
    due to possible synchronization issues.

    <p>

    The only non static methods are for reading data exclusivly. This
    way a thread trying to look at the debug state of another thread
    will not be able to, under any circumstances, damage the thread it
    is looking at. Although it certainly may trigger exceptions in its
    own thread if race conditions are hit.

    @aribaapi documented
*/
public class ThreadDebugState
{

    public static boolean threadStateEnabled = true;
    private static final ThreadDebugKey ThreadNameKey =
        new ThreadDebugKey("ThreadName");

    /*
        private static int threadLocalAccessCount = 0;

            private static final GrowOnlyHashtable statAggrigation = new GrowOnlyHashtable();


    private static void updateStats (ThreadDebugKey key)
    {
        Integer i = (Integer)statAggrigation.get(key);
        if (i == null) {
            i = Util.getInteger(0);
        }
        statAggrigation.put(key, Util.getInteger(i.intValue()+1));
    }
        */

    /**
        The key specified will overwrite any other date stored with a
        pointer equal key. The value reference will be saved, but not
        used, until needed. This is to allow operations to be
        significantly faster. Callers of this method should not do
        much computation or memory allocation to create the value
        object. It is ok if the value object's toString() is expensive
        as that will be called rarely. It is also ok if the value
        object changes internal contents during operation.
        <p>
        If <code>value</code> implements DebugState the debugState()
        method will be called instead of toString() on
        <code>value</code> when it is needed. This should happen
        rarely, so this method implementation may also be expensive
        without hurting performance of the overall application.
        <p>

        The toString() method of the value object must be be made safe
        for other threads to call, even if the value is being modified
        at the time. Throwing exceptions from the toString if the
        thread gets unlucky is acceptable, but data corruption is not.

        @param key the ThreadDebugKey to use for this object when
        printed. <code>key</code> may not be null.
        @param value the object to print out as part of the
        state. <code>value</code> may be null.
        @aribaapi documented
        @see DebugState
    */
    public static void set (ThreadDebugKey key, Object value)
    {
        if (!threadStateEnabled) {
            return;
        }
            //updateStats(key);
        StateMap htable = getThisThreadHashtable();
            // make work for the caller be as little as possible and
            // handle null values - since those are determined at
            // runtime unlike the key...
        if (value == null) {
            value = NullObject;
        }
        htable.put(key, value);
    }

    /**
        Retrieve a value for a specified key. The key must be pointer
        equal to the key used to set the value.
        @aribaapi ariba
    */
    public static Object get (ThreadDebugKey key)
    {
        if (!threadStateEnabled) {
            return null;
        }
            //updateStats(key);
        StateMap htable = getThisThreadHashtable();
        return internalGet(key, htable);
    }

    /**
        Remove a value for a specified key. The key must be pointer
        equal to the key used to set the value. The old value will be
        returned.

        @param key the ThreadDebugKey to delete from the current
        application state.
        @return the old value that was saved.
        @aribaapi documented
    */
    public static Object remove (ThreadDebugKey key)
    {
        if (!threadStateEnabled) {
            return null;
        }
            //updateStats(key);
        Hashtable htable = getThisThreadHashtable();
        Object oldValue = htable.remove(key);
        if (oldValue == NullObject) {
            oldValue = null;
        }
        return oldValue;
    }

    /**
        Remove all values that are currently stored.
        @aribaapi ariba
    */
    public static void clear ()
    {
        ProgressMonitor.internalClear();
        
        if (!threadStateEnabled) {
            return;
        }
        StateMap state = getThisThreadHashtable();
        PerformanceState.internalClear(state, true);
        Object threadName = state.get(ThreadNameKey);

        // clear the hashtable, but restore the thread name key
        state.clear();
        state.put(ThreadNameKey, threadName);

        // we force clear of the PerformanceState as well
    }

    private static Boolean localTrue = Boolean.TRUE;
    private static Boolean localFalse = Boolean.FALSE;
    private static final State reentryCheck = StateFactory.createState();

    /**
        Return a string representing the contents of the state.
        @return The current state of this thread as a string. If there
        are errors in creating the string, the returned string will
        contain the text of the exception.

        @aribaapi documented
    */
    public static String makeString ()
    {
        if (!threadStateEnabled) {
            return "";
        }
        if (localTrue == reentryCheck.get()) {
                // Add a check here so that if the protectedToString
                // call causes an assert to fail we do not end up with
                // a stack overflow.
            return "do not recursivly call makeString";
        }
        try {
            reentryCheck.set(localTrue);
            return protectedToString(getThisThreadHashtable());
        }
        finally {
            reentryCheck.set(localFalse);
        }
    }

    /**
        This is the method to call to print state from a different
        thread. To print your own thread's state, call the static
        makeString() method instead. This method will catch any
        exceptions that are thrown during processing and return them
        as the string.
        @aribaapi ariba
    */
    public String toString ()
    {
        return protectedToString(someRandomThreadState);
    }

    /**
        This is an unsafe method. The caller is required to protect
        against exceptions that occur as a result of synchronization
        issues. It will attempt to perform an equivilent of a get.
        @aribaapi private
    */
    public Object unsafeGet (ThreadDebugKey key)
    {
        if (!threadStateEnabled) {
            return null;
        }

        Assert.that(someRandomThreadState != null,
                    "Do not call unsafe methods unless you know what " +
                    "you are doing and deeply understand this code");
        return internalGet(key, someRandomThreadState);
    }
    private static final ThreadDebugState EmptyThreadDebugState =
        new ThreadDebugState(new StateMap());

    /**
        This method is thread safe, but is the first step towards
        getting non safe data. It will return a new ThreadDebugState
        object that will allow unsafe calls on it from a different
        thread. That object will, from then on, be able to see state
        that is set on this thread - even state set after this call.
        @aribaapi ariba
    */
    public static ThreadDebugState getUnsafeThreadDebugState ()
    {
        if (!threadStateEnabled) {
            return EmptyThreadDebugState;
        }
        return new ThreadDebugState(getThisThreadHashtable());
    }

    private static Object internalGet (ThreadDebugKey key, StateMap htable)
    {
        Object value = htable.get(key);
        if (value == NullObject) {
            value = null;
        }
        return value;
    }


    protected static String protectedToString (StateMap someRandomThreadState)
    {
        try {
            return unprotectedToString(someRandomThreadState);
        }
            // don't want debugging code to bring down the system.
        catch (RuntimeException ex) {
            return Fmt.S("Unable to print state: %s", SystemUtil.stackTrace(ex));
        }
    }

    private static String unprotectedToString (StateMap stateInfo)
    {
        if (!threadStateEnabled) {
            return "";
        }
        // Note: We MUST call toString() explicitly here to work around a glitch
        // where FormattingSerializer skips calling toString() on subclasses of Map.
        if (stateInfo._performanceState != null) {
            String perfString = stateInfo._performanceState.toString();
            return Fmt.S("%s, PerformanceState: %s", stateInfo, perfString);
        }
        return stateInfo.toString();
    }

    private static final State state = StateFactory.createState();
    private static final Object NullObject = new String("NullValue"); // OK
    private final StateMap someRandomThreadState;

    private ThreadDebugState (StateMap someRandomThreadState)
    {
        this.someRandomThreadState = someRandomThreadState;
    }

    protected static StateMap getThisThreadHashtable ()
    {
        StateMap htable = (StateMap)state.get();
        if (htable == null) {
            htable = new StateMap();
            state.set(htable);
                // make sure this is after the state is set so there
                // are no recursion problems.
            set(ThreadNameKey, Thread.currentThread().toString());
        }
        /*
        threadLocalAccessCount++;
        if (threadLocalAccessCount % 100 == 0) {
            Util.out().println("At " + new java.util.Date() + " there are " +
                               threadLocalAccessCount +
                               " calls to thread local");
            Util.out().println("access pattern: " + statAggrigation);
        }
    */
        return htable;
    }

    /**
        This is just a convinence for callers who wish to create
        Hashtables or Lists that contain objects that may be
        null. ThreadDebugState itself can handle null values
    */
    public static Object nullSafeObject (Object o)
    {
        if (o == null) {
            return "null";
        }
        return o;
    }

    /**
     *  @aribaapi ariba
     */
    public static class StateMap extends EqHashtable
    {
        protected PerformanceState.Stats _performanceState;
    }
}

