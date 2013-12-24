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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWErrorManager.java#56 $
*/

package ariba.ui.aribaweb.core;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import ariba.util.core.MapUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.Constants;
import ariba.util.core.MultiKeyHashtable;
import ariba.util.core.StringUtil;
import ariba.util.core.SparseVector;
import ariba.util.core.ClassUtil;
import ariba.util.core.MathUtil;
import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.ui.aribaweb.util.Log;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.html.BindingNames;

/**
 * Key Concepts:
 *  Repositories
 *  Error Keys
 *  Error Display
 *  Page Error Display
 *  Validation, Revalidation
 *  Frozen Repositories
 *  Navigation Handlers
 *  Validation Handlers
 *  Highlighted Errors
 *  Deferring
 *  Error Info
 *  Selected Error
 *  Autoscroll
 */
public class AWErrorManager extends AWBaseObject implements AWNavigation.Interceptor
{
    public static final String GeneralErrorKey = "general";
    public static final String InstanceKey = "AWErrorManagerKey";
    public static final String EnvironmentErrorKey = "scopeErrorKey";

    private static final String Separator = "--------";

    /** only used for writing errors; these are the errors accumulated during take */
    protected ErrorRepository _newRepository;

    /** Only used for reading errors; these are the errors for rendering the page */
    private ErrorRepository _currentRepository;

    private Object _mostRecentErrorKey;
    private boolean _enablePageErrorDisplay;
    private boolean _changesSavedSafely;
    private boolean _requiresRevalidation;

    /** keep track of whether we're frozen */
    private boolean _curRepositoryFrozen;

    private List<PrioritizedHandler> _prioritizedNavHandlers;
    private List<AWFullValidationHandler> _fullValidationHandlers;
    private Map<AWFullValidationHandler, AWFullValidationHandler>
        _invokedValidationHandlers;
    /**
        Whether or not we are current invoking the validation handlers.
    */
    private boolean _invokingValidationHandlers;
    private AWErrorInfo _curHighLightedError = null;
    private AWErrorInfo _prevHighLightedError = null;
    private EMMultiKeyHashtable<Object[], AWErrorInfo> _visitedErrors =
        new EMMultiKeyHashtable(AWErrorInfo.NumKeys);
    private AWPage _page;
    private Integer _pendingNavOffset = null;
    private AWComponent _deferredNavPage = null;
    private AWErrorHandler _deferredNavHandler = null;
    private AWErrorHandler _immediateNavHandler = null;
    private AWEncodedString _submitFormName = null;
    private boolean _curErrorIsFromPreviousPage = false;
    private boolean _rerunValidationInProgress = true;
    private boolean _validateInAppend;
    private boolean _ignoreUnknownWarningsMode = false;
    private boolean _selectedErrorNeedAdjustment = false;
    private boolean _enablePageAutoScroll = true;
    private boolean _tableAutoScrollInProgress;
    private boolean _disableErrorPanel;

    // constants indicate the phase we're in
    public static int OutOfPhase = 0;
    public static int RenderPhase = 1;
    public static int ApplyValuesPhase = 2;
    public static int InvokePhase = 3;

    private int _phase = OutOfPhase;

    /*
        The lifecycle of errors...
        The basic idea of what we're doing is that we want to have the set of known errors
        to be constant from append through invoke, because page structure depends on
        whether there are errors and the content of the error values.  But during take
        we want to be able to accumulate errors (usually parse errors).

        So, _currentErrors is used to track "errors to be used rendering the UI"
        and _newErrors is the new errors being accumulated.

        During a page lifetime cycle the errors will be like this:
        Append - current and new are the same objects
        Take - current is errors to draw ui, new are errors being accumulated
        Invoke - while finding the component to invoke the current errors are used to
        draw ui & new is different.  Once we find the component then we make current
        and new identical objects.
    */

    public AWErrorManager (AWPage page)
    {
        clearRegisteredErrorHandlers();
        clearRegisteredFullValidationHandlers();

        // the page this error manager is for
        _page = page;

        _enablePageErrorDisplay = false;

        _invokingValidationHandlers = false;

        // the very first phase we'll walk through is Append, so get the objects
        // initialized as they will be in all subsequent Appends:
        // with current and new being the same objects

        // fake takeActions operation
        freezeForNextCycle();
        // fake invoke operation
        _privateCalledBeforeActionFiring();

        // we're now in the normal RenderResponse state
    }

    // convert new errors to current errors.
    // should be called just before actions fire, so that errors are "current" for
    // actions to test them.  If no actions fire this method must be called
    // before append
    // this method may be called multiple times between action & append
    protected void _privateCalledBeforeActionFiring ()
    {
        // Before we swap, remember the order which errors were displayed.
        // This information will be used for error navigation.
        // Make sure we only do this once even if this method is called
        // more than once.
        if (_currentRepository != _newRepository) {
            if (isErrorDisplayEnabled()) {
                invokeValidationHandlers();
            }

            if (_currentRepository != null) {
                _currentRepository.rememberDisplayOrder();
                _newRepository.setDisplayedErrors(_currentRepository.getDisplayedErrors());
                _newRepository.setUndisplayedErrors(_currentRepository.getUndisplayedErrors());
                _newRepository.setHasUnnavigableError(_currentRepository.hasUnnavigableErrors());
                _newRepository.rememberTableErrors(_currentRepository);
            }
            else {
                _newRepository.setDisplayedErrors(MapUtil.<Integer, AWErrorBucket>map());
                _newRepository.setUndisplayedErrors(ListUtil.<AWErrorBucket>list());
            }
        }
        _currentRepository = _newRepository;
//        Log.aribaweb_errorManager.debug("%s: Point _currentRepository to _newRepository: %s",
//            getLogPrefix(), _currentRepository);

        _curRepositoryFrozen = false;
    }

    private void invokeValidationHandlers ()
    {
        // fire off full validation if handlers are registered
        for (int i = 0; i < _fullValidationHandlers.size(); i++) {
            AWFullValidationHandler handler = _fullValidationHandlers.get(i);
            invokeValidationHandler(handler);
        }
    }

    private void invokeValidationHandler (AWFullValidationHandler handler)
    {
        if (isErrorDisplayEnabled() &&
            _invokedValidationHandlers.get(handler) == null) {
            // This handler hasn't been invoke before - do it now
            try {
                Log.aribaweb_errorManager.debug(
                        "%s: Calling full-validation handler: %s", getLogPrefix(), handler);
                _invokingValidationHandlers = true;
                handler.evaluateValidity(this._page.pageComponent());
                _invokedValidationHandlers.put(handler, handler);
            }
            finally {
                _invokingValidationHandlers = false;
            }
        }
    }

    protected void freezeForNextCycle ()
    {
        _newRepository = new ErrorRepository(this);
//        Log.aribaweb_errorManager.debug(
//            "%s: Created _newRepository: %s for page %s", getLogPrefix(), _newRepository, _page.pageComponent());

        _curRepositoryFrozen = true;
        _pendingNavOffset = null;
        _curErrorIsFromPreviousPage = false;
        _rerunValidationInProgress = false;
        _tableAutoScrollInProgress = false;
        _invokedValidationHandlers = MapUtil.map();

        int errorSetId = (_currentRepository != null)
            ? _currentRepository.getErrorSetId() + 1 : 0;
        _newRepository.setErrorSetId(errorSetId);
    }

    public void setPhase (int phase)
    {
        if (phase == RenderPhase) {
            Log.aribaweb_errorManager.debug("%s %s: begin APPEND %s",
                Separator, getLogPrefix(), Separator);
            // navToErrorAsNeeded(false, null);  // defer nav further
            clearRegisteredErrorHandlers();
            clearRegisteredFullValidationHandlers();

            if (isValidationRequiredInAppend()) {
                rerunValidation();
                _rerunValidationInProgress = true;
            }
            _disableErrorPanel = false;
        }
        else if (phase == OutOfPhase && _phase == RenderPhase) { // exiting append
            if (getNumberOfErrors() == 0) {
                // all errors are clearend
                clearHighLightedError();
            }
            cleanupVisitedErrors();
            _selectedErrorNeedAdjustment = false;
        }
        else if (phase == ApplyValuesPhase) {
            Log.aribaweb_errorManager.debug("");
            Log.aribaweb_errorManager.debug("=========================================");
            Log.aribaweb_errorManager.debug("%s %s: begin TAKE %s",
                Separator, getLogPrefix(), Separator);
            // setup for take & invoke by freezing errors that are visible, but allow for new errors to be added.
            // errors added in this frozen state will become visible after call to promoteNewErrors.
            freezeForNextCycle();
        }
        else if (phase == OutOfPhase && _phase == ApplyValuesPhase) { // exiting take
            // Ideally, we should call promoteNewErrors here.  But FormValue queue
            // is not processed yet at this point.  It is processed inside
            // AWGenericElement.invokeAction() and must remain there.
        }
        else if (phase == InvokePhase) {
            Log.aribaweb_errorManager.debug("%s %s: begin INVOKE %s",
                Separator, getLogPrefix(), Separator);
            clearValidateInAppend();
        }
        else if (phase == OutOfPhase && _phase == InvokePhase) { // exiting invoke
            // make sure new errors have been promoted
            _privateCalledBeforeActionFiring();
        }

        _phase = phase;
    }

    public int phase ()
    {
        return _phase;
    }

    public void validateOnAppend ()
    {
        ariba.ui.aribaweb.util.Log.aribaweb_errorManager.debug(
            "%s setting validateInAppend for %s",
            getLogPrefix(), _page.pageComponent());
        _validateInAppend = true;
    }

    public boolean isValidationRequiredInAppend ()
    {
        return _validateInAppend;
    }

    private void clearValidateInAppend ()
    {
        // optimize by not doing validate in append, unless we decide otherwise
        _validateInAppend = false;
        ariba.ui.aribaweb.util.Log.aribaweb_errorManager.debug(
            "%s clearing validateInAppend for %s",
            getLogPrefix(), _page.pageComponent());
    }

    /*--------------------------------------------------------------------------
        Validation Methods
    --------------------------------------------------------------------------*/

    public void setErrorNavSubmitForm (AWRequestContext requestContext)
    {
        AWHtmlForm form = (requestContext != null) ? requestContext.currentForm() : null;
        _submitFormName = (form != null) ? _submitFormName = form.formName() : null;
        Log.aribaweb_errorManager.debug("Setting form proxy: %s", _submitFormName);
    }

    public AWEncodedString getErrorNavSubmitForm ()
    {
        if (_submitFormName == null) {
            String defaultFormId = _page.getDefaultFormId();
            if (defaultFormId != null) {
                _submitFormName = new AWEncodedString(defaultFormId);
            }
            if (_page.hasMultipleForms()) {
                Log.aribaweb_errorManager.debug(
                    "***** Warning: There are multiple forms but form proxy is not set for navigation actions.");
            }
        }
        Log.aribaweb_errorManager.debug("Using form: %s", _submitFormName);
        return _submitFormName;
    }

    public boolean isErrorDisplayEnabled ()
    {
        return _enablePageErrorDisplay;
    }

    public void enableErrorDisplay (boolean enable)
    {
        enableErrorDisplay(enable, true);
    }

