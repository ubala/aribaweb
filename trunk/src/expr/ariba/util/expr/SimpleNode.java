/*
    Copyright (c) 2013 Ariba, Inc.
    All rights reserved. Patents pending.
  
    $Id: //ariba/platform/util/expr/ariba/util/expr/SimpleNode.java#6 $
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

import java.io.PrintWriter;
import java.io.Serializable;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import ariba.util.core.ArrayUtil;
import ariba.util.fieldtype.TypeInfo;

/**
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@ognl.org)
 */
public abstract class SimpleNode implements Node, Serializable
{
    protected Node          parent;
    protected Node[]        children;
    protected int           id;
    protected ExprParser    parser;

    private boolean         constantValueCalculated;
    private boolean         hasConstantValue;
    private Object          constantValue;

    private TypeInfo _typeInfo;

    public SimpleNode (int i)
    {
        id = i;
        _typeInfo = null;
    }

    public SimpleNode (ExprParser p, int i)
    {
        this(i);
        parser = p;
        _typeInfo = null;
    }

    public void jjtOpen ()
    {
        // default is to do nothing
    }

    public void jjtClose ()
    {
        // default is to do nothing
    }

    public void jjtSetParent (Node n)
    {
        parent = n;
    }
    
    public Node jjtGetParent ()
    {
        return parent;
    }

