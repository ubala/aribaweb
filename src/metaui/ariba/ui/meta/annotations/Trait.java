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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/annotations/Trait.java#7 $
*/
package ariba.ui.meta.annotations;

import ariba.ui.meta.core.ObjectMeta;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.reflect.AnnotatedElement;
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

    static public void registerAnnotationListeners (ObjectMeta meta)
    {
        registerAnnotation(LongText.class, "longtext", meta);
        registerAnnotation(RichText.class, "richtext", meta);
        registerAnnotation(LabelField.class, "labelField", meta);
        registerAnnotation(Secret.class, "secret", meta);
        registerAnnotation(Required.class, "required", meta);
        registerAnnotation(Truncated.class, "truncated", meta);
    }

    static public void registerAnnotation (final Class annotationClass, final String name, ObjectMeta meta)
    {
        meta.registerAnnotationListener(annotationClass, new ObjectMeta.AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List selectorList, Map propertyMap, boolean isAction)
            {
                if (!isAction) ObjectMeta.addTrait(name, propertyMap);
            }
        });
    }
}
