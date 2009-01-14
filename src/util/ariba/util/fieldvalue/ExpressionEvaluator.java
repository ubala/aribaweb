/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ExpressionEvaluator.java#2 $
*/
package ariba.util.fieldvalue;

import java.util.Collection;

/**
    Abstraction of a factory class for creating {@link Expression}s.<p/>
    @aribaapi ariba
*/
public abstract class ExpressionEvaluator
{
    /**
        Creates, compiles and returns a new {@link Expression} based
        off the supplied <code>stringRepresentation</code>. <p/>
        @aribaapi ariba
    */
    public abstract Expression compile (String stringRepresentation)
    throws ExpressionException;

    //--------------------------------------------------------------------------
    // convenience methods

    /**
        Given an expression, return the field paths.
        @param expression the specified expression, must not be null
        @return a list of field paths represented as Strings.
        @exception ExpressionEvaluatorException
        @aribaapi ariba
    */
    public final Collection/*<String>*/ getFieldPaths (String expression)
    throws ExpressionException
    {
        Expression expr = compile(expression);
        return expr.getFieldPaths();
    }

    /**
        Evaluate the given expression against the specified object.
        @param expression the expression to evaluate, must not be <code>null</code>
        @param object the specified object to apply the expression to,
               must not be <code>null</code>
        @param fieldvalue an optional field value insance that can be used to
               evaluate the given expression. Can be <code>null</code> condition,
               <code>false</code> otherwise
        @return the evaluated result
        @exception ExpressionEvaluatorException
        @aribaapi ariba
    */
    public final Object evaluate (
            String expression,
            Object object,
            FieldValue fieldvalue
    )
    throws ExpressionException
    {
        Expression expr = compile(expression);
        return expr.evaluate(object, fieldvalue);
    }

    /**
        Evaluate the given expression against the specified object. This is equivalent
        to calling evaluate(String, Object, null)
        @param expression the expression to evaluate, must not be <code>null</code>
        @param object the specified object to apply the expression to,
                must not be <code>null</code>
        @return the evaluated result
        @exception ExpressionEvaluatorException
        @aribaapi ariba
    */
    public final Object evaluate (String expression, Object object)
    throws ExpressionEvaluatorException
    {
        return evaluate(expression, object, null);
    }

}
