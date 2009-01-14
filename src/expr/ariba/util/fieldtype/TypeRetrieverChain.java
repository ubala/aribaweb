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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/TypeRetrieverChain.java#2 $
*/

package ariba.util.fieldtype;
import java.util.List;
import ariba.util.core.ListUtil;

/**
 * TypeRetreiverChain is a special TypeRegistry that provides type lookup from
 * a chain of TypeRegistries.
 *
 * @aribaapi private
*/
public class TypeRetrieverChain extends TypeRegistry
{
    public static final String ChainedTypeRetrieverName = "TypeRetrieverChain";

    protected List  _retrieversChain;

    public TypeRetrieverChain ()
    {
       _retrieversChain  = ListUtil.list();
    }

    public TypeRetrieverChain (List retrievers)
    {
        _retrieversChain = retrievers;
    }

    public String getName ()
    {
        return ChainedTypeRetrieverName;
    }

    public void add (TypeRetriever retriever)
    {
        _retrieversChain.add(retriever);
    }

    protected List getRetrieversChain ()
    {
        return _retrieversChain;
    }

    protected TypeInfo getRegisteredTypeInfo (String name)
    {
        return null;
    }

    protected TypeInfo registerTypeInfo (TypeInfo type)
    {
        return type;
    }
}
