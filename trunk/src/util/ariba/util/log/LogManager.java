/*
    Copyright 1996-2012 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/log/LogManager.java#31 $
*/

package ariba.util.log;

import ariba.util.core.ArgumentParser;
import ariba.util.core.ClassUtil;
import ariba.util.core.FileUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.Parameters;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import ariba.util.i18n.I18NUtil;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.varia.DenyAllFilter;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
    Helper class with utility methods for creation and access of
    common appenders, the means to archive log files, and stamdard
    "lifecycle" methods (setupArguments, processArguments, startup,
    shutdown, etc.).
    @aribaapi ariba
*/
public class LogManager extends org.apache.log4j.LogManager
{

    /**
        The standard log filename suffix including extension.
        @aribaapi private
    */
    public static final String LogFileSuffix  = "Log.txt";

    /**
        The command line-style option name for the console logging switch.
        @aribaapi ariba
        @see #logToConsole
    */
    public static final String OptionLogToConsole  = "logToConsole";


    static {
        ClassUtil.classTouch("org.apache.log4j.LogManager");
    }

    /**
        The appender for standard out.
        @aribaapi private
    */
    private static Appender commonConsole = null;

    /**
        The appender for the primary file.
        @aribaapi private
    */
    private static Appender commonFile  = null;

    /**
        List of appenders to support the legacy model for printing.
        at warning level or below
        @aribaapi private
    */
    private static List warningLogHandlers = ListUtil.list();

    /**
        The common name for the file in Parameters.table.
        @aribaapi ariba
    */
    public static final String CommonNameForFile = "MainLogFile";

    /**
        The common name for the console listener.
        @aribaapi ariba
    */
    public static final String CommonNameForConsole = "Console";

    /**
        The common name for the Inspector log in Parameters.table.
        @aribaapi ariba
    */
    public static final String CommonNameForInspector = "InspectorAuditFile";

    /**
        The default directory where log files will be stored.
        @aribaapi private
    */
    public static final String DefaultLogDirectory = "logs";

    /**
        The default directory where archived log files will be
        stored.
        @aribaapi private
    */
    public static final String DefaultLogArchive =
        FileUtil.fixFileSeparators(DefaultLogDirectory + "/archive");

    /**
        The directory where active log files will be stored.
        @aribaapi private
    */
    private static String directoryName;

    /**
        The directory where archive log files will be stored.
        @aribaapi private
    */
    private static String archiveDirectoryName;

    /**
        The standard encoding method for log files.
        @aribaapi private
    */
    private static String encoding = I18NUtil.EncodingUTF_8;

    /**
        A switch for controlling logging to standard out.
        Note: this controls whether the initial console appender
        established with log4j parameters is removed from the
        root logger when the startup method is called. In some
        cases the removeDefaultConsole method is called explicitly.
        @aribaapi private
        @see #removeDefaultConsole()
        @see #startup()
    */
    private static boolean logToConsole;

    /**
        The time LogManager was initialized.
        @aribaapi private
    */
    private long creationTimeStamp = System.currentTimeMillis();

    /**
        Accessor for timestamp when logging was initialized - pretty
        close to start of process.

        @aribaapi ariba
    */
    public long loggingInitializedAt ()
    {
        return creationTimeStamp;
    }

    /**
        Override the log4j method to make sure we only return
        our Loggers. Curse you, Apache Commons!

        @aribaapi ariba
    */
    public static Enumeration getCurrentLoggers ()
    {
        return ListUtil.listToEnumeration(getCurrentLoggerList());
    }

    /**
        Replace the log4j functionality to make sure we only return
        our Loggers. Curse you, Apache Commons!

        @aribaapi ariba
    */
    public static List getCurrentLoggerList ()
    {
        Enumeration e = org.apache.log4j.LogManager.getCurrentLoggers();
        List returnList = ListUtil.list();
        while (e.hasMoreElements()) {
            Object next = e.nextElement();
            if (next instanceof Logger) {
                returnList.add(next);
            }
        }

        return returnList;
    }

    /**
        Establish the directory where active log files will be stored.
        @param dir the name (not a path) of the directory
        @aribaapi ariba
    */
    public static void setDirectoryName (String dir)
    {
        directoryName = dir;
    }

    /**
        Return the directory where active log files will be stored.
        @return String the name (not a path) of the directory
        @aribaapi ariba
    */
    public static String getDirectoryName ()
    {
        return (directoryName != null) ?
            directoryName :
            DefaultLogDirectory;
    }

    /**
        Establish the directory where archive log files will be stored.
        @param archiveDir the name (not a path) of the directory
        @aribaapi ariba
    */
    public static void setArchiveDirectoryName (String archiveDir)
    {
        archiveDirectoryName = archiveDir;
    }

