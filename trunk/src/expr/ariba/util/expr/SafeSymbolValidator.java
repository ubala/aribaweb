/*
    Copyright 1996-2009 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/expr/ariba/util/expr/SafeSymbolValidator.java#6 $
*/

package ariba.util.expr;

import ariba.util.core.Fmt;
import ariba.util.fieldtype.FieldInfo;
import ariba.util.fieldtype.TypeInfo;
import ariba.util.fieldvalue.Expression;
import java.util.List;

/**
    @aribaapi private
*/
public class SafeSymbolValidator extends SymbolValidator
{

   /////////////////////////////////////////////////////////////////

   public SafeSymbolValidator (Environment env,
                         Expression expression,
                         List errorCollector)
    {
        super(env, expression, errorCollector);
    }

    ////////////////////////////////////////////////////////////////////////

    protected void validate (FieldInfo field, int usage)
    {
        int fieldAccess = field.getAccessibility();
        if (fieldAccess < TypeInfo.AccessibilitySafe) {
            addError(field, Fmt.S("Field is not safe: %s", field.getName()));
        }
    }
}
