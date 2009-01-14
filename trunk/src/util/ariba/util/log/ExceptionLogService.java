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

    $Id: //ariba/platform/util/core/ariba/util/log/ExceptionLogService.java#4 $
*/

package ariba.util.log;

import ariba.util.core.ThreadDebugKey;
import org.apache.log4j.Priority;

/**
    Service to trap and log exceptions that would otherwise go unhandled
    
    @aribaapi ariba
*/
public abstract class ExceptionLogService
{
    /**
        Allows the caller to set a context object to be associated to exceptions
        that may occur while handling that object. The value may be a <code>ClusterRoot</code>
        or a <code>BaseId</code>. Note that care needs to be taken when using this
        paradigm, as lots of background processing may happen in new threads.
    
        @aribaapi ariba 
    */
    public static final ThreadDebugKey ExceptionContextObjectKey = 
        new ThreadDebugKey("ExceptionContextObject");

    private static ExceptionLogService _service;

    /**
        Log an exception. Note that this method should only be called for
        unexpected exceptions. This writes to the database, so the caller should
        be aware that frequent calls to this method are costly.
     
        If a context object was set for the key <code>ExceptionContextObjectKey</code>
        in the <code>ThreadDebugState</code> the exception will be associated to that
        object. 
    
        @aribaapi ariba
    
        @param  exception - Exception to be logged.
        @return a long integer specifying the success or failure of the logging.
    */
    abstract public long onException (Throwable exception);

    /**
        Log an exception. Note that this method should only be called for
        unexpected exceptions. This writes to the database, so the caller should
        be aware that frequent calls to this method are costly.
        
        If a no context object is passed, but a
        context object was set for the key <code>ExceptionContextObjectKey</code>
        in the <code>ThreadDebugState</code> the exception will be associated to that
        object, else it will be associated to the context object passed in.
    
        @aribaapi ariba
    
        @param  contextObjectId - a <code>BaseId</code> or <code>ClusterRoot</code>,
                                  if a context for this exception is available. Can be
                                  null.                                  
        @param  exception - Exception to be logged
        @return a long integer specifying the success or failure of the logging.
    */
    abstract public long onException (Object contextObjectId, Throwable exception);

    /**
        Log an exception. Note that this method should only be called for
        unexpected exceptions. This writes to the database, so the caller should
        be aware that frequent calls to this method are costly.
            
        If a no context object is passed, but a
        context object was set for the key <code>ExceptionContextObjectKey</code>
        in the <code>ThreadDebugState</code> the exception will be associated to that
        object, else it will be associated to the context object passed in.
    
        @aribaapi ariba
    
        @param  level           - log level (INFO, WARN or ERROR)
        @param  contextObjectId - a <code>BaseId</code> or <code>ClusterRoot</code>,
                               if a context for this exception is available. Can be
                               null.
        @param  exception       - Exception to be logged
        @param  message         - custom message text
        @return a long integer specifying the success or failure of the logging.
    */
    abstract public long onException (Priority level,
                                      Object contextObjectId, 
                                      Throwable exception, 
                                      String message);

    public static void setService (ExceptionLogService service)
    {
        _service = service;
    }

    public static ExceptionLogService getService ()
    {
        return _service;
    }
}
