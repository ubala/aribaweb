/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/NotPredicate.java#1 $

    Responsible: ckoulman
*/


package ariba.util.core;

/**
 * Implements a boolean not of the contained predicate.
 */
public class NotPredicate<T> extends Predicate<T>
{
    protected Predicate<T> _predicate;

    /**
     * For NotPredicate to be true, the contained predicate must be false.
     *
     * @param t of type T
     * @return
     */
    @Override
    public boolean evaluate(T t)
    {
        return !_predicate.evaluate(t);
    }

    public NotPredicate(Predicate<T> predicate)
    {
        _predicate = predicate;
    }
}
