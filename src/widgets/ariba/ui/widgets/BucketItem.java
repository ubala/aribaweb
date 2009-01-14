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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/BucketItem.java#20 $
*/

package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWGenericActionTag;
import ariba.util.core.StringUtil;

public final class BucketItem extends AWComponent
{
    private static final AWEncodedString NBSP = AWEncodedString.sharedEncodedString("&nbsp;");
    public AWEncodedString _labelRight;
    public AWEncodedString _spaceRight;
    private boolean _isSelected;

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

    protected void sleep ()
    {
        _labelRight = null;
        _spaceRight = null;
        _isSelected = false;
    }

    public AWEncodedString styleClass ()
    {
        return _isSelected ? StepByStepTOC.WizStepCurrent : StepByStepTOC.WizStep;
    }

    public String behaviorName()
    {
        return _isSelected ? null : "BI";
    }

    public boolean hasForm ()
    {
        return requestContext().currentForm() != null;
    }

    /* The way we detect whether this bucketitem is clickable is:
     * 1. if enabled binding exists, use the boolean value
     * 2. then defaut it to clickable, unless labelRight == 0 (not sure logic)
     *    seems this is application logic
     */
    public boolean disabled ()
    {
        boolean enabled = false;
        AWBinding enabledBinding = bindingForName(BindingNames.enabled);
        if (enabledBinding != null) {
            enabled = booleanValueForBinding(enabledBinding);
        }
        else {
            AWEncodedString labelRight = encodedStringValueForBinding(BindingNames.labelRight);
            enabled = labelRight == null ? true : !"0".equals(labelRight.string());
        }
        return !enabled;
    }

    public AWResponseGenerating linkClicked ()
    {
        return AWGenericActionTag.evaluateActionBindings(this);
    }

    protected boolean _debugSemanticKeyInteresting ()
    {
        return true;
    }

    protected String _debugSemanticKey ()
    {
        return stringValueForBinding(AWBindingNames.label);
    }
}
