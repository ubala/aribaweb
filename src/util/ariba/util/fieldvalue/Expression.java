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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/Expression.java#2 $
*/
package ariba.util.fieldvalue;

import java.util.Collection;

/**
    Abstract representation of an expression in an unspecified expression
    language. <p/>

    @aribaapi ariba
*/
public abstract class Expression
{
    //--------------------------------------------------------------------------
    // public abstract methods

    /**
        Evaluates <code>this</code> against <code>object</code> using the
        supplied <code>fieldValueProtocol</code> to object field values
        from <code>object</code> during the evaluation. (Expressions may
        refer to fields of the objects against which they are evaluated.)

        @param object the object against which to evaluate <code>this</code>;
               may be <code>null</code> but in this case cannot have any fields
               dereferenced against it
        @param fieldValueProtocol the protocol to use to obtain field values from
               <code>object</code> during the evaulation; may be <code>null</code>
               in which case the default field value protocol for the class
               of <code>object</code> is used (see {@link FieldValue#get(Object)})

        @aribaapi ariba
    */
    public abstract Object evaluate (Object object, FieldValue fieldValueProtocol)
    throws ExpressionEvaluatorException;

    /**
        Returns the collection of fields paths referenced in this expression. <p/>
        @aribaapi ariba
    */
    public abstract Collection/*<String>*/ getFieldPaths ();

    //--------------------------------------------------------------------------
    // public convenience methods

    /**
        Calls {@link #evaluate(Object,FieldValue)} with a <code>null</code>
        <code>FieldValue</code>. <p/>

        @aribaapi ariba
    */
    public final Object evaluate (Object object) throws ExpressionEvaluatorException
    {
        return evaluate(object, null);
    }

    /**
        Returns the <code>String</code> representation of <code>this</code>.
        The returned string should be the <code>String</code> that was
        used when <code>this</code> was {@link ExpressionEvaluator#compile}
        (ideally) or at least the string should be equivalent in the
        sense that it would result in the creation of an identically behaving
        expression. <p/>

        @aribaapi ariba
    */
    public abstract String toString ();
}
