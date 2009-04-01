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

    $Id: //ariba/platform/ui/metaui-jpa/examples/appcore/ariba/appcore/util/LoginPage.java#3 $
*/
package ariba.appcore.util;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWLocalLoginSessionHandler;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.layouts.MetaNavTabBar;
import ariba.appcore.User;

public class LoginPage extends AWComponent
{
    AWLocalLoginSessionHandler.CompletionCallback _callback;
    public String _userName;
    public String _password;

    public void init (AWLocalLoginSessionHandler.CompletionCallback callback)
    {
        _callback = callback;
    }

    protected boolean shouldValidateSession ()
    {
        return false;
    }

    public AWResponseGenerating login ()
    {
        User user = ObjectContext.get().findOne(User.class, AWUtil.map("name", _userName));
        if (user == null) {
            recordValidationError("username", localizedJavaString(1, "Unknown user" /*  */), _userName);
        } else {
            if (user.matchingPassword(_password)) {
                User.bindUserToSession(user, session());
                MetaNavTabBar.invalidateState(session());
                return _callback.proceed(requestContext());
            }
            recordValidationError("password", localizedJavaString(2, "Invalid credentials" /*  */), null);
        }
        errorManager().checkErrorsAndEnableDisplay();
        return null;
    }

    public AWResponseGenerating cancel ()
    {
        return _callback.cancel(requestContext());
    }
}