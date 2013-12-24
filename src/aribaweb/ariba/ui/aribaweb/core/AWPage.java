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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWPage.java#138 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWArrayManager;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.AWChangeNotifier;
import ariba.ui.aribaweb.util.AWCharacterEncoding;
import ariba.ui.aribaweb.util.AWCommunityContext;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.util.AWDisposable;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWEnvironmentStack;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResourceManager;
import ariba.ui.aribaweb.util.AWSingleLocaleResourceManager;
import ariba.ui.aribaweb.util.AWStringDictionary;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.Log;
import ariba.util.core.Constants;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.StringUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpSession;

public final class AWPage extends AWBaseObject implements AWDisposable, AWRequestContext._SubcomponentLookup
{
    private static final AWEncodedString SetPageScroll = AWEncodedString.sharedEncodedString("ariba.Dom.setPageScroll");
    private static final AWEncodedString RedirectStringStart =
        new AWEncodedString("<script id='AWRefreshComplete'>");
    private static final AWEncodedString RedirectStringEnd =
        new AWEncodedString("ariba.Request.redirectRefresh();</script>");

    private AWComponent _pageComponent;
    private List _modalPanels;
    private Map _subcomponents;
    private Map _subcomponentTraces;
    private AWRequestContext _requestContext;
    private HttpSession _httpSession;
    private AWSession _session;
    private AWStringDictionary _otherBindingsValues;
    private AWSingleLocaleResourceManager _resourceManager;
    private AWCharacterEncoding _characterEncoding;
    private TimeZone _clientTimeZone;
    private AWEnvironmentStack _environmentStack;
    private AWBacktrackState _currentBacktrackState;
    private AWBacktrackState _previousBacktrackState;
    private AWBacktrackState _nextBacktrackState;
    private boolean _hasBeenHibernated = false;
    protected boolean isBrowserMicrosoft = true;
    protected boolean isMacintosh = false;
    private AWResponse _downloadResponse;
    private AWTemplateParser _templateParser;
    private AWEncodedString _pageScrollTop = AWConstants.Zero;
    private AWEncodedString _pageScrollLeft = AWConstants.Zero;
    private Map _userState;
    private Map _formIds;
    // this is the validation context used by component api integrity checking
    private AWValidationContext _validationContext = null;
    // this is the errorManager used by components (like AWTextField and AWTextArea) to register parsing errors.
    private AWErrorManager _errorManager;
    private AWErrorManager _foregroundErrorManager;
    private AWHiddenFormValueManager _hiddenFormValueManager;

    // grid indexed by component class name and configuration name to store configuration objects.
    // flushed and disposed on page transitions.
    private Map _componentConfigurations;

    private AWChangeNotifier _changeNotifier = null;
    private boolean _hasChanged = false;
    private boolean _initiatePolling = false;
    private int _pollInterval = defaultPollInterval();
    List _currScriptList;

    // the community context to be passed when requesting help
    private AWCommunityContext _communityContext;

    // Allocated by the RequestHistory (but maintained by AWPage)
    public static class BrowserState {
        protected List _pageScriptList;
        protected AWBaseResponse _previousResponse;
        protected AWPage _lastPage;
        protected int _incrementalRefreshCount;
    }

    public static boolean AllowCrossPageRefresh = true;
    public static boolean AllowParentFrame = false;
    public static boolean AllowIncrementalScriptLoading () { return AllowCrossPageRefresh; }
    public static boolean DeferGlobalScopeScript () { return true; }

    // Max # of incremental refreshes to perform before forcing an FPR to clear out browser leaks
    public static final int NewPageForceFPRThreshold = 30;
    public static final int SamePageForceFPRThreshold = 99999;

    public void dispose ()
    {
        // Note: not disposing of shared objects like _resourceManager, _characterEncoding, etc.
        Object disposeTarget = null;

        _otherBindingsValues = null;
        _environmentStack = null;
        _currentBacktrackState = null;
        _previousBacktrackState = null;
        _nextBacktrackState = null;

        disposeTarget = _subcomponents;
        _subcomponents = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _subcomponentTraces;
        _subcomponentTraces = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _requestContext;
        _requestContext = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _httpSession;
        _httpSession = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _session;
        _session = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _userState;
        _userState = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _validationContext;
        _validationContext = null;
        AWUtil.dispose(_validationContext);
    }

    public AWPage (AWComponent pageComponent, AWRequestContext requestContext)
    {
        super();
        setPageComponent(pageComponent);
        setRequestContext(requestContext);
    }

    //////////////////
    // Accessors
    //////////////////
    private void setPageComponent (AWComponent component)
    {
        _pageComponent = component;
    }

    public AWComponent pageComponent ()
    {
        return _pageComponent;
    }

    private void setRequestContext (AWRequestContext requestContext)
    {
        if (requestContext != _requestContext) {
            _requestContext = requestContext;
            if (_requestContext != null) {
                _httpSession = _requestContext.existingHttpSession();
                isBrowserMicrosoft = _requestContext.isBrowserMicrosoft();
                AWRequest request = _requestContext.request();
                isMacintosh = (request != null) ? request.isMacintosh() : false;
            }
        }
    }

    public AWRequestContext requestContext ()
    {
        return _requestContext;
    }

    protected void setResourceManager (AWResourceManager resourceManager)
    {
        _resourceManager = AWSingleLocaleResourceManager.ensureSingleLocale(resourceManager);
    }

