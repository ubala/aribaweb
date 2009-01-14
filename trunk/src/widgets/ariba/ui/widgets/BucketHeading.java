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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BucketHeading.java#18 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.util.core.StringUtil;

public final class BucketHeading extends AWComponent
{
    private static final AWEncodedString NBSP = AWEncodedString.sharedEncodedString("&nbsp;");
    public AWEncodedString _labelRight;
    public AWEncodedString _spaceRight;
    public boolean _isOpen;
    private boolean _isHyperlink;
    private boolean _isSelected;

    public void awake ()
    {
        super.awake();
        _isOpen = _isOpenState();
        _isHyperlink = _isHyperlink();
    }

    protected void sleep ()
    {
        _labelRight = null;
        _spaceRight = null;
        _isOpen = false;
        _isHyperlink = false;
        _isSelected = false;
    }

    private boolean _isOpenState ()
    {
        AWBinding binding = bindingForName(BindingNames.state);
        if (binding == null) {
            return _isOpen;
        }
        else {
            return booleanValueForBinding(binding);
        }
    }

    /* BucketHeading can function as hyperlink, this is an existing feature of BucketItem
     * but the way it is determined to be hyperlink is different than BucketItem. Regardly of
     * labelRight binding, we can for action binding, if action exist, then hyperlink, otherwise
     * no. BucketItem checks enable binding first.
     */
    private boolean _isHyperlink ()
    {
        AWBinding binding = bindingForName(BindingNames.action);
        if (binding != null) {
            return true;
        }
        return false;
    }

    public boolean isOpenState ()
    {
        return _isOpen;
    }

    public boolean isNotHyperlink ()
    {
        return !_isHyperlink;
    }

    public boolean hasForm ()
    {
        return requestContext().currentForm() != null;
    }

    protected boolean _debugSemanticKeyInteresting()
    {
        return true;
    }

    protected String _debugSemanticKey()
    {
        String semanticKey = stringValueForBinding(AWBindingNames.label);
        return semanticKey;
    }

    public AWEncodedString styleClass ()
    {
        if (!_isHyperlink) {
            return null;
        }
        return _isSelected ? StepByStepTOC.WizStepCurrent : StepByStepTOC.WizStep;
    }

    public AWEncodedString onMouseOver ()
    {
        if (!_isHyperlink) {
            return null;
        }
        return _isSelected ? null : StepByStepTOC.MouseOver;
    }

    public AWEncodedString onMouseOut ()
    {
        if (!_isHyperlink) {
            return null;
        }
        return _isSelected ? null : StepByStepTOC.MouseOut;
    }

    /* labelRight binding will be checked during the renderResponse phase, if it is null or empty
     * string, we will append &nbsp into the outgoing html. Reason is otherwise, we will have
     * <td></td> and netscape has problem displaying it. Since <AWString value="$labelRight"> will
     * escape so, we have use AWPrimitiveString to display NBSP.
     */
    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        _isSelected = booleanValueForBinding(BindingNames.isSelected);
        _labelRight = encodedStringValueForBinding(BindingNames.labelRight);
        _spaceRight = _labelRight == null || StringUtil.nullOrEmptyString(_labelRight.string())
                        ? NBSP : null;
        super.renderResponse(requestContext, component);
    }
}

