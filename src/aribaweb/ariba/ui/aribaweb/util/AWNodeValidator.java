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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWNodeValidator.java#9 $
*/
package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWFormRedirect;
import ariba.ui.aribaweb.core.AWDirectActionUrl;
import ariba.ui.aribaweb.core.AWRedirect;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.HTML;
import java.util.Iterator;

/**
 * Node validation is used to support application node affinity.  Using the AWNodeManager
 * and the AWNodeValidator classes, the application can associate a session to a specific
 * node.
 *
 * The first step is to associate an AWNodeValidator to a DirectAction classname
 * and method name.  This DirectAction classname / method name are used at the target
 * DirectAction when node redirection occurs.  The target action is the first action run
 * on the new node when the user is moved from the current node to the new node.
 *
 * There are two mechanisms for initiating node validation -- one for DirectAction
 * requests and one for ComponentAction requests.
 *
 * For a DirectAction:
 * In this case, the DirectAction class name / method name are used as the keys to look
 * up the AWNodeValidator.  In this case, the DirectAction which is "protected"
 * by the NodeValidator is the same as the target DirectAction used during node
 * redirection.
 * 1. At the point that the DirectAction is invoked, an AWNodeValidator is retrieved (if
 *    registered) and the isValid method is called.
 * 2. If isValid returns false, then the AWNodeValidator.handleNodeValidationException is
 *    invoked.  This method should be implemented by the AWNodeValidator subclass to
 *    create the AWResponse which should redirect the browser to the correct node.
 * 3. The AWNodeValidator.terminateCurrentSession method is called just before actually
 *    returning the AWResponse to the browser.  This method should be used to terminate
 *    the session on the current node and handle any additional cleanup before the user is
 *    moved to the new node.
 * 4. The AWResponse is returned to the browser and a new request is initiated to the
 *    correct node using the same DirectAction class / method used in step 1.
 *
 * For a ComponentAction:
 * In this case, the application is responsible for initiating node validation.
 * 1. The application manages when the node validation is initiated
 * 2. An AWNodeValidator instance can (should?) be used to validate the current request.
 * 3. If the current node is determined to be invalid, then the application code should
 *    throw  an AWNodeChangeException.  The getNodeChangeException() method can be used as
 *    a convenience.
 *
 *      AWNodeValidator nv = getMyComponentActionNodeValidator();
 *      if (!nv.isValid()) {
 *          throw nv.getNodeChangeException();
 *      }
 *
 *      SoftAffinityComponentActionTestValidator validator =
 *          SoftAffinityComponentActionTestValidator.SharedInstance;   // a
 *      AWRequestContext requestContext = requestContext();
 *      if (validator.isValid(requestContext)) {                       // b
 *          validator.setTargetNode(requestContext,"Node2");           // c
 *          throw validator.getNodeChangeException();
 *      }
 *      // on correct node so continue processing
 *
 *    Several items to note.
 *    a) using a shared instance to avoid additional object instantiation
 *    b) encapsulation of validation check in the validator is not strictly necessary
 *    c) "setTargetNode" is an example utility method in this particular test validator
 *       that will "save" the target node name in the requestContext.  This is not
 *       necessary, but is nice because we're able to encapsulate saving / retrieving of
 *       this information in the SoftAffinityComponentActionTestValidator.
 *
 *
 * 4. At some point, the AWNodeValidator.handleNodeValidationException will be invoked on
 *    the NodeValidator (shared)instance.  This method should be implemented by the
 *    AWNodeValidator subclass to create the AWResponse which should redirect the browser
 *    to the correct node.
 * 5. The AWNodeValidator.terminateCurrentSession method is called just before actually
 *    returning the AWResponse to the browser.  This method should be used to terminate
 *    the session on the current node and handle any additional cleanup before the user is
 *    moved to the new node.
 * 6. The AWResponse is returned to the browser and a new request is initiated to the
 *    correct node using the same DirectAction class / method used in step 1.
 *
 * Two utility methods are provided to make the creation of this AWResponse simpler.  The
 * first is getFormRedirectToNode.  Given the a nodename, this method will create an
 * AWFormRedirect and retrieve the appropriate adaptorUrl by calling
 * AWNodeManager.adaptorUrlForNode.  This method will create a redirect which invokes the
 * SAME DirectAction which was called in step 1 above.
 *
 * Once this AWFormRedirect is created, the subclass implementation of
 * handleNodeValidationException can use formRedirect.addFormValue(name,value) to add
 * additional form values onto the AWFormRedirect.  In addition the
 * AWNodeValidator.addCurrentRequestParams can be used to add all current request params
 * onto the form redirect.
 *
 * NOTE: only one AWNodeValidator instance is registered per DirectAction class / method
 * so all methods must be threadsafe.
 *
 * ----------------------------------------------
 * Mechanisms for transferring node affinity
 * ----------------------------------------------
 * There are a number of possibilities for the actual handling of browser session
 * to node association.  Given a servletadaptor implementation (ie, AW running on a
 * servlet engine), possibilities are:
 * 1) Webserver plugin handles URL rewriting
 * AWNodeManager returns an adaptor URL which looks like:
 *      http(s)://hostname/contextroot/[nodename]
 * Using the getFormRedirectToNode() method will then create a redirect to a URL of the form:
 *      http(s)://hostname/contextroot/[nodename]/appName/ad/[method]/[class]
 * where [method] and [class] are the directAction methodname and classname.
 * This request is handled by the webserver plugin which uses [nodename] to determine
 * the specific node to forward to AND rewrites the forwarded URL to strip off [nodename]
 * so when the request gets to the servlet engine, the URL looks like:
 *      http(s)://hostname/contextroot/appName/ad/[method]/[class]
 * Standard session affinity occurs at this point which will then pin the session to
 * the right node.  Note that the session cookie path is scoped to context root so the
 * [nodename] must appear somewhere AFTER contextroot in the URL request from the browser.
 *
 * 2) Servlet on each node responsible for forcing session affinity and redirecting to
 *    target URL.
 * AWNodeManager returns an adaptor URL which looks like:
 *      http(s)://hostname/contextroot/[nodename]
 * Using the getFormRedirectToNode() method will then create a redirect to a URL of the form:
 *      http(s)://hostname/contextroot/[nodename]/appName/ad/[method]/[class]
 * The webserver still needs to be configured to forward the request to the right node
 * based on [nodename] BUT no URL rewriting occurs.
 * The request to the servlet engine is then:
 *      http(s)://hostname/contextroot/[nodename]/appName/ad/[method]/[class]
 * A servlet is registered in the servletengine which corresponds to [nodename].  This
 * servlet is merely responsible for creating an httpsession (which allows standard
 * session affinity to occur) and redirecting to the target URL:
 *      http(s)://hostname/contextroot/appName/ad/[method]/[class]
 *
 */
