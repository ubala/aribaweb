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

import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;

import java.io.StringReader;
import java.util.Map;

/**
 * <P>This class provides static methods for parsing and interpreting AribaExpr expressions.</P>
 *
 * <P>The simplest use of the Expr class is to get the value of an expression from
 * an object, without extra context or pre-parsing.</P>
 *
 * <PRE>
 * import ariba.util.expr.Expr;
 * import ariba.util.expr.ExprException;
 *
 *    try {
 *        result = Expr.getValue(expression, root);
 *    } catch (ExprException ex) {
 *       // Report error or recover
 *    }
 * </PRE>
 *
 * <P>This will parse the expression given and evaluate it against the root object
 * given, returning the result.  If there is an error in the expression, such
 * as the property is not found, the exception is encapsulated into an
 * {@link ariba.util.expr.ExprException ExprException}.</P>
 *
 * <P>Other more sophisticated uses of Expr can pre-parse expressions.  This
 * provides two advantages: in the case of user-supplied expressions it
 * allows you to catch parse errors before evaluation and it allows you to
 * cache parsed expressions into an AST for better speed during repeated use.
 * The pre-parsed expression is always returned as an <CODE>Object</CODE>
 * to simplify use for programs that just wish to store the value for
 * repeated use and do not care that it is an AST.  If it does care
 * it can always safely cast the value to an <CODE>AST</CODE> type.</P>
 *
 * <P>The Expr class also takes a <I>context map</I> as one of the parameters
 * to the set and get methods.  This allows you to put your own variables
 * into the available namespace for AribaExpr expressions.  The default context
 * contains only the <CODE>THIS</CODE> and <CODE>#context</CODE> keys,
 * which are required to be present.  The <CODE>addDefaultContext(Object, Map)</CODE>
 * method will alter an existing <CODE>Map</CODE> to put the defaults in.
 * Here is an example that shows how to extract the <CODE>documentName</CODE>
 * property out of the root object and append a string with the current user
 * name in parens:</P>
 *
 * <PRE>
 *     private Map	context = new HashMap();
 *
 *     public void setUserName(String value)
 *     {
 *         context.put("userName", value);
 *     }
 *
 *     try {
 *        // get value using our own custom context map
 *        result = Expr.getValue("documentName + \" (\" + ((#userName == null) ? \"&lt;nobody&gt;\" : #userName) + \")\"", context, root);
 *     } catch (ExprException ex) {
 *         // Report error or recover
 *     }
 *
 * </PRE>
 *
 * @author Luke Blanshard (blanshlu@netscape.net)
 * @author Drew Davidson (drew@expr.org)
 * @version 27 June 1999
 */
public abstract class Expr 
{
      private static final String stringTable = "ariba.util.expression";
      private static final String key = "MalformedExpression";

      /**
       * Parses the given AribaExpr expression and returns a tree representation of the
       * expression that can be used by <CODE>Expr</CODE> static methods.
       *
       * @param expression the AribaExpr expression to be parsed
       * @return a tree representation of the expression
       * @throws ExpressionSyntaxException if the expression is malformed
       * @throws ExpressionSyntaxException if there is a pathological environmental problem
       */
    public static Object parseExpression( String expression )
          throws ExpressionSyntaxException
    {
        try {
            ExprParser parser = new ExprParser( new StringReader(expression) );
            return parser.topLevelExpression();
        }
        catch (ParseException e) {
            Log.expression.debug(e.toString());
            Token token = e.currentToken.next;
            if (StringUtil.nullOrEmptyOrBlankString(token.image)) {
                token = e.currentToken;
            }
            String msg = Fmt.Sil(stringTable, key, token.image,
                token.beginLine, token.beginColumn);
            throw new ExpressionSyntaxException(msg, expression, null );
        }
        catch (TokenMgrError e) {
            Log.expression.debug(e.toString());
            throw new ExpressionSyntaxException(e.getMessage(), expression, null );
        }
    }

