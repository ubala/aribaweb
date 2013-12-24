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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWSession.java#96 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.html.AWLabel;
import ariba.ui.aribaweb.util.*;
import ariba.util.core.Hashtable;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.PerformanceState;
import ariba.util.core.Fmt;
import ariba.util.core.HTTP;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.ArrayList;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

class AXSizeLimitedHashtable extends Hashtable implements AWDisposable
{
    private final int _sizeLimit;
    private final List _orderedList;

    public void dispose ()
    {
        _orderedList.clear();
    }

    public AXSizeLimitedHashtable (int sizeLimit)
    {
        _sizeLimit = sizeLimit;
        _orderedList = ListUtil.list(_sizeLimit);
    }

    public Object put (Object key, Object value)
    {
        _orderedList.add(key);
        if (_orderedList.size() > _sizeLimit) {
            Object removedKey = ListUtil.removeFirstElement(_orderedList);
            super.remove(removedKey);
        }
        return super.put(key, value);
    }

    public Object get (Object key)
    {
        Object objectValue = super.get(key);
        if (objectValue != null) {
            // doing this makes this into an LRU cache
            put(key, objectValue);
        }
        return objectValue;
    }

    public Object elementAtOffsetFromLastElement (int offset)
    {
        Object elementAtOffsetFromLastElement = null;
        if (offset >= 0) {
            int index = _orderedList.size() - 1 - offset;
            if (index >= 0) {
                String key = (String)_orderedList.get(index);
                elementAtOffsetFromLastElement = super.get(key);
            }
        }
        return elementAtOffsetFromLastElement;
    }
}

final class AWRequestHistory extends AWBaseObject implements AWDisposable
{
    private List _requestIds = ListUtil.list();
    private List _pages = ListUtil.list();
    private List _appBoundaries = null;
    private List _pageCacheMarks = null;
    private int _currentRequestIdIndex = -1;
    private int _currentPageIndex = -1;
    private int _requestType = AWSession.NewRequest;
    private final int _maxPageCount;
    private AWPage.BrowserState _browserState;

    public void dispose ()
    {
        Object disposeTarget = _pages;
        _pages = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _pageCacheMarks;
        _pageCacheMarks = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _requestIds;
        _requestIds = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _appBoundaries;
        _appBoundaries = null;
        AWUtil.dispose(disposeTarget);
    }

    protected AWRequestHistory (int pageCacheSize)
    {
        super();
        _maxPageCount = pageCacheSize;
    }

    private void setCurrentPageIndex (int index)
    {
        _currentPageIndex = index;
    }

    private void setCurrentRequestIdIndex (int index)
    {
        _currentRequestIdIndex = index;
        // shift all app boundaries
        updateRequestIdCacheMarks(_appBoundaries, _currentRequestIdIndex);
    }

    private void updateRequestIdCacheMarks (List cacheMarks, int indexToRemove)
    {
        if (cacheMarks != null) {
            for (int index = cacheMarks.size() - 1; index >= 0; index--) {
                AWConcretePageCacheMark currentPageCacheMark =
                    (AWConcretePageCacheMark)cacheMarks.get(index);
                int currentPageCacheMarkIndex = currentPageCacheMark.index();
                if (currentPageCacheMarkIndex >= indexToRemove) {
                    cacheMarks.remove(index);
                }
            }
        }
    }

    protected void initRequestType (AWRequestContext requestContext)
    {
        AWRequest request = requestContext.request();
        String requestId =  request.requestId();
        int requestType = AWSession.NewRequest;

        if (requestContext.isHistoryRequest()) {
            if (Log.aribaweb.isDebugEnabled()) {
                Log.aribaweb.debug("******* historyRequest %s",
                                   requestContext.historyAction());
            }

            requestType = requestContext.historyAction();

            if (requestType == AWSession.BacktrackRequest) {
                backTrack();
            }
            else if (requestType == AWSession.ForwardTrackRequest) {
                forwardTrack();
            }

            if (_currentRequestIdIndex == -1) {
                if (Log.aribaweb.isDebugEnabled()) {
                    AWEncodedString frameName = requestContext.frameName();
                    if (frameName == null) {
                        frameName = AWRequestContext.TopFrameName;
                    }
                    Log.aribaweb_ihr.debug(
                        "Invalid history request detected. RequestId: %s  " +
                        "HistoryRequestType: %s  Switching to request type: %s",
                        requestId,
                        AWSession.requestTypeString(requestType),
                        AWSession.requestTypeString(AWSession.NewRequest));
                }

                requestType = AWSession.NewRequest;
            }
            else {
                // set up the request id after setting up the request id cache ...
                requestId = getCurrentRequestId();

                AWBaseRequest baseRequest = (AWBaseRequest)requestContext.request();
                baseRequest.setRequestId(requestId);
            }
        }
        else {
            int indexOfRequestId = _requestIds.lastIndexOf(requestId);
            if (indexOfRequestId == -1) {
                AWEncodedString responseId = request.responseId();
                if (responseId != null && _currentRequestIdIndex >= 0) {
                    String requestsResponseId = request.responseId().string();
                    String currentRequestId = (String)_requestIds.get(_currentRequestIdIndex);
                    if (currentRequestId.regionMatches(true, 0, requestsResponseId, 0, requestsResponseId.length())) {
                        // An interrupted request just causes a refresh on the client (i.e.
                        // the request is ignored, and the client is brought in sync with the server
                        requestType = AWSession.InterruptedNewRequest;
                    }
                }
            }
            else if (requestContext.isInPageRequest()) {
                // this is a stray request from an action that does not 
                // require page navigation. ignore it.
                requestType = AWSession.NoOpRequest;
            }
            else {
                if (indexOfRequestId < _currentRequestIdIndex) {
                    requestType = AWSession.BacktrackRequest;
                    backTrack();
                }
                else if (indexOfRequestId > _currentRequestIdIndex) {
                    requestType = AWSession.ForwardTrackRequest;
                    forwardTrack();
                }
                else if (indexOfRequestId == _currentRequestIdIndex) {
                    requestType = AWSession.RefreshRequest;
                }
            }
        }
        _requestType = requestType;
        if (Log.aribaweb.isDebugEnabled()) {
            Log.aribaweb.debug("******* requestType: %s %s",
                               requestId, AWSession.requestTypeString(_requestType));
        }
    }

