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

    $Id: //ariba/platform/util/expr/ariba/util/expr/SymbolTable.java#4 $
*/

package ariba.util.expr;

import ariba.util.core.MapUtil;
import ariba.util.core.Assert;
import ariba.util.core.SetUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.ListUtil;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
    @aribaapi private
*/
public class SymbolTable
{
    private Map /*<Symbol, SemanticRecord>*/  _symbolMap = MapUtil.map();
    private Map /*<Integer, Set>*/ _kindMap = MapUtil.map();
    private Map /*<String, List>*/ _contextMap = MapUtil.map();

    public void addSymbol (Symbol symbol, SemanticRecord record)
    {
        _symbolMap.put(symbol, record);

        Assert.that(record.getSymbolKind() != null,
                    "The kind attribute of symbol '%s' is not known.",
                    symbol.getName());
        Set symbols = (Set)_kindMap.get(record.getSymbolKind());
        if (symbols == null) {
            symbols = SetUtil.set();
            _kindMap.put(record.getSymbolKind(), symbols);
        }
        symbols.add(record);

        String ctxName = record.getContextSymbolName();
        if (!StringUtil.nullOrEmptyOrBlankString(ctxName)) {
            List ctxsymbols = (List)_contextMap.get(ctxName);
            if (ctxsymbols == null) {
                ctxsymbols = ListUtil.list();
                _contextMap.put(ctxName, ctxsymbols);
            }
            ctxsymbols.add(record);
        }
    }

    public SemanticRecord getSymbolRecord (Symbol symbol)
    {
        return (SemanticRecord)_symbolMap.get(symbol);
    }

    /**
     * Get the symbol based on the name or kind of the symbol.  The symbol
     * kind is an integer defined in ariba.util.expr.Symbol. Note that the
     * symbol table does not implementation scoping.  This method
     * should be used with care when using outside of expression evaluation.
     * @param name
     * @param kind
     * @see ariba.util.expr.Symbol
     * @return A semantic record corresponding to the symbol
     */
    public SemanticRecord getSymbolRecord (String name, Integer kind)
    {
        Collection symbols = getSymbolRecords(kind);
        if (symbols != null) {
            Iterator iter = symbols.iterator();
            while (iter.hasNext()) {
                SemanticRecord symbol = (SemanticRecord)iter.next();
                if (symbol.getSymbolName().equals(name)) {
                    return symbol;
                }
            }
        }
        return null;
    }

    public Collection /* <SemanticRecord> */ getSymbolRecords (Integer kind)
    {
        return (Collection)_kindMap.get(kind);
    }

    public List /* <SemanticRecord> */ getSymbolRecordsForContext (
                                                 String ctxName)
    {
        return (List)_contextMap.get(ctxName);
    }

    public Set getAllContext ()
    {
        return _contextMap.keySet();
    }
}
