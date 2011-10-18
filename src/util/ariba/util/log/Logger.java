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

    $Id: //ariba/platform/util/core/ariba/util/log/Logger.java#28 $
*/

package ariba.util.log;

import ariba.util.core.ArgumentParser;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.Constants;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.MapUtil;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Map;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.NullEnumeration;
import org.apache.log4j.helpers.OptionConverter;

/**
    This class provides additional functionality over the Logger class provided in Log4j.
    Instances of this class are obtained through the static getLogger() method.

    For the levels INFO, WARN, and ERROR, the messages are
    internationalized and require a messageID for their first
    parameter.  This parameter will be used to lookup the message in a known
    resource file.  The resource file can be found by looking for the file
    Log.<categoryName>.csv where categoryName is the name of the category that
    is having the message logged to.  Unlike Fmt.Sil where you pass the name
    of the resource file (StringTable) LogMessageCategory does this step for
    you based on the categoryName used.

    @aribaapi documented
*/
public class Logger extends org.apache.log4j.Logger
{

    static {
        ariba.util.core.ClassUtil.classTouch("ariba.util.log.LogManager");
    }

    /**
        The factory which creates instances of this class.

        @aribaapi private
    */
    private static LoggerFactory myFactory = new LoggerFactory();

    /**
        The fully qualified name of this class.

        @aribaapi private
    */
    public static String FQCN = Logger.class.getName() + ".";

    /**
        The default locale prefix is the English language.

        @aribaapi private
    */
    private static final String AribaDefaultLocalePrefix = "en_";

    /**
        The locale by which localizable messages should be translated to.

        @aribaapi private
    */
    private static Locale localeToUse = null;

    /**
        The delimiter that is used on the command line to separate the
        severity from the category.
        @aribaapi private
    */
    public static final String SeverityDelimiter = "/";

    /**
        The delimiter that is used on the command line to separate categories.
        @aribaapi private
    */
    public static final String CategoryDelimiter = ":";

    /**
        A static string for command line option to turn on and off logging.

        @aribaapi private
    */
    public static final String OptionLog      = "log";

    /**
        A static string for the command line option to turn on and off stack
        traces for a specified category and severity.

        @aribaapi private
    */
    public static final String OptionLogWhere = "logWhere";

    /**
        Readonly.  The prefix applied to string table names.

        @see #StringTableFormat
        @aribaapi ariba
    */
    public static final String StringTablePrefix = "Log.";

    /**
        Readonly.  The string table where this Logger's messageIDs
        can be found for localization.

        @aribaapi ariba
    */
    public static final String StringTableFormat = StringTablePrefix + "%s";

    private static final Object Null = Fmt.Null;

    /**
        The name of the table file containing localization strings

        @aribaapi private
    */
    public String stringTable;

    // the suppressers for this category
    protected Map _suppressors;


    /**
        Construct a Logger based on the name.

        @param name The name for this Logger
        @aribaapi private
    */
    protected Logger (String name)
    {
        super(name);
        this.stringTable = Fmt.S(StringTableFormat, name);
    }

    /**
        StringTable for LogMessageCategory descriptions.
        @aribaapi private
    */
    public static final String LogDescTable = "ariba.logDescriptions";

    /**
        Returns the localized UI description for this Logger

        @param locale The locale to return the description for
        @return the description of the Logger
        @aribaapi documented
    */
    public String description (Locale locale)
    {
        return description(getName(), locale);
    }

    /**
        Returns the localized UI description for a Logger.

        @param name The name of the Logger
        @param locale The locale to return the description for
        @return the description of the Logger
        @aribaapi ariba
    */
    public static String description (String name, Locale locale)
    {
            //using 4 argument call so that we can turn off warnings
            //since a logger is not guaranteed to have a description
            //(it will have one only if it is "public")
        String desc = ResourceService.getService().getLocalizedString(LogDescTable,
                                                                      name,
                                                                      locale,
                                                                      false);
        return desc;
    }

    /**
        @aribaapi ariba
    */
    public boolean isPrivate ()
    {
        return PrivateLoggers.contains(getName());
    }

    /**
     * Create a logging event, with correct level and message.
     * @aribaapi private
     */
    protected LoggingEvent createLoggingEvent (String fqcn, Priority level,
                                             String messageId, Object message,
                                             Throwable t)
    {
        // check to see whether we modify the level
        if (_suppressors != null) {
            // check if this message should be suppressed
            LogSuppressor sup = (LogSuppressor)_suppressors.get(messageId);
            if (sup != null) {
                level = sup.possiblySuppress(level);
            }
        }

        return new LoggingEvent(fqcn, this, level, messageId, message, t);
    }