    protected AWSingleLocaleResourceManager resourceManager ()
    {
        if (_resourceManager == null) {
            AWResourceManager resourceManager = null;
            HttpSession existingHttpSession = (_requestContext != null) ? _requestContext.existingHttpSession() : null;
            if (existingHttpSession != null) {
                resourceManager = AWSession.session(existingHttpSession).resourceManager();
            }
            else {
                resourceManager = AWConcreteApplication.SharedInstance.resourceManager(Locale.US);
            }
            setResourceManager(resourceManager);
        }
        return _resourceManager;
    }

    public void _setResourceManager (AWResourceManager resourceManager)
    {
        setResourceManager(resourceManager);
    }

    public AWResourceManager _resourceManager ()
    {
        return resourceManager();
    }

    public AWTemplateParser templateParser ()
    {
        if (_templateParser == null) {
            _templateParser = AWComponent.defaultTemplateParser();
        }
        return _templateParser;
    }

    public void setTemplateParser (AWTemplateParser templateParser)
    {
        _templateParser = templateParser;
    }

    public AWResponse downloadResponse ()
    {
        return _downloadResponse;
    }

    public void setDownloadResponse (AWResponse response)
    {
        _downloadResponse = response;
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        _characterEncoding = characterEncoding;
    }

    public AWCharacterEncoding characterEncoding ()
    {
        if (_characterEncoding == null) {
            AWResourceManager resourceManager = resourceManager();
            _characterEncoding = (resourceManager != null) ? resourceManager.characterEncoding() : AWCharacterEncoding.Default;
        }
        return _characterEncoding;
    }

    public void setClientTimeZone (TimeZone timeZone)
    {
        _clientTimeZone = timeZone;
    }

    public TimeZone clientTimeZone ()
    {
        if (_clientTimeZone == null) {
            HttpSession existingHttpSession = _requestContext.existingHttpSession();
            if (existingHttpSession != null) {
                _clientTimeZone = AWSession.session(existingHttpSession).clientTimeZone();
            }
            if (_clientTimeZone == null) {
                _clientTimeZone = TimeZone.getDefault();
            }
        }
        return _clientTimeZone;
    }

    protected void setPreferredLocale (Locale locale)
    {
        if (_resourceManager != null) {
            setResourceManager(_resourceManager.resourceManagerForLocale(locale));
        }
    }

    public Locale preferredLocale ()
    {
        return resourceManager().locale();
    }

    public AWStringDictionary otherBindingsValuesScratch ()
    {
        if (_otherBindingsValues == null) {
            _otherBindingsValues = new AWStringDictionary();
        }
        else {
            // no need to clear here as long as the genericElement removes all keys, but to be safe.
            _otherBindingsValues.clear();
        }
        return _otherBindingsValues;
    }

    protected HttpSession existingHttpSession ()
    {
        if (_httpSession == null) {
            _httpSession = (_requestContext != null) ? _requestContext.existingHttpSession() : null;
        }
        return _httpSession;
    }

    protected HttpSession httpSession ()
    {
        return httpSession(true);
    }

    protected HttpSession httpSession (boolean required)
    {
        if (_httpSession == null) {
            _httpSession = (_requestContext != null) ? _requestContext.httpSession(required) : null;
        }
        return _httpSession;
    }

    protected AWSession session ()
    {
        return session(true);
    }

    protected AWSession session (boolean required)
    {
        if (_session == null) {
            _session = AWSession.session(httpSession(required));
        }
        return _session;
    }

    protected void setEnv (AWEnvironmentStack environmentStack)
    {
        _environmentStack = environmentStack;
    }

    protected AWEnvironmentStack env ()
    {
        if (_environmentStack == null) {
            AWSession session = requestContext().session(false);
            if (session == null) {
                _environmentStack = new AWEnvironmentStack();
            }
            else {
                _environmentStack = session.environmentStack();
            }
        }
        return _environmentStack;
    }

    public boolean isScrolled ()
    {
        return !((_pageScrollTop == null || _pageScrollTop == AWConstants.Zero) &&
               (_pageScrollLeft == null || _pageScrollTop == AWConstants.Zero));
    }

    public void setPageScrollTop (String offset)
    {
        try {
            if (offset != null) {
                Integer.parseInt(offset);
                _pageScrollTop = "0".equals(offset) ? AWConstants.Zero :
                    AWEncodedString.sharedEncodedString(offset);
            }
        }
        catch (NumberFormatException e) {
            // if offset is not an integer, ignore
        }
    }

    public String pageScrollTop ()
    {
        return _pageScrollTop == null ? null : _pageScrollTop.string();
    }

    public void setPageScrollLeft (String offset)
    {
        try {
            if (offset != null) {
                Integer.parseInt(offset);
                _pageScrollLeft = "0".equals(offset) ? AWConstants.Zero :
                    AWEncodedString.sharedEncodedString(offset);
            }
        }
        catch (NumberFormatException e) {
            // if offset is not an integer, ignore
        }
    }

    public String pageScrollLeft ()
    {
        return _pageScrollLeft == null ? null : _pageScrollLeft.string();
    }

    protected boolean requiresPreGlidCompatibility ()
    {
        return _pageComponent.requiresPreGlidCompatibility();
    }

    public AWHiddenFormValueManager hiddenFormValueManager ()
    {
        if (_hiddenFormValueManager == null) {
            _hiddenFormValueManager = new AWHiddenFormValueManager();
        }
        return _hiddenFormValueManager;
    }

    public AWErrorManager errorManager ()
    {
        if (_errorManager == null) {
            _errorManager = new AWErrorManager.AWNewErrorManager(this);
        }
        return _errorManager;
    }

    // used for modal panels
    public Object pushErrorManager (Object stored)
    {
        // remember last error manager and then clear so we'll lazily create another
        AWErrorManager last = errorManager();
        _errorManager = (stored != null) ? (AWErrorManager)stored : new AWErrorManager.AWNewErrorManager(this);
        // Log.aribaweb_errorManager.debug("*** pushErrorManager -> %s", _errorManager.getLogPrefix());
        _errorManager.setPhase(last.phase());
        return last;
    }

    public Object popErrorManager (Object state)
    {
        // remember pushed error manager since it is the foreground panel
        _errorManager.setPhase(AWErrorManager.OutOfPhase);
        _foregroundErrorManager = _errorManager;
        _errorManager = (AWErrorManager)state;
        // Log.aribaweb_errorManager.debug("*** popErrorManager -> %s", _errorManager.getLogPrefix());
        return _foregroundErrorManager;
    }

    public AWErrorManager foregroundErrorManager ()
    {
        return (_foregroundErrorManager != null) ? _foregroundErrorManager : errorManager();
    }

    public List modalPanels ()
    {
        return _modalPanels;
    }

    public AWComponent topPanel ()
    {
        return (_modalPanels != null) ? (AWComponent)ListUtil.lastElement(_modalPanels) : null;
    }

    public String perfPageName ()
    {
       return getPerfComponent().namePath();
    }

    /**
     * Returns the component that should be used for performance logging.  Typically this
     * is the topPanel() but if that is null, it falls back to the page component.
     *
     * @return
     */
    public AWComponent getPerfComponent ()
    {
        return (topPanel() != null) ? topPanel() : pageComponent();
    }

    public void addModalPanel (AWComponent panel)
    {
        if (_modalPanels == null) _modalPanels = ListUtil.list();
        ListUtil.addElementIfAbsent(_modalPanels, panel);
    }

    public AWComponent popModalPanel ()
    {
        return (_modalPanels != null) ? (AWComponent)ListUtil.removeLastElement(_modalPanels) : null;
    }

    //////////////////
    // Cycleable
    //////////////////
    public void applyValues()
    {
        _foregroundErrorManager = null;
        AWErrorManager.AWNewErrorManager errorManager = (AWErrorManager.AWNewErrorManager)errorManager();
        errorManager.setPhase(AWErrorManager.ApplyValuesPhase);

        // This completely skips the applyValues phase if there are no form values to be taken.
        String formComponentElementId = _requestContext.request().formValueForKey(AWComponentActionRequestHandler.FormComponentIdKey, false);
        if (formComponentElementId != null) {
            _pageComponent._topLevelApplyValues(_requestContext, null);
            if (!_requestContext.dataValuePushedInInvokeAction()) {
                _pageComponent.postTakeValueActions();
            }
        }

        errorManager().setPhase(AWErrorManager.OutOfPhase);
    }

    public AWResponseGenerating invokeAction()
    {
        if (PerformanceState.threadStateEnabled()) {
            PerformanceState.getThisThreadHashtable().setSourcePage(perfPageName());
        }
        recordPageScrollingValues();

        AWFormValueManager formManager = null;
        if (hasFormValueManager()) {
            // we don't call formValueManager directly in order to avoid creating
            // one unnecessarily
            formManager = formValueManager();
            formManager.waitingToPushQueues();
        }

        // Note:
        // We have a loophole here.  We cannot guarantee that values are set
        // before component's invoke is called if the component overrides
        // invokeAction and doesn't call super first.

        _foregroundErrorManager = null;
        AWErrorManager.AWNewErrorManager errorManager = (AWErrorManager.AWNewErrorManager)errorManager();
        errorManager.setPhase(AWErrorManager.InvokePhase);

        AWResponseGenerating actionResults = null;
        String requestSenderId = _requestContext.requestSenderId();
        if (requestSenderId != null) {
            actionResults = _pageComponent._topLevelInvokeAction(_requestContext, _pageComponent);
        }

        if (formManager != null) {
            // if there was no action, the queues weren't processed
            // process them now
            if (!formManager.didPushAllQueues()) {
                formManager.processAllQueues();
            }
        }

        errorManager().setPhase(AWErrorManager.OutOfPhase);

        // take any "done with page" actions
        AWSession session = _requestContext.session(false);
        if (session != null) {
            session.postInvoke();
        }

        return actionResults;
    }

    private void recordPageScrollingValues ()
    {
        AWRequest request = _requestContext.request();

        String scrollTop = request.formValueForKey(AWRequestContext.PageScrollTopKey);
        String scrollLeft = request.formValueForKey(AWRequestContext.PageScrollLeftKey);
        setPageScrollTop(scrollTop);
        setPageScrollLeft(scrollLeft);
    }

    private static final AWEncodedString Debug_RootContents_Separator = AWEncodedString.sharedEncodedString(
            "\n ================================ root contents =========================================\n");
    public static boolean DEBUG_REFRESH_REGION_TOP_LEVEL_CHANGE = false;
    public static String ARIBA_DIFF_TOOL_COMMAND = AWUtil.getenv("ARIBA_DIFF_TOOL_COMMAND");

    protected BrowserState browserState ()
    {
        AWSession session = session(false);
        return (session != null)
                ? session.requestHistory(requestContext().frameName()).browserState()
                : null;
    }

