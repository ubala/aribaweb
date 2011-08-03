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

import ariba.util.fieldtype.MethodInfo;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
class ASTMethod extends SimpleNode implements Symbol
{
    private String methodName;

    public ASTMethod (int id)
    {
        super(id);
    }

    public ASTMethod (ExprParser p, int id)
    {
        super(p, id);
    }

      /** Called from parser action. */
    void setMethodName (String methodName )
    {
        this.methodName = methodName;
    }

    /**
        Returns the method name that this node will call.
     */
    public String getMethodName ()
    {
        return methodName;
    }

    public String getName ()
    {
        // do not use getMethodName() since we want the parameter list to
        // be part of the symbol name.
        return toString();
    }

    protected Object getValueBody (ExprContext context,Object source) throws ExprException
    {
        SymbolTable table = context.getSymbolTable();
        SemanticRecord record = (table != null ?
                                (SemanticRecord)table.getSymbolRecord(this) : null);
        MethodInfo methodInfo = (record != null?
                                 (MethodInfo)record.getPropertyInfo() : null);
        boolean isStatic = (methodInfo != null ? methodInfo.isStatic() : false);

        // Wrap the source as well as to provide proper evaluation context
        // for method invocation.
        source = ExprRuntime.convert(source);

        Object value = null;
        Object[]    args = ExprRuntime.getObjectArrayPool().create(jjtGetNumChildren());
        Object      root = context.getRoot();

        try {
            for ( int i=0, icount = args.length; i < icount; ++i ) {
                args[i] = children[i].getValue(context, root);
            }
            context.put(ExprContext.CURRENT_METHODINFO_IN_EXECUTION,methodInfo);
            if (isStatic) {
                String typeName = methodInfo.getParentType().getName();
                value = ExprRuntime.callStaticMethod(context,typeName,methodName,args);
            }
            else {
                value = ExprRuntime.callMethod( context, source, methodName, null, args);
                if (value == null) {
                    Class clazz = ExprRuntime.getTargetClass(source);
                    NullHandler nh = ExprRuntime.getNullHandler(clazz);
                    value = nh.nullMethodResult(context, source, methodName, args);
                }
            }
        }
        finally {
            context.remove(ExprContext.CURRENT_METHODINFO_IN_EXECUTION);
            ExprRuntime.getObjectArrayPool().recycle(args);
        }
        return ExprRuntime.convert(value);
    }

    public String toString ()
    {
        String      result = methodName;

        result = result + "(";
        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; i++) {
                if (i > 0) {
                    result = result + ", ";
                }
                result = result + children[i];
            }
        }
        result = result + ")";
        return result;
    }

    public void accept (ASTNodeVisitor visitor)
    {
        // visitor will have to navigate the child.
        visitor.visit(this);
    }
}