    /**
        Overrides the callAppenders method in org.apache.log4j.Logger.
        The extra code implements the necessary logic for partially additive.

        @aribaapi private
    */
    public void callAppenders (LoggingEvent event)
    {
        if (useWarningLogHandlers()) {
            if (event.getLoggerName().equals(this.getName()) &&
                event.getLevel().isGreaterOrEqual(Log.WarnLevel)) {
                List handlers = LogManager.getWarningLogHandlers();
                int size = handlers.size();
                Appender appender;
                for (int i = 0; i < size; i++) {
                    appender = (Appender)handlers.get(i);
                    appender.doAppend(event);
                }
            }
        }

        super.callAppenders(event);

    }

    /**
       Specifies whether this class wants to make use of warning log handlers when the appenders
       associated with this logger append the log event.
       Most subclasses should just inherit this implementation, but can override if
       so desired.
       @return <code>true</code> to use warning log handlers, <code>false</code> otherwise.
       @aribaapi ariba
    */
    protected boolean useWarningLogHandlers ()
    {
        return true;
    }


    /**
        Add the the usage arguments to the command line parser.

        @param arguments command-line arguments
        @aribaapi ariba
    */
    public static void setupArguments (ArgumentParser arguments)
    {
        String logUsage = Fmt.S("<category>%s<severity>",
                                SeverityDelimiter);
        logUsage = Fmt.S("%s%s:%s%s*",
                         logUsage,
                         ArgumentParser.OptionalOpen,
                         logUsage,
                         ArgumentParser.OptionalClose);
        arguments.addOptionalString(OptionLog, null, logUsage);
        arguments.addOptionalString(OptionLogWhere, null, logUsage);
    }

    /**
        Used strictly during processArguments to output debugging details
        if logging debugging is enabled.

        @aribaapi private
        @see #LoggingDebugPropertyName
    */
    private static void argDebugInfo (List loggers)
    {
        if (isDebugging()) {
            debugS("processArg loggers: %s", loggers, null, null, false);
            Iterator it = loggers.iterator();
            while (it.hasNext()) {
                Logger logger = (Logger)it.next();
                debugS(" category: %s, level: %s, logger: %s", logger.getName(),
                    logger.getLevel(), logger, false);
                Enumeration e = logger.getAllAppenders();
                debugS("  Appenders: %s", e, null, null, false);
                if (e != null) {
                    for (; e.hasMoreElements() ;) {
                        Appender a = (Appender)e.nextElement();
                        debugS("   name: %s, object: %s", a.getName(), a, null,
                            false);
                    }
                }
            }
        }
    }

    /**
        Process the commmand line arguments that are passed.

        @param arguments command-line arguments
        @aribaapi ariba
    */
    public static void processArguments (ArgumentParser arguments)
    {
        String  log      = arguments.getString(OptionLog);
        String  logWhere = arguments.getString(OptionLogWhere);

        if (!StringUtil.nullOrEmptyString(log)) {
            List loggers = setLevelsFromFlags(log, 0);
                // Make sure the command line loggers have the common appenders
                // (console, main file)
            addCommonAppendersToEmptyLoggers(loggers);
            argDebugInfo(loggers);
        }
            // ToDo: need to implement logWhere if we want to keep it
    }

    /**
        Force the specified log message to be sent to all configured Appenders.

        @param fqcn class name of Logger class
        @param level Level for this log message
        @param message this log message
        @param t throwable object associated with this message
        @aribaapi ariba
    */
    protected void forcedLog (String fqcn,
                              Priority level,
                              Object message,
                              Throwable t)
    {
            //use null for messageId since not localized
        callAppenders(createLoggingEvent(fqcn, level, null, message, t));
    }

    /**
        Localize a message.

        @param messageId ID of this message
        @param args List of arguments to be passed to format string
        @aribaapi ariba
    */
    public String localizeMessage (
        String messageId,
        List args)
    {

        String formatString =
            ResourceService.getService().getLocalizedString(
                stringTable,
                messageId,
                ResourceService.getService().getLocale(),
                true);

        String localized = ensureArguments(Fmt.Si(formatString, args),
                            messageId, args);

            // localize the message only if we have a locale whose language is not the
            // one associated with the default locale (en_US). All locales that starts
            // with 'en_' (case sensitive) have the same language (English) as en_US.
        if (localeToUse != null &&
            !localeToUse.toString().startsWith(AribaDefaultLocalePrefix)) {
            String nonEnglish = Fmt.Sil(localeToUse,
                                      stringTable,
                                      messageId,
                                      args);
            nonEnglish = ensureArguments(nonEnglish, messageId, args);
            localized = Fmt.S("%s\n%s",
                              nonEnglish,
                              localized);
        }

        return localized;
    }