    private void incrementalUpdateAppend (AWRequest request, AWBaseResponse response, AWSession session)
    {
        BrowserState browserState = browserState();
        AWBaseResponse previousResponse = browserState._previousResponse;

        // we only send delta responses for new, backtrack, and forwardtrack requests.  For refreshes (or aborted
        // requests) we force a full page refresh
        if (session.requestType(request) <= 2 &&
            previousResponse != null &&
            ((browserState()._lastPage == this && browserState._incrementalRefreshCount < SamePageForceFPRThreshold)
                  || (AllowCrossPageRefresh && browserState._incrementalRefreshCount < NewPageForceFPRThreshold)) &&
            !_requestContext.fullPageRefreshRequired() &&
            !response.hasRootBufferChanged(previousResponse) &&
            !response.hasNoChangeBufferChanged(previousResponse)) {
            // This is case of delta response -- response will compute the delta
            response.setPreviousResponse(previousResponse);
            if (Log.domsync.isDebugEnabled()) {
                Log.domsync.debug("Return incremental refresh for %s",
                                  _pageComponent.name());
            }
            if (!AWConcreteApplication.IsAutomationTestModeEnabled) {
                browserState._incrementalRefreshCount++;
            }
        }
        else {
            // This is case of full-page redirect
            if (AWPage.DEBUG_REFRESH_REGION_TOP_LEVEL_CHANGE) {
                debug_showReasonForFullPageRedirect(request, response, session, previousResponse);
            }

            if (AWConcreteServerApplication.IsDebuggingEnabled || Log.domsync.isDebugEnabled()) {
                Log.domsync.debug("FULL PAGE REFRESH for %s -- cause: %s",
                        _pageComponent.name(), _computePageRefreshCause(true));
            }

            AWResponse redirectResponse = _requestContext.application().createResponse();
            redirectResponse.appendContent(RedirectStringStart);
            redirectResponse.appendContent(AWCurrWindowDecl.currWindowDecl(_requestContext));
            redirectResponse.appendContent(RedirectStringEnd);
            if (PerformanceState.threadStateEnabled()) {
                PerformanceState.getThisThreadHashtable().setToBeContinued(true);
            }
            _requestContext.setXHRRCompatibleResponse(redirectResponse);
            if (!_requestContext.forceRerenderRequired()) response.setDeferred(true);

            browserState._incrementalRefreshCount = 0;
        }
    }

    // called near the end of the render by AWDebugPane
    public String fullPageRefreshCause ()
    {
        return _computePageRefreshCause(false);
    }

    // Returns null if no FPR (or cause not yet known) or a description string if known
    public String _computePageRefreshCause (boolean isComplete)
    {
        AWRequestContext requestContext = requestContext();

        if (AWComponentActionRequestHandler.wasMainPageRequest(requestContext))
            return "MainPage (OK)";

        if (AWDirectActionRequestHandler.wasDirectActionRedirect(requestContext))
            return "DirectAction (OK)";

        int reqType = session().requestType(requestContext.request());
        if (!_requestContext.isIncrementalUpdateRequest())
            return "Non iFrame request! - link target set?";

        // && reqType != AWSession.RefreshRequest
        if (reqType != AWSession.NewRequest)
            return Fmt.S("session.requestType() != AWSession.NewRequest (== %s)", reqType);

        if (!AllowCrossPageRefresh && browserState()._lastPage != this)
            return "AllowCrossPageRefresh == false, and is new page";

        if (_requestContext.fullPageRefreshRequired())
            return "forceFullPageRefresh() -- global scope script?";

        AWBaseResponse previousResponse = browserState()._previousResponse;

        if (browserState()._previousResponse == null)
            return "previousResponse == null";

        if (previousResponse != null) {
            AWBaseResponse response = (AWBaseResponse)requestContext.response();
            if (response.hasNoChangeBufferChanged(previousResponse))
                return "global scope content changed -- VBScript?";
            if (isComplete && response.hasRootBufferChanged(previousResponse))
                return "content outside page wrapper has changed";
        }

        if (browserState()._incrementalRefreshCount >= NewPageForceFPRThreshold)
            return "Force for Browser leak GC (OK)";

        return null;
    }

    private void debug_showReasonForFullPageRedirect (AWRequest request, AWBaseResponse response,
                                                      AWSession session, AWBaseResponse previousResponse)
    {
        if (previousResponse != null && response.hasRootBufferChanged(previousResponse)) {
            OutputStream outputStream = System.out;
            AWResponseBuffer.WriteContext writeContext
                = new AWResponseBuffer.WriteContext(outputStream, previousResponse.characterEncoding());
            AWUtil.write(outputStream, Debug_RootContents_Separator, _characterEncoding);
            previousResponse.rootBuffer().debug_writeTopLevelOnly(writeContext);
            AWUtil.write(outputStream, Debug_RootContents_Separator, _characterEncoding);
            response.rootBuffer().debug_writeTopLevelOnly(writeContext);

            try {
                String tempDir = System.getProperty("java.io.tmpdir");
                String separator = tempDir.endsWith(File.separator) ? "" : File.separator;
                String fileName1 = tempDir + separator + "aw_fullpagerefresh1.txt";
                String fileName2 = tempDir + separator + "aw_fullpagerefresh2.txt";
                FileOutputStream file1 = new FileOutputStream(fileName1, false);
                FileOutputStream file2 = new FileOutputStream(fileName2, false);
                previousResponse.rootBuffer().debug_writeTopLevelOnly(
                        new AWResponseBuffer.WriteContext(file1, previousResponse.characterEncoding())
                );
                response.rootBuffer().debug_writeTopLevelOnly(
                        new AWResponseBuffer.WriteContext(file2, previousResponse.characterEncoding())
                    );
                if (!StringUtil.nullOrEmptyOrBlankString(ARIBA_DIFF_TOOL_COMMAND)) {
                    String diffToolCommand = Fmt.S(ARIBA_DIFF_TOOL_COMMAND, fileName1, fileName2);
                    Runtime.getRuntime().exec(diffToolCommand);
                }
            }
            catch (FileNotFoundException fileNotFoundException) {
                throw new AWGenericException(fileNotFoundException);
            }
            catch (IOException ioexception) {
                throw new AWGenericException(ioexception);
            }
        }
    }

