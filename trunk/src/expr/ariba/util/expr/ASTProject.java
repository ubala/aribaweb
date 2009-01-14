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

import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldvalue.OrderedList;

import java.util.*;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
class ASTProject extends SimpleNode implements Symbol
{
    public static final String CollectMethod = "collect";
    public static final String FindAllMethod = "findAll";
    public static final String FindMethod = "find";
    public static final String SumMethod = "sum";
    public static final String AvgMethod = "avg";
    public static final String MinMethod = "min";
    public static final String MaxMethod = "max";

    private String methodName;

    public ASTProject(int id) {
        super(id);
    }

    public ASTProject(ExprParser p, int id) {
        super(p, id);
    }

    public String getMethodName ()
    {
        return methodName;
    }

    /** Called from parser action. */
    void setMethodName( String methodName ) {
        this.methodName = methodName;
    }

    protected Object getValueBody( ExprContext context, Object source ) throws ExprException
    {
        // Eventually we need to generalize to apply via classextension...
        Node                expr = children[0];
        OrderedList listInterface = OrderedList.get(source);
        Iterator e = listInterface.elements(source);

        if (methodName.equals("collect")) return collect(e, expr, context, source);
        else if (methodName.equals("findAll")) return findAll(e, expr, context, source);
        else if (methodName.equals("find")) return find(e, expr, context, source);
        else if (methodName.equals("sum")) return sum(e, expr, context, source);
        else if (methodName.equals("avg")) return avg(e, expr, context, source);
        else if (methodName.equals("min")) return min(e, expr, context, source);
        else if (methodName.equals("max")) return max(e, expr, context, source);

        return null;
    }

    protected Object collect(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        List answer = new ArrayList();
        while (e.hasNext()) {
            answer.add( expr.getValue(context, e.next()) );
        }
        return answer;
    }

    protected Object findAll(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        List                answer = new ArrayList();

        while (e.hasNext()) {
            Object next = e.next();
            if ( ExprOps.booleanValue(expr.getValue(context, next)) ) answer.add(next);
        }
        return answer;
    }

    protected Object find(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        List                answer = new ArrayList();

        while (e.hasNext()) {
            Object next = e.next();
            if ( ExprOps.booleanValue(expr.getValue(context, next)) ) return next;
        }
        return null;
    }

    protected Object sum(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        Object result = null;

        while (e.hasNext()) {
        	Object value = expr.getValue(context, e.next());
        	TypeInfo valueInfo = getTypeInfo();
            result = ExprOps.add(
            	result, value, null, valueInfo != null? valueInfo.getName(): null);
        }
        return result;
    }

    protected Object avg(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        Object result = new Integer(0);
        String resultType = Integer.class.getName();
        int count = 0;

        while (e.hasNext()) {
            Object value = expr.getValue(context, e.next());
            TypeInfo valueInfo = expr.getTypeInfo();
            result = ExprOps.add(
            		result, 
            		value,
            		resultType,
            		valueInfo != null? valueInfo.getName(): null);
            resultType = result != null? result.getClass().getName(): null;
            count++;
        }
        return ExprOps.divide(result, new Integer(count), resultType);
    }

    protected Object min(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        Object result = null;

        while (e.hasNext()) {
            Object value = expr.getValue(context, e.next());
            if (result == null || (ExprOps.compareWithConversion(value, result) < 0)) result = value;
        }
        return result;
    }

    protected Object max(Iterator e, Node expr, ExprContext context, Object source) throws ExprException
    {
        Object result = null;

        while (e.hasNext()) {
            Object value = expr.getValue(context, e.next());
            if (result == null || (ExprOps.compareWithConversion(value, result) > 0)) result = value;
        }
        return result;
    }

    public String toString()
    {
        return methodName + "{ " + children[0] + " }";
    }

    public void accept (ASTNodeVisitor visitor)
    {
        visitor.visit(this);
    }

    public String getName ()
    {
        return methodName;
    }
}