    private String ensureArguments (String localized, String key, List args)
    {
        if (localized == null || localized.equalsIgnoreCase(key)) {
            localized = "Unmatched messageID & argument values are: %s";
            Iterator it = args.iterator();
            while (it.hasNext()) {
                if (it.next() != null) {
                    localized = StringUtil.strcat(localized, ",%s");
                }
            }
            localized = Fmt.S(localized, ArrayUtil.prepend(key, args.toArray()));
        }
        return localized;
    }

    /**
        Force the specified log to be localized and sent to all configured
        Appenders.

        @param fqcn class name of Logger class
        @param level Level for this log message
        @param t throwable object associated with this message
        @param messageId localization ID for this message
        @param args the list of arguments
        @aribaapi private
    */
    private void forcedLocalizedLog (String fqcn,
                                     Priority level,
                                     Throwable t,
                                     int messageId,
                                     List args)
    {
        forcedLocalizedLog(fqcn, level, t,
            localizeMessage(String.valueOf(messageId), args),
            messageId);
    }

    /**
        Force the specified log to be localized and sent to all configured
        Appenders.

        @param fqcn class name of Logger class
        @param level Level for this log message
        @param t throwable object associated with this message
        @param message the message to log
        @param messageId localization ID for this message
        @aribaapi private
    */
    protected void forcedLocalizedLog (String fqcn,
                                       Priority level,
                                       Throwable t,
                                       Object message,
                                       int messageId)
    {
        callAppenders(createLoggingEvent(fqcn,
                                       level,
                                       String.valueOf(messageId),
                                       message,
                                       t));
    }

    /**
        package the given arguments into a list of arguments. This is the argument list
        passed to the localizeMessage method.
        @param a1 the first argument
        @param a2 the second argument
        @param a3 the third argument
        @param a4 the forth argument
        @param a5 the fifth argument
        @param a6 the sixth argument
        @param more more arguments if needed
        @aribaapi private
    */
    protected List makeList (Object a1, Object a2, Object a3, Object a4,
                             Object a5, Object a6, Object[] more)
    {
        List args = ListUtil.list();
        if (!Null.equals(a1)) {
            args.add(a1);
        }
        if (!Null.equals(a2)) {
            args.add(a2);
        }
        if (!Null.equals(a3)) {
            args.add(a3);
        }
        if (!Null.equals(a4)) {
            args.add(a4);
        }
        if (!Null.equals(a5)) {
            args.add(a5);
        }
        if (!Null.equals(a6)) {
            args.add(a6);
        }

        if (more != null && more.length > 0) {
            int i = 0;
            while (i < more.length) {
                args.add(more[i++]);
            }
        }
        return args;
    }

    /**
        Set states (Levels) for Loggers from flag string.

        @param flags flag string
        @param type 0
        @return List of loggers modified
        @aribaapi ariba
    */
    public static List setLevelsFromFlags (String flags, int type)
    {
        List loggersModified = ListUtil.list();
            // Note: only log which not log where for now
        StringTokenizer tok = new StringTokenizer(flags, ":");
        while (tok.hasMoreTokens()) {
            String categorySeverityPair = tok.nextToken();
            StringTokenizer pairTok = new StringTokenizer(categorySeverityPair,
                                                          SeverityDelimiter);
            String category = pairTok.nextToken();
            Level level;
            if (!pairTok.hasMoreTokens()) {
                if (category.startsWith("+")) {
                    level = Log.InfoLevel;
                }
                if (category.startsWith("-")) {
                    level = Log.ErrorLevel;
                }
                else {
                    continue;
                }
            }
            else {
                    //make upper-case just in case
                level = Level.toLevel(pairTok.nextToken().toUpperCase(),
                                      Level.WARN);
            }
            if (category.startsWith("+") || category.startsWith("-")) {
                category = category.substring(1);
            }
            org.apache.log4j.Logger logger;
            if (category.equals("all")) {
                logger = LogManager.getRootLogger();
            }
            else {
                    //this code may instantiate a logger of name 'category'
                    //if the logger has not been created already
                    //this will happen if setLevelFromFlags gets called
                    //and tries to configure a logger who has not yet been
                    //initialized because the Log.java that initializes it
                    //has not been touched yet
                logger = Logger.getLogger(category);
            }
            if (logger != null) {
                logger.setLevel(level);
                loggersModified.add(logger);
            }
        }

        return loggersModified;
    }


