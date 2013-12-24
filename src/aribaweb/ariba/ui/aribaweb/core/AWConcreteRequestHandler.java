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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWConcreteRequestHandler.java#22 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.StringUtil;

abstract public class AWConcreteRequestHandler extends AWBaseObject implements AWRequestHandler
{
    public static final String DefaultSecureHttpPort = "443";

    private AWApplication _application;
    private String _requestHandlerKey;
    private String _adaptorPrefix;
    private String _applicationNameSuffix;
    private String _alternateSecurePort;

    // ** Thread Safety Considerations: although this is shared by multiple threads, the Strings are immutable (no lokcing required) and the application locking will take place elsewhere.

    public void init (AWApplication application, String requestHandlerKey)
    {
        this.init();
        if (_application == null) {
            _application = application;
            _requestHandlerKey = requestHandlerKey;
            // ** this needs to be gotten from the (initial) request.
            _adaptorPrefix = "/AppName/AribaWeb/";
            _applicationNameSuffix = ".aw/";
        }
    }

    public void setAlternateSecurePort (String port)
    {
        _alternateSecurePort = port;
        AWDirectActionUrl.setAlternateSecurePort(port);
    }

    public String alternateSecurePort ()
    {
        return _alternateSecurePort;
    }

    ////////////////////
    // Handle Exception
    ////////////////////
    protected AWResponseGenerating defaultHandleException (AWRequestContext requestContext, Exception exception)
    {
        AWResponse newResponse = application().createResponse(requestContext.request());
        AWConcreteApplication.appendGenericExceptionMessageToResponse(newResponse, exception);
        return newResponse;
    }

    public AWResponseGenerating handleException (AWRequestContext requestContext, Exception exception)
    {
        AWApplication application = application();
        requestContext.setXHRRCompatibleResponse(application.createResponse(requestContext.request()));
        return application.handleException(requestContext, exception);
    }

    public AWResponseGenerating handleSessionRestorationError (AWRequestContext requestContext)
    {
        return application().handleSessionRestorationError(requestContext);
    }

    public AWResponseGenerating handleSessionValidationError (AWRequestContext requestContext, Exception exception)
    {
        return application().handleSessionValidationError(requestContext, exception);
    }

    public AWResponseGenerating handleComponentActionSessionValidationError (AWRequestContext requestContext, Exception exception)
    {
        return application().handleComponentActionSessionValidationError(requestContext, exception);
    }

    public AWResponseGenerating handleSiteUnavailableException (AWRequestContext requestContext)
    {
        AWResponseGenerating response = application().handleSiteUnavailableException(requestContext);
        // if no siteunavailableexception handler is registered with the app, throw
        // a generic exception
        if (response == null) {
            AWGenericException newException =
                new AWGenericException("Site handler not registered.");
            response = handleException(requestContext, newException).generateResponse();
        }
        else {
            Log.aribaweb_shutdown.debug("Handled site unavailable exception");
        }
        return response;
    }

    public AWResponseGenerating handleRemoteHostMismatchException (
            AWRequestContext requestContext,
            AWRemoteHostMismatchException exception)
    {
        return application().handleRemoteHostMismatchException(requestContext, exception); 
    }

    //////////////////////
    // Misc
    //////////////////////
    public String requestHandlerKey ()
    {
        return _requestHandlerKey;
    }

    protected AWApplication application ()
    {
        return _application;
    }

    protected String addTrailingSlash (String url)
    {
        if (!url.endsWith("/")) {
            return StringUtil.strcat(url, "/");
        }
        return url;
    }

    /**
     * The adaptorPrefix is the part of the URL reserved for
     * {tomcat-application-name}.
     * @return The tomcat-application-name.
     */
    public String adaptorPrefix ()
    {
        return _adaptorPrefix;
    }

    public void setAdaptorPrefix (String prefixString)
    {
        _adaptorPrefix = prefixString;
    }

    public void setApplicationNameSuffix (String suffixNameString)
    {
        if (!suffixNameString.endsWith("/")) {
            suffixNameString = StringUtil.strcat(suffixNameString, "/");
        }
        _applicationNameSuffix = suffixNameString;
    }

    public String applicationNameSuffix ()
    {
        return _applicationNameSuffix;
    }

    public AWResponse handleRequest (AWRequest request)
    {
        return null;
    }

    public String fullAdaptorUrlForRequest (AWRequest request)
    {
        return fullAdaptorUrl(request.isSecureScheme());
    }


    protected String fullAdaptorUrl (boolean httpSecureScheme)
    {
        String adaptorUrl = null;
        AWApplication application = application();

        if (!httpSecureScheme) {
            adaptorUrl = application.adaptorUrl();
        }
        else {
            adaptorUrl = application.adaptorUrlSecure();
        }
        return adaptorUrl;        
    }

    // record and playback
    protected void _debugDoRecordAndPlayback (AWRequestContext requestContext,
                                              AWRequest request,
                                              AWResponse response)
    {
        String uri = request.uri();
        if (!(uri.indexOf(".gif") != -1)
            && !(uri.indexOf(".js") != -1)
            && !(uri.indexOf(".css") != -1)) {
            AWEncodedString frameName = requestContext.frameName();
            if (frameName == null ||
                !AWRecordingManager.AWRecordPlayBackFrameName.equals(frameName)) {
                try {
                    if (requestContext._debugIsInPlaybackMode() ||
                        requestContext._debugIsInRecordingMode()) {
                        AWRecordingManager.setPlayBackHeaders(requestContext, request, response);
                        AWRecordingManager manager = AWRecordingManager.instance(request);
                        if (manager != null) {
                            manager.recordRequest(request);
                        }
                        response._debugSetRecordPlaybackParameters(manager, true);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
