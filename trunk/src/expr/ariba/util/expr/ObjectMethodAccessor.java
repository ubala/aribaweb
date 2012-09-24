//--------------------------------------------------------------------------
//	Copyright (c) 1998-2004, Drew Davidson and Luke Blanshard
//  All rights reserved.
//
//	Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are
//  met:
//
//	Redistributions of source code must retain the above copyright notice,
//  this list of conditions and the following disclaimer.
//	Redistributions in binary form must reproduce the above copyright
//  notice, this list of conditions and the following disclaimer in the
//  documentation and/or other materials provided with the distribution.
//	Neither the name of the Drew Davidson nor the names of its contributors
//  may be used to endorse or promote products derived from this software
//  without specific prior written permission.
//
//	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
//  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
//  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
//  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
//  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
//  AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
//  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
//  THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
//  DAMAGE.
//--------------------------------------------------------------------------
package ariba.util.expr;

import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.fieldtype.JavaTypeProvider.JavaMethodInfo;
import ariba.util.fieldtype.JavaTypeProvider.JavaTypeInfo;
import ariba.util.fieldtype.JavaTypeProvider;
import ariba.util.fieldtype.JavaTypeRegistry;
import ariba.util.fieldtype.MethodInfo;
import ariba.util.fieldtype.NullTypeInfo;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldtype.TypeRetriever;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of PropertyAccessor that uses reflection on the target object's class to
 * find a field or a pair of set/get methods with the given property name.
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public class ObjectMethodAccessor implements MethodAccessor
{

        /* MethodAccessor interface */
    public Object callStaticMethod (ExprContext context,
                                    Class targetClass,
                                    String methodName,
                                    Object[] args ) throws MethodFailedException
    {
        Method method = getMethod(targetClass,methodName,args,true);
        Object ret = ExprRuntime.callAppropriateMethod(context,targetClass,null,
                                                            methodName,method,args);
        return ret;
    }

    public Object callMethod (ExprContext context,
                              Object target,
                              String methodName,
                              Object[] args ) throws MethodFailedException
    {
        Class  targetClass = (target == null) ? null : target.getClass();
        Method method = getMethod(targetClass,methodName,args,false);
        Object ret = ExprRuntime.callAppropriateMethod(context,target,target,
                                                       methodName,method,args);
        return ret;
    }

    private Method getMethod (Class targetClass,String methodName,
                                              Object[] args, boolean isStatic)
    {
        Method ret = null;
        if (targetClass != null) {
            TypeRetriever retriever = JavaTypeRegistry.instance();
            List<String> argTypes = getArgumentTypes(args);
            TypeInfo targetType = retriever.getTypeInfo(targetClass.getName());

            // In the case of nested class loaders we could have a class for which
             // classForName() (using the system class loader) might fail,
             // so we look up using the class explicitly
             // Todo: reconsider the getTypeInfo interface: maybe add class as an (optional) arg
            if (targetType == null) {
                targetType = JavaTypeProvider.instance().getTypeInfo(targetClass);
            }

            if (targetType instanceof JavaTypeInfo) {
                JavaTypeInfo javaType =(JavaTypeInfo)targetType;
                MethodInfo methodInfo = javaType.getMethodForName(
                    retriever, methodName, argTypes, isStatic);

                if (methodInfo instanceof JavaMethodInfo) {
                   ret = ((JavaMethodInfo)methodInfo).getMethod();
                }
            }
        }
        return ret;
    }

    private static List<String> getArgumentTypes (Object[] args)
    {
        if (ArrayUtil.nullOrEmptyArray(args)) {
            return Collections.EMPTY_LIST;
        }

        TypeRetriever retriever = JavaTypeRegistry.instance();
        List<String> result = ListUtil.list(args.length);
        for (Object arg : args) {
            TypeInfo type = NullTypeInfo.instance;
            if (arg != null) {
                Class argCls = arg.getClass();
                type = retriever.getTypeInfo(argCls.getName());
                Assert.that(type != null,
                        "Failed to retrieve type for name '%s'.", argCls.getName());
            }
            result.add(type.getName());
        }

        return result;
    }
}
