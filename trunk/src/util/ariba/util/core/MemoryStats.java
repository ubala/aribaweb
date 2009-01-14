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

    $Id: //ariba/platform/util/core/ariba/util/core/MemoryStats.java#2 $
*/
package ariba.util.core;

/**
    Simple class that captures the current memory situation.
    @aribaapi ariba
*/
public class MemoryStats
{
    /**
        Converts a long number of bytes to a double rounded to one decimal place.
        @aribaapi ariba
    */
    public static double convertToMB (long bytes)
    {
        double unrounded = ((double)bytes) / (1024 * 1024);
        return ((long)(unrounded * 10)) / 10.0;
    }

    /**
        Returns the current memory stats.
        @aribaapi ariba
    */
    public static MemoryStats getCurrentStats ()
    {
        Runtime runtime = Runtime.getRuntime();
        return new MemoryStats(runtime.maxMemory(),
                               runtime.freeMemory(),
                               runtime.totalMemory());
    }

    /**
        Returns the free memory as a percentage of max memory in the system
        for the specified <code>max</code> and <code>free</code> memory.
        @aribaapi ariba
    */
    public static double getAvailablePercentage (long max, long free, long total)
    {
        long used = total - free;
        long available = max - used;
        return ((long)(1000 * ((double)available / (double)max))) / 10.0;
    }

    /**
        @aribaapi ariba
    */
    public static double getAvailablePercentage ()
    {
        Runtime runtime = Runtime.getRuntime();
        return getAvailablePercentage(runtime.maxMemory(),
                                 runtime.freeMemory(),
                                 runtime.totalMemory());
    }

    //--------------------------------------------------------------------------
    // data members

    private long _max;
    private long _free;
    private long _total;

    //--------------------------------------------------------------------------
    // constructors

    public MemoryStats (long max, long free, long total)
    {
        _max = max;
        _free = free;
        _total = total;
    }

    public long max ()
    {
        return _max;
    }

    public long free ()
    {
        return _free;
    }

    public long total ()
    {
        return _total;
    }

    public long used ()
    {
        return _total - _free;
    }

    public long available ()
    {
        return _max - used();
    }

    public double maxInMB ()
    {
        return convertToMB(_max);
    }

    public double freeInMB ()
    {
        return convertToMB(_free);
    }

    public double usedInMB ()
    {
        return convertToMB(used());
    }

    public double totalInMB ()
    {
        return convertToMB(_total);
    }

    public double availableInMB ()
    {
        return convertToMB(available());
    }

    /**
        @aribaapi ariba
    */
    public double availablePercentage ()
    {
        return getAvailablePercentage(_max, _free, _total);
    }
}
