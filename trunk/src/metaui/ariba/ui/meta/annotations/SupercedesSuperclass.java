package ariba.ui.meta.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;

/**
    Marker to indicate that this persistent entity class should replace its superclass.
    E.g. if placed on "class MyUser extends ariba.appcore.User", then this class will
    be created in place of User in calls to ObjectContext.create(User.class), and the
    module tab for the original User will be hidden in favor of this one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface SupercedesSuperclass
{
}
