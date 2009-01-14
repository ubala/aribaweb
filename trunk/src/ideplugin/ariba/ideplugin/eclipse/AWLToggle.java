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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/AWLToggle.java#2 $
*/
package ariba.ideplugin.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

import java.util.List;
import java.util.Arrays;

public class AWLToggle implements IObjectActionDelegate,
		IWorkbenchWindowActionDelegate {

    IWorkbenchWindow _window;
    ISelection _selection;

	public void setActivePart (IAction action, IWorkbenchPart targetPart)
    {

    }

    static List<String> TemplateExtensions = Arrays.asList("awl", "htm");
    static List<String> ComponentExtensions = Arrays.asList("java", "groovy");
    
    public void run (IAction action)
    {
        IFile selectedFile = getSelectedFile();
        _selection = null;
        if (selectedFile == null) {
            return;
        }
        IPath originalPath = selectedFile.getFullPath();
        String extension = originalPath.getFileExtension();
        String originalPathAsString = originalPath.toPortableString();
        if (extension == null) return;

        List<String> newExtensions = (TemplateExtensions.contains(extension))
                ? ComponentExtensions
                : (ComponentExtensions.contains(extension) ? TemplateExtensions : null);
        if (newExtensions == null) return;

        String baseName = originalPathAsString.substring(0,
                originalPathAsString.length() - (extension.length() + 1));
        for (String newExtension : newExtensions) {
            IPath path = Path.fromPortableString(baseName + "." + newExtension);
            IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
            if (file == null) continue;
            try {
                if (IDE.openEditor(_window.getActivePage(), file, true) != null) return;
            }
            catch (PartInitException e) {
                e.printStackTrace();
            }
        }
    }

    public void selectionChanged (IAction action, ISelection selection)
    {
        this._selection = selection;
    }

    public void dispose ()
    {
        _selection = null;
    }

    public void init (IWorkbenchWindow window)
    {
        _window = window;
    }

    public IFile getSelectedFile ()
    {
        IEditorInput input = _window.getActivePage().getActiveEditor().getEditorInput();
        IFile file = (IFile)input.getAdapter(IFile.class);
        return file;
    }
}
