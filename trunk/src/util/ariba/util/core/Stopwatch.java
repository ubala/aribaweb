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

    $Id: //ariba/platform/util/core/ariba/util/core/Stopwatch.java#5 $
*/
package ariba.util.core;

/**
    @aribaapi private
*/
public interface Stopwatch
{
    /**
        Start the stopwatch.
    */
    public void start ();
    /**
        Stop the stopwatch.
        @return time in microseconds
    */
    public long stop ();

    /**
        @return true if start has been called, and stop has not been called since
    */
    public boolean isRunning ();

    /**
     Current elapsed time of a running timer
     @return time in microseconds
    */
    public long runningTime ();
    
    /**
        @return number of decimal places to store when counting seconds.
    */
    public int resolutionLevel ();
}