    protected int requestType ()
    {
        return _requestType;
    }

    protected void clear ()
    {
        AWPage currentPage = restoreCurrentPage();
        if (currentPage != null) {
            cleanUpPageState(currentPage);
        }
        _pages.clear();
        setCurrentPageIndex(-1);
        _requestIds.clear();
        _currentRequestIdIndex = -1;
        _requestType = AWSession.NewRequest;
        _pageCacheMarks = null;
        _appBoundaries = null;

        // Clearing the page cache to prevent backtrack (bad!) doesn't mean that
        // this state still doesn't represent the current contents of the browser...
        if (!AWPage.AllowCrossPageRefresh) _browserState = null;
    }

    private void popTopsOfStacks ()
    {
        AWUtil.removeFromIndex(_requestIds, _currentRequestIdIndex + 1);
        // todo: deactivate notifiers associated with pages being removed from page cache

        AWUtil.removeFromIndex(_pages, _currentPageIndex + 1);
        updatePageCacheMarks(_pageCacheMarks, _currentPageIndex + 1);
    }

    protected void savePage (String requestId, AWPage page, boolean isException)
    {
        if (_requestType == AWSession.NewRequest || _requestType == AWSession.InterruptedNewRequest || isException) {
            popTopsOfStacks();
            setCurrentRequestIdIndex(_requestIds.size());
            if (_currentRequestIdIndex > 128) {
                _requestIds.remove(0);
                setCurrentRequestIdIndex(_currentRequestIdIndex - 1);
            }
            _requestIds.add(requestId);
            AWPage lastPage = (AWPage)ListUtil.lastElement(_pages);
            if (page != lastPage) {
                // only add a page to the _pages stack if its different from previous page.
                setCurrentPageIndex(_pages.size());
                if (_currentPageIndex > _maxPageCount) {
                    removeFirstPage();
                }
                _pages.add(page);
                if (lastPage != null) {
                    cleanUpPageState(lastPage);
                }
            }
        }
    }

    private void cleanUpPageState (AWPage lastPage)
    {
        lastPage.exit();
    }

    public AWPage.BrowserState browserState ()
    {
        if (_browserState == null) {
            _browserState = new AWPage.BrowserState();
        }
        return _browserState;
    }

    private List pageCacheMarks ()
    {
        if (_pageCacheMarks == null) {
            _pageCacheMarks = ListUtil.list();
        }
        return _pageCacheMarks;
    }

    private List appBoundaries ()
    {
        if (_appBoundaries == null) {
            _appBoundaries = ListUtil.list();
        }
        return _appBoundaries;
    }

    private void removeFirstPage ()
    {
        // todo: deactivate AWChangeNotifiers associated with top page
        _pages.remove(0);
        setCurrentPageIndex(_currentPageIndex - 1);
        // shift all page cache marks
        updatePageCacheMarks(_pageCacheMarks, 0);
    }

    private void updatePageCacheMarks (List cacheMarks, int indexToRemove)
    {
        if (cacheMarks != null) {
            for (int index = cacheMarks.size() - 1; index >= 0; index--) {
                AWConcretePageCacheMark currentPageCacheMark = (AWConcretePageCacheMark)cacheMarks.get(index);
                int currentPageCacheMarkIndex = currentPageCacheMark.index();
                if (currentPageCacheMarkIndex > indexToRemove) {
                    currentPageCacheMark.decrementIndex();
                }
                else if (currentPageCacheMarkIndex == indexToRemove) {
                    cacheMarks.remove(index);
                }
            }
        }
    }

    protected void removePage (AWPage targetPage)
    {
        int indexOfTargetPage = AWUtil.lastIndexOfIdentical(_pages, targetPage);
        if (indexOfTargetPage != -1) {
            // todo: deactivate AWChangeNotifier

            _pages.remove(indexOfTargetPage);
            if (_currentPageIndex >= indexOfTargetPage) {
                setCurrentPageIndex(_currentPageIndex - 1);
            }
            // shift all page cache marks
            updatePageCacheMarks(_pageCacheMarks, indexOfTargetPage);
        }
    }

    protected AWPage restoreCurrentPage ()
    {
        AWPage currentPage = null;
        if (!_pages.isEmpty()) {
            currentPage = (AWPage)_pages.get(_currentPageIndex);
        }
        return currentPage;
    }

    protected AWPage restoreNextPage (AWPage page)
    {
        AWPage nextPage = null;
        int indexOfPage = AWUtil.lastIndexOfIdentical(_pages, page);
        if (indexOfPage != -1) {
            if (indexOfPage < (_pages.size() - 1)) {
                indexOfPage++;
            }
            setCurrentPageIndex(indexOfPage);
            nextPage = (AWPage)_pages.get(_currentPageIndex);

            cleanUpPageState(page);
        }
        return nextPage;
    }

    protected AWPage restorePreviousPage (AWPage page)
    {
        AWPage previousPage = null;
        int indexOfPage = AWUtil.lastIndexOfIdentical(_pages, page);
        if (indexOfPage != -1) {
            AWConcretePageCacheMark pageCacheMark = (_pageCacheMarks != null) ?
                (AWConcretePageCacheMark)ListUtil.lastElement(_pageCacheMarks) :
                null;
            if (pageCacheMark != null && pageCacheMark.preventsBacktracking()) {
                int markIndex = pageCacheMark.index();
                if (indexOfPage > (markIndex + 1)) {
                    indexOfPage--;
                }
            }
            else if (indexOfPage > 0) {
                indexOfPage--;
            }
            setCurrentPageIndex(indexOfPage);
            previousPage = (AWPage)_pages.get(_currentPageIndex);

            cleanUpPageState(page);
        }
        return previousPage;
    }

