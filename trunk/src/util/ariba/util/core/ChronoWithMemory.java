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

    $Id: //ariba/platform/util/core/ariba/util/core/ChronoWithMemory.java#4 $
*/

package ariba.util.core;

import ariba.util.log.Logger;

/**
    Chrono subclass which displayed more information when being printed.
    @aribaapi ariba
*/
public class ChronoWithMemory extends Chrono
{
    private long freeMemoryAtStart;
    private long freeMemoryAtStop;
    private long consumedMemory;

    private long trialConsumedMemory;
    private long trialMemoryAtStart;
    private long trialMemoryAtStop;
    
    public ChronoWithMemory (String name)
    {
        super(name);
    }

    public ChronoWithMemory (String name, Logger logger)
    {
        super(name, logger);
    }

    /**
        Creates a new instance of a <code>Chrono</code> timer.
        @param name the name of the event.  Used to print out the timing summary.
        @param logger the Logger to use to print debug statements
        @param startInfoId id of a resource for logging an INFO start message to logger.
        @param stopInfoId id of a resource for logging an INFO stop message to logger.
    */
    public ChronoWithMemory (String name, Logger logger, 
                             int startInfoId, int stopInfoId)
    {
        super(name, logger, startInfoId, stopInfoId);
    }

    public void reset ()
    {
        super.reset();
        freeMemoryAtStart = 0;
        freeMemoryAtStop = 0;
        consumedMemory = 0;
    }

    public void start ()
    {
        super.start();
        if (enabled) {
            trialMemoryAtStart = Runtime.getRuntime().freeMemory();
            if (freeMemoryAtStart == 0) {
                freeMemoryAtStart = trialMemoryAtStart;
            }
        }
    }

    public void stop ()
    {
        if (enabled) {
            trialMemoryAtStop = Runtime.getRuntime().freeMemory(); 
            freeMemoryAtStop = trialMemoryAtStop;
            trialConsumedMemory = freeMemoryAtStart - freeMemoryAtStop;
            if (trialConsumedMemory < 0) {
                // this could happen if the GC makes it hard to figure out the used memory
                trialConsumedMemory = 0;
            }
            consumedMemory += trialConsumedMemory;
        }
        super.stop();
    }

    protected String getExtraInfo (boolean latestTrial)
    {
        if (latestTrial) {
            return Fmt.S("%s - Mem. in KB: %s->%s (%s)",
                         super.getExtraInfo(true),
                         Constants.getLong(trialMemoryAtStart / 1024),
                         Constants.getLong(trialMemoryAtStop / 1024),
                         Constants.getLong(trialConsumedMemory / 1024));
        }
        else {
            return Fmt.S("%s - Mem. in KB: %s->%s (%s)",
                         super.getExtraInfo(false),
                         Constants.getLong(freeMemoryAtStart / 1024),
                         Constants.getLong(freeMemoryAtStop / 1024),
                         Constants.getLong(consumedMemory / 1024));
        }
    }
    
    
}