    /*
        Overridden only because it is a convenient point to debug many
        uses of Loggers in our code.
        (deliberately not javadoc to avoid overriding log4j docs)
    */
    public void setLevel (Level level)
    {
        debugSTrace("setLevel name: %s, setLevel: %s", getName(), level);
        super.setLevel(level);
    }

    /*
        Overridden because of a bug in log4j that puts the logger in a
        state where getAllAppenders() can return null (which it isn't
        supposed to do) if addAppender(Appender) is passed null.
        (deliberately not javadoc to avoid overriding log4j docs)
    */
    public Enumeration getAllAppenders ()
    {
        Enumeration e = super.getAllAppenders();
        if (e == null) {
            e = NullEnumeration.getInstance();
        }
        return e;
    }

    /*
        Overridden because of a bug in log4j that puts the logger in a
        state where getAllAppenders() can return null (which it isn't
        supposed to do) if this method is passed null.
        (deliberately not javadoc to avoid overriding log4j docs)
    */
    public void addAppender (Appender appender)
    {
        if (appender != null) {
            super.addAppender(appender);
        }
    }

    /**
        Add specified Appender to loggers.
        @param flags flag string
        @param appender Appender to add to Loggers
        @return List of loggers added to
        @aribaapi ariba
    */
    public static List addAppenderFromFlags (String flags,
                                             Appender appender)
    {
        if (appender == null) {
            return ListUtil.list();
        }
        List loggersAddedTo = ListUtil.list();
            // ToDo: only log which not log where for now
        StringTokenizer tok = new StringTokenizer(flags, ":");
        while (tok.hasMoreTokens()) {
            String categorySeverityPair = tok.nextToken();
            StringTokenizer pairTok = new StringTokenizer(categorySeverityPair,
                                                          SeverityDelimiter);
                //ignore "/<severity" if specified
            String category = pairTok.nextToken();
            org.apache.log4j.Logger logger;
            if (category.startsWith("+") || category.startsWith("-")) {
                category = category.substring(1);
            }
            if (category.equals("all")) {
                logger = LogManager.getRootLogger();
            }
            else {
                logger = getLogger(category);
            }
            if (logger != null) {
                logger.addAppender(appender);
                loggersAddedTo.add(logger);
            }
        }
        return loggersAddedTo;
    }

    /**
        Add specified Appender to the loggers in a list.
        @param loggers list of loggers to have the appender added
        @param appender Appender to add to loggers
        @aribaapi ariba
    */
    public static void addAppenderToLoggers (List loggers,
                                             Appender appender)
    {
        Iterator it = loggers.iterator();
        while (it.hasNext()) {
            org.apache.log4j.Logger l = (org.apache.log4j.Logger)it.next();
            l.addAppender(appender);
        }
    }

    /**
        Add standard appenders to the loggers in a list, but only if the logger
        has no appenders already.
        @param loggers list of loggers to have the appenders added
        @aribaapi ariba
    */
    public static void addCommonAppendersToEmptyLoggers (List loggers)
    {
        List appenders = LogManager.getCommonAppenders();
        boolean isDebug = isDebugging();
        if (isDebug) {
                debugS("addCommonAppenders appenders: %s", appenders, null,
                    null, false);
                if (appenders.isEmpty()) {
                    debugSTrace(
                     "WARNING: addCommonAppenders called before common appenders exist.",
                     null, null);
                }
        }
        Iterator it = loggers.iterator();
        while (it.hasNext()) {
            Logger l = (Logger)it.next();
                // skip this logger if it already has appender(s)
            if (l.getAllAppenders().hasMoreElements()) {
                continue;
            }
            Iterator appIt = appenders.iterator();
            while (appIt.hasNext()) {
                Object next = appIt.next();
                if (isDebug) {
                    debugS("   logger: %s, appender: %s",
                        l, next, null, false);
                }
                l.addAppender((Appender)next);
            }
        }
    }

    /**
        Remove specified Appender from all Loggers in a list.
        @param loggers list of loggers to have appender removed
        @param appender Appender to remove from loggers
        @aribaapi ariba
    */
    public static void removeAppenderFromLoggers (List loggers,
                                             Appender appender)
    {
        Iterator it = loggers.iterator();
        while (it.hasNext()) {
            org.apache.log4j.Logger l = (org.apache.log4j.Logger)it.next();
            l.removeAppender(appender);
        }
    }

