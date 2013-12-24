/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/Feature.java#1 $

    Responsible: ckoulman
*/


package ariba.util.core;

/**
 * Represents a feature or capability in the system and can answer the
 * question of whether the feature is enabled in an instance of a given
 * context, which is specified by the generic T parameter.
 */
public class Feature<T>
{
    protected final String _featureName;
    protected final Predicate<T> _predicate;

    public Feature(String featureName, Predicate<T> predicate)
    {
        _featureName = featureName;
        _predicate = predicate;
    }

    /**
     * Return name of feature
     * @return
     */
    public final String featureName()
    {
        return _featureName;
    }

    /**
     * Use information from T and the implementation of the
     * predicate to calculate whether the feature is enabled.
     * @param t
     *
     * @return  boolean true if feature is enabled
     */
    public boolean isEnabled(T t)
    {
        return _predicate.evaluate(t);
    }

    /**
     * The predicate to evaluate to determine if the feature is enabled.
     *
     * @return  the predicate
     */
    public Predicate<T> getPredicate()
    {
        return _predicate;
    }

}
