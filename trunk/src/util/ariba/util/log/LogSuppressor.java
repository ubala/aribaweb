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

    $Id: //ariba/platform/util/core/ariba/util/log/LogSuppressor.java#2 $
*/

package ariba.util.log;

import org.apache.log4j.Priority;

/**
 * A LogSuppressor will downgrade the level of a log message for all occurrences
 * after the first, for some period of time.  The effect is to quiet down messages
 * so that readers of the log aren't flooded with repeated high-level log message.
 * @aribaapi private
*/
public class LogSuppressor
{
    // time of the first event time
    private long _firstTime;
    // amount of time that we suppress
    private long _suppressTime;
    // level that we suppress the message to
    private Priority _suppressToLevel;

    /**
     * Create a LogSuppressor for a particular log message.
     * @param suppressTime - time, in milliseconds, that we suppress a log message after
     * an occurence.
     * @param suppressLevel - the level we suppress the message to
     * @aribaapi private
     */
    public LogSuppressor (long suppressTime, Priority suppressLevel)
    {
        _suppressTime = suppressTime;
        _suppressToLevel = suppressLevel;
        _firstTime = 0;
    }

    /**
     * Possibly change the level of a log event.
     * This message is called on each log event for the given message.  It will
     * return the level to use, either the original (if not suppressed) or
     * the suppression level.
     * @param eventLevel - the event about to be logged.
     * @return the level that should be used for the event
     * @aribaapi private
     */ 
    public synchronized Priority possiblySuppress (Priority eventLevel)
    {
        Priority levelToUse = eventLevel;

        long now = System.currentTimeMillis();
        if (now > _firstTime + _suppressTime) {
            // we've exceeded our time bucket.  Use the event as is and restart clock.
            _firstTime = now;
        }
        else {
            // we're within the suppression time, downgrade the event
            // it would be better to clone the event, but we don't have enough
            // information to do that, so use the deprecated public level
            // method to just switch the event's level
            levelToUse = _suppressToLevel;
        }

        return levelToUse;
    }
}
