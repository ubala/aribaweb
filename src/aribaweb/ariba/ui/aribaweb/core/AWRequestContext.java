/*
    Copyright 1996-2013 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWRequestContext.java#162 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWArrayManager;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Assert;
import ariba.util.core.DebugState;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringArray;
import ariba.util.core.StringUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.ThreadDebugKey;
import ariba.util.core.ThreadDebugState;
import ariba.util.core.WrapperRuntimeException;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.http.multitab.MultiTabSupport;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;

/**
    The context tracking state through all phases of a single request/response cycle.
    The RequestContext encapsulates the incoming {@link AWRequest} and the outgoing
    {@link AWResponse}.  As the primary argument in the {@link AWCycleable methods}
    it tracks the evolving ElementId as the document structure unfolds, providing Ids
    through {@link #nextElementId()}, {@link #pushElementIdLevel()}, and {@link @popElementIdLevel}.
  */
public class AWRequestContext extends AWBaseObject implements DebugState
{
    public static final AWEncodedString TopFrameName =
        AWEncodedString.sharedEncodedString("_top");
    public static final AWEncodedString SelfFrameName =
        AWEncodedString.sharedEncodedString("_self");
    public static final String SessionIdKey = "aws";
    public static final String ResponseIdKey = "awr";
    public static final String SessionSecureIdKey = "awssk";
    public static final String SessionSecureIdKeyEquals = AWRequestContext.SessionSecureIdKey + "=";
    public static final String FrameNameKey = "awf";
    public static final String PageScrollTopKey = "awst";
    public static final String PageScrollLeftKey = "awsl";
    public static final String SessionRendevousKey = "awsr";
    public static final String IncrementalUpdateKey = "awii";
    public static final ThreadDebugKey RequestContextID =
        new ThreadDebugKey("RequestContext");
    public static final String RefreshRequestKey = "awrr";
    public static final String RecordingModeKey = "awrm";
    public static final String InPageAction = "awipa";
    public static final String InPageRequest = "awip";
    public static boolean UseXmlHttpRequests = true;

    public static final String IgnoreRefreshCompleteKey = "IgnoreRefreshComplete";

    private AWApplication _application;
    private AWRequest _request;
    private List _requestSenderIds;
    private String _currentRequestSenderId;
    private AWElementIdPath _currentRequestSenderIdPath;
    private boolean _isBrowserFirefox;
    private boolean _isBrowserMicrosoft;
    private boolean _isBrowserSafari;
    private boolean _isBrowserChrome;
    private boolean _isMetaTemplateMode;
    private String _browserMinWidth;
    private String _browserMaxWidth;
    private AWElementIdGenerator _elementIdGenerator;
    private HttpSession _httpSession;
    private String _httpSessionId;
    private AWSession _awsession;
    private AWResponse _response;
    private AWEncodedString _responseId;
    private AWEncodedString _frameName;
    private Map _userState;
    private AWHtmlForm _currentForm;
    private AWPage _currentPage;
    private AWPage _requestPage;
    private AWPage _responsePage;
    private AWBacktrackState _backtrackState;
    private int _formIndex;
    private AWArrayManager _newPages;
    private AWComponent _currentComponent;
    private AWBaseElement _currentElement;
    private boolean _isDebuggingEnabled = AWConcreteApplication.IsDebuggingEnabled;
    private List _cookies;
    private boolean _didAddCookies;
    // form input paths
    private AWArrayManager _formInputIds;
    private int _targetFormIdIndex;
    private AWElementIdPath _targetFormIdPath;
    private boolean _dataValuePushedInInvokeAction;
    private boolean _isTooManyTabRequest;

    // history requests
    private boolean _isHistoryRequest = false;
    private int     _historyAction = -1;

    // record and playback
    private Boolean _debugIsInPlaybackMode;
    private Boolean _debugIsInRecordingMode;
    // debugging / validation
    private boolean _componentPathDebuggingEnabled = false;
    private boolean _pageRequiresPreGlidCompatibility = false;
    private boolean _allowsSkipping = true;

    // rendering variables
    private UIRenderMeta.RenderVersion _queryRenderVersion = null;

    // generating printable rendering, or data export?
    private boolean _isPrintMode;
    private boolean _isExportMode;

    private boolean _initializingSession = false;
    private boolean _isContentGeneration = false;
    private boolean _fullRefreshRequired = false;
    private int     _tabIndex = 0;

    private boolean _allowIncrementalUpdateApppend = true;
    private AWBaseResponse.AWResponseCompleteCallback _responseCompleteCallback;

    private List _globalFormInputIdPaths;

    private boolean _disableElementIdGeneration;

    private String _directActionClassName;
    private String _directActionName;

    private int _currentPhase;
    public static final int Phase_ApplyValues = 1;
    public static final int Phase_InvokeAction = 2;
    public static final int Phase_Render = 3;

    public void init (AWApplication application, AWRequest request)
    {
        this.init();
        _application = application;
        _request = request;
        if (_request != null) {
            /*
             This NPE can be caused by:
             //ariba/buyer/contentui/ariba/htmlui/content/PunchOutSetupRequestHandler.java
             If the AWRequest.init is never called with a valid
             HttpServletRequest, then the AWRequest object is in a fubar
             state, and method promised to be implemented by the interface
             will throw NPEs.
             */
            try {
                MultiTabSupport multiTabSupport = MultiTabSupport.Instance.get();
                _tabIndex = multiTabSupport.uriToTabNumber(request.uri(), 0);
            }
            catch (NullPointerException e) {
                _tabIndex = 0;
            }

            _isBrowserFirefox = _request.isBrowserFirefox();
            _isBrowserMicrosoft = _request.isBrowserMicrosoft();
            _isBrowserSafari = _request.isBrowserSafari();
            _isBrowserChrome = _request.isBrowserChrome();
            _isTooManyTabRequest = _request.isTooManyTabRequest();
            _frameName = request.frameName();
            String[] requestSenderIds = ((AWBaseRequest)request).senderIds();
            if (requestSenderIds != null) {
                String currentRequestSenderId = requestSenderIds[0];
                setCurrentRequestSenderId(currentRequestSenderId);
                if (requestSenderIds.length > 1) {
                    _requestSenderIds = ListUtil.arrayToList(requestSenderIds);
                    // pop the first -- it's set
                    ListUtil.removeFirstElement(_requestSenderIds);
                }
            }
           try {
                // override the render version that is set on the session
                if (_request.queryString() != null) {
                    String value = _request.formValueForKey("renderVersion");

                    if (!StringUtil.nullOrEmptyString(value)) {
                        try {
                            _queryRenderVersion = UIRenderMeta.RenderVersion
                                .valueOf(value);
                        }
                        catch (IllegalArgumentException e) {
                            // it's okay, invalid renderVersion provided
                            // we'll just use the default behavior
                        }
                    }
                }
            }
            catch (NullPointerException e) {
                //Request coming from PunchOutSetupRequestHandler.java
                // does not have query string, so it would raise Null POinter
                //Exception if we validate query string. We just catch the 
                //Exception and do nothing for the Punch Out to happen
            }
        }
        else {
            _isBrowserMicrosoft = true;
        }
        _browserMinWidth = _isBrowserMicrosoft ? "1%" : "1";
        _browserMaxWidth = _isBrowserMicrosoft ? "99%" : "100%";
        _elementIdGenerator = createElementIdGenerator();
        _isMetaTemplateMode = false;
        ThreadDebugState.set(RequestContextID, this);
        _dataValuePushedInInvokeAction = false;
        _disableElementIdGeneration = false;
    }

    /**
     * Return the request context if there is one is associated with the
     * current thread and null otherwise
     *
     * @return the request context or null
     */
    public static AWRequestContext _requestContext ()
    {
        return (AWRequestContext)ThreadDebugState.get(RequestContextID);
    }

