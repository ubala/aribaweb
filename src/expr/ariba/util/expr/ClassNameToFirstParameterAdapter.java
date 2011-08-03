/*
    Copyright (c) 1996-2011 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/expr/ariba/util/expr/ClassNameToFirstParameterAdapter.java#2 $

    Responsible: kgangadharapppa
*/
package ariba.util.expr;

import ariba.util.fieldtype.JavaTypeProvider.JavaMethodInfo;

/**
 * Takes care of getting the first argument from the methodInfo and passing
 * that during runtime based on the annotation. Registered with ExprRuntime
 * during startup.
 */
public class ClassNameToFirstParameterAdapter extends ObjectMethodAccessor
{

    public Object callStaticMethod (ExprContext context,
                            Class targetClass,
                            String methodName,
                            Object[] args ) throws MethodFailedException
    {
        Object[] newArgs = null;
        try {
            Object obj = context.get(ExprContext.CURRENT_METHODINFO_IN_EXECUTION);

            if (targetClass!=null && obj instanceof JavaMethodInfo) {
              String firstParam = ((JavaMethodInfo)obj).getFirstArgumentClassName();
              if (firstParam != null) {
                  newArgs = ExprRuntime.getObjectArrayPool().create(args.length + 1);
                  newArgs[0] = firstParam;
                  System.arraycopy(args,0,newArgs,1,args.length);
                  args = newArgs;
              }
            }

            Object ret = super.callStaticMethod(context,targetClass,methodName,args);
            return ret;
        }
        finally {
            if (newArgs!=null) {
               ExprRuntime.getObjectArrayPool().recycle(newArgs);
            }
        }
    }
}
