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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/OutlineBox.java#4 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.util.core.Constants;

public class OutlineBox extends AWComponent
{

    private Boolean _isExpanded;
    private AWBinding _state;
    private AWBinding _toggleAction;

    private static final String InitiallyExpandedBinding = "initiallyExpanded";
    private static final String ShowExpandoCollapsoBinding = "showExpandoCollapso";
    private static final String TitleMaxLength = "titleMaxLength";
    private static final String CollapsoImage = "awxToggleImageTrue.gif";
    private static final String ExpandoImage = "awxToggleImageFalse.gif";
    private static final String CollapsoStyle = "padding-right:4px;";
    private static final String ExpandoStyle = "padding-right:4px; margin-bottom:-2px;";

    /* Needs to be stateful to handle expando/collapso */
    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();
        _state = bindingForName(AWBindingNames.state);
        _toggleAction = bindingForName(AWBindingNames.action);
    }

    /* Return true if we should show the body of the box as expanded, false if we should
       show it as collapsed */
    public boolean isExpanded ()
    {
        if (_state != null) {
            return booleanValueForBinding(_state);
        }
        if (_isExpanded != null) {
            return _isExpanded.booleanValue();
        }

        if (!showExpandoCollapsoControl()) {
            // If we are not even showing the expando collapso, assume we should show
            // the box as being expanded
            _isExpanded = Boolean.TRUE;
        }
        else {
            // If we were passed a binding to tell us whether to initially show the
            // box as expanded or collapse, use that to initialize the value.  If we
            // were not passed that binding, assume we should initially show the box
            // as expanded
            _isExpanded = (Boolean)valueForBinding(InitiallyExpandedBinding);
            if (_isExpanded == null) {
                _isExpanded = Boolean.TRUE;
            }
        }

        return _isExpanded.booleanValue();
    }

    public boolean hasTitleMaxLength ()
    {
        return hasBinding(TitleMaxLength);
    }

    /* If we were passed a binding showExpandoCollapso = true, then we should display
       the expando collapso control for the box */
    public boolean showExpandoCollapsoControl ()
    {
        return booleanValueForBinding(ShowExpandoCollapsoBinding);
    }

    public String expandoCollapsoImage ()
    {
        return isExpanded() ?  CollapsoImage : ExpandoImage;
    }

    public String expandoCollapsoToolTip ()
    {
        return isExpanded() ? localizedJavaString(1, "Minimize" /* Tooltip on collapse button */) :
                              localizedJavaString(2, "Maximize this view" /* Tooltip on expand button */);
    }

    public AWComponent toggleExpandoCollapsoAction ()
    {
        _toggleAction = bindingForName(AWBindingNames.action);
        if (_toggleAction != null) {
            valueForBinding(_toggleAction);
        }
        else if (_state != null) {
            boolean state = booleanValueForBinding(_state);
            setValueForBinding(!state, _state);            
        }
        else {
            _isExpanded = Constants.getBoolean(!isExpanded());
        }
        return null;
    }

    public String expandoCollapsoStyle ()
    {
        return isExpanded() ? CollapsoStyle : ExpandoStyle;
    }

}