    public AWValidationContext validationContext ()
    {
        return page().validationContext();
    }

    /**
     * Fetch the render version set by the URL query parameters; usually null.
     */
    public UIRenderMeta.RenderVersion getQueryRenderVersion ()
    {
        return _queryRenderVersion;
    }

    public boolean isDebuggingEnabled ()
    {
        return _isDebuggingEnabled;
    }

    public void setIsDebuggingEnabled (boolean state)
    {
       _isDebuggingEnabled = state;
    }

    public void setMetaTemplateMode (boolean flag)
    {
        _isMetaTemplateMode = flag;
    }

    public boolean isMetaTemplateMode ()
    {
        return _isMetaTemplateMode;
    }

    protected AWElementIdGenerator createElementIdGenerator ()
    {
        return new AWElementIdGenerator();
    }

    private boolean elementIdTracingEnabled ()
    {
        if (_awsession == null) {
            return false;
        }
        else {
            Boolean flag = ((Boolean)session().dict().get(AWConstants.ElementIdTracingEnabled));
            boolean flagValue = flag != null ? flag.booleanValue() : false;
            return flagValue && _isDebuggingEnabled;
        }
    }

    public void setPage (AWPage page)
    {
        if (page != _currentPage) {
            _currentPage = page;
            _pageRequiresPreGlidCompatibility = _currentPage.requiresPreGlidCompatibility();
            if (_allowsSkipping) {
                _allowsSkipping = !_pageRequiresPreGlidCompatibility;
            }
        }
    }

    public AWPage page ()
    {
        return _currentPage;
    }

    public AWComponent pageComponent ()
    {
        return _currentPage.pageComponent();
    }

    public AWComponent pageWithName (String pageName)
    {
        return _application.createPageWithName(pageName, this);
    }

    public AWComponent pageWithName (String pageName, Map<String, Object>assignments)
    {
        AWComponent page = pageWithName(pageName);
        if (!MapUtil.nullOrEmptyMap(assignments)) {
            for (Map.Entry<String, Object>e : assignments.entrySet()) {
                FieldValue.setFieldValue(page, e.getKey(), e.getValue());
            }
        }
        return page;
    }

    public void setRequestPage (AWPage page)
    {
        _requestPage = page;
    }

    public AWPage requestPage ()
    {
        return _requestPage;
    }

    public AWEncodedString responseId ()
    {
        if (_responseId == null) {
            AWSession session = session();
            int nextResponseId = session.nextResponseId();
            String responseIdString = AWUtil.integerIdString(nextResponseId);
            _responseId = AWEncodedString.sharedEncodedString(responseIdString);
        }
        return _responseId;
    }

    public void setFrameName (AWEncodedString frameName)
    {
        if (frameName != null) {
            if ((frameName == TopFrameName) || frameName.equals(TopFrameName)
                    || (frameName == SelfFrameName) || frameName.equals(SelfFrameName)) {
                _frameName = null;
            }
            else {
                _frameName = frameName;
            }
        }
    }

    public void setFrameName (String frameName)
    {
        setFrameName(AWEncodedString.sharedEncodedString(frameName));
    }

    public AWEncodedString frameName ()
    {
        return _frameName;
    }

    public void registerNewPageComponent (AWComponent pageComponent)
    {
        if (_newPages == null) {
            _newPages = new AWArrayManager(AWComponent.ClassObject);
        }
        _newPages.addElement(pageComponent);
    }

    /**
     * The name of this "setElementId" is retained for backward compatibility with code in the ariba.encoder.xml
     * package.  It would be better to call it "setElementIdGenerator", but then I've have to change more code
     * in ariba.encoder.xml.
     * @param elementIdGenerator
     */
    public void setElementId (AWElementIdGenerator elementIdGenerator)
    {
        _elementIdGenerator = elementIdGenerator;
    }

    public AWElementIdGenerator _getElementIdGenerator ()
    {
        return _elementIdGenerator;
    }

    //////////////////
    // BacktrackState
    //////////////////
    public void setBacktrackState (AWBacktrackState backtrackState)
    {
        _backtrackState = backtrackState;
    }

    public AWBacktrackState backtrackState ()
    {
        return _backtrackState;
    }

    //////////////////////////
    // Application/Session
    //////////////////////////
    public AWApplication application ()
    {
        return _application;
    }

    /**
     * Returns the session currently associated with this request context.  Only returns
     * a session if a previous call has associated the session with this request.  This
     * means that even if there is a valid session for this request, this method could
     * return null if there has not been a previous call to session() or httpSession().
     *
     * Should only be used by calls made during session initialization (since
     * circular calls to session(false) are not allowed).  All other checks for
     * session should use session(false).
     *
     * @aribaapi private
     */
    public AWSession existingSession ()
    {
        return _awsession;
    }

    public void setHttpSession (HttpSession httpSession)
    {
        _httpSession = httpSession;
        if (_httpSession == null) {
            _awsession = null;
        }
        else {
            // Note that we do not null out the sessionId as we need that to checkin and avoid deadlock.
            _httpSessionId = _httpSession.getId();
        }
    }

    public HttpSession existingHttpSession ()
    {
        return _httpSession;
    }

    public HttpSession createHttpSession ()
    {
        HttpSession httpSession = _application.createHttpSession(_request);
        String sessionId = httpSession.getId();
        // Take the lock for the sessionId
        _application.checkoutHttpSessionId(sessionId);

        setHttpSession(httpSession);
        int sessionTimeout = _application.sessionTimeout();
        if (sessionTimeout != -1) {
            // A value of -1 means to use the sevlet engines default (which is
            // usually controlled through some other mechanism like an admin app).
            httpSession.setMaxInactiveInterval(sessionTimeout);
        }
        initSession(httpSession);
        return httpSession;
    }

    protected void initSession (HttpSession httpSession)
    {
        _awsession = AWSession.session(httpSession);
        if (_awsession == null) {
            _awsession = createSessionForHttpSession(httpSession);
        }
        else {
            // This path used in conjunction with UIToolkit
            _awsession.ensureAwake(this);
        }
    }

    public HttpSession restoreHttpSessionForId (String sessionId,
                                                boolean checkRemoteHostAddress,
                                                boolean throwException)
    {
        HttpSession httpSession = _application.restoreHttpSession(_request, sessionId);
        if (httpSession == null) {
            _request.resetRequestId();
            if (throwException) {
                throw new AWSessionRestorationException("Unable to restore session with sessionId: \"" + sessionId + ".\"  Session possibly timed out.");
            }
            else {
                return null;
            }
        }
        // Take the lock for the sessionId
        _application.checkoutHttpSessionId(httpSession.getId());
        setHttpSession(httpSession);
        _awsession = AWSession.session(httpSession);
        if (_awsession == null) {
            _awsession = createSessionForHttpSession(httpSession);
        }
        _awsession.ensureAwake(this);
        if (!AWRecordingManager.isInPlaybackMode(this)) {
            checkRemoteHostAddress(_awsession, sessionId, checkRemoteHostAddress);
        }


        return httpSession;
    }

