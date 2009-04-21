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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/persistence/PersistenceMeta.java#15 $
*/
package ariba.ui.meta.persistence;

import ariba.ui.meta.core.*;
import ariba.ui.meta.annotations.SupercedesSuperclass;
import ariba.ui.validation.ChoiceSourceRegistry;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWJarWalker;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.ClassUtil;

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
    public static final String PropUseTextSearch = "useTextIndex";
    public static final String PropTextSearchSupported = "textSearchSupported";
    public static final String KeywordsField = "keywords";
    static boolean _DoNotConnect = false;
    static Map<Class, Class> _SupercededClassMap = MapUtil.map();

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
                    hideSupercededClasses(UIMeta.getInstance());
                }
            });

            AWJarWalker.registerAnnotationListener(SupercedesSuperclass.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        // Ick: forces non-lazy class initialization!
                        Class sub = ClassUtil.classForName(className);
                        _SupercededClassMap.put(sub.getSuperclass(), sub);
                    }
                });
       }
    }

    public static void registerEntityClass (String className)
    {
        _EntityClasses.add(className);
        UIMeta.registerLocalizedClass(className);

        ChoiceSourceRegistry.registerProvider(className,
                new ChoiceSourceRegistry.Provider() {
                    public Object choiceSourceForParams (String className, Map params) {
                        // Todo -- implement meta choiceSource
                        return new EntityChoiceSource(className);
                    }
                });
    }

    public static Set<String> getEntityClasses ()
    {
        return _EntityClasses;
    }

    public static Set<Class>getSupercededClasses ()
    {
        return _SupercededClassMap.keySet();
    }

    public static Class supercedingChildClass (Class parentClass)
    {
        Class child = _SupercededClassMap.get(parentClass);
        return child == null ? parentClass : child;
    }

    public static void registerEmbeddableClass (String className)
    {
        _EmbeddableClasses.add(className);
    }

    public static boolean doNotConnect ()
    {
        return _DoNotConnect;
    }

    public static void setDoNotConnect (boolean doNotConnect)
    {
        PersistenceMeta._DoNotConnect = doNotConnect;
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
                meta.beginRuleSet(TypeToOneMetaProvider.class.getName());
                Map selector = new HashMap();
                selector.put(ObjectMeta.KeyType, value);
                Map properties = new HashMap();
                properties.put(Meta.KeyTrait, Arrays.asList(KeyToOneRelationship));
                meta.addRule(selector, properties, Meta.ClassRulePriority);
                meta.endRuleSet();
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
                meta.beginRuleSet(TypeToManyMetaProvider.class.getName());
                Map selector = new HashMap();
                selector.put(ObjectMeta.KeyElementType, value);
                Map properties = new HashMap();
                properties.put(Meta.KeyTrait, Arrays.asList(KeyToManyRelationship));
                meta.addRule(selector, properties, Meta.ClassRulePriority);
                meta.endRuleSet();
            }
        }
    }

    public static AWResponseGenerating validateAndSave (UIMeta.UIContext ctx)
    {
        if (!ctx.requestContext().getCurrentComponent().errorManager().checkErrorsAndEnableDisplay()) {
            ariba.ui.meta.persistence.ObjectContext.get().save();
        }
        return null;
    }

    // Simple Map wrapper that implements identity equals / hashCode for stability when used
    // as key (e.g. as valueSource in AWErrorManager)
    public static class SearchMap extends HashMap
    {
        public int hashCode ()
        {
            return System.identityHashCode(this);
        }

        public boolean equals (Object o)
        {
            return (o instanceof SearchMap) && (o == this);
        }
    }

    public static void hideSupercededClasses(UIMeta meta)
    {
        for (Map.Entry<Class, Class> e : _SupercededClassMap.entrySet()) {
            Class overrider = e.getValue();
            meta.beginRuleSet(Meta.ClassRulePriority + 1000, overrider.getName().replace(".", "/") +".java");
            Rule rule = new Rule(AWUtil.list(new Rule.Selector(UIMeta.KeyModule, e.getKey().getName())),
                    AWUtil.map(UIMeta.KeyHidden, true));
            meta.addRule(rule);
            meta.endRuleSet();
        }
    }
}