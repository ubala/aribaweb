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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/servletadaptor/AWServletApplication.java#20 $
*/

package ariba.ui.servletadaptor;

import ariba.ui.aribaweb.core.AWBaseMonitorStatsPage;
import ariba.ui.aribaweb.core.AWComponentActionRequestHandler;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWConcreteServerApplication;
import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWDirectActionRequestHandler;
import ariba.ui.aribaweb.core.AWMergedStringLocalizer;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWRequestHandler;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWServerApplication;
import ariba.ui.aribaweb.core.AWSessionValidator;
import ariba.ui.aribaweb.core.AWStringLocalizer;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWMultiLocaleResourceManager;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.ClassUtil;
import ariba.util.core.ResourceService;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import ariba.util.core.URLUtil;
import ariba.util.http.multitab.MultiTabSupport;
import ariba.util.i18n.LocalizedJavaString;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
    An {@link ariba.ui.aribaweb.core.AWApplication} for javax.servlet-based applications.
    By default will serve resources from WAR files.
    AWServletApplications use {@link AWServletRequest} for requests and {@link AWServletResponse}
    for responses.
 */
public class AWServletApplication extends AWConcreteApplication
{
    public String cleardotUrl = "/cleardot.gif";
    private int _servletUrlPrefixComponentCount = -1;
    private AWMergedStringLocalizer _localizer = new AWMergedStringLocalizer();

    private static boolean _allowDefaultDirectAction = false;
    protected static ServletConfig _ServletConfig;
    protected static String _ServletUrlPrefix = null;
    protected static boolean _servingResourcesFromWAR;

    // ** Thread Safety Considerations: everything in here (as of 9/2/99) executes at init time, which should all happen in a single thread.

    public void init ()
    {
        if (_ServletConfig != null) {
            // serve resources from WAR if we find a docroot there
            _servingResourcesFromWAR = (_ServletConfig.getServletContext().getResourcePaths("/docroot") != null);

            // Horrible hack:  Under appengine we have no app name in the URL, but I can't find a portable
            // way to get the contextPath, so I'm testing explicitly for app engine...
            Enumeration en = _ServletConfig.getServletContext().getAttributeNames();
            while (en.hasMoreElements()) {
                String name = (String)en.nextElement();
                if (name.startsWith("com.google.appengine.")) _ServletUrlPrefix = "";
                // System.out.printf(" && %s = %s\n", name, _ServletConfig.getServletContext().getAttribute(name));
            }
        }

        // Note: must init *after* line above, because super will call our resourceUrl()
        super.init();

        // Todo!  Why were we calling "couldBeNull" -- we'll blow up downstream it this isn't initialized...
        ResourceService resourceService = ResourceService.getService(); // ResourceService.serviceCouldBeNull();
        if (resourceService != null) {
            resourceService.registerStringProcessor(_localizer);
        }
        LocalizedJavaString.registerLocalizer(_localizer);
    }

    public static void initializeServletConfig(ServletConfig servletConfig)
    {
        _ServletConfig = servletConfig;
    }

    public static AWServerApplication sharedInstance ()
    {
        return AWConcreteServerApplication.sharedInstance();
    }

    protected String initAdaptorUrl ()
    {
        return  _adaptorUrl;
    }

    protected String initAdaptorUrlSecure()
    {
        return _adaptorUrlSecure;
    }


    /**
     * This is called by our DispatcherServlet on every request.  The first time
     * we use the request to determine our URL prefix (which we subsequently use
     * when constructing DirectAction URLs).
     */
    /**
        This is called by our DispatcherServlet on every request.  The first time
        we use the request to determine our URL prefix (which we subsequently use
        when constructing DirectAction URLs).
    */
    public synchronized void initAdaptorUrl (HttpServletRequest servletRequest)
    {
        boolean isRequestSecure = servletRequest.isSecure();
        String requestUrlString = servletRequest.getRequestURL().toString();

        if (((_adaptorUrl == null) && (!isRequestSecure)) ||
            ((_adaptorUrlSecure == null) && (isRequestSecure)))
        {
            try {
                String url = null;
                Log.servletadaptor.debug("****requestUrlString=%s",requestUrlString);
                URL requestUrl = URLUtil.makeURL(requestUrlString);
                int port = requestUrl.getPort();

                String baseUrlString =
                    StringUtil.strcat(requestUrl.getProtocol(),
                                        "://",
                                        requestUrl.getHost(),
                                        port == -1 ? null : ":",
                                        port == -1 ? null : Integer.toString(port));

                Log.servletadaptor.debug("prefix url: %s", baseUrlString);

                url = strcatPaths(baseUrlString, servletUrlPrefix());
                initAdaptorUrl(isRequestSecure, url);


            }
            catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
            Log.servletadaptor.debug("****adaptorUrl = %s", _adaptorUrl);
        }
    }

