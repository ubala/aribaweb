/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.

    $Id: //ariba/platform/util/expr/ariba/util/expr/ASTProperty.java#10 $
 */
//--------------------------------------------------------------------------
//  Copyright (c) 1998-2004, Drew Davidson and Luke Blanshard
//  All rights reserved.
//
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions are
//  met:
//
//  Redistributions of source code must retain the above copyright notice,
//  this list of conditions and the following disclaimer.
//  Redistributions in binary form must reproduce the above copyright
//  notice, this list of conditions and the following disclaimer in the
//  documentation and/or other materials provided with the distribution.
//  Neither the name of the Drew Davidson nor the names of its contributors
//  may be used to endorse or promote products derived from this software
//  without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
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

import ariba.util.core.Assert;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldvalue.FieldPath;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
class ASTProperty extends SimpleNode implements Symbol
{

    /*
        Use of a cached field path provides us with fast field lookup -- the <class, field> accessor
        is cached and reapplied (without lookup) if the class still matches.  Since these cache the
        java reflection field/method (or compiled accessor) this approaches optimally efficient.
    */
    FieldPath _fieldPath;

    public ASTProperty (int id)
    {
        super(id);
    }

    public ASTProperty (ExprParser p, int id)
    {
        super(p, id);
    }

    public void jjtClose ()
    {
        Assert.that(children != null && children.length == 1
                    && children[0] instanceof ASTConst,
                    "Property node must have constant child");
        _fieldPath = new FieldPath(toString());
    }

    protected Object getValueBody (ExprContext context, Object source)
    throws ExprException
    {
        Integer symbolKind = context.getSymbolKind(this);

        if (Symbol.Field.equals(symbolKind) &&
            (source instanceof TypeInfo)) {
            // If the source is a TypeInfo, then the property is a static
            // field of the type/class.
            String typeName = ((TypeInfo)source).getName();
            return ExprRuntime.getStaticField(context, typeName, getName());
        }
        else if (Symbol.Variable.equals(symbolKind)) {
            return context.get(getName());
        }
        else if (Symbol.Type.equals(symbolKind)) {
            return context.getSymbolType(this);
        }
        else {
            Object value = ExprRuntime.convert(source);
            value = _fieldPath.getFieldValue(value);
            return ExprRuntime.convert(value);
        }
    }

    protected void setValueBody (ExprContext context, Object target, Object value)
    throws ExprException
    {
        Integer symbolKind = context.getSymbolKind(this);

        if (Symbol.Field.equals(symbolKind) && (target instanceof TypeInfo)) {
            // If the source is a TypeInfo, then the property is a static
            // field of the type/class.
            String typeName = ((TypeInfo)target).getName();
            ExprRuntime.setStaticField(context, typeName, getName(), value);
        }
        else if (Symbol.Variable.equals(symbolKind)) {
            context.put(getName(), value);
        }
        else {
            // Convert the primitive to the type of this property. This is
            // necessary because numeric literal are Integer or Double and
            // there is no cast operation to convert to the property type.
            TypeInfo nodeType = getTypeInfo();
            value = TypeConversionHelper.convertPrimitive(nodeType, value);
            _fieldPath.setFieldValue(target, value);
        }
    }

    public boolean isNodeSimpleProperty (ExprContext context) throws ExprException
    {
        //was: return (children != null) && (children.length == 1) && ((SimpleNode)children[0]).isConstant(context);
        return true;
    }

    public String toString ()
    {
        return ((ASTConst)children[0]).getValue().toString();
    }

    public String getName ()
    {
        return _fieldPath._fieldName;
    }

    public void accept (ASTNodeVisitor visitor)
    {
        acceptChildren(visitor);
        visitor.visit(this);
    }

    @Override
    protected void traceEvaluation (final Object result)
    {
        if (!(this.parent instanceof ASTChain) && SimpleNode.shouldTraceValue(result)) {
            super.traceEvaluation(result);
        }
    }
}