    public AWPage getPreviousPage (AWPage page)
    {
        AWPage previousPage = null;
        int indexOfPage = AWUtil.lastIndexOfIdentical(_pages, page, _currentPageIndex);
        if (indexOfPage > 0) {
            indexOfPage--;
            previousPage = (AWPage)_pages.get(indexOfPage);
        }
        return previousPage;
    }

    public AWPage pageAtOffsetFromLastElement (int offset)
    {
        AWPage pageAtOffsetFromLastElement = null;
        if (offset >= 0) {
            int index = _pages.size() - 1 - offset;
            if (index >= 0) {
                pageAtOffsetFromLastElement = (AWPage)_pages.get(index);
            }
        }
        return pageAtOffsetFromLastElement;
    }

    public AWConcretePageCacheMark markPageCache ()
    {
        AWConcretePageCacheMark pageCacheMark = new AWConcretePageCacheMark(_currentPageIndex);
        pageCacheMarks().add(pageCacheMark);
        return pageCacheMark;
    }

    public AWConcretePageCacheMark markAppBoundary ()
    {
        AWConcretePageCacheMark appBoundaryMark = new AWConcretePageCacheMark(_currentRequestIdIndex);
        appBoundaries().add(appBoundaryMark);
        return appBoundaryMark;
    }

    public void truncatePageCache (AWConcretePageCacheMark pageCacheMark, boolean inclusive)
    {
        // a pageCacheMark may only be used one time.
        List pageCacheMarks = pageCacheMarks();
        int indexOfMark = pageCacheMarks.indexOf(pageCacheMark);
        if (indexOfMark != -1) {
            pageCacheMarks.remove(indexOfMark);
            int indexOfPage = pageCacheMark.index();
            if (indexOfPage < 0) {
                clear();
            }
            else {
                if (inclusive) {
                    indexOfPage--;
                }
                for (int index = _pages.size() - 1; index > indexOfPage; index--) {
                    _pages.remove(index);
                }
                setCurrentPageIndex(_pages.size() - 1);
            }
        }
    }

    protected int getHistoryPosition ()
    {
        int offset = 0;
        if (!ListUtil.nullOrEmptyList(_appBoundaries)) {
            AWConcretePageCacheMark appBoundaryMark =
                (AWConcretePageCacheMark)ListUtil.lastElement(_appBoundaries);
            offset = appBoundaryMark.index();
        }

        return _currentRequestIdIndex - offset;
    }

    protected int getHistoryLength ()
    {
        int historyLength = 0;
        if (_requestIds != null) {
            int offset = 0;
            if (!ListUtil.nullOrEmptyList(_appBoundaries)) {
                AWConcretePageCacheMark appBoundaryMark =
                    (AWConcretePageCacheMark)ListUtil.lastElement(_appBoundaries);
                offset = appBoundaryMark.index();
            }

            historyLength = _requestIds.size() - offset;
        }
        return historyLength;
    }

    protected String getCurrentRequestId ()
    {
        return (String)_requestIds.get(_currentRequestIdIndex);
    }

    protected void backTrack ()
    {
        if (_currentRequestIdIndex > 0) {
            setCurrentRequestIdIndex(_currentRequestIdIndex - 1);
        }
        else {
            // log error
            Log.aribaweb.debug("Attempt to backtrack off of request history");
        }
    }

    protected void forwardTrack ()
    {
        if (_currentRequestIdIndex < _requestIds.size() - 2) {
            setCurrentRequestIdIndex(_currentRequestIdIndex + 1);
        }
        else {
            // log error
            Log.aribaweb.debug("Attempt to forwardtrack off of request history");
        }
    }

    protected List<AWPage> _getPages()
    {
        return _pages;
    }
}

/**
 * @aribaapi private
 */