    private void checkRemoteHostAddress (AWSession session, String sessionId, boolean checkRemoteHostAddress)
    {
        AWPage page = session.restoreCurrentPage();
        if (page != null) {
            AWComponent pageComponent = page.pageComponent();
            if (pageComponent != null) {
                checkRemoteHostAddress = pageComponent.shouldValidateRemoteHost();
            }
        }
        if (checkRemoteHostAddress) {
            InetAddress sessionRemoteIPAddress = session.remoteIPAddress();
            int remoteHostMask = AWConcreteApplication.RemoteHostMask;
            if (sessionRemoteIPAddress != null && remoteHostMask != 0) {
                boolean remoteHostsMatch = false;
                String remoteHostAddress = _request.remoteHostAddress();
                if (remoteHostAddress != null) {
                    remoteHostsMatch = checkHostsAgainstMask(sessionRemoteIPAddress, remoteHostAddress, remoteHostMask);
                }
                if (!remoteHostsMatch) {
                    checkInExistingHttpSession();
                    String message = Fmt.S("Unable to restore session with sessionId: '%s'.  The remote address of the current request '%s' does not match the remote address of the initial request '%s' with mask '%s'.",
                        sessionId, remoteHostAddress, sessionRemoteIPAddress.getHostAddress(), String.valueOf(remoteHostMask));
                    Log.aribaweb_session.debug(message);
                    String sessionRemoteHost = sessionRemoteIPAddress.getHostAddress();
                    throw new AWRemoteHostMismatchException(sessionRemoteHost,
                                                            remoteHostAddress);
                }
            }
        }
    }

    protected AWSession createSessionForHttpSession (HttpSession httpSession)
    {
        AWSession session = _application.createSession(this);
        AWSession.setSession(httpSession, session);
        return session;
    }

    public HttpSession restoreHttpSessionForId (String sessionId)
    {
        return restoreHttpSessionForId(sessionId, true, true);
    }

    public HttpSession httpSession ()
    {
        return httpSession(true);
    }

    public HttpSession httpSession (boolean required)
    {
        if (_httpSession == null) {
            String sessionId = _request != null ? _request.sessionId() : null;

            if (sessionId == null) {
                if (required) {
                    createHttpSession();
                }
            }
            else {
                restoreHttpSessionForId(sessionId, true, required);
            }
        }
        return _httpSession;
    }

    /**
     * Returns the current AWSession associated with the client making the request or,
     * if if there is no current AWSession and required is true, throws
     * AWSessionRestorationException.
     * If required is false and the client making this request does not have a valid
     * AWSession, this method returns null.
     *
     * @param required
     * @aribaapi private
     */
    public AWSession session (boolean required)
    {
        if (_initializingSession) {
            // if we're in the middle of initializing and we get a call for an optional session, no need to blow up
            if (!required) return null;
            _initializingSession = false;
            throw new AWGenericException("Circular call to session(boolean flag) detected.  Ensure that session initialization does not call session(boolean flag).");
        }

        _initializingSession = true;
        try {
            if (_awsession == null && _request != null) {
                if (required && StringUtil.nullOrEmptyOrBlankString(_request.sessionId())) {
                    _request.resetRequestId();
                    _initializingSession = false;
                    throw new AWSessionRestorationException("Unable to restore session, no sessionId.");
                }
                else {
                    HttpSession httpSession = httpSession(required);
                    if (httpSession != null) {
                        initSession(httpSession);
                    }
                }
            }
        }
        finally {
            _initializingSession = false;
        }

        return _awsession;
    }

    public AWSession session ()
    {
        return session(true);
    }

    /**
     * Associates and returns a new AWSession.  If an existing session has been associated
     * with the current request, a FatalAssertionException will be thrown.
     *
     * @return newly created AWSession
     * @aribaapi private
     */
    public AWSession createSession ()
    {
        Assert.that(session(false) == null,
                    "createSession() called but request already has a valid AWSession");
        HttpSession httpSession = createHttpSession();
        initSession(httpSession);
        return _awsession;
    }

    /**
     * @return The Http Session or null if it's not available yet.
     */
    public HttpSession peekHttpSession ()
    {
        return _awsession == null ? null : _awsession.httpSession();
    }

    ////////////////////
    // Request/Response
    ////////////////////
    public AWRequest request ()
    {
        return _request;
    }

    // AW internal method for temporarilty overriding the request -- used for internal dispatch of direct actions
    public void _overrideRequest (AWRequest newRequest)
    {
        _request = newRequest;
    }

    /*
     * Do not use this method!
     *
     * For XMLHTTP responses use setXHRRCompatibleResponse .
     * For normal downloads return the response on the method invocation.
     * For large downloads return AWStreamingServletResponse on AWFileDownload.action binding
     */
    public void setResponse (AWResponse response)
    {
        // swapping responses is generally not permitted if doing an XMLHHTP incremental refresh
        assertFileDownloadCompatibleRequestRequired();
        setXHRRCompatibleResponse(response);
    }

    public AWResponse temporarilySwapReponse (AWResponse response)
    {
        AWResponse orig = _response;
        _response = response;
        return orig;
    }

    public void restoreOriginalResponse (AWResponse response)
    {
        _response = response;
    }

    public void setXHRRCompatibleResponse (AWResponse response)
    {
        _response = response;
        if (_responseCompleteCallback != null) {
            if (response instanceof AWBaseResponse) {
                ((AWBaseResponse)response).setResponseCompleteCallback(_responseCompleteCallback);
            }
        }
    }

    public AWResponse response ()
    {
        return _response;
    }

    public boolean isBrowserFirefox ()
    {
        return _isBrowserFirefox;
    }

    public boolean isBrowserMicrosoft ()
    {
        return _isBrowserMicrosoft;
    }

    public boolean isBrowserSafari ()
    {
        return _isBrowserSafari;
    }

    public boolean isBrowserChrome ()
    {
        return _isBrowserChrome;
    }

    public String browserMinWidth ()
    {
        return _browserMinWidth;
    }

    public String browserMaxWidth ()
    {
        return _browserMaxWidth;
    }

    ///////////////
    // SenderId
    ///////////////
    private void setCurrentRequestSenderId (String requestSenderId)
    {
        _currentRequestSenderId = requestSenderId;
        _currentRequestSenderIdPath = AWElementIdPath.lookup(requestSenderId);
        if (_currentRequestSenderIdPath == null) {
            // If the requestSenderId is invalid and no path can be found due to cache flush,
            // the skipping algorithm will skip every branch
            // Disallow skipping to force a full traversal.
            _currentRequestSenderIdPath = AWElementIdPath.emptyPath();
            _allowsSkipping = false;
        }
    }

    public String requestSenderId ()
    {
        return _currentRequestSenderId;
    }

    public AWElementIdPath requestSenderIdPath ()
    {
        return _currentRequestSenderIdPath;
    }

    public void dequeueSenderId ()
    {
        String nextId = null;
        if (_requestSenderIds != null && !_requestSenderIds.isEmpty()) {
            nextId = (String)ListUtil.removeFirstElement(_requestSenderIds);
        }
        setCurrentRequestSenderId(nextId);
    }

    public void enqueueSenderId (String newId)
    {
        if (_requestSenderIds == null) {
            _requestSenderIds = ListUtil.list();
        }
        _requestSenderIds.add(newId);
    }

    public boolean hasMoreSenderIds ()
    {
        return (_requestSenderIds != null) && (!_requestSenderIds.isEmpty());
    }

    /////////////////////
    // Element Id
    /////////////////////
    public AWElementIdPath nextElementIdPath ()
    {
        _elementIdGenerator.increment(1);
        return _elementIdGenerator.currentElementIdPath();
    }

    public AWEncodedString nextElementId ()
    {
        return nextElementIdPath().elementId();
    }

    public AWElementIdPath currentElementIdPath ()
    {
        return _elementIdGenerator.currentElementIdPath();
    }

    public AWEncodedString currentElementId ()
    {
        return currentElementIdPath().elementId();
    }

    public int currentElementIdPathLength ()
    {
        return _elementIdGenerator.currentLevel();
    }

    public void incrementElementId ()
    {
        _elementIdGenerator.increment(1);
    }

    public void incrementElementId (int amount)
    {
        _elementIdGenerator.increment(amount);
    }

    public void pushElementIdLevel (int elementIdComponent)
    {
        _elementIdGenerator.pushLevel(elementIdComponent);
    }