    protected void initAdaptorUrl(boolean requestSecure, String url)
    {
        // since AWDirectActionUrl is normally initialized non-lazily during app init,
        // we need to override its URL with one we get later (i.e. now, during
        // the first request).
        if (requestSecure) {
            _adaptorUrlSecure = url;
        }
        else {
            _adaptorUrl = url;
        }
    }


    private String strcatPaths (String path1, String path2)
    {
        String separator = "/";
        if (path1.endsWith(separator) && path2.startsWith(separator)) {
            return StringUtil.strcat(path1.substring(0, path1.length() -1), path2);
        }
        else if (path1.endsWith(separator) || path2.startsWith(separator)) {
            return StringUtil.strcat(path1, path2);
        }
        else {
            return StringUtil.strcat(path1, separator, path2);
        }
    }

    public boolean initIsStatisticsGatheringEnabled ()
    {
        return false;
    }

    public boolean allowsConcurrentRequestHandling ()
    {
        return true;
    }

    protected Class sessionClass ()
    {
        Class cls = AWUtil.classForName("app.Session");
        return (cls != null) ? cls : super.sessionClass();
    }

    public String name ()
    {
        return "AribaWeb";
    }

    public String fullUrlPrefix ()
    {
        return servletUrlPrefix() + "/" + name();
    }

    public String resourceUrl ()
    {
        String absPath = "/".concat(resourceFilePath());
        // by default, expecting appserver war to serve up resources...
        return _servingResourcesFromWAR ? servletUrlPrefix().concat(absPath)
                                        : absPath;
    }

    public String resourceURL ()
    {
        return resourceUrl();
    }

    public String resourceFilePath ()
    {
        // this is our default path inside of the wars and jars (and web server as well)
        return "docroot/";
    }

    public String deploymentRootDirectory ()
    {
        return null;
    }

    public boolean useServletEnginesSession ()
    {
        return true;
    }

    //////////////////////////
    // Localization
    ///////////////////////////
    public AWStringLocalizer getStringLocalizer ()
    {
        return _localizer;
    }

     /**
        implement LocalizedJavaString.Localizer interface
    */
    public String getLocalizedString (String className,
                                      String key,
                                      String defaultString,
                                      Locale locale)
    {
        String stringTableFile = ClassUtil.stripClassFromClassName(className);
        String shortClassName = ClassUtil.stripPackageFromClassName(className);
        Map map = _localizer.getLocalizedStrings(stringTableFile, shortClassName, locale);
        String value = defaultString;
        if (map != null) {
            value = (String)map.get(key);
            if (value == null) {
                value = defaultString;
            }
        }
        return value;
    }

    public AWMultiLocaleResourceManager createResourceManager ()
    {
        AWServletResourceManager resourceManager = new AWServletResourceManager();
        resourceManager.init();
        return resourceManager;
    }

    public AWRequest createRequest (Object servletRequest)
    {
        AWServletRequest awservletRequest = new AWServletRequest();
        awservletRequest.init((HttpServletRequest)servletRequest);
        return awservletRequest;
    }

    public AWResponse createResponse ()
    {
        AWServletResponse servletResponse = new AWServletResponse();
        servletResponse.init();
        return servletResponse;
    }

    protected int calculateServletUrlPrefixComponentCount ()
    {
        String servletUrlPrefix = servletUrlPrefix();
        if (StringUtil.startsWithIgnoreCase(servletUrlPrefix, "http:") ||
            StringUtil.startsWithIgnoreCase(servletUrlPrefix, "https:")) {
            // this is a full URL, we need to remove the beginning portion up to the third / character
            int firstSecondSlash = servletUrlPrefix.indexOf("//");
            servletUrlPrefix = servletUrlPrefix.substring(servletUrlPrefix.indexOf("/", firstSecondSlash + 2) + 1);
        }
        if ("".equals(servletUrlPrefix)) return 0;
        StringArray pathComponents = AWUtil.componentsSeparatedByString(servletUrlPrefix, "/");
        // subtract one here to account for preceeding slash.
        int servletUrlPrefixComponentCount = pathComponents.inUse();
        if (servletUrlPrefix.startsWith("/")) {
            servletUrlPrefixComponentCount--;
        }
        if (servletUrlPrefix.endsWith("/")) {
            servletUrlPrefixComponentCount--;
        }
        return servletUrlPrefixComponentCount;
    }

