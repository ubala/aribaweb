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

    $Id: //ariba/platform/util/core/ariba/util/log/AuditLogger.java#7 $
*/

package ariba.util.log;

import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.SystemUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.StringUtil;
import ariba.util.core.Fmt;
import ariba.util.core.Assert;
import java.util.List;
import java.util.Locale;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;

/**
    AuditLogger is a special logger implemented to provide a framework for event
    auditing. One of the major design goals is to make it easy for developers to
    use. Towards this end, the API closely resembles ariba's logging API.
    After all, AuditLogger is actually a subclass of {@link ariba.util.log.Logger}.
    <p>
    Instead of doing something like Log.util.info(messageId, arg1, arg2,...), call
    Log.aribaAudit.auditInfo(messageId, Object, arg1, arg2, ...). Here, aribaAudit
    is a user defined 'audit logger'. The similarity between the 2 calls is evident.
    <p>
    The differences are:
    <ul>
    <li>The string 'audit' is prepended to the info, error, warning  methods:
    Instead of info, warning, error, the method names are auditInfo, auditWarning,
    auditError. Note that there is no support for debug level. This is intentional
    since this is auditing, not debugging.
    </li>
    <li>
    The various audit methods return a unique long int that serves as an Id that specifies
    this audit event, which is persisted to the database by the special
    {@link ariba.app.server.AuditAppender} appender.
    </li>
    <li>
    auditInfo and auditWarning take an additional Object parameter which specifies a context
    object (if any) that can be referenced by this audit event. The auditError method takes
    yet another additional parameter (Throwable) to spefify the stack trace where applicable.
    </li>
    <li>The usual debug, info, warning, error methods are not allowed. For example,
    calling Log.aribaAudit.info(...) results in an error. Use Log.aribaAudit.auditInfo
    instead.
    </li>
    </ul><p>
    <p> The following are the quick and easy steps of using the auditing framework,
    assuming we want to audit events in the app.admin component.
    <ul>
    <li>define the logger aribaAudit.admin by adding the following entry in
    ariba/util/log/PrivateLoggers.csv:<p>
        aribaAudit.admin, ariba.app.audit.AuditLoggerFactory<p>
    This will result in the aribaAudit.admin audit logger created with the above
    logger factory. It is important that the correct logger factory (and no typos please)
    be used.
    </li>
    <li>
    If the aribaAudit logger has not been added in the logging section in
    Parameters.table, add aribaAudit to the System.Logging.AribaAuditingAppender.Categories
    parameter. System.Logging.Categories should have aribaAudit:INFO, add this too if not
    already present. And of course, enable the appender by setting
    System.Logging.AribaAuditingAppender.Disable to false (default is true, we don't audit
    by default). The audit calls will have no effect this Disabled flag is true. One can
    also control the logging by turning on/off the aribaAudit (and its children) loggers,
    in exactly the same way other loggers work.
    </li>
    <li>
         Define this logger is the Log.java in the component in this case app.admin
    <pre>
         import ariba.base.audit.AuditLogger;
         public static final AuditLogger aribaAuditAdmin =
                     (AuditLogger)Logger.getLogger("aribaAudit.admin");
    </pre>

    </li>
    <li>
    Create Log.aribaAudit.admin.csv in the resource directory in the app.admin component as
    usual to specify any messages you want to audit. This csv file has exactly the same
    format as any other csv files used by the logging framework.
    </li>
    <li>
    <pre>audit an event, example:
         long id = Log.aribaAudit.admin.auditInfo(1234, contextObject, "auditing an event");
         if (id >= 0) {
            // audit logging is successful
            ....
         }
         else {
           // audit log is not successful, nothing persisted.
           // the value can be either a defined IgnoreNameLongValue (which basically says the audit logger is not
           // enabled, so nothing is done), or defined InvalidNameLongValue (which means some error happens, may
           // be the transaction commit fails, or some other errors).
           ...
         }
         The above persists an audit event specified by 'id' into a table named "AuditInfo".
         The detailed localized message is looked up from message code 1234 in Log.aribaAudit.admin.csv.
    </pre>
    </ul>
    @aribaapi ariba
*/
public class AuditLogger extends Logger
{

    /**
        The fully qualified name of this class.

        @aribaapi private
    */
    public static String FQCN = AuditLogger.class.getName() + ".";

    /* see javadoc for AuditLoggerMessage.IgnoreNameLongValue */
    private static final long IgnoreNameLongValue =
        AuditLoggerMessage.IgnoreNameLongValue;

    /* see javadoc for AuditLoggerMessage.InvalidNameLongValue */
    private static final long InvalidNameLongValue =
        AuditLoggerMessage.InvalidNameLongValue;

    private AuditLoggerMessageFactory messageFactory;

    /* set of friendly names for displaying categories as event types */
    private static Set categories = SetUtil.set();

    /* categories that we don't want displayed in a pick list--not leaf cats */
    public static final String aribaAuditName = "aribaAudit";
    public static final String aribaAuditCommitNowName = "aribaAuditCommitNow";

    public AuditLogger (String name, AuditLoggerMessageFactory factory)
    {
        super(name);
        this.messageFactory = factory;
        addCategory(name);
    }

        // ToDo: remove?
    protected AuditLogger (String name)
    {
        super(name);
    }

    /**
        Add a category friendly name to the set that tracks them.

        @param name the name of the category (Logger)
        @see #categories
    */
    private void addCategory (String name)
    {
        if (!aribaAuditName.equals(name) &&
            !aribaAuditCommitNowName.equals(name)) {
            synchronized(categories) {
                categories.add(name);
            }
            String display = lookupCategory(name,
                ResourceService.getService().getDefaultLocale());
            Assert.that(!name.equals(display),
                "AuditLogger %s has no entry in %s.",
                name, AuditCategoryTable);
        }
    }

    /**
        Returns the locale used to localize messages. Currently this is
        the system locale.

        @return the locale used to localize messages.
    */
    private Locale getLocaleToLocalizeMessage ()
    {
            // the audit logger will localize messages using the system default
            // locale because the intended audience for the auditing message is
            // the site admin. There is an issue of whether the locale to use
            // should be the system locale or the 'localeToUse' specified in
            // the logging section of Parameters.table
        return ResourceService.getService().getDefaultLocale();
    }

    /**
        Given a locale, use the static resource name and arguments to
        materialize the final message.

     @param args the values to substitute in the message template.
     @param name the string table name (though it may lack the StringTablePrefix).
     @param id the key into the string table to get the template.
     @param locale the locale to use for the parameters and resource.
     @return the localized, completed message
     @aribaapi ariba
    */
    public static String getCompletedMessage (List args,
                                              String name,
                                              String id,
                                              Locale locale)
    {
        String table = StringUtil.strcat(StringTablePrefix, name);
        String result = formatMessage(table, id, args, locale);
        if (id.equals(result)) {
            // if not found, then try the table name without prefixing
            // (for backwards compatibility)
            result = formatMessage(name, id, args, locale);
        }

        return result;
    }

    /**
        Perform a resource lookup on the string table and return the
        fully materialized, formatted result.
    */
    private static String formatMessage (String table,
                                         String id,
                                         List args,
                                         Locale locale)
    {
        if (ListUtil.nullOrEmptyList(args)) {
            return Fmt.Sil(locale, table, id);
        }
        return Fmt.Sil(locale, table, id, args);
    }

    /**
        StringTable for friendly event category names.
    */
    private static final String AuditCategoryTable = "ariba.auditCategoryNames";

    /**
        Must return resource if the category is not found.
    */
    public static String getCategoryName (String resource,
                                          Locale locale)
    {
        String name = lookupCategory(resource, locale);
        // if we get the key back, the lookup failed, so try prepending the
        // StringTablePrefix for backward compatibility
        if (resource.equals(name)) {
            name =
                lookupCategory(StringUtil.strcat(StringTablePrefix, resource),
                    locale);
        }
        return name;
    }

    private static String lookupCategory (String key, Locale locale)
    {
        return ResourceService.getService().getLocalizedString(AuditCategoryTable,
                key,
                locale,
                false);
    }

    public static Iterator getCategories ()
    {
        return categories.iterator();
    }

    /**
        Creates an instance of {@link AuditLoggerMessage} to be passed
        to the appender.
        @param contextObject the base Id that specifies any cluster root that is
        referenced by this message, can be <code>null</code>
        @param level the log4j level for the message
        @param t the throwable, if any. Can be <code>null</code>
        @param messageId the messageId that provides information to
        localize a give message for a given log csv file.
        @param args the various arguments for the audit log. Can
        be null or empty, but the size is limited to the value of
        {@link #ariba.base.audit.AuditLoggerMessage.MAX_NUM_ARGS}
        @return an instance of {@link AuditLoggerMessage}.
    */
    protected AuditLoggerMessage createAuditLoggerMessage
        (Object contextObject,
         Priority level,
         Throwable t,
         int messageId,
         List args)
    {
        AuditLoggerMessage alm = messageFactory.createAuditLoggerMessage();
        alm.setLevelAndThrowable(level.toInt(), t);
        alm.setMessageId(messageId);
        alm.setResourceName(removeLogPrefix(stringTable));
        alm.setContextObject(contextObject);
        alm.setArgs(args);
        return alm;
    }

    /**
        Remove the standard string table prefix, if present, and return
        the result.
        @param s the string to remove the prefix from
        @return the string passed in but with the prefix removed
    */
    private String removeLogPrefix (String s)
    {
        FastStringBuffer buf = new FastStringBuffer(s);
        if (buf.startsWith(StringTablePrefix)) {
            return buf.substring(StringTablePrefix.length(), s.length());
        }

        return s;
    }

    public void setLevel (Level level)
    {
            // setting to DEBUG is not allowed for the audit logger
        if (level == Level.DEBUG) {
            Log.util.warning(8786, getName());
            level = Level.INFO;
        }
        super.setLevel(level);
    }

    protected boolean useWarningLogHandlers ()
    {
        return false;
    }

    /*
        Overrides this method to disallow debug calls.
    */
    protected void forcedLog (String fqcn,
                              Priority level,
                              Object message,
                              Throwable t)
    {
            // debug level audit logging is not allowed
            // use util logger for lack of better choice
        Log.util.error(8777, "Debug", message);
    }

    /*
        Overrides this method so that all normal info, warning, error methods
        which are not **directly** implemented by this class result in an
        error.
    */
    protected void forcedLocalizedLog (String fqcn,
                                       Priority level,
                                       Throwable t,
                                       Object message,
                                       int messageId)
    {
            // use util logger for lack of better choice
        Log.util.error(8777, level, message);
    }

