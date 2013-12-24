/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/core/FeatureConfig.java#1 $

    Responsible: ckoulman
*/


package ariba.util.core;

import java.util.Set;

/**
 * Different products will fetch configuration in various ways: file, DB, etc.
 * Those products will implement that mechanism.
 *
 * A class implementing this feature should be passed to the feature code at startup.
 */

public abstract class FeatureConfig<T>
{

    /**
     * Return a Set of features names for this feature set.
     * This will each be created from configuration in turn.
     * @return
     */
    public abstract Set<String> getFeatures();

    /**
     * For each feature name, configure a T feature.
     * @return
     */
    public abstract Feature<T> makeFeature(String featureName);

}
