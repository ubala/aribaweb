/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $id$

    Responsible: kgangadharapppa
*/
package ariba.util.fieldtype;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * This annotation is used for dynamic detection as well as routing of static methods.
 * This was implemented as part of FMD.lookup() enhancement.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface SafeMethodOptions
{
    /**
     * Specifies if the first parameter in the method is supposed to be a string that
     * represents a classname.
     * @return
     */
    boolean firstParameterIsClassName();

    /**
     * Specifies if the return value of the method on which this annotation is used
     * is of the type passed in the first parameter. If this is set to true,
     * firstParameterIsClassName should be set to true as well. However this could
     * be false; an example is: boolean exists(String className, String uniqueName)
     * @return
     */
    boolean covariantReturn();
}