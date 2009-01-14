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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/DispatcherServlet.java#10 $
*/

package ariba.ui.demoshell;

import ariba.ui.servletadaptor.AWDispatcherServlet;
import ariba.ui.servletadaptor.AWServletApplication;
import ariba.ui.servletadaptor.AWServletRequest;
import ariba.ui.servletadaptor.AWServletResponse;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ariba.util.core.ClassUtil;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.ui.aribaweb.core.AWConcreteRequestHandler;

import java.io.IOException;

/**
    The DispatcherServlet determines which Application will be run.
*/
public class DispatcherServlet extends AWDispatcherServlet
{
    public String applicationClassName ()
    {
        return "ariba.ui.demoshell.Application";
    }

    /**
     * We override so that we don't force creation of our app
     * if another app is already in place.
     */
    public AWServletApplication createApplication ()
    {
        AWServletApplication app = (AWServletApplication)AWConcreteApplication.sharedInstance();

        if (app == null) {
            app = super.createApplication();
        }

        // Log.demoshell.debug("*** createApplication -- returning instance: %s", app);

        // force initialization of demoshell stuff
        DemoShell.sharedInstance();

        return app;
    }

    public void service (HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse)
      throws IOException, ServletException
    {
        AWServletApplication app = createApplication();

        // See if we should redirect
        String redirectUrl = DemoShell.sharedInstance().redirectForRequestURI(servletRequest);
        if (redirectUrl != null) {
            // direct to front door of servlet for app
            Log.demoshell.debug("*** Redirecting request %s \n  --- to URL: %s", servletRequest.getRequestURI(), redirectUrl);
            AWRedirect.setupHeaders(servletResponse, redirectUrl);
        } else {
            super.service(servletRequest, servletResponse);
        }
    }
}