      /**
       * Creates and returns a new standard naming context for evaluating an AribaExpr
       * expression.
       *
       * @param root the root of the object graph
       * @return a new Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map createDefaultContext( Object root )
    {
        return addDefaultContext( root, null, null, null, new ExprContext() );
    }

    /**
       * Creates and returns a new standard naming context for evaluating an AribaExpr
       * expression.
       *
       * @param root the root of the object graph
       * @return a new Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map createDefaultContext( Object root, SymbolTable symbolTable )
    {
        return addDefaultContext( root, null, null, null, new ExprContext(symbolTable) );
    }

      /**
       * Creates and returns a new standard naming context for evaluating an AribaExpr
       * expression.
       *
       * @param root the root of the object graph
       * @return a new ExprContext with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map createDefaultContext( Object root, ClassResolver classResolver )
    {
        return addDefaultContext( root, classResolver, null, null, new ExprContext() );
    }

      /**
       * Creates and returns a new standard naming context for evaluating an AribaExpr
       * expression.
       *
       * @param root the root of the object graph
       * @return a new Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map createDefaultContext( Object root, ClassResolver classResolver, TypeConverter converter )
    {
        return addDefaultContext( root, classResolver, converter, null, new ExprContext() );
    }

      /**
       * Creates and returns a new standard naming context for evaluating an AribaExpr
       * expression.
       *
       * @param root the root of the object graph
       * @return a new Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map createDefaultContext( Object root, ClassResolver classResolver, TypeConverter converter, MemberAccess memberAccess )
    {
        return addDefaultContext( root, classResolver, converter, memberAccess, new ExprContext() );
    }

      /**
       * Appends the standard naming context for evaluating an AribaExpr expression
       * into the context given so that cached maps can be used as a context.
       *
       * @param root the root of the object graph
       * @param context the context to which AribaExpr context will be added.
       * @return Context Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map addDefaultContext( Object root, Map context )
    {
    	return addDefaultContext( root, null, null, null, context );
    }

      /**
       * Appends the standard naming context for evaluating an AribaExpr expression
       * into the context given so that cached maps can be used as a context.
       *
       * @param root the root of the object graph
       * @param context the context to which AribaExpr context will be added.
       * @return Context Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map addDefaultContext( Object root, ClassResolver classResolver, Map context )
    {
        return addDefaultContext(root, classResolver, null, null, context);
    }

      /**
       * Appends the standard naming context for evaluating an AribaExpr expression
       * into the context given so that cached maps can be used as a context.
       *
       * @param root the root of the object graph
       * @param context the context to which AribaExpr context will be added.
       * @return Context Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map addDefaultContext( Object root, ClassResolver classResolver, TypeConverter converter, Map context )
    {
        return addDefaultContext(root, classResolver, converter, null, context);
    }

      /**
       * Appends the standard naming context for evaluating an AribaExpr expression
       * into the context given so that cached maps can be used as a context.
       *
       * @param root the root of the object graph
       * @param context the context to which AribaExpr context will be added.
       * @return Context Map with the keys <CODE>root</CODE> and <CODE>context</CODE>
       *         set appropriately
       */
    public static Map addDefaultContext( Object root,
                                         ClassResolver classResolver,
                                         TypeConverter converter,
                                         MemberAccess memberAccess,
                                         Map context )
    {
        ExprContext     result;

        if (!(context instanceof ExprContext)) {
            result = new ExprContext();
            result.setValues(context);
        } else {
            result = (ExprContext)context;
        }
        if (classResolver != null) {
            result.setClassResolver(classResolver);
        }
        if (converter != null) {
            result.setTypeConverter(converter);
        }
        if (memberAccess != null) {
            result.setMemberAccess(memberAccess);
        }
        result.setRoot(root);
        return result;
    }

    public static void setClassResolver( Map context, ClassResolver classResolver)
    {
        context.put(ExprContext.CLASS_RESOLVER_CONTEXT_KEY, classResolver);
    }

    public static ClassResolver getClassResolver( Map context )
    {
        return (ClassResolver)context.get(ExprContext.CLASS_RESOLVER_CONTEXT_KEY);
    }

    public static void setTypeConverter( Map context, TypeConverter converter)
    {
        context.put(ExprContext.TYPE_CONVERTER_CONTEXT_KEY, converter);
    }

    public static TypeConverter getTypeConverter( Map context )
    {
        return (TypeConverter)context.get(ExprContext.TYPE_CONVERTER_CONTEXT_KEY);
    }

	public static void setMemberAccess(Map context, MemberAccess memberAccess)
	{
	    context.put(ExprContext.MEMBER_ACCESS_CONTEXT_KEY, memberAccess);
	}

