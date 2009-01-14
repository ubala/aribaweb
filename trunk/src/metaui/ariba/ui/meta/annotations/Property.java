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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/annotations/Property.java#4 $
*/
package ariba.ui.meta.annotations;

import ariba.ui.meta.core.ObjectMeta;
import ariba.util.core.Fmt;
import ariba.util.fieldvalue.FieldValue;

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
public class Property
{
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Visible { public abstract String value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Valid { public abstract String value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Editable { public abstract String value(); }

    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD,ElementType.METHOD}) @Inherited
    public @interface Label { public abstract String value(); }

    static public void registerAnnotationListeners (ObjectMeta meta)
    {
        registerProperty(Visible.class, meta);
        registerProperty(Valid.class, meta);
        registerProperty(Editable.class, meta);
        registerProperty(Label.class, meta);
    }

    static public void registerProperty (final Class annotationClass, final ObjectMeta meta)
    {
        meta.registerAnnotationListener(annotationClass, new ObjectMeta.AnnotationProcessor(){
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List selectorList, Map propertyMap, boolean isAction)
            {
                String propertyDecl = Fmt.S("%s:%s", annotationClass.getSimpleName().toLowerCase(),
                        FieldValue.getFieldValue(annotation,  "value"));
                meta.processPropertiesAnnotation(propertyDecl, prop, selectorList);
            }
        });
    }
}
