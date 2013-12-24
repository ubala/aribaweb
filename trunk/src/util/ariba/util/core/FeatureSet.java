/*
    Copyright 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/core/FeatureSet.java#1 $
*/


package ariba.util.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * A feature set groups a set of features (Feature<T>) by name.
 * When passed a configuration, the configuration is queried for the name
 * of the set of features, and then each is created in turn.
 *
 * The method isEnabled fetches a feature by name and then dispatched to that feature.
 * @param <T>
 */
public class FeatureSet<T>
{

    // this is the map from feature name to the feature
    private Map<String, Feature<T>> _featureMap;

    // a feature set groups sets of features grouped by name
    private String _featureSetName;

    public FeatureSet(String featureSetName)
    {
        _featureSetName = featureSetName;

        _featureMap = new HashMap<String, Feature<T>>();
    }

    /**
     * The config adaptor provides a set of feature names it can configure, and
     * provides the API to configure them.
     * Once we get the list of features we get them all rather then fetching them in a lazy fashion.
     * The rationale is to take any performance hit at startup.
     *
     * @param configAdaptor
     * @return
     */
    public void makeFeatureSet(FeatureConfig<T> configAdaptor)
    {
        Set<String> features = configAdaptor.getFeatures();

        _featureMap = new HashMap<String, Feature<T>>();

        for(String s : features) {
            Feature<T> feature = configAdaptor.makeFeature(s);
            _featureMap.put(s, feature);
        }

    }

    /**
     * Use information from T and the implementation of the
     * Feature to calculate whether the feature is enabled.
     * @param featureName
     * @return
     */
    public boolean isEnabled(String featureName, T t)
    {
        Feature<T> feature = _featureMap.get(featureName);

        if (feature == null) {
            return false;
        }
        return feature.isEnabled(t);
    }

    /**
     * Name for logical grouping of features
     * @return
     */
    public String featureSetName()
    {
        return _featureSetName;
    }


}
