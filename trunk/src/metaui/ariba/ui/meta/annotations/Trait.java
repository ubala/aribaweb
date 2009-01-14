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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/annotations/Trait.java#1 $
*/
package ariba.ui.meta.annotations;

import ariba.ui.meta.core.UIMeta;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.reflect.AccessibleObject;
import java.util.List;
import java.util.Map;

/**
    Set of annotations that correspond to *traits* (defined in WidgetsRules.oss)
 */
public class Trait
{
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface LongText {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface RichText {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface LabelField {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Secret {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Required {}

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Truncated {}

    static public void registerAnnotationListeners (UIMeta meta)
    {
        registerAnnotation(LongText.class, meta);
        registerAnnotation(RichText.class, meta);
        registerAnnotation(LabelField.class, meta);
        registerAnnotation(Secret.class, meta);
        registerAnnotation(Required.class, meta);
        registerAnnotation(Truncated.class, meta);
    }

    static public void registerAnnotation (final Class annotationClass, UIMeta meta)
    {
        meta.registerAnnotationListener(annotationClass, new UIMeta.AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AccessibleObject prop, List predicateList, Map propertyMap, boolean isAction)
            {
                UIMeta.addTrait(annotationClass.getSimpleName(), propertyMap);
            }
        });
    }
}