    /**
        Audit an info message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject)
    {
        return auditInfo(messageID, contextObject, null);
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject, Object a1)
    {
        return auditInfo(messageID, contextObject,
                         ListUtil.list(a1));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject, Object a1, Object a2)
    {
        return auditInfo(messageID, contextObject, ListUtil.list(a1, a2));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject,
                           Object a1, Object a2, Object a3)
    {
        return auditInfo(messageID, contextObject, ListUtil.list(a1, a2, a3));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           Object a2,
                           Object a3,
                           Object a4)
    {
        return auditInfo(messageID, contextObject, ListUtil.list(a1, a2, a3, a4));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           Object a2,
                           Object a3,
                           Object a4,
                           Object a5)
    {
        return auditInfo(messageID, contextObject, ListUtil.list(a1, a2, a3, a4, a5));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           Object a2,
                           Object a3,
                           Object a4,
                           Object a5,
                           Object a6)
    {
        return auditInfo(messageID, contextObject,
                         ListUtil.list(a1, a2, a3, a4, a5, a6));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param more addtional arguments
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           Object a2,
                           Object a3,
                           Object a4,
                           Object a5,
                           Object a6,
                           Object[] more)
    {
        return auditInfo(messageID, contextObject,
                         makeList(a1, a2, a3, a4, a5, a6, more));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param args the argument list
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject, List args)
    {
        return logMessage(messageID, contextObject, Level.INFO, null, args);
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditInfo (int messageID, Object contextObject, int a1)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(Constants.getInteger(a1)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           int a1,
                           int a2)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(Constants.getInteger(a1),
                                           Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           int a1,
                           Object a2)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(Constants.getInteger(a1),
                                           a2));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           int a1,
                           int a2,
                           int a3)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(Constants.getInteger(a1),
                                           Constants.getInteger(a2),
                                           Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }


    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           int a2)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(a1,
                                           Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           int a2,
                           Object a3)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(a1,
                                           Constants.getInteger(a2),
                                           a3));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditInfo (int messageID,
                           Object contextObject,
                           Object a1,
                           Object a2,
                           int a3)
    {
        if (isEnabledFor(Level.INFO)) {
            return auditInfo(messageID, contextObject,
                             ListUtil.list(a1, a2, Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;

    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject)
    {
        return auditWarning(messageID, contextObject, null);
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject, Object a1)
    {
        return auditWarning(messageID, contextObject, ListUtil.list(a1));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject, Object a1, Object a2)
    {
        return auditWarning(messageID, contextObject, ListUtil.list(a1, a2));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject,
                              Object a1, Object a2, Object a3)
    {
        return auditWarning(messageID, contextObject, ListUtil.list(a1, a2, a3));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              Object a2,
                              Object a3,
                              Object a4)
    {
        return auditWarning(messageID, contextObject, ListUtil.list(a1, a2, a3, a4));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              Object a2,
                              Object a3,
                              Object a4,
                              Object a5)
    {
        return auditWarning(messageID, contextObject, ListUtil.list(a1, a2, a3, a4, a5));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              Object a2,
                              Object a3,
                              Object a4,
                              Object a5,
                              Object a6)
    {
        return auditWarning(messageID, contextObject,
                            ListUtil.list(a1, a2, a3, a4, a5, a6));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param more addtional arguments
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              Object a2,
                              Object a3,
                              Object a4,
                              Object a5,
                              Object a6,
                              Object[] more)
    {
        return auditWarning(messageID, contextObject,
                            makeList(a1, a2, a3, a4, a5, a6, more));
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t an associated throwable
        @param args the argument list
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject, Throwable t, List args)
    {
        return logMessage(messageID, contextObject, Level.WARN, t, args);
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param args the argument list
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID, Object contextObject, List args)
    {
        return logMessage(messageID, contextObject, Level.WARN, null, args);
    }
    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              int a1)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(Constants.getInteger(a1)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              int a1,
                              int a2)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(Constants.getInteger(a1),
                                              Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              int a2)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(a1,
                                              Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message
        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              int a1,
                              Object a2)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(Constants.getInteger(a1),
                                              a2));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              int a1,
                              int a2,
                              int a3)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(Constants.getInteger(a1),
                                              Constants.getInteger(a2),
                                              Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              Object a2,
                              int a3)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(a1,
                                              a2,
                                              Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an warning message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditWarning (int messageID,
                              Object contextObject,
                              Object a1,
                              int a2,
                              int a3)
    {
        if (isEnabledFor(Level.WARN)) {
            return auditWarning(messageID, contextObject,
                                ListUtil.list(a1,
                                              Constants.getInteger(a2),
                                              Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID, Object contextObject, Throwable t)
    {
        return auditError(messageID, contextObject, t, null);
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID, Object contextObject, Throwable t, Object a1)
    {
        return auditError(messageID, contextObject, t, ListUtil.list(a1));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.

        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba

    */
    public long auditError (int messageID, Object contextObject, Throwable t,
                            Object a1, Object a2)
    {
        return auditError(messageID, contextObject, t, ListUtil.list(a1, a2));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID, Object contextObject, Throwable t,
                            Object a1, Object a2, Object a3)
    {
        return auditError(messageID, contextObject, t, ListUtil.list(a1, a2, a3));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            Object a2,
                            Object a3,
                            Object a4)
    {
        return auditError(messageID, contextObject, t, ListUtil.list(a1, a2, a3, a4));
    }

