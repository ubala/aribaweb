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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/wizards/CreateProject.java#4 $
*/
package ariba.ideplugin.eclipse.wizards;

import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.io.File;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import ariba.ideplugin.core.AWScriptRunner;
import ariba.ideplugin.core.WrapperRuntimeException;
import ariba.ideplugin.eclipse.Activator;

public class CreateProject implements IRunnableWithProgress
{

    String _projectName;
    Map<String, String> _paramMap;
    File _templateLoc;
    AWScriptRunner _connector;
    Shell _shell;

    public CreateProject (Shell shell,
                          AWScriptRunner connector,
                          String projectName,
                          File templateLoc,
                          Map<String, String> paramMap)
    {
        _projectName = projectName;
        _paramMap = paramMap;
        _templateLoc = templateLoc;
        _connector = connector;
        _shell = shell;
    }

    public void run (IProgressMonitor monitor)
    {
        try {
            execute(monitor);
        }
        catch (Exception e) {
            final Exception ex = e;
            Display.getDefault().asyncExec(new Runnable() {
                public void run ()
                {
                    Status status = new Status(IStatus.ERROR, Activator
                     .getDefault().getBundle().getSymbolicName(), 0, ex.getMessage(), ex);
                    ErrorDialog.openError(_shell, null, null, status);
                }
            });
        }
    }

    public void execute (IProgressMonitor monitor) throws Exception
    {
        monitor.beginTask("Creating AribaWeb Project", 4);
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot wroot = workspace.getRoot();

        IProject proj = wroot.getProject(_projectName);
        if (!proj.exists()) {
            File ploc = new File(wroot.getLocation().toFile(), _projectName);
            ploc.mkdirs();
            _paramMap.put("ProjectName", _projectName);

            _connector.createProject(_templateLoc, ploc, _paramMap);

            monitor.worked(1);

            File locationAsFile = new File(wroot.getLocation().toString(),
                _projectName);
            if (!locationAsFile.exists() && !locationAsFile.mkdir()) {
                // String msg = "Could not create " + locationAsFile;
                // log error
                return;
            }
            File f = new File(locationAsFile, ".project");
            if (f.exists()) {
                f.delete();
            }
            f = new File(locationAsFile, ".classpath");
            if (f.exists()) {
                f.delete();
            }
            IProjectDescription pid = workspace
                .newProjectDescription(_projectName);
            String natures[] = { "org.eclipse.jdt.core.javanature" };
            pid.setNatureIds(natures);
            pid.setLocation(null);

            proj.create(pid, monitor);
            final IJavaProject javaProj = JavaCore.create(proj);
            if (!proj.isOpen()) {
                proj.open(monitor);
            }

            monitor.worked(1);
            updateClasspath(javaProj, locationAsFile, _projectName, monitor);
            monitor.worked(1);
        }
        else {
            // this should not happen
        }
    }

    public void updateClasspath (IJavaProject proj,
                                 File root,
                                 String projectName,
                                 IProgressMonitor monitor)
    {
        try {
            File buildFile = new File(root, "build.xml");
            List<String> paths = _connector.getRequiredLibs(buildFile);

            IClasspathEntry jreEntry = JavaCore.newContainerEntry(new Path(
                "org.eclipse.jdt.launching.JRE_CONTAINER"));
            IClasspathEntry srcEntry = JavaCore.newSourceEntry(new Path("/"
                + projectName));
            ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
            entries.add(srcEntry);
            entries.add(jreEntry);

            String awhome = new File(_connector.getAWHome()).getCanonicalPath();
            String fileSeperator = System.getProperty("file.separator");
            int len = awhome.endsWith(fileSeperator) ? awhome.length() - 1 : awhome.length();

            for (int i = 0; i < paths.size(); i++) {
                String path = new File(paths.get(i)).getCanonicalPath();

                if (path.startsWith(awhome)) {
                    path = Activator.ClasspathAWHome + path.substring(len);
                }
                if (fileSeperator.equals("\\") && path.indexOf('\\') > -1) {
                    path = path.replace('\\', '/');
                }
                if (path.endsWith(".jar") || path.endsWith(".zip")) {
                    Path src = null;
                    if (paths.get(i).indexOf("ariba.appcore") > -1) {
                        src = new Path(Activator.ClasspathAWHome + "/lib/ariba.appcore-src.jar");
                    }
                    else if (paths.get(i).indexOf("ariba.") > -1) {
                        src = new Path(Activator.ClasspathAWHome + "/lib/ariba.aw-all-src.jar");
                    }                                        
                    if (path.startsWith(Activator.ClasspathAWHome)) {
                        entries.add(JavaCore.newVariableEntry(new Path(path),src, null));
                    }
                    else {
                        entries.add(JavaCore.newLibraryEntry(new Path(path),src, null));
                    }
                }
            }
            IClasspathEntry[] centries = new IClasspathEntry[entries.size()];
            proj.setRawClasspath(entries.toArray(centries), monitor);
        }
        catch (JavaModelException e) {
            throw new WrapperRuntimeException(e);
        }
        catch (IOException e) {
            throw new WrapperRuntimeException(e);
        }
    }
}
