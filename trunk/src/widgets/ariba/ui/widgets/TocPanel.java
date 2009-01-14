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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TocPanel.java#7 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBinding;

public final class TocPanel extends AWComponent
{
    /*-----------------------------------------------------------------------
        Constants
      -----------------------------------------------------------------------*/

    private static final String ExpandedImage =
        "awxToggleImageFalse.gif";
    private static final String CollapsedImage =
        "awxToggleImageTrue.gif";

    private static final String ActiveHeadingStyle = "this.className='tocItemRollover';";
    private static final String InactiveHeadingStyle = "this.className='tocItem';";

    private static final String InitStateBinding = "initState";

    /*-----------------------------------------------------------------------
        Members
      -----------------------------------------------------------------------*/

    private boolean _isExpanded;

    /*-----------------------------------------------------------------------
        Init
      -----------------------------------------------------------------------*/

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        super.init();

        _isExpanded = true;
    }

    /*-----------------------------------------------------------------------
        Bindings
      -----------------------------------------------------------------------*/

    public boolean isExpanded ()
    {
        AWBinding binding = bindingForName(BindingNames.state);
        if (binding != null) {
            _isExpanded = booleanValueForBinding(binding);
        }

        return _isExpanded;
    }

    public void setIsExpanded (boolean isExpanded)
    {
        _isExpanded = isExpanded;
        setValueForBinding(_isExpanded, BindingNames.state);
    }

    public void toggleIsExpanded ()
    {
        setIsExpanded(!isExpanded());
    }

    public String toggleImage ()
    {
        if (isExpanded()) {
            return CollapsedImage;
        }
        else {
            return ExpandedImage;
        }
    }

    public String headingStyle ()
    {
        return (isExpanded() ? InactiveHeadingStyle : ActiveHeadingStyle);
    }

    public String toggleTip ()
    {
        if (isExpanded()) {
            return localizedJavaString(1, "Collapse view" /*  */);
        }
        else {
            return localizedJavaString(2, "Expand view" /*  */);
        }
    }
    /*-----------------------------------------------------------------------
        Actions
      -----------------------------------------------------------------------*/

    public AWComponent toggleAction ()
    {
        // Toggle
        toggleIsExpanded();

        return null;
    }

    public boolean getInitState ()
    {
        if (hasBinding(InitStateBinding)) {
            return booleanValueForBinding(InitStateBinding);
        }

        return false;
    }
}