	public static MemberAccess getMemberAccess(Map context)
	{
	    return (MemberAccess)context.get(ExprContext.MEMBER_ACCESS_CONTEXT_KEY);
	}

    public static void setRoot( Map context, Object root)
    {
        context.put(ExprContext.ROOT_CONTEXT_KEY, root);
    }

    public static Object getRoot( Map context )
    {
        return context.get(ExprContext.ROOT_CONTEXT_KEY);
    }

    public static Evaluation getLastEvaluation( Map context )
    {
        return (Evaluation)context.get(ExprContext.LAST_EVALUATION_CONTEXT_KEY);
    }

      /**
       * Evaluates the given AribaExpr expression tree to extract a value from the given root
       * object. The default context is set for the given context and root via
       * <CODE>addDefaultContext()</CODE>.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param context the naming context for the evaluation
       * @param root the root object for the AribaExpr expression
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( Object tree, Map context, Object root ) throws ExprException
    {
        return getValue( tree, context, root, null );
    }

      /**
       * Evaluates the given AribaExpr expression tree to extract a value from the given root
       * object. The default context is set for the given context and root via
       * <CODE>addDefaultContext()</CODE>.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param context the naming context for the evaluation
       * @param root the root object for the AribaExpr expression
       * @param resultType the converted type of the resultant object, using the context's type converter
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( Object tree, Map context, Object root, Class resultType ) throws ExprException
    {
        Object          result;
        ExprContext     exprContext = (ExprContext)addDefaultContext(root, context);

        result = ((Node)tree).getValue( exprContext, root );
        if (resultType != null) {
            result = getTypeConverter( context ).convertValue( context, root, null, null, result, resultType);
        }
        return result;
    }

      /**
       * Evaluates the given AribaExpr expression to extract a value from the given root
       * object in a given context
       *
       * @see #parseExpression(String)
       * @see #getValue(Object,Object)
       * @param expression the AribaExpr expression to be parsed
       * @param context the naming context for the evaluation
       * @param root the root object for the AribaExpr expression
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( String expression, Map context, Object root ) throws ExprException
    {
        return getValue( expression, context, root, null );
    }

      /**
       * Evaluates the given AribaExpr expression to extract a value from the given root
       * object in a given context
       *
       * @see #parseExpression(String)
       * @see #getValue(Object,Object)
       * @param expression the AribaExpr expression to be parsed
       * @param context the naming context for the evaluation
       * @param root the root object for the AribaExpr expression
       * @param resultType the converted type of the resultant object, using the context's type converter
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( String expression, Map context, Object root, Class resultType ) throws ExprException
    {
        return getValue( parseExpression(expression), context, root, resultType );
    }

      /**
       * Evaluates the given AribaExpr expression tree to extract a value from the given root
       * object.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param root the root object for the AribaExpr expression
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( Object tree, Object root ) throws ExprException
    {
        return getValue( tree, root, null );
    }

      /**
       * Evaluates the given AribaExpr expression tree to extract a value from the given root
       * object.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param root the root object for the AribaExpr expression
       * @param resultType the converted type of the resultant object, using the context's type converter
       * @return the result of evaluating the expression
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( Object tree, Object root, Class resultType ) throws ExprException
    {
        return getValue( tree, createDefaultContext(root), root, resultType );
    }

      /**
       * Convenience method that combines calls to <code> parseExpression </code> and
       * <code> getValue</code>.
       *
       * @see #parseExpression(String)
       * @see #getValue(Object,Object)
       * @param expression the AribaExpr expression to be parsed
       * @param root the root object for the AribaExpr expression
       * @return the result of evaluating the expression
       * @throws ExpressionSyntaxException if the expression is malformed
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( String expression, Object root ) throws ExprException
    {
        return getValue( expression, root, null );
    }

      /**
       * Convenience method that combines calls to <code> parseExpression </code> and
       * <code> getValue</code>.
       *
       * @see #parseExpression(String)
       * @see #getValue(Object,Object)
       * @param expression the AribaExpr expression to be parsed
       * @param root the root object for the AribaExpr expression
       * @param resultType the converted type of the resultant object, using the context's type converter
       * @return the result of evaluating the expression
       * @throws ExpressionSyntaxException if the expression is malformed
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static Object getValue( String expression, Object root, Class resultType ) throws ExprException
    {
        return getValue( parseExpression(expression), root, resultType );
    }

      /**
       * Evaluates the given AribaExpr expression tree to insert a value into the object graph
       * rooted at the given root object.  The default context is set for the given
       * context and root via <CODE>addDefaultContext()</CODE>.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param context the naming context for the evaluation
       * @param root the root object for the AribaExpr expression
       * @param value the value to insert into the object graph
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static void setValue( Object tree, Map context, Object root, Object value ) throws ExprException
    {
        ExprContext     exprContext = (ExprContext)addDefaultContext(root, context);
        Node            n = (Node) tree;

        n.setValue( exprContext, root, value );
    }

      /**
       * Evaluates the given AribaExpr expression to insert a value into the object graph
       * rooted at the given root object given the context.
       *
       * @param expression the AribaExpr expression to be parsed
       * @param root the root object for the AribaExpr expression
       * @param context the naming context for the evaluation
       * @param value the value to insert into the object graph
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static void setValue( String expression, Map context, Object root, Object value ) throws ExprException
    {
        setValue( parseExpression(expression), context, root, value );
    }

      /**
       * Evaluates the given AribaExpr expression tree to insert a value into the object graph
       * rooted at the given root object.
       *
       * @param tree the AribaExpr expression tree to evaluate, as returned by parseExpression()
       * @param root the root object for the AribaExpr expression
       * @param value the value to insert into the object graph
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static void setValue( Object tree, Object root, Object value ) throws ExprException
    {
        setValue( tree, createDefaultContext(root), root, value );
    }

      /**
       * Convenience method that combines calls to <code> parseExpression </code> and
       * <code> setValue</code>.
       *
       * @see #parseExpression(String)
       * @see #setValue(Object,Object,Object)
       * @param expression the AribaExpr expression to be parsed
       * @param root the root object for the AribaExpr expression
       * @param value the value to insert into the object graph
       * @throws ExpressionSyntaxException if the expression is malformed
       * @throws MethodFailedException if the expression called a method which failed
       * @throws NoSuchPropertyException if the expression referred to a nonexistent property
       * @throws InappropriateExpressionException if the expression can't be used in this context
       * @throws ExprException if there is a pathological environmental problem
       */
    public static void setValue( String expression, Object root, Object value ) throws ExprException
    {
        setValue( parseExpression(expression), root, value );
    }

