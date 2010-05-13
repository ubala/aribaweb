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

    $Id: //ariba/platform/util/expr/ariba/util/expr/AribaExprEvaluator.java#24 $
*/
package ariba.util.expr;

import ariba.util.fieldvalue.Expression;
import ariba.util.fieldvalue.ExpressionEvaluator;
import ariba.util.fieldvalue.ExpressionException;
import ariba.util.fieldvalue.ExpressionEvaluatorException;
import ariba.util.fieldvalue.FieldValue;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import ariba.util.core.ListUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Assert;
import ariba.util.core.StringUtil;
import ariba.util.fieldtype.NullTypeInfo;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldtype.JavaTypeRegistry;
import ariba.util.fieldtype.PropertyInfo;
import ariba.util.fieldtype.TypeRetriever;

/**
    Factory for Expr-based expressions
*/

public class AribaExprEvaluator extends ExpressionEvaluator
{
    private static AribaExprEvaluator _instance;

    public static AribaExprEvaluator instance ()
    {
        if (_instance == null) {
            _instance = new AribaExprEvaluator();
        }
        return _instance;
    }

    /**
     * This method parses a string expression into an expression tree.  This
     * method should only be used for unit test.  Use compile(String) instead.
     * @param stringRepresentation
     * @return an expression tree
     * @throws ExpressionException
     */
    public ariba.util.fieldvalue.Expression parse (
                                             String stringRepresentation)
        throws ExpressionException
    {
        return parse(null, stringRepresentation, ListUtil.list());
    }

    /**
     * This method parses a string expression into an expression tree.  This
     * method should only be used for unit test.  Use compile(String) instead.
     * @param stringRepresentation
     * @return an expression tree
     * @throws ExpressionException
     */
    public ariba.util.fieldvalue.Expression parse (
                                             Environment env,
                                             String stringRepresentation,
                                             List errorCollector)
        throws ExpressionException
    {
        Node node = null;
        List errors = getErrorCollector(env, errorCollector);
        try {
            if (StringUtil.nullOrEmptyOrBlankString(stringRepresentation)) {
                return new Expression(null);
            }
            node = (Node)Expr.parseExpression (stringRepresentation);
        } catch (ExpressionSyntaxException e) {
            errors.add(e.toString());
        }

        return (ListUtil.nullOrEmptyList(errors) ? new Expression(node) : null);
    }

    public ariba.util.fieldvalue.Expression compile (String stringRepresentation)
        throws ExpressionException
    {
        Environment env = new Environment(JavaTypeRegistry.instance());
        ariba.util.fieldvalue.Expression expr = compile(env, null, null,
            null, null, false, stringRepresentation, env.getErrorCollector());
        if (!ListUtil.nullOrEmptyList(env.getErrorCollector())) {
            throw new ExpressionEvaluatorException(
                getErrorString(env.getErrorCollector()));
        }
        return expr;
    }

    public ariba.util.fieldvalue.Expression compile (
                                                 Environment env,
                                                 String rootType,
                                                 String fieldName,
                                                 String stringRepresentation)
        throws ExpressionException
    {
        return compile (env, rootType, fieldName, null, null, false,
                        stringRepresentation, env.getErrorCollector());
    }

    public ariba.util.fieldvalue.Expression compile (
                                                 Environment env,
                                                 String rootType,
                                                 String fieldName,
                                                 String expectedType,
                                                 String containerType,
                                                 boolean exactMatch,
                                                 String stringRepresentation,
                                                 List errorCollector)
        throws ExpressionException
    {
        Expression expr = (Expression)parse(env,
                                            stringRepresentation,
                                            errorCollector);
        if (expr != null) {
            typeCheck(env, rootType, fieldName, expr, expectedType, containerType,
                      exactMatch, errorCollector);
            Node exprRootNode = expr.getRootNode();
            if (exprRootNode != null &&
                (exprRootNode.getTypeInfo() == null ||
                    exprRootNode.getTypeInfo() instanceof NullTypeInfo)) {
                TypeRetriever retriever = env.getTypeRetriever();
                TypeInfo typeInfo = (!StringUtil.nullOrEmptyOrBlankString(expectedType) ?
                                 retriever.getTypeInfo(expectedType) :
                                 null);
                exprRootNode.setTypeInfo(typeInfo);

            }
        }
        return expr;
    }

