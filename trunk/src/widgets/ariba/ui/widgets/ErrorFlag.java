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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ErrorFlag.java#6 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWErrorBucket;
import ariba.ui.aribaweb.core.AWErrorManager;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWErrorManager.MultiErrorBucket;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Constants;
import ariba.util.core.ListUtil;
import ariba.util.core.FastStringBuffer;

import java.util.List;

public class ErrorFlag extends AWComponent
{
    protected List<AWErrorInfo> _errorInfoList;
    private String _errorMsg;
    private Boolean _showError;
    private Boolean _showWarning;

    protected void awake ()
    {
        initErrorInfo();
    }

    protected void sleep ()
    {
        _errorInfoList = null;
        _errorMsg = null;
        _showError = null;
        _showWarning = null;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (_errorInfoList != null) {
            boolean navigable = isNavigable();
            for (int i = 0; i < _errorInfoList.size(); i++) {
                AWErrorInfo error = _errorInfoList.get(i);
                errorManager().setErrorDisplayOrder(error, navigable);
                if (navigable && indicatorId() != null) {
                    error.setIndicatorId(indicatorId());
                }
            }
        }
        super.renderResponse(requestContext, component);
    }

    public AWEncodedString indicatorId ()
    {
        // subclass overrides this - used for popping the bubble
        return null;
    }

    protected boolean isNavigable ()
    {
        AWBinding binding = bindingForName(BindingNames.isNavigable, true);
        boolean navigable = binding != null ? booleanValueForBinding(binding) : true;
        return navigable;
    }

    public boolean hasErrorsOrWarnings ()
    {
        AWErrorManager errorManager = errorManager();

        if (errorManager.isErrorDisplayEnabled() == false) {
            return false;
        }

        return showError() || showWarning();
    }


    public String error ()
    {
        if (_errorMsg == null) {
            if (!ListUtil.nullOrEmptyList(_errorInfoList)) {
                if (_errorInfoList.size() == 1) {
                    _errorMsg = (_errorInfoList.get(0)).getMessage();
                }
                else {
                    _errorMsg = formatErrorList(_errorInfoList);
                }
            }
        }
        return _errorMsg;
    }

    private String formatErrorList (List errors)
    {
        FastStringBuffer fsb = new FastStringBuffer();
        for (int i = 0; i < errors.size(); i++) {
            AWErrorInfo error = (AWErrorInfo)errors.get(i);
            if (i > 0) {
                // the rendering code is expected to use safe escaping
                fsb.append("<br/>");
            }
            fsb.append(error.getMessage());
        }
        return fsb.toString();
    }

    private void initErrorInfo ()
    {
        if (_errorInfoList == null) {
            Object[] errorKeys = AWErrorManager.getErrorKeyFromBindings(this);
            //Log.aribaweb_errorManager.debug("ErrorFlag: errorKeys=[%s, %s, %s]", errorKeys[0], errorKeys[1], errorKeys[2]);
            List<AWErrorBucket> errors = errorManager().errorsForKeys(errorKeys);
            if (errors != null) {
                _errorInfoList = ListUtil.list();
                for (int i = 0; i < errors.size(); i++) {
                    _errorInfoList.addAll(errors.get(i).getErrorInfos());
                }
            }
            else {
                _errorInfoList = null;
            }
        }
    }

    public String warning ()
    {
        return error();
    }

    public boolean isGeneralError ()
    {
        AWErrorManager errorManager = errorManager();
        Object[] errorKeys = AWErrorManager.getErrorKeyFromBindings(this);
        Object singleKey = errorKeys[AWErrorInfo.SingleKeyIndex];
        return AWErrorManager.GeneralErrorKey.equals(singleKey);
    }

    public boolean showError ()
    {
        if (_showError == null) {
            AWBinding binding = bindingForName("showError", true);
            if (binding == null) {
                _showError = Boolean.TRUE;
            }
            else {
                _showError = Constants.getBoolean(booleanValueForBinding(binding));
            }
        }
        boolean hasError = hasError(false);
        boolean inErrorDisplayMode = errorManager().isErrorDisplayEnabled();

        /*
        Log.aribaweb_errorManager.debug("ErrorFlag: (hasError=%s inErrorDisplayMode=%s showError=%s) show=%s error: %s",
            Constants.getBoolean(hasError), Constants.getBoolean(inErrorDisplayMode), _showError,
            Constants.getBoolean(_showError.booleanValue() && hasError && inErrorDisplayMode),
            error());
        */

        return _showError.booleanValue() && hasError && inErrorDisplayMode;
    }

    private boolean hasError (boolean isWarning)
    {
        if (!ListUtil.nullOrEmptyList(_errorInfoList)) {
            for (int i = 0; i < _errorInfoList.size(); i++) {
                AWErrorInfo error = _errorInfoList.get(i);
                if (error.isWarning() == isWarning) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean showWarning ()
    {
        if (_showWarning == null) {
            AWBinding binding = bindingForName("showWarning", true);
            if (binding == null) {
                _showWarning = Boolean.TRUE;
            }
            else {
                _showWarning = Constants.getBoolean(booleanValueForBinding(binding));
            }
        }
        boolean hasWarning = hasError(true);

        return _showWarning.booleanValue() && hasWarning;
    }

    protected boolean actionTracingEnabled ()
    {
        return true;
    }
}
