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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/TypeRegistry.java#2 $
*/

package ariba.util.fieldtype;
import  java.util.List;
import  ariba.util.core.ListUtil;
import  ariba.util.core.Assert;

/**
 * TypeRegistry provides type lookup from a logical type system.   A logical
 * type can have multiple aspects catpured in different physical representations.
 * Each physical representation is captured in a "type source" (e.g. Java class, AML files).
 * A TypeRegistry consolidates the type attributes from these different type
 * sources into a logical notion of a type which is invariant of its physical
 * representations.
 *
 *  @aribaapi private
*/
public abstract class TypeRegistry implements TypeRetriever
{
    protected TypeRegistry ()
    {
    }

    public TypeInfo getTypeInfo (String name)
    {
        TypeInfo info = getRegisteredTypeInfo(name);
        if (info == null) {
            info = getTypeInfoFromRetriever(name);
            if (info != null) {
                registerTypeInfo(info);
            }
        }

        return info;
    }

    private TypeInfo getTypeInfoFromRetriever (String name)
    {
        TypeInfo info = null;
        List retrievers = getRetrieversChain();
        Assert.that(!ListUtil.nullOrEmptyList(retrievers),
            "Cannot find Type Retrievers from the environment.");

        for (int i=0; i < retrievers.size(); i++) {
            TypeRetriever retriever = (TypeRetriever)retrievers.get(i);
            if (retriever != null) {
                info = retriever.getTypeInfo(name);
                if (info != null) {
                    break;
                }
            }
        }
        return info;
    }

    public abstract String getName ();

    protected abstract List getRetrieversChain ();

    protected abstract TypeInfo getRegisteredTypeInfo (String name);

    protected abstract TypeInfo registerTypeInfo (TypeInfo type);
}