    public static boolean isConstant( Object tree, Map context ) throws ExprException
    {
        return ((SimpleNode)tree).isConstant( (ExprContext)addDefaultContext( null, context ) );
    }

    public static boolean isConstant( String expression, Map context ) throws ExprException
    {
        return isConstant(parseExpression(expression), context);
    }

    public static boolean isConstant( Object tree ) throws ExprException
    {
        return isConstant(tree, createDefaultContext(null));
    }

    public static boolean isConstant( String expression ) throws ExprException
    {
        return isConstant(parseExpression(expression), createDefaultContext(null));
    }

    public static boolean isSimpleProperty( Object tree, Map context ) throws ExprException
    {
        return ((SimpleNode)tree).isSimpleProperty( (ExprContext)addDefaultContext( null, context ) );
    }

    public static boolean isSimpleProperty( String expression, Map context ) throws ExprException
    {
        return isSimpleProperty(parseExpression(expression), context);
    }

    public static boolean isSimpleProperty( Object tree ) throws ExprException
    {
        return isSimpleProperty(tree, createDefaultContext(null));
    }

    public static boolean isSimpleProperty( String expression ) throws ExprException
    {
        return isSimpleProperty(parseExpression(expression), createDefaultContext(null));
    }

    public static boolean isSimpleNavigationChain( Object tree, Map context ) throws ExprException
    {
        return ((SimpleNode)tree).isSimpleNavigationChain( (ExprContext)addDefaultContext( null, context ) );
    }

    public static boolean isSimpleNavigationChain( String expression, Map context ) throws ExprException
    {
        return isSimpleNavigationChain(parseExpression(expression), context);
    }

    public static boolean isSimpleNavigationChain( Object tree ) throws ExprException
    {
        return isSimpleNavigationChain(tree, createDefaultContext(null));
    }

    public static boolean isSimpleNavigationChain( String expression ) throws ExprException
    {
        return isSimpleNavigationChain(parseExpression(expression), createDefaultContext(null));
    }

      /** You can't make one of these. */
    private Expr()
    {
    }
}
