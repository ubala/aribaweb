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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/html/AWClientSideScript.java#26 $
*/

package ariba.ui.aribaweb.html;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.util.core.StringUtil;
import ariba.util.core.GrowOnlyHashtable;
import ariba.util.core.Assert;
import ariba.util.core.Fmt;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWXDebugResourceActions;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.Log;

import java.io.InputStream;

public final class AWClientSideScript extends AWComponent
{
    private static final String isSingleton = "isSingleton";
    private static final String executeOnIncrementalUpdate =
            "executeOnIncrementalUpdate";
    private static final String globalScope = "globalScope";
    private static final String executeOn = "executeOn";
    private static final String handle = "handle";
    private static final String RJSMayNotBeDefined = "RJSMayNotBeDefined";

    private static final String[] SupportedBindingNames =
        {BindingNames.scriptFile, BindingNames.scriptString,
         BindingNames.language, BindingNames.filename,
         BindingNames.elementId, BindingNames.invokeAction, RJSMayNotBeDefined,
         isSingleton, executeOnIncrementalUpdate, globalScope, executeOn, handle,
         BindingNames.forceDirectInclude, BindingNames.synchronous};

    private static final GrowOnlyHashtable ScriptFileHashtable =
            new GrowOnlyHashtable();

    private static final String Javascript = "javascript";
    private static final String VBScript = "vbscript";

    public String _filename;
    public boolean _isGlobalScope = false;
    public boolean _isVBScript = false;
    public AWEncodedString _spanId;
    public String _executeOn;

    protected void sleep ()
    {
        _filename = null;
        _isGlobalScope = false;
        _isVBScript = false;
        _spanId = null;
        _executeOn = null;
    }

    protected void awake ()
    {
        super.awake();

        _filename = stringValueForBinding(BindingNames.filename);

        String language = stringValueForBinding(BindingNames.language);
        language = StringUtil.nullOrEmptyOrBlankString(language) ?
                Javascript : language.toLowerCase();
        _isVBScript = VBScript.equals(language);

        if (booleanValueForBinding(globalScope)) {
            _isGlobalScope = true;
        } else {
            _isGlobalScope = !Javascript.equals(language) && !_isVBScript;
        }
        _executeOn = stringValueForBinding(executeOn);
    }

    private static boolean nullOrFalse (Object o)
    {
        return (o == null) || !((Boolean)o).booleanValue();
    }

    public boolean useDirectInclude ()
    {
        return !AWPage.AllowIncrementalScriptLoading()
            || booleanValueForBinding(BindingNames.forceDirectInclude)
            || !nullOrFalse(env().peek(AWBindingNames.scriptForceDirectInclude));
    }

    public boolean useDirectGlobalScript ()
    {
        return _isGlobalScope && !AWPage.DeferGlobalScopeScript();
    }

    public boolean formatInPre ()
    {
        return (_executeOn != null || hasBinding(handle));
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        // if this clientsidescript is for a .js resource, then we need to have a full
        // page refresh so the methods, global vars, etc. get registered in the main
        // window rather than the refresh iframe.  We could copy all these from the
        // refresh iframe over to the main window, but there are additional issues there
        // that we have to solve.  Since we can't incrementally render a full page
        // refresh -- ie, figure out during diffing that we need a fpr and then bust out
        // of the diff mode and send a fpr (see AWPage, AWResponseBuffer, etc.) -- we do
        // the following test.
        if (_filename !=  null) {
            // Our basic strategy is to force a FPR the first time we see a script
            // rendered on a page.  The corner case that causes problems with a single
            // script tracking list is the following --
            // * p1 displayed and script1 rendered, then script1 placed in p1 script list
            // * subsequent redisplay of p1 causes fpr (p1'), but p1' does not contain
            //   script1. (thus at this point, p1' does not contain the script1, but the
            //   script list on the server still indicates that script1 has been
            //   registered on the page)
            // * incremental update of p1' (p1'') once again contains script1, but since
            //   script1 is contained in p1 script list, we don't trigger the FPR required
            //   for script1 to once again be registered on the main window.

            // Solve this by using two script lists --
            // page script list -- what is currently registered on the page on the client
            // current script list -- script list from current rendering of page
            // if a FPR occurs, then copy current script list to page script list.
            // use page script list when determining whether or not to force a FPR

            // if this is the first time we have rendered the script, then force a FPR
            if (!page().hasScript(_filename)) {
                if (!requestContext().fullPageRefreshRequired() && !useDirectInclude()) {
                    Log.domsync.debug("Adding new script to page: %s (page:%s, list:%s)",
                            _filename, page(), page()._pageScriptList());
                }
                if (!AWPage.AllowIncrementalScriptLoading()) requestContext().forceFullPageRefresh();
            }
            // always track the list of current scripts -- in case of a FPR, we replace
            // the previous list of scripts with this list
            page().recordCurrentScript(_filename);
        }

        super.renderResponse(requestContext, component);
    }

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public String scriptFileUrl ()
    {
        String scriptFileUrl =
                (((AWConcreteApplication)application()).allowsJavascriptUrls())
            ? urlForResourceNamed(_filename, false, true)
            : AWXDebugResourceActions.urlForResourceNamed(requestContext(), _filename);
        Assert.that(scriptFileUrl != null, "%s: unable to locate " +
                "file named \"%s\"", getClass().getName(), _filename);
        return scriptFileUrl;
    }

    public boolean hasScriptBinding ()
    {
        boolean hasScriptBinding =
                (bindingForName(BindingNames.scriptFile, true) != null) ||
                (bindingForName(BindingNames.scriptString, true) != null);
        return hasScriptBinding;
    }

    public String scriptString ()
    {
        String scriptString = null;
        AWBinding binding = bindingForName(BindingNames.scriptFile, false);
        if (binding != null) {
            String scriptFileName = (String)valueForBinding(binding);
            scriptString = (String)ScriptFileHashtable.get(scriptFileName);
            if (scriptString == null) {
                AWResource resource = resourceManager().resourceNamed(scriptFileName);
                if (resource == null) {
                    throw new AWGenericException("Unable to locate script " +
                            "file named: " + scriptFileName);
                }
                InputStream scriptInputStream = resource.inputStream();
                scriptString = AWUtil.stringWithContentsOfInputStream(scriptInputStream);
                AWUtil.close(scriptInputStream);
                if (!AWConcreteApplication.IsRapidTurnaroundEnabled) {
                    ScriptFileHashtable.put(scriptFileName, scriptString);
                }
            }
        }
        else if ((binding = bindingForName(BindingNames.scriptString, false)) != null) {
            scriptString = (String)valueForBinding(binding);
        }
        else {
            scriptString = getClass().getName() + ": Invalid script binding";
        }
        return scriptString;
    }

    public void setupSpanId ()
    {
        _spanId = requestContext().nextElementId();
        if (hasBinding(handle)) {
            String str = Fmt.S("ariba.Event.evalJSSpan('%s')", _spanId);
            setValueForBinding(str, handle);
        }
    }

    public String vbsFlag ()
    {
        return _isVBScript ? "1" : null;
    }

    protected String _debugCompositeSemanticKey (String bestKeySoFar)
    {
        // prevent semantic key generation
        return null;
    }
}
