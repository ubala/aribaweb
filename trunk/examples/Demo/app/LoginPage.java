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

    $Id: //ariba/platform/ui/opensourceui/examples/Demo/app/LoginPage.java#1 $
*/
package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWLocalLoginSessionHandler;
import ariba.ui.aribaweb.core.AWResponseGenerating;

public class LoginPage extends AWComponent
{
    AWLocalLoginSessionHandler.CompletionCallback _callback;
    public String _userName;
    public String _password;

    public void init (AWLocalLoginSessionHandler.CompletionCallback callback)
    {
        _callback = callback;
    }

    public AWResponseGenerating login ()
    {
        ((Session)session()).setAuthenticated(true);
        return _callback.proceed(requestContext());
    }

    public AWResponseGenerating cancel ()
    {
        return _callback.cancel(requestContext());
    }
}