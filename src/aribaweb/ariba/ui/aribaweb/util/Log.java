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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/Log.java#32 $
*/

package ariba.ui.aribaweb.util;

import ariba.util.log.Logger;
import ariba.util.core.Fmt;
import ariba.ui.aribaweb.core.AWComponent;
import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
*/
public class Log extends ariba.util.log.Log
{
    //
    //  Log message category for logging Aribaweb framework
    //
    public static final Logger aribaweb =
        (Logger)Logger.getLogger("aribaweb");

    public static final Logger aribaweb_request =
        (Logger)Logger.getLogger("aribaweb_request");

    // request form values
    public static final Logger aribaweb_formValue =
        (Logger)Logger.getLogger("aribaweb.formValue");

    // track invalid history requests
    public static final Logger aribaweb_ihr =
        (Logger)Logger.getLogger("aribaweb.ihr");

    // session construction
    public static final Logger aribaweb_session =
        (Logger)Logger.getLogger("aribaweb.session");

    // file upload error logging
    public static final Logger aribaweb_fileupload =
        (Logger)Logger.getLogger("aribaweb.fileupload");

    // servlet adapter
    public static final Logger servletadaptor =
        (Logger)Logger.getLogger("servletadaptor");

    // node validation
    public static final Logger aribaweb_nodeValidate =
        (Logger)Logger.getLogger("aribaweb.nodeValidate");

    // user status manager
    public static final Logger aribaweb_userstatus =
        (Logger)Logger.getLogger("aribaweb_userstatus");

    public static final Logger aribaweb_userstatus_notification =
        (Logger)Logger.getLogger("aribaweb_userstatus_notification");

    public static final Logger aribaweb_userstatus_nodeDown =
        (Logger)Logger.getLogger("aribaweb_userstatus_nodeDown");

    public static final Logger aribaweb_userstatus_disconnectTime =
        (Logger)Logger.getLogger("aribaweb_userstatus_disconnect");

    public static final Logger aribaweb_userstatus_dump =
        (Logger)Logger.getLogger("aribaweb_userstatus_dump");

    public static final Logger aribaweb_userstatus_pause =
        (Logger)Logger.getLogger("aribaweb_userstatus_pause");

    public static final Logger aribaweb_shutdown =
        (Logger)Logger.getLogger("shutdown.aribaweb");

    public static final Logger aribaweb_monitor_sessionCount =
        (Logger)Logger.getLogger("aribaweb_monitor_sessionCount");

    //
    public static final Logger aribaweb_html =
        (Logger)Logger.getLogger("aribaweb_html");

    public static final Logger aribaweb_errorManager =
        (Logger)Logger.getLogger("aribaweb_errorManager");

    //
    // tree traversal categories
    //

    // element id
    public static final Logger aribawebexec_elementId =
        (Logger)Logger.getLogger("aribawebexec.elementId");

    // skipping
    public static final Logger aribawebexec_skip =
        (Logger)Logger.getLogger("aribawebexec.skip");

    //
    // incremental update categories
    //

    // page refresh
    public static final Logger domsync =
        (Logger)Logger.getLogger("aribawebdomsync");

    // page refresh
    public static final Logger domsync_element =
        (Logger)Logger.getLogger("aribawebdomsync.element");

    //
    // validation log categories
    //

    // detect invalid states of objects (eg stateless components not cleared in sleep)
    public static final Logger aribawebvalidation_state =
        (Logger)Logger.getLogger("aribawebvalidation.state");

    public static final Logger aribawebvalidation_state_primitive =
        (Logger)Logger.getLogger("aribawebvalidation.state_primitive");

    // detect bindable elements that are non-empty at initialization time
    public static final Logger aribawebvalidation_bindableElement =
        (Logger)Logger.getLogger("aribawebvalidation.bindableElement");

    // detect violations in export mode
    public static final Logger aribawebvalidation_exportMode =
        (Logger)Logger.getLogger("aribawebvalidation.exportMode");

    //
    // helper methods
    //

    public static final Logger aribawebResource =
        (Logger)Logger.getLogger("aribawebResource");

    public static final Logger aribawebResource_lookup =
        (Logger)Logger.getLogger("aribawebResource.lookup");

    public static final Logger aribawebResource_register =
        (Logger)Logger.getLogger("aribawebResource.register");

    public static final Logger aribawebResource_brand =
        (Logger)Logger.getLogger("aribawebResource.brand");

        // direct action logs
    public static final Logger directAction =
        (Logger)Logger.getLogger("directAction");
    
    public static void logStack (Logger category)
    {
        logStack(category, "Log stack.");
    }

    public static void logStack (Logger category, String msg)
    {
        if (category.isDebugEnabled()) {
            Exception e = new Exception(msg);
            logException(category, e);
        }
    }

    public static void logException (Logger category, Exception e)
    {
        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        category.debug(result.toString());
    }

    public static void dumpAWStack (AWComponent component, String msg)
    {
        String error = 
            Fmt.S("The following exception occurred in Component path:\n%s\n%s",
                component.componentPath("\n").toString(), msg);

        Exception e = new Exception(error);

        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        Log.aribaweb.warning(9339, result.toString());
    }
}