    /**
     Returns the number of "component parts" in the {@link #servletUrlPrefix()}.
     This turns out to be the number of path parts in the web app name.
     E.g. if the web app name is "Buyer", this method
     will return 1. If it's "foo/bar", it will return 2.
     @return the number of component parts in the web app name
     */
    public int servletUrlPrefixComponentCount ()
    {
        if (_servletUrlPrefixComponentCount == -1) {
            synchronized(this) {
                if (_servletUrlPrefixComponentCount == -1) {
                    _servletUrlPrefixComponentCount =
                        calculateServletUrlPrefixComponentCount();
                }
            }
        }
        return _servletUrlPrefixComponentCount;
    }

    /**
     Returns a string representing the part before the servlet name in
     request URLs. This turns out to be the web app name with a leading slash.
     @return a string of the following form: "/{web-app-name}"
     */
    public String servletUrlPrefix ()
    {
        if (_ServletUrlPrefix == null) {
            _ServletUrlPrefix = "/".concat(_ServletConfig.getServletName());
        }
        return _ServletUrlPrefix;
    }

    public String applicationNameSuffix ()
    {
        return "/";
    }

    /**
     Parses the RequestURLInfo out from <code>urlString</code> and returns it.
     @param urlString the "path" part of the URI, not including request params.
                      Doesn't include the protocol and hostname.
                      E.g. usually looks like one of the following:
     /{web-app-name}/{servlet-name}/{request-handler-key}/{extra-stuff}
     /{web-app-name}/{servlet-name}/{tab-index}/{request-handler-key}/{extra-stuff}
     @return
     */
    public AWConcreteApplication.RequestURLInfo requestUrlInfo (
            String urlString)
    {
        // if fully specified (versus relative) then strip it
        if (urlString.startsWith("http")) {
            try {
                URL url = new URL(urlString);
                urlString = url.getPath();
            } catch (MalformedURLException e) {
                throw new AWGenericException(e);
            }
        }

        AWConcreteApplication.RequestURLInfo info =
                new AWConcreteApplication.RequestURLInfo();
        // Note: the requestHandlerPath is really the URI
        //String requestHandlerPath = _servletRequest.getPathInfo();
        String requestHandlerPath = urlString;
        if (requestHandlerPath != null) {
            int requestHandlerPathLength = requestHandlerPath.length();
            if (requestHandlerPathLength > 0) {
                StringArray pathComponents = AWUtil.componentsSeparatedByString(
                        requestHandlerPath.replaceAll("//", "/"), "/");
                // Note: first entry is always empty string since
                //  requestHandlerPath starts with "/".
                int pathComponentCount = pathComponents.inUse();
                int servletUrlPrefixComponentCount = (
                        (AWServletApplication)AWConcreteApplication
                              .SharedInstance).servletUrlPrefixComponentCount();
                int currentFieldIndex = servletUrlPrefixComponentCount + 1;
                if (requestHandlerPath.startsWith("/")) {
                    currentFieldIndex ++;
                }
                // aling: I don't not fully understand why for catalog login
                // URL currentFieldIndex could equal to array length, for buyer
                // logic url it equals length - 1, which is correct

                if (pathComponentCount > currentFieldIndex) {
                    String[] pathComponentsArray = pathComponents.array();
                    String pathPart = pathComponentsArray[currentFieldIndex];

                    // Note: when user types trailing "/" in their URL,
                    // say http://y:8001/Ariba/Buyer/, here we got "", so this
                    // if branch is to solve the trailing "/" problem
                    // Defect 81707 and 79077
                    if (!StringUtil.nullOrEmptyString(pathPart)) {
                        char firstChar = pathPart.charAt(0);
                        String pref = MultiTabSupport.Instance.get().defaultTabPrefix();
                        // first field is instance id or tab id
                        while ((firstChar >= '0') && (firstChar <= '9')) {
                            // looking at previous field, we are tabbing
                            if (pathComponentsArray[currentFieldIndex - 1]
                                    .contains(pref)) {
                                info.tabIndex = pathPart;
                            }
                            else {
                                // if we're in here, pathPart must be an
                                // instance id.
                                info.applicationNumber = pathPart;
                            }

                            currentFieldIndex++;

                            if (pathComponentCount > currentFieldIndex) {
                                pathPart = pathComponentsArray[
                                        currentFieldIndex];

                                if (StringUtil.nullOrEmptyString(pathPart)) {
                                    currentFieldIndex++;
                                    pathPart = "";
                                    break;
                                }
                                else {
                                    firstChar = pathPart.charAt(0);
                                }
                            }
                            else {
                                pathPart = "";
                                break;
                            }
                        }

                        if (!StringUtil.nullOrEmptyString(pathPart)) {
                            info.requestHandlerKey = pathPart;
                        }

                        currentFieldIndex++;

                        if (pathComponentCount > currentFieldIndex) {
                            pathPart = pathComponentsArray[currentFieldIndex];

                            if (!StringUtil.nullOrEmptyString(pathPart)) {
                                info.requestHandlerPath =
                                        (String[])AWUtil.sublist(
                                                pathComponentsArray,
                                                currentFieldIndex,
                                                pathComponentCount);
                            }
                        }
                    }
                }
            }
        }
        return info;
    }

    public void registerRequestHandlerForKey (AWRequestHandler requestHandler, String requestHandlerKey)
    {
        String servletUrlPrefix = servletUrlPrefix();
        if (!(servletUrlPrefix.toLowerCase().startsWith("http") || servletUrlPrefix.startsWith("/"))) {
            servletUrlPrefix = StringUtil.strcat("/", servletUrlPrefix);
        }
        if (!servletUrlPrefix.endsWith("/")) {
            servletUrlPrefix = StringUtil.strcat(servletUrlPrefix, "/");
        }
        requestHandler.setAdaptorPrefix(servletUrlPrefix);
        requestHandler.setApplicationNameSuffix(applicationNameSuffix());
        super.registerRequestHandlerForKey(requestHandler, requestHandlerKey);
    }

    public String webserverDocumentRootPath ()
    {
        return resourceUrl();
    }

    public String applicationUrl (AWRequest request)
    {
        throw new AWGenericException(getClass().getName() + ": applicationUrl not currently supported");
    }

    public void setCleardotUrl (String cleardotUrlString)
    {
        cleardotUrl = cleardotUrlString;
    }

    //////////////////////
    // Monitoring Support
    //////////////////////
    public AWResponseGenerating monitorStatsPage (AWRequestContext requestContext)
    {
        AWBaseMonitorStatsPage page =
            (AWBaseMonitorStatsPage)createPageWithName(AWBaseMonitorStatsPage.PageName,
                    requestContext);

        page.setMonitorStats(monitorStats());
        return page;
    }

    protected class DefaultSessionValidatior implements AWSessionValidator
    {
        // Khaled's wrath...
        public AWResponseGenerating handleSessionValidationError (
            AWRequestContext requestContext, Exception exception)
        {
            return this.handleComponentActionSessionValidationError(
                requestContext, exception);
        }

        public AWResponseGenerating handleComponentActionSessionValidationError (
            AWRequestContext requestContext, Exception exception)
        {
            // force creation of session if not available
            if (requestContext.session(false) == null) {
                HttpSession httpSession = requestContext.createHttpSession();
            }
            return AWComponentActionRequestHandler.SharedInstance.processFrontDoorRequest(requestContext);
        }

        /**
         * If we have a session timeout, just go to the start page
         */
        public AWResponseGenerating handleSessionRestorationError (AWRequestContext requestContext)
        {
            return mainPage(requestContext);
        }

        public void assertExistingSession (AWRequestContext requestContext)
        {

        }

        public void assertValidSession (AWRequestContext requestContext)
        {

        }
    }

    public void initSessionValidator ()
    {
        if (_sessionValidator == null) setSessionValidator(new DefaultSessionValidatior());
    }

    public static class DefaultDirectAction extends AWDirectAction
    {
        public void init (AWRequestContext requestContext)
        {
            Assert.that(_allowDefaultDirectAction, "Cannot use DefaultDirectAction if other DirectAction implementation available");
            super.init(requestContext);
        }
    }

    public String directActionClassNameForKey(String classNameKey)
    {
        if (classNameKey.equals(AWDirectActionRequestHandler.DefaultDirectActionClassName)
                && (resourceManager().classForName(classNameKey) == null)) {
            _allowDefaultDirectAction = true;
            return DefaultDirectAction.class.getName();
        }

        return classNameKey;
    }
}
