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

    $Id: //ariba/platform/util/expr/ariba/util/expr/SemanticRecord.java#7 $
*/

package ariba.util.expr;

import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldtype.PropertyInfo;

/**
    @aribaapi private
*/
public class SemanticRecord
{
    private Node     _node;
    private TypeInfo _type;
    private PropertyInfo _property;
    private Integer  _symbolKind;
    private String   _symbolName;
    private String   _contextSymbolName;
    private Node     _enclosingScope;


    public SemanticRecord (Node node, TypeInfo type, PropertyInfo propertyInfo)
    {
        _node = node;
        _type = type;
        _symbolKind = null;
        _symbolName = null;
        _contextSymbolName = null;
        _property = propertyInfo;
    }

    public Node getNode ()
    {
        return _node;
    }

    public TypeInfo getTypeInfo ()
    {
        return _type;
    }

    public PropertyInfo getPropertyInfo ()
    {
        return _property;
    }

    public String getSymbolName ()
    {
        return _symbolName;
    }

    public Integer getSymbolKind ()
    {
        return _symbolKind;
    }

    public String getSymbolKindName ()
    {
        return Symbol.SymbolNames[_symbolKind.intValue()];
    }

    public String getContextSymbolName ()
    {
        return _contextSymbolName;
    }

    public void setTypeInfo (TypeInfo info)
    {
        _type = info;
    }

    public void setSymbolName (String name)
    {
        _symbolName = name;
    }

    public void setSymbolKind (Integer kind)
    {
        _symbolKind = kind;
    }

    public void setContextSymbolName (String contextSymbolName)
    {
        _contextSymbolName = contextSymbolName;
    }

    public boolean hasParentContext ()
    {
        return (_contextSymbolName != null &&
                !_symbolName.equals(_contextSymbolName));
    }

    /**
     * This method sets a node which contains a extended field path for this
     * node.  This is usually used in field path containing within a
     * projection.  In this case, the field path is not fully valid without
     * knowing the (extended) field path that leads to the projection node.
     */
    public Node getExtendedFieldPathNode ()
    {
        return _enclosingScope;
    }

    public void setExtendedFieldPathNode (Node node)
    {
        _enclosingScope = node;
    }
}