    /**
        Remove specified Appender from Loggers specified in an option string.
        @param flags flag string
        @param appender Appender to remove from Loggers
        @return List of loggers to have appender removed
        @aribaapi ariba
    */
    public static List removeAppenderFromFlags (String flags, Appender appender)
    {

        if (appender == null) {
            return ListUtil.list();
        }
        List loggersRemovedFrom = ListUtil.list();
        StringTokenizer tok = new StringTokenizer(flags, ":");
        while (tok.hasMoreTokens()) {
            String categorySeverityPair = tok.nextToken();
            StringTokenizer pairTok = new StringTokenizer(categorySeverityPair,
                                                          SeverityDelimiter);
                //ignore "/<severity" if specified
            String category = pairTok.nextToken();
            org.apache.log4j.Logger logger;
            if (category.startsWith("+") || category.startsWith("-")) {
                category = category.substring(1);
            }
            if (category.equals("all")) {
                logger = LogManager.getRootLogger();
            }
            else {
                logger = getLogger(category);
            }
            if (logger != null) {
                logger.removeAppender(appender);
                loggersRemovedFrom.add(logger);
            }
        }
        return loggersRemovedFrom;
    }

    /**
     * Set a particular message on this logger to be supressable.
     */
    public void makeSuppressable (String id, long millisecondThreshold,
                                  Priority suppressToLevel)
    {
        synchronized(this) {
            if (_suppressors == null) {
                _suppressors = MapUtil.map();
            }
            LogSuppressor ls = new LogSuppressor(millisecondThreshold,
                suppressToLevel);
            _suppressors.put(id, ls);
        }

        return;
    }

    /**
        Returns the level (severity) for printing stack traces.
        @return Level the level for stack traces
        @aribaapi ariba
    */
    public Level printWhere ()
    {
        return Level.ERROR;
    }

    /**
        Set the locale by which localizable messages should be translated to.

        @param locale locale for message localization
        @aribaapi ariba
    */
    public static void setLocaleToUse (Locale locale)
    {
        localeToUse = locale;
    }

    /**
        Gets the logger instance of the specified name.
        @param name the name of the logger
        @return the logger instance of the specified name
        @see org.apache.log4j.Category#getInstance
        @aribaapi documented
    */
    public static Category getInstance (String name)
    {
        return Logger.getLogger(name, myFactory);
    }

    /**
        Gets the logger instance of the specified name.
        @param name the name of the logger
        @return the logger instance of the specified name
        @see org.apache.log4j.Logger#getLogger
        @aribaapi documented
    */
    public static org.apache.log4j.Logger getLogger (String name)
    {
            // If we're in development mode and this logger is being created,
            // check to make sure it's listed as a public or private logger.
        if (SystemUtil.isDevelopment() && (LogManager.exists(name) == null)) {
            checkRegistration(name, true);
        }
        LoggerFactory factory = PrivateLoggers.getLoggerFactory(name);
        if (factory == null) {
            factory = myFactory;
        }
        return Logger.getLogger(name, factory);
    }

    /**
        See if a logger name is registered as public or private. If neither,
        log a warning.
        This is API private--the method is only public because it is called
        from the unit tests. Clients should not need access to this.
        @param loggerName the name to check
        @aribaapi private
    */
    public static void checkRegistration (String loggerName, boolean exitOnError)
    {
            // Test for description if the logger isn't listed as private and
            // if we are currently checking.
        if (!suspendChecking && !PrivateLoggers.contains(loggerName)) {
            checkPublic(loggerName, exitOnError);
        }
    }

    /**
        Static members to track unregistered loggers that are detected
        before logging is set up enough to log them.
    */
    private static boolean doneEarlyLogging = false;
    private static boolean suspendChecking = false;
    private static List earlyLoggers = ListUtil.list();

    /**
        Return true if logger registration checking ia off.
        This is not normally of interest to clients of Logger,
        but (for instance) TableEdit has to suspend checking.
        @return true if logger registration is off
        @see org.apache.log4j.Category#getInstance
        @aribaapi ariba
    */
    public static boolean isSuspendCheckingOn ()
    {
        return suspendChecking;
    }

    /**
        Turn logger registration checking on or off. This should not normally
        be used, but TableEdit has to suspend checking.
        @param doSuspend true to stop checking for registration
        @see org.apache.log4j.Category#getInstance
        @aribaapi ariba
    */
    public static void setSuspendChecking (boolean doSuspend)
    {
        suspendChecking = doSuspend;
    }

    /**
        See if a logger name is registered as public. If not,
        log a warning then blow a fatal assert.
        @param loggerName the name to check
        @see test.ariba.util.core.LogTest#main
        @aribaapi private
    */
    private static void checkPublic (String loggerName, boolean exitOnError)
    {
        if (!doneEarlyLogging) {
            earlyLoggers.add(loggerName);
        }
        else {
            String desc = description(loggerName,
                ResourceService.getService().getDefaultLocale());

                // if loggerName is not found then loggerName will be returned as
                // the description rather than null
            if (StringUtil.nullOrEmptyString(desc) || loggerName.equals(desc)) {
                String msg =
                    Fmt.S("Logger %s has no description or PrivateLoggers entry.",
                        loggerName);
                Log.util.error(msg);
                Assert.that(!exitOnError, "Unregistered loggers found--see above."); // OK
            }
        }
    }

