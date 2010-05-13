/*
    Copyright (c) 1996-2009 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/core/ariba/util/test/TestDestager.java#1 $

    Responsible: achung
*/

package ariba.util.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention (RetentionPolicy.RUNTIME)
@Target ({ElementType.TYPE,ElementType.FIELD,ElementType.METHOD})
@Inherited
public @interface TestDestager
{
    public String name() default "";
    public String superType() default "";
    public String typeList () default "";
    public boolean requireUser() default true;
    public String description () default "";
}
