/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/PersistenceMeta.java#3 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.Log;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.validation.ChoiceSourceRegistry;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

public class PersistenceMeta
{
    private static boolean _DidInit = false;
    static Set<String> _EntityClasses = new HashSet();
    static Set<String> _EmbeddableClasses = new HashSet();
    public static final String KeyToOneRelationship = "toOneRelationship";
    public static final String KeyToManyRelationship = "toManyRelationship";

    public static void initialize ()
    {
        if (!_DidInit) {
            _DidInit = true;

            UIMeta.getInstance().registerKeyInitObserver(ObjectMeta.KeyType, new TypeToOneMetaProvider());
            UIMeta.getInstance().registerKeyInitObserver(ObjectMeta.KeyElementType, new TypeToManyMetaProvider());

            AWConcreteApplication application = (AWConcreteApplication) AWConcreteServerApplication.sharedInstance();
            application.registerDidInitCallback(new AWConcreteApplication.DidInitCallback() {
                public void applicationDidInit (AWConcreteApplication application) {
                    UIMeta.getInstance().loadRuleFile("PersistenceRules.oss", true, Meta.SystemRulePriority + 2000);
                }
            });
       }
    }

    public static void registerEntityClass (String className)
    {
        _EntityClasses.add(className);

        ChoiceSourceRegistry.registerProvider(className,
                new ChoiceSourceRegistry.Provider() {
                    public Object choiceSourceForParams (String className, Map params) {
                        // Todo -- implement meta choiceSource
                        return new EntityChoiceSource(className);
                    }
                });
    }

    public static void registerEmbeddableClass (String className)
    {
        _EmbeddableClasses.add(className);
    }

    static class TypeToOneMetaProvider implements Meta.ValueQueriedObserver
    {
        Map<String, Meta.RuleSet> _ruleSetsByClassName = new HashMap();

        public void notify(Meta meta, String key, Object value)
        {
            // ToDo -- what about Set<type> and List<type>?

            boolean isEntity = _EntityClasses.contains(value);
            Log.meta.debug("PersistenceMeta field Type listener notified of first use of field type: %s - isEntity: %s", value, isEntity);
            if (isEntity) {
                Map predicate = new HashMap();
                predicate.put(ObjectMeta.KeyType, value);
                Map properties = new HashMap();
                properties.put(ObjectMeta.KeyTraits, Arrays.asList(KeyToOneRelationship));
                meta.addRule(predicate, properties, Meta.ClassRulePriority);
            }
        }
    }

    static class TypeToManyMetaProvider implements Meta.ValueQueriedObserver
    {
        Map<String, Meta.RuleSet> _ruleSetsByClassName = new HashMap();

        public void notify(Meta meta, String key, Object value)
        {
            // ToDo -- what about Set<type> and List<type>?

            boolean isEntity = _EntityClasses.contains(value);
            Log.meta.debug("PersistenceMeta field Type listener notified of first use of field type: %s - isEntity: %s", value, isEntity);
            if (isEntity) {
                Map predicate = new HashMap();
                predicate.put(ObjectMeta.KeyElementType, value);
                Map properties = new HashMap();
                properties.put(ObjectMeta.KeyTraits, Arrays.asList(KeyToManyRelationship));
                meta.addRule(predicate, properties, Meta.ClassRulePriority);
            }
        }
    }
}