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

import ariba.util.fieldvalue.OrderedList;
import ariba.util.core.Assert;

import java.lang.reflect.Array;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
class ASTIndex extends SimpleNode
{
    public ASTIndex(int id)
    {
        super(id);
    }

    public ASTIndex(ExprParser p, int id)
    {
        super(p, id);
    }

    /**
        Returns true if this property is described by an IndexedPropertyDescriptor
        and that if followed by an index specifier it will call the index get/set
        methods rather than go through property accessors.
     */
    public int getIndexedPropertyType(ExprContext context, Object source) throws ExprException
    {
        Object              property = getProperty(context, source);

        if (property instanceof String) {
            return ExprRuntime.getIndexedPropertyType(context, (source == null) ? null : source.getClass(), (String)property);
        }
        return ExprRuntime.INDEXED_PROPERTY_NONE;
    }

    public Object getProperty( ExprContext context, Object source ) throws ExprException
    {
        return children[0].getValue( context, context.getRoot() );
    }

    protected Object getValueBody( ExprContext context, Object source ) throws ExprException
    {
        Object result = null;
        OrderedList listProto = OrderedList.get(source);
        // int indexType = getIndexedPropertyType(context, source);
        Object      index = getProperty(context, source);

        if (index instanceof DynamicSubscript) {
            Object      array = source;
            int         len = listProto.size(array);

            switch (((DynamicSubscript)index).getFlag()) {
                case DynamicSubscript.ALL:
                    return listProto.copy(array);
                case DynamicSubscript.FIRST:
                    index = new Integer((len > 0) ? 0 : -1);
                    break;
                case DynamicSubscript.MID:
                    index = new Integer((len > 0) ? (len / 2) : -1);
                    break;
                case DynamicSubscript.LAST:
                    index = new Integer((len > 0) ? (len - 1) : -1);
                    break;
            }
        } else {
            Assert.that(index instanceof Number, "array subscript must be number");
            // if (indexType == ExprRuntime.INDEXED_PROPERTY_OBJECT) {
            // throw new ExprException("DynamicSubscript '" + this + "' not allowed for object indexed property '" + source + "'");
        }
        try {
            Object value = listProto.elementAt(source, ((Number)index).intValue());
            return ExprRuntime.convert(value);
        }
        catch (IndexOutOfBoundsException e) {
            // If there is no element, return null
            return null;
        }
    }

    protected void setValueBody( ExprContext context, Object target, Object value ) throws ExprException
    {
        Object      index = getProperty(context, target);
        OrderedList listProto = OrderedList.get(target);
        if (index instanceof DynamicSubscript) {
            Object      array = target;
            int         len = Array.getLength(array);

            switch (((DynamicSubscript)index).getFlag()) {
                case DynamicSubscript.ALL:
                    Assert.that(false, "Array assignment to subscript All not supported");
                case DynamicSubscript.FIRST:
                    index = new Integer((len > 0) ? 0 : -1);
                    break;
                case DynamicSubscript.MID:
                    index = new Integer((len > 0) ? (len / 2) : -1);
                    break;
                case DynamicSubscript.LAST:
                    index = new Integer((len > 0) ? (len - 1) : -1);
                    break;
            }
        } else {
            Assert.that(index instanceof Number, "array subscript must be number");
/*
            if (indexType == ExprRuntime.INDEXED_PROPERTY_OBJECT) {
                throw new ExprException("DynamicSubscript '" + this + "' not allowed for object indexed property '" + target + "'");
            }
*/
        }
        try {
            listProto.setElementAt(target, value, ((Number)index).intValue());
        }
        catch (IndexOutOfBoundsException e) {
            // If there is no element, no op.
        }
    }

    public boolean isNodeSimpleProperty( ExprContext context ) throws ExprException
    {
        return (children != null) && (children.length == 1) && ((SimpleNode)children[0]).isConstant(context);
    }

    public String toString()
    {
        return "[" + children[0] + "]";
    }

    public void accept (ASTNodeVisitor visitor)
    {
        acceptChildren(visitor);
        visitor.visit(this);
    }
}