    public void pushElementIdLevel ()
    {
        _elementIdGenerator.pushLevel();
    }

    public void popElementIdLevel ()
    {
        _elementIdGenerator.popLevel();
    }

    public void popElementIdLevel (int elementIdComponent)
    {
        _elementIdGenerator.popLevel(0);
    }

    public int currentElementIdLevel ()
    {
        return _elementIdGenerator.currentLevel();
    }

    protected boolean nextPrefixMatches (AWElementIdPath elementIdPath)
    {
        return elementIdPath == null ? false : _elementIdGenerator.nextPrefixMatches(elementIdPath);
    }

    /**
        Disable element id generation is used for non-interactive page rendering
     */
    public void disableElementIdGeneration ()
    {
        _disableElementIdGeneration = true;
    }

    public String currentElementIdTrace ()
    {
        return _elementIdGenerator.toString();
    }

    ////////////////////
    // Glid Form Support
    ////////////////////
    public void setFormInputIds (AWArrayManager formInputIds)
    {
        if (_isPrintMode) {
            Log.aribawebvalidation_exportMode.debug("Attempt to record form input id in print mode.");
        }
        else {
            _formInputIds = formInputIds;
            _targetFormIdIndex = 0;
            _targetFormIdPath = _formInputIds == null ? null
                : (AWElementIdPath)_formInputIds.objectAt(_targetFormIdIndex);
        }
    }

    public void recordFormInputId (AWElementIdPath elementIdPath)
    {
        Assert.that(_currentForm != null, "Attempt to record form input id outside AWForm.");
        if (_isExportMode || _isPrintMode) {
            Log.aribawebvalidation_exportMode.debug("Attempt to record form input id in export mode.");
        }
        else {
            _formInputIds.addElement(elementIdPath);
        }
    }


    /**
     * Keeps track of this element id for all subsequent forms and add the element id to
     * all existing forms.
     * @param elementIdPath
     * @aribaapi private
     */
    public void recordGlobalFormInputId (AWElementIdPath elementIdPath)
    {
        if (!_isExportMode) {
            if (_globalFormInputIdPaths == null) {
                _globalFormInputIdPaths = ListUtil.list();
            }
            _globalFormInputIdPaths.add(elementIdPath);
            _currentPage.addGlobalFormInputIdPath(elementIdPath);
        }
    }

    public List getGlobalFormInputIdPaths ()
    {
        return _globalFormInputIdPaths;
    }

    public AWElementIdPath targetFormIdPath ()
    {
        return _targetFormIdPath;
    }

    public void popFormInputElementId ()
    {
        _targetFormIdIndex++;
        _targetFormIdPath = _targetFormIdIndex < _formInputIds.size() ?
            (AWElementIdPath)_formInputIds.objectAt(_targetFormIdIndex) :
            null;
    }

    //////////////
    // Cycling
    //////////////
    public void resetForNextCycle ()
    {
        if (elementIdTracingEnabled()) {
            _elementIdGenerator = new DebugElementIdGenerator(this);
        }
        if (_disableElementIdGeneration) {
            _elementIdGenerator = new NoOpElementIdGenerator();
        }
        _elementIdGenerator.reset();
        setFormIndex(0);
    }

    private void logActivityBegin (String phaseName)
    {
        String sessionId = _httpSession == null ? "-none-" : _httpSession.getId();
        String responseId = _awsession == null ? "-none-" : responseId().string();
        String actionLogMessage = Fmt.S("** sessionId: \"%s\" responseId: \"%s\" BEGIN: \"%s\" pageName: \"%s\"", sessionId, responseId, phaseName, _currentPage.pageComponent().name());
        _application.logActionMessage(actionLogMessage);
    }

    private void logActivityEnd (String phaseName)
    {
        String sessionId = _httpSession == null ? "-none-" : _httpSession.getId();
        String responseId = _awsession == null ? "-none-" : responseId().string();
        String actionLogMessage = Fmt.S("** sessionId: \"%s\" responseId: \"%s\" END:   \"%s\" pageName: \"%s\" **", sessionId, responseId, phaseName, _currentPage.pageComponent().name());
        _application.logActionMessage(actionLogMessage);
    }

    private String currentComponentPath ()
    {
        return _currentComponent == null ? "No current component" : _currentComponent.componentPath("\n").toString();
    }

    public void applyValues()
    {
        resetForNextCycle();
        _currentPhase = Phase_ApplyValues;
        boolean isActionLoggingEnabled = AWConcreteApplication.IsActionLoggingEnabled;
        if (isActionLoggingEnabled) {
            logActivityBegin("applyValues");
        }
        if (isPollRequest()) {
            ;
        }
        else if (isPollUpdateRequest()) {
            ;
        }
        else {
            // normal case
           _requestPage = _currentPage;
            try {
                String formSender = _request.formValueForKey(AWComponentActionRequestHandler.FormSenderKey);
                if (formSender != null) {
                    AWArrayManager formInputIds = _requestPage.getFormIds(formSender);
                    setFormInputIds(formInputIds);
                }
                _currentPage.applyValues();
            }
            catch (Throwable t) {
                String message = currentComponentPath();
                throw AWGenericException.augmentedExceptionWithMessage(message, t);
            }
        }
        if (isActionLoggingEnabled) {
            logActivityEnd("applyValues");
        }
    }

    public AWResponseGenerating invokeActionForRequest ()
    {
        AWResponseGenerating actionResults = null;
        resetForNextCycle();
        _currentPhase = Phase_InvokeAction;
        boolean isActionLoggingEnabled = AWConcreteApplication.IsActionLoggingEnabled;
        if (isActionLoggingEnabled) {
            logActivityBegin("invokeAction");
        }
        _requestPage = _currentPage;

        boolean moreSenderIds = true;
        AWResponseGenerating previousActionResults = null;

        do {
            try {
                if (isPollRequest()) {
                    _response = _application.createResponse(_request);
                    actionResults = _response;
                    boolean hasSessionChanged = false;
                    AWSession session = session(false);
                    if (session != null) {
                        hasSessionChanged = session.hasChanged();
                    }
                    if (hasSessionChanged || _currentPage.hasChanged()) {
                        _response.appendContent("<AWPoll state='update'/>");
                    }
                    else {
                        _response.appendContent("<AWPoll state='nochange'/>");
                    }

                    // Record perf trace info
                    if (PerformanceState.threadStateEnabled()) {
                        PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();
                        String sourcePage = _currentPage.perfPageName();
                        stats.setSourcePage(sourcePage);
                        stats.setSourceArea("poll");
                        stats.setDestinationPage(sourcePage);
                        stats.setType(PerformanceState.Type_User);
                    }
                }
                else if (isPollUpdateRequest()) {
                    // if we're doing a poll update, then just return null and rerender
                    // the current page
                    return null;
                }
                else if (page().downloadResponse() != null) {
                    //if we have a cached download response, return it.
                    actionResults = page().downloadResponse();
                    page().setDownloadResponse(null);
                }
                else {
                    try {
                        actionResults = _currentPage.invokeAction();

                        if ((actionResults instanceof AWResponse) && (actionResults != _response)) {
                            if (actionResults instanceof AWBaseResponse &&
                                    _responseCompleteCallback != null) {
                                ((AWBaseResponse)actionResults).setResponseCompleteCallback(
                                        _responseCompleteCallback);
                            }
                            //if we have a normal response (html or file download) on a
                            //XMLHttp request, cache this response on the page issue a
                            //retry. Return this response on the retry.
                            if (isXMLHttpIncrementalRequest()) {
                                page().setDownloadResponse((AWResponse)actionResults);
                                assertFileDownloadCompatibleRequestRequired();
                            }
                        }
                    }
                    catch (AWRequestContext.RetryRequestException e) {
                        // ignore: we already wrote the response
                        actionResults = _response;
                    }
                    catch (WrapperRuntimeException e) {
                        if (e.originalException() instanceof AWRequestContext.RetryRequestException) {
                            // ignore: we already wrote the response
                            actionResults = _response;
                        } else {
                            throw e;
                        }
                    }
                }
            }
            catch (Throwable t) {
                String message = "-- Component Path:\n" + currentComponentPath();
                throw AWGenericException.augmentedExceptionWithMessage(message, t);
            }
            finally {
                // clear the form input ids only if we have a component result.
                if (_formInputIds != null &&
                    actionResults instanceof AWComponent) {
                    _formInputIds.reset();
                    setFormInputIds(null);
                }
            }
            // if we need to invoke again, move on to the next senderId
            moreSenderIds = hasMoreSenderIds();
            if (moreSenderIds) {
                previousActionResults = actionResults;
                dequeueSenderId();
                resetForNextCycle();
            }
            if ((previousActionResults != null) && ((actionResults == null) ||
                (actionResults == _currentPage.pageComponent()))) {
                actionResults = previousActionResults;
            }
        }
        while (moreSenderIds);

        if (isPathDebugRequest() && (actionResults instanceof AWComponent)) {
            // do a renderResponse on this page so we populate the AWDebugTrace ComponentTraceNode tree
            debugTrace().didFinishPathTracePhase();
            // stopComponentPathRecording();
        }

        if (isActionLoggingEnabled) {
            logActivityEnd("invokeAction");
        }
        return actionResults;
    }

