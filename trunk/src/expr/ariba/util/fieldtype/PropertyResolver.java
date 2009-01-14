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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/PropertyResolver.java#3 $
*/

package ariba.util.fieldtype;

/**
 * A PropertyResolver resolves the type info for a property given the property
 * can be a field name or a method.
 * @aribaapi private
*/
public interface PropertyResolver
{
    public abstract TypeInfo resolveTypeForName (TypeRetriever retriever,
                                                 String name);

   public abstract PropertyInfo resolvePropertyForName (TypeRetriever retriever,
                                                        String name);
}