    public void enableErrorDisplay (
        boolean enablePageErrorDisplay, boolean enablePageAutoScroll)
    {
        boolean callValidationHandler =
            (_enablePageErrorDisplay == false && enablePageErrorDisplay);
        _enablePageErrorDisplay = enablePageErrorDisplay;

        // call the validation handlers to carry out the deferred validation
        if (callValidationHandler) {
            Log.aribaweb_errorManager.debug(
                "%s: Invoke deferred validation evaluation", getLogPrefix());
            invokeValidationHandlers();

            // This generally takes place during invoke.  We have to catch up
            // with error nav too to give the nav handlers a chance to take us to another
            // page before append.
            navToErrorAsNeeded(false, null, false);
        }

        // we are turning off error display - reset submit form info
        if (!enablePageErrorDisplay) {
            _submitFormName = null;
        }
        else /*enablePageErrorDisplay*/ {
            setErrorNavSubmitForm(_page.requestContext());
            
            // make sure we auto scroll for the current error
            _tableAutoScrollInProgress = true;
            _enablePageAutoScroll = enablePageAutoScroll;
        }
    }

    public int getNumberOfErrors ()
    {
        return getNumberOfErrors(false);
    }

    /**
        Indicates whether error count should include the warnings that
        have been displayed before.  Let's say a page has only warnings.
        When we ask the question "Is the page valid?" before taking a submit
        action, the error count will not be zero because there are warnings.
        However, if ask again (and go through another append cycle), the
        exact same warnings are considered known warnings.  If desired, setting
        this flag will result in the error count to exclude the known
        warnings.  In that case, the answer to "Is the page valid?" will be Yes
        and the submit action will go through.  By default we don't ignore
        known warnings.

        @aribaapi private

        @param  ignore
    */
    public void setIgnoreKnownWarnings (boolean ignore)
    {
        _ignoreUnknownWarningsMode = ignore;
    }

    public boolean getIgnoreKnownWarnings ()
    {
        return _ignoreUnknownWarningsMode;
    }

    public int getNumberOfErrors (boolean excludeKnownWarnings)
    {
        // For warnings, we only warning the user once
        // and then let the user proceed.
        if (_ignoreUnknownWarningsMode && excludeKnownWarnings &&
            _currentRepository.warningsAreIdenticalToPreviousWarnings()) {
            Log.aribaweb_errorManager.debug("%s: Same warnings have been issued before - dont count as errors", getLogPrefix());
            return 0;
        }

        return _currentRepository.size();
    }

    public List<AWErrorInfo> getAllErrors ()
    {
        return getAllErrorsWithSeverity(null);
    }

    private List<AWErrorInfo> getAllErrorsWithSeverity (Boolean isWarning)
    {
        List<AWErrorBucket> orderedElements = _currentRepository.elements();
        List<AWErrorInfo> list = ListUtil.list();
        for (AWErrorBucket bucket : orderedElements) {
            for (AWErrorInfo error : bucket.getErrorInfos()) {
                if (isWarning == null || error.isWarning() == isWarning) {
                    list.add(error);
                }
            }
        }
        return list;
    }

    public boolean hasErrors ()
    {
        return _currentRepository.hasErrorsWithSeverity(Boolean.FALSE);
    }

    public List<AWErrorInfo> getAllWarnings ()
    {
        return getAllErrorsWithSeverity(Boolean.TRUE);
    }

    public boolean hasWarnings ()
    {
        return _currentRepository.hasErrorsWithSeverity(Boolean.TRUE);
    }

    public boolean allErrorsAreWarnings ()
    {
        return _currentRepository.allErrorsAreWarnings();
    }

    public void setErrorDisplayOrder (AWErrorInfo error, boolean isNavigable)
    {
        if (isNavigable) {
            _currentRepository.assignDisplayOrder(error);
        }
        else {
            _currentRepository.assignUnnavigableDisplayOrder(error);
        }
    }

    public boolean checkErrorsAndEnableDisplay ()
    {
        // First we need to enable error display
        // to make sure deferred validation are calculated
        enableErrorDisplay(true);

        // Then we turn it off if there are no errors or warnings.
        enableErrorDisplay(hasErrors() || hasWarnings());

        // let the caller know whether there are errors
        return isErrorDisplayEnabled();
    }

    public boolean hasNewError (Object key)
    {
        if (key == null) {
            return false;
        }
        if (_newRepository != null) {
            return _newRepository.get(key) != null;
        }
        else {
            return false;
        }
    }
    
    /*-------------------------------------------------------------------------------
     * Error getters
     --------------------------------------------------------------------------------*/

