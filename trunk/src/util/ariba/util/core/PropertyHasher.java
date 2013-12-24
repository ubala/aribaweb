/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/PropertyHasher.java#1 $

    Responsible: ckoulman
*/


package ariba.util.core;

public abstract class PropertyHasher<T>
{

    /**
     * Return a hash on a property, say an ANID, given a T (which might be a session, etc)
     * @param t of type T
     * @return
     */
    abstract public int getPropertyHash(T t);
}