    /**
        Return the directory where archive log files will be stored.
        @return String the name (not a path) of the archive directory
        @aribaapi ariba
    */
    public static String getArchiveDirectoryName ()
    {
        return addYearMonthDayHierarchy((archiveDirectoryName != null) ?
            archiveDirectoryName : DefaultLogArchive);
    }

    /**   
     * Add year, month (padded 0), day(padded 0) hierarchy
     */
    private static String addYearMonthDayHierarchy (String directoryName)
    {
        if (StringUtil.nullOrEmptyOrBlankString(directoryName)) {
            return directoryName;
        }

        Calendar gc = Calendar.getInstance();
        String y = String.valueOf(gc.get(Calendar.YEAR));

        /* month starts with 0 */
        String m = String.valueOf(gc.get(Calendar.MONTH)+1);
        if (gc.get(Calendar.MONTH) < 9) {
            m = StringUtil.strcat("0", m);
        }

        String d = String.valueOf(gc.get(Calendar.DAY_OF_MONTH));
        if (gc.get(Calendar.DAY_OF_MONTH) < 10) {
            d = StringUtil.strcat("0", d);
        }

        return FileUtil.fixFileSeparators(Fmt.S("%s/%s/%s/%s",directoryName, y, m, d));
    }
    
    /**
        Deactivates the DefaultConsole appender that is defined as part of
        the static configuration of log4j.
        @aribaapi ariba
    */
    public static void removeDefaultConsole ()
    {
        Appender defaultConsole = Logger.getRootLogger().getAppender("DefaultConsole");
        if (defaultConsole != null) {
            defaultConsole.addFilter(new DenyAllFilter());
        }
    }

    /**
        Creates a new appender that writes to standard out using the default
        layout.
        @return Appender a new appender that writes to standard out
        @aribaapi ariba
    */
    public static Appender createConsoleAppender ()
    {
        ConsoleAppender console = new ConsoleAppender(new StandardLayout());
        console.setName(CommonNameForConsole);
        AsyncAppender asyncConsole = wrapInAsyncAppender(console);
        return asyncConsole;
    }

    /**
        Return the shared standard out appender, if one has been
        defined.
        @return Appender null or the shared standard out appender
        @aribaapi ariba
    */
    public static Appender getCommonConsoleAppender ()
    {
        return commonConsole;
    }

    /**
        Set the shared standard out appender.
        @param console the shared standard out appender to set
        @param makeRoot if true add this appender to the root logger
        @aribaapi ariba
    */
    public static void setCommonConsoleAppender (Appender console, boolean makeRoot)
    {
        if ((commonConsole == null) && (console != null)) {
            commonConsole = console;
            addWarningLogHandler(console);
            if (makeRoot) {
                getRootLogger().addAppender(commonConsole);
            }
        }
    }

    /**
        Creates a new appender that writes to the named file using the archiving
        options.
        @param prefix the filename prefix
        @return Appender a new appender that writes to a file named: prefix +
        standard suffix
        @aribaapi ariba
        @see #LogFileSuffix
    */
    public static Appender createFileAppender (String prefix)
    {
        ArchivingAppender archiving = new ArchivingAppender();
        archiving.setFile(StringUtil.strcat(prefix, LogFileSuffix));
        archiving.setName(prefix);
        archiving.activateOptions();
        AsyncAppender asyncArchiving = wrapInAsyncAppender(archiving);
        return asyncArchiving;
    }

    /**
        Return the primary file appender, if one has been defined.
        @return Appender null or the primary file appender
        @aribaapi ariba
    */
    public static Appender getCommonFileAppender ()
    {
        return commonFile;
    }

    /**
        Set the primary file appender.
        @param file the primary file appender to set
        @param makeRoot if true add this appender to the root logger
        @aribaapi ariba
    */
    public static void setCommonFileAppender (Appender file, boolean makeRoot)
    {
        if ((commonFile == null) && (file != null)) {
            commonFile = file;
            addWarningLogHandler(file);
            if (makeRoot) {
                getRootLogger().addAppender(commonFile);
            }
        }
    }

    /**
        Return a list of the common appenders if they've been defined.
        When the appenders are set up from logging parameters, a
        console and a file appender are created under normal
        circumstances.
        @return List possibly empty list of the common Appenders
        @aribaapi ariba
        @see LogManager#setCommonConsoleAppender(Appender, boolean)
        @see LogManager#setCommonFileAppender(Appender, boolean)
    */
    static List getCommonAppenders ()
    {
        List appenders = ListUtil.list();
        Appender commonFile = getCommonFileAppender();
        Appender commonConsole = getCommonConsoleAppender();
        if (commonFile != null) {
            appenders.add(commonFile);
        }
        if (commonConsole != null) {
            appenders.add(commonConsole);
        }
        return appenders;
    }

