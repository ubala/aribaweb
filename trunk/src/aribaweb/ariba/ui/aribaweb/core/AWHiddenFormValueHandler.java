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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWHiddenFormValueHandler.java#2 $
*/

package ariba.ui.aribaweb.core;

/**
    Handler for a hidden from value.

    @aribaapi private
*/
public interface AWHiddenFormValueHandler
{
    /**
        Specifies a name for the form value.
    */
    public String getName ();

    /**
        Specifies the initial value for the hidden form value.
    */
    public String getValue ();

    /**
        Hidden form values are generally manipulated by the client side script.
        When the hidden form value is posted from the server, this method
        is called.
     */
    public void setValue (String newValue);
}
