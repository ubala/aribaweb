/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/expr/ariba/util/expr/SymbolValidator.java#11 $
*/

package ariba.util.expr;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.fieldtype.FieldInfo;
import ariba.util.fieldtype.MethodInfo;
import ariba.util.fieldtype.PropertyInfo;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldvalue.Expression;
import java.util.List;

/**
    @aribaapi private
*/
public class SymbolValidator extends ASTNodeVisitor
{
    private Environment _env;
    private Node        _tree;
    private SymbolTable _symbolTable;
    private List        _errorCollector;

    protected final static int SymbolUsageRead = 1;
    protected final static int SymbolUsageWrite = 2;

   /////////////////////////////////////////////////////////////////

    public SymbolValidator (Environment env,
                     Expression expression,
                     List errorCollector)
    {
        Assert.that(expression instanceof AribaExprEvaluator.Expression,
            "Expression is an Ariba Expression.");

        AribaExprEvaluator.Expression aExpr = (AribaExprEvaluator.Expression)expression;
        Assert.that(aExpr.getRootNode() != null && aExpr.getSymbolTable() != null,
            "Expression is not a compiled expression.");

        _env = env;
        _tree = aExpr.getRootNode();
        _symbolTable = aExpr.getSymbolTable();
        _errorCollector = errorCollector;
    }

    public void validate ()
    {
        _tree.accept(this);
    }

    /////////////////////////////////////////////////////////////////

    protected Environment getEnvironment ()
    {
        return _env;
    }

    protected Node getRootNode ()
    {
        return _tree;
    }

    protected SymbolTable getSymbolTable ()
    {
        return _symbolTable;
    }

    protected List getErrorCollector ()
    {
        return _errorCollector;
    }

    /////////////////////////////////////////////////////////////////

    public void visit (ASTChain node)
    {
        node.acceptChildren(this);
    }

    public void visit (ASTProject node)
    {
        node.acceptChildren(this);
    }

    protected void doVisit (Node node)
    {
        printVisitNode(node);
    }

    protected void doVisit (ASTProperty node)
    {
        printVisitNode(node);

        // Look up the semantic record for the node
        SemanticRecord record = _symbolTable.getSymbolRecord(node);
        if (record == null) {
            // not a known symbol, just return.
            return;
        }
        // Verify that the node is a field node (versus Type or Variable)
        Integer symbolKind = record.getSymbolKind();
        if (symbolKind == Symbol.Field) {
            PropertyInfo property = record.getPropertyInfo();
            if (property != null) {
                if (property instanceof FieldInfo) {
                     validate((FieldInfo)property,
                         (isLHS(node) ? SymbolUsageWrite : SymbolUsageRead));
                     return;
                }
                else if (property instanceof MethodInfo) {
                    validate((MethodInfo)property);
                    return;
                }
            }

            addError(property, Fmt.S("The symbol '%s' does not resolve to a field or method.",
                        node));
        }
    }

    private boolean isLHS (ASTProperty node)
    {
        boolean isLHS = false;
        Node parent = node.jjtGetParent();

        if (parent instanceof ASTAssign) {
            // property = value
            isLHS = (node.equals(parent.jjtGetChild(0)));
        }
        else if (parent instanceof ASTChain &&
                 parent.jjtGetParent() instanceof ASTAssign) {
            // property1.property2 = value
            // property usage = read, property usage = write

            // grandParent is ASTAssign
            Node grandParent = parent.jjtGetParent();
            isLHS = (parent.equals(grandParent.jjtGetChild(0)));
        }
        else if (parent instanceof ASTChain &&
                 parent.jjtGetParent() instanceof ASTChain &&
                 parent.jjtGetParent().jjtGetChild(1) instanceof ASTIndex &&
                 parent.jjtGetParent().jjtGetParent() instanceof ASTAssign) {
            // property1.property2[0] = value
            // property usage = read, property usage = write

            // grandParent is ASTChain, ancestor is ASTAssign
            Node grandParent = parent.jjtGetParent();
            Node ancestor = parent.jjtGetParent().jjtGetParent();
            isLHS = (grandParent.equals(ancestor.jjtGetChild(0)));
        }

        return isLHS;
    }

    protected void doVisit (ASTMethod node)
    {
        printVisitNode(node);
        node.acceptChildren(this);

        // Look up the semantic record for the node
        SemanticRecord record = _symbolTable.getSymbolRecord(node);
        if (record == null) {
            return;
        }

        // Verify that the node is a method node
        Integer symbolKind = record.getSymbolKind();
        if (symbolKind == Symbol.Method) {
            PropertyInfo property = record.getPropertyInfo();
            if (property == null || !(property instanceof MethodInfo)) {
                addError(property, Fmt.S("The symbol '%s' does not resolve to a method.",
                    getMethodNameStr(node)));
                return;
            }
            validate((MethodInfo)property);
        }
    }

    private String getMethodNameStr (ASTMethod node)
    {
        FastStringBuffer buffer = new FastStringBuffer(200);
        buffer.append(node.getMethodName());
        buffer.append("(");
        for (int i=0; i < node.jjtGetNumChildren(); i++) {
            Node child = node.jjtGetChild(i);
            if (i > 0) {
                buffer.append(", ");
            }
            TypeInfo type = child.getTypeInfo();
            // Just print the arugment type without the argument name.  The
            // argument name can be an expression and it would be hard to read.
            buffer.append(type != null ? type.getName() : "<type unknown>");
        }
        buffer.append(")");

        return buffer.toString();
    }


    protected void validate (FieldInfo field, int usage)
    {
        ;
    }


    protected void addError (PropertyInfo property, String errorStr)
    {
        printDebugStr("Validation Error | " +
                     (property != null ? property.getName() : "") +
                     " | " + errorStr);
        _errorCollector.add(errorStr);
    }

    private void printDebugStr (String debugStr)
    {
        // ToDo
        //System.out.println(debugStr);
        //System.out.flush();
    }

    private void printVisitNode (Node node)
    {
       printDebugStr("SymbolValidator : Visiting Node = " + getNodeStr(node));
    }

    private String getNodeStr (Node node)
    {
        return ("(" + node.getClass().getName() + "," + node.hashCode() + ")" +
                " : Parent( " +
                (node.jjtGetParent() != null ?
                    node.jjtGetParent().getClass().getName() +
                    "," + node.jjtGetParent().hashCode() : "") + ")");
    }

    protected void validate (MethodInfo method)
    {
        List reservedNames = TypeChecker.getReservedMethodNames();

        int fieldAccess = method.getAccessibility();
        if (fieldAccess < TypeInfo.AccessibilitySafe &&
            !reservedNames.contains(method.getName())) {
            addError(method, Fmt.S("Method is not safe: %s",
                                   method.getName()));
        }
    }
}
