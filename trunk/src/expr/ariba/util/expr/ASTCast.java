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

    $Id: //ariba/platform/util/expr/ariba/util/expr/ASTCast.java#4 $
*/
package ariba.util.expr;

import ariba.util.fieldtype.TypeInfo;

class ASTCast extends SimpleNode
{
    private String _classname;

    public ASTCast(int id) {
        super(id);
    }

    public ASTCast (ExprParser p, int id) {
        super(p, id);
    }

    public void jjtClose()
    {
        // flattenTree();
    }

    void setClassName (String classname ) {
        _classname = classname;
    }

    public String getClassName () {
        return _classname;
    }

    protected Object getValueBody ( ExprContext context, Object source )
            throws ExprException
    {
        Object value = children[0].getValue( context, source );
        // If both _classname and child value are primitives, then this
        // method will convert the child value to an instance of the casted type.
        // Otherwise, the method will just return child value as it is if it is
        // convertable or return null.
        String classNameToUse = _classname;
        Class targetClass = TypeConversionHelper.getClassForType(classNameToUse);
        if (targetClass == null) {
            //Could it be Dynamic type?
            TypeInfo myType = getTypeInfo();
            if (myType!=null) {
                classNameToUse = myType.getImplementationName();
                targetClass = TypeConversionHelper.getClassForType(classNameToUse);
            }
        }
        if (TypeConversionHelper.canConvert(targetClass, value)) {
            return TypeConversionHelper.convertPrimitive(classNameToUse, value);
        }
        else {
            return null;
        }
    }

    protected void setValueBody( ExprContext context, Object target, Object value )
        throws ExprException
    {
        children[0].setValue(context, target, value);
    }

    public String toString ()
    {
        return "(@(" + _classname + ")" + children[0] + ")";
    }

    public void accept (ASTNodeVisitor visitor)
    {
        acceptChildren(visitor);
        visitor.visit(this);
    }
}