    public void jjtAddChild (Node n, int i)
    {
        if (children == null) {
            children = new Node[i + 1];
        }
        else if (i >= children.length) {
            Node c[] = new Node[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }
        children[i] = n;
    }

    public Node jjtGetChild (int i)
    {
        return children[i];
    }

    public int jjtGetNumChildren ()
    {
        return (children == null) ? 0 : children.length;
    }

      /* You can override these two methods in subclasses of SimpleNode to
         customize the way the node appears when the tree is dumped.  If
         your output uses more than one line you should override
         toString(String), otherwise overriding toString() is probably all
         you need to do. */

    public String toString ()
    {
        return ExprParserTreeConstants.jjtNodeName[id];
    }

/*
 * AribaExpr additions
 */

    public String toString (String prefix)
    {
        return prefix + ExprParserTreeConstants.jjtNodeName[id] + " " + toString();
    }

      /* Override this method if you want to customize how the node dumps
         out its children. */

    public void dump (PrintWriter writer, String prefix)
    {
        writer.println(toString(prefix));
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode)children[i];
                if (n != null) {
                    n.dump(writer, prefix + "  ");
                }
            }
        }
    }

    public int getIndexInParent ()
    {
        int     result = -1;

        if (parent != null) {
            int     icount = parent.jjtGetNumChildren();

            for (int i = 0; i < icount; i++) {
                if (parent.jjtGetChild(i) == this) {
                    result = i;
                    break;
                }
            }
        }
        return result;
    }

    public Node getNextSibling ()
    {
        Node    result = null;
        int     i = getIndexInParent();

        if (i >= 0) {
            int     icount = parent.jjtGetNumChildren();

            if (i < icount) {
                result = parent.jjtGetChild(i + 1);
            }
        }
        return result;
    }

    protected Object evaluateGetValueBody (ExprContext context, Object source)
    throws ExprException
    {
        context.setCurrentObject(source);
        context.setCurrentNode(this);
        if (!constantValueCalculated) {
            constantValueCalculated = true;
            hasConstantValue = isConstant(context);
            if (hasConstantValue) {
                constantValue = getValueBody(context, source);
            }
        }
        return hasConstantValue ? constantValue : getValueBody(context, source);
    }

    protected void evaluateSetValueBody (ExprContext context, Object target, Object value)
    throws ExprException
    {
        context.setCurrentObject(target);
        context.setCurrentNode(this);
        setValueBody(context, target, value);
    }

    public final Object getValue (ExprContext context, Object source) throws ExprException
    {
        Object result = null;

        if (context.getTraceEvaluations()) {
            EvaluationPool  pool = ExprRuntime.getEvaluationPool();
            Throwable       evalException = null;
            Evaluation      evaluation = pool.create(this, source);

            context.pushEvaluation(evaluation);
            try {
                result = evaluateGetValueBody(context, source);
            }
            catch (ExprException ex) {
                evalException = ex;
                throw ex;
            }
            catch (RuntimeException ex) {
                evalException = ex;
                throw ex;
            }
            finally {
                Evaluation      eval = context.popEvaluation();

                eval.setResult(result);
                if (evalException != null) {
                    eval.setException(evalException);
                }
                if ((evalException == null) && (context.getRootEvaluation() == null)
                  && !context.getKeepLastEvaluation()) {
                    pool.recycleAll(eval);
                }
            }
        }
        else {
            result = evaluateGetValueBody(context, source);
        }

        traceEvaluation(result);

        return result;
    }

    /**
     * Subclasses implement this method to do the actual work of extracting the
     * appropriate value from the source object.
     */
    protected abstract Object getValueBody (ExprContext context, Object source)
    throws ExprException;

    public final void setValue (ExprContext context, Object target, Object value)
    throws ExprException
    {
        if (context.getTraceEvaluations()) {
            EvaluationPool      pool = ExprRuntime.getEvaluationPool();
            Throwable           evalException = null;
            Evaluation          evaluation = pool.create(this, target, true);

            context.pushEvaluation(evaluation);
            try {
                evaluateSetValueBody(context, target, value);
            }
            catch (ExprException ex) {
                evalException = ex;
                ex.setEvaluation(evaluation);
                throw ex;
            }
            catch (RuntimeException ex) {
                evalException = ex;
                throw ex;
            }
            finally {
                Evaluation      eval = context.popEvaluation();

                if (evalException != null) {
                    eval.setException(evalException);
                }
                if ((evalException == null) && (context.getRootEvaluation() == null)
                  && !context.getKeepLastEvaluation()) {
                    pool.recycleAll(eval);
                }
            }
        }
        else {
            evaluateSetValueBody(context, target, value);
        }
    }

    /** Subclasses implement this method to do the actual work of setting the
        appropriate value in the target object.  The default implementation
        throws an <code>InappropriateExpressionException</code>, meaning that it
        cannot be a set expression.
     */
    protected void setValueBody (ExprContext context, Object target, Object value)
    throws ExprException
    {
        throw new InappropriateExpressionException( this );
    }

    /**
        Returns true iff this node is constant without respect to the children.
     */
    public boolean isNodeConstant (ExprContext context) throws ExprException
    {
        return false;
    }

    public boolean isConstant (ExprContext context) throws ExprException
    {
        return isNodeConstant(context);
    }

    public boolean isNodeSimpleProperty (ExprContext context) throws ExprException
    {
        return false;
    }

    public boolean isSimpleProperty (ExprContext context) throws ExprException
    {
        return isNodeSimpleProperty(context);
    }

    public boolean isSimpleNavigationChain (ExprContext context) throws ExprException
    {
        return isSimpleProperty(context);
    }

      /**
       * This method may be called from subclasses' jjtClose methods.  It flattens the
       * tree under this node by eliminating any children that are of the same class as
       * this node and copying their children to this node.
       */
    protected void flattenTree ()
    {
        boolean shouldFlatten = false;
        int newSize = 0;

        for ( int i=0; i < children.length; ++i ) {
            if ( children[i].getClass() == getClass() ) {
                shouldFlatten = true;
                newSize += children[i].jjtGetNumChildren();
            }
            else {
                ++newSize;
            }
        }

        if ( shouldFlatten )
          {
            Node[] newChildren = new Node[newSize];
            int j = 0;

            for ( int i=0; i < children.length; ++i ) {
                Node c = children[i];
                if ( c.getClass() == getClass() ) {
                    for ( int k=0; k < c.jjtGetNumChildren(); ++k ) {
                        newChildren[j++] = c.jjtGetChild(k);
                    }
                }
                else {
                    newChildren[j++] = c;
                }
            }

            if (j != newSize) {
                throw new Error( "Assertion error: " + j + " != " + newSize );
            }

            this.children = newChildren;
          }
    }

    protected void acceptChildren (ASTNodeVisitor visitor)
    {
        if (!ArrayUtil.nullOrEmptyArray(children)) {
            for (int i=0; i < children.length; i++) {
                children[i].accept(visitor);
            }
        }
    }

    public void accept (ASTNodeVisitor visitor)
    {
        acceptChildren(visitor);
        visitor.visit(this);
    }

    public TypeInfo getTypeInfo ()
    {
        return _typeInfo;
    }

    public void setTypeInfo (TypeInfo typeInfo)
    {
        _typeInfo = typeInfo;
    }

