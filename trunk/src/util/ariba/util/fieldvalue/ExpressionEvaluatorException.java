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

    $Id: //ariba/platform/util/core/ariba/util/fieldvalue/ExpressionEvaluatorException.java#2 $
*/

package ariba.util.fieldvalue;

/**
    An Exception class for the {@link ExpressionEvaluator} class.
    
    @aribaapi ariba
*/
public class ExpressionEvaluatorException extends ExpressionException
{
    public ExpressionEvaluatorException ()
    {
        super();
    }
    public ExpressionEvaluatorException (Throwable t)
    {
        super(t);
    }
    public ExpressionEvaluatorException (String message)
    {
        super(message);
    }
    public ExpressionEvaluatorException (String message, Throwable t)
    {
        super(message, t);
    }
}
