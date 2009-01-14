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

    $Id: //ariba/platform/util/core/ariba/util/log/LoggingEvent.java#5 $
*/

package ariba.util.log;

import ariba.util.core.ThreadDebugState;
import org.apache.log4j.Priority;
import org.apache.log4j.Category;

/**
    The internal representation of logging events. When an affirmative
    decision is made to log then a LoggingEvent instance is created. This
    instance is passed around to the apenders we registered. <p>

    This class also adds support for custom tag and localized messages. <p>

    @aribaapi ariba
*/
public class LoggingEvent extends org.apache.log4j.spi.LoggingEvent
{
    /**
        The custom tag implementation
    */
    private static CustomTag customTagImpl = null;
    /**
        The custom tag.
    */
    private String customTag = null;

    /**
        The message Id for localized messages.
    */
    private String messageId;

        //we should change this to use log4j's MDC in the future
    private String callerThreadDebugState;
    
    /**
        Instantiates an instance of this class.
        @param fqnOfCategoryClass the fully qualified class name of the logging category
        @param logger the logger that logs this event
        @param timeStamp The time in millis since the Java epoch that this
        @param priority the prority of the event
        @param messageId the message id for localization
        @param message the log message
        @param throwable The stackTrace associated with this event if
        any. May be null if no stack trace generated.
        @aribaapi ariba
    */
    public LoggingEvent (String fqnOfCategoryClass,
                         Category logger,
                         long timeStamp,
                         Priority priority,
                         String messageId,
                         Object message,
                         Throwable throwable)
    {
        super(fqnOfCategoryClass,
              logger,
              timeStamp,
              priority,
              message,
              throwable);
        this.messageId = messageId;

        createCustomTag();

        if (throwable != null) {
            callerThreadDebugState = ThreadDebugState.makeString();
        }
    }
    
    /**
        Instantiates an instance of this class.
        @param fqnOfCategoryClass the fully qualified class name of the logging category
        @param logger the logger that logs this event
        @param priority the prority of the event
        @param messageId the message id for localization
        @param message the log message
        @param throwable The stackTrace associated with this event if
        any. May be null if no stack trace generated.
        @aribaapi ariba
    */
    public LoggingEvent (String fqnOfCategoryClass,
                         Category logger,
                         Priority priority,
                         String messageId,
                         Object message,
                         Throwable throwable)
    {
        super(fqnOfCategoryClass, logger, priority, message, throwable);
        this.messageId = messageId;
        createCustomTag();

        if (throwable != null) {
            callerThreadDebugState = ThreadDebugState.makeString();
        }
    }

    /**
        Creates the custom tag.
    */
    private void createCustomTag ()
    {
        customTag = (customTagImpl == null) ? null : customTagImpl.getTag();
    }

    public static void setCustomTagImpl (CustomTag impl)
    {
        customTagImpl = impl;
    }

    public static CustomTag getCustomTagImpl ()
    {
        return customTagImpl;
    }
    
    public String getCustomTag ()
    {

        return customTag;
    }

    public void setMessageId (String messageId)
    {
        this.messageId = messageId;
    }

    public String getMessageId ()
    {
        return messageId;
    }

    public String getCallerThreadDebugState ()
    {
        return callerThreadDebugState;
    }
}
