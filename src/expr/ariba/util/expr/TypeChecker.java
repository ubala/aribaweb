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

    $Id: //ariba/platform/util/expr/ariba/util/expr/TypeChecker.java#38 $
*/

package ariba.util.expr;

import ariba.util.core.ArithmeticOperations;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.SetUtil;
import ariba.util.core.StringUtil;
import ariba.util.fieldtype.ContainerTypeInfo;
import ariba.util.fieldtype.FieldInfo;
import ariba.util.fieldtype.MethodInfo;
import ariba.util.fieldtype.NullTypeInfo;
import ariba.util.fieldtype.PrimitiveTypeProvider;
import ariba.util.fieldtype.PropertyInfo;
import ariba.util.fieldtype.PropertyResolver;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldtype.TypeRetriever;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
    @aribaapi private
*/
public class TypeChecker extends ASTNodeVisitor
{
    private static final String stringTable = "ariba.util.expression";
    private static final String key = "MalformedExpression";

    private Environment _env;
    private TypeInfo    _rootType;
    private String      _thisField;
    private Map         _semanticRecordMap;
    private List        _contextStack;
    private SymbolTable _symbolTable;
    private List        _unresolvedPropertyList;
    private String      _firstUnresolvedProperty;
    private List        _errorCollector;

    /* _nodeValueMap is a map between a node in the syntax tree with
       a constant node (ASTConst, ASTMap, ASTList).
     */
    private Map         _nodeValueMap;

    /* _constants contains the set of constant nodes in the syntax tree.
       Constant Nodes are ASTConst, ASTMap and ASTList.  For ASTMap and
       ASTList, it may contain child nodes that are not ASTConst.
     */
    private Set         _constants;

    private static final List ReservedVarNames = ListUtil.list(
        ASTThisFieldVarRef.Name,
        ASTRootVarRef.Name,
        ASTThisVarRef.Name
    );

    private static final String[][] ReservedMethodNames = {
        {"isNull", "ariba.util.expr.ExprOps", "isNull"}
    };

    private static final List NonAssignableVarNames = ListUtil.list(
        ASTRootVarRef.Name,
        ASTThisVarRef.Name
    );


   /////////////////////////////////////////////////////////////////

    TypeChecker (Environment env,
                 TypeInfo type,
                 String thisField,
                 Map semanticRecordMap,
                 List errorCollector)
    {
        _env = env;
        _rootType = type;
        _thisField = thisField;
        _semanticRecordMap = semanticRecordMap;
        _symbolTable = new SymbolTable();
        _contextStack = new Stack();
        _unresolvedPropertyList = ListUtil.list();
        _firstUnresolvedProperty = null;
        _errorCollector = errorCollector;
        _nodeValueMap = MapUtil.map();
        _constants = SetUtil.set();
    }

    void check (Node tree)
    {
        _errorCollector.clear();
        beginLexicalScope(_rootType, tree);
        tree.accept(this);

        // check for variables that have unresolved type
        Collection allVariables = _symbolTable.getSymbolRecords(Symbol.Variable);
        if (hasRootType() && allVariables != null) {
            Iterator iter = allVariables.iterator();
            while (iter.hasNext()) {
                SemanticRecord record = (SemanticRecord)iter.next();
                if (record.getTypeInfo() == null) {
                    addError(record.getNode(),
                        Fmt.S("Cannot resolved type for Variable '%s'.",
                            record.getSymbolName()));
                }
            }
        }
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     Node tree,
                                     List errorCollector)
    {
        return check(env, rootType, tree, MapUtil.map(), errorCollector);
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     String fieldName,
                                     String expectedType,
                                     String containerType,
                                     boolean exactMatch,
                                     Node tree,
                                     List errorCollector)
    {
        return check(env, rootType, fieldName, expectedType, containerType, exactMatch,
                     tree, MapUtil.map(), errorCollector);
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     Node tree)
    {
        return check(env, rootType, tree, MapUtil.map(), env.getErrorCollector());
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     Node tree,
                                     Map semanticRecordMap)
    {
        return check(env, rootType, tree, semanticRecordMap, env.getErrorCollector());
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     String fieldName,
                                     Node tree,
                                     Map semanticRecordMap)
    {
        return check(env, rootType, fieldName, null, null, false,
                     tree, semanticRecordMap, env.getErrorCollector());
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     Node tree,
                                     Map semanticRecordMap,
                                     List errorCollector)
    {
        return check(env, rootType, null, null, null, false,
                     tree, semanticRecordMap, errorCollector);
    }

    public static SymbolTable check (Environment env,
                                     String rootType,
                                     String thisField,
                                     String expectedType,
                                     String containerType,
                                     boolean exactMatch,
                                     Node tree,
                                     Map semanticRecordMap,
                                     List errorCollector)
    {
        TypeRetriever retriever = env.getTypeRetriever();
        TypeInfo type = (!StringUtil.nullOrEmptyOrBlankString(rootType) ?
                         retriever.getTypeInfo(rootType) :
                         null);
        TypeChecker checker = new TypeChecker(env, type, thisField,
                                     semanticRecordMap, errorCollector);
        checker.check(tree);

        if (rootType != null) {
            verifyReturnedType (env, tree, expectedType,
                                containerType, exactMatch, errorCollector, true);
        }
        return checker._symbolTable;
    }


    protected static List getReservedMethodNames ()
    {
        List result = ListUtil.list();
        if (ReservedMethodNames != null) {
            int length = ReservedMethodNames.length;
            for (int i=0; i < length; i++) {
                result.add(ReservedMethodNames[i][0]);
            }
        }
        return result;
    }

    public static void verifyReturnedType (Environment env,
                                            Node tree,
                                            String expectedType,
                                            String containerType,
                                            boolean exactMatch,
                                            List errors,
                                            boolean skipForObjectClass)
    {
         boolean verifyReturnType = env.getBooleanEnvVariable(
                                             Environment.CheckReturnType, true);
         if (!StringUtil.nullOrEmptyOrBlankString(expectedType) && verifyReturnType) {
             TypeRetriever retriever = env.getTypeRetriever();
             TypeInfo convertedTo = retriever.getTypeInfo(expectedType);
             if (convertedTo != null) {
                 TypeInfo convertedFrom = tree.getTypeInfo();

                 // The return type is expected to be a container type.
                 if (!StringUtil.nullOrEmptyOrBlankString(containerType)) {

                     // Get the expected container type
                     TypeInfo toContainerType = retriever.getTypeInfo(containerType);
                     if (toContainerType == null) {
                         errors.add (Fmt.S(
                             "Cannot find type '%s' when evaluating the return value of the expression.",
                             toContainerType));
                         return;
                     }

                     // If the actual type is not null type and it cannot cast
                     // to the expected container type.
                     if (!(convertedFrom instanceof NullTypeInfo) &&
                         !(TypeConversionHelper.canCastTo(toContainerType,
                                                          convertedFrom))) {
                         errors.add (Fmt.S(
                             "Returned Type expects to be type of '%s', but the actual type is '%s'.",
                             toContainerType, convertedFrom));
                         return;
                     }

                     // Get the element type of the container for further
                     // comparision.  ConvertedFrom.getElementType() returns
                     // null if it is not a container type.
                     convertedFrom = convertedFrom.getElementType();
                 }

                 if (convertedFrom != null) {
                     // If convertFrom is an object, then it implies that the
                     // type checker cannot reliably find out the resultant
                     // type of the expression.  For example, the expr may
                     // have use List or Map that always require explicit cast
                     // of its element.  Skip validation for now.
                     if (skipForObjectClass &&
                         convertedFrom.getName().equals(Object.class.getName())) {
                         return;
                     }

                     if (exactMatch &&
                         !TypeConversionHelper.exactMatch(convertedTo, convertedFrom)) {
                          errors.add( Fmt.S(
                             "Returned Type expects to be of type '%s', but the actual type is '%s'.",
                              expectedType, convertedFrom.getName()));
                     }
                     else if (!TypeConversionHelper.canCastTo(convertedTo, convertedFrom)) {
                          errors.add( Fmt.S(
                             "Returned Type expects to be of type '%s', but the actual type is '%s'. %s",
                              expectedType, convertedFrom.getName(),
                              "It is not possible to convert from the actual type to expected type."));
                     }
                 }
             }
         }
    }

