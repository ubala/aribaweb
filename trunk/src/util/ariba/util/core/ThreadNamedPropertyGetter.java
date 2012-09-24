/*
    Copyright (c) 1996-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadNamedPropertyGetter.java#1 $

    Responsible: zliu
*/

package ariba.util.core;

/**
 * use this interface to get named property per thread
 */
public interface ThreadNamedPropertyGetter
{
    /**
     * @param name
     * @return the property found on thread, if not found return null
     */
    public Object getThreadProperty (String name);
}
