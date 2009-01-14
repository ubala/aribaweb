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

        // Set up JPAContext provider, with default persistence factory
        EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("Main");
        JPAContext.initialize();
        JPAContext.setDefaultFactory(emf);

        // Auto-bind ObjectContexts to AWPages
        ContextBinder.initialize();

        AWJarWalker.registerAnnotationListener(Entity.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        System.out.println("++++++++ Entity Annotation on class: " + className);
                        PersistenceMeta.registerEntityClass(className);
                    }
                });
        AWJarWalker.registerAnnotationListener(Embeddable.class,
                new AWJarWalker.AnnotationListener () {
                    public void annotationDiscovered(String className, String annotationType)
                    {
                        System.out.println("++++++++ Embeddable Annotation on class: " + className);
                        PersistenceMeta.registerEntityClass(className);
                    }
                });

        UIMeta.getInstance().registerAnnotationListener(OneToMany.class, new ObjectMeta.AnnotationProcessor() {
            public void processAnnotation(Annotation annotation, AnnotatedElement prop, List predicateList, Map propertyMap, boolean isAction)
            {
                ObjectMeta.addTrait("ownedToMany", propertyMap);
            }
        });

    }
}
