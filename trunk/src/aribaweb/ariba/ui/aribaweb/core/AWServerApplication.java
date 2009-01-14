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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWServerApplication.java#8 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import java.util.Locale;

public interface AWServerApplication extends AWRequestDispatching, AWRequestLocking
{
    public AWMultiLocaleResourceManager createResourceManager ();
    public AWSingleLocaleResourceManager createResourceManager (AWMultiLocaleResourceManager multiLocaleResourceManager, Locale locale);
    public AWSingleLocaleResourceManager createTemplateResourceManager ();
    public AWMultiLocaleResourceManager resourceManager ();
    public AWSingleLocaleResourceManager resourceManager (Locale locale);
    public void flushResourceManager ();
    public void setName (String nameString);
    public String name ();
    public AWRequest createRequest (Object nativeRequestObject);
    public AWResponse createResponse ();
    public AWResponse createResponse (AWRequest request);
    public void handleGarbageCollectionIssues ();
    public String deploymentRootDirectory ();

    //////////////////
    // User Defaults
    //////////////////
    public boolean isDebuggingEnabled ();

    ////////////////////
    // Request Handlers
    ////////////////////
    public void initRequestHandlers ();
    public void registerRequestHandlerForKey (AWRequestHandler requestHandler, String requestHandlerKey);
    public AWRequestHandler requestHandlerForKey (String requestHandlerKey);
    public void setDefaultRequestHandler (AWRequestHandler requestHandler);
    public AWRequestHandler defaultRequestHandler ();
    public AWResponse handleMalformedRequest (String message);
    public AWResponse handleMalformedRequest (AWRequest request, String message);

    public boolean initIsRapidTurnaroundEnabled ();
    public boolean isRapidTurnaroundEnabled ();
    public boolean isStatisticsGatheringEnabled ();
    public boolean requiresThreadSafety ();

    ////////////////
    // Threading
    ////////////////
    // now defined in AWCooperativeMultithreadLocking

    //////////////////////////
    // Localization
    ///////////////////////////
    public AWStringLocalizer getStringLocalizer ();

    ////////////////
    // Debugging
    ////////////////
    public void debugString (String message);
    public void logString (String message);
}
