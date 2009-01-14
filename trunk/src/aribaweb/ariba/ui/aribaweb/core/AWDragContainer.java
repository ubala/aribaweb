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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWDragContainer.java#9 $
*/
package ariba.ui.aribaweb.core;

/**
 * An AWDragComponents can play two roles, Drag Control and Drag Container.  The Drag
 * Control defines a 'draggable' region that can be selected and dragged by the user.  The
 * Drag Container defines the region of content that is moved during the drag.
 * Typically, a Drag Component is both a Drag Control and a Drag Container.  In
 * other words, the draggable region is the same as the region that will be affected by
 * the drag gesture.  In some cases, it is desireable to have a Drag Control that is a
 * subset of the Drag Container.  For example, dragging a portlet in the dashboard can
 * be initiated by dragging the menu bar of the portlet (Drag Control)
 * while the drag gesture will affect the entire portlet (Drag Container).
 *
 * By default an AWDragComponent is a Drag Control and a Drag Container.  In this case,
 * the draggable region and the region affected are exactly the same.
 *
 * An AWDragComponent is considered to be only a Drag Container if it does not have a
 * drag action defined.
 *
 * An AWDragComponent whose showParent binding is set to true will
 * use its parent AWDragComponent (next component higher in the hierarchy with the the
 * same drag type) as its Drag Container.
 */

import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.util.core.StringUtil;

public final class AWDragContainer extends AWComponent
{
    private static final String DragPrefix = " dc awDrg_";
    private static final String DragContainerPrefix = " dc awDrgCnt_";
    private static final String ShowParentKey = "awDrgPrt";

    public static final String ShowParentBinding = "showParent";
    public static final String DragActionBinding = "dragAction";
    public AWEncodedString _elementId;

    private static final String[] SupportedBindingNames = {
        AWBindingNames.tagName,
        AWBindingNames.classBinding,
        AWBindingNames.bh,
        DragActionBinding,
        AWBindingNames.type,
        ShowParentBinding, AWBindingNames.omitTags,
    };

    ///////////////////
    // Bindings Support
    ///////////////////
    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    public void sleep ()
    {
        _elementId = null;
        super.sleep();
    }

    public static AWResponseGenerating processAction (AWComponent component, AWEncodedString elementId)
    {
        AWResponseGenerating actionResults = null;
        // drag/drop requests come in with two senderIds.  If there are more to come, then this is the first (the drag)
        if (component.requestContext().hasMoreSenderIds()) {
            actionResults = (AWResponseGenerating)component.valueForBinding(DragActionBinding);
            // Assert.that(actionResults == null, "AWDragContainer action must return a null result -- the drop container returns the next page");
        } else {
            actionResults = (AWResponseGenerating)component.valueForBinding(AWDropContainer.DropActionBinding);
        }
        return actionResults;
    }

    public AWResponseGenerating fireAction ()
    {
        return processAction(this, _elementId);
    }

    public boolean isContainer ()
    {
        return !hasBinding(DragActionBinding);
    }

    public String cssClass ()
    {
        // if there is no dragaction defined, then assume that the Drag Component is
        // actually a Drag Container -- there'll be another Drag Component contained
        // within it that will act as the Drag Control.  (See description above).
        String prefix = isContainer() ? DragContainerPrefix : DragPrefix;

        if (booleanValueForBinding(ShowParentBinding)) {
            prefix = StringUtil.strcat(ShowParentKey, " ", prefix);
        }

        String type = stringValueForBinding(AWBindingNames.type);
        if (!StringUtil.nullOrEmptyOrBlankString(type)) {
            return StringUtil.strcat(prefix,type);
        }
        else {
            return prefix;
        }
    }
}