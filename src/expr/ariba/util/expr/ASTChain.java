/*
    Copyright (c) 1996-2013 Ariba, Inc.
    All rights reserved. Patents pending.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/expr/ariba/util/expr/ASTChain.java#8 $
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

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
class ASTChain extends SimpleNode implements Symbol
{
    public ASTChain (int id)
    {
        super(id);
    }

    public ASTChain (ExprParser p, int id)
    {
        super(p, id);
    }

    public void jjtClose ()
    {
        // flattenTree();
    }

    protected Object getValueBody (ExprContext context, Object source)
    throws ExprException
    {
        Integer symbolKind = context.getSymbolKind(this);
        TypeInfo type = context.getSymbolType(this);

        if (Symbol.Type.equals(symbolKind)) {
            // If the property chain actually represents a type.
            Assert.that(type != null,
                        "Type information is missing for %s", getName());
            return type;
        }

        Object object = children[0].getValue( context, source );
        return (object != null ?
                children[1].getValue( context, object ) :
                null);
    }

    protected void setValueBody (ExprContext context, Object target, Object value)
    throws ExprException
    {
        Object object = children[0].getValue( context, target );
        // if the last child is null, then skip the evaluation.  This is
        // identical to how FieldPath handles null field path element during
        // setValue().
        if (object != null) {
            children[1].setValue( context, object, value );
        }
    }

    public boolean isSimpleNavigationChain (ExprContext context)
    throws ExprException
    {
        boolean     result = false;

        if ((children != null) && (children.length > 0)) {
            result = true;
            for (int i = 0; result && (i < children.length); i++) {
                if (children[i] instanceof SimpleNode) {
                    result = ((SimpleNode)children[i]).isSimpleProperty(context);
                }
                else {
                    result = false;
                }
            }
        }
        return result;
    }

    public String getName ()
    {
        return toString();
    }

    public String toString ()
    {
        String      result = "";

        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; i++) {
                if (i > 0) {
                    if (!(children[i] instanceof ASTIndex)) {
                        result = result + ".";
                    }
                }
                result += children[i].toString();
            }
        }
        return result;
    }

    public void accept (ASTNodeVisitor visitor)
    {
        // Let the visitor navigate.  This is breaking the pattern, until
        // the navigation chain is represented as a subtree.
        visitor.visit(this);
    }

    @Override
    protected void traceEvaluation (final Object result)
    {
        if (SimpleNode.shouldTraceValue(result)) {
            super.traceEvaluation(result);
        }
    }
}
