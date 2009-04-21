/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.
    
    $Id: //ariba/platform/util/core/ariba/util/core/OptimizingGridtable.java#1 $
    
    Responsible: bjegerlehner
*/
package ariba.util.core;

import java.util.Map;

/**
 @aribaapi */

public class OptimizingGridtable
        extends Gridtable
{
    protected Map makeMap ()
    {
        return MemoryOptimizedMap.getOptimizedMap(1);
    }

    protected Map optimize (Map m)
    {
        return MemoryOptimizedMap.optimize(m);
    }
}