//
// Ariba's expression tracing subsystem.
//

    /** Logger for expression tracing */
    private static final Logger TRACER = Log.exprTrace;

    /** Level at which expression logging is enabled */
    private static final Priority TRACE_LEVEL = Priority.INFO;

    /** Level at which expression logging debugging is enabled */
    private static final Priority DEBUG_LEVEL = Priority.DEBUG;

    /** "Normal" value field size -- smaller sized output is padded to this length */
    private static final String TRACE_VALUE_PADDING = "                ";

    /** Text output when field/value can not be determined */
    public static final String TRACE_UNKNOWN = "-???-";

    /** Text output when value is <code>null</code> */
    public static final String TRACE_NULL = "-null-";

    /**
     * Trace the progress of the current expression's evaluation.
     * <p/>
     * OVERRIDE iff you want non-standard behavior.
     * For example, if you do not want a subclass to display its result, use code similar to:
     * <pre>
     *    {@code @Override}
     *    protected void traceEvaluation (final Object result)
     *    {
     *        // do not trace this node
     *    }
     * </pre>
     * Examples of AST* subclasses which do this are:
     * <ul>
     *   <li>{@link ASTChain} - don't trace symbol table walk, only final value</li>
     *   <li>{@link ASTConst} - never trace: it's name is its value</li>
     *   <li>{@link ASTList} - never trace: all non-constant values already shown</li>
     *   <li>{@link ASTProperty} - trace only simple values (numbers, boolean, etc.)</li>
     * </ul>
     *
     * @param result evaluation result; may be {@code null}
     */
    protected void traceEvaluation (final Object result)
    {
        if (!SimpleNode.isExpressionTracingEnabled()) {
            return;
        }

        final String value = (result == null) ? SimpleNode.TRACE_NULL : result.toString();
        final StringBuilder text
        = new StringBuilder((this instanceof ASTProperty) ? "PROP::" : "");

        if (result instanceof CharSequence) {
            text.append('"').append(value).append('"');
        }
        else {
            text.append(value);
        }

        final int textLength = text.length();

        if (textLength < SimpleNode.TRACE_VALUE_PADDING.length()) {
            text.append(SimpleNode.TRACE_VALUE_PADDING.substring(textLength));
        }
        SimpleNode.traceExpression(text.append("  == ").append(this.toString()));

        // additional output for debugging the trace output logic itself
        if (SimpleNode.isExpressionDebuggingEnabled()) {
            /*
             * Some AST* nodes are known to behave well in trace.
             * Extend the hideTypeInfo expression as additional nodes appear in the trace
             *  for which the trace output is deemed appropriate.
             */
            final boolean hideTypeInfo = (this instanceof ExpressionNode
                                         || this instanceof ASTChain
                                         || this instanceof ASTIn
                                         || this instanceof ASTMethod
                                         || this instanceof ASTProperty);

            if (!hideTypeInfo) {
                final String type = (result == null)
                                    ? SimpleNode.TRACE_UNKNOWN
                                    : result.getClass().getCanonicalName();

                text.setLength(0);
                text.append("types: ").append(type).append(" from ");
                text.append(this.getClass().getCanonicalName()).toString();
                SimpleNode.debugExpression(text);
            }
        }
    }

    /**
     * Determine if expression tracing is enabled.
     *
     * @return {@code true} iff expression tracing is enabled
     */
    public static final boolean isExpressionTracingEnabled ()
    {
        return SimpleNode.TRACER.isEnabledFor(SimpleNode.TRACE_LEVEL);
    }

    /**
     * Log an expression trace.
     * Nothing logged if {@code null} text or tracing level insufficient.
     *
     * @param msg text to output; may be {@code null} or empty
     */
    public static final void traceExpression (final CharSequence msg)
    {
        if (msg != null) {
            SimpleNode.TRACER.log(SimpleNode.TRACE_LEVEL, msg);
        }
    }

    /**
     * Determine if expression tracing debugging is enabled.
     *
     * @return {@code true} iff expression tracing debugging is enabled
     */
    private static final boolean isExpressionDebuggingEnabled ()
    {
        return SimpleNode.TRACER.isEnabledFor(SimpleNode.DEBUG_LEVEL);
    }

    /**
     * Log an expression debug trace.
     * Nothing logged if {@code null} text or tracing level insufficient.
     *
     * @param msg text to output; may be {@code null} or empty
     */
    private static final void debugExpression (final CharSequence msg)
    {
        if (msg != null) {
            SimpleNode.TRACER.log(SimpleNode.DEBUG_LEVEL, msg);
        }
    }

    /**
     * Determine if a value is simple enough that it should be traced.
     * Generally this means numbers, characters, strings and booleans, since more complex
     *  objects need to be further processed.
     * <code>null</code>s are always traced.
     *
     * @param value value being considered for tracing
     * @return {@code true} iff <em>value</em> should be output/traced
     */
    static final boolean shouldTraceValue (final Object value)
    {
        return (value == null)
         || (value instanceof CharSequence) || (value instanceof Boolean)
         || (value instanceof Number) || (value instanceof Character);
    }
}