public abstract class AWNodeValidator
{
    private String _actionName;
    private String _actionClassName;
    private AWNodeManager _nodeManager;

    public AWNodeValidator (String directionActionClassName, String directActionName)
    {
        _actionClassName = directionActionClassName;
        _actionName = directActionName;
    }

    public abstract boolean isValid (AWRequestContext requestContext);

    public abstract AWResponseGenerating handleNodeValidationException (
        AWRequestContext requestContext);

    protected final String getRedirectUrl (AWRequestContext requestContext,
                                           String nodeName)
    {
        AWDirectActionUrl directActionUrl = AWDirectActionUrl.checkoutUrl();
        directActionUrl.setDirectActionName(getRequestDirectActionName(requestContext));
        directActionUrl.setDirectActionClassName(getRequestDirectActionClassName(requestContext));
        String adaptorUrl = _nodeManager.adaptorUrlForNode(nodeName);
        directActionUrl.setAdaptorUrl(adaptorUrl);
        return directActionUrl.finishUrl();
    }

    protected String getRequestDirectActionName (AWRequestContext requestContext)
    {
        return _actionName;
    }

    protected String getRequestDirectActionClassName (AWRequestContext requestContext)
    {
        return _actionClassName;
    }

    protected final AWRedirect getRedirectToNode (AWRequestContext requestContext,
                                                  String nodeName)
    {
        return AWRedirect.getRedirect(requestContext,
                                      getRedirectUrl(requestContext, nodeName));
    }