    /**
        Log warnings about unregistered loggers that were queued up before
        util logging was ready.
        @see Log
        @aribaapi private
    */
    public static void convertEarlyChecks ()
    {
        doneEarlyLogging = true;
        if (earlyLoggers != null) {
            List loggers = earlyLoggers;
            earlyLoggers = null;
            Iterator it = loggers.iterator();
            while (it.hasNext()) {
                String name = (String)it.next();
                checkPublic(name, !it.hasNext());
            }
        }
    }

    /**
        AsyncAppenders are tracked here so they can be polled for error/
        debug messages.
    */
    private static List asyncAppenders = ListUtil.list();

    /**
        Add an AsyncAppender to the list so we can poll it for error/debug
        messages.
        @see LogManager#wrapInAsyncAppender
        @aribaapi private
    */
    static void registerAsyncAppender (AsyncAppender app)
    {
        asyncAppenders.add(app);
    }

    /**
        Remove an AsyncAppender (presumably the appender is going away).
        @see AsyncAppender#close
        @aribaapi private
    */
    static void unregisterAsyncAppender (AsyncAppender app)
    {
        asyncAppenders.remove(app);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.

        @param a1 the object to print
        @aribaapi documented
    */
    public void debug (Object a1)
    {
        debug("%s", a1, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param args an array containing all arguments to the format
               string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control, Object[] args)
    {
        debug(control, Null, Null, Null, Null, Null, Null, args);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control, Object a1)
    {
        debug(control, a1, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param t Throwable for this log message
        @aribaapi documented
    */
    public void debug (String control, Throwable t)
    {
        debug(control, t, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control, Object a1, Object a2)
    {
        debug(control, a1, a2, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @param a3 the third argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control, Object a1, Object a2, Object a3)
    {
        debug(control, a1, a2, a3, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @param a3 the third argument to the format string <b>control</b>
        @param a4 the fourth argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control, Object a1, Object a2, Object a3, Object a4)
    {
        debug(control, a1, a2, a3, a4, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @param a3 the third argument to the format string <b>control</b>
        @param a4 the fourth argument to the format string <b>control</b>
        @param a5 the fifth argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control,
                       Object a1,
                       Object a2,
                       Object a3,
                       Object a4,
                       Object a5)
    {
        debug(control, a1, a2, a3, a4, a5, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @param a3 the third argument to the format string <b>control</b>
        @param a4 the fourth argument to the format string <b>control</b>
        @param a5 the fifth argument to the format string <b>control</b>
        @param a6 the sixth argument to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control,
                       Object a1,
                       Object a2,
                       Object a3,
                       Object a4,
                       Object a5,
                       Object a6)
    {
        debug(control, a1, a2, a3, a4, a5, a6, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param control a String defining the format of the output
        @param a1 the first argument to the format string <b>control</b>
        @param a2 the second argument to the format string <b>control</b>
        @param a3 the third argument to the format string <b>control</b>
        @param a4 the fourth argument to the format string <b>control</b>
        @param a5 the fifth argument to the format string <b>control</b>
        @param a6 the sixth argument to the format string <b>control</b>
        @param more additional arguments to the format string <b>control</b>
        @aribaapi documented
    */
    public void debug (String control,
                       Object a1,
                       Object a2,
                       Object a3,
                       Object a4,
                       Object a5,
                       Object a6,
                       Object[] more)
    {
        if (isEnabledFor(Level.DEBUG)) {
            forcedLog(FQCN,
                      Level.DEBUG,
                      Fmt.S(control, makeList(a1, a2, a3, a4, a5, a6, more).toArray()),
                      null);
        }
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Loggger is enabled for the DEBUG level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param message the log message
        @aribaapi documented
    */
    public void debug (String message)
    {
        if (isEnabledFor(Level.DEBUG)) {
            forcedLog(FQCN,
                      Level.DEBUG,
                      message,
                      null);
        }
    }
    
    /**
     * Print a message to this Logger. This will only print a
     * message if this Logger is enabled for the DEBUG level.
     * The control string works in the same fashion as the <b>Fmt</b> control
     * string.
     * 
     * @param messageID ID of the message that is to be used
     * @param args the first argument to the format string of <b>messageID</b>
     * @aribaapi documented
     */
    public void debug (int messageID, Object... args)
    {
        if (isEnabledFor(Level.DEBUG)) {
            forcedLocalizedLog(FQCN, Level.DEBUG, null, messageID, ListUtil.arrayToList(args));
        }
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @aribaapi documented
    */
    public void info (int messageID)
    {
        info(messageID, Null, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param args an array containing all arguments to the format
               string <b>control</b>
        @aribaapi documented
    */
    public void info (int messageID, Object[] args)
    {
        info(messageID, Null, Null, Null, Null, Null, Null, args);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID, Object a1)
    {
        info(messageID, a1, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID, Object a1, Object a2)
    {
        info(messageID, a1, a2, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID, Object a1, Object a2, Object a3)
    {
        info(messageID, a1, a2, a3, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4)
    {
        info(messageID, a1, a2, a3, a4, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5)
    {
        info(messageID, a1, a2, a3, a4, a5, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5,
                      Object a6)
    {
        info(messageID, a1, a2, a3, a4, a5, a6, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the INFO level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @param more additional arguments to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void info (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5,
                      Object a6,
                      Object[] more)
    {
        if (isEnabledFor(Level.INFO)) {
            forcedLocalizedLog(
                FQCN,
                Level.INFO,
                null,
                messageID,
                makeList(a1, a2, a3, a4, a5, a6, more));
        }
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @aribaapi documented
    */
    public void warning (int messageID)
    {
        warning(messageID, Null, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param args an array containing all arguments to the format
               string <b>control</b>
        @aribaapi documented
    */
    public void warning (int messageID, Object[] args)
    {
        warning(messageID, Null, Null, Null, Null, Null, Null, args);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID, Object a1)
    {
        warning(messageID, a1, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID, Object a1, Object a2)
    {
        warning(messageID, a1, a2, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.
        The control string works in the same fashion as the <b>Fmt</b> control string.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID, Object a1, Object a2, Object a3)
    {
        warning(messageID, a1, a2, a3, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4)
    {
        warning(messageID, a1, a2, a3, a4, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5)
    {
        warning(messageID, a1, a2, a3, a4, a5, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5,
                      Object a6)
    {
        warning(messageID, a1, a2, a3, a4, a5, a6, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the WARN level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @param more additional arguments to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void warning (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5,
                      Object a6,
                      Object[] more)
    {
        if (isEnabledFor(Level.WARN)) {
            forcedLocalizedLog(FQCN,
                Level.WARN,
                null,
                messageID,
                makeList(a1, a2, a3, a4, a5, a6, more));
        }
    }


    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @aribaapi documented
    */
    public void error (int messageID)
    {
        error(messageID, Null, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param args an array containing all arguments to the format
               string <b>control</b>
        @aribaapi documented
    */
    public void error (int messageID, Object[] args)
    {
        error(messageID, Null, Null, Null, Null, Null, Null, args);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID, Object a1)
    {
        error(messageID, a1, Null, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID, Object a1, Object a2)
    {
        error(messageID, a1, a2, Null, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID, Object a1, Object a2, Object a3)
    {
        error(messageID, a1, a2, a3, Null, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4)
    {
        error(messageID, a1, a2, a3, a4, Null, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5)
    {
        error(messageID, a1, a2, a3, a4, a5, Null, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID,
                       Object a1,
                       Object a2,
                       Object a3,
                       Object a4,
                       Object a5,
                       Object a6)
    {
        error(messageID, a1, a2, a3, a4, a5, a6, null);
    }

    /**
        Print a message to this Logger. This will only print a
        message if this Logger is enabled for the ERROR level.

        @param messageID ID of the message that is to be used
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        @param more additional arguments to the format string of <b>messageID</b>
        @aribaapi documented
    */
    public void error (int messageID,
                      Object a1,
                      Object a2,
                      Object a3,
                      Object a4,
                      Object a5,
                      Object a6,
                      Object[] more)
    {
        if (isEnabledFor(Level.ERROR)) {
            forcedLocalizedLog(FQCN,
                               Level.ERROR,
                               new Exception("Stack trace"),
                               messageID,
                               makeList(a1, a2, a3, a4, a5, a6, more));
        }
    }

    /**
        The debugging property defines these values:
            Debug (output logging framework debug info)
            DebugWhere (sometimes adds a stack trace)
        @aribaapi private
        @see #Debug
        @see #DebugWhere
    */
    private static final String LoggingDebugPropertyName = "ariba.logging.debug";

    /**
        Property value to output logging framework debug info.
        @aribaapi private
        @see #LoggingDebugPropertyName
    */
    private static final String Debug = "Debug";

    /**
        Property value to output logging framework debug info including
        a stack trace.
        @aribaapi private
        @see #LoggingDebugPropertyName
    */
    private static final String DebugWhere = "DebugWhere";

    /**
        Each line of logging debugging output is prefixed with this string.
        @aribaapi private
    */
    private static final String DebugPrefix = "Util logging: ";


    /**
        Has debugging mode been turned on for logging?
        @return boolean true if debugging is enabled
        @aribaapi private
        @see #getDebugProperty()
    */
    static boolean isDebugging ()
    {
        return isDebugging(getDebugProperty());
    }


    /**
        Does this property value indicate debugging is on for logging?
        @param property will be one of the debug values or the empty string
        @return boolean true if debugging is enabled by property
        @aribaapi private
        @see #Debug
        @see #DebugWhere
    */
    static boolean isDebugging (String property)
    {
        return property.equalsIgnoreCase(Debug) ||
            property.equalsIgnoreCase(DebugWhere);
    }

    /**
        Does this property value indicate debugging with stack trace is on
        for logging?
        @param property will be the DebugWhere value or the empty string
        @return boolean true if debugging with trace is enabled by property
        @aribaapi private
        @see #DebugWhere
    */
    static boolean isDebugWhere (String property)
    {
        return property.equalsIgnoreCase(DebugWhere);
    }

    /**
        Retrieve the logging property used to indicate a debugging state.
        @return String one of the debug values or the empty string
        @aribaapi private
        @see #LoggingDebugPropertyName
        @see #Debug
        @see #DebugWhere
    */
    static String getDebugProperty ()
    {
        String p = System.getProperty(LoggingDebugPropertyName);
        if (p == null) {
            p = System.getProperty(LogLog.DEBUG_KEY, "");
                // if log4j debugging is on, we'll turn ours on, too
            if (p.length() > 0) {
                if (OptionConverter.toBoolean(p, true)) {
                    p = Debug;
                }
            }
        }
        return p;
    }

    /**
        If debugging is enabled, output the object using the formatting string,
        according to the Fmt class's conventions.
        @param format the control string
        @param o object to output
        @aribaapi private
        @see ariba.util.core.Fmt#S(String, Object)
        @see #debugSTrace(String, Object, Object)
    */
    static void debugS (String format, Object o)
    {
        debugS(format, o, null, null);
    }

    /**
        If debugging is enabled, output the objects using the formatting string,
        according to the Fmt class's conventions.
        @param format the control string
        @param o1 object to output
        @param o2 object to output
        @aribaapi private
        @see ariba.util.core.Fmt#S(String, Object, Object)
        @see #debugSTrace(String, Object, Object)
    */
    static void debugS (String format, Object o1, Object o2)
    {
        debugS(format, o1, o2, null);
    }

    /**
        If debugging is enabled, output the objects using the formatting string,
        according to the Fmt class's conventions.
        @param format the control string
        @param o1 object to output
        @param o2 object to output
        @param o3 object to output
        @aribaapi private
        @see ariba.util.core.Fmt#S(String, Object, Object)
        @see #debugSTrace(String, Object, Object)
    */
    static void debugS (String format, Object o1, Object o2, Object o3)
    {
        debugS(format, o1, o2, o3, true);
    }

    /**
        If debugging is enabled, output the objects using the formatting string,
        according to the Fmt class's conventions. Check for debugging properties
        only when check is true.
        @param format the control string
        @param o1 object to output
        @param o2 object to output
        @param o3 object to output
        @param check if true check the debugging properties, otherwise assume
        debugging is already enabled
        @aribaapi private
        @see ariba.util.core.Fmt#S(String, Object, Object)
        @see #debugSTrace(String, Object, Object)
    */
    static void debugS (String format, Object o1, Object o2, Object o3,
                              boolean check)
    {
        if (!check || isDebugging()) {
            SystemUtil.out().println(Fmt.S(StringUtil.strcat("%s", format),
                DebugPrefix, o1, o2, o3));
        }
    }

    /**
        If debugging is enabled, output the objects using the formatting string,
        according to the Fmt class's conventions. If the debugging mode is
        DebugWhere then also output a stack trace.
        @param format the control string
        @param o1 object to output
        @param o2 object to output
        @aribaapi private
        @see ariba.util.core.Fmt#S(String, Object, Object)
        @see #debugSTrace(String, Object, Object)
    */
    static void debugSTrace (String format, Object o1, Object o2)
    {
        String p = getDebugProperty();
        if (isDebugging(p)) {
            boolean where = isDebugWhere(p);
            debugS(StringUtil.strcat(format, where ? "(stack follows)" : ""),
                o1, o2, null, false);
            if (where) {
                SystemUtil.out().println(SystemUtil.stackTrace());
            }
        }
    }
}
