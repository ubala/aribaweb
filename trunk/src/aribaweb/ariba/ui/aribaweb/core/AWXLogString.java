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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWXLogString.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWFastStringBuffer;
import ariba.util.core.StringUtil;

public final class AWXLogString extends AWComponent
{
    public String _logString;

    // ** Thread Safety Considerations: see AWComponent.

    // This component can be useful for printing log messages from within the .awl template.  Just put
    // <AWXLogString value="Some message"/>
    // at strategic points in the template, and it'll print to the console as well as drop a comment
    // in the outgoing html.

    protected AWFastStringBuffer componentPath ()
    {
        return parent().componentPath();
    }

    private String logValueString ()
    {
        String logValueString = null;
        Object logValue = valueForBinding(AWBindingNames.value);
        if (logValue == null) {
            logValueString = "null";
        }
        else if (!(logValue instanceof String)) {
            logValueString = StringUtil.strcat("(", logValue.getClass().getName(), ") ", logValue.toString());
        }
        else {
            logValueString = (String)logValue;
        }
        return logValueString;
    }

    private String formatLogMessage (AWRequestContext requestContext, String phaseName, String logValueString)
    {
        String messageString = ariba.util.core.Fmt.S("** %s %s: (%s:%s) %s", getClass().getName(), phaseName,
                requestContext.currentElementId(), componentPath(), logValueString);
        return messageString;
    }

    private void printSeparator (AWRequestContext requestContext, String phaseName)
    {
        if (requestContext.get(phaseName) == null) {
            debugString("------------------------ " + phaseName + " -----------------------------");
            requestContext.put(phaseName, phaseName);
        }
    }

    private boolean verbose ()
    {
        return !hasBinding("verbose") || booleanValueForBinding("verbose");
    }

    public void applyValues(AWRequestContext requestContext, AWComponent component)
    {
        printSeparator(requestContext, "applyValues");
        if (verbose()) {
            String logValueString = logValueString();
            String messageString = formatLogMessage(requestContext, "/applyValues", logValueString);
            debugString(messageString);
        }
        else {
            terseLog(requestContext);
        }
        super.applyValues(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        printSeparator(requestContext, "invokeAction");
        if (verbose()) {
            String logValueString = logValueString();
            String messageString = formatLogMessage(requestContext, "/invokeAction", logValueString);
            debugString(messageString);
        }
        else {
            terseLog(requestContext);
        }
        return super.invokeAction(requestContext, component);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        printSeparator(requestContext, "renderReponse");
        if (verbose()) {
            String logValueString = logValueString();
            _logString = StringUtil.strcat("<!--", logValueString, "-->");
            String messageString = formatLogMessage(requestContext, "/renderResponse", logValueString);
            debugString(messageString);
        }
        else {
            terseLog(requestContext);
        }
        super.renderResponse(requestContext, component);
        _logString = null;
    }

    private void terseLog (AWRequestContext requestContext)
    {
        Object logValue = valueForBinding(AWBindingNames.value);
        debugString("[" + requestContext.currentElementIdPath().elementId() + "/" +
                requestContext().currentElementId().string() + "] " + logValue.toString());
    }
}
