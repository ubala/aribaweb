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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/SelectableRegionWrapper.java#5 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.Fmt;

public final class SelectableRegionWrapper extends AWComponent
{
    private static final String BaseLabelClass = "regionWrapper";
    private static final String SelectedLabelClass = "selectedRegionWrapper";
    private static final String TitleBaseLabelClass = "regionTitle";
    private static final String TitleSelectedLabelClass = "selectedRegionTitle";

    private boolean _isSelected = false;
    public AWEncodedString _elementId;

    /////////////
    // Awake
    /////////////
    protected void awake ()
    {
        _isSelected = booleanValueForBinding("selected");
        _elementId = requestContext().nextElementId();
    }

    protected void sleep ()
    {
        _isSelected = false;
        _elementId = null;
    }

    public String regionClass ()
    {
        return (_isSelected) ? SelectedLabelClass:BaseLabelClass;
    }

    public String regionTitleClass ()
    {
        return (_isSelected) ? TitleSelectedLabelClass:TitleBaseLabelClass;
    }

    public String onMouseOver ()
    {
        return Fmt.S("return ariba.Widgets.showSpan('%s');",_elementId);
    }

    public String onMouseOut ()
    {
        return Fmt.S("return ariba.Widgets.hideSpan('%s');",_elementId);
    }

    public String iframeId ()
    {
        return Fmt.S("%sIFrame", _elementId);
    }
}
