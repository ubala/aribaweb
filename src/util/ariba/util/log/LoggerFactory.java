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

    $Id: //ariba/platform/util/core/ariba/util/log/LoggerFactory.java#5 $
*/

package ariba.util.log;

/**
    The simple extension of log4j's LoggerFactory class so that we can
    create our own ariba.util.log.Logger instead of log4j's Logger insances.

    @aribaapi private
*/
public class LoggerFactory implements org.apache.log4j.spi.LoggerFactory
{
    /**
        Instantiate an instance of this class.
        @aribaapi private
    */
    public LoggerFactory ()
    {
    }

    /**
        Create new {@link ariba.util.log.Logger} instances with the specified name
        @param name the name of the logger, must not be null.
        @return the new {@link ariba.util.log.Logger instance}
        @aribaapi private
    */
    public org.apache.log4j.Logger makeNewLoggerInstance (String name)
    {
        return new ariba.util.log.Logger(name);
    }

}
