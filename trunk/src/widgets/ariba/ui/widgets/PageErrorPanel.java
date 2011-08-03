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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/PageErrorPanel.java#32 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWHiddenFormValueHandler;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;
import ariba.util.core.StringUtil;
import ariba.util.core.ResourceService;
import ariba.util.formatter.BooleanFormatter;

public final class PageErrorPanel extends AWComponent
    implements AWHiddenFormValueHandler
{
    private static final String PageErrorPanelIsMinimizedKey = "PageErrorPanelIsMinimized";

    private boolean _showingPanel = false;
    private boolean _allowNext = true;
    private boolean _allowPrevious = true;
    private AWErrorInfo _highLightedError = null;
    private AWEncodedString _deferredBubbleIndicatorId = null;
    private AWErrorManager _foregroundErrorManager = null;

    public boolean isStateless ()
    {
        return false;
    }

    // we use the error manager of the foreground panel
    public AWErrorManager errorManager ()
    {
        if (_foregroundErrorManager == null) {
            // Take and Invoke can be skipped for the top most page/panel.
            // Remember the errorManager that we are working with so that
            // our error nav actions are on the foreground error manager that
            // we rendered against.
            _foregroundErrorManager = page().foregroundErrorManager();
            Log.aribaweb_errorManager.debug("PageErrorPanel is operating on errorManager %s",
                _foregroundErrorManager.getLogPrefix());
        }
        return _foregroundErrorManager;
    }

    public void init ()
    {
        super.init();
        page().hiddenFormValueManager().registerHiddenFormValue(this);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {

        _foregroundErrorManager = null;

        // calculate once per lifecycle to keep this value stable thru append/take/invoke
        AWErrorManager errorManager = errorManager();
        _showingPanel = errorManager.isErrorDisplayEnabled() &&
                        errorManager.getNumberOfErrors() > 0 &&
                        !errorManager.errorPanelDisabled();
        if (_showingPanel) {
            initAllowPrevious();
            initAllowNext();
            initHighLightedErrorMsg();
            initDeferredBubble();
        }
        if (Log.aribaweb_errorManager.isDebugEnabled()) {
            Log.aribaweb_errorManager.debug(
                "%s: PageErrorPanel: showingPanel=%s (errorMode=%s numErrors=%s)",
                errorManager.getLogPrefix(),
                ariba.util.core.Constants.getBoolean(_showingPanel),
                ariba.util.core.Constants.getBoolean(errorManager.isErrorDisplayEnabled()),
                Integer.toString(errorManager.getNumberOfErrors()));
        }
        // If we are not showing the error panel, then tell the error manager
        // to clear out any memory of the current error.  This will ensure
        // a clean slate for future error navigation.
        if (!_showingPanel) {
            errorManager.clearHighLightedError();
        }

        super.renderResponse(requestContext, component);
    }

    public String getDivId ()
    {
        return "PageErrorPanel";
    }

    public boolean showingPageErrorPanel ()
    {
        return _showingPanel;
    }

    public String errorMsg ()
    {
        // Should reconcile with the error string in ARPPage
        if (Log.aribaweb_errorManager.isDebugEnabled()) {
            int numErrorsExcludingKnownWarnings= errorManager().getNumberOfErrors(true);
            if (numErrorsExcludingKnownWarnings == 0) {
                Log.aribaweb_errorManager.debug("Current warnings have been shown to the user");
            }
        }

        AWErrorManager errMgr = errorManager();
        int numErrors = errMgr.getNumberOfErrors(false);
        boolean warningsOnly = errMgr.allErrorsAreWarnings() && errMgr.getIgnoreKnownWarnings();
        if (!warningsOnly) {
            if (numErrors > 1) {
                return Fmt.Si(localizedJavaString(1, "There are {0} problems that require completion or correction in order to complete your request." /* generic page error 0-number of errs */), ariba.util.core.Constants.getInteger(numErrors));
            }
            else {
                return Fmt.Si(localizedJavaString(2, "There is {0} problem that requires completion or correction in order to complete your request." /* generic page error 0-number of errs */), ariba.util.core.Constants.getInteger(numErrors));
            }
        }
        else {
            if (numErrors > 1) {
                return Fmt.Si(localizedJavaString(4, "There are {0} warnings that require your attention.  Please try again if you want to proceed after reviewing the warnings."), ariba.util.core.Constants.getInteger(numErrors));
            }
            else {
                return Fmt.Si(localizedJavaString(5, "There is {0} warning that requires your attention.  Please try again if you want to proceed after reviewing the warning."), ariba.util.core.Constants.getInteger(numErrors));
            }
        }
    }

    public String hintMsg ()
    {
        return localizedJavaString(3, "Mouse over the red icons to learn more.  Use the <b><i>Next</i></b> and <b><i>Previous</i></b> links to step through the errors as needed." /*  */);
    }

    /*-----------------------------------------------------------------------
       Bindings for displaying actual error messages
      -----------------------------------------------------------------------*/

    public boolean isSingleErrorDisplay ()
    {
        int numErrors = errorManager().getNumberOfErrors();
        return numErrors == 1 &&
               !StringUtil.nullOrEmptyString(getHighLightedErrorMsg());
    }

    public String highLightedErrorAlign ()
    {
        return hasDeferredNavHandler() ? "left" : "center";
    }

    public String highLightedErrorStyle ()
    {
        return isSingleErrorDisplay() ? "msgTextSingleError errBg" : "msgText errBg";
    }

    public String getHighLightedErrorMsg ()
    {
        String message = null;
        if (_highLightedError != null) {
            message = _highLightedError.getMessage();
            if (message != null && message.startsWith(ResourceService.NlsTag)) {
                message = ResourceService.getService().getLocalizedCompositeKey(message, preferredLocale());
            }
        }
        return message;
    }

    public boolean highLightedErrorIsWarning ()
    {
        return _highLightedError != null ? _highLightedError.isWarning() : false;
    }


    private void initHighLightedErrorMsg ()
    {
        // If errors are registered during append, then auto nav might
        // not have taken place.  This panel is the last component to be
        // rendered on the page.  So everything else should be rendered
        // by now - ensure auto nav takes place.
        // This call should NOT modify server state. Assuming the error was 
        // registered during render, the error should be visible on the same page.
        errorManager().navToErrorAsNeeded(false, null, true);
        AWErrorInfo error = errorManager().getHighLightedError();
        if (error != null && error.getDisplayOrder() == AWErrorInfo.NotDisplayed) {
            _highLightedError = error;
            Log.aribaweb_errorManager.debug(
                "PageErrorPanel: error displayed in the panel: %s", error.getMessage());
        }
        else {
            _highLightedError = null;
        }
    }

    public boolean hasDeferredNavHandler ()
    {
        if (getHighLightedErrorMsg() == null) {
            return false;
        }

        return errorManager().hasDeferredNavHandlerForCurrentError();
    }

    public AWComponent navToErrorAction ()
    {
        return errorManager().navUsingDeferredNavHandler(pageComponent());
    }

    public boolean hasDeferredBubbleDisplay ()
    {
        return getDeferredBubbleIndicatorId() != null;
    }

    public AWEncodedString getDeferredBubbleIndicatorId ()
    {
        return _deferredBubbleIndicatorId;
    }

    private void initDeferredBubble ()
    {
        // Instead of highlighting the error by popping up the bubble as we
        // render the error indicator, we ask the error manager to do it when
        // append is done and we know which error manager is in the foreground.
        // Also, for the pages that recalculate validity during append, the
        // "current" error selection is not finalized until append is done.
        AWErrorInfo curError = errorManager().getHighLightedError();
        if (curError != null && curError.getIndicatorId() != null) {
            _deferredBubbleIndicatorId = curError.getIndicatorId();
            Log.aribaweb_errorManager.debug("set _deferredBubbleDisplay=%s",
                curError);
        }
        else {
            _deferredBubbleIndicatorId = null;
        }
    }

    /*-----------------------------------------------------------------------
        Bindings and actions for minimize/maximize
      -----------------------------------------------------------------------*/

    public String minButtonTooltip ()
    {
        return localizedJavaString(7, "Minimize this view" /*  */);
    }

    public String maxButtonTooltip ()
    {
        return localizedJavaString(6, "Maximize this view" /*  */);
    }

    public String minImage ()
    {
        return "pageErrorPanelMin.gif";
    }

    public String maxImage ()
    {
        return "pageErrorPanelMax.gif";
    }

    public String getName ()
    {
        return PageErrorPanelIsMinimizedKey;
    }

    public String getValue ()
    {
        return isMinimizedFormValue();
    }

    public void setValue (String newValue)
    {
        setIsMinimizedFormValue(newValue);
    }

    public Boolean isMinimized ()
    {
        Boolean minimized =
            (Boolean)session().dict().get(PageErrorPanelIsMinimizedKey);
        if (minimized == null) {
            session().dict().put(PageErrorPanelIsMinimizedKey,
                ariba.util.core.Constants.getBoolean(false));
            minimized =  (Boolean)session().dict().get(PageErrorPanelIsMinimizedKey);
        }
        return minimized;
    }

    public void setIsMinimized (boolean isMinimized)
    {
        session().dict().put(PageErrorPanelIsMinimizedKey,
            ariba.util.core.Constants.getBoolean(isMinimized));
    }

    public String isMinimizedFormValue ()
    {
        return isMinimized().toString();
    }

    public void setIsMinimizedFormValue (String formValue)
    {
        if (!StringUtil.nullOrEmptyOrBlankString(formValue)) {
            boolean isMinimized = BooleanFormatter.parseBoolean(formValue);
            setIsMinimized(isMinimized);
        }
    }

    public String minimizedViewDisplayAttr ()
    {
        return isMinimized().booleanValue() ? null : "display:none";
    }

    public String maximizedViewDisplayAttr ()
    {
        return isMinimized().booleanValue() ? "display:none" : null;
    }

    /*-----------------------------------------------------------------------
        Actions for cycling through the errors
      -----------------------------------------------------------------------*/

    public boolean allowPrevious ()
    {
        return _allowPrevious;
    }

    private void initAllowPrevious ()
    {
        _allowPrevious = errorManager().getNumberOfErrors() > 1;
    }

    public boolean allowNext ()
    {
        return _allowNext;
    }

    public void initAllowNext ()
    {
        AWErrorManager errMgr = errorManager();
        int numErrors = errMgr.getNumberOfErrors();
        _allowNext = numErrors > 1 || (numErrors == 1 && !errMgr.hasHighLightedError());
    }

    public boolean allowPreviousAndNext ()
    {
        return allowPrevious() && allowNext();
    }

    public boolean allowPreviousOrNext ()
    {
        return allowPrevious() || allowNext();
    }

    public AWComponent previousAction ()
    {
        return errorManager().prevError(pageComponent());
    }

    public AWComponent nextAction ()
    {
        AWErrorManager errorManager = errorManager();
        Log.aribaweb_errorManager.debug("nav to Next in errorManager %s", errorManager.getLogPrefix());
        return errorManager.nextError(pageComponent());
    }

    public boolean omitFormProxy ()
    {
        return (errorManager().getErrorNavSubmitForm() == null);
    }

    public boolean showWide ()
    {
        return !ModalWindowWrapper.isInModalWindow(this);
    }

}