    /**
         Get the recently added error for this key.
         This method should NOT be used to get the errors while building the
         user interface, because it will violate phase boundaries.  Use
         errorMessageForKey instead.

         @param key The error key
         @return the error object if any
         @aribaapi private
     */
    public AWErrorInfo newErrorForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        AWErrorInfo error = (AWErrorInfo)_newRepository.get(key);
        return error;
    }

    /**
        Get the recently added error for this key.
        This method should NOT be used to get the errors while building the
        user interface, because it will violate phase boundaries.  Use
        errorMessageForKey instead.
        @param key The error key
        @return the error message if any, or null if there is no error
        @see #errorMessageForKey
        @aribaapi private
    */
    public String newErrorMessageForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        AWErrorInfo error = (AWErrorInfo)_newRepository.get(key);
        if (error != null) {
            return error.getMessage();
        }
        else {
            return null;
        }
    }

    /**
        Get the recently added error value for this key.
        This method should NOT be used to get the errors while building the
        user interface, because it will violate phase boundaries.  Use
        errorValueForKey instead.
        @param key The error key
        @return the error value if any, or null if there is no error
        @see #errantValueForKey
        @aribaapi private
    */
    public Object newErrorValueForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        // exact match lookup
        AWErrorInfo error = firstErrorForKeyExactMatch(_newRepository, key, Boolean.FALSE);
        return (error != null) ? error.getErrantValue() : null;
    }

    // exact-match lookup for single key, wildcard lookup for multi-keys
    public String errorMessageForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        AWErrorInfo error = firstErrorForKeyInternal(key, false);
        return (error != null) ? error.getMessage() : null;
    }

    // exact-match lookup for single key, wildcard lookup for multi-keys
    public String warningMessageForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        AWErrorInfo error = firstErrorForKeyInternal(key, true);
        return (error != null) ? error.getMessage() : null;
    }

    // exact-match lookup for single key, wildcard lookup for multi-keys
    public Object errantValueForKey (Object key)
    {
        if (key == null) {
            return null;
        }
        setMostRecentErrorKey(key);

        AWErrorInfo error = firstErrorForKeyInternal(key, false);
        return (error != null) ? error.getErrantValue() : null;
    }

    private AWErrorInfo firstErrorForKeyInternal (Object key, boolean isWarning)
    {
        if (key == null) {
            return null;
        }
        AWErrorInfo error = null;
        if (key instanceof Object[]) {
            Object[] keys = (Object[])key;
            error = firstErrorForKeys(keys, isWarning);
        }
        else {
            error = firstErrorForKeyExactMatch(
                _currentRepository, key, Constants.getBoolean(isWarning));
        }
        return error;
    }

    // exact match lookup
    private static AWErrorInfo firstErrorForKeyExactMatch (ErrorRepository repository,
                                                           Object key, Boolean isWarning)
    {
        if (key == null) {
            return null;
        }
        // this method does an exact match on the single key - no wildcarding
        AWErrorBucket bucket = repository.get(key);
        if (bucket != null) {
            // first error by default
            return bucket.getFirstError(isWarning);
        }
        return null;
    }

    // with wildcard lookup
    public List<AWErrorBucket> errorsForKeys (Object[] keys)
    {
        return errorsForValueSource(
            keys[AWErrorInfo.ValueSourceKeyIndex],
            (String)keys[AWErrorInfo.FieldPathKeyIndex],
            (String)keys[AWErrorInfo.GroupNameKeyIndex]);
    }

    // with wildcard lookup
    public String firstErrorMessageForKeys (Object[] keys)
    {
        if (keys == null) {
            return null;
        }
        return firstMessageForKeysInternal(keys, false);
    }

    // with wildcard lookup
    public String firstWarningMessageForKeys (Object[] keys)
    {
        if (keys == null) {
            return null;
        }
        return firstMessageForKeysInternal(keys, true);
    }

    // with wildcard lookup
    private String firstMessageForKeysInternal (Object[] keys, boolean isWarning)
    {
        AWErrorInfo error = firstErrorForKeys(keys, isWarning);
        return error == null ? null : error.getMessage();
    }

    // with wild-card lookup
    public AWErrorInfo firstErrorForKeys (Object[] keys, boolean isWarning)
    {
        if (keys == null) {
            return null;
        }
        List<AWErrorBucket> errors = errorsForKeys(keys);
        if (!ListUtil.nullOrEmptyList(errors)) {
            for (AWErrorBucket error : errors) {
                for (AWErrorInfo errorInfo : error.getErrorInfos()) {
                    if (errorInfo.isWarning() == isWarning) {
                        return errorInfo;
                    }
                }
            }
        }
        return null;
    }

    public List errorsForValueSource (Object valueSource)
    {
        List errors = _currentRepository.getErrorForKeyIndex(
            AWErrorInfo.ValueSourceKeyIndex, valueSource);
        return errors;
    }

    public List errorsForValueSource (Object valueSource, String fieldPath)
    {
        List errors = _currentRepository.getErrorForKeyIndex(
            AWErrorInfo.ValueSourceKeyIndex, valueSource,
            AWErrorInfo.FieldPathKeyIndex, fieldPath);
        return errors;
    }

    // with wildcard lookup
    public List<AWErrorBucket> errorsForValueSource (Object valueSource, String fieldPath, String group)
    {
        List<AWErrorBucket> errors = null;
        if (AWErrorInfo.NullKey.equals(fieldPath) && AWErrorInfo.NullKey.equals(group)) {
            errors = _currentRepository.getErrorForKeyIndex(
                AWErrorInfo.ValueSourceKeyIndex, valueSource);
        }
        else if (AWErrorInfo.NullKey.equals(group)) {
            errors = _currentRepository.getErrorForKeyIndex(
                AWErrorInfo.ValueSourceKeyIndex, valueSource,
                AWErrorInfo.FieldPathKeyIndex, fieldPath);
        }
        else {
            errors = _currentRepository.getErrorForKeyIndex(
                AWErrorInfo.ValueSourceKeyIndex, valueSource,
                AWErrorInfo.FieldPathKeyIndex, fieldPath,
                AWErrorInfo.GroupNameKeyIndex, group);
        }
        return errors;
    }

    // Simple version for use by components
    public static Object getErrorKeyForComponent (AWComponent comp)
    {
        Object errorKey = null;
        AWBinding keyBinding = comp.bindingForName(BindingNames.errorKey, true);
        if (keyBinding != null) {
            errorKey = comp.valueForBinding(keyBinding);
        }
        if (errorKey == null) {
            errorKey = comp.env().peek(EnvironmentErrorKey);
        }
        return errorKey;
    }

    public static Object[] getErrorKeyFromBindings (AWComponent comp)
    {
        Object[] errorKeys = getErrorKeyFromBindingsOnly(comp);

        if (errorKeys == null) {
            Object key = comp.env().peek(EnvironmentErrorKey);
            if (key != null) {
                errorKeys = (key instanceof Object[]) ? (Object[])key : AWErrorInfo.makeKeyArray(key);
            }
        }

        if (errorKeys == null) {
            Object recentKey = comp.errorManager().mostRecentErrorKey();
            if (recentKey != null) {
                errorKeys = AWErrorInfo.makeKeyArray(recentKey);
            }
        }

        if (errorKeys == null) {
            errorKeys = AWErrorInfo.makeKeyArray(AWErrorManager.GeneralErrorKey);
        }

        return errorKeys;
    }

    public static Object[] getErrorKeyFromBindingsOnly (AWComponent comp)
    {
        Object[] errorKeys = null;
        AWBinding keyBinding = comp.bindingForName(BindingNames.errorKey, true);
        if (keyBinding != null) {
            // Perf? should return single object in this case instead of instantiating an array
            errorKeys = AWErrorInfo.makeKeyArray(comp.valueForBinding(keyBinding));
        }
        else {
            AWBinding keysBinding = comp.bindingForName(BindingNames.errorKeys, true);
            if (keysBinding != null) {
                errorKeys = (Object[])comp.valueForBinding(keysBinding);
            }
            else {
                AWBinding vsBinding = comp.bindingForName(BindingNames.errorValueSource, true);
                if (vsBinding != null) {
                    Object valueSource = comp.valueForBinding(vsBinding);
                    AWBinding fieldBinding = comp.bindingForName(BindingNames.errorFieldPath, true);
                    Object field = (fieldBinding != null) ? comp.valueForBinding(fieldBinding) : null;
                    AWBinding groupBinding = comp.bindingForName(BindingNames.errorGroupName, true);
                    Object group = (groupBinding != null) ? comp.valueForBinding(groupBinding) : null;
                    errorKeys = AWErrorInfo.makeKeyArray(valueSource, field, group);
                }
            }
        }
        return errorKeys;
    }

    /*-------------------------------------------------------------------------------
     * End Error getters
     --------------------------------------------------------------------------------*/

    /**
     * Clear a single error, during append.
     * A call to this method is not allowed except during append
     * @param key - the error key that should be cleared
     * @aribaapi private
     */
    public void clearErrorInAppend (Object key)
    {
        Assert.that(!_curRepositoryFrozen,
            "clearErrorInAppend called outside of append phase");

        // Todo: should call this: _privateClearErrorForKey(key, _currentRepository)?
        _currentRepository.remove(key);
    }

    /**
     * This should only be called from AWErrorManager.AWNewErrorManager
     * @param error
     */
    protected void _privateSetErrorMessageAndValue (AWErrorInfo error)
    {
        error.setValidationError(_invokingValidationHandlers);
        // Allow more than one error per key, but de-dup on duplicates
        AWErrorBucket bucket = _newRepository.get(error.getKeys());
        if (bucket == null) {    
            // use a single-key bucket
            _newRepository.assignRegistryOrder(error);
            _newRepository.put(error.getKeys(), error);
            Log.aribaweb_errorManager.debug(
                "   recordValidationError: (single) %s",
                error.toString(Log.aribaweb_errorManager));

        }
        else  if (!bucket.isDuplicateError(error)) {
            // no duplicates
            AWErrorBucket newBucket = bucket.add(error);
            if (newBucket != null) {
                // replace the existing bucket with a new bucket
                _newRepository.put(error.getKeys(), newBucket);
            }
            Log.aribaweb_errorManager.debug(
                "   recordValidationError: (multi) %s",
                error.toString(Log.aribaweb_errorManager));
        }
        else {
            // duplicate error exists - update dup counter
            for (int i = 0; i < bucket.size(); i++) {
                AWErrorInfo existingErr = bucket.get(i);
                if (existingErr.isSameError(error)) {
                    // duplicate count is tracked on the error, not the bucket
                    existingErr.incrementDuplicateCount();
                    Log.aribaweb_errorManager.debug(
                        "   recordValidationError: de-dup on %s (dup count=%s)",
                        existingErr.toString(Log.aribaweb_errorManager),
                        Integer.toString(existingErr.getDuplicateCount()));
                    break;
                }
            }
        }
    }

    /**
     * This should only be called from AWErrorManager.AWNewErrorManager
     * @param error
     */
    protected void _privateSetWarningMessage (AWErrorInfo error)
    {
        // Todo: don't need this API anymore - consolidate
        _privateSetErrorMessageAndValue(error);
    }

    /**
     * This should only be called from AWErrorManager.AWNewErrorManager
     * @param key
     */
    protected void _privateClearErrorForKey (Object key, ErrorRepository repository)
    {
        // Note:           We don't have enough information to decrement the dup
        //                 count on the actual error when there are multiple errors
        //                 recorded for the same key.  For now, we just decrement
        //                 all errors in the bucket.  This should be OK since we
        //                 only expect one error and one warning at most.  We will have
        //                 to revisit when we have a use case for multiple errors.
        //                 Multiple keys should be used for that case anyway.
        AWErrorBucket bucket = repository.get(key);
        if (bucket != null && bucket.hasDuplicate()) {
            for (int i = 0; i < bucket.size(); i++) {
                AWErrorInfo existingErr = bucket.get(i);
                existingErr.decrementDuplicateCount();
            }
            Log.aribaweb_errorManager.debug(
                "   removeErrorForKey: decremented dup count for %s", bucket.get(0));
        }
        else if (bucket != null) {
            repository.remove(key);
            Log.aribaweb_errorManager.debug(
                "   removeErrorForKey: removed error %s", bucket.get(0));
        }
        else {
            Log.aribaweb_errorManager.debug(
                "   removeErrorForKey: error does not exist - skipped. %s", key);
        }
    }

    public Object mostRecentErrorKey ()
    {
        return _mostRecentErrorKey;
    }

    public void setMostRecentErrorKey (Object errorKey)
    {
        _mostRecentErrorKey = errorKey;
    }

    public void setChangesSavedSafely (boolean flag)
    {
        _changesSavedSafely = flag;
    }

    public boolean changesSavedSafely ()
    {
        return _changesSavedSafely;
    }

    /*-------------------------------------------------------------------------------
     * Error Handler Registeration
     --------------------------------------------------------------------------------*/

    public void registerErrorHandler (AWErrorHandler handler, int priority)
    {
        registerErrorHandler(handler, priority, false);
    }

    public void registerErrorHandler (
        AWErrorHandler handler, int priority, boolean isTableAutoScrollHandler)
    {
        PrioritizedHandler newHandler =
            new PrioritizedHandler(handler, priority, isTableAutoScrollHandler);

        // invoke now if invocation is pending
        if (isTableAutoScrollHandler && _tableAutoScrollInProgress) {
            if (_curHighLightedError != null) {
                lookupTableAssociationForError(_curHighLightedError);
                invokeTableAutoScrollHandler(_curHighLightedError, newHandler);
            }
        }

        // insert the new handler according to priority
        for (PrioritizedHandler cur : _prioritizedNavHandlers) {
            if (priority >= cur.priority) {
                // the list is sorted from highest to lowest priority
                ListUtil.insertElementBefore(_prioritizedNavHandlers, newHandler, cur);
                return;
            }
        }
        // If we get here, then the new handler has
        // lower priority than all existing ones.
        // Put it at the end of the list.
        _prioritizedNavHandlers.add(newHandler);
    }

    public void unregisterErrorHandler (PrioritizedHandler handler)
    {
        boolean removed =
            ListUtil.removeElementIdentical(_prioritizedNavHandlers, handler);
        if (!removed) {
            Log.aribaweb.warning(9527, handler);
        }
    }

    protected void clearRegisteredErrorHandlers ()
    {
        _prioritizedNavHandlers = ListUtil.list();
    }

    public void registerFullValidationHandler (AWFullValidationHandler handler)
    {
        _fullValidationHandlers.add(handler);
        if (_rerunValidationInProgress) {
            invokeValidationHandler(handler);
            navToErrorAsNeeded(true, null, true);
        }
    }

    public void unregisterFullValidationHandler (AWFullValidationHandler handler)
    {
        boolean removed = _fullValidationHandlers.remove(handler);
        if (!removed) {
            Log.aribaweb.warning(9570, handler);
        }
    }

    protected void clearRegisteredFullValidationHandlers ()
    {
        _fullValidationHandlers = ListUtil.list();
    }

    public List<AWFullValidationHandler> _getRegisteredValidationHandlers ()
    {
        // This method is intended for fieldsui sanity checking and should
        // not be used for any other purpose.
        return _fullValidationHandlers;
    }

    /*-------------------------------------------------------------------------------
     * End Error Handler Registeration
     --------------------------------------------------------------------------------*/

    public void rerunValidation ()
    {
        _currentRepository.clearValidationErrors();
        _invokedValidationHandlers.clear();
        Log.aribaweb_errorManager.debug("%s: all errors are cleared", getLogPrefix());
        if (_phase == RenderPhase) {
            // defer invocation until append when the handlers will be registered
            invokeValidationHandlers();
            navToErrorAsNeeded(false, null, true);
        }
    }

    /*--------------------------------------------------------------------------
       Error navigation
      --------------------------------------------------------------------------*/

    public AWErrorInfo getHighLightedError ()
    {
        return _curHighLightedError;
    }

    // not intended for public access - just for error mgr related classes
    public AWErrorInfo getPreviousError ()
    {
        return _prevHighLightedError;
    }

    public EMMultiKeyHashtable<Object[], AWErrorInfo> getVisitedErrors ()
    {
        return _visitedErrors;
    }

    public boolean isHighLightedError (Object key)
    {
        navToErrorAsNeeded(true, key, false);
        if (key instanceof Object[]) {
            return isHighLightedError((Object[])key);
        }
        return _curHighLightedError != null && _curHighLightedError.isSingleKey() &&
               _curHighLightedError.getKey().equals(key);
    }

    public boolean isHighLightedError (Object[] keys)
    {
        navToErrorAsNeeded(true, keys, false);
        return _curHighLightedError != null &&
               _curHighLightedError.keysEqualLoosely(keys);
    }

    public boolean hasHighLightedError ()
    {
        navToErrorAsNeeded(true, null, false);
        return _curHighLightedError != null;
    }

    public AWComponent getDeferredNavigationDestination ()
    {
        if (hasDeferredNavHandlerForCurrentError()) {
            if (_deferredNavPage == null) {
                navUsingDeferredNavHandler(_page.pageComponent());
            }
            else {
                return _deferredNavPage;
            }
        }
        return null;
    }

    public void clearHighLightedError ()
    {
        _curHighLightedError = null;
        _prevHighLightedError = null;
        _deferredNavHandler = null;
        _immediateNavHandler = null;
        _deferredNavPage = null;
        clearAllVisitedErrors();
    }

    /**
     * This is for AWDataTable scrolling?
     *
     * @param onlyInAppend
     * @param keysForPendingDisplayError
     * @param forceCheckExistence
     */
    public void navToErrorAsNeeded (boolean onlyInAppend,
                                    Object keysForPendingDisplayError,
                                    boolean forceCheckExistence)
    {
        // Consider this scenario: The current error has not been set
        // (because error display mode has been off or for
        // other reasons) and we are rendering the FIRST error indicator
        // that is not the first registered error.  During this rendering,
        // the highlighted error scope is determined by asking whether
        // the error being highlighted is the current error.  This method
        // will get called to determine the current error.  By default,
        // when there are no displayed errors, we will pick the first
        // registered error.  But if we know the error that is about to
        // be displayed, then we pick it as the current error instead.
        // Hence we pass in the keysForPendingDisplayError when it is known.

        if (onlyInAppend && _phase != RenderPhase) {
            return;
        }

        if (!isErrorDisplayEnabled() || getNumberOfErrors() == 0) {
            return;
        }

        // automatically go the first error if we are in error display mode
        boolean exists = doesCurHighLightedErrorExist();
        boolean ignoreExistence = isValidationRequiredInAppend() && !forceCheckExistence;
        boolean noneSelected = _curHighLightedError == null;
        boolean pending = _pendingNavOffset != null;
        if (noneSelected || pending || (!exists && !ignoreExistence)) {
            Log.aribaweb_errorManager.debug("%s: navToErrorAsNeeded: nav'ing because: %s",
                getLogPrefix(), getAutoNavReasonForLogging(noneSelected, pending, exists));
            AWComponent pageComponent = _page.pageComponent();
            int offset = (_pendingNavOffset != null) ? _pendingNavOffset.intValue() : 1;
            _pendingNavOffset = null;
            AWComponent retComp =
                navigateToError(offset, pageComponent, keysForPendingDisplayError);
            AWComponent navToPage = (retComp != null) ? retComp.pageComponent() : null;
            if (navToPage != null && pageComponent != null && navToPage != pageComponent) {
                // Nav handler wants to go to error immediate, but we might be in append
                // right now.  So we automatically put up a hyperlink in the floating
                // error panel.  App's action method can query for this information
                // and return the dstPage as well.
                _deferredNavHandler = _immediateNavHandler;
                _immediateNavHandler = null;
                _deferredNavPage = navToPage;
                Log.aribaweb_errorManager.debug(
                    "%s: auto nav leads to off-page - converted to deferred nav",
                    getLogPrefix());
            }
        }
        // Check to see if we have a better choice than what we have
        // selected.  We only make this adjustment if we know a nav
        // handler is going to navigate for the undisplayed error.
        // We cannot make this adjustment automatically as this behavior
        // will not work for all UI.
        else if (_curHighLightedError != null &&
                _currentRepository.hasSelectedUndisplayedError() &&
                (_immediateNavHandler != null || _selectedErrorNeedAdjustment) &&
                !_curErrorIsFromPreviousPage &&
                keysForPendingDisplayError != null)
        {
            AWErrorBucket bucket = _currentRepository.get(keysForPendingDisplayError);
            if (bucket != null) {
                // this should be the first displayed error
                // since we only do this check once per cycle
                AWErrorInfo error = bucket.getFirstError(null);
                if (_visitedErrors.getValueForKeys(error.getKeys()) == null) {
                    // this seems like a better choice
                    Log.aribaweb_errorManager.debug(
                        "%s: navToErrorAsNeeded: better choice found - going to it: %s",
                        getLogPrefix(), error);
                    removeVisitedError(_curHighLightedError);
                    selectCurError(error);
                    invokeNavHandlers(error, _page.pageComponent());
                }
                _currentRepository.setHasSelectedUndisplayedError(false);
            }
        }

    }

    private void selectCurError (AWErrorInfo error)
    {
        _curHighLightedError = error;
        _deferredNavHandler = null;
        _immediateNavHandler = null;
        _deferredNavPage = null;
        addVisitedError(_curHighLightedError);
        lookupTableAssociationForError(error);
    }

    private void lookupTableAssociationForError (AWErrorInfo error)
    {
        if (error != null && error.getAssociatedTableItem() == null) {
            AWErrorInfo assoc = _currentRepository.getTableAssociation(error);
            if (assoc != null) {
                error.setAssociatedTableItem(
                    assoc.getAssociatedDataTable(), assoc.getAssociatedTableItem());
                Log.aribaweb_errorManager.debug(
                    "***** fill in missing table item association for error: %s", error);
            }
        }
    }

    private AWComponent invokeTableAutoScrollHandler (
        AWErrorInfo error, PrioritizedHandler pHandler)
    {
        if (pHandler.isTableAutoScrollHandler) {
            lookupTableAssociationForError(_curHighLightedError);
            if (pHandler.handler.canGoToErrorImmediately(error, _page.pageComponent())) {
                AWComponent dstPage =
                    pHandler.handler.goToError(error, _page.pageComponent());
                if (dstPage != null) {
                    _tableAutoScrollInProgress = false;
                    error.setWasTableAutoScrolled(true);
                    return dstPage;
                }
            }
        }
        return null;
    }

    private String getAutoNavReasonForLogging (boolean noneSelected,
                                               boolean pending,
                                               boolean exists)
    {
        if (noneSelected) {
            return "auto nav to 1st";
        }
        else if (pending) {
            return "was deferred";
        }
        else if (!exists) {
            return "cur error no long exists";
        }
        return "unknown";
    }

    private boolean doesCurHighLightedErrorExist ()
    {
        if (_curHighLightedError != null) {
            List orderedElements = _currentRepository.elements();
            List list = ListUtil.list();
            for (int i = 0; i < orderedElements.size(); i++) {
                AWErrorBucket bucket = (AWErrorBucket)orderedElements.get(i);
                if (bucket.keysEqual(_curHighLightedError.getKeys())) {
                    // make sure the current error is an instance of the registered errors
                    // and not from a previous cycle so we pickup the up-to-date attributes
                    // about the error, like whether it is displayed on the page.
                    // We should only do this after all errors have been recorded and
                    // error nav is done.
                    if (bucket.size() == 1) {
                        if (_curHighLightedError != bucket.get(0)) {
                            _curHighLightedError = bucket.get(0);
                            lookupTableAssociationForError(_curHighLightedError);
                        }
                    }
                    else {
                        for (int j = 0; j < bucket.size(); j++) {
                            AWErrorInfo error = bucket.get(j);
                            if (_curHighLightedError == error) {
                                // compare reference
                                break;
                            }
                            else {
                                // compare error msg
                                if (!StringUtil.nullOrEmptyString(error.getMessage()) &&
                                    error.getMessage().equals(_curHighLightedError)) {
                                    _curHighLightedError = bucket.get(0);
                                    lookupTableAssociationForError(_curHighLightedError);
                                    break;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        else {
            return true;
        }
    }

    public AWComponent nextError (AWComponent pageComponent)
    {
        // In the event that invoke was skipped, make sure new errors
        // have been promoted and validation handlers have been invoked
        pushErrorMgrAndPromoteNewErrors();

        // enable scrolling to the error
        _enablePageAutoScroll = true;
        return navigateToError(1, pageComponent, null);
    }

    private void pushErrorMgrAndPromoteNewErrors ()
    {
        AWErrorManager curErrorManager = _page.errorManager();
        Object oldErrorManager = (curErrorManager != this)
                ? _page.pushErrorManager(this)
                : null;
        _privateCalledBeforeActionFiring();
        if (oldErrorManager != null) {
            _page.popErrorManager(oldErrorManager);
        }
    }

    public AWComponent prevError (AWComponent pageComponent)
    {
        // In the event that invoke was skipped, make sure new errors
        // have been promoted and validation handlers have been invoked
        pushErrorMgrAndPromoteNewErrors();

        // enable scrolling to the error
        _enablePageAutoScroll = true;
        return navigateToError(-1, pageComponent, null);
    }

    private AWComponent navigateToError (int offset,
                                         AWComponent pageComponent,
                                         Object keysForPendingDisplayedError)
    {
        Log.aribaweb_errorManager.debug("     %s %s: BEGIN navigateToError %s%s%s",
            Separator, getLogPrefix(), Separator, Separator, Separator);
        logNavigationContext();

        // Navigation action is in invoke.  If there are no errors during invoke,
        // then we should defer navigation until append, because components might
        // register more errors during append.
        if (getNumberOfErrors() == 0) {
            if (_pendingNavOffset == null) {
                Log.aribaweb_errorManager.debug(
                    "%s: %s - defer and check again during append",
                    getLogPrefix(), (getNumberOfErrors() == 0) ? "No errors" : "No displayed errors");
                _pendingNavOffset = Constants.getInteger(offset);
            }
            return null;
        }

        // identify the error to navigate to.
        AWErrorInfo dstError = getErrorRelativeToCurrent(
                offset, _curHighLightedError, _prevHighLightedError,
                keysForPendingDisplayedError);

        Log.aribaweb_errorManager.debug("%s: Navigating to error: %s", getLogPrefix(), dstError);
        _prevHighLightedError = _curHighLightedError;
        selectCurError(dstError);

        Log.aribaweb_errorManager.debug("     %s %s: END navigateToError %s%s%s",
           Separator, getLogPrefix(), Separator, Separator, Separator);

        if (dstError != null) {
            return invokeNavHandlers(dstError, pageComponent);
        }

        // There are no handlers - do the default highlighting on the cur page
        return null;
    }

    public boolean getEnablePageAutoScroll ()
    {
        return _enablePageAutoScroll;
    }

    public void enablePageAutoScrolling ()
    {
        _enablePageAutoScroll = true;
    }

    public void disableErrorPanel ()
    {
        _disableErrorPanel = true;
    }

    public boolean errorPanelDisabled ()
    {
        return _disableErrorPanel;
    }
    
    private AWComponent invokeNavHandlers (AWErrorInfo dstError,
                                           AWComponent pageComponent)
    {
        // find a handler to handle it
        for (PrioritizedHandler pHandler : _prioritizedNavHandlers) {
            if (pHandler.isTableAutoScrollHandler) {
                invokeTableAutoScrollHandler(dstError, pHandler);
            }
            else {
                if (pHandler.handler.canGoToErrorImmediately(dstError, pageComponent)) {
                    AWComponent dstPage =
                        pHandler.handler.goToError(dstError, pageComponent);
                    if (dstPage == null) {
                        // this handler cannot handle this error after all - try another handler
                        continue;
                    }
                    if (dstPage != null) {
                        Log.aribaweb_errorManager.debug(
                            "%s: Nav handler %s returned page %s",
                            getLogPrefix(), pHandler.handler, dstPage);
                        _deferredNavHandler = null;
                        _deferredNavPage = null;
                        _immediateNavHandler = pHandler.handler;
                        if (pageComponent != dstPage) {
                            setupOffPageNavigationAtSrc(pageComponent, dstPage, _immediateNavHandler);
                        }
                        return dstPage;
                    }
                }
                else if (pHandler.handler.canGoToErrorWithLink(dstError, pageComponent)) {
                    _deferredNavHandler = pHandler.handler;
                    _deferredNavPage = null;
                    _immediateNavHandler = null;
                    return null;
                }
            }
        }

        // There are no handlers - do the default highlighting on the cur page
        _deferredNavHandler = null;
        _deferredNavPage = null;
        _immediateNavHandler = null;
        return null;
    }

    private void setupOffPageNavigationAtSrc (AWComponent pageComponent,
                                               AWComponent dstPage,
                                              AWErrorHandler navHandler)
    {
        if (pageComponent == dstPage) {
            return;
        }
        AWErrorManager dstErrorMgr = dstPage.errorManager();
        dstErrorMgr._setupOffPageNavigationAtDst(getHighLightedError(), navHandler);
    }

    public void _setupOffPageNavigationAtDst (AWErrorInfo curError, AWErrorHandler navHandler)
    {
        // carry the current error forward
        selectCurError(curError);

        // carry error mode forward
        enableErrorDisplay(true);

        // remember that fact that we are naviging off-page
        // so adjustments can be done
        _immediateNavHandler = navHandler;
    }

    private void addVisitedError (AWErrorInfo error)
    {
        // We need to maintain the order errors are visited.  So we
        // need to remove an earlier visit, if exists, before adding.
        AWErrorInfo existing = _visitedErrors.getValueForKeys(error.getKeys());
        if (existing != null) {
            _visitedErrors.remove(existing.getKeys());
        }
        _visitedErrors.put(error.getKeys(), error);
    }

    private void removeVisitedError (AWErrorInfo error)
    {
        _visitedErrors.remove(error.getKeys());
    }

    private void clearAllVisitedErrors ()
    {
        if (_visitedErrors.size() != 0) {
            _visitedErrors = 
                new EMMultiKeyHashtable<Object[], AWErrorInfo>(AWErrorInfo.NumKeys);
        }
    }

    private void cleanupVisitedErrors ()
    {
        // remove the errors that no longer exist
        List<AWErrorInfo> visitedList = _visitedErrors.elementsVector();
        for (int i = visitedList.size() - 1; i >= 0; i--) {
            AWErrorInfo visited = visitedList.get(i);
            if (_currentRepository.get(visited.getKeys()) == null) {
                _visitedErrors.remove(visited.getKeys());
                Log.aribaweb_errorManager.debug(
                    "%s: cleanupVisitedErrors: removed non-existing: %s",
                    getLogPrefix(), visited);
            }
        }

        // If we have visited all errors, clear the list.
        if (visitedList.size() != 0 &&
            visitedList.size() >= _currentRepository.elements().size()) {
            // all errors have been visited, clear the list
            clearAllVisitedErrors();
            Log.aribaweb_errorManager.debug(
                "%s: cleanupVisitedErrors: all errors have been visited - resetting",
                getLogPrefix());
        }
    }

    public boolean hasDeferredNavHandlerForCurrentError ()
    {
        return _curHighLightedError != null && _deferredNavHandler != null;
    }

    public AWComponent navUsingDeferredNavHandler (AWComponent pageComponent)
    {
        if (hasDeferredNavHandlerForCurrentError()) {
            AWComponent dstPage =
                _deferredNavHandler.goToError(_curHighLightedError, pageComponent);
            setupOffPageNavigationAtSrc(pageComponent, dstPage, _deferredNavHandler);
            return dstPage;
        }
        return null;
    }

    private String _logPrefix = null;

    public String getLogPrefix ()
    {
        if (_logPrefix == null) {
            if (Log.aribaweb_errorManager.isDebugEnabled()) {
                // This log prefix helps us identify the page and the nested error
                // managers.  We need to page name because the portlet content
                // components are all pages, and there are a lot of them.
                // So we need to be able to tell them apart.
                String hashCode = Integer.toString(System.identityHashCode(this));
                if (_page.pageComponent() != null &&
                    _page.pageComponent().componentDefinition() != null &&
                    _page.pageComponent().componentDefinition().componentName() != null) {
                    _logPrefix = ClassUtil.stripPackageFromClassName(
                        _page.pageComponent().componentDefinition().componentName())
                        + hashCode;
                }
                else if (_page.pageComponent() != null) {
                    _logPrefix = _page.pageComponent().toString() + hashCode;
                }
                else {
                    _logPrefix = hashCode;
                }
            }
            else {
                _logPrefix = "";
            }
        }
        return _logPrefix;
    }

    private void logNavigationContext ()
    {
        if (Log.aribaweb_errorManager.isDebugEnabled()) {
            Log.aribaweb_errorManager.debug("%s: Recently visited errors:", getLogPrefix());

            List<AWErrorInfo> visitedList = _visitedErrors.elementsVector();
            for (AWErrorInfo error : visitedList) {
                Log.aribaweb_errorManager.debug("   %s", error);
            }

            Log.aribaweb_errorManager.debug(
                "%s: current=%s prev=%s", getLogPrefix(), _curHighLightedError, _prevHighLightedError);
            Log.aribaweb_errorManager.debug("%s: In the prev cycle: displayed errors:", getLogPrefix());
            Iterator<AWErrorBucket> values =
                _currentRepository._orderedDisplayedErrors.values().iterator();
            while (values.hasNext()) {
                AWErrorBucket bucket = values.next();
                for (int e = 0; e < bucket.size(); e++) {
                    Log.aribaweb_errorManager.debug("   %s", bucket.get(e));
                }
            }
            Log.aribaweb_errorManager.debug("%s: In the prev cycle: undisplayed errors:", getLogPrefix());
            for (int i = 0; i < _currentRepository._orderedUndisplayedErrors.size(); i++) {
                AWErrorBucket bucket = 
                    _currentRepository._orderedUndisplayedErrors.get(i);
                for (int e = 0; e < bucket.size(); e++) {
                    Log.aribaweb_errorManager.debug("   %s", bucket.get(e));
                }
            }
            Log.aribaweb_errorManager.debug("%s: In the current cycle: errors:", getLogPrefix());
            List allErrors = _currentRepository.elements();
            for (int i = 0; i < allErrors.size(); i++) {
                AWErrorBucket bucket = (AWErrorBucket)allErrors.get(i);
                for (int e = 0; e < bucket.size(); e++) {
                    Log.aribaweb_errorManager.debug(
                        "   %s: %s", Integer.toString(i), bucket.get(e));
                }
            }
        }
    }

    private AWErrorInfo getErrorRelativeToCurrent (int offset,
                                                   AWErrorInfo current,
                                                   AWErrorInfo previous,
                                                   Object keysForPendingDisplayedError)
    {
        if (_currentRepository.size() == 0) {
            return null;
        }

        int index = 0;
        if (current != null) {
            index = _currentRepository.getErrorIndex(current, offset);
            Log.aribaweb_errorManager.debug(
                "%s: Looking for %s relative to current error: %s",
                getLogPrefix(),
                (offset == 1) ? "Next" : "Prev",
                (index != -1) ?  "found at index " + Integer.toString(index) : "Not Found");
            if (index == -1 && previous != null) {
                index = _currentRepository.getErrorIndex(previous, offset);
                Log.aribaweb_errorManager.debug(
                    "%s: Looking for %s relative to previous error: %s",
                    getLogPrefix(),
                    (offset == 1) ? "Next" : "Prev",
                    (index != -1) ?  "found at index " + Integer.toString(index) : "Not Found");
            }
        }
        else if (previous != null) {
            index = _currentRepository.getErrorIndex(previous, offset);
            Log.aribaweb_errorManager.debug(
                "%s: Looking for %s relative to previous error: %s",
                getLogPrefix(),
                (offset == 1) ? "Next" : "Prev",
                (index != -1) ?  "found at index " + Integer.toString(index) : "Not Found");
        }
        else {
            if ((index = getFirstDisplayedErrorIndex()) != -1) {
                // go to the first displayed error in the current cycle
                Log.aribaweb_errorManager.debug(
                    "%s: No cur/prev error - go to the first displayed error in current cycle - %s",
                    getLogPrefix(),
                    Integer.toString(index));
            }
            else if (keysForPendingDisplayedError != null &&
                     (index = _currentRepository.getRegistrationOrder(keysForPendingDisplayedError)) != -1) {
                // We know that we are about to display an error
                index = _currentRepository.getRegistrationOrder(keysForPendingDisplayedError);
                Log.aribaweb_errorManager.debug(
                    "%s: No cur/prev error - but we are about to display error at index %s - go to it",
                    getLogPrefix(),
                    Integer.toString(index));
            }
            else if (!_currentRepository.getDisplayedErrors().isEmpty()) {
                // go to the first displayed error in the previous cycle
                AWErrorBucket oldError = _currentRepository.getDisplayedError(0);
                index = _currentRepository.getRegistrationOrder(oldError.getKeys());
                Log.aribaweb_errorManager.debug(
                    "%s: No cur/prev error - try first displayed error in prev cycle - %s",
                    getLogPrefix(),
                    (index == -1) ? "none found" : "found at index " + Integer.toString(index));
            }
            else {
                // We have no current error and no displayed - completely contextless.
                // Give the nav handler a say about which error we should nav to.
                AWErrorInfo error = letNavHandlersSelectNextError();
                if (error != null) {
                    index = _currentRepository.getRegistrationOrder(error.getKeys());
                    if (index != -1 && error.getDisplayOrder() == AWErrorInfo.NotDisplayed) {
                        // make notes of the fact that the nav handler picked an undisplayed error
                        _currentRepository.setHasSelectedUndisplayedError(true);
                    }
                }
                if (index == -1) {
                    // last resort: go to the first registered error
                    index = 0;
                    _currentRepository.setHasSelectedUndisplayedError(true);
                    _selectedErrorNeedAdjustment = true;
                    Log.aribaweb_errorManager.debug(
                        "%s: No cur/prev error - go to first registered error", getLogPrefix());
                }
            }
        }

        if (index == -1) {
            index = 0;
        }

        AWErrorInfo error = null;
        List allErrors = _currentRepository.elements();
        if (index < allErrors.size()) {
            AWErrorBucket bucket = (AWErrorBucket)allErrors.get(index);
            error = bucket.getFirstError(null);
        }
        Log.aribaweb_errorManager.debug("%s: getErrorRelativeToCurrent: %s=%s",
            getLogPrefix(),
            (offset == 1) ? "NEXT" : "PREV", error);

        return error;
    }

    private AWErrorInfo letNavHandlersSelectNextError ()
    {
        AWErrorInfo nextError = null;
        for (PrioritizedHandler pHandler : _prioritizedNavHandlers) {
            nextError = pHandler.handler.selectFirstError(getAllErrors());
            if (nextError != null) {
                Log.aribaweb_errorManager.debug(
                    "%s: Nav handler %s selects error: %s",
                    getLogPrefix(), pHandler.handler, nextError);
                break;
            }
        }
        return nextError;
    }

    private int getFirstDisplayedErrorIndex ()
    {
        List<AWErrorBucket> allErrors = _currentRepository.elements();
        for (int i = 0; i < allErrors.size(); i++) {
            AWErrorBucket bucket = allErrors.get(i);
            if (bucket.getDisplayOrder() != AWErrorInfo.NotDisplayed) {
                return i;
            }
        }
        return -1;
    }

    public void setAssociatedTableItem (Object[] errorKeys,
                                        AWComponent datatable,
                                        Object tableItem)
    {
        _currentRepository.setAssociatedTableItem(errorKeys, datatable, tableItem);
    }

    public int getErrorSetId ()
    {
        return _newRepository.getErrorSetId();
    }

    /*--------------------------------------------------------------------------
        AWComponent Overrides
    --------------------------------------------------------------------------*/

    public AWComponent alternateResponseForNavigationAction (AWComponent target,
                                                             int action)
    {
        // don't allow hide or leave if we have errors
        return ((action != AWNavigation.Drill) && checkErrorsAndEnableDisplay())
            ? target.pageComponent()
            : null;
    }

    protected void _privateSetRequiresRevalidation (boolean flag)
    {
        _requiresRevalidation = flag;
    }

    /**
     * @return whether a set has resulted in the need to re-validate
     */
    public boolean requiresRevalidation ()
    {
        return _requiresRevalidation;
    }

    /**
     * This class is here to restrict access to the AWErrorManager so that the only interaction
     * we do with it is to call setErrorMessageAndValue(...).
     * If this is too restrictive, then we can eliminate this, but I wanted to avoid the
     * problem of developers getting confused between the current and new errorManagers.
     * The new errorManager is for record errors while the errorManager is for everything else.
     * We swizzle the new -> current just before we invokeAction in AWGenericElement.
     */
    public static class AWNewErrorManager extends AWErrorManager
    {
        public AWNewErrorManager (AWPage page)
        {
            super(page);
        }

        protected void setErrorMessageAndValue (AWErrorInfo error)
        {
            _privateSetErrorMessageAndValue(error);
        }

        protected void clearErrorForKey (Object key)
        {
            _privateClearErrorForKey(key, _newRepository);
        }

        protected void setRequiresRevalidation (boolean flag)
        {
            _privateSetRequiresRevalidation(flag);
        }

        protected void promoteNewErrors ()
        {
            _privateCalledBeforeActionFiring();
        }
    }

    /**
     * This holds several errors.
     */
    public static class MultiErrorBucket implements AWErrorBucket
    {
        private List<AWErrorInfo> _errorsForSameKey;
        private boolean _hasDuplicate = false;

        public MultiErrorBucket (AWErrorInfo error)
        {
            _errorsForSameKey = ListUtil.list();
            _errorsForSameKey.add(error);
        }

        private AWErrorInfo getFirstError ()
        {
            return ListUtil.firstElement(_errorsForSameKey);
        }

        public AWErrorInfo getFirstError (Boolean isWarning)
        {
            AWErrorInfo firstError = getFirstError();
            for (int i = 0; isWarning != null && i < _errorsForSameKey.size(); i++) {
                // match on severity too if specified
                AWErrorInfo error = _errorsForSameKey.get(i);
                if (error.isWarning() == isWarning.booleanValue()) {
                    firstError = error;
                    break;
                }
            }
            return firstError;
        }

        public Object getKey ()
        {
            return getFirstError().getKey();
        }

        public Object[] getKeys ()
        {
            return getFirstError().getKeys();
        }

        public boolean keysEqual (Object[] theirKeys)
        {
            return getFirstError().keysEqual(theirKeys);
        }

        public int getDisplayOrder ()
        {
            return getFirstError().getDisplayOrder();
        }

        public int getUnnavigableDisplayOrder ()
        {
            return getFirstError().getUnnavigableDisplayOrder();
        }

        public int getRegistrationOrder ()
        {
            return getFirstError().getRegistrationOrder();
        }

        public void setRegistrationOrder (int order)
        {
            for (AWErrorInfo info : _errorsForSameKey) {
                info.setRegistrationOrder(order);
            }
        }

        public boolean isSingleErrorBucket ()
        {
            return false;
        }

        public AWErrorBucket add (AWErrorInfo error)
        {
            // copy registration order
            int existingRegOrder = getFirstError().getRegistrationOrder();
            error.setRegistrationOrder(existingRegOrder);

            _errorsForSameKey.add(error);

            return null;
        }

        public boolean isDuplicateError (AWErrorInfo error)
        {
            for (int i = 0; i < _errorsForSameKey.size(); i++) {
                AWErrorInfo existing = (AWErrorInfo)_errorsForSameKey.get(i);
                if (existing.isSameError(error)) {
                    // same error if the error messages match
                    Log.aribaweb_errorManager.debug("Duplicate error: %s",
                        error.toString(Log.aribaweb_errorManager));
                    return true;
                }
            }
            return false;
        }

        public boolean hasErrorsWithSeverity (Boolean isWarning)
        {
            for (int i = 0; i < _errorsForSameKey.size(); i++) {
                AWErrorInfo error = (AWErrorInfo)_errorsForSameKey.get(i);
                if (isWarning == null ||
                    error.isWarning() == isWarning.booleanValue()) {
                    return true;
                }
            }
            return false;
        }

        public AWErrorInfo get (int i)
        {
            return (AWErrorInfo)_errorsForSameKey.get(i);
        }

        public int size ()
        {
            return _errorsForSameKey.size();
        }

        public List<AWErrorInfo> getErrorInfos ()
        {
            return _errorsForSameKey;
        }

        public boolean hasDuplicate ()
        {
            for (int i = 0; i < _errorsForSameKey.size(); i++) {
                AWErrorInfo error = (AWErrorInfo)_errorsForSameKey.get(i);
                if (error.hasDuplicate()) {
                    return true;
                }
            }
            return false;
        }

        public AWComponent getAssociatedDataTable ()
        {
            return getFirstError().getAssociatedDataTable();
        }

        public Object getAssociatedTableItem ()
        {
            return getFirstError().getAssociatedTableItem();
        }

        public void setAssociatedTableItem (AWComponent table, Object item)
        {
            for (int i = 0; i < _errorsForSameKey.size(); i++) {
                AWErrorInfo error = _errorsForSameKey.get(i);
                error.setAssociatedTableItem(table, item);
            }
        }

        public List<AWErrorInfo> getErrorInfos (Boolean validationErrors)
        {
            if (validationErrors == null) {
                return getErrorInfos();
            }
            List<AWErrorInfo> result = null;
            for (AWErrorInfo error : _errorsForSameKey) {
                if (validationErrors == error.isValidationError()) {
                    if (result == null) {
                        result = ListUtil.list();
                    }
                    result.add(error);
                }
            }
            return result != null ? result : Collections.<AWErrorInfo>emptyList();
        }
    }

    /**
     * This holds some errors (AWErrorBuckets) and provides some methods that act on them.
     * An error repository is owned by an error manager.
     */
    private static class ErrorRepository
    {
        private EMMultiKeyHashtable<Object[],AWErrorBucket> _errors;
        private int _displayOrderCounter = 0;
        private int _unnavigableDisplayOrderCounter = 0;
        private boolean _hasUnnavigableErrors = false;
        private int _registrationOrderCounter = 0;
        private Map<Integer, AWErrorBucket> _orderedDisplayedErrors;
        private List<AWErrorBucket> _orderedUndisplayedErrors;
        private AWErrorManager _errorManager;
        private boolean _selectedUndisplayError = false;

        // Assign an id to each set of errors.  Two errors are the same only
        // if they have the same keys and same set id.  This member var tracks
        // the set id for the current set of errors.
        private int _errorSetId = 0;

        // information that indicates whether an error flag is
        // rendered inside a table.  We need this information to
        // auto-scroll the table to the error spot.
        private EMMultiKeyHashtable<Object[], AWErrorInfo> _tableAssociation =
            new EMMultiKeyHashtable(AWErrorInfo.NumKeys);

        // We also keep information from the last append so this
        // information is available before the current append.
        private EMMultiKeyHashtable<Object[], AWErrorInfo> _previousTableAssociation =
            null;

        public ErrorRepository (AWErrorManager errorManager)
        {
            _errors = 
                new EMMultiKeyHashtable<Object[],AWErrorBucket>(AWErrorInfo.NumKeys);
            _errorManager = errorManager;
        }

        public int size ()
        {
            return _errors.size();
        }

        public List<AWErrorBucket> elements ()
        {
            return _errors.elementsVector();
        }

        public boolean hasErrorsWithSeverity (Boolean isWarning)
        {
            List errors = elements();
            for (int i = 0; i < errors.size(); i++) {
                AWErrorBucket bucket = (AWErrorBucket)errors.get(i);
                if (bucket.hasErrorsWithSeverity(isWarning)) {
                    return true;
                }
            }
            return false;
        }

        public AWErrorBucket get (Object key)
        {
            Object[] keys;
            if (key instanceof Object[]) {
                keys = (Object[])key;
            }
            else {
                keys = AWErrorInfo.makeKeyArray(key);
            }
            return _errors.getValueForKeys(keys);
        }

        public AWErrorBucket get (Object[] keys)
        {
            return _errors.getValueForKeys(keys);
        }

        public List<AWErrorBucket> getErrorForKeyIndex (int keyIndex, Object key)
        {
            List<AWErrorBucket> matches = ListUtil.list();
            List<AWErrorBucket> errors = _errors.elementsVector();
            for (AWErrorBucket bucket : errors) {
                if (bucket.getKeys()[keyIndex].equals(key)) {
                    matches.add(bucket);
                }
            }
            return matches;
        }

        public List<AWErrorBucket> getErrorForKeyIndex (int keyIndex1, Object key1,
                                                           int keyIndex2, Object key2)
        {
            List<AWErrorBucket> matches = ListUtil.list();
            List<AWErrorBucket> errors = _errors.elementsVector();
            for (AWErrorBucket bucket : errors) {
                if (bucket.getKeys()[keyIndex1].equals(key1) &&
                    bucket.getKeys()[keyIndex2].equals(key2)) {
                    matches.add(bucket);
                }
            }
            return matches;
        }

        public List<AWErrorBucket> getErrorForKeyIndex (int keyIndex1, Object key1,
                                                           int keyIndex2, Object key2,
                                                           int keyIndex3, Object key3)
        {
            List<AWErrorBucket> matches = ListUtil.list();
            List<AWErrorBucket> errors = _errors.elementsVector();
            for (AWErrorBucket bucket : errors) {
                if (bucket.getKeys()[keyIndex1].equals(key1) &&
                    bucket.getKeys()[keyIndex2].equals(key2) &&
                    bucket.getKeys()[keyIndex3].equals(key3)) {
                    matches.add(bucket);
                }
            }
            return matches;
        }

        public AWErrorBucket remove (Object key)
        {
            Object[] keys =  (key instanceof Object[])
                ? (Object[])key : AWErrorInfo.makeKeyArray(key);
            return _errors.remove(keys);
        }

        public AWErrorBucket remove (Object[] keys)
        {
            return _errors.remove(keys);
        }

        public Object put (Object[] keys, AWErrorBucket errors)
        {
            AWErrorBucket old = _errors.put(keys, errors);
            if (old instanceof AWErrorBucket) {
                return old;
            }
            else if (old != null) {
                // Note: The RemovedObject is getting returned
                //       by MultiKeyHashtable.  Work around it here...
                Log.aribaweb.warning(9535, old);
            }
            return null;
        }

        public int getRegistrationOrder (Object key)
        {
            // Todo: need to change bucket.keysEqual() to take an opaque Object
            Object[] keys = (key instanceof Object[])
                ? (Object[])key : AWErrorInfo.makeKeyArray(key);
            List<AWErrorBucket> errors = _errors.elementsVector();
            for (int i = 0; i < errors.size(); i++) {
                AWErrorBucket bucket = errors.get(i);
                boolean match = bucket.keysEqual(keys);
                if (match) {
                    return i;
                }
            }
            return -1;
        }

        public Map<Integer, AWErrorBucket> getDisplayedErrors ()
        {
            return _orderedDisplayedErrors;
        }

        public void setDisplayedErrors (Map<Integer, AWErrorBucket> list)
        {
            _orderedDisplayedErrors = list;
        }

        public List<AWErrorBucket> getUndisplayedErrors ()
        {
            return _orderedUndisplayedErrors;
        }

        public void setUndisplayedErrors (List<AWErrorBucket> list)
        {
            _orderedUndisplayedErrors = list;
        }

        public boolean hasUnnavigableErrors()
        {
            return _hasUnnavigableErrors;
        }

        public void setHasUnnavigableError(boolean hasUnnavigable)
        {
            _hasUnnavigableErrors = hasUnnavigable;
        }

        public void rememberTableErrors (ErrorRepository oldRepository)
        {
            _previousTableAssociation = oldRepository._tableAssociation;
        }

        public void rememberDisplayOrder ()
        {
            // sort the errors by display order
            // and undisplayed errors by registration order
            _orderedDisplayedErrors = MapUtil.map();
            _orderedUndisplayedErrors = ListUtil.list();
            List<AWErrorBucket> allErrorsInRegOrder = _errors.elementsVector();
            for (AWErrorBucket bucket : allErrorsInRegOrder) {
                if (bucket.getDisplayOrder() != AWErrorInfo.NotDisplayed) {
                    _orderedDisplayedErrors.put(
                        Constants.getInteger(bucket.getDisplayOrder()), bucket);
                }
                else {
                    _orderedUndisplayedErrors.add(bucket);
                }
            }
        }

        public boolean allPreviousErrorsWereWarnings ()
        {
            if (_orderedDisplayedErrors.isEmpty() &&
                _orderedUndisplayedErrors.isEmpty()) {
                return false;
            }

            Iterator<AWErrorBucket> values = _orderedDisplayedErrors.values().iterator();
            while (values.hasNext()) {
                AWErrorBucket bucket = values.next();
                for (int i = 0; i < bucket.size(); i++) {
                    AWErrorInfo error = bucket.get(i);
                    if (!error.isWarning()) {
                        return false;
                    }
                }
            }
            for (int i = 0; i < _orderedUndisplayedErrors.size(); i++) {
                AWErrorBucket bucket = _orderedUndisplayedErrors.get(i);
                for (int j = 0; j < bucket.size(); j++) {
                    AWErrorInfo error = bucket.get(j);
                    if (!error.isWarning()) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean allErrorsAreWarnings ()
        {
            List<AWErrorBucket> orderedElements = elements();
            for (AWErrorBucket bucket : orderedElements) {
                for (int j = 0; j < bucket.size(); j++) {
                    AWErrorInfo error = bucket.get(j);
                    if (!error.isWarning()) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean warningsAreIdenticalToPreviousWarnings ()
        {
            if (!allErrorsAreWarnings()) {
                return false;
            }
            if (!allPreviousErrorsWereWarnings()) {
                return false;
            }
            Log.aribaweb_errorManager.debug("All errors in current and previous cycle are warnings.");

            Iterator<AWErrorBucket> values = _orderedDisplayedErrors.values().iterator();
            while (values.hasNext()) {
                AWErrorBucket bucket = values.next();
                for (int i = 0; i < bucket.size(); i++) {
                    AWErrorInfo error = bucket.get(i);
                    if (get(error.getKeys()) == null) {
                        return false;
                    }
                }
            }
            for (AWErrorBucket bucket : _orderedUndisplayedErrors) {
                for (int j = 0; j < bucket.size(); j++) {
                    AWErrorInfo error = bucket.get(j);
                    if (get(error.getKeys()) == null) {
                        return false;
                    }
                }
            }

            Log.aribaweb_errorManager.debug("All warnings in current and previous cycle are identical.");
            return true;
        }

        public int getErrorIndex (AWErrorInfo positioningError, int offset)
        {
            if (_orderedDisplayedErrors.isEmpty() &&
                _orderedUndisplayedErrors.isEmpty()) {
                Log.aribaweb_errorManager.debug(
                    "There is no information about displayed and undisplayed errors in the prev cycle");
                return -1;
            }

            int displayIndex = -1;
            int undisplayIndex = -1;
            // Todo: this part is not tested - so we don't actually use unnavigable error indicators
            // to help us determine the order of navigation yet.
            // _orderedUndisplayedErrors = sortUndisplayedErrorsOnUnnavigableOrder(positioningError);
            if (positioningError.getDisplayOrder() != AWErrorInfo.NotDisplayed) {
                // currently the displayed error portion
                displayIndex = positioningError.getDisplayOrder() + offset;
                if (displayIndex < 0) {
                    // wrap around to the bottom
                    if (_orderedUndisplayedErrors.isEmpty()) {
                        displayIndex = _orderedDisplayedErrors.size() -1;
                        undisplayIndex = -1;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has displayOrder %s - wrap around to last displayed error",
                            Integer.toString(positioningError.getDisplayOrder()));
                    }
                    else {
                        displayIndex = -1;
                        undisplayIndex = getLastUndisplayedErrorIndex();
                        Log.aribaweb_errorManager.debug(
                            "positioningError has displayOrder %s - wrap around to last undisplayed error",
                            Integer.toString(positioningError.getDisplayOrder()));
                    }
                }
                else if (displayIndex >= _orderedDisplayedErrors.size()) {
                    // wrap around to the top
                    if (_orderedUndisplayedErrors.isEmpty()) {
                        displayIndex = 0;
                        undisplayIndex = -1;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has displayOrder %s - wrap around to first displayed error",
                            Integer.toString(positioningError.getDisplayOrder()));
                    }
                    else {
                        displayIndex = -1;
                        undisplayIndex = getFirstUndisplayedErrorIndex();
                        Log.aribaweb_errorManager.debug(
                            "positioningError has displayOrder %s - wrap around to first undisplayed error",
                            Integer.toString(positioningError.getDisplayOrder()));
                    }
                }
            }
            else {
                // currently in the undisplayed error portion
                int positioningRegOrder = positioningError.getRegistrationOrder();
                undisplayIndex = -1;
                for (int i = 0; i < _orderedUndisplayedErrors.size(); i++) {
                    AWErrorBucket bucket = _orderedUndisplayedErrors.get(i);
                    if (bucket.getRegistrationOrder() == positioningRegOrder) {
                        Log.aribaweb_errorManager.debug(
                            "positioningError has regOrder %s - found match in undisplayed errors in prev cycle",
                            Integer.toString(positioningError.getRegistrationOrder()));
                        undisplayIndex = getUndisplayedErrorIndex(i);
                        break;
                    }
                }
                if (undisplayIndex == -1) {
                    Log.aribaweb_errorManager.debug(
                        "positioningError has regOrder %s - NO match in undisplayed errors in prev cycle - UNEXPECTED ***",
                        Integer.toString(positioningError.getRegistrationOrder()));
                    return -1;
                }
                undisplayIndex = undisplayIndex + offset;
                if (undisplayIndex < 0) {
                    // wrap around to the bottom
                    if (_orderedDisplayedErrors.isEmpty()) {
                        undisplayIndex = getUndisplayedErrorIndex(_orderedUndisplayedErrors.size() - 1);
                        displayIndex = -1;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has regOrder %s - wrap around to last undisplayed error",
                            Integer.toString(positioningError.getRegistrationOrder()));
                    }
                    else {
                        undisplayIndex = -1;
                        displayIndex = _orderedDisplayedErrors.size() - 1;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has regOrder %s - wrap around to last displayed error",
                            Integer.toString(positioningError.getRegistrationOrder()));
                    }
                }
                else if (undisplayIndex >= _orderedUndisplayedErrors.size()) {
                    // wrap around to the top
                    if (_orderedDisplayedErrors.isEmpty()) {
                        undisplayIndex = getUndisplayedErrorIndex(0);
                        displayIndex = -1;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has regOrder %s - wrap around to top undisplayed error",
                            Integer.toString(positioningError.getRegistrationOrder()));
                    }
                    else {
                        undisplayIndex = -1;
                        displayIndex = 0;
                        Log.aribaweb_errorManager.debug(
                            "positioningError has regOrder %s - wrap around to top displayed error",
                            Integer.toString(positioningError.getRegistrationOrder()));
                    }
                }
            }

            // If the next/prev error has gone away, we don't keep moving down/up.
            // The user will end up nav'ing to the first error.  We can improve
            // this later.
            if (displayIndex != -1) {
                AWErrorBucket oldError = 
                    _orderedDisplayedErrors.get(Constants.getInteger(displayIndex));
                if (oldError == null) {
                    Assert.assertNonFatal(false,
                        "The error display order indices are not contiguous as expected.");
                    // use the first error as a fallback
                    return -1;
                }
                Log.aribaweb_errorManager.debug("positioningError -> an old displayed error %s", oldError);
                return getRegistrationOrder(oldError.getKeys());
            }
            else if (undisplayIndex != -1) {
                setHasSelectedUndisplayedError(true);
                AWErrorBucket oldError =
                    _orderedUndisplayedErrors.get(undisplayIndex);
                Log.aribaweb_errorManager.debug("positioningError -> an old undisplayed error %s", oldError);
                return getRegistrationOrder(oldError.getKeys());
            }

            return -1;
        }

        private List<AWErrorBucket> sortUndisplayedErrorsOnUnnavigableOrder (
            AWErrorInfo positioningError)
        {
            if (!_hasUnnavigableErrors) {
                return _orderedUndisplayedErrors;
            }

            SparseVector /*AWErrorBucket*/ sortedErrors =
                new SparseVector(_orderedUndisplayedErrors.size(), false);
            allocEmptySlots(sortedErrors, _orderedUndisplayedErrors.size());
            int bottomCounter = _orderedUndisplayedErrors.size() - 1;
            Map<Integer, AWErrorBucket> unnavigableErrors = MapUtil.map();
            int highestUnnavNumber = 0;
            for (int i = _orderedUndisplayedErrors.size() - 1; i >= 0; i--) {
                AWErrorBucket bucket = _orderedUndisplayedErrors.get(i);
                if (bucket.getUnnavigableDisplayOrder() != AWErrorInfo.NotDisplayed) {
                    // pick out the unnavigable errors
                    Integer key = Constants.getInteger(bucket.getUnnavigableDisplayOrder());
                    unnavigableErrors.put(key, bucket);
                    highestUnnavNumber = Math.max(bucket.getUnnavigableDisplayOrder(), highestUnnavNumber);
                }
                else {
                    sortedErrors.add(bottomCounter, bucket);
                    bottomCounter--;
                }
            }

            int topCounter = 0;
            int start = (positioningError.getUnnavigableDisplayOrder() != AWErrorInfo.NotDisplayed)
                ? positioningError.getUnnavigableDisplayOrder() + 1
                : 0;
            for (int i = start; i <= highestUnnavNumber; i++) {
                Integer key = Constants.getInteger(i);
                AWErrorBucket bucket = unnavigableErrors.get(key);
                if (bucket != null) {
                    sortedErrors.add(topCounter, bucket);
                    topCounter++;
                }
            }
            for (int i = 0; i < start; i++) {
                Integer key = Constants.getInteger(i);
                AWErrorBucket bucket = unnavigableErrors.get(key);
                if (bucket != null) {
                    sortedErrors.add(topCounter, bucket);
                    topCounter++;
                }
            }

            return sortedErrors;
        }

        private void allocEmptySlots (SparseVector vector, int newSize)
        {
            int curSize = vector.size();
            for (int j = curSize; j < newSize; j++) {
                vector.add(j, null);
            }
        }

        private boolean hasSelectedUndisplayedError ()
        {
            return _selectedUndisplayError;
        }

        private void setHasSelectedUndisplayedError (boolean newValue)
        {
            _selectedUndisplayError = newValue;
        }

        private AWErrorBucket getDisplayedError (int displayOrder)
        {
            // try quick lookup first
            AWErrorBucket bucket =
                _orderedDisplayedErrors.get(Constants.getInteger(displayOrder));

            // now we have to loop
            if (bucket == null) {
                Iterator<AWErrorBucket> values =
                    _orderedDisplayedErrors.values().iterator();
                while (values.hasNext()) {
                    AWErrorBucket curBucket = values.next();
                    if (curBucket.getDisplayOrder() == displayOrder) {
                        return curBucket;
                    }
                }
            }

            return bucket;
        }

        private int getUndisplayedErrorIndex (int index)
        {
            // don't think we need to give visited more weight
            return index;
        }

        private int getFirstUndisplayedErrorIndex ()
        {
            // Give errors that haven't been visited more weight.
            // Find the first unvisited error.
            int secondChoice = -1;
            EMMultiKeyHashtable visitedErrors = _errorManager.getVisitedErrors();
            for (int i = 0; i < _orderedUndisplayedErrors.size(); i++) {
                AWErrorBucket bucket = _orderedUndisplayedErrors.get(i);
                if (visitedErrors.getValueForKeys(bucket.getKeys()) == null) {
                    AWErrorInfo prevError = _errorManager.getPreviousError();
                    if (prevError != null && !bucket.keysEqual(prevError.getKeys())) {
                        Log.aribaweb_errorManager.debug("Going to the first unvisited undisplayed error: %s", bucket);
                        return i;
                    }
                    else {
                        Log.aribaweb_errorManager.debug("First unvisited undisplayed error is also the prev error: %s", bucket);
                        secondChoice = i;
                    }
                }
            }

            // fall back to the error at the first position
            if (secondChoice != -1) {
                return secondChoice;
            }
            else {
                return 0;
            }
        }

        private int getLastUndisplayedErrorIndex ()
        {
            // Give errors that haven't been visited more weight.
            // Find the last unvisited error.
            EMMultiKeyHashtable visitedErrors = _errorManager.getVisitedErrors();
            for (int i = _orderedUndisplayedErrors.size() - 1; i >= 0; i--) {
                AWErrorBucket bucket = _orderedUndisplayedErrors.get(i);
                if (visitedErrors.getValueForKeys(bucket.getKeys()) == null) {
                    return i;
                }
            }

            // fall back to the error at the last position
            return _orderedUndisplayedErrors.size() - 1;
        }

        public void assignDisplayOrder (AWErrorInfo error)
        {
            // When the same error is displayed using multiple error flags,
            // this will get called multiple times.  In this situation, we only
            // assign the display order for the first flag.  Otherwise, our display
            // order indices are not contiguous.
            if (error.getDisplayOrder() != AWErrorInfo.NotDisplayed) {
                return;
            }

            int order = _displayOrderCounter;
            error.setDisplayOrder(order);
            _displayOrderCounter++;

            AWErrorBucket bucket = get(error.getKeys());
            for (int i = 0; i < bucket.size(); i++) {
                // assign the same display order for all errors of the same key
                AWErrorInfo curError = bucket.get(i);
                // set the display order only if it is not set
                if (curError.getDisplayOrder() == AWErrorInfo.NotDisplayed) {
                    curError.setDisplayOrder(order);
                }
            }

            Log.aribaweb_errorManager.debug(
                "%s: assignDisplayOrder: %s: %s (%s errors for the same key)",
                _errorManager.getLogPrefix(),
                Integer.toString(order), error.getMessage(),
                Integer.toString(bucket.size()));
        }

        public void assignUnnavigableDisplayOrder (AWErrorInfo error)
        {
            if (error.getUnnavigableDisplayOrder() != AWErrorInfo.NotDisplayed) {
                return;
            }

            int order = _unnavigableDisplayOrderCounter;
            error.setUnnavigableDisplayOrder(order);
            _unnavigableDisplayOrderCounter++;
            setHasUnnavigableError(true);

            AWErrorBucket bucket = get(error.getKeys());
            for (int i = 0; i < bucket.size(); i++) {
                // assign the same display order for all errors of the same key
                AWErrorInfo curError = bucket.get(i);
                curError.setUnnavigableDisplayOrder(order);
            }

            Log.aribaweb_errorManager.debug(
                "assignUnnavigableDisplayOrder: %s: %s (%s errors for the same key)",
                Integer.toString(order), error.getMessage(),
                Integer.toString(bucket.size()));
        }

        public void assignRegistryOrder (AWErrorBucket error)
        {
            int order = _registrationOrderCounter;
            error.setRegistrationOrder(order);
            _registrationOrderCounter++;
        }

        public void clearAllErrors ()
        {
            List errors = elements();
            for (int i = errors.size() - 1; i >= 0; i--) {
                AWErrorBucket bucket = (AWErrorBucket)errors.get(i);
                remove(bucket.getKeys());
            }
            _registrationOrderCounter = 0;
            _displayOrderCounter = 0;
            _errorSetId++;
        }

        public void clearValidationErrors ()
        {
            List<AWErrorBucket> errors = elements();
            List<AWErrorBucket> newErrors = ListUtil.list();
            for (int i=errors.size() - 1; i >= 0; --i) {
                AWErrorBucket error = errors.get(i);
                remove(error.getKeys());
                List<AWErrorInfo> nonValidationErrors = error.getErrorInfos(false);
                AWErrorBucket nonValidationError = null;
                if (nonValidationErrors.size() == 1) {
                    nonValidationError = nonValidationErrors.get(0);
                }
                else if (!nonValidationErrors.isEmpty()) {
                    nonValidationError = new MultiErrorBucket(nonValidationErrors.get(0));
                    for (int j=1, size=nonValidationErrors.size(); j<size; ++j) {
                        nonValidationError.add(nonValidationErrors.get(j));
                    }
                }
                if (nonValidationError != null) {
                    newErrors.add(nonValidationError);
                }
            }
            _registrationOrderCounter = 0;
            _displayOrderCounter = 0;
            _errorSetId++;
            Collections.sort(newErrors, new Comparator<AWErrorBucket>() {
                public int compare (AWErrorBucket first, AWErrorBucket second) {
                    return MathUtil.sgn(
                        first.getRegistrationOrder(), second.getRegistrationOrder());
                }
            });
            for (AWErrorBucket newError : newErrors) {
                assignRegistryOrder(newError);
                put(newError.getKeys(), newError);
            }
        }

        public int getErrorSetId ()
        {
            return _errorSetId;
        }

        public void setErrorSetId (int id)
        {
            _errorSetId = id;
        }

        public void setAssociatedTableItem (Object[] errorKeys,
                                            AWComponent datatable,
                                            Object tableItem)
        {
            AWErrorInfo errorInfo = new AWErrorInfo(errorKeys, null, null, false);
            errorInfo.setAssociatedTableItem(datatable, tableItem);
            _tableAssociation.put(errorKeys, errorInfo);
        }

        public AWErrorInfo getTableAssociation (AWErrorInfo error)
        {
            AWErrorInfo assoc = _getTableAssociationInternal(_tableAssociation, error);
            if (assoc == null && _previousTableAssociation != null) {
                assoc = _getTableAssociationInternal(_previousTableAssociation, error);
            }
            return assoc;
        }

        private AWErrorInfo _getTableAssociationInternal (
            EMMultiKeyHashtable<Object[], AWErrorInfo> associationInfo, AWErrorInfo error)
        {
            // Note that the keys in tableAssociationInfo is more coarse
            // then the error keys in the error objects.

            // try exact match first
            AWErrorInfo assoc = associationInfo.getValueForKeys(error.getKeys());

            if (assoc == null) {
                // now try coarse match
                Object[] tmpKeys = new Object[AWErrorInfo.NumKeys];
                int numNonNullKeys = error.getNumberOfKeys(error.getKeys());
                for (int len = numNonNullKeys - 1; len >= 1; len--) {
                    // build the partial keys
                    for (int i = 0; i < len; i++) {
                        tmpKeys[i] = error.getKeys()[i];
                    }
                    for (int i = len; i < AWErrorInfo.NumKeys; i++) {
                        tmpKeys[i] = AWErrorInfo.NullKey;
                    }

                    // lookup
                    assoc = associationInfo.getValueForKeys(tmpKeys);
                    if (assoc != null) {
                        break;
                    }
                }
            }
            return assoc;
        }
    }

    /**
     * Error Manager Multi Key Hashtable: keeps a list of all values for easy reference.
     */
    private static class EMMultiKeyHashtable<Keys, Value>
        extends MultiKeyHashtable /*<Keys, Value>*/
    {
        private List<Value> _allValues;

        public EMMultiKeyHashtable (int keyCount)
        {
            super(keyCount);
            _allValues = ListUtil.list();
        }

        public Value getValueForKeys (Object[] targetKeyList)
        {
            return (Value)super.get(targetKeyList);
        }

        public int indexForKeys (Object[] targetKeyList)
        {
            return super.indexForKeyList(targetKeyList);
        }

        public Value put (Object[] targetKeyList, Value value)
        {
            Value ret = (Value)super.put(targetKeyList, value, false);
            if (ret == null) {
                _allValues.add(value);
            }
            else {
                _allValues.remove(ret);
                _allValues.add(value);
            }
            return ret;
        }

        public Value remove (Object[] targetKeyList)
        {
            Object value = super.get(targetKeyList);
            if (value != null) {
                _allValues.remove(value);
            }
            return (Value)super.remove(targetKeyList);
        }

        protected List<Value> elementsVector ()
        {
            return _allValues;
        }
    }

    /**
     * This just holds some public fields.  Basically like a C struct.
     */
    private static class PrioritizedHandler
    {
        public AWErrorHandler handler;
        public int priority;
        public boolean isTableAutoScrollHandler;

        public PrioritizedHandler (
            AWErrorHandler handler, int priority, boolean isTableAutoScroll)
        {
            this.handler = handler;
            this.priority = priority;
            this.isTableAutoScrollHandler = isTableAutoScroll;
        }
    }
}