    /**
     * Utility method that creates a component which redirects the browser to the
     * correct node.  The DirectActionClassName + DirectActionName are used to determine
     * the direct action to which the user is redirected.  The nodeName is key used to
     * request the adaptorURL (protocol://host/contextroot/
     * @param requestContext
     * @param nodeName
     * @aribaapi private
     */
    protected final AWFormRedirect getFormRedirectToNode (AWRequestContext requestContext,
                                                          String nodeName)
    {
        AWFormRedirect formRedirect =
            (AWFormRedirect)requestContext.createPageWithName(AWFormRedirect.PageName);
        formRedirect.setFormActionUrl(getRedirectUrl(requestContext,nodeName));

        // use: formRedirect.addFormValue(name,value);
        // to add additional form params

        return formRedirect;
    }

    /**
     * Utility method to determine if redirecting the current request will require a
     * form post redirect.
     * @param requestContext
     * @aribaapi private
     */
    protected final boolean useFormRedirect (AWRequestContext requestContext)
    {
        int count = 0;
        Iterator keys = requestContext.formValues().keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            count += key.length();
            count += requestContext.formValueForKey(key).length();
        }

        return count > 1024;
    }


    private static final char BeginQueryChar = '?';
    private static final char QueryDelimiter ='&';
    private static final char Equals = '=';
    protected final void addQueryParam (FastStringBuffer url, String name, String value)
    {
        if (url.indexOf(BeginQueryChar) == -1) {
            url.append(BeginQueryChar);
        }
        else {
            if (url.charAt(url.length()-1) != QueryDelimiter) {
                url.append(QueryDelimiter);
            }
        }
        String safeName = AWUtil.encodeString(name);
        url.append(safeName);
        url.append(Equals);
        String safeValue = AWUtil.encodeString(value);
        url.append(safeValue);
    }

    /**
     * Utility method to add all current request params onto the AWFormRedirect.
     * @param requestContext
     * @param redirect
     * @aribaapi private
     */
    protected final void addCurrentRequestParams (AWRequestContext requestContext,
                                                  AWRedirect redirect)
    {
        FastStringBuffer url = new FastStringBuffer(redirect.url());

        Iterator keys = requestContext.formValues().keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            addQueryParam(url,key,requestContext.formValueForKey(key));
        }

        redirect.setUrl(url.toString());
    }

    /**
     * Utility method to add all current request params onto the AWFormRedirect.
     * @param requestContext
     * @param formRedirect
     * @aribaapi private
     */
    protected final void addCurrentRequestParams (AWRequestContext requestContext,
                                                  AWFormRedirect formRedirect)
    {
        Iterator keys = requestContext.formValues().keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            String safeParamName = HTML.escape(key);
            if (key.equals(safeParamName)) {
                // only add safe params
                String formValue = requestContext.formValueForKey(key);
                formValue = AWUtil.attributeEscape(formValue);
                formRedirect.addFormValue(key, formValue);
            }
            else {
                Log.aribaweb.warning(10347, key);                
            }
        }
    }

    public abstract void terminateCurrentSession (AWRequestContext requestContext);

    public String getActionClassName ()
    {
        return _actionClassName;
    }

    public String getActionName ()
    {
        return _actionName;
    }

    public void setNodeManager (AWNodeManager manager)
    {
        _nodeManager = manager;
    }

    public AWNodeChangeException getNodeChangeException ()
    {
        return new AWNodeChangeException(this);
    }
}