    private void assignCharacterEncoding (AWResponse response)
    {
        response.setCharacterEncoding(_currentPage.characterEncoding());
    }

//    private int _requestInterval = -1;
//    public void setRequestInterval (int requestInterval)
//    {
//        if (_requestInterval == -1 ||
//            requestInterval < _requestInterval) {
//            _requestInterval = requestInterval;
//        }
//    }

    private void updateRequestInterval ()
    {
        AWSession session = session(false);
        if (session != null) {
            AWPage page = page();
            if (page.isPollingInitiated()) {
                session.setRequestInterval(page.getPollInterval());
            }
        }
    }

    private boolean isPollRequest ()
    {
        return AWPollInterval.AWPollSenderId.equals(requestSenderId());
    }

    public boolean isPollUpdateRequest ()
    {
        return AWPollInterval.AWPollUpdateSenderId.equals(requestSenderId());
    }

    public boolean isInPageRequest ()
    {
        return "1".equals(formValueForKey(InPageRequest));
    }

    public AWResponse generateResponse (AWResponse response)
    {
        try {
            resetForNextCycle();
            _currentPhase = Phase_Render;
            boolean isActionLoggingEnabled = AWConcreteApplication.IsActionLoggingEnabled;
            if (isActionLoggingEnabled) {
                logActivityBegin("renderResponse");
            }
            _responsePage = _currentPage;
            _response = (response != null) ? response : _application.createResponse(_request);
            if ((_response instanceof AWBaseResponse) && isXMLHttpIncrementalRequest()) {
                ((AWBaseResponse)_response).setWriteRefreshRegionBoundaryMarkers(true);
            }
            if (_cookies != null) {
                for (int i = 0, size = _cookies.size(); i < size; i++) {
                    _response.addCookie((AWCookie)_cookies.get(i));
                }
            }
            _currentPage.ensureAwake(this);
            try {
                _currentPage.renderResponse();
            }
            catch (WrapperRuntimeException e) {
                if (e.originalException() instanceof AWRequestContext.RetryRequestException) {
                    // ignore: we already wrote the response
                } else {
                    throw e;
                }
            }

            if (isActionLoggingEnabled) {
                logActivityEnd("renderResponse");
            }
            assignCharacterEncoding(_response);

            updateRequestInterval();
        }
        catch (AWSessionRestorationException e) {
            throw e;
        }
        catch (Throwable t) {
            Log.aribaweb.info(9023,
                              "Runtime exception in AWRequestContext.generateResponse",
                              SystemUtil.stackTrace(t));
            String message = currentComponentPath();
            throw AWGenericException.augmentedExceptionWithMessage(message, t);
        }
        finally {
            // Note: we only clear the _userState at the end of renderResponse which means the app
            // can pass values from one phase to the next in this hashtable.
            clear();
        }

        // stash debug trace for debug panel
        if (_debugTrace != null) session().dict().put("_AWLastDebugTrace", _debugTrace);

        return _response;
    }

    public void addCookie (AWCookie cookie)
    {
        _didAddCookies = true;
        if (_response != null) {
            // if a response is already available, then just add the cookie
            _response.addCookie(cookie);
        }
        else {
            // otherwise, store cookies until generateResponse
            if (_cookies == null) {
                _cookies = ListUtil.list();
            }
            _cookies.add(cookie);
        }
    }

    public boolean didAddCookies ()
    {
        return _didAddCookies;
    }

    public AWResponse generateResponse ()
    {
        return generateResponse(_response);
    }

    public AWResponse handleRequest (AWRequest request, AWRequestHandler requestHandler)
    {
        // subclasses can overide this method to have access to both the request and
        // response in the same scope on a per request basis.
        return requestHandler.handleRequest(request, this);
    }

    public void checkInExistingHttpSession ()
    {
        try {
            if (_httpSession != null) {
                _application.archiveHttpSession(_httpSession);
            }
        } finally {
            if (_httpSessionId != null) {
                _application.checkinHttpSessionId(_httpSessionId);
                if (_httpSession == null) {
                    logString("Error: httpSessionId exists but httpSession is null.");
                }
            }
        }
    }

    private RuntimeException putToSleep (AWPage page, RuntimeException existingExcpetion)
    {
        RuntimeException sleepException = existingExcpetion;
        if (page != null) {
            try {
                page.ensureAsleep();
            }
            catch (RuntimeException runtimeException) {
                if (sleepException == null) {
                    sleepException = runtimeException;
                }
            }
        }
        return sleepException;
    }

    public void sleep ()
    {
        cleanupThreadLocalState();
        try {
            RuntimeException sleepException = null;
            sleepException = putToSleep(_requestPage, sleepException);
            if (_responsePage != _requestPage) {
                sleepException = putToSleep(_responsePage, sleepException);
            }
            if (_currentPage != _responsePage && _currentPage != _requestPage) {
                sleepException = putToSleep(_currentPage, sleepException);
            }
            if (_newPages != null) {
                AWComponent[] newPagesArray = (AWComponent[])_newPages.array();
                for (int index = _newPages.size() - 1; index >= 0; index--) {
                    try {
                        AWComponent newPageComponent = newPagesArray[index];
                        // temporary workaround              ////////////////////////////
                        if (newPageComponent == null) break; ////////////////////////////
                        newPageComponent.ensureAsleep();
                    }
                    catch (RuntimeException runtimeException) {
                        if (sleepException == null) {
                            sleepException = runtimeException;
                        }
                    }
                }
            }

            if (_awsession != null) {
                _awsession.ensureAsleep();
            }
            if (sleepException != null) {
                throw sleepException;
            }
        }
        finally {
            _currentPage = null;
            _request = null;
            _response = null;
            _responseId = null;
            _elementIdGenerator = null;
            _userState = null;
            _currentForm = null;
            _requestPage = null;
            _responsePage = null;
            _requestSenderIds = null;
            _frameName = null;
            _backtrackState = null;
            _newPages = null;
            _currentComponent = null;
            /* NOTE: The following code should not be executed.
            // The _httpSession is needed in here after sleep executes
            // Do not remove this comment.
            //_httpSession = null;
            */
        }
    }

    //////////////////
    // User State
    //////////////////
    public Object get (String keyString)
    {
        return (_userState == null) ? null : _userState.get(keyString);
    }

