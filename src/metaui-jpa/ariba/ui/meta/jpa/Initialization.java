package ariba.ui.meta.jpa;

import ariba.ui.meta.persistence.ContextBinder;
import ariba.ui.meta.persistence.PersistenceMeta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.aribaweb.util.AWJarWalker;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Entity;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import javax.persistence.ManyToMany;
import javax.persistence.Column;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;

public class Initialization
{
    private static boolean _DidInit = false;

    public static void initialize ()
    {
        if (_DidInit) return;
        _DidInit = true;
        PersistenceMeta.initialize();

        // Set up JPAContext provider
        JPAContext.initialize();

        // Auto-bind ObjectContexts to AWPages
        ContextBinder.initialize();

        AWJarWalker.registerAnnotationListener(Entity.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        Log.metajpa.debug("++++++++ Entity Annotation on class: " + className);
                        PersistenceMeta.registerEntityClass(className);
                    }
                });
        AWJarWalker.registerAnnotationListener(Embeddable.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        Log.metajpa.debug("++++++++ Embeddable Annotation on class: " + className);
                        PersistenceMeta.registerEntityClass(className);
                    }
                });

        UIMeta.getInstance().registerAnnotationListener(OneToMany.class, new ObjectMeta.AnnotationProcessor() {
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (!isAction) ObjectMeta.addTrait("ownedToMany", propertyMap);
            }
        });

        UIMeta.getInstance().registerAnnotationListener(ManyToMany.class, new ObjectMeta.AnnotationProcessor() {
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (!isAction) ObjectMeta.addTrait("manyToMany", propertyMap);
            }
        });

        UIMeta.getInstance().registerAnnotationListener(Column.class, new ObjectMeta.AnnotationProcessor() {
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                if (!((Column)annotation).nullable()) ObjectMeta.addTrait("required", propertyMap);
            }
        });
    }
}
