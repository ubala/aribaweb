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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWFormRedirect.java#16 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.test.TestContext;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.util.core.PerformanceState;
import java.util.List;
import java.util.Map;

public class AWFormRedirect extends AWComponent
{
    public static final String PageName = "AWFormRedirect";
    public static final String TopTarget = "_top";

    private static final String FormNamesBinding     = "formNames";
    private static final String FormValuesBinding    = "formValues";
    private static final String DebugBinding         = "debug";
    private static final String FormActionUrlBinding = "formActionUrl";

    private String _title;
    private String _formActionUrl;
    private String _debugString;
    private List _names;
    private List _values;
    private int _currentIndex;
    private String _target;

    protected boolean shouldValidateSession ()
    {
        // disable automatic session validation for this page
        return false;
    }

    protected boolean allowDeferredResponse ()
    {
        return false;
    }

    // ** Thread Safety Considerations: see AWComponent.
    public boolean shouldCachePage ()
    {
        return false;
    }

    public void awake ()
    {
        _names = (List)valueForBinding(FormNamesBinding);
        _values = (List)valueForBinding(FormValuesBinding);
        _debugString = (String)valueForBinding(DebugBinding);
        _formActionUrl = (String)valueForBinding(FormActionUrlBinding);

        super.awake();
    }

    public void sleep ()
    {
        _names = null;
        _values = null;
        _debugString = null;
        _formActionUrl = null;

        super.sleep();
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        if (PerformanceState.threadStateEnabled()) {
            // TODO: only do this for *self* redirects?
            PerformanceState.getThisThreadHashtable().setToBeContinued(true);
        }

        super.renderResponse(requestContext, component);
    }

    public void applyValues(AWRequestContext requestContext,
                                       AWComponent component)
    {
        throw new AWGenericException("AWFormRedirect: applyValues " +
            "should never be called since this page should never be cached.");
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext,
                                                        AWComponent component)
    {
        throw new AWGenericException("AWFormRedirect: invokeAction should " +
            "never be called since this page should never be cached.");
    }

    public void setTitle (String title)
    {
        _title = title;
    }

    public String getTitle ()
    {
        return _title;
    }

    public void setCurrIndex (int index)
    {
        _currentIndex = index;
    }

    public String currentName ()
    {
        return (String)_names.get(_currentIndex);
    }

    public String currentValue ()
    {
        return (String)_values.get(_currentIndex);
    }

    public List getFormValues ()
    {
        if (_values == null) {
            _values = ListUtil.list();
        }
        return _values;

    }

    protected List getFormNames ()
    {
        if (_names == null) {
            _names = ListUtil.list();
        }
        return _names;
    }

    public void addFormValue (String name, String value)
    {
        getFormNames().add(name);
        getFormValues().add(value);
    }

    public void addFormValues (Map <String, String[]> values)
    {
        for (Map.Entry<String, String[]> e : values.entrySet()) {
            String[] vals = (String[])e.getValue();
            for (int i = 0; i < vals.length; i++) {
                addFormValue(e.getKey(), vals[i]);
            }
        }
    }

    public void setFormActionUrl (String url)
    {
        _formActionUrl = url;
    }

    public String getFormActionUrl ()
    {
        return _formActionUrl;
    }

    public void setDebugString (String debug)
    {
        _debugString = debug;
    }

    public String getDebugString ()
    {
        return _debugString;
    }

    public void setTarget (String target)
    {
        _target = target;
    }

    public String getTarget ()
    {
        String target = _target;
        if (StringUtil.nullOrEmptyOrBlankString(target)) {
            if (requestContext().isIncrementalUpdateRequest()) {
                target = TopTarget;
            }
        }
        return target;
    }

    public boolean isDebuggingMode()
    {
        return AWConcreteServerApplication.IsDebuggingEnabled;
    }
}