    /////////////////////////////////////////////////////////////////

    protected void doVisit (SimpleNode node)
    {
        printVisitNode(node);
        endScopeIfNecessary(node);
    }

    protected void doVisit (ExpressionNode node)
    {
        printVisitNode(node);
        try {
            switch (node.getExpressionType())
            {
                case ExpressionNode.LogicalExpression :
                    handleLogicalExpr(node);
                    break;
                case ExpressionNode.ArithmeticExpression :
                    handleArithmeticExpr(node);
                    break;
                case ExpressionNode.RelationalExpression :
                    handleRelationalExpr(node);
                    break;
                case ExpressionNode.BitwiseExpression :
                    handleBitwiseExpr(node);
                    break;
                case ExpressionNode.ShiftExpression :
                    handleShiftExpr(node);
                    break;
                case ExpressionNode.ConditionalTestExpression :
                    handleConditionalTestExpr(node);
                    break;
                default:
                    break;
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void handleLogicalExpr (ExpressionNode node)
    {
        checkOperandsType(node, getTypeInfo(node, Boolean.class.getName()));
        addSemanticRecordToNode(node, getTypeInfo(node, Boolean.class.getName()));
    }

    protected void handleArithmeticExpr (ExpressionNode node)
    {
        // If it is an addition, then do not return error.  The Add operator
        // is overloaded as concatenation.
        TypeInfo operandTypeInfo = checkNumericOperands(node, true,
                                                       !(node instanceof ASTAdd));
        if (operandTypeInfo != null) {
            addSemanticRecordToNode(node, operandTypeInfo);
        }
        else if (node instanceof ASTAdd) {
            addSemanticRecordToNode(node, getTypeInfo(node, String.class.getName()));
        }
        else if (hasRootType()) {
            addError(node,
                Fmt.S("Cannot resolve type for expression '%s'.", node));
        }
    }

    protected void handleRelationalExpr (ExpressionNode node)
    {
        checkCompatibleOperands(node, true);
        addSemanticRecordToNode(node, getTypeInfo(node, Boolean.class.getName()));
    }

    protected void handleBitwiseExpr (ExpressionNode node)
    {
        TypeInfo operandTypeInfo = checkNumericOperands(node, false, true);
        if (operandTypeInfo != null) {
            addSemanticRecordToNode(node, operandTypeInfo);
        }
        else if (hasRootType()) {
            addError(node,
                Fmt.S("Cannot resolve type for expression '%s'.", node));
        }
    }

    protected void handleShiftExpr (ExpressionNode node)
    {
        TypeInfo operandTypeInfo = checkNumericOperands(node, false, true);
        if (operandTypeInfo != null) {
            addSemanticRecordToNode(node, operandTypeInfo);
        }
        else if (hasRootType()) {
            addError(node,
                Fmt.S("Cannot resolve type for expression '%s'.", node));
        }
    }

    protected void handleConditionalTestExpr (ExpressionNode node)
    {
        Node testNode = node.jjtGetChild(0);
        Node trueNode = node.jjtGetChild(1);
        Node falseNode = node.jjtGetChild(2);

        // condition must be boolean
        checkOperandsType(ListUtil.list(testNode), node,
                          getTypeInfo(node, Boolean.class.getName()));

        // branches must be compatible
        List operands = ListUtil.list(trueNode, falseNode);
        TypeInfo operandTypeInfo = checkCompatibleOperands(node, operands, true);
        if (operandTypeInfo != null) {
            addSemanticRecordToNode(node, operandTypeInfo);
        }
        else if (hasRootType()) {
            addError(node,
                Fmt.S("Cannot resolve type for expression '%s'.", node));
        }
    }

    private TypeInfo checkCompatibleOperands (Node node, boolean addError)
    {
        List operands = ListUtil.list();
        int numOfChild = node.jjtGetNumChildren();
        for (int i=0; i < numOfChild; i++) {
            operands.add(node.jjtGetChild(i));
        }

        return checkCompatibleOperands(node, operands, addError);
    }

    private TypeInfo checkCompatibleOperands (Node node, List operands, boolean addError)
    {
        TypeInfo operandTypeInfo = null;

        for (int i=0; i < operands.size(); i++) {
            Node child = (Node)operands.get(i);
            TypeInfo info = getTypeInfoForNode(child);
            if (info != null) {
                if (operandTypeInfo == null ||
                    operandTypeInfo instanceof NullTypeInfo) {
                        operandTypeInfo = info;
                }
                else if (!operandTypeInfo.isCompatible(info) &&
                         !info.isCompatible(operandTypeInfo)) {
                    if (addError) {
                        addError(node, Fmt.S(
                            "Types of operands in expression '%s' are not compatible.",
                            node));
                    }
                    return null;
                }
            }
            else {
                return null;
            }
        }

        return operandTypeInfo;
    }

    private TypeInfo checkNumericOperands (Node node,
                                           boolean allowFloatingPoint,
                                           boolean addError)
    {
        int numOfChild = node.jjtGetNumChildren();

        if (numOfChild > 0) {
            TypeInfo operandTypeInfo = null;

            for (int i=0; i < numOfChild; i++) {
                Node child = node.jjtGetChild(i);
                TypeInfo info = getTypeInfoForNode(child);
                if (info != null) {

                    // Is this a known numeric type or does it support
                    // arithmetic operations.
                    if (!PrimitiveTypeProvider.isNumericType(info) &&
                        !(info instanceof NullTypeInfo) &&
                        ArithmeticOperations.getByName(info.getName()) == null) {
                        if (addError) {
                            addError(node, Fmt.S(
                                "Operands in expression '%s' must be numeric.",
                                node));
                        }
                        return null;
                    }

                    // Is this a known floating point numeric?
                    if (PrimitiveTypeProvider.isNumericType(info)) {
                        // Do we allow floating point numeric?
                        if (!allowFloatingPoint &&
                             PrimitiveTypeProvider.isFloatingPointNumericType(info)) {
                             if (addError) {
                                 addError(node, Fmt.S(
                                    "Expression '%s' does not allow floating point operands.",
                                     node));
                             }
                            return null;
                        }
                    }

                    // Let's checkin if this operand is compatible with the others.
                    if (operandTypeInfo == null) {
                        operandTypeInfo = info;
                    }
                    else if (operandTypeInfo instanceof NullTypeInfo) {
                        operandTypeInfo = getNullArithmeticOperationsReturnType(node, info);
                    }
                    else if (PrimitiveTypeProvider.isNumericType(operandTypeInfo) &&
                             PrimitiveTypeProvider.isNumericType(info)) {
                        // If the operands are both known numeric type, then
                        // look for a coerced type that can be the resulting
                        // type of containing expression.
                        TypeInfo coercedTypeInfo =
                            PrimitiveTypeProvider.getCoercedType(operandTypeInfo, info);
                        if (coercedTypeInfo != null) {
                            operandTypeInfo = coercedTypeInfo;
                        }
                    }
                    else if (!(operandTypeInfo instanceof NullTypeInfo) &&
                             !(info instanceof NullTypeInfo)) {
                        // if any of the operands is non-numeric type, then
                        // look for the return type of the arithmetic operations
                        String operandType = getTypeName(operandTypeInfo);
                        String infoType = getTypeName(info);
                        ArithmeticOperations op = 
                            getArithmeticOperations(node, operandType, infoType);
                        if (op != null) {
                            Class operandTypeClass = ClassUtil.classForName(operandType, false);
                            Class infoTypeClass = ClassUtil.classForName(infoType, false);
                            if (operandTypeClass != null && infoTypeClass != null) {
                                Class resultClass = getArithmeticOperationsReturnTypeClass(
                                        node, op, operandTypeClass, infoTypeClass);                             
                                if (resultClass != null) {
                                    operandTypeInfo = getTypeInfo(resultClass.getName());   
                                }
                                else {
                                    return null;
                                }
                            }
                        }
                    }
                }
                else {
                    return null;
                }
            }

            return operandTypeInfo;
        }
        return null;
    }
    
    /**
        Returns the {@link ArithmeticOperations} for the given
        <code>node</code> with operands with the given
        <code>type1</code> and <code>type2</code>.  Please note that
        this needs to be in sync with the way we get arithmetic
        operations when the actual operation happens in ExprOps.
    */
    private ArithmeticOperations getArithmeticOperations (
            Node node, String type1, String type2)
    {
        ArithmeticOperations result = null;
        if (node instanceof ASTAdd ||
            node instanceof ASTSubtract) {
            result = ExprOps.getArithmeticOperations(type1, type2);         
        }
        else if (node instanceof ASTMultiply) {
            result = ExprOps.getArithmeticOperations(type1);
            if (result == null) {
                result = ExprOps.getArithmeticOperations(type2);
            }
        }
        else if (node instanceof ASTDivide) {
            result = ExprOps.getArithmeticOperations(type1);
        }
        return result;
    }
    
    /**
        Gets the type name for the given <code>info</code>.  If the
        given <code>info</code> is unboxed type, it returns the
        equivalent boxed type.
    */
    private String getTypeName (TypeInfo info)
    {
        if (!PrimitiveTypeProvider.isBoxedType(info)) {
            TypeInfo boxedTypeInfo = PrimitiveTypeProvider.getBoxedTypeInfo(info);
            if (boxedTypeInfo != null) {
                return boxedTypeInfo.getName();
            }
        }
        return info.getName();
    }

    //Null (operator) X is usually of X's type except for non primitive
    //types e.g. NULL/Money is double
    //@todo dlee any better way to optmize this?
    private TypeInfo getNullArithmeticOperationsReturnType (Node node, TypeInfo otherInfo)
    {
        TypeInfo result = otherInfo;
        if (node instanceof ASTDivide) {
            String infoType = getTypeName(otherInfo);
            ArithmeticOperations op = ExprOps.getArithmeticOperations(infoType);
            if (op != null) {
                Class infoTypeClass = ClassUtil.classForName(infoType, false);
                if (infoTypeClass != null) {
                    Class resultClass = getArithmeticOperationsReturnTypeClass(
                        node, op,
                        /*Null is convertible to Any type*/ infoTypeClass,
                        infoTypeClass);
                    if (resultClass != null) {
                        result = getTypeInfo(resultClass.getName());
                    }
                }
            }
        }
        return result;
    }

    private Class getArithmeticOperationsReturnTypeClass (
            Node node,
            ArithmeticOperations op,
            Class operandType1,
            Class operandType2) 
    {
        Class result = null;
        if (node instanceof ASTAdd) {
            result = op.additionReturnType(operandType1, operandType2);
        }
        else if (node instanceof ASTSubtract) {
            result = op.subtractionReturnType(operandType1, operandType2);
        }
        else if (node instanceof ASTMultiply) {
            result = op.multiplicationReturnType(operandType1, operandType2);
        }
        else if (node instanceof ASTDivide) {
            result = op.divisionReturnType(operandType1, operandType2);
        }
        return result;
    }

    private void checkOperandsType (Node node,
                                    TypeInfo operandTypeInfo)
    {
        List operands = ListUtil.list();
        int numOfChild = node.jjtGetNumChildren();
        for (int i=0; i < numOfChild; i++) {
            operands.add(node.jjtGetChild(i));
        }
        checkOperandsType(operands, node, operandTypeInfo);
    }

    private void checkOperandsType (List operands,
                                    Node node,
                                    TypeInfo operandTypeInfo)
    {
        for (int i=0; i < operands.size(); i++) {
            Node child = (Node)operands.get(i);
            TypeInfo info = getTypeInfoForNode(child);
            if (info != null) {
                if (!operandTypeInfo.isCompatible(info) &&
                    !info.isCompatible(operandTypeInfo)) {
                    addError(node, Fmt.S(
                        "Types of operands in expression is not '%s'.",
                        operandTypeInfo.getName()));
                    break;
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////

    protected void doVisit (ASTProject node)
    {
        printVisitNode(node);
        try {
            Assert.that(node.jjtGetParent() instanceof ASTChain,
                    "projection operation should be part of a property chain.");

            // The current type is null.  There is nothing else to check.
            if (getCurrentTypeInfo() == null) {
                return;
            }

            if (!(getCurrentTypeInfo() instanceof ContainerTypeInfo)) {
                addError(node, Fmt.S("%s%s%s%s'.",
                    "It is expected that type of '", node,
                    "' is an array or collection.  The actual type is '",
                    getCurrentTypeInfo().getName()));
                return;
            }

            // Get the element type of the container.   The element type is
            // the root type of the sub-expression in the projection.
            ContainerTypeInfo containerType = (ContainerTypeInfo)getCurrentTypeInfo();
            TypeInfo elementType = containerType.getElementType();
            beginLexicalScope(elementType, node);

            try {
                // Iterate through the children.
                int numOfChild = node.jjtGetNumChildren();
                TypeInfo result = null;
                for (int i=0; i < numOfChild; i++) {
                    Node child = node.jjtGetChild(i);

                    // resolve the type of this child
                    child.accept(this);
                }
            }
            finally {
                endScopeIfNecessary(node);
            }

            // Now check the project methods
            String methodName = node.getMethodName();
            if (methodName.equals(ASTProject.CollectMethod)) {
                handleCollectMethod(node);
            }
            else if (methodName.equals(ASTProject.FindAllMethod)) {
                handleFindAllMethod(node, elementType);
            }
            else if (methodName.equals(ASTProject.FindMethod)) {
                handleFindMethod(node, elementType);
            }
            else if (methodName.equals(ASTProject.SumMethod) ||
                     methodName.equals(ASTProject.AvgMethod) ||
                     methodName.equals(ASTProject.MinMethod) ||
                     methodName.equals(ASTProject.MaxMethod)) {
                handleAggregateMethod(node);
            }
            else {
                addError(node, Fmt.S(
                "Unknown projection method '%s'.  %s", methodName,
                "Allowed Methods are collect, findAll, find, sum, avg, min, max."));
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    private void handleFindAllMethod (ASTProject node, TypeInfo elementType)
    {
        // The child of a projection node is an expression.  The expression
        // selects the all elements in the projection list that satisifes
        // this condition.  The result of "FindAll" is ContainerTypeInfo whose
        // element type is the element type of the project list.
        if (elementType != null) {
            TypeInfo listType = getTypeInfo("java.util.ArrayList");
            ContainerTypeInfo containerType =
                new ContainerTypeInfo(listType, elementType);
            addSemanticRecordToNode(node, containerType, null,
                    Symbol.ProjectionFindAll, getFullPath(node));
        }
    }

    private void handleCollectMethod (ASTProject node)
    {
        // The child of a projection node is an expression.  Collect method
        // evaluates the expression for each element in the projection list.
        // The result of "Collect" is list of evaluation results.
        Node expr = node.jjtGetChild(0);
        if (expr != null) {
            // Find out the result type of the expression
            SemanticRecord record = getSemanticRecordForNode(expr);
            TypeInfo elementType = (record != null ? record.getTypeInfo() : null);
            TypeInfo listType = getTypeInfo("java.util.ArrayList");
            if (elementType != null) {
                ContainerTypeInfo containerType =
                    new ContainerTypeInfo(listType, elementType);
                addSemanticRecordToNode(node, containerType, null,
                        Symbol.ProjectionCollect, getFullPath(node));
            }
        }
    }

    private void handleFindMethod (ASTProject node, TypeInfo elementType)
    {
        // The child of a projection node is an expression.  The expression
        // selects the first element in the projection list that satisifes
        // this condition.  The result of "Find" is the type of the element
        // in the project list.
        if (elementType != null) {
            addSemanticRecordToNode(node, elementType, null,
                    Symbol.ProjectionFind, getFullPath(node));
        }
    }

    private void handleAggregateMethod (ASTProject node)
    {
        TypeInfo operandTypeInfo = checkNumericOperands(node, true, true);
        if (operandTypeInfo != null) {
            addSemanticRecordToNode(node, operandTypeInfo, null,
                    Symbol.ProjectionAggregate, getFullPath(node));
        }
    }

     /////////////////////////////////////////////////////////////////

    protected void doVisit (ASTConst node)
    {
        printVisitNode(node);
        try {
            Object value = node.getValue();
            if (value != null) {
                String className = value.getClass().getName();
                TypeInfo info = getTypeInfo(node, className);
                if (info != null) {
                    addSemanticRecordToNode(node, info);
                }
            }
            else {
                addSemanticRecordToNode(node, NullTypeInfo.instance);
            }

            // Register that this node is a constant.
            _constants.add(node);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTVarDecl node)
    {
        printVisitNode(node);
        try {
            // The type checker does not differentiate between variable
            // definition and variable reference.
            addSemanticRecordToNode(node, null, null, Symbol.Variable, null);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTProperty node)
    {
        printVisitNode(node);
        try {

            String name = ((ASTConst)node.jjtGetChild(0)).getValue().toString();
            Assert.that(!StringUtil.nullOrEmptyOrBlankString(name),
                "Property Name is missing.");

            if (!ListUtil.nullOrEmptyList(_unresolvedPropertyList)) {
                // If the preceding property of this chain is not known,
                // then cannot resolve this property either.  So add this
                // property to the unresolved list.
                _unresolvedPropertyList.add(node.getName());
                return;
            }

            TypeInfo type = getCurrentTypeInfo();
            TypeInfo fieldType = getFieldTypeInfo(node, type, name);
            if (fieldType != null) {
                // This property is a known field type for the class.
                addSemanticRecordToNode(node, fieldType,
                                        getPropertyInfo(type, name),
                                        Symbol.Field,
                                        getFullPath(node));
            }
            else if (ReservedVarNames.indexOf(name) != -1) {
                // Reserved word such as "this" and "it" will be handled by
                // ASTThis and ASTIt.  If these symbols are encountered here,
                // they are embedded within the property chain.
                addError(node, Fmt.S("%s %s",
                    "Fail to find field '%s' in type '%s'.",
                    "Field '%s' is a reserved word which should be used at the beginning of a field path."));
            }
            else if (isFirstInPropertyChain(node) &&
                     (type = getTypeInfo(node, name)) != null) {
                // This symbol represents a type alias.
                addSemanticRecordToNode(node, type, null, Symbol.Type, null);
            }
            else if (isVariableSymbol(node)) {
                // This is a variable.
                SemanticRecord record = getSemanticRecordForSymbol(
                                              node.getName(), Symbol.Variable);
                if (record != null && record.getSymbolKind() == Symbol.Variable) {
                    // It is a known variable.  There is no scoping rule
                    // for variable. So for the symbol table, the latest type
                    // info of the variable takes over.
                    TypeInfo info = (record != null ? record.getTypeInfo() : null);
                    addSemanticRecordToNode(node, info, null, Symbol.Variable, null);

                    // If this varaible is associated with a constant value,
                    // then associate this node with that constant.
                    setNodeConstantValue(node, record.getNode());
                }
            }
            else if (predecessorHasConstantValue(node)) {
                // If the predecessor of this property is a map constant, then
                // this property may specify the key to lookup the map element.
                Node value = getValueFromConstantPredecessor(node);
                TypeInfo info = getTypeInfoForNodeValue(value);

                addSemanticRecordToNode(node, info, null, Symbol.Key, getFullPath(node));
                setNodeConstantValue(node, value);
            }
            else if (isInPropertyChain(node)) {
                // If this property is part of a property chain but we cannot
                // find out what the type is, remember this property as an
                // unresolved property.   As the chain is navigated, this
                // property name will be used to determine if the name indeed not
                // a property but part of the class name.
                _unresolvedPropertyList.add(node.getName());
                if (_firstUnresolvedProperty == null) {
                    if (type != null && fieldType == null &&
                        !isFirstInPropertyChain(node)) {
                        //we are in a propertyChain and predecessors are resolved already
                        //and it is not a field log an error
                        //e.g. cus_myField.Integer.numberOfLeadingZeros(4)
                        Log.expression.error(10950,getFullPath(node));
                    }
                    _firstUnresolvedProperty = node.getName();
                }
            }
            else  {
                if (!hasRootType()) {
                    // Unknown property, default it a field when there is no
                    // root type.
                    addSemanticRecordToNode(node, null, null, Symbol.Field, null);
                }
                else {
                    addError(node,
                      Fmt.S("Cannot lookup type for field '%s'.", name));
                }
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean isInPropertyChain (Node node)
    {
        return (node.jjtGetParent() instanceof ASTChain);
    }

    private SemanticRecord getSemanticRecordForPredecessorInPropertyChain (Node node)
    {
        SemanticRecord record = null;
        if (isInPropertyChain(node)) {
            ASTChain parent = (ASTChain)node.jjtGetParent();
            Node predecessor = parent.jjtGetChild(0);
            if (predecessor != null && predecessor != node) {
                record = getSemanticRecordForNode(predecessor);
            }
        }
        return record;
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean isFirstInPropertyChain (ASTProperty node)
    {
        Node parent = node.jjtGetParent();
        if (parent != null && parent instanceof ASTChain) {
            return (node == parent.jjtGetChild(0));
        }

        return false;
    }

    /**
     *
     * @param node
     * @return
     */
    private boolean isVariableSymbol (ASTProperty node)
    {
       if (isFirstInPropertyChain(node) || !isInPropertyChain(node))
       {
           SemanticRecord record = getSemanticRecordForSymbol(
                                             node.getName(), Symbol.Variable);
           if (record != null && record.getSymbolKind() == Symbol.Variable) {
               return true;
           }
       }

        return false;
    }

    /**
     * This method finds out if the Predecessor node of this property
     * has a constant map value.
     * @param node
     * @return
     */
    private boolean predecessorHasConstantValue (ASTProperty node)
    {
       if (!isInPropertyChain(node)) {
           return false;
       }
       else {
           ASTChain parent = (ASTChain)node.jjtGetParent();
           Node predecessor = parent.jjtGetChild(0);
           Node value = getNodeConstantValue(predecessor);
           // The predecessor needs to be a map if this node is a property.
           return (value instanceof ASTMap);
       }
    }

    /**
     * If the precdecessor of <code>node</code> has a constant map value, this
     * method retrieves a map element using the given proeprty as the map key.
     * @param node
     * @return
     */
    private Node getValueFromConstantPredecessor (ASTProperty node)
    {
        Node result = null;
        ASTChain parent = (ASTChain)node.jjtGetParent();
        Node predecessor = parent.jjtGetChild(0);
        Object predecessorVal = getNodeConstantValue(predecessor);
        if (predecessorVal instanceof ASTMap) {
            ASTMap map = (ASTMap)predecessorVal;
            result = getValueWithKey(map, node.getName());
        }

        return result;
    }

    /**
     * This method finds out if the Predecessor node has map TypeInfo.
     * @param node
     * @return
     */
    private boolean predecessorHasMap (Node node)
    {
       if (!(node.jjtGetParent() instanceof ASTChain)) {
           return false;
       }
       else {
           ASTChain parent = (ASTChain)node.jjtGetParent();
           Node predecessor = parent.jjtGetChild(0);
           SemanticRecord record = getSemanticRecordForNode(predecessor);
           if (record != null) {
               TypeInfo type = record.getTypeInfo();
               if (type != null) {
                   TypeInfo mapType = getTypeInfo("java.util.Map");
                   if (mapType.isAssignableFrom(type)) {
                       return true;
                   }
               }
           }
           return predecessorHasMap(predecessor);
       }
    }

    /**
     * This method return a map element with key matching <code>name</code>.
     * If the map key is not a constant literal, then this method return null.
     * @param name
     * @return
     */
    private Node getValueWithKey (ASTMap map, String name)
    {
        for ( int i=0; i < map.jjtGetNumChildren(); ++i ) {
             ASTKeyValue     kv = (ASTKeyValue)map.jjtGetChild(i);
             Node key = kv.getKey();
             // Based on grammar, key is a literal constant, but we'll call
             // getNodeConstantValue anyway.
             Node keyVal = getNodeConstantValue(key);
             if (keyVal instanceof ASTConst &&
                ((ASTConst)key).getValue().equals(name)) {
                 Node constant = getNodeConstantValue(kv.getValue());
                 return (constant != null ? constant : kv.getValue());
             }
        }

        return null;
    }

    /**
     * For the node value returned from getValueForConstantPredecessor, this
     * method looks up the type fo the node value.
     * @param value
     * @return
     */
    private TypeInfo getTypeInfoForNodeValue (Node value)
    {
        TypeInfo info = null;
        if (value != null) {
            SemanticRecord record = getSemanticRecordForNode(value);
            if (record != null) {
                info = record.getTypeInfo();
            }
        }
        return info;
    }

    /**
     *
     * @param node
     * @return
     */
    private String getPropertyChainStr (Node node)
    {
        if ((node instanceof ASTProperty ||
            node instanceof ASTChain ||
            node instanceof ASTMethod) &&
            node.jjtGetParent() instanceof ASTChain) {
            return getFullPath(node);
        }
        return null;
    }

    private String getFullPath (Node node)
    {
        if (node.jjtGetParent() instanceof ASTChain) {
            return getFullPath(node.jjtGetParent());
        }
        return node.toString();
    }

    protected void doVisit (ASTIndex node)
    {
        printVisitNode(node);
        try {
            // There is not type info.  So there is nothing to check.
            if (getCurrentTypeInfo() == null) {
                return;
            }

            if (!(getCurrentTypeInfo() instanceof ContainerTypeInfo)) {
                addError(node, Fmt.S("%s%s%s%s'.",
                    "It is expected that type of '", node,
                    "' is an array or collection.  The actual type is '",
                    getCurrentTypeInfo().getName()));
                return;
            }

            // Element type at the index
            TypeInfo elementType = null;

            // If the predecessor of the index is a list constant, then try
            // to get the actual element type if this index is a constant.
            if (predecessorHasConstantValue(node)) {
                // getValueFromConstantPredecessor may return null if the
                // index is not a constant.
                Node value = getValueFromConstantPredecessor(node);
                if (value != null) {
                    elementType = getTypeInfoForNodeValue(value);
                    setNodeConstantValue(node, value);
                }
            }

            // If we do not know the element type, then see if the list
            // contains the element type.
            if (elementType == null) {
                // Get the element type of the container.   The element type is
                // the root type of the child node in the parse tree.
                ContainerTypeInfo containerType = (ContainerTypeInfo)getCurrentTypeInfo();
                elementType = containerType.getElementType();
            }

            addSemanticRecordToNode(node, elementType);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    /**
     * This method finds out if the Predecessor node of this index (a list)
     * has a constant list value.
     * @param node
     * @return
     */
    private boolean predecessorHasConstantValue (ASTIndex node)
    {
        ASTChain parent = (ASTChain)node.jjtGetParent();
        Node predecessor = parent.jjtGetChild(0);
        Node value = getNodeConstantValue(predecessor);
        // The predecessor needs to be a list if this node is an index.
        return (value instanceof ASTList);
    }

    /**
     * If the precdecessor of <code>node</code> has a constant list value, this
     * method retrieves a list element based on the index value of
     * <code>node</code>.  If this index is a dynamic index, then this method
     * returns null.
     * @param node
     * @return
     */
    private Node getValueFromConstantPredecessor (ASTIndex node)
    {
        Node result = null;
        ASTChain parent = (ASTChain)node.jjtGetParent();
        Node predecessor = parent.jjtGetChild(0);
        Object predecessorVal = getNodeConstantValue(predecessor);
        if (predecessorVal instanceof ASTList) {
            ASTList list = (ASTList)predecessorVal;
            Node index = getNodeConstantValue(node.jjtGetChild(0));
            if (index instanceof ASTConst &&
                ((ASTConst)index).getValue() instanceof Number) {
                Number indexVal = (Number)((ASTConst)index).getValue();
                result = list.jjtGetChild(indexVal.intValue());
                result = (getNodeConstantValue(result) != null ?
                          getNodeConstantValue(result) : result);
            }
        }

        return result;
    }

    protected void doVisit (ASTVarRef node)
    {
        printVisitNode(node);
        try {
            SemanticRecord record = getSemanticRecordForSymbol(
                                           node.getName(), Symbol.Variable);
            TypeInfo info = (record != null ? record.getTypeInfo() : null);
            addSemanticRecordToNode(node, info, null, Symbol.Variable, null);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTRootVarRef node)
    {
        printVisitNode(node);
        try {
            addSemanticRecordToNode(node, _rootType,  null,
                    Symbol.This, getFullPath(node));
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTThisVarRef node)
    {
        printVisitNode(node);
        try {

            addSemanticRecordToNode(node, getCurrentTypeInfo());
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTThisFieldVarRef node)
    {
        printVisitNode(node);
        try {
            if (_thisField == null) {
                addError(node, Fmt.S("%s", "thisField is not allowed in expression"));
                return;
            }

            if (!hasRootType()) {
                addSemanticRecordToNode(node, null, _thisField, null,
                                        Symbol.ThisField, null);
                return;
            }

            FieldInfo field = _rootType.getField(_env.getTypeRetriever(),
                                                 _thisField);
            if (field == null) {
                addError(node, Fmt.S("Field %s does not exist in class %s.",
                               _thisField, _rootType.getName()));
                return;
            }
            TypeInfo fieldType = field.getType(_env.getTypeRetriever());
            if (fieldType != null) {
                addSemanticRecordToNode(node, fieldType, _thisField, null,
                                        Symbol.ThisField, null);
            }
            else {
                 addError(node,
                      Fmt.S("Cannot lookup type for symbol thisField from class '%s' and field '%s'.",
                            _rootType.getName(), _thisField));
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTLoginUserVarRef node)
    {
        printVisitNode(node);
        try {
            TypeRetriever retriever = _env.getTypeRetriever();

            TypeInfo type;
            String typeName = ExprContext.getLoginUserTypeName();
            if (typeName != null) {
                type = retriever.getTypeInfo(typeName);
                if (type == null) {
                    addError(node, Fmt.S("Cannot find login user type '%s'", typeName));
                }
            }
            else {
                type = NullTypeInfo.instance;
            }
            if (type != null) {
                addSemanticRecordToNode(node, type);
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTStaticField node)
    {
        printVisitNode(node);
        try {
            String className = node.getClassName();
            String fieldName = node.getFieldName();
            TypeInfo type = getTypeInfo(node, className);
            if (type != null) {
                TypeInfo fieldType = getFieldTypeInfo(node, type, fieldName);
                if (fieldType != null) {
                    addSemanticRecordToNode(node, fieldType);
                }
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTCtor node)
    {
        printVisitNode(node);
        try {
            String className = node.getClassName();
            TypeInfo type = getTypeInfo(node, className);
            if (type != null) {
                addSemanticRecordToNode(node, type);
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTStaticMethod node)
    {
        printVisitNode(node);
        try {
            String className = node.getClassName();
            String methodName = node.getMethodName();
            TypeInfo type = getTypeInfo(node, className);
            if (type != null) {
                setMethodReturnType(type, methodName, node);
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTMethod node)
    {
        printVisitNode(node);
        try {
            // Method parameters are evaluated based on the last lexical
            // scope.
            try {
               if (!ListUtil.nullOrEmptyList(_unresolvedPropertyList)) {
                    // If the preceding property of this chain is not known,
                    // then cannot resolve this property either.  So add this
                    // property to the unresolved list.
                    _unresolvedPropertyList.add(node.getName());
                    return;
                }

                TypeInfo type = getCurrentLexicalTypeInfo();
                beginLexicalScope(type, node);

                // Iterate through the paremeters
                int numOfChild = node.jjtGetNumChildren();
                for (int i=0; i < numOfChild; i++) {
                    Node child = node.jjtGetChild(i);

                    // resolve the type of this child
                    child.accept(this);
                }
            }
            finally {
                endScopeIfNecessary(node);
            }

            TypeInfo type = getCurrentTypeInfo();
            String methodName = node.getMethodName();
            setMethodReturnType(type, methodName, node);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    private void setMethodReturnType (TypeInfo type,
                                      String methodName,
                                      Node node)
    {
        MethodInfo method = null;

        // prepare the parameter list
        List parameters = ListUtil.list();
        int numOfChild = node.jjtGetNumChildren();
        for (int i=0; i < numOfChild; i++) {
            Node child = node.jjtGetChild(i);
            TypeInfo argType = getTypeInfoForNode(child);
            if (argType != null) {
                parameters.add(argType.getName());
            }
            else if (hasRootType()) {
                addError(node,
                    Fmt.S("Fail to resolve type for parameter '%s' in method %s.",
                        child, methodName));
                return;
            }
            else {
                parameters.add(NullTypeInfo.instance.getName());
            }
        }

        boolean found = false;

        // find the method if the parent type is known
        if (type != null) {
            // check if the method is in invoked in a class context
            SemanticRecord record = getSemanticRecordForPredecessorInPropertyChain(node);
            boolean staticOnly = (record != null && record.getSymbolKind() == Symbol.Type);
            method = type.getMethodForName(
                    _env.getTypeRetriever(), methodName, parameters, staticOnly);
            if (method != null) {
                registerMethodReturnType(node, method);
                found = true;
            }
            else if (staticOnly &&
                    type.getMethodForName(
                            _env.getTypeRetriever(), methodName, parameters, false) != null) {
                addError(node,
                    Fmt.S("Try to invoke a non-static method '%s' from a class context '%s'.",
                        methodName, record.getSymbolName()));
                return;
            }
        }

        if (method == null && ReservedMethodNames != null) {
            int length = ReservedMethodNames.length;
            for (int i=0; i < length; i++) {
                String name = ReservedMethodNames[i][0];
                String typename = ReservedMethodNames[i][1];
                String newMethodName = ReservedMethodNames[i][2];

                if (methodName.equals(name)) {
                    type = getTypeInfo(node, typename);
                    method = type.getMethodForName(
                            _env.getTypeRetriever(),
                            newMethodName,
                            parameters,
                            false);
                    if (method != null) {
                        registerMethodReturnType(node, method);
                        found = true;
                        break;
                    }
                }
            }
        }

        if (!found) {
            if (hasRootType()) {
              addError(node,
                  Fmt.S("Fail to find method %s(%s) in class %s.",
                      methodName, getParameterSignature(parameters), type.getName()));
            }
            else {
                // If the parent type  is not known, then the type of the method
                // return type is also unknown.
                addSemanticRecordToNode(node, null, null,
                                Symbol.Method,
                                getFullPath(node));
            }
        }
    }

    private static String getParameterSignature (List<String> parameters) 
    {
        FastStringBuffer buffer = new FastStringBuffer();
        if (!ListUtil.nullOrEmptyList(parameters)) {
            for (Iterator iter = parameters.iterator(); iter.hasNext();) {
                String parameter = (String)iter.next();
                if (StringUtil.nullOrEmptyOrBlankString(parameter)) {
                    parameter = "<unknown>";
                }
                buffer.append(parameter);
                if (iter.hasNext()) {
                    buffer.append(", ");
                }
            }
        }
        return buffer.toString();
    }

    private void registerMethodReturnType (Node node, MethodInfo method)
    {
        TypeInfo returnType = method.getReturnType(_env.getTypeRetriever());
        if (returnType != null) {
            addSemanticRecordToNode(node, returnType, method,
                                    Symbol.Method,
                                    getFullPath(node));
        }
        else {
            addError(node,
                Fmt.S("Fail to retrieve return type info for method %s.",
                    method.getName()));
        }
    }

     /////////////////////////////////////////////////////////////////

    protected void doVisit (ASTCast node)
    {
        printVisitNode(node);

        TypeInfo actualType = null;

        Node child = node.jjtGetChild(0);
        SemanticRecord record = getSemanticRecordForNode(child);
        if (record != null) {
            actualType = record.getTypeInfo();
        }

        TypeInfo castTo = castTo(node, actualType);
        addSemanticRecordToNode(node, castTo, null, null, null);
    }

    private TypeInfo castTo (ASTCast node, TypeInfo actualType)
    {
        String castTo = node.getClassName();
        if (StringUtil.nullOrEmptyOrBlankString(castTo)) {
            addError(node,
                    Fmt.S("Class name is missing in cast operation."));
            return actualType;
        }

        TypeInfo castToType = _env.getTypeRetriever().getTypeInfo(castTo);
        if (castToType == null) {
            addError(node,
                    Fmt.S("Cannot resolve string '%s' to a type.", castTo));
            return actualType;
        }
        else if (actualType == null) {
            // If the acutal type is null, then there must be an error
            // added to already.  Just return the castTo type to continue
            // on the type checking.
            return castToType;
        }
        else  if (actualType.isCompatible(castToType)) {
            // isCompatible() returns true if actualType can be widened or
            // narrowed to the castToType.
            return castToType;
        }
        else {
            addError(node,
                    Fmt.S("Cannot cast '%s' to '%s' since they are not compatible.",
                            actualType.getName(), castToType.getName()));
            // An error is already logged.  Return the castTo type so we can
            // continue on the type checking.
            return castToType;
        }
    }

    protected void doVisit (ASTAssign node)
    {
        printVisitNode(node);
        try {
            if (_env.getBooleanEnvVariable(Environment.CheckSideEffect, false)) {
                addError(node,
                    Fmt.S("Expression contains assignment statement which can cause undesirable side-effect.",
                    node));
            }

            Node leftValue = node.jjtGetChild(0);
            Node rightValue = node.jjtGetChild(1);
            SemanticRecord record = getSemanticRecordForNode(rightValue);
            if (record != null) {
                if (NonAssignableVarNames.indexOf(leftValue.toString()) != -1) {
                    addError(node,
                        Fmt.S("%s%s", "Cannot assign new value for following variables: ",
                            StringUtil.fastJoin(NonAssignableVarNames, ",")));
                    return;
                }

                SemanticRecord leftRecord = getSemanticRecordForNode(leftValue);
                if (leftRecord != null) {
                    if (leftRecord.getSymbolKind() == Symbol.Variable) {
                        // Just set the type info for the LHS symbol
                        addSemanticRecordToNode(leftValue,
                                                record.getTypeInfo(),
                                                null, null, null);
                        addSemanticRecordToNode(node,
                                                record.getTypeInfo(),
                                                null, null, null);
                    }
                    else {
                        addSemanticRecordToNode(node,
                                                leftRecord.getTypeInfo(),
                                                null, null, null);
                    }
                }
                else {
                    addSemanticRecordToNode(node,
                                        record.getTypeInfo(),
                                        null, null, null);
                }

                // If the right value is a literal constant, list or map,
                // associate the constant with the symbol.
                changeConstantValue(leftValue, rightValue);
                setNodeConstantValue(leftValue, rightValue);
            }
            else if (hasRootType()) {
                addError(node,
                    Fmt.S("Fail to evaluate the type of the RHS of assignment expression '%s'.",
                    node));
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTIn node)
    {
        printVisitNode(node);
        try {

            // ToDo: Type check for arguments?
            addSemanticRecordToNode(node, getTypeInfo(node, Boolean.class.getName()));
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTNotIn node)
    {
        printVisitNode(node);
        try {

            // ToDo: Type check for arguments?
            addSemanticRecordToNode(node, getTypeInfo(node, Boolean.class.getName()));
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTInstanceof node)
    {
        printVisitNode(node);
        try {

            // ToDo: Type check for arguments?
            addSemanticRecordToNode(node, getTypeInfo(node, Boolean.class.getName()));
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTEval node)
    {
        printVisitNode(node);
        try {
            // TODO : Check why ASTEval.children[0] could return string.  But
            // this node does not seem to be used in the grammer.
            Node expr = node.jjtGetChild(0);
            Node itObj = node.jjtGetChild(1);

            SemanticRecord record = getSemanticRecordForNode(itObj);
            if (record != null) {
                beginLexicalScope(record.getTypeInfo(), expr);
            }
            else {
                addError(node, Fmt.S(
                    "Type Info for context object '%s' is null.", itObj));
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTSequence node)
    {
        printVisitNode(node);
        try {
            int numOfChild = node.jjtGetNumChildren();
            Node lastChild = node.jjtGetChild(numOfChild-1);
            SemanticRecord record = getSemanticRecordForNode(lastChild);
            if (record != null) {
                TypeInfo result = record.getTypeInfo();
                addSemanticRecordToNode(node, result);
            }
            else {
                if  (hasRootType()) {
                    addError(node,
                        Fmt.S("Fail to lookup type for sub-expression '%s'.", lastChild));
                }
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTChain node)
    {
        printVisitNode(node);
        try {
            if (isTopPropertyChainNode(node)) {
                _unresolvedPropertyList.clear();
                _firstUnresolvedProperty = null;
            }

            int numOfChild = node.jjtGetNumChildren();
            TypeInfo result = null;
            Node lastChild = null;
            for (int i=0; i < numOfChild; i++) {
                lastChild = node.jjtGetChild(i);

                // resolve the type of this child
                lastChild.accept(this);

                // if we can find the type for this child, set the scope
                // using this child's type so that it becomes the base
                // type for the next child.
                SemanticRecord record = getSemanticRecordForNode(lastChild);
                if (record != null) {
                    result = record.getTypeInfo();

                    // setup a new scope using the child's type.  The scope
                    // will be poped when the next child is done visiting.
                    if (i < numOfChild-1) {
                        Node nextChild = node.jjtGetChild(i+1);
                        beginFieldPathScope(result, nextChild);
                    }
                }
            }

            if (!_unresolvedPropertyList.isEmpty()) {
                // Check if the unresolved properties indeed is the package name
                // of a known type.  If so, push this type as the base type
                // of the next child.
                String typeName = getUnresolvedPropertiesStr();
                result = getTypeInfo(typeName);
                // if the property chain refers to a type, also make sure that
                // the type is followed by a property.  A standalone type
                // declaration is not allowed.
                if (result != null && !isTopPropertyChainNode(node)) {
                    addSemanticRecordToNode(node, result, null, Symbol.Type, null);
                    _unresolvedPropertyList.clear();
                    _firstUnresolvedProperty = null;
                }
                else if (isTopPropertyChainNode(node)) {
                    if (hasRootType()) {
                        addError(node,
                            Fmt.S("Element '%s' in the field path '%s' cannot be resolved. ",
                            _firstUnresolvedProperty, getFullPath(node)));
                    }
                    else {
                        // There is no beginning type to start with.  So
                        // it is not an error if this field path cannot
                        // be resolved.
                        addSemanticRecordToNode(node, null, null, Symbol.Path, null);
                    }
                }
            }
            else if (result != null) {
                addSemanticRecordToNode(node, result, null, Symbol.Path, null);
                if (lastChild != null) {
                    // If the last child in the chain is a constant, then
                    // this node should have the same constant value.
                    Node constantValue = getNodeConstantValue(lastChild);
                    if (constantValue != null) {
                        setNodeConstantValue(node, constantValue);
                    }
                }
            }
        }
        finally {
            if (isTopPropertyChainNode(node)) {
                _unresolvedPropertyList.clear();
                _firstUnresolvedProperty = null;
            }
            endScopeIfNecessary(node);
        }
    }

    private boolean isTopPropertyChainNode (ASTChain node)
    {
        return !(node.jjtGetParent() instanceof ASTChain);
    }

    protected void doVisit (ASTList node)
    {
        printVisitNode(node);
        try {
            TypeInfo childInfo = checkCompatibleOperands(node, false);
            ContainerTypeInfo listInfo =
                (ContainerTypeInfo)getTypeInfo(node, List.class.getName());
            if (childInfo != null) {
                listInfo.setElementTypeInfo(childInfo);
            }

            addSemanticRecordToNode(node, listInfo);
            // register this node as a constant
            _constants.add(node);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTMap node)
    {
        printVisitNode(node);
        try {
            addSemanticRecordToNode(node, getTypeInfo(node, Map.class.getName()));
            // register this node as a constant
            _constants.add(node);
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    protected void doVisit (ASTKeyValue node)
    {
        printVisitNode(node);
        try {
            int numOfChild = node.jjtGetNumChildren();
            if (numOfChild > 1) {
                Node child = node.jjtGetChild(1);
                SemanticRecord record = getSemanticRecordForNode(child);
                if (record != null) {
                    TypeInfo result = record.getTypeInfo();
                    addSemanticRecordToNode(node, result);
                }
                else {
                    if (hasRootType()) {
                        addError(node,
                            Fmt.S("Fail to find type for value in key-value expression '%s'.",
                                node));
                    }
                }
            }
            else {
                addSemanticRecordToNode(node,
                    getTypeInfo(node, Void.TYPE.getName()));
            }
        }
        finally {
            endScopeIfNecessary(node);
        }
    }

    //-----------------------------------------------------------------------
    // Private Methods

    private void addToSymbolTable (Node node,
                                   SemanticRecord record,
                                   String symbolName,
                                   Integer symbolKind,
                                   String contextSymbolName)
    {
        if (node instanceof Symbol) {
            Symbol symbol = (Symbol)node;
            record.setSymbolName(
                !StringUtil.nullOrEmptyOrBlankString(symbolName) ?
                symbolName :
                !StringUtil.nullOrEmptyOrBlankString(record.getSymbolName()) ?
                record.getSymbolName() : symbol.getName());

            record.setSymbolKind(symbolKind != null ?
                                 symbolKind : record.getSymbolKind());

            record.setContextSymbolName(
                !StringUtil.nullOrEmptyOrBlankString(contextSymbolName) ?
                contextSymbolName : record.getContextSymbolName());

            _symbolTable.addSymbol(symbol, record);
        }
    }

    /**
     * This method associate a constant value with the <code>source</code> node.
     * The constant value is provided by the <code>target</code> node.  If
     * the target node itself represents a constant, then the target node is
     * associated with the source.  If the target node has a constant value,
     * then the target's constant value is associated with the source.
     * @param source
     * @param target
     */
    private void setNodeConstantValue (Node source, Node target)
    {
        if (target == null || source == null) {
            return;
        }

        // If the target is a constant or it has a constant value,
        // then set this node to the same constant value.
        if (_constants.contains(target)) {
            _nodeValueMap.put(source, target);
        }
        else if (getNodeConstantValue(target) != null) {
            _nodeValueMap.put(source, getNodeConstantValue(target));
        }
    }

    /**
     * Get the constant value associated with the <code>source</code> node.
     * @param source
     * @return Return a node representing the constant value in the expression.
     * Return null if there is no constant value.
     */
    private Node getNodeConstantValue (Node source)
    {
        if (source == null) {
            return source;
        }
        // This method needs to look up the constant from the _nodeValueMap
        // first such that if the constant has changed, the changed value
        // will be used first.  Literal constant does not change, but
        // constant map or list may change.
        Node result = (Node)_nodeValueMap.get(source);
        if (result == null && _constants.contains(source)) {
            result = source;
        }
        return result;
    }

    /**
     * If source is a map key for a constant value, then change the key to
     * point to <code>target</code>.
     * @param source
     * @param newValue
     */
    private void changeConstantValue (Node source, Node newValue)
    {
        Node original = getNodeConstantValue(source);
        if (original != null) {
            Node lastChild = getLastNonIndexNode(source);
            SemanticRecord record = getSemanticRecordForNode(lastChild);
            if (record != null && record.getSymbolKind() == Symbol.Key) {
                setNodeConstantValue(original, newValue);
            }
        }
    }

    private Node getLastNonIndexNode (Node node)
    {
        if (node instanceof ASTChain) {
            Node lChild = node.jjtGetChild(0);
            Node rChild = node.jjtGetChild(1);
            if (rChild instanceof ASTIndex) {
                return getLastNonIndexNode(lChild);
            }
            else if (rChild instanceof ASTChain) {
                return getLastNonIndexNode(rChild);
            }
            return rChild;
        }
        return node;
    }

    //////////////////////////////////////////////////////////////////////////

    private void addSemanticRecordToNode (Node node,
                                          TypeInfo info,
                                          PropertyInfo property,
                                          Integer symbolKind,
                                          String contextSymbolName)
    {
        addSemanticRecordToNode(node, info, null, property, symbolKind, contextSymbolName);
    }

    private void addSemanticRecordToNode (Node node,
                                          TypeInfo info,
                                          String symbolName,
                                          PropertyInfo property,
                                          Integer symbolKind,
                                          String contextSymbolName)
    {
        // ToDo
        printDebugStr("Associate TypeInfo to Node : " +
            info +
            " = (" + node.getClass().getName() +
            "," + node.hashCode() + ")");

        SemanticRecord record = (SemanticRecord)_semanticRecordMap.get(node);
        if (record == null) {
            record = new SemanticRecord(node, info, property);
            _semanticRecordMap.put(node, record);
        }
        record.setTypeInfo(info);
        record.setExtendedFieldPathNode(getExtendedFieldPathNode(node));
        node.setTypeInfo(info);
        addToSymbolTable(node, record, symbolName, symbolKind, contextSymbolName);
    }

    private void addSemanticRecordToNode (Node node, TypeInfo info)
    {
        addSemanticRecordToNode(node, info, null, null, null);
    }

    private SemanticRecord getSemanticRecordForNode (Node node)
    {
        return (SemanticRecord)_semanticRecordMap.get(node);
    }

    private SemanticRecord getSemanticRecordForSymbol (String symbolName, Integer kind)
    {
        return _symbolTable.getSymbolRecord(symbolName, kind);
    }

    private TypeInfo getTypeInfoForNode (Node node)
    {
        SemanticRecord record = getSemanticRecordForNode(node);
        return (record != null ? record.getTypeInfo() : null);
    }

    //////////////////////////////////////////////////////////////////////////

    private TypeInfo getTypeInfo (String className)
    {
        TypeRetriever retriever = _env.getTypeRetriever();
        return retriever.getTypeInfo(className);
    }

    private  TypeInfo getTypeInfo (Node node, String className)
    {
        return getTypeInfo(className);
    }

    private TypeInfo getFieldTypeInfo (Node node,
                                       TypeInfo classTypeInfo,
                                       String fieldName)
    {
        if (classTypeInfo == null) {
            // If type info is not known, then the return null for field info.
            return null;
        }
        PropertyResolver resolver = classTypeInfo.getPropertyResolver();
        TypeInfo ret = resolver.resolveTypeForName(_env.getTypeRetriever(), fieldName);
        if (ret != null) {
            SemanticRecord record = getSemanticRecordForPredecessorInPropertyChain(node);
            boolean staticInvocation =
                (record != null && record.getSymbolKind() == Symbol.Type);
            if (staticInvocation) {
                //fieldName may resolve to Field or a Method; check here is limited to Field
                //example expression : "ariba.use.core.UpdatePassword.hasSecretAnswer"
                FieldInfo fieldInfo = classTypeInfo.getField(_env.getTypeRetriever(),
                    fieldName);
                if (fieldInfo != null && !fieldInfo.isStatic()) {
                    Log.expression.error(10962, getFullPath(node));
                    //@todo kiran in future when we tighten this up completely we will return null
                    //ret = null;
                }
            }
        }
        return ret;
    }

    private PropertyInfo getPropertyInfo (TypeInfo typeInfo,
                                          String propertyName)
    {
        if (typeInfo == null) {
            // If the type info is not known, then return null for field info.
            return null;
        }

        PropertyInfo property =
            typeInfo.getPropertyResolver().resolvePropertyForName(
                _env.getTypeRetriever(), propertyName);

        if (property != null) {
            return property;
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////////////

    private String getUnresolvedPropertiesStr ()
    {
        return StringUtil.fastJoin(_unresolvedPropertyList, ".");
    }

    /////////////////////////////////////////////////////////////////////////

    private boolean hasRootType ()
    {
        return _rootType != null;
    }

    private TypeInfo getCurrentTypeInfo ()
    {
        return getCurrentTypeInfo(Scope.AnyScope);
    }

    private TypeInfo getCurrentLexicalTypeInfo ()
    {
        return getCurrentTypeInfo(Scope.LexicalScope);
    }

    private TypeInfo getCurrentFieldPathTypeInfo ()
    {
        return getCurrentTypeInfo(Scope.LexicalScope);
    }

    /**
     * A lexical scope signifies the type context for expression elements
     * which may contain field paths.  Examples of such elements are method
     * invocation (parameters) and projection.   In the case of projection,
     * the lexical scope also de
     * signifies the beginning of the containing lexical scope.  If the
     * containing lexical scope is the root lexical scope, then this method
     * will return null.
     * @param childNode
     * @return
     */
    private Node getExtendedFieldPathNode (Node childNode)
    {
        Scope result = null;
        for (int size=_contextStack.size(), i=size-1;
             i >= 0 && result == null; --i) {
            Scope scope = (Scope)_contextStack.get(i);
            if (scope.getType() == Scope.LexicalScope &&
                scope.getRootNode() != childNode) {
                result = scope;
                break;
            }
        }

        // If we can find a parent lexical scope and it is not the root
        // scope, then return it.
        if (result != null && result != _contextStack.get(0)) {
            Node candidate = result.getRootNode();
            if (isExtendedFieldPathNode(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isExtendedFieldPathNode (Node node)
    {
        return node instanceof ASTProject;
    }

    private Scope getCurrentScope (int type)
    {
        Scope result = null;
        for (int size=_contextStack.size(), i=size-1;
             i >= 0 && result == null; --i) {
            Scope scope = (Scope)_contextStack.get(i);
            if (type == Scope.AnyScope || scope.getType() == type) {
                result = scope;
            }
        }
        return result;
    }

    private TypeInfo getCurrentTypeInfo (int type)
    {
        Scope result = getCurrentScope(type);
        Assert.that(result != null, "Fail to get the current object Type info.");
        return result.getRootType();
    }

    private void beginFieldPathScope (TypeInfo rootType, Node rootNode)
    {
        beginScope(rootType, rootNode, Scope.FieldPathScope);
    }

    private void beginLexicalScope (TypeInfo rootType, Node rootNode)
    {
        beginScope(rootType, rootNode, Scope.LexicalScope);
    }

    private void beginScope (TypeInfo rootType, Node rootNode, int type)
    {
        printDebugStr("Enter Scope " +
            String.valueOf(_contextStack.size()) +
            ": (" + rootType +
            "," + rootNode.getClass().getName() +
            "," + rootNode.hashCode() + ")");

        _contextStack.add(new Scope(rootType, rootNode, type));
    }

    private void endScopeIfNecessary (Node currentNode)
    {
        if (_contextStack.size() == 0) {
            return;
        }

        Scope scope = (Scope)_contextStack.get(_contextStack.size() - 1);
        if (scope.getRootNode() == currentNode) {
            printDebugStr("Exit Scope " +
                String.valueOf(_contextStack.size()) +
                ": (" + scope.getRootType() +
                "," + scope.getRootNode().getClass().getName() +
                "," + scope.getRootNode().hashCode() + ")");
            _contextStack.remove(_contextStack.size() - 1);
        }
    }

    ////////////////////////////////////////////////////////////////////////

    private void addError (Node node, String errorStr)
    {
        printDebugStr("ERROR | " + getNodeStr(node) + " | " + errorStr);
        _errorCollector.add(errorStr + "\n");
    }

    private void printDebugStr (String debugStr)
    {
        Log.expression.debug(debugStr);
    }

    private void printVisitNode (Node node)
    {
       printDebugStr("Visiting Node = " + getNodeStr(node));
    }

    private String getNodeStr (Node node)
    {
        return ("(" + node.getClass().getName() + "," + node.hashCode() + ")" +
                " : Parent( " +
                (node.jjtGetParent() != null ?
                    node.jjtGetParent().getClass().getName() +
                    "," + node.jjtGetParent().hashCode() : "") + ")");
    }

    //////////////////////////////////////////////////////////////////////

    private static class Scope
    {
        static final int AnyScope = 0;
        static final int LexicalScope = 1;
        static final int FieldPathScope = 2;

        private TypeInfo _rootType;
        private Node     _rootNode;
        private int      _type;

        Scope (TypeInfo rootType, Node rootNode, int type)
        {
            _rootType = rootType;
            _rootNode = rootNode;
            _type = type;
        }

        TypeInfo getRootType ()
        {
            return _rootType;
        }

        Node getRootNode ()
        {
            return _rootNode;
        }

        int getType ()
        {
            return _type;
        }
    }

}
