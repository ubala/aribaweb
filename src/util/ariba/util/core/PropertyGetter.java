/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/PropertyGetter.java#1 $

    Responsible: ckoulman
*/


package ariba.util.core;

public abstract class PropertyGetter<T,P>
{

    /**
     * Return a property, say a language, locale or ANID given a T (which might be a AWSession, etc.)
     * @param t of type T
     * @return
     */
    abstract public P getProperty(T t);
}