    public void put (String keyString, Object objectValue)
    {
        if (_userState == null) {
            _userState = MapUtil.map();
        }
        if (objectValue == null) {
            _userState.remove(keyString);
        }
        else {
            _userState.put(keyString, objectValue);
        }
    }

    public Object remove (String keyString)
    {
        return (_userState == null) ? null : _userState.remove(keyString);
    }

    public void clear ()
    {
        if (_userState != null) {
            _userState.clear();
        }
        if (_cookies != null) {
            _cookies.clear();
        }
    }

    protected Map userData ()
    {
        return _userState;
    }

    ////////////////////
    // Form Handling
    ////////////////////
    public void setCurrentForm (AWHtmlForm currentForm)
    {
        if (currentForm != null && _currentForm != null) {
            throw new AWGenericException("Nested forms detected.");
        }
        _currentForm = currentForm;
    }

    public AWHtmlForm currentForm ()
    {
        return _currentForm;
    }

    public void incrementFormIndex ()
    {
        setFormIndex(_formIndex + 1);
    }

    private void setFormIndex (int index)
    {
        _formIndex = index;
    }

    public int formIndex ()
    {
        return _formIndex;
    }

    /**
        This could live in a util module.
    */
    private boolean checkHostsAgainstMask (InetAddress address1, String hostAddress, int mask)
    {
        InetAddress address2 = null;

        try {
            address2 = InetAddress.getByName(hostAddress);

            byte[] bytes1 = address1.getAddress();
            int addr1  = bytes1[3] & 0xff;
            addr1 |= ((bytes1[2] << 8) & 0xff00);
            addr1 |= ((bytes1[1] << 16) & 0xff0000);
            addr1 |= ((bytes1[0] << 24) & 0xff000000);

            byte[] bytes2 = address2.getAddress();
            int addr2  = bytes2[3] & 0xff;
            addr2 |= ((bytes2[2] << 8) & 0xff00);
            addr2 |= ((bytes2[1] << 16) & 0xff0000);
            addr2 |= ((bytes2[0] << 24) & 0xff000000);

            return (addr1 & mask) == (addr2 & mask);

        } catch (UnknownHostException e) {
            // this should never happen because the string should be %d.%d.%d.%d;
            e = null;
        }
        return false;
    }

    /////////////////////
    // Debug State
    /////////////////////
    public Object debugState ()
    {
        Map ht = MapUtil.map();
        String componentPath = _currentComponent == null ?
            "-- no current component --" : _currentComponent.componentPath().toString();
        ht.put("AWComponentPath", componentPath);
        return ht;
    }

    public void setCurrentDirectAction(String directActionClassName,
            String directActionName)
    {
        _directActionClassName = directActionClassName;
        _directActionName = directActionName;
    }

    public String getDirectActionClassName ()
    {
        return _directActionClassName;
    }

    public String getDirectActionName ()
    {
        return _directActionName;
    }

    public void setCurrentComponent (AWComponent currentComponent)
    {
        _currentComponent = currentComponent;
    }

    public AWComponent getCurrentComponent ()
    {
        return _currentComponent;
    }

    /**
     * @deprecated use pushCurrentElement / popCurrentElement instead
     */
    public void setCurrentElement (AWBaseElement element)
    {
        _currentElement = element;
    }

    public AWBaseElement pushCurrentElement (AWBaseElement element)
    {
        AWBaseElement old = _currentElement;
        _currentElement = element;
        return old;
    }

    public void popCurrentElement (AWBaseElement prev)
    {
        if (_componentPathDebuggingEnabled) debugTrace().existingElement(_currentElement);
        _currentElement = prev;
    }


    public AWBaseElement getCurrentElement ()
    {
        return _currentElement;
    }

    /**
     * This may change during a request as not all components want this enabled.
     */
    public void enableComponentPathDebugging ()
    {
        AWSession session = session(false);
        if (session == null) {
            _componentPathDebuggingEnabled = false;
        }
        else {
            Boolean flag = ((Boolean)session().dict().get(AWConstants.ComponentPathDebugFlagKey));
            boolean flagValue = flag != null ? flag.booleanValue() : false;
            _componentPathDebuggingEnabled = (flagValue || wasPathDebugRequest()) && isDebuggingEnabled();
        }
    }

    public void disableComponentPathDebugging ()
    {
        _componentPathDebuggingEnabled = false;
    }

    public boolean componentPathDebuggingEnabled ()
    {
        return _componentPathDebuggingEnabled;
    }

    protected AWDebugTrace _debugTrace;

    public AWDebugTrace debugTrace ()
    {
        if (_debugTrace == null) _debugTrace = new AWDebugTrace(this);
        return _debugTrace;
    }

    public int currentPhase ()
    {
        return _currentPhase;
    }

    public AWDebugTrace lastDebugTrace ()
    {
        return (AWDebugTrace)session().dict().get("_AWLastDebugTrace");
    }

    public void pushCurrentComponent (AWComponent component)
    {
        setCurrentComponent(component);
        if (_componentPathDebuggingEnabled && (_currentPhase == Phase_Render)) {
            debugTrace().pushTraceNode(component.componentReference());
        }
    }

    public void popCurrentComponent (AWComponent parent)
    {
        if (_currentPhase == Phase_Render && _debugShouldRecord()) {
            _currentComponent._debugRecordMapping (this, _currentComponent);
        }
        setCurrentComponent(parent);
        if (_componentPathDebuggingEnabled && (_currentPhase == Phase_Render)) {
            debugTrace().popTraceNode();
        }
    }

    public void suppressTraceForCurrentScopingElement ()
    {
        if (_componentPathDebuggingEnabled) debugTrace().suppressTraceForCurrentScopingElement();
    }

    public void markNextComponentAsMainInTrace ()
    {
        if (_componentPathDebuggingEnabled) debugTrace().markNextComponentAsMainInTrace();
    }

    int _isPathDebugRequest = -1;

    public boolean isPathDebugRequest ()
    {
        if (_isPathDebugRequest == -1) {
            _isPathDebugRequest = _isDebuggingEnabled && formValueForKey("cpDebug") != null ? 1 : 0;
        }
        return _isPathDebugRequest == 1;
    }

    public boolean wasPathDebugRequest ()
    {
        if (_isPathDebugRequest == -1) isPathDebugRequest();
        return (_isPathDebugRequest != 0);
    }


    public void stopComponentPathRecording ()
    {
        _isPathDebugRequest = -2;
    }

        //////////////////////
        // Record and Playback
        ///////////////////////
    private StringArray _semanticKeyPrefixes = new StringArray();

    public void _debugPushSemanticKeyPrefix ()
    {
        _semanticKeyPrefixes.add(null);
    }

    public void _debugPopSemanticKeyPrefix ()
    {
        int currentLevel = _semanticKeyPrefixes.inUse() - 1;
        if (currentLevel < 0) {
            Assert.assertNonFatal(false, "unbalanced AWFor level push/pop");
        }
        else {
            _semanticKeyPrefixes.remove(currentLevel);
        }
    }

    public void _debugSetSemanticKeyPrefix (String prefix)
    {
        int currentLevel = _semanticKeyPrefixes.inUse() - 1;
        if (currentLevel >= 0) {
            _semanticKeyPrefixes.array()[currentLevel] = prefix;
        }
        else {
            Assert.assertNonFatal(false, "can't increment without pushing");
        }
    }

    public String _debugSemanticKeyPrefix ()
    {
        FastStringBuffer sb = null;
        int currentLevel = _semanticKeyPrefixes.inUse() - 1;
        String[] array = _semanticKeyPrefixes.array();
        for (int index = 0; index <= currentLevel; index++) {
            String prefix = array[index];
            if (prefix != null) {
                if (sb == null) {
                    sb = new FastStringBuffer(prefix);
                }
                else {
                    sb.append("_");
                    sb.append(prefix);
                }
            }
        }
        return sb == null ? null : sb.toString();
    }

