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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/editor/MetaNavEditorMenu.java#2 $
*/
package ariba.ui.meta.editor;

import ariba.ui.meta.core.UIMeta;
import ariba.ui.aribaweb.core.AWComponent;

public class MetaNavEditorMenu extends AWComponent
{
    /*
    <w:PopupMenuHeading>Metadata Editor</w:PopupMenuHeading>
    <a:If ifTrue="$editingEnabled">
        <w:PopupMenuItem action="$disableEditing">Disable Editing</w:PopupMenuItem>
        <w:PopupMenuItem action="$saveChanges">Save Changes</w:PopupMenuItem>
    <a:Else/>
        <w:PopupMenuItem action="$enableEditing">Enable Editing</w:PopupMenuItem>
    </a:If>

     */
    public boolean editingEnabled ()
    {
        return EditManager.activeEditManager(UIMeta.getInstance(), session()) != null;
    }

    public void enableEditing ()
    {
        EditManager editManager = EditManager.currentEditManager(UIMeta.getInstance(), session(), true);
        editManager.setEditing(true);
    }

    public void disableEditing ()
    {
        // EditManager.clearEditManager(UIMeta.getInstance(), session());
        EditManager editManager = EditManager.currentEditManager(UIMeta.getInstance(), session(), false);
        editManager.setEditing(false);
    }

    public boolean hasChanges ()
    {
        EditManager editManager = EditManager.activeEditManager(UIMeta.getInstance(), session());
        return (editManager != null && editManager.hasChanges());
    }

    public void saveChanges ()
    {
        EditManager editManager = EditManager.currentEditManager(UIMeta.getInstance(), session(), false);
        editManager.saveChanges();
    }
}
