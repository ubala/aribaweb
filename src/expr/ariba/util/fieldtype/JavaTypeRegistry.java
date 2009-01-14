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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/JavaTypeRegistry.java#2 $
*/

package ariba.util.fieldtype;
import  ariba.util.core.ListUtil;
import  java.util.List;

/**
    @aribaapi private 
*/
public class JavaTypeRegistry extends TypeRegistry
{
    public static final String JavaTypeRegistryName = "JavaTypeRegistry";

    private static JavaTypeRegistry _instance = new JavaTypeRegistry();
    protected static List  _retrieversChain;

    static {
        _retrieversChain = ListUtil.list(
            PrimitiveTypeProvider.instance(),
            JavaTypeProvider.instance()
        );
    }

    private JavaTypeRegistry ()
    {
    }

    public static TypeRegistry instance ()
    {
        return _instance;
    }

    public String getName ()
    {
        return JavaTypeRegistryName;
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