    public ariba.util.fieldvalue.Expression compile (
                                                 Environment env,
                                                 String rootType,
                                                 String stringRepresentation)
        throws ExpressionException
    {
        return compile(env, rootType, null, null, null, false,
                       stringRepresentation, env.getErrorCollector());
    }

    public ariba.util.fieldvalue.Expression compile (
                                                 Environment env,
                                                 String rootType,
                                                 String expectedType,
                                                 String containerType,
                                                 boolean exactMatch,
                                                 String stringRepresentation)
        throws ExpressionException
    {
        return compile(env, rootType, null, expectedType, containerType, exactMatch,
                       stringRepresentation, env.getErrorCollector());
    }

    /*
    public ariba.util.fieldvalue.Expression compile (
                                                 Environment env,
                                                 String rootType,
                                                 String fieldName,
                                                 String stringRepresentation)
        throws ExpressionException
    {
        return compile(env, rootType, fieldName, stringRepresentation, env.getErrorCollector());
    }
    */

    public void typeCheck ( Environment env,
                            String rootType,
                            String fieldName,
                            ariba.util.fieldvalue.Expression expression,
                            String expectedType,
                            String containerType,
                            boolean exactMatch,
                            List errorCollector)
        throws ExpressionException
    {
        Assert.that(expression instanceof Expression,
                 "Expression is not compiled from Ariba expression language.");
        Expression aExpr = (Expression)expression;
        if (aExpr._exprNode != null) {
            SymbolTable table = TypeChecker.check(env, rootType, fieldName,
                expectedType, containerType, exactMatch, aExpr._exprNode, errorCollector);
            aExpr.setSymbolTable(table);
	    }
    }

    /**
     * Check the return type of the expression.
     * @param env
     * @param expression
     * @param expectedType
     * @param containerType
     * @param exactMatch
     * @param skipForObjectType
     */
    public void checkReturnType (Environment env,
                                 ariba.util.fieldvalue.Expression expression,
                                 String expectedType,
                                 String containerType,
                                 boolean exactMatch,
                                 boolean skipForObjectType)
    {
        Assert.that(expression instanceof Expression,
                 "Expression is not compiled from Ariba expression language.");
        Expression aExpr = (Expression)expression;
        if (aExpr._exprNode != null) {
            TypeChecker.verifyReturnedType(env,
                aExpr._exprNode, expectedType, containerType, exactMatch,
                env.getErrorCollector(), skipForObjectType);
	    }
    }

    private List getErrorCollector (Environment env, List errorCollector)
    {
        return (!ListUtil.nullOrEmptyList(errorCollector) ?
                errorCollector : (env != null ? env.getErrorCollector() :
                ListUtil.list()));
    }

