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

    $Id: //ariba/platform/util/expr/ariba/util/expr/Environment.java#6 $
*/

package ariba.util.expr;

import java.util.Map;
import java.util.List;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import ariba.util.fieldtype.TypeRetriever;
import ariba.util.fieldtype.TypeRetrieverChain;

/**
    @aribaapi private 
*/
public class Environment 
{
    public static final String CheckSideEffect = "CheckSideEffect";
    public static final String CheckReturnType = "CheckReturnType";

    private Map _envVariables = MapUtil.map();
    private List _errorCollector = ListUtil.list();
    private TypeRetriever _typeRetriever;

    public Environment ()
    {
    }

    public Environment (TypeRetriever typeRetriever)
    {
        _typeRetriever = typeRetriever;
    }

    public Environment (List /* <TypeRetriever> */ typeRetrievers)
    {
        _typeRetriever = new TypeRetrieverChain(typeRetrievers);
    }

    public List /* <String> */ getErrorCollector ()
    {
        return _errorCollector;
    }

    public Object getEnvVariable (String name)
    {
        return _envVariables.get(name);
    }

    public boolean getBooleanEnvVariable (String name, boolean defaultValue)
    {
        Object value = _envVariables.get(name);
        return (value != null && value instanceof Boolean ?
            ((Boolean)value).booleanValue() : defaultValue);
    }

    public void setEnvVariable (String name, Object value)
    {
        _envVariables.put(name, value);
    }

    public void setBooleanEnvVariable (String name, boolean value)
    {
        _envVariables.put(name, (value ? Boolean.TRUE : Boolean.FALSE));
    }

    public TypeRetriever getTypeRetriever ()
    {
        return _typeRetriever;
    }

    public void setTypeRetriever (List typeProviders)
    {
        _typeRetriever = new TypeRetrieverChain(typeProviders);
    }

    public void setTypeRetriever (TypeRetriever typeRetriever)
    {
        _typeRetriever = typeRetriever;
    }
}
