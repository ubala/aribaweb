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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWLocalLoginSessionHandler.java#7 $
*/
package ariba.ui.aribaweb.core;

import ariba.util.core.MapUtil;

import javax.servlet.http.HttpSession;
import java.util.Map;

public abstract class AWLocalLoginSessionHandler implements AWSessionValidator
{
    /*
        Client must override to present their login page.
        Upon login success or failure, client should return callback.success()/failure()
     */
    protected abstract AWResponseGenerating showLoginPage (AWRequestContext requestContext,
                                                           CompletionCallback callback);

    /*
        Override this instead of assertValidSession() to determine whether user is logged in
     */
    protected boolean validateSession (AWRequestContext requestContext)
    {
        return true;
    }

    /*
        App can override if assertValidSession should apply for all actions.
        Otherwise, actions / pages should check for a valid user explicitly and
        throw a AWSessionValidationException where necessary
     */
    protected boolean requireSessionValidationForAllComponentActions ()
    {
        return false;
    }

    public static class CompletionCallback
    {
        ReplayInvocation _replayInvocation;

        CompletionCallback(ReplayInvocation replayInvocation)
        {
            _replayInvocation = replayInvocation;
        }

        public AWResponseGenerating proceed (AWRequestContext requestContext)
        {
            return _replayInvocation.proceed(requestContext);
        }

        public AWResponseGenerating cancel (AWRequestContext requestContext)
        {
            return _replayInvocation.cancel(requestContext);
        }
    }

    public AWResponseGenerating handleSessionValidationError (
        AWRequestContext requestContext, Exception exception)
    {
        // user not authenticated
        return _handleValidationError(requestContext, (exception instanceof AWSessionRestorationException));
    }

    public AWResponseGenerating handleComponentActionSessionValidationError (
        AWRequestContext requestContext, Exception exception)
    {
        return _handleValidationError(requestContext, (exception instanceof AWSessionRestorationException));
    }

    AWResponseGenerating _handleValidationError (
        AWRequestContext requestContext, boolean hadNoSession)
    {
        // if no session just create one and hit front door
        AWSession session = requestContext.session(false);
        if (session == null) {
            HttpSession httpSession = requestContext.createHttpSession();
            session = AWSession.session(httpSession);

            if (hadNoSession) {
                try {
                    return AWComponentActionRequestHandler.SharedInstance.processFrontDoorRequest(requestContext);
                }
                catch (AWSessionValidationException e) {
                    return _handleValidationError(requestContext, false);
                }
            }
        }

        ReplayInvocation invocation = new ReplayInvocation(requestContext);
        AWResponseGenerating login = showLoginPage(requestContext, new CompletionCallback(invocation));
        return processExceptionHandlerResults(login, requestContext, session);
    }

    private AWResponse processExceptionHandlerResults (
        AWResponseGenerating handlerResults,
        AWRequestContext requestContext,
        AWSession session)
    {
        AWResponse response = null;
        if (handlerResults instanceof AWComponent) {
            AWPage handlerPage = ((AWComponent)handlerResults).page();
            if (session != null && handlerPage.pageComponent().shouldCachePage()) {
                session.savePage(handlerPage, true);
                requestContext.setPage(handlerPage);
                response = requestContext.generateResponse();
            }
        }
        if (response == null) {
            response = handlerResults.generateResponse();
        }
        return response;
    }

    /**
     * If we have a session timeout, just go to the start page
     */
    public AWResponseGenerating handleSessionRestorationError (AWRequestContext requestContext)
    {
        return requestContext.application().mainPage(requestContext);
    }

    public void assertExistingSession (AWRequestContext requestContext)
    {

    }

    public void assertValidSession (AWRequestContext requestContext)
    {
        AWRequestHandler requestHandler = ((AWConcreteServerApplication)requestContext.application())
                .requestHandlerForRequest(requestContext.request());
        if (!(requestHandler instanceof AWComponentActionRequestHandler)
            || requireSessionValidationForAllComponentActions())
        {
            if (!validateSession(requestContext)) throw new AWSessionValidationException();      
        }
    }

    /*
        Captures a request for replay (via a later form-post redirect
     */
    static final String[] FormTrue = { "true" };

    static class ReplayInvocation
    {
        String _requestUrl;
        Map<String, String[]> _formValues;
        AWComponent _prevPage;
        AWPageCacheMark _pageMark;

        public ReplayInvocation (AWRequestContext requestContext)
        {
            AWBaseRequest req = (AWBaseRequest)requestContext.request();
            _formValues = MapUtil.cloneMap(requestContext.formValues());

            _requestUrl = req.uriForReplay(requestContext, _formValues);
            // TODO: breaks ASN!

            AWSession session = requestContext.session(false);
            if (requestContext.page() != null && session != null) {
                _prevPage = requestContext.pageComponent();
                _pageMark = _prevPage.session().markPageCache();
            }
        }

        public AWResponseGenerating cancel (AWRequestContext requestContext)
        {
            if (_pageMark != null) requestContext.session().truncatePageCache(_pageMark, false);
            return _prevPage;
        }

        public AWResponseGenerating proceed (AWRequestContext requestContext)
        {
            AWSession session = requestContext.session(false);

            if (session != null) {
                // pop the login page off the page cache
                if (_pageMark != null) session.truncatePageCache(_pageMark, false);

                String[] rIDA = _formValues.get(AWRequestContext.ResponseIdKey);
                if (rIDA != null) {
                    String origRequestId = rIDA[0];
                }

                // advance our request ID so this doesn't look like a backtrack / refresh
                int nextId = session.nextResponseId();
                rIDA = new String[1];
                rIDA[0] = Integer.toString(nextId);
                _formValues.put(AWRequestContext.ResponseIdKey, rIDA);

                // In case this was a front door request, force session rendezvous
                // so we don't kill the session on replay
                _formValues.put(AWBaseRequest.IsSessionRendezvousFormKey, FormTrue);
                _formValues.put(AWBaseRequest.AllowFailedComponentRendezvousFormKey, FormTrue);
            }
            // If this was an incremental request, it's not anymore...
            _formValues.remove(AWRequestContext.IncrementalUpdateKey);

            AWFormRedirect formRedirect =
                (AWFormRedirect)requestContext.createPageWithName(AWFormRedirect.PageName);
            formRedirect.addFormValues(_formValues);
            formRedirect.setFormActionUrl(_requestUrl);
            return formRedirect;
        }
    }
}