    public void renderResponse()
    {
        AWRequest request = _requestContext.request();
        AWSession session = _requestContext.session(false);
        if (session != null) {
            session.preAppend();
        }

        BrowserState browserState = browserState();

        // If this is not a refresh request and there is a previously unwritten response,
        // then this must be a a request for a deferred response
        // (aka full page refresh). Reuse the previous response as is.

        // This method gets calls twice for full page refreshes.
        // Once to get generate the FPR redirect,
        // and again to vent the deferred response.
        // Any code not meant for the vending should be below this block.
        if (session != null && (session.requestType(request) == AWSession.RefreshRequest)
                && !_requestContext.isIncrementalUpdateRequest()) {

            AWBaseResponse previousResponse = browserState._previousResponse;
            if (previousResponse != null && previousResponse.isDeferred()) {
                _requestContext.setResponse(previousResponse);
                previousResponse.setDeferred(false);
                browserState._pageScriptList = _currScriptList;
                Log.domsync.debug("Returning deferred response (full page refresh)");
                return;
            }
        }

        if (hasFormValueManager()) {
            // we don't call formValueManager directly in order to avoid creating
            // one unnecessarily
            formValueManager().clear();
        }
        
        // clear out the validation context before we start real append phase
        _validationContext = null;

        // clear the curr script list before append
        _currScriptList = null;

        getPerfComponent().setPerfDestinationInfo();


        _foregroundErrorManager = null;
        AWErrorManager.AWNewErrorManager errorManager = (AWErrorManager.AWNewErrorManager)errorManager();
        errorManager.setPhase(AWErrorManager.RenderPhase);

        if (_LifecycleListeners != null) {
            for (LifecycleListener l : _LifecycleListeners) l.pageWillRender(this);
        }

        _pageComponent._topLevelRenderResponse(_requestContext, _pageComponent);
        if (_requestContext._debugShouldRecord()) {
            AWResponse response = _requestContext.response();
            AWEncodedString _debugResponseId = _requestContext._debugResponseIdAsIs();
            if (_debugResponseId != null) {
                response.setHeaderForKey(_debugResponseId.toString(),
                                          AWRecordingManager.HeaderResponseId);
            }
            if (_pageComponent != null) {
                String pageName = _pageComponent.componentDefinition().componentName();
                response.setHeaderForKey(pageName,
                                         AWRecordingManager.HeaderPageName);
            }
        }

        errorManager().setPhase(AWErrorManager.OutOfPhase);

        if (PerformanceState.threadStateEnabled() &&
                (_requestContext.response().contentType() == AWContentType.TextHtml)) {
            PerformanceState.getThisThreadHashtable().setType(PerformanceState.Type_User);
        }

        if (session != null &&
            _pageComponent.allowDeferredResponse() &&
            !_requestContext.isContentGeneration()) {
            // if this request was initiated incrmentally
            // NOTE: the AWBaseResponse check / isContentGeneration is purely for AN use
            // TODO: REMOVE after verifying with AN
            AWBaseResponse response = (AWBaseResponse)_requestContext.response();
            if (! response.isContentGeneration()) {
                // NOTE: allowIncrementalUpdateAppend for AN metatemplate mode
                if (_requestContext.isIncrementalUpdateRequest() &&
                    _requestContext.allowIncrementalUpdateApppend()) {
                    incrementalUpdateAppend(request, response, session);
                    // Save our state for subsequent requests to this page
                }
                if (browserState != null) {
                    browserState._lastPage = this;
                    browserState._previousResponse = response;
                }
            }
        }
    }

    public void appendPageScrollingScript ()
    {
        if ((_pageScrollTop != null) || (_pageScrollLeft != null)) {
            AWResponse response = _requestContext.response();
            response.appendContent(SetPageScroll);
            response.appendContent(AWConstants.OpenParen);
            response.appendContent(_pageScrollLeft == null ? AWConstants.Zero : _pageScrollLeft);
            response.appendContent(AWConstants.Comma);
            response.appendContent(_pageScrollTop == null ? AWConstants.Zero : _pageScrollTop);
            response.appendContent(AWConstants.CloseParen);
            response.appendContent(AWConstants.Semicolon);
        }
    }

    //////////////////////
    // Backtrack Handling
    //////////////////////
    protected AWBacktrackState updateBacktrackStateForBackButton ()
    {
        _currentBacktrackState = _previousBacktrackState;
        if (_previousBacktrackState != null) {
            _nextBacktrackState = _previousBacktrackState;
            _previousBacktrackState = _previousBacktrackState.previousBacktrackState();
        }
        return _currentBacktrackState;
    }

    protected AWBacktrackState updateBacktrackStateForForwardButton ()
    {
        _currentBacktrackState = _nextBacktrackState;
        if (_nextBacktrackState != null) {
            _previousBacktrackState = _nextBacktrackState;
            _nextBacktrackState = _nextBacktrackState.nextBacktrackState();
        }
        return _currentBacktrackState;
    }

    protected void swapBacktrackStates (Object userBacktrackState)
    {
        _currentBacktrackState.userState = userBacktrackState;
    }

    protected void recordBacktrackState (AWComponent component, Object userBacktrackState)
    {
        AWBacktrackState newBacktrackState = new AWBacktrackState(component, userBacktrackState);
        if (_previousBacktrackState != null) {
            _previousBacktrackState.setNextBacktrackState(newBacktrackState);
            _previousBacktrackState = newBacktrackState;
            _nextBacktrackState = null;
        }
        else {
            _previousBacktrackState = newBacktrackState;
        }
    }