public class AWSession extends AWBaseObject
    implements HttpSessionBindingListener, AWDisposable
{
    public static final String ResourceManagerKey = "ResourceManagerKey";
    private static final String SessionKey = "awsession";
    public static final int NewRequest = 0;
    public static final int BacktrackRequest = 1;
    public static final int ForwardTrackRequest = 2;
    public static final int RefreshRequest = 3;
    // This occurrs when the user clicks stop and then clicks something else.
    public static final int InterruptedNewRequest = 4;
    public static final int NoOpRequest = 5;
    private AWApplication _application;
    private HttpSession _httpSession;
    private String _sessionId;
    private String _sessionSecureId;
    private AWRequestContext _requestContext;
    private AWCharacterEncoding _characterEncoding;
    private TimeZone _clientTimeZone;
    private boolean _isEnhancedAccessibility;
    private AWEnvironmentStack _environmentStack;
    private int _responseIdInt = 0;
    private String _remoteHostAddress;
    private InetAddress _remoteIPAddress;
    private AWCookie _trackingCookie;
    private boolean _isTerminated = false;
    private boolean _isInvalidated = false;
    private boolean _isDisposed = false;

    private String _brandName = null;
    private String _brandVersion = null;
    private boolean _brandTestMode = false;

    private Map _requestHistories;
    private AWPage _redirectPage;

    private int _timeoutSeconds = -1;

    private Object _notificationsLock;
    private List _notifications;

    private long _lastAccessedTime = -1;
    private int _requestInterval = -1;
    private boolean _isConnected = true;
    private String _sessionIdentifier;
    private AWShutdownState _shutdownState;

        // Perf items for performance tracking on the bottom of each page.
    private PerformanceState.Stats _performanceStateHashtable = null;

    // Use componentName.key to ensure uniqueness of keys placed in this table
    private Map _dict;

    private boolean _markForTermination = false;
    // specifies whether to update the performance stats in ensureAsleep
    private boolean _updateLatestPerformanceStats;

    // session scoped URL's
    public String _refreshURL;
    public String _backTrackURL;
    public String _forwardTrackURL;

    private String _testShortId;
    private String _testId;
    private String _testLine;

    /**
        Overrides global AWPage.AllowParentFrame
    */
    private boolean _allowParentFrame = false;
    
    private boolean _omitWrapperFrame;

    // ** Thread Safety Considerations: sessions are never shared by multiple threads -- no locking required.

    public void dispose ()
    {
        if (_isDisposed) {
            return;
        }
        _isDisposed = true;
        _application = null;
        _httpSession = null;
        _characterEncoding = null;
        _clientTimeZone = null;
        _isEnhancedAccessibility = false;
        _remoteHostAddress = null;
        _trackingCookie = null;
        _testId = null;
        _testShortId = null;
        _testLine = null;

        Object disposeTarget = _requestContext;
        _requestContext = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _environmentStack;
        _environmentStack = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _requestHistories;
        _requestHistories = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _redirectPage;
        _redirectPage = null;
        AWUtil.dispose(disposeTarget);

        disposeTarget = _dict;
        _dict = null;
        AWUtil.dispose(disposeTarget);

        disposeNotifications();

        _brandName = null;
        _brandVersion = null;
        _sessionIdentifier = null;
    }

    public static void setSession (HttpSession httpSession, AWSession awsession)
    {
        if (httpSession != null && awsession != null) {
            httpSession.setAttribute(SessionKey, awsession);
        }
    }

    public static AWSession session (HttpSession httpSession)
    {
        try {
            return (httpSession != null) ? (AWSession)httpSession.getAttribute(SessionKey): null;
        }
        catch (IllegalStateException e) {
            throw new AWGenericException(e);
        }
    }

    /**
        For AW processing by long running threads that do not execute the
        usual UI request / response code path. Performs any initialization
        required on the AW thread state.
    */
    public static void initializeThreadState ()
    {
        // currently a no op
    }

    /**
        For AW processing by long running threads that do not execute the
        usual UI request / response code path. For instance, the use of
        AW by worker threads to generate email.

        Performs any cleanup of the AW thread state. Called after AW
        processing has completed.
    */
    public static void cleanupThreadState ()
    {
        AWRequestContext.cleanupThreadLocalState();
    }

    /**
         Registers the session with monitor stats object.
         The session registration is used with gracefull shutdown.
     */
    public boolean registerActiveSession ()
    {
        _application.monitorStats().incrementActiveSessionCount(this);
        return true;
    }

    /**
         UnRegisters the session with monitor stats object.
     */
    public boolean unregisterActiveSession ()
    {
        _application.monitorStats().decrementActiveSessionCount(this);
        return true;
    }

    public HttpSession httpSession ()
    {
        return _httpSession;
    }

    protected void setHttpSession (HttpSession httpSession)
    {
        _httpSession = httpSession;
    }

    /**
     *
     */
    public String sessionId ()
    {
        return _sessionId;
    }

    /**
        The AWSession id should have been assigned during initialization.
        For cases where a new httpSession Id is created for the same httpSession, we want to be able to sync that new id
        for AWSession.

        Currently this httpSessionId reset is only supported in WO Framework.
    */
    public void setSessionId (String newSessionId)
    {
        _sessionId = newSessionId;
    }

    public String sessionSecureId ()
    {
        return _sessionSecureId;
    }

    protected static String requestTypeString (int requestType)
    {
        String requestTypeString = null;
        switch (requestType) {
            case AWSession.BacktrackRequest: {
                requestTypeString = "BacktrackRequest";
                break;
            }
            case AWSession.ForwardTrackRequest: {
                requestTypeString = "ForwardTrackRequest";
                break;
            }
            case AWSession.RefreshRequest: {
                requestTypeString = "RefreshRequest";
                break;
            }
            case AWSession.NewRequest: {
                requestTypeString = "NewRequest";
                break;
            }
            case AWSession.InterruptedNewRequest: {
                requestTypeString = "InterruptedNewRequest";
                break;
            }
            default: {
                requestTypeString = "Unrecognized requestType: " + requestType;
                break;
            }
        }
        return requestTypeString;
    }

    public void init (AWApplication application, AWRequestContext requestContext)
    {
        _httpSession = requestContext.httpSession();
        _sessionId = _httpSession.getId();
        _characterEncoding = AWCharacterEncoding.Default;
        _application = application;
        _requestContext = requestContext;
        _requestHistories = MapUtil.map();
        // copy the environment from page in case session is created in the middle
        // of request handling
        AWPage page = requestContext.page();
        if (page == null) {
            _environmentStack = new AWEnvironmentStack();
        }
        else {
            _environmentStack = page.env();
        }

        // on initial creation of the AWSession, pull the brandName / version
        // from the request and determine the current version
        AWBrand brand = _application.getBrand(requestContext);
        if (brand != null) {
            _brandName = brand.getName();
            _brandVersion = brand.getRequestVersion(requestContext);
        }

        _notificationsLock = new Object();
        _notifications = ListUtil.list();
        _sessionSecureId = initSessionSecureId();
        this.init();
        initEnvironmentStack();
        awake();

        Log.aribaweb_session.debug(
            "Registering awsession: %s for httpsession: %s", this, _sessionId);
        application.registerSession(this);
    }

    public Map dict ()
    {
        if (_dict == null) {
            _dict = MapUtil.map();
        }
        return _dict;
    }

    protected String initSessionSecureId ()
    {
        return null;
    }

    protected void initEnvironmentStack ()
    {
        _environmentStack.push("editable", Boolean.TRUE);
        _environmentStack.push(AWLabel.awinputId, NullObject);
    }

    public AWApplication application ()
    {
        return _application;
    }

    public AWRequest request ()
    {
        return _requestContext.request();
    }

    public AWRequestContext requestContext ()
    {
        return _requestContext;
    }

    protected AWResourceManager initResourceManager (Locale locale)
    {
        AWResourceManager rm =
            AWConcreteApplication.SharedInstance.resourceManager(locale);
        if (_brandName != null) {
            rm = rm.resolveBrand(_brandName, _brandVersion);
        }
        return rm;
    }

    protected void setResourceManager (AWResourceManager resourceManager)
    {
        if (resourceManager == null) {
            httpSession().removeAttribute(AWSession.ResourceManagerKey);
        }
        else {
            httpSession().setAttribute(AWSession.ResourceManagerKey,
                AWSingleLocaleResourceManager.ensureSingleLocale(resourceManager));
        }
    }

    public AWResourceManager resourceManager ()
    {
        AWResourceManager resourceManager = (AWResourceManager)httpSession().getAttribute(AWSession.ResourceManagerKey);
        if (resourceManager == null) {
            resourceManager = initResourceManager(request().preferredLocale());
            setResourceManager(resourceManager);
        }
        return resourceManager;
    }

    public void setPreferredLocale (Locale locale)
    {
        setResourceManager(initResourceManager(locale));
        _characterEncoding = null;
    }

    // Used for debugging purposes to force a new (unrestricted) locale on the session
    public void _forceLocale (Locale locale)
    {
        setResourceManager(application().resourceManager()._resourceManagerForLocale(locale));
        _characterEncoding = null;
        _updatePageResourceManager(resourceManager());
    }

    public Locale preferredLocale ()
    {
        AWResourceManager resourceManager = resourceManager();
        return (resourceManager instanceof AWSingleLocaleResourceManager) ?
            resourceManager.locale() :
            Locale.US;
    }

    protected AWCharacterEncoding initCharacterEncoding ()
    {
        return resourceManager().characterEncoding();
    }

    public void setCharacterEncoding (AWCharacterEncoding characterEncoding)
    {
        _characterEncoding = characterEncoding;
    }

    public AWCharacterEncoding characterEncoding ()
    {
        if (_characterEncoding == null) {
            _characterEncoding = initCharacterEncoding();
        }
        return _characterEncoding;
    }

    public void setClientTimeZone (TimeZone timeZone)
    {
        _clientTimeZone = timeZone;
    }

    public TimeZone clientTimeZone ()
    {
        return _clientTimeZone;
    }

    public void setAccessibilityEnabled (boolean flag)
    {
        _isEnhancedAccessibility = flag;
    }

    public boolean isAccessibilityEnabled ()
    {
        return _isEnhancedAccessibility;
    }

    public AWEnvironmentStack environmentStack ()
    {
        return _environmentStack;
    }

    protected int nextResponseId ()
    {
        // This should NOT be called if the request is a refresh region request
        ++_responseIdInt;
        return _responseIdInt;
    }

    public void setRemoteHostAddress (String remoteHostAddress)
    {
        _remoteHostAddress = remoteHostAddress;
        if (_remoteHostAddress == null) {
            _remoteIPAddress = null;
        }
        else {
            try {
                _remoteIPAddress = InetAddress.getByName(_remoteHostAddress);
            }
            catch (UnknownHostException e) {
                // this should never happen because the string should be %d.%d.%d.%d;
                _remoteIPAddress = null;
            }
        }
    }

    public String remoteHostAddress ()
    {
        return _remoteHostAddress;
    }

    public InetAddress remoteIPAddress ()
    {
        return _remoteIPAddress;
    }

    public void setTimeout (int timeoutSeconds)
    {
        httpSession().setMaxInactiveInterval(timeoutSeconds);
        Log.aribaweb.debug("AWSession.setTimeout():called with timeout val %s",
                           timeoutSeconds);
        _timeoutSeconds = timeoutSeconds;
    }

    /**
     * session timeout in seconds
     * Note: this method can be called from an asynchronous non-UI thread
     */
    public final int getTimeout ()
    {
        int timeout = _timeoutSeconds;
        if (timeout == -1) {
            AWConcreteApplication application =
                (AWConcreteApplication)AWConcreteApplication.sharedInstance();
            timeout = application.sessionTimeout();
        }
        return timeout;
    }

    public long getLastAccessedTime ()
    {
        return _lastAccessedTime;
    }

    /**
     * Sets the expected amount of time between requests for this session in seconds.
     *
     * @param requestInterval the amount of time (seconds) between expected requests.
     *                        Set to -1 if the amount of time between requests is unknown.
     */
    public void setRequestInterval (int requestInterval)
    {
        _requestInterval = requestInterval;
    }

    /**
     * Note: this method can be called from an asynchronous non-UI thread
     * @return
     * @aribaapi private
     */
    private int requestInterval ()
    {
        if (_requestInterval == -1) {
            return getTimeout();
        }
        return _requestInterval;
    }

    protected int disconnectPad ()
    {
        return 60;
    }

    /**
     * Returns the time (in millis) at which point this session is considered to be
     * disconnected.  If -1 is returned, then there is no defined disconnectTime and
     * the appserver session timeout should be used as the disconnect time.
     *
     * DisconnectPad is used to specify how long past the actual expected request interval
     * is allowed before the session enters the pending disconnect state.
     *
     * @return the time (in millis) at which point this session is considered to be
     * disconnected
     * @aribaapi private
     */
    public long disconnectTime ()
    {
        long disconnectTime = -1;
        // see ensureAsleep for debugging
        int requestInterval = requestInterval();
        if (requestInterval != -1) {
            // give ourselves a buffer past the requestInterval
            long disconnectIntervalMillis = (requestInterval + disconnectPad()) * 1000;
            disconnectTime = _lastAccessedTime + disconnectIntervalMillis;
        }
        return disconnectTime;
    }

    /**
     * Note: this method can be called from an asynchronous non-UI thread
     * @aribaapi private
     */
    protected String debugIdentifier ()
    {
        return _sessionId;
    }

    /**
     * Note: this method can be called from an asynchronous non-UI thread
     * @aribaapi private
     */
    public String debugDisconnectString ()
    {
        long requestIntervalMillis = requestInterval() * 1000;
        return Fmt.S("Session: '%s'  RequestInterval: '%s'  Disconnect time: '%s'",
            debugIdentifier(),
            String.valueOf(requestIntervalMillis),
            new java.util.Date(_lastAccessedTime+requestIntervalMillis).toString());
    }

    public boolean isConnected ()
    {
        return _isConnected;
    }

    public void setConnected (boolean flag)
    {
        _isConnected = flag;
    }

    public void setSessionIdentifier (String sessionIdentifier)
    {
        _sessionIdentifier = sessionIdentifier;
    }

    public String sessionIdentifier ()
    {
        return _sessionIdentifier;
    }

    /////////////////
    // Page Caching
    /////////////////
    protected AWRequestHistory requestHistory (AWEncodedString frameName)
    {
        if (frameName == null) {
            frameName = AWRequestContext.TopFrameName;
        }
        AWRequestHistory requestHistory = (AWRequestHistory)_requestHistories.get(frameName);
        if (requestHistory == null) {
            int pageCacheSize = _application.pageCacheSize();
            requestHistory = new AWRequestHistory(pageCacheSize);
            _requestHistories.put(frameName, requestHistory);
        }
        return requestHistory;
    }

    protected void initRequestType ()
    {
        AWEncodedString frameName = _requestContext.frameName();
        requestHistory(frameName).initRequestType(_requestContext);
    }

    protected void incrementResponseId ()
    {
        String responseId = String.valueOf(nextResponseId());
        AWBaseRequest baseRequest = (AWBaseRequest)_requestContext.request();
        baseRequest.setRequestId(responseId);
        baseRequest.setResponseId(responseId);

        initRequestType();
    }

    public int requestType (AWRequest request)
    {
        AWEncodedString frameName = requestContext().frameName();
        return requestHistory(frameName).requestType();
    }

    public void savePage (AWPage page, boolean isExceptionPage)
    {
        if (page.pageComponent().shouldCachePage()) {
            AWRequestContext requestContext = requestContext();
            AWRequest request = requestContext.request();
            String requestId = request.requestId();
            if (AWBaseRequest.InitialRequestId.equals(requestId)) {
                _requestHistories = MapUtil.map();
            }
            AWEncodedString frameName = requestContext.frameName();
            if (frameName == null) {
                frameName = AWRequestContext.TopFrameName;
            }
            AWRequestHistory requestHistory = requestHistory(frameName);
            requestHistory.savePage(requestId, page, isExceptionPage);
            AWPage stalePage = requestHistory.pageAtOffsetFromLastElement(_application.hibernationDepth());
            if ((stalePage != null) && (stalePage != page)) {
                stalePage.hibernate();
            }
        }
    }

    public void savePage (AWPage page)
    {
        if (!isTerminated()) {
            savePage(page, false);
        }
    }

    public AWPage restoreCurrentPage ()
    {
        AWRequest request = requestContext().request();
        AWEncodedString frameName = request.frameName();
        return requestHistory(frameName).restoreCurrentPage();
    }

    public AWPage restoreNextPage (AWPage page)
    {
        AWEncodedString frameName = requestContext().request().frameName();
        return requestHistory(frameName).restoreNextPage(page);
    }

    public AWPage restorePreviousPage (AWPage page)
    {
        AWEncodedString frameName = requestContext().request().frameName();
        return requestHistory(frameName).restorePreviousPage(page);
    }

    /**
        Use this to get the previously cached page -- do not use session.previousPage()
        as that is only for refreshRegions support, especially since previousPage() has been removed.
    */
    public AWPage previousPage (AWPage page)
    {
        AWEncodedString frameName = requestContext().request().frameName();
        return requestHistory(frameName).getPreviousPage(page);
    }

    public void clearPageCache ()
    {
        AWEncodedString frameName = requestContext().frameName();
        requestHistory(frameName).clear();
    }

    public void removePageCache (AWEncodedString frameName)
    {
        _requestHistories.remove(frameName);
    }

    public void clearAllPageCaches ()
    {
        Iterator requestHistoryIterator = _requestHistories.values().iterator();
        while (requestHistoryIterator.hasNext()) {
            AWRequestHistory currentRequestHistory = (AWRequestHistory)requestHistoryIterator.next();
            currentRequestHistory.clear();
        }
    }

    public void _updatePageResourceManager (AWResourceManager resourceManager)
    {
        Iterator requestHistoryIterator = _requestHistories.values().iterator();
        while (requestHistoryIterator.hasNext()) {
            AWRequestHistory currentRequestHistory = (AWRequestHistory)requestHistoryIterator.next();
            for (AWPage page : currentRequestHistory._getPages()) {
                page.setResourceManager(resourceManager);
            }
        }
    }

    public void removeFromPageCache (AWPage page)
    {
        AWEncodedString frameName = requestContext().request().frameName();
        requestHistory(frameName).removePage(page);
    }

    public AWPageCacheMark markPageCache ()
    {
        AWEncodedString frameName = requestContext().request().frameName();
        return requestHistory(frameName).markPageCache();
    }

    public void truncatePageCache (AWPageCacheMark pageCacheMark, boolean inclusive)
    {
        AWEncodedString frameName = requestContext().request().frameName();
        requestHistory(frameName).truncatePageCache((AWConcretePageCacheMark)pageCacheMark, inclusive);
    }

    public void truncatePageCache (AWPageCacheMark pageCacheMark)
    {
        truncatePageCache(pageCacheMark, false);
    }

    public void markAppBoundary ()
    {
        AWEncodedString frameName = requestContext().request().frameName();
        requestHistory(frameName).markAppBoundary();
    }

    public void setRefreshURL (String url)
    {
        _refreshURL = url;
    }

    public String getRefreshURL ()
    {
        return _refreshURL;
    }

    public void setBackTrackURL (String url)
    {
        _backTrackURL = url;
    }

    public String getBackTrackURL ()
    {
        return _backTrackURL;
    }

    public void setForwardTrackURL (String url)
    {
        _forwardTrackURL = url;
    }

    public String getForwardTrackURL ()
    {
        return _forwardTrackURL;
    }

    ////////////////////
    // Redirect Support
    ////////////////////
    protected void setRedirectPage (AWPage page)
    {
        _redirectPage = page;
    }

    protected AWPage redirectPage ()
    {
        _redirectPage.ensureAwake(_requestContext);
        return _redirectPage;
    }

    ///////////////
    // Termination
    ///////////////

    protected boolean isInvalidated ()
    {
        return _isInvalidated;
    }

    public boolean isTerminated ()
    {
        return _isTerminated;
    }

    public void terminate ()
    {
        _isTerminated = true;
        if (_trackingCookie != null) {
            _trackingCookie.configureForDeletion();
        }
    }

    public void markForTermination ()
    {
        _markForTermination = true;
    }

    public boolean isMarkedForTermination ()
    {
        return _markForTermination;
    }

    protected boolean shouldInvalidate ()
    {
        return isTerminated() && !_isInvalidated;
    }

    public void valueBound (HttpSessionBindingEvent event)
    {
        // no op
    }

    //
    // This method is called when the HttpSession is invalidated
    // either through a direct call to HttpSession.invalidate() method
    // or when the app server times out the session.
    //
    // Note that HttpSession.invalidate() should never be called
    // directly by an AW application. Instead, AWSession.terminate()
    // should be called instead because it ensures that HttpSession will
    // not be invalidated until all processing has completed.
    // The HttpSession.invalidate() is then called during checkin of the
    // AWSession (see AWApplication.archiveHttpSession)
    //
    public void valueUnbound (HttpSessionBindingEvent event)
    {
        Log.aribaweb.debug("AWSession.valueUnbound() called");

        // Clear all page caches to give pages a chance to clean their state --
        // this state may include AWChangeNotifiers that are being referenced from outside
        // the AW Framework.
        clearAllPageCaches();

        setHttpSession(event.getSession());
        _isInvalidated = true;
        if (!_isTerminated) {
            terminate();
        }

        AWApplication app = (AWApplication)AWConcreteApplication.sharedInstance();
        app.deregisterSession(this);

        // Note: valueUnbound() is the last method within AWSession
        // called when the terminate() method is called. The dispose()
        // method should happen here if the session is terminated.

        if (_isTerminated) {
            dispose();
        }
    }

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        // default is to do nothing -- subclasses needn't call super.
    }

    public interface LifecycleListener
    {
        // called before awake
        void sessionWillAwake (AWSession session);

        // called before sleep (and before requestContext removed)
        void sessionWillSleep (AWSession session);
    }

    private static List<LifecycleListener> _LifecycleListeners = null;

    public static void registerLifecycleListener (LifecycleListener listener)
    {
        if (_LifecycleListeners == null) _LifecycleListeners = new ArrayList();
        _LifecycleListeners.add(listener);
    }

    protected void ensureAwake (AWRequestContext requestContext)
    {
        if (requestContext != _requestContext) {
            _requestContext = requestContext;

            AWRequest request = requestContext.request();
//            AWEncodedString frameName = request.frameName();
//            requestHistory(frameName).initRequestType(requestContext);

            if (_remoteHostAddress == null) {
                setRemoteHostAddress(request.remoteHostAddress());
            }

            HttpSession httpSession = requestContext.httpSession();
            setHttpSession(httpSession);

            // continue using stashed recording if we're finishing up a redirect request (i.e. full page refresh)
            if (_performanceStateHashtable != null && _performanceStateHashtable.isToBeContinued()) {
                _performanceStateHashtable.setToBeContinued(false);
                PerformanceState.restoreContinuedHashtable(_performanceStateHashtable);
            }

            if (PerformanceState.threadStateEnabled()) {

                PerformanceState.Stats stats = PerformanceState.getThisThreadHashtable();

                stats.setSessionID(sessionId());

                InetAddress addr = remoteIPAddress();
                if (addr != null) {
                    stats.setIPAddress(addr.getHostAddress());
                }

                String referer = request.headerForKey(HTTP.HeaderReferer);
                if (referer != null) {
                    stats.setReferer(referer);
                }

                String acceptLanguage = request.headerForKey(HTTP.HeaderAcceptLanguage);
                if (acceptLanguage != null) {
                    stats.setAcceptLanguage(acceptLanguage);
                }

                String userAgent = request.headerForKey(HTTP.HeaderUserAgent);
                if (userAgent != null) {
                    stats.setUserAgent(userAgent);
                }
                
                String[] screenSize = request.cookieValuesForKey("awscreenstats");
                if (screenSize != null && screenSize.length > 0 && screenSize[0] != null){
                    stats.setScreenSize(screenSize[0]);
                }

                String seleniumShortId = request.formValueForKey("testShortId");
                String seleniumId = request.formValueForKey("testId");
                String seleniumLineId = request.formValueForKey("testLineNb");

                if (seleniumShortId != null) {
                    _testShortId = seleniumShortId;
                }
                if (seleniumId != null) {
                    _testId = seleniumId;
                }
                if (seleniumLineId != null) {
                    _testLine = seleniumLineId;
                }

                stats.setTestShortId(_testShortId);
                stats.setTestId(_testId);
                stats.setTestLine(_testLine);

                stats.setShutdownMode(Boolean.toString(
                    application().monitorStats().isInShutdownWarningPeriod()));

                stats.setTabIndex(Integer.toString(requestContext.getTabIndex()));
            }

            _lastAccessedTime = _httpSession.getLastAccessedTime();
            _updateLatestPerformanceStats = true;

            if (_LifecycleListeners != null) {
                for (LifecycleListener l : _LifecycleListeners) l.sessionWillAwake(this);
            }

            awake();
            //logString("***** Session awake environmentStack:\n" + environmentStack().topOfStacks());
        }
    }

    protected void sleep ()
    {
            // default is to do nothing -- subclasses needn't call super.
    }

    protected void ensureAsleep ()
    {
        if (_requestContext != null) {
            if (PerformanceState.threadStateEnabled()) {
                if (_updateLatestPerformanceStats) {
                    // save this for later lookup (by AWPerfPane)
                    _performanceStateHashtable =
                        PerformanceState.getThisThreadHashtable();
                }
            }

            if (_LifecycleListeners != null) {
                for (LifecycleListener l : _LifecycleListeners) l.sessionWillSleep(this);
            }

            sleep();
            setHttpSession(null);
            _requestContext = null;
        }
    }

    public PerformanceState.Stats lastRequestPerfStats ()
    {
        return _performanceStateHashtable;
    }

    /**
        Support to segment monitor Stats
    */
    protected Object monitorBucket ()
    {
        return null;
    }

    /**
     * This call prevents that the last perf stats will be replaced
     * by the current one in the sleep phase.
     *
     * @aribaapi private
     */
    public void dontReplaceLastPerformanceStats ()
    {
        _updateLatestPerformanceStats = false;
    }

    /**
     * Called after the page has finished running the invokeAction phase.
     * Put logic here to clear state that is not needed from one append/take/invoke
     * cycle to another.  Beware that under some circumstatnces this method may
     * not be called from one append to another.
     * @aribaapi private
     */
    protected void postInvoke ()
    {
        return;
    }

    /**
     * Called before the page runs the append phase.
     * Put logic here to initialize or clear state at the beginning of an
     * append/take/invoke cycle.
     * @aribaapi private
     */
    protected void preAppend ()
    {
        return;
    }

    ///////////////
    // Convenience
    ///////////////
    public AWComponent pageWithName (String pageName)
    {
        return _requestContext.pageWithName(pageName);
    }

    protected AWCookie initTrackingCookie (AWRequest request)
    {
        // expiration in seconds == -1: expire when browser closed*
        //domainName == null: use current server
        return new AWCookie(AWRequestContext.SessionIdKey, _httpSession.getId(), null, _application.applicationUrl(request()), request.isSecureScheme(), -1);
    }

    protected AWCookie trackingCookie ()
    {
        if (_trackingCookie == null && !_isTerminated) {
            _trackingCookie = initTrackingCookie(request());
        }
        return _trackingCookie;
    }

    public int historyPosition ()
    {
        return requestHistory(requestContext().frameName()).getHistoryPosition();
    }

    public int historyLength ()
    {
        return requestHistory(requestContext().frameName()).getHistoryLength();
    }

    public String brandName ()
    {
        return _brandName;
    }

    // Is this method ever necessary?  Security.
    public void setBrandName (String brandName)
    {
        if (_brandName == null || !_brandName.equals(brandName)) {
            _brandName = brandName;
            // we've changed the realmName so make sure resource manager
            // is setup properly
            setResourceManager(initResourceManager(preferredLocale()));
        }
    }

    public String brandVersion ()
    {
        return _brandVersion;
    }
    public void setBrandVersion (String brandVersion)
    {
        if (_brandVersion == null || !_brandVersion.equals(brandVersion)) {
            _brandVersion = brandVersion;
            // we've changed the brandVersion so make sure resource manager
            // is setup properly
            setResourceManager(initResourceManager(preferredLocale()));
        }
    }

    public boolean isBrandTestMode ()
    {
        return _brandTestMode;
    }
    public void setBrandTestMode (boolean flag)
    {
        _brandTestMode = flag;
    }

    public boolean hasChanged ()
    {
        return hasNotification() ||
               hasShutdownWarning();
    }

    /**
        We are assuming many writers, but only one reader
    */
    public void addNotification (AWNotification notification)
    {
        synchronized (_notificationsLock) {
            if (_notifications != null) {
                _notifications.add(notification);
            }
        }
    }

    public List getNotifications ()
    {
        synchronized (_notificationsLock) {
            List notifications = _notifications;
            _notifications = ListUtil.list();
            return notifications;
        }
    }

    /**
       No need to synchronize, since this will become false
       only after the one reader consumes the list
    */
    public boolean hasNotification ()
    {
        return !_notifications.isEmpty();
    }

    private void disposeNotifications ()
    {
        Object disposeTarget;
        synchronized (_notificationsLock) {
            disposeTarget = _notifications;
            _notifications = null;
        }
        AWUtil.dispose(disposeTarget);
    }

    public boolean hasShutdownWarning ()
    {
        boolean hasShutdownWarning = false;
        if (_application.monitorStats().isInShutdownWarningPeriod()) {
            if (_shutdownState == null) {
                _shutdownState = new AWShutdownState();
            }
            hasShutdownWarning = _shutdownState.shouldDisplayWarning();
        }
        else {
            // Set to null in case shutdown was cancelled.
            _shutdownState = null;
        }
        return hasShutdownWarning;
    }

    public void getShutdownWarning ()
    {
        if (_shutdownState != null) {
            _shutdownState.getWarning();
        }
    }

    public void setAllowParentFrame (boolean allowParentFrame)
    {
        _allowParentFrame = allowParentFrame;
    }

    public boolean allowParentFrame ()
    {
        return AWPage.AllowParentFrame || _allowParentFrame;
    }

    public boolean omitWrapperFrame ()
    {
        return _omitWrapperFrame;
    }

    /**
     * @param omit when true, the wrapper frame is omitted on all pages rendered
     * in this session. When false, the binding value on the page wrapper is used.  
     */
    public void setOmitWrapperFrame (boolean omit)
    {
        this._omitWrapperFrame = omit;
    }
}

final class AWConcretePageCacheMark extends AWBaseObject implements AWPageCacheMark
{
    private int _index;
    private boolean _preventsBacktracking = false;

    public AWConcretePageCacheMark (int index)
    {
        super();
        _index = index;
        _preventsBacktracking = false;
    }

    public int index ()
    {
        return _index;
    }

    public void decrementIndex ()
    {
        _index--;
    }

    public void setPreventsBacktracking (boolean flag)
    {
        _preventsBacktracking = flag;
    }

    public boolean preventsBacktracking ()
    {
        return _preventsBacktracking;
    }
}
