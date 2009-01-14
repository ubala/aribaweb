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

    $Id: //ariba/platform/util/expr/ariba/util/fieldtype/TypeRetriever.java#3 $
*/

package ariba.util.fieldtype;

/**
 * TypeRetriever provides lookup of a type within a specific scope.
 * @aribaapi private
*/
public interface TypeRetriever
{
    /**
     * Retrieve the TypeInfo given the type name.
     * @param name - fully qualified type name
     * @return TypeInfo for the specified type name.  If type is not found,
     * return null;
     */
    public abstract TypeInfo getTypeInfo (String name);
}