    protected void removeBacktrackState ()
    {
        _nextBacktrackState = _previousBacktrackState;
        if (_nextBacktrackState != null) {
            _nextBacktrackState.setNextBacktrackState(null);
            _previousBacktrackState = _nextBacktrackState.previousBacktrackState();
        }
        else {
            _previousBacktrackState = null;
        }
    }

    public void truncateBacktrackState ()
    {
        _nextBacktrackState = null;
        if (_previousBacktrackState != null) {
            _previousBacktrackState.setNextBacktrackState(null);
        }
    }

    public void truncateBacktrackState (AWBacktrackState backtrackStateMark)
    {
        _previousBacktrackState =  backtrackStateMark;
        truncateBacktrackState();
    }

    public AWBacktrackState markBacktrackState ()
    {
        return _previousBacktrackState;
    }

    public AWPage previousPage ()
    {
        AWSession session = session();
        return session != null ? session.previousPage(this) : null;
    }

    /////////////////
    // Awake/Sleep
    /////////////////
    public interface LifecycleListener
    {
        // called before awake (but after requestContext assigned)
        void pageWillAwake (AWPage page);

        // called before render() passed on the pageComponent
        void pageWillRender (AWPage page);

        // called before sleep (and before requestContext removed)
        void pageWillSleep (AWPage page);
    }

    private static List<LifecycleListener> _LifecycleListeners = null;

    public static void registerLifecycleListener (LifecycleListener listener)
    {
        if (_LifecycleListeners == null) _LifecycleListeners = new ArrayList();
        _LifecycleListeners.add(listener);
    }

    public void awake ()
    {
        // default is to do nothing
    }

    public void ensureAwake (AWRequestContext requestContext)
    {
        _hasBeenHibernated = false;
        boolean shouldAwake = _requestContext != requestContext;
        if (shouldAwake) {
            setRequestContext(requestContext);
        }

        // pageWillAwake() notification
        // TODO: the metaui ContextBinder requires this gets called even on already awake pages
        // so we're going it even when !shouldSleep.  Needs investigation...
        if (_LifecycleListeners != null) {
            for (LifecycleListener l : _LifecycleListeners) l.pageWillAwake(this);
        }

        if (shouldAwake) awake();

        // Check here if our page instance is out of date (due to dynamic class reloading)
        if (AWConcreteApplication.IsRapidTurnaroundEnabled
                    && requestContext != null
                    && requestContext.currentPhase() == AWRequestContext.Phase_Render) {
            AWComponentDefinition componentDefinition = _pageComponent.componentDefinition();
            if (componentDefinition != null) {
                AWComponent component = AWComponentReference.refreshedComponent(componentDefinition,
                        null, _pageComponent, AWElementIdPath.emptyPath());
                setPageComponent(component);}
        }

        _pageComponent.ensureAwake(this);
    }

    public void sleep ()
    {
        // default is to do nothing
    }

    protected void ensureAsleep ()
    {
        if (_requestContext != null) {
            _pageComponent.ensureAsleep();
            if (_subcomponents != null && !_subcomponents.isEmpty()) {
                Iterator subcomponentIterator = _subcomponents.values().iterator();
                while (subcomponentIterator.hasNext()) {
                    AWComponent currentSubcomponent = (AWComponent)subcomponentIterator.next();
                    currentSubcomponent.ensureAsleep();
                }
            }

            sleep();

            if (_LifecycleListeners != null) {
                for (LifecycleListener l : _LifecycleListeners) l.pageWillSleep(this);
            }

            setRequestContext(null);
        }
    }

    protected void hibernate ()
    {
        if (!_hasBeenHibernated) {
            _hasBeenHibernated = true;
            _pageComponent.hibernate();
            if (_subcomponents != null && !_subcomponents.isEmpty()) {
                Iterator subcomponentIterator = _subcomponents.values().iterator();
                while (subcomponentIterator.hasNext()) {
                    AWComponent currentSubcomponent = (AWComponent)subcomponentIterator.next();
                    currentSubcomponent.hibernate();
                }
            }
            _otherBindingsValues = null;
        }
    }

    protected void exit ()
    {
        // when the page is off the top of the page cache, deactivate all listeners to
        // the page
        if (_changeNotifier != null) {
            AWChangeNotifier notifier = _changeNotifier;
            _changeNotifier = null;
            notifier.deactivate();
        }

        resetValidationContext();
        flushComponentConfigurations();

        // clear it and let the page component set it as appropriate
        debug_setPrevPageHasValidationDisplayError(false);

        _pageComponent.exit();
    }

    public static final String PrevPageHasValidationDisplayError =
        "PrevPageHasValidationDisplayError";

    public void debug_setPrevPageHasValidationDisplayError (boolean state)
    {
        try {
            if (AWConcreteApplication.IsDebuggingEnabled) {
                AWSession session = session();
                if (session != null) {
                    session.dict().put(PrevPageHasValidationDisplayError,
                                       Constants.getBoolean(state));
                }
            }
        }
        catch (RuntimeException e) {
        // bug 1-4W8SL3, this method will be called in AWSession.valueUnbind
        // through AWPage.exit(). It will fail because it tries to access an invalid
        // httpSession's attribute in session(). Throw a runtime exception here
        // will interrupt tomcat's expired session cleaning thread.

                Log.aribaweb.debug("Runtime Exception Suppressed: %s", e);
        }
    }

    public boolean debug_prevPageHasValidationDisplayError ()
    {
        if (AWConcreteApplication.IsDebuggingEnabled) {
            AWSession session = session();
            if (session != null) {
                Boolean state = (Boolean)session.dict().get(PrevPageHasValidationDisplayError);
                return (state != null) ? state.booleanValue() : false;
            }
        }
        return false;
    }