    // record & playback, need to responseId for this requestContext without incrementing it
    public AWEncodedString _debugResponseIdAsIs ()
    {
        return _responseId;
    }

    public boolean _debugIsInPlaybackMode ()
    {
        // do lazy initialization here
        if (_debugIsInPlaybackMode == null) {
            _debugIsInPlaybackMode = (_request != null) ?
                AWRecordingManager.isInPlaybackMode(this) : Boolean.FALSE;
        }

        return _debugIsInPlaybackMode;
    }

    public boolean _debugIsInRecordingMode ()
    {
        // do lazy initialization here
        if (_debugIsInRecordingMode == null) {
            _debugIsInRecordingMode = (_request != null) ?
                AWRecordingManager.isInRecordingMode(this) : Boolean.FALSE;
        }

        return _debugIsInRecordingMode;
    }

    public void setDebugIsInRecordingMode (boolean value)
    {
        _debugIsInRecordingMode = value;
    }

    // this is need to turn
    public void _debugSkipRecordPlayback ()
    {
        _debugIsInRecordingMode = false;
        _debugIsInPlaybackMode = false;
    }

    public boolean _debugShouldRecord ()
    {
        return _debugIsInPlaybackMode() || _debugIsInRecordingMode();
    }

    public static void cleanupThreadLocalState ()
    {
        ThreadDebugState.remove(RequestContextID);
    }

    //////////////////////
    // SSORequestContext
    ///////////////////////
    public String cookieValueForKey (String key)
    {
        return request().cookieValueForKey(key);
    }

    public String formValueForKey (String key)
    {
        return request().formValueForKey(key);
    }

    public Map formValues ()
    {
        return request().formValues();
    }

    public boolean isRequestSecure ()
    {
        return request().isSecureScheme();
    }

    public AWComponent createPageWithName (String name)
    {
        return application().createPageWithName(name, this);
    }

    public int sessionTimeout ()
    {
        return application().sessionTimeout();
    }

    public String requestUrl ()
    {
        return request().requestString();
    }

    public boolean isPrintMode ()
    {
        return _isPrintMode;
    }

    public void setIsPrintMode (boolean isPrintMode)
    {
        _isPrintMode = isPrintMode;
    }

    public boolean isExportMode ()
    {
        return _isExportMode;
    }

    public void setExportMode (boolean isExportMode)
    {
        _isExportMode = isExportMode;
    }

    protected boolean pageRequiresPreGlidCompatibility ()
    {
        return _pageRequiresPreGlidCompatibility;
    }

    protected boolean allowsSkipping ()
    {
        return _allowsSkipping;
    }

        // Called by components that require a data value to be pushed
        // in invoke action instead of during take values. One reason why
        // this may be necessary is that the data value from input controls like radio buttons, check
        // boxes, and popup menus, may change the structure of the page, and interfere with take values.
        // setting this flag will tell the application that triggers need to fire after the action has
        // already been invoked.

    public void setDataValuePushedInInvokeAction (boolean value)
    {
        _dataValuePushedInInvokeAction = value;
    }

    public boolean dataValuePushedInInvokeAction ()
    {
        return _dataValuePushedInInvokeAction;
    }

    public boolean isIncrementalUpdateRequest ()
    {
        return _request != null &&
               _request.formValuesForKey(IncrementalUpdateKey) != null;
    }

    public boolean isXMLHttpIncrementalRequest ()
    {
        if (_request != null) {
            String key = _request.formValueForKey(IncrementalUpdateKey);
            return (key != null && key.equals("xmlhttp"));
        }
        return false;
    }

    public static class RetryRequestException extends RuntimeException {
    }

    public void assertFileDownloadCompatibleRequestRequired ()
    {
        if (isXMLHttpIncrementalRequest()) {
            _response = _application.createResponse(_request);
            _response.appendContent("<script>ariba.Request.__retryRequest('"
                                + requestSenderId() + "');\n"
                    + "ariba.Request.refreshRequestComplete();\n"
                    + "</script>\n");
            throw new RetryRequestException();
        }
    }

    // Whether this request has been flagged as allowing non-strict component rendezvous
    // e.g. on reply of an action request that resulted in login (and thus expected change in page structure)
    public boolean allowFailedComponentRendezvous ()
    {
        return request().formValueForKey(AWBaseRequest.AllowFailedComponentRendezvousFormKey) != null;
    }

    public boolean isContentGeneration ()
    {
        return _isContentGeneration;
    }

    public void isContentGeneration (boolean flag)
    {
        _isContentGeneration = flag;
    }

    public void forceFullPageRefresh ()
    {
        _fullRefreshRequired = true;
    }

    public boolean fullPageRefreshRequired ()
    {
        return _fullRefreshRequired;
    }

    static final String _ForceReRender = "RR_FoRR";
    public void forceRerender ()
    {
        forceFullPageRefresh();
        put(_ForceReRender, true);
    }

    public boolean forceRerenderRequired ()
    {
        return fullPageRefreshRequired() && get(_ForceReRender) != null;
    }

    public boolean allowIncrementalUpdateApppend ()
    {
        // NOTE: this defaults to true.  If set to false and the request is initiated
        // via an incrementalUpdateRequest, then the response will be generated directly
        // into the incrementalRequest iframe.
        return _allowIncrementalUpdateApppend;
    }

    public void allowIncrementalUpdateApppend (boolean flag)
    {
        _allowIncrementalUpdateApppend = flag;
    }

    public void setHistoryRequest (boolean historyRequest)
    {
        _isHistoryRequest = historyRequest;
    }
    public boolean isHistoryRequest ()
    {
        return _isHistoryRequest;
    }

    public void setHistoryAction (int historyAction)
    {
        _historyAction = historyAction;
        setHistoryRequest(true);
    }

    public int historyAction ()
    {
        return _historyAction;
    }

    /**
     * @deprecated use put(key,value) and get(key) API's directly
     */
    public Map dict ()
    {
        if (_userState == null) {
            _userState = MapUtil.map();
        }
        return _userState;
    }

    public void setResponseCompleteCallback (AWBaseResponse.AWResponseCompleteCallback callback)
    {
        _responseCompleteCallback = callback;
    }

//    private void responseCompleted ()
//    {
//        if (_responseCompleteCallback != null) {
//            _responseCompleteCallback.responseCompleted();
//        }
//    }

    public boolean isAccessibilityEnabled ()
    {
        AWSession session = session(false);
        return (session != null) ? session.isAccessibilityEnabled() : false;
    }

    /*
        return omitLink() || requestContext().isStaticGeneration();
    }

    public String staticUrlForActionResults ()
    {
        return requestContext().staticUrlForActionResults(AWGenericActionTag.evaluateActionBindings(this, _pageNameBinding, _actionBinding));

     */

    public boolean isStaticGeneration ()
    {
        return ((AWConcreteApplication)_application).getStaticizer() != null;
    }

    /**
     * Evaluate the current URL is a tabbed one.
     * @return Is tabbed request.
     */
    public boolean isTabbed ()
    {
        return 0 < _tabIndex;
    }

    /**
     * Fetch the tab index.
     * @return The tab index.
     */
    public int getTabIndex ()
    {
        return _tabIndex;
    }

    public String staticUrlForActionResults (AWResponseGenerating responseGenerating)
    {
        if (responseGenerating == null) {
            // Assert.assertNonFatal(false, "Null actionResults");
            return "#";
        }
        if (responseGenerating instanceof AWResponseGenerating.ResponseSubstitution) {
            responseGenerating = ((AWResponseGenerating.ResponseSubstitution)responseGenerating).replacementResponse();
        }

        Assert.that(responseGenerating instanceof AWComponent, "Static link returned non-AW Component results");
        return ((AWConcreteApplication)_application).getStaticizer().note((AWComponent)responseGenerating);
    }

