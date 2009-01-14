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

    $Id: //ariba/platform/util/expr/ariba/util/expr/ASTThisFieldVarRef.java#5 $
*/
package ariba.util.expr;

import ariba.util.fieldvalue.FieldPath;

/**
 * @aribaapi private
 */
class ASTThisFieldVarRef extends ASTVarRef implements Symbol
{
    public static final String Name = "thisField"; 
    private String name;

    public ASTThisFieldVarRef(int id) {
        super(id);
    }

    public ASTThisFieldVarRef(ExprParser p, int id) {
        super(p, id);
    }

    protected Object getValueBody( ExprContext context, Object source )
        throws ExprException
    {
        FieldPath fieldPath = getFieldPath(context);
        Object root = context.getRoot();
        Object value = (fieldPath != null ? fieldPath.getFieldValue(root) : null);
        if (value != null) {
            value = ExprRuntime.convert(value);
        }
        return value;
    }

    protected void setValueBody( ExprContext context, Object target, Object value )
        throws ExprException
    {
        FieldPath fieldPath = getFieldPath(context);
        if (fieldPath != null) {
            Object root = context.getRoot();
            fieldPath.setFieldValue(root, value);
        }
    }

    private FieldPath getFieldPath (ExprContext context)
    {
        SymbolTable table = context.getSymbolTable();
        SemanticRecord record = table.getSymbolRecord(this);
        return (record != null ? new FieldPath(record.getSymbolName()) : null);
    }

    public String getName ()
    {
        return Name;
    }

    public String toString()
    {
        return getName(); 
    }

    public void accept (ASTNodeVisitor visitor)
    {
        acceptChildren(visitor);
        visitor.visit(this);
    }
}
