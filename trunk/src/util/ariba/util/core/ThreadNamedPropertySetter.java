/*
    Copyright (c) 1996-2012 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/ThreadNamedPropertySetter.java#1 $

    Responsible: zliu
*/

package ariba.util.core;

/**
 * use this interface to set named property per thread
 */
public interface ThreadNamedPropertySetter
{
    /**
     * @param name
     * @param property
     * @return  true for success, otherwise false
     */
    public boolean setThreadProperty (String name, Object property);
}
