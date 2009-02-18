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

    $Id: //ariba/platform/util/core/ariba/util/log/AuditLoggerMessage.java#6 $
*/

package ariba.util.log;
import java.util.List;

/**
    ToDo: javadoc
    
    @aribaapi ariba
*/
public interface AuditLoggerMessage
{
    /**
        The various WithId methods in {@link ariba.base.audit.AuditLogger} return this
        if the logging operation failed.
        @aribaapi ariba
    */
    public static final long InvalidNameLongValue = -1;
    /**
        The various WithId methods in {@link ariba.base.audit.AuditLogger} return this
        if the underlying logger is not enabled, essentially this means a logical no-op
        has been performed.

        @aribaapi ariba
    */
    public static final long IgnoreNameLongValue = -2;

    /**
        Convenient method to obtain a string from the specified object.
        @param arg the specified object
        @return the string representing the object.
    */
    public String getStringFromArg (Object arg);

    /**
        set the Id field. This setter method is provided because the Id
        which is unique may not be known at the time of instantiation of
        this class. The inteneded use is that this method be called once
        in one thread (where the instance is created). Because of this, this
        is not synchronized
        @param value the value to set
    */
    public void setId (long value);

    public long getId ();

    public Object getRealUserId ();

    public Object getEffectiveUserId ();

    public String getNodeName ();
    
    public String getIPAddress ();
    
    public int getRealmId ();

    public void setContextObject (Object contextObject);
        
    public Object getContextObject ();

    /**
        The (log4j) level of the message.
    */
    public int getLevel ();

    public void setMessageId (int messageId);

    public int getMessageId ();

    public void setResourceName (String name);

    public String getResourceName ();

    public void setArgs (List args);

    public List getArgs ();

    public int getNumArgs ();

    public void setLevelAndThrowable (int level, Throwable t);
    
    public Throwable getThrowable ();    

    public String getDescription ();
}
