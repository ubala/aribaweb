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

    $Id: //ariba/platform/util/core/ariba/util/log/Log.java#25 $
*/

package ariba.util.log;

import org.apache.log4j.Level;
import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
    Log messages are grouped into Loggers which can be turned
    on and off.

    All messages are sent to the log writer with is a set of other
    writers -- usually a file and the console.

    @aribaapi documented
*/
public class Log
{

    /**
        Add the common Console Appender to the root Logger
        @deprecated There's no need to use addConsole anymore
        @aribaapi ariba
    */
    public static void addConsole ()
    {
    }

    /**
        @see org.apache.log4j.Level#DEBUG
        @aribaapi ariba
    */
    public static Level DebugLevel = Level.DEBUG;

    /**
        @see org.apache.log4j.Level#INFO
        @aribaapi ariba
    */
    public static Level InfoLevel = Level.INFO;

    /**
        @see org.apache.log4j.Level#WARN
        @aribaapi ariba
    */
    public static Level WarnLevel = Level.WARN;

    /**
        @see org.apache.log4j.Level#ERROR
        @aribaapi ariba
    */
    public static Level ErrorLevel = Level.ERROR;

    /**
        Convert string representation of level to a Level object
        @see org.apache.log4j.Level#toLevel(java.lang.String)
    */
    public static Level toLevel (String level)
    {
        return Level.toLevel(level);
    }

    /*
        The standard set of Loggers.  These are here rather than in
        Logger just for the shorter name to reference them.
    */

    /**
        Logger for startup messages for all components.

        @aribaapi private
    */
    public static final Logger startup =
        (Logger)Logger.getLogger("startup");

    /**
        Logger for startup messages for the util component,
        which is a child of the startup Logger.

        @aribaapi private
    */
    public static final Logger startupUtil =
        (Logger)Logger.getLogger("startup.util");


    /**
        Logger for messages that are created in custom
        implementations of Ariba products at customer sites.

        @aribaapi documented
    */
    public static final Logger customer =
        (Logger)Logger.getLogger("customer");

    /**
     Log message categories for socket connect and disconnect
     reporting.

     @aribaapi documented
     */
    public static final Logger http =
            (Logger)Logger.getLogger("http");

    /**
     Log message categories for servlet related logging. This is a
     child of the connect category.

     @aribaapi documented
     */
    public static final Logger httpServlet =
            (Logger)Logger.getLogger("http.servlet");

    /**
        Logger for util class messages.

        @aribaapi documented
    */
    public static final Logger util =
        (Logger)Logger.getLogger("util");

    /**
        Logger for util I/O class messages.

        @aribaapi documented
    */
    public static final Logger utilIO =
        (Logger)Logger.getLogger("util.io");

    /**
        Logger for partner messages.

        @aribaapi documented
    */
    public static final Logger partner =
        (Logger)Logger.getLogger("partner");

    /* Internationalization */

    /**
        Logger for internationalization and localized
        resource related messages.

        @aribaapi documented
    */
    public static final Logger i18n =
        (Logger)Logger.getLogger("i18n");

    /* Cluster and workflow level operations */

    /**
        Logger for client requested server operations.

        @aribaapi documented
    */
    public static final Logger serverOps =
        (Logger)Logger.getLogger("serverOps");

    /**
        Logger to turn on and report on consistency
        checks on LRUHashtables.

        @aribaapi private
    */
    public static final Logger lruCheck =
        (Logger)Logger.getLogger("lruCheck");

    /**
        Logger for LRUHashtable debugging.

        @aribaapi private
    */
    public static final Logger lruDebug =
        (Logger)Logger.getLogger("lruDebug");

    /**
        Logger for recording objects being removed from
        LRUHashtables.

        @aribaapi private
    */
    public static final Logger lruPurge =
        (Logger)Logger.getLogger("lruPurge");

    /**
        Logger for parameter referencing

        @aribaapi private
    */
    public static final Logger paramReference =
        (Logger)Logger.getLogger("paramReference");

    /**
        Hack category where we put messages we aren't sure what
        do do with yet.  There shouldn't be anything here when
        we ship.

        @aribaapi private
    */
    public static final Logger fixme =
        (Logger)Logger.getLogger("general");

    /**
        This category is on by default within Ariba Development, but off in
        release builds or for the customer.  Use it catch things that are
        warnings for us but not for customers.  Examples are defaulters
        without names, or strings that aren't localized.

        @aribaapi private
    */
    public static final Logger aribaInternalDev =
        (Logger)Logger.getLogger("aribaInternalDev");

    /**
        This category is off by default.  Parent category for perf loggers.
        Not used directly.

        @aribaapi private
    */
    public static final Logger perf_log =
        (Logger)Logger.getLogger("perfLog");

    /**
        This category is off by default.  It allows turning on performance
        trace logging.

        @aribaapi private
    */
    public static final Logger perf_log_trace =
        (Logger)Logger.getLogger("perfLog.trace");

    /**
        This category is off by default.  It allows turning on performance
        trace logging.

        @aribaapi private
    */
    public static final Logger perf_log_detail =
        (Logger)Logger.getLogger("perfLog.detail");

    /**
        This category is off by default.  It allows turning on performance
        monitoring and logging of long running requests.

        @aribaapi private
    */
    public static final Logger perf_log_exception =
        (Logger)Logger.getLogger("perfLog.exception");

    /**
        Logger for thread pool

        @aribaapi ariba
    */
    public static final Logger threadpool =
        (Logger)Logger.getLogger("threadpool");

    public static final Logger shutdown =
        (Logger)Logger.getLogger("shutdown");

    /**
        Instances of Log can not be instantiated

        @aribaapi private
    */
    protected Log ()
    {
    }

    /*
        Do any needed processing now that we know these loggers are
        set up.
    */
    static {
        Logger.convertEarlyChecks();
    }


    // utilities
    public static void logStack (Logger category, String msg)
    {
        Exception e = new Exception(msg);
        logException(category, e);
    }

    public static void logException (Logger category, Exception e)
    {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        category.debug(result.toString());
    }

}