    /**
        Adds an appender to the list of <i>warning log handlers</i>.
        <i>Warning log handlers</i> are the special appenders which print
        the log messages when the level is warning or lower, even if the
        logger has not been configured for these appenders.
        @param appender the appender to add.
        @aribaapi ariba
    */
    public static void addWarningLogHandler (Appender appender)
    {
        synchronized (warningLogHandlers) {
            ListUtil.addElementIfAbsent(warningLogHandlers, appender);
        }
    }

    /**
        Removes an appender from the list of <i>warning log handlers</i>.
        <i>Warning log handlers</i> are the special appenders that print
        the log messages when the level is warning or lower, even if the
        logger has not been configured for these appenders.
        @param appender the appender to remove.
        @aribaapi ariba
    */
    public static void removeWarningLogHandler (Appender appender)
    {
        synchronized (warningLogHandlers) {
            warningLogHandlers.remove(appender);
        }
    }

    /**
        Return the <i>warning log handlers</i> list.
        <i>Warning log handlers</i> are the special appenders that print
        the log messages when the level is warning or lower, even if the
        logger has not been configured for these appenders.
        @return List the warning log handler appender list
        @aribaapi ariba
    */
    static List getWarningLogHandlers ()
    {
        return warningLogHandlers;
    }

    /**
        Wrap an appender so it works asynchronously, and will buffer logging
        events.
        @param appender the appender to be made asynchronous.
        @aribaapi ariba
    */
    public static final AsyncAppender wrapInAsyncAppender (Appender appender)
    {
        AsyncAppender async = new AsyncAppender(Logger.isSuspendCheckingOn());
        async.addAppender(appender);
        async.setName(appender.getName());
        async.addFilter(new AvoidDuplicateEvents());
        async.activateOptions();
            // Register this appender so it can be polled for error/debug
            // messages.
        Logger.registerAsyncAppender(async);
        return async;
    }

    /**
        Set the standard encoding method for log files.
        @param enc the standard encoding method
        @aribaapi ariba
    */
    public static void setEncoding (String enc)
    {
        encoding = enc;
    }

    /**
        Return the standard encoding method for log files.
        @return String the standard encoding method
        @aribaapi ariba
    */
    public static String getEncoding ()
    {
        return encoding;
    }

    /**
        First stage of option argument parsing. Used to indicate which
        options LogManager is interested in, what type their values
        are, and whether each option is mandatory or optional.
        @param arguments the source of the current command line arguments
        @aribaapi ariba
    */
    public static void setupArguments (ArgumentParser arguments)
    {
        arguments.addOptionalBoolean(OptionLogToConsole, true);
    }

    /**
        Processing stage of option argument parsing. Used to process the
        options LogManager is interested in.
        @param arguments the source of the current command line arguments
        @aribaapi ariba
    */
    public static void processArguments (ArgumentParser arguments)
    {
        logToConsole = arguments.getBoolean(OptionLogToConsole);
    }

    /**
        Indicates the controlling server or command line tool has completed
        initialization (of parameters, etc.) and is ready to put the
        LogManager in "running" mode.
        @aribaapi ariba
    */
    public static void startup ()
    {
        if (!logToConsole) {
            removeDefaultConsole();
        }
    }

    /**
        Archives registered logging files.
        @aribaapi ariba
        @see ArchivingAppender
    */
    public static void archiveLogFiles (Parameters params)
    {
        Iterator appenders = ArchivingAppender.getIteratorForAppenders();

        while (appenders.hasNext()) {
            ArchivingAppender appender =
                (ArchivingAppender)appenders.next();
            appender.archiveLogFile();
        }
        //request perfLogger to archive itself
        PerformanceState.archiveLogFile(params);
    }

    /**
        ToDo: seemingly vestigial method with a handful of callers
        Should probably remove as it does nothing anyway.
        @aribaapi ariba
    */
    public static void flush ()
    {
            // ToDo: probably should get rid of this (and update the
            // handful of callers)
    }

    /**
        Indicates the controlling server or command line tool has completed.
        @aribaapi ariba
    */
    public static void shutdown ()
    {
        if (commonFile != null) {
            commonFile.close();
            commonFile = null;
        }

        if (commonConsole != null) {
            commonConsole.close();
            commonConsole = null;
        }
        org.apache.log4j.LogManager.shutdown();
    }

    /**
        ToDo: seemingly vestigial method with a handful of callers
        Should probably remove--it's hardcoded to return true.
        @aribaapi ariba
    */
    public static boolean loggingInitialized ()
    {
        return true;
    }
}