    /*
        Support for item-scoped subcomponent state.
        See AWFor scopeSubcomponentsByItem for more details.

        We are keying subcomponent state trees by a combo-key of elementId *prefix* (parent path)
        and object (item) identity, so that subcomponent lookup will map based on the object (item
        of the For) rather than *position* -- inserting / removing objects from the array
        of the For (or re-ordering the array via a sort) will thereby not disturb the rendezvous
        with existing stateful components.

        We accomplish this by storing an <item, id-suffix> subcomponent state map, keyed by the id of the parent --
        ** we effectively ignore the part of the elementId that contains the position in the For, instead
        using the item (object) identity, in the lookup key.

        To make this (reasonably) efficient we:
            - Are lazy in creating scope objects -- we only create the scope if stateful components
                (or other sub-scopes) are actually accessed.
            - Are lazy in creating scope hashtables -- we only create them when used.
        So, a leaf For that actually contains no stateful sub-components consumes no overhead, and one
        with subcomponents results in a single allocation of a scope and MultiKeyHashTable (plus entries
        in the hashtable for any stateful components -- but we had those in the base case).
     */
    private SubcomponentScope _currentSubcomponentScope = null;
    private Object _scopeItem;
    private AWElementIdPath _scopeParentPath;

    protected void _pushSubcomponentScope(AWElementIdPath parentPath, Object item)
    {
        // we're lazy about actually instantiating scopes.  We'll push remember the leaf scope params
        // until we're either ased for the scope, or are asked to push another child.

        // force resolution of existing parent, if any
        if (_scopeParentPath != null) _currentLookupScope();
        _scopeParentPath = parentPath;
        _scopeItem = item;
    }

    protected void _popSubcomponentScope()
    {
        if (_scopeParentPath != null) {
            _scopeItem = null;
            _scopeParentPath = null;
        } else {
            _SubcomponentLookup prev = _currentSubcomponentScope._prevScope;
            _currentSubcomponentScope = (prev instanceof SubcomponentScope) ? (SubcomponentScope)prev : null;
        }
    }

    protected _SubcomponentLookup _currentLookupScope ()
    {
        // Lazily instantiate sub scope if necessary
        if (_scopeParentPath != null) {
            _SubcomponentLookup prevScope = (_currentSubcomponentScope != null) ? (_SubcomponentLookup)_currentSubcomponentScope : page();

            _currentSubcomponentScope = (SubcomponentScope)prevScope.get(_scopeParentPath);
            if (_currentSubcomponentScope == null) {
                _currentSubcomponentScope = new SubcomponentScope(_scopeParentPath);
                prevScope.put(_scopeParentPath, _currentSubcomponentScope);
            }
            _currentSubcomponentScope.prepare(_scopeItem, prevScope);
            _scopeItem = null;
            _scopeParentPath = null;
        }
        return (_currentSubcomponentScope != null)
                ? (_SubcomponentLookup)_currentSubcomponentScope : page();
    }

    protected AWComponent getStatefulComponent(AWElementIdPath path)
    {
        return _currentLookupScope().getStatefulComponent(path);
    }

    protected void putStatefulComponent(AWElementIdPath path, AWComponent instance)
    {
        _currentLookupScope().putStatefulComponent(path, instance);
    }

    public boolean isTooManyTabRequest ()
    {
        return _isTooManyTabRequest;
    }

    // implemented by SubcomponentScope and AWPage
    public interface _SubcomponentLookup
    {
        public AWComponent getStatefulComponent(AWElementIdPath path);
        public void putStatefulComponent(AWElementIdPath path, AWComponent instance);
        public Object get (Object key);
        public void put (Object key, Object value);
    }

    protected static class SubcomponentScope implements _SubcomponentLookup
    {
        protected AWElementIdPath _parentPath;
        protected ScopedIdMap _map;
        protected Object _currentItem;
        protected _SubcomponentLookup _prevScope;

        public SubcomponentScope (AWElementIdPath parentPath)
        {
            _parentPath = parentPath;
        }

        public void prepare (Object item, _SubcomponentLookup prevScope)
        {
            _currentItem = item;
            _prevScope = prevScope;
        }

        protected ScopedIdMap mapForCurrentItem ()
        {
            if (_map == null) {
                _map = new ScopedIdMap(_parentPath.privatePath().length + 2);
            }
            return _map;
        }

        public AWComponent getStatefulComponent(AWElementIdPath path)
        {
            return (AWComponent)mapForCurrentItem().get(_currentItem, path);
        }

        public void putStatefulComponent(AWElementIdPath path, AWComponent instance)
        {
            mapForCurrentItem().put(_currentItem, path, instance);
        }

        public Object get (Object key)
        {
            return mapForCurrentItem().get(_currentItem, key);
        }

        public void put (Object key, Object value)
        {
            mapForCurrentItem().put(_currentItem, key, value);
        }
    }

    /*
        Multi-key hashmap: <Item, ElementIdPath>
            Item uses identity equality.
            ElementIdPath limits comparisons to the path elements after skipping prefixLength elements
    */
    protected static class ScopedIdMap extends ariba.util.core.MultiKeyHashtable {
        int _prefixLength;

        public ScopedIdMap (int prefixLength)
        {
            super(2);
            _prefixLength = prefixLength;
        }

        protected int getHashValueForObject (Object o, int index)
        {
            return (index == 0) ? System.identityHashCode(o) : ((AWElementIdPath)o).hashCodeSkipping(_prefixLength);
        }

        protected boolean objectsAreEqualEnough (Object obj1, Object obj2, int index)
        {
            return (obj1 == obj2)
                    || ((index == 1) && (obj1 != null)
                         && ((AWElementIdPath)obj1).equalsSkipping((AWElementIdPath)obj2, _prefixLength));
        }
    }

    static class NoOpElementIdGenerator extends AWElementIdGenerator
    {
        protected void increment (int amount)
        {
            return;
        }

        public void pushLevel ()
        {
            return;
        }

        public void popLevel ()
        {
            return;
        }

        public void pushLevel (int elementIdComponent)
        {
            return;
        }

        public void popLevel (int elementIdComponent)
        {
            return;
        }

        public AWElementIdPath currentElementIdPath ()
        {
            return AWElementIdPath.noOpPath();
        }
    }

    static class DebugElementIdGenerator extends AWElementIdGenerator
    {
        private AWRequestContext _requestContext;
        private List _trace;

        public DebugElementIdGenerator (AWRequestContext requestContext)
        {
            super();
            _requestContext = requestContext;
            _trace = ListUtil.list();
        }

        public void reset ()
        {
            _trace = ListUtil.list();
            super.reset();
        }

        protected void increment (int amount)
        {
            String traceElement = Fmt.S("\nincrement %s\n", amount);
            addTraceElement(traceElement);
            addContextToTrace();
            super.increment(amount);
        }

        public void pushLevel ()
        {
            addTraceElement("\npushLevel\n");
            addContextToTrace();
            super.pushLevel();
        }

        public void popLevel ()
        {
            addTraceElement("\npopLevel\n");
            addContextToTrace();
            super.popLevel();
        }

        private void addContextToTrace ()
        {
            AWBaseElement element = _requestContext.getCurrentElement();
            if (element != null) {
                addTraceElement(element.toString());
            }
            else {
                addTraceElement("no element");
            }
        }

        private void addTraceElement (String traceElement)
        {
            if (_trace.size() > 200) {
                ListUtil.removeFirstElement(_trace);
            }
            _trace.add(traceElement);
        }

        public String toString ()
        {
            return _trace.toString();
        }
    }
}
