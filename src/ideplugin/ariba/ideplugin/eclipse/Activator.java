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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/Activator.java#5 $
*/
package ariba.ideplugin.eclipse;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.osgi.framework.BundleContext;

import ariba.ideplugin.core.RemoteOpen;
import ariba.ideplugin.core.RemoteOpen.Opener;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements Opener
{

    // The plug-in ID
    public static final String PLUGIN_ID = "ariba.ideplugin.eclipse";

    public static final String PrefAWPath = "AWPath";
    public static final String PrefAutoCheck = "AutoCheck";
    public static final String ClasspathAWHome = "AW_HOME";

    // The shared instance
    private static Activator plugin;
    private RemoteOpen ropen;

    public Activator ()
    {
        plugin = this;
    }

    public void start (BundleContext context) throws Exception
    {
        super.start(context);
        ropen = new RemoteOpen(this);
        ropen.start();
    }

    public void stop (BundleContext context) throws Exception
    {
        plugin = null;
        ropen.stop();
        super.stop(context);
    }

    public boolean open (String name, int line)
    {
        Display.getDefault().asyncExec(new OpenAction(name, line));
        return true;
    }

    public String getAWHome ()
    {
        return getPluginPreferences().getString(PrefAWPath);
    }

    public class OpenAction implements Runnable
    {
        String _file;
        int _line;

        public OpenAction (String f, int l)
        {
            _file = f;
            _line = l;
        }

        public void run ()
        {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject[] projects = root.getProjects();
            try {
                IFile file = recurseFind(projects);
                if (file != null) {
                    IWorkbenchWindow window = PlatformUI.getWorkbench()
                        .getWorkbenchWindows()[0];
                    IEditorPart editor = IDE.openEditor(window.getActivePage(),
                                                        file, true);
                    if (editor instanceof AbstractTextEditor) {
                        AbstractTextEditor te = (AbstractTextEditor)editor;
                        IDocument document = te.getDocumentProvider()
                            .getDocument(te.getEditorInput());
                        try {
                            int start = document.getLineOffset(_line - 1);
                            te.selectAndReveal(start, 0);
                            te.getSite().getPage().activate(te);
                        }
                        catch (BadLocationException ble) {

                        }
                    }
                }
            }
            catch (CoreException ce) {

            }
        }

        protected IFile recurseFind (IResource[] resources) throws CoreException
        {
            for (int i = 0; i < resources.length; i++) {
                if (resources[i] instanceof IContainer) {
                    if (resources[i] instanceof IProject) {
                        if (!((IProject)resources[i]).isOpen()) {
                            continue;
                        }
                    }
                    IFile ret = recurseFind(((IContainer)resources[i])
                        .members());
                    if (ret != null)
                        return ret;
                }
                else if (resources[i] instanceof IFile) {
                    if (resources[i].exists()) {
                        if (((IFile)resources[i]).getFullPath().toString()
                            .indexOf(_file) > -1)
                        {
                            return (IFile)resources[i];
                        }
                    }
                }
            }
            return null;
        }
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault ()
    {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     * 
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor (String path)
    {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }
}
