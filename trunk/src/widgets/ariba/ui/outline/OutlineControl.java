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

    $Id: //ariba/platform/ui/widgets/ariba/ui/outline/OutlineControl.java#2 $
*/

package ariba.ui.outline;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.table.AWTDataTable;

public final class OutlineControl extends AWComponent
{
    public static final AWEncodedString FourNBSPs = new AWEncodedString("&nbsp;&nbsp;&nbsp;&nbsp;");

    public OutlineRepetition _currentRepetition;
    public boolean _hasTitle;
    private int _indentationPerLevel;

    public void awake ()
    {
        _currentRepetition = OutlineRepetition.currentInstance(this);
        _hasTitle = hasBinding(AWBindingNames.title);
        _indentationPerLevel = (hasBinding(BindingNames.indentationPerLevel)) ? intValueForBinding(BindingNames.indentationPerLevel) : 21;
    }

    public void sleep ()
    {
        _currentRepetition = null;
        _hasTitle = false;
        _indentationPerLevel = 0;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        AWTDataTable table = AWTDataTable.currentInstance(this);
        if (table != null) table.noteOutlineColumnIndex();
        super.renderResponse(requestContext, component);
        _currentRepetition._outlineState.setLastIndentationPx(indentationWithoutControl());
    }

    public int indentation ()
    {
        return _currentRepetition.nestingLevel() * _indentationPerLevel;
    }

    public int indentationWithoutControl ()
    {
        return indentation() + ((_currentRepetition._expandAll) ? 0 : (4 + 11 + 4));  //  Omit + 8  -- no padding
    }

    public int indentationWithControl ()
    {
        return 4 + indentation();
    }

    private static final AWEncodedString StylePaddingWithout = AWEncodedString.sharedEncodedString(" style=\"padding-left:");
    private static final AWEncodedString PxQuote = AWEncodedString.sharedEncodedString("px\"");
    private static final AWEncodedString StylePaddingWIth = AWEncodedString.sharedEncodedString(" style=\"padding:0px 4px 0px ");

    public String indentationWithControlStyle ()
    {
        AWResponse response = response();
        response.appendContent(StylePaddingWIth);
        response.appendContent(String.valueOf(indentationWithControl()));
        response.appendContent(PxQuote);
        return null;
    }

    public String indentationWithoutControlStyle ()
    {
        AWResponse response = response();
        response.appendContent(StylePaddingWithout);
        response.appendContent(String.valueOf(indentationWithoutControl()));
        response.appendContent(PxQuote);
        return null;
    }
    
    public String spacePadding ()
    {
        int i = _currentRepetition.nestingLevel();
        AWResponse response = response();
        while (i-- != 0) {
            response.appendContent(FourNBSPs);
        }
        return null;
    }

    public boolean showExpansionControl ()
    {
        return _currentRepetition._hasChildren && !_currentRepetition._expandAll && !_currentRepetition.hideImages();
    }

    public String currentToggleImageName ()
    {
        return (_currentRepetition.isExpanded()) ? "AWXArrowDown.gif": "AWXArrowRight.gif";
    }

    public AWComponent toggleExpansion ()
    {
        _currentRepetition.toggleExpansion();
        return (AWComponent)valueForBinding(AWBindingNames.action);
    }

    public boolean isSelected ()
    {
        return _currentRepetition.selectedObject() == _currentRepetition.currentItem();
    }

    public AWComponent select ()
    {
        _currentRepetition.setSelectedObject(_currentRepetition.currentItem());

        // Call their action if necessary
        return (AWComponent)valueForBinding(BindingNames.selectAction);
    }

    public boolean noLinkOnTitle ()
    {
        return !showExpansionControl() || !_hasTitle;
    }

    public boolean divWrapTitle ()
    {
        return _hasTitle;
    }

    public String title ()
    {
        return stringValueForBinding(AWBindingNames.title);
    }

    public boolean renderInTable ()
    {
        AWBinding binding = bindingForName(BindingNames.renderAsTable);
        return !requestContext().isExportMode() && ((binding == null) || booleanValueForBinding(binding));
    }
}
