/*
    Copyright (c) 2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/http/multitab/UnexpectedTabException.java#1 $

    Responsible: fkolar
 */
package ariba.util.http.multitab;

/**
 * exception raised when something goes wrong, treat as default tab
 */
public class UnexpectedTabException extends Exception {
    public UnexpectedTabException ()
    {
        super();
    }

    public UnexpectedTabException (String s)
    {
        super(s);
    }
}