    private String getErrorString (List errors)
    {
        FastStringBuffer buffer = new FastStringBuffer(200);
        for (int i=0; i < errors.size(); i++) {
            buffer.append("Error :\n");
            buffer.append(errors.get(i).toString());
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /**
        @aribaapi ariba
    */
    public static TypeInfo getTypeInfo (ariba.util.fieldvalue.Expression expr)
    {
        if (expr instanceof Expression) {
            Node node = ((Expression)expr).getRootNode();
            return node != null ? node.getTypeInfo() : null;
        }
        return null;
    }

/*
    protected static class Accessor implements ExprContext.FieldAccessor
    {
        FieldValue _fieldValueProtocol;

        public Accessor (FieldValue proto)
        {
            _fieldValueProtocol = proto;
        }

        public Object getFieldValue (Object target, String key)
        {
            FieldValue proto = (_fieldValueProtocol != null) ? _fieldValueProtocol : FieldValue.get(target.getClass());
            FieldValueGetter getter = (FieldValueGetter)proto.getAccessor(target, key, FieldValue.Getter);
            return (getter != null) ? getter.getValue(target) : null;
        }

        public boolean setFieldValue (Object target, String key, Object value)
        {
            FieldValue proto = (_fieldValueProtocol != null) ? _fieldValueProtocol : FieldValue.get(target.getClass());
            FieldValueSetter setter = (FieldValueSetter)proto.getAccessor(target, key, FieldValue.Setter);
            if (setter != null) {
                setter.setValue(target, value);
                return true;
            }
            return false;
        }
    }

    protected static Accessor _DefaultAccessor = new Accessor(null);
*/
    public static class Expression extends ariba.util.fieldvalue.Expression
    {
        Node _exprNode;
        SymbolTable _symbolTable;

        public Expression (Node node)
        {
            _exprNode = node;
            // System.out.println("$$$ Expression: " + toString());
            // System.out.println("--- fieldPaths: " + getFieldPaths().toString());
            // printExprTree();
        }

        public Node getRootNode ()
        {
            return _exprNode;
        }

        public void setSymbolTable (SymbolTable symbolTable)
        {
            _symbolTable = symbolTable;
        }

        public SymbolTable getSymbolTable ()
        {
            return _symbolTable;
        }

        public List getFieldsInExpr ()
        {
            List result = ListUtil.list();
            Collection allFields = _symbolTable.getSymbolRecords(Symbol.Field);
            if (allFields != null) {
                Iterator iter = allFields.iterator();
                while (iter.hasNext()) {
                    SemanticRecord record = (SemanticRecord)iter.next();
                    PropertyInfo info = record.getPropertyInfo();
                    if (info != null) {
                        result.add(info);
                    }
                }
            }
            return result;
        }


        public List getMethodsInExpr ()
        {
            List result = ListUtil.list();
            Collection allMethods = _symbolTable.getSymbolRecords(Symbol.Method);
            if (allMethods != null) {
                Iterator iter = allMethods.iterator();
                while (iter.hasNext()) {
                    SemanticRecord record = (SemanticRecord)iter.next();
                    PropertyInfo info = record.getPropertyInfo();
                    if (info != null) {
                        result.add(info);
                    }
                }
            }
            return result;
        }

        public boolean isOnlyFieldInPath (String symbolName)
        {
            SemanticRecord record = _symbolTable.getSymbolRecord(
                                            symbolName, Symbol.Field);
            if (record == null || record.getSymbolKind() != Symbol.Field) {
                return false;
            }

            String context = record.getContextSymbolName();
            if (!StringUtil.nullOrEmptyOrBlankString(context)) {
                Collection allRecords = _symbolTable.getSymbolRecordsForContext(context);
                if (allRecords != null) {
                    Iterator iter = allRecords.iterator();
                    while (iter.hasNext()) {
                        SemanticRecord childRecord = (SemanticRecord)iter.next();
                        if (childRecord.getSymbolKind() == Symbol.Field &&
                            !childRecord.getSymbolName().equals(symbolName)) {
                            return false;
                        }
                    }
                }

                return true;
            }

            return true;
        }

        public Set getAllPaths ()
        {
            return _symbolTable.getAllContext();
        }

        /**
         * Get all the semantic records for this path.  The semantic records
         * contain parsed information about each element in the path.  The
         * returned is ordered based on the position of the element in the
         * path.  A path element can be a field or a method.  
         * @param path
         * @return list of records
         */
        public List getSemanticRecordsInPath (String path)
        {
            return _symbolTable.getSymbolRecordsForContext(path);
        }

        public List getAllSemanticRecordsInPath (String path)
        {
            List result = ListUtil.list();
            _getSemanticRecordsInEnclosingPath(path, null, result);
            return result;
        }

        private void _getSemanticRecordsInEnclosingPath (String path,
                                                         SemanticRecord pivot,
                                                         List result)
        {
            List <SemanticRecord> records = getSemanticRecordsInPath(path);
            if (!ListUtil.nullOrEmptyList(records)) {
                SemanticRecord record = records.get(0);
                // Get the enclosing lexical scope from the first element in the
                // path.  Typically, it is a method or projection.
                Node enclosingScope = record.getExtendedFieldPathNode();
                if (enclosingScope instanceof Symbol) {
                    SemanticRecord scope =
                          _symbolTable.getSymbolRecord((Symbol)enclosingScope);
                    // Get the context semantic record
                    _getSemanticRecordsInEnclosingPath(
                            scope.getContextSymbolName(), scope, result);
                }
                if (pivot == null) {
                    result.addAll(records);
                }
                else {
                    for (SemanticRecord r : records) {
                        if (r == pivot) {
                            break;
                        }
                        result.add(r);
                    }
                }
            }
        }

        public List getFieldsInPath (String path)
        {
            List result = ListUtil.list();
            Collection allFields = getSemanticRecordsInPath(path);
            if (allFields != null) {
                Iterator iter = allFields.iterator();
                while (iter.hasNext()) {
                    SemanticRecord record = (SemanticRecord)iter.next();
                    PropertyInfo info = record.getPropertyInfo();
                    if (info != null && record.getSymbolKind() == Symbol.Field) {
                        result.add(info);
                    }
                }
            }
            return result;
        }

        protected ExprContext createContext (Object object, FieldValue fieldValueProtocol)
        {
            ExprContext context = (ExprContext)Expr.createDefaultContext(object, _symbolTable);
            // context.setFieldAccessor((fieldValueProtocol == null) ? _DefaultAccessor : new Accessor(fieldValueProtocol));
            return context;
        }

        public Object evaluate (Object object, FieldValue fieldValueProtocol) throws ExpressionEvaluatorException
        {
            try {
                if (_exprNode == null) {
                    return null;
                }
                return Expr.getValue(_exprNode, createContext(object, fieldValueProtocol), object);
            } catch (ExprException e) {
                throw new ExpressionEvaluatorException(e);
            }
        }

        public Object evaluate (Object object,
                                String expectedType,
                                FieldValue fieldValueProtocol)
            throws ExpressionEvaluatorException
        {
            try {
                if (_exprNode == null) {
                    return null;
                }
                Object value = Expr.getValue(_exprNode, createContext(object, fieldValueProtocol), object);
                if (!StringUtil.nullOrEmptyOrBlankString(expectedType) &&
                    value != null) {
                    value = TypeConversionHelper.convertPrimitive(expectedType, value);
                }
                return value;
            } catch (ExprException e) {
                throw new ExpressionEvaluatorException(e);
            }
        }

        public void evaluateSet (Object object, Object value, FieldValue fieldValueProtocol) throws ExpressionEvaluatorException
        {
            try {
                if (_exprNode == null) {
                    return;
                }
                Expr.setValue(_exprNode, createContext(object, fieldValueProtocol), object, value);
            } catch (ExprException e) {
                throw new ExpressionEvaluatorException(e);
            }
        }

        public Collection/*<String>*/ getFieldPaths ()
        {
            // ToDo -- should cache!
            return computeFieldPaths();
        }

        public String toString ()
        {
            return (_exprNode != null ? _exprNode.toString() : "");
        }

        protected Collection computeFieldPaths ()
        {
            // ToDo: bogus passing this for object...
            final ExprContext context = createContext(this, null);
            final Set paths = new HashSet();
            final List path = ListUtil.list();
            visitNodes(_exprNode, new NodeVisitor() {
               public Object visit (Node node) {
                   boolean didPush = false;
                   try {
                       if (node instanceof SimpleNode) {
                           if (((SimpleNode)node).isNodeSimpleProperty(context)) {
                               path.add(node.toString());
                               paths.add(ListUtil.listToString(path, "."));
                               didPush = true;
                           }
                       }
                   } catch (ExprException e) {
                       throw new ExpressionException(e);
                   }
                   visitChildren();
                   if (didPush) ListUtil.removeLastElement(path);
                   return null;
               }
            });
            return paths;
        }

        public void printExprTree ()
        {
            // ToDo: bogus passing this for object...
            final ExprContext context = createContext(this, null);
            visitNodes(_exprNode, new NodeVisitor() {
               int level;
               public Object visit (Node node) {
                   try {
                       String str = spaces(level*2) + node.getClass().getName();
                       if (node instanceof SimpleNode) {
                           if (((SimpleNode)node).isConstant(context)) {
                               str += " " + node.toString();
                           }
                       }
                       System.out.println(str);
                   } catch (ExprException e) {
                       throw new ExpressionException(e);
                   }
                   level++;
                   visitChildren();
                   level--;
                   return null;
               }
            });
        }
    }

    protected static String spaces (int n)
    {
        FastStringBuffer buf = new FastStringBuffer(n);
        while (n-- >= 0) buf.append(' ');
        return buf.toString();
    }

    public abstract static class NodeVisitor
    {
        protected Node _currentNode;

        public abstract Object visit (Node node);

        public Object visitChildren ()
        {
            Object result = null;
            Node parent = _currentNode;
            for (int i=0, c=_currentNode.jjtGetNumChildren(); i < c; i++) {
                _currentNode = parent.jjtGetChild(i);
                Object r = visit(_currentNode);
                if (r != null) result = r;
            }
            _currentNode = parent;
            return result;
        }
    }

    public static Object visitNodes (Node root, NodeVisitor visitor)
    {
        if (root == null) {
            return null;
        }
        visitor._currentNode = root;
        return visitor.visit(root);
    }
}