    ///////////////
    // Component configuration
    ///////////////
    protected Object componentConfiguration (AWComponent component,
                                             String configurationName)
    {
        Object configuration = null;
        if (!StringUtil.nullOrEmptyOrBlankString(configurationName)) {
            Class componentClass = component.getClass();
            // get it from grid
            if (_componentConfigurations != null) {
                Map configurationsByName = (Map)_componentConfigurations.get(componentClass);
                if (configurationsByName != null) {
                    configuration = configurationsByName.get(configurationName);
                }
            }
            // get it from configuration source if one exists
            if (configuration == null) {
                AWComponentConfigurationSource componentConfigurationSource =
                        component.application().getComponentConfigurationSource(componentClass);
                if (componentConfigurationSource != null) {
                    configuration = componentConfigurationSource.loadConfiguration(configurationName);
                }
            }
            // put it in grid
            setComponentConfiguration(component, configurationName, configuration);
        }
        return configuration;
    }

    protected void setComponentConfiguration (AWComponent component,
                                              String configurationName,
                                              Object configuration)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(configurationName) && configuration != null) {
            Class componentClass = component.getClass();
            if (_componentConfigurations == null) {
                _componentConfigurations = MapUtil.map();
            }
            Map componentConfigurationsByName =
                    (Map)_componentConfigurations.get(componentClass);
            if (componentConfigurationsByName == null) {
                componentConfigurationsByName = MapUtil.map();
            }
            componentConfigurationsByName.put(configurationName, configuration);
            _componentConfigurations.put(componentClass, componentConfigurationsByName);
        }
    }

    protected void flushComponentConfigurations ()
    {
        if (_componentConfigurations != null) {
            AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
            Iterator componentClasses = _componentConfigurations.keySet().iterator();
            while (componentClasses.hasNext()) {
                Class componentClass = (Class)componentClasses.next();
                // lookup configuration source for each class
                AWComponentConfigurationSource componentConfigurationSource =
                        application.getComponentConfigurationSource(componentClass);
                if (componentConfigurationSource != null) {
                    Map componentConfigurationsByName =
                            (Map)_componentConfigurations.get(componentClass);
                    componentConfigurationSource.saveConfigurations(componentConfigurationsByName);
                }
            }
        }
        // dispose component configurations on flush
        Object disposeTarget = _componentConfigurations;
        _componentConfigurations = null;
        AWUtil.dispose(disposeTarget);
    }

    //////////////////
    // Subcomponents
    //////////////////
    public AWComponent getStatefulComponent (AWElementIdPath elementIdPath)
    {
        AWComponent subcomponent = null;
        if (_subcomponents != null) {
            subcomponent = (AWComponent)_subcomponents.get(elementIdPath);
        }
        return subcomponent;
    }

    public void putStatefulComponent (AWElementIdPath elementIdPath, AWComponent subcomponent)
    {
        if (_subcomponents == null) {
            _subcomponents = MapUtil.map(16);
        }
        _subcomponents.put(elementIdPath, subcomponent);

        if (_requestContext.isDebuggingEnabled()) {
            if (_subcomponentTraces == null) {
                _subcomponentTraces = MapUtil.map(16);
            }
            _subcomponentTraces.put(subcomponent, _requestContext.currentElementIdTrace());
        }
    }

    protected void _clearSubcomponentsWithParentPath (AWElementIdPath parentPath,
                                                      boolean clearLaterSiblings)
    {
        Iterator iter = _subcomponents.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry)iter.next();
            AWElementIdPath componentPath = (AWElementIdPath)entry.getKey();
            boolean remove = (clearLaterSiblings) 
                ? componentPath.isParentOrSiblingPredecessor(parentPath)
                : componentPath.hasPrefix(parentPath);
            if (remove) iter.remove();
        }
    }

    protected String _debugSubcomponentString (String componentNamePath)
    {
        FastStringBuffer buf = new FastStringBuffer();
        if (_subcomponents != null) {
            Set entrySet = _subcomponents.entrySet();
            Iterator iter = entrySet.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                AWComponent component = (AWComponent)entry.getValue();
                AWElementIdPath path = (AWElementIdPath)entry.getKey();
                buf.append(AWElementIdPath.debugElementIdPath(path));
                buf.append("=");
                buf.append(component);
                buf.append("\n");
                if (_subcomponentTraces != null) {
                    if (component.namePath().equals(componentNamePath)) {
                        String elementIdTrace = (String)_subcomponentTraces.get(component);
                        if (elementIdTrace != null) {
                            buf.append(elementIdTrace);
                            buf.append("\n");
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    //////////////
    // User State
    //////////////
    public void put (Object key, Object value)
    {
        if (_userState == null) {
            _userState = MapUtil.map();
        }
        if (value == null) {
            _userState.remove(key);
        }
        else {
            _userState.put(key, value);
        }
    }

    public Object get (Object key)
    {
        return _userState == null ? null : _userState.get(key);
    }

    //////////////
    // Form management
    //////////////
    /**
     * Maintains the lists of element id's for each form elements keyed by the id of the
     * form itself.  Used during applyValues to allow element id tree skipping.
     * @param formId element id of the AWForm
     * @param array list of element id's of all input elements in the AWForm
     * @aribaapi private
     */
    public void putFormIds (String formId, AWArrayManager array)
    {
        if (_formIds == null) {
            _formIds = MapUtil.map();
        }
        if (array == null) {
            _formIds.remove(formId);
        }
        else {
            _formIds.put(formId, array);
        }
    }

    /**
     * Given the element id of a form, returns the list of form input element id's for
     * the form.
     * @param formId
     * @aribaapi private
     */
    public AWArrayManager getFormIds (String formId)
    {
        return _formIds == null ? null : (AWArrayManager)_formIds.get(formId);
    }

    public boolean hasMultipleForms ()
    {
        return _formIds != null && _formIds.size() > 1;
    }

    public String getDefaultFormId ()
    {
        if (_formIds != null && _formIds.size() > 0) {
            List keys = MapUtil.keysList(_formIds);
            return (String)keys.get(0);
        }
        return null;
    }

    /**
     * Adds a global form element id to all existing forms.
     * @param elementIdPath
     * @aribaapi private
     */
    protected void addGlobalFormInputIdPath (AWElementIdPath elementIdPath)
    {
        if (_formIds != null) {
            Iterator iterator = _formIds.values().iterator();
            while (iterator.hasNext()) {
                ((AWArrayManager)iterator.next()).addElement(elementIdPath);
            }
        }
    }

    //////////////
    // Client Side Script
    //////////////
    public List _pageScriptList ()
    {
        BrowserState state = browserState();
        return (state != null) ? state._pageScriptList : null;
    }

    public boolean hasScript (String scriptName)
    {
        List pageScriptList = _pageScriptList();
        return (pageScriptList != null && pageScriptList.contains(scriptName));
    }

    public void recordCurrentScript (String scriptName)
    {
        // _currScriptList is the list of scripts we'll have for an FPR
        // _pageScriptList is the list that will be on the page for an IR
        if (_currScriptList == null || !_currScriptList.contains(scriptName)) {
            if (_currScriptList == null) _currScriptList = ListUtil.list();
            _currScriptList.add(scriptName);

            if (browserState() != null) {
                List pageScriptList = _pageScriptList();
                if (pageScriptList == null) pageScriptList = ListUtil.list();
                if (!pageScriptList.contains(scriptName)) {
                    pageScriptList.add(scriptName);
                    browserState()._pageScriptList = pageScriptList;
                }
            }
        }
    }

    //////////////
    // Validation
    //////////////
    public AWValidationContext validationContext ()
    {
        if (_validationContext == null) {
            _validationContext = new AWValidationContext();
        }
        return _validationContext;
    }

    protected void resetValidationContext ()
    {
        _validationContext = null;
    }

    /**
       Notify that AWPoll-relevant changes have taken place (and client should be updated)
       @return true if a changeNotifier is in effect
       @aribaapi private
    */
    public final boolean notifyChange ()
    {
        boolean isActive = _changeNotifier != null;

        if (isActive) {
            _hasChanged = true;
            pageComponent().notifyChange();
        }

        return isActive;
    }

    final boolean hasChanged ()
    {
        boolean hasChanged = _hasChanged;
        // one time read -- always reset to false after reading value.
        _hasChanged = false;
        return hasChanged;
    }

    public void resetHasChanged ()
    {
        _hasChanged = false;
    }

    public AWChangeNotifier getChangeNotifier ()
    {
        if (_changeNotifier == null) {
            _changeNotifier = new AWChangeNotifier(this);
        }
        return _changeNotifier;
    }

    public boolean hasChangeNotifier ()
    {
        return _changeNotifier != null;
    }

    /**
     * Determines if polling should be initiated for this page.
     * @return boolean true if polling is enabled.
     * @aribaapi private
     */
    public boolean isPollingInitiated ()
    {
        return _initiatePolling;
    }

    /**
     * Set the flag to initiate polling for this page.
     * @aribaapi private
     */
    public void setPollingInitiated (boolean pollEnabled)
    {
        _initiatePolling = pollEnabled;
    }

    public static int defaultPollInterval ()
    {
        return ((AWApplication)AWConcreteApplication.sharedInstance()).getPollInterval();
    }

    /**
       Returns the poll interval for this page.  If the value has not been explicitly
       set, then this method will return the default poll interval value defined by
       AWApplication.
       @return int poll interval in seconds
       @aribaapi private
    */
    public int getPollInterval ()
    {
        return _pollInterval;
    }

    /**
       Overrides the default poll interval as defined by AWApplication.
       @param pollInterval interval in seconds
       @aribaapi private
    */
    public void setPollInterval (int pollInterval)
    {
        _pollInterval = pollInterval;
    }

    public boolean pollOnError ()
    {
        return getPollInterval() <= AWPollInterval.FrequentPollInterval;
    }

    /////////
    // Debug
    /////////
    public String toString ()
    {
        return StringUtil.strcat(super.toString(), ":", pageComponent().toString());
    }

    public AWBaseResponse getPreviousResponse ()
    {
        return browserState()._previousResponse;
    }

    /////////////////////////
    // User Input Handling
    /////////////////////////

    // The _formValueManager is a queue of deferred operations that are to be performed
    // after the applyValues phase and before the invokeAction phase.  In some cases,
    // you may want to defer the actual value setting (this avoids the problems associated with
    // modifying the data model during take values).  Then you may also want to run "triggers"
    // and finally perform validations.

    private AWFormValueManager _formValueManager;

    public AWFormValueManager formValueManager ()
    {
        if (_formValueManager == null) {
            _formValueManager = new AWFormValueManager(this);
        }
        return _formValueManager;
    }

    public boolean hasFormValueManager ()
    {
        return _formValueManager != null;
    }

    //////////////////////////
    // Community Context
    //////////////////////////

    /**
     * Fetch the community context
     */
    public AWCommunityContext getCommunityContext()
    {
        if (_communityContext == null) {
            _communityContext = new AWCommunityContext(); 
            _communityContext.setPage(perfPageName());
            _communityContext.setApplication(AWConcreteApplication.sharedInstance().name());
        }
        return _communityContext;
    }

}