    /**

        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b
>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            Object a2,
                            Object a3,
                            Object a4,
                            Object a5)
    {
        return auditError(messageID, contextObject, t, ListUtil.list(a1, a2, a3, a4, a5));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a5 the fifth argument to the format string of <b>messageID</b>
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            Object a2,
                            Object a3,
                            Object a4,
                            Object a5,
                            Object a6)
    {
        return auditError(messageID, contextObject, t,
                          ListUtil.list(a1, a2, a3, a4, a5, a6));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba

    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            int a1)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(Constants.getInteger(a1)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            int a2)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID,
                              contextObject, t,
                              ListUtil.list(a1,
                                            Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba

    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            int a2,
                            int a3)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(a1,
                                            Constants.getInteger(a3),
                                            Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            int a1,
                            Object a2)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(Constants.getInteger(a1),
                                            a2));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba

    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            int a1,
                            int a2)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(Constants.getInteger(a1),
                                            Constants.getInteger(a2)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba

    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            int a1,
                            int a2,
                            int a3)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(Constants.getInteger(a1),
                                            Constants.getInteger(a2),
                                            Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>
        @param a3 the third argument to the format string of <b>messageID</b>
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            Object a2,
                            int a3)
    {
        if (isEnabledFor(Level.ERROR)) {
            return auditError(messageID, contextObject, t,
                              ListUtil.list(a1,
                                            a2,
                                            Constants.getInteger(a3)));
        }
        return IgnoreNameLongValue;
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param a1 the first argument to the format string of <b>messageID</b>.
        Can be <b>null</b>.
        @param a2 the second argument to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @param a3 the third argument to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @param a4 the fourth argument to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @param a5 the fifth argument to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @param a6 the sixth argument to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @param more additional arguments to the format string of <b>messageID</b>
        Can be <b>null</b>.
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.

        @aribaapi ariba
    */
    public long auditError (int messageID,
                            Object contextObject,
                            Throwable t,
                            Object a1,
                            Object a2,
                            Object a3,
                            Object a4,
                            Object a5,
                            Object a6,
                            Object[] more)
    {
        return auditError(messageID, contextObject, t,
                          makeList(a1, a2, a3, a4, a5, a6, more));
    }

    /**
        Audit an error message

        @param messageID id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param t the throwable, if any. Can be <code>null</code>
        @param args a list of arguments to provide the details of the message.
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
        @aribaapi ariba
    */
    public long auditError (int messageID, Object contextObject, Throwable t, List args)
    {
        return logMessage(messageID, contextObject, Level.ERROR,
                          t == null ? new Exception("Stack trace") : t,
                          args);
    }

    /**
        sends a message to the appender

        @param messageId id of the message used to localize the message.
        @param contextObject the context object, a cluster root (can be null) that
        provides the context related to this auditing event.
        @param level the log4j level for the message
        @param t the throwable, if any. Can be <code>null</code>
        @param args a list of arguments to provide the details of the message.
        @return a unique long integer which if non-negative specifies a unique
        audit log event being successfully audited. Returns #IgnoreNameLongValue
        if the audit log mechanism is not enabled.  Returns #InvalidNameLongValue
        if the audit logging event failed.
    */
    private long logMessage (int messageId,
                             Object contextObject,
                             Level level,
                             Throwable t,
                             List args)
    {
        long id = IgnoreNameLongValue;
        if (isEnabledFor(level)) {
            try {
                id = InvalidNameLongValue;
                AuditLoggerMessage alm =
                    createAuditLoggerMessage(contextObject, level, t, messageId, args);
                super.forcedLocalizedLog(FQCN, level, t, alm, messageId);
                id = alm.getId();
            }
            catch (RuntimeException e) {
                    // something is not right here. Stop the exception from
                    // propagating upwards (logging failure should not crash ou
                    // system. Log an error message.
                Log.util.error(8787,
                               Constants.getInteger(messageId),
                               getName(),
                               SystemUtil.stackTrace(e));
                id = InvalidNameLongValue;
            }
        }
        return id;
    }

}
