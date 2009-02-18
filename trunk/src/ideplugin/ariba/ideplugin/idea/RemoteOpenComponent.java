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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/RemoteOpenComponent.java#7 $
*/
package ariba.ideplugin.idea;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import ariba.ideplugin.core.RemoteOpen;

public class RemoteOpenComponent implements ProjectComponent, RemoteOpen.Opener
{

    private Project _project;
    private RemoteOpen _remoteOpen;

    public RemoteOpenComponent(Project project)
    {
        _project = project;
        _remoteOpen = new RemoteOpen(this);
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    public String getComponentName() {
        return "RemoteOpenComponent";
    }

    public void projectOpened() {
        _remoteOpen.start();
    }

    public void projectClosed() {
        _remoteOpen.stop();
    }

    public boolean open (String name, int line) {
        line = line > 0 ?  line - 1 : 0;
        FileOpenAction action = new FileOpenAction(_project, name, line);
        ApplicationManager.getApplication().invokeLater(action);
        return true;
    }

    class FileOpenAction implements Runnable
    {
        Project _project;
        String _name;
        int _line;

        public FileOpenAction (Project project, String name, int line)
        {
            _project = project;
            _name = name;
            _line = line;
        }

        public void run () {
            String shortName = _name.substring(_name.lastIndexOf("/") + 1);
            PsiFile[] files = filesByShortName(_project, shortName);
            // PsiManager.getInstance(_project).getShortNamesCache().getFilesByName(shortName);
            // FilenameIndex.getFilesByName(_project, shortName, ProjectScope.getProjectScope(_project))
            
            if (files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    VirtualFile file = files[i].getVirtualFile();
                    if (file.getPath().endsWith(_name)) {
                        OpenFileDescriptor desc = new OpenFileDescriptor(_project, file, _line, 0);
                        Editor editor = FileEditorManager.getInstance(_project).openTextEditor(desc, true);
                        LogicalPosition position = new LogicalPosition(_line, 0);
                        editor.getCaretModel().moveToLogicalPosition(position);
                        editor.getScrollingModel().scrollTo(position, ScrollType.CENTER);
                        Window window = WindowManager.getInstance().suggestParentWindow(_project);
                        window.setAlwaysOnTop(true);
                        window.setAlwaysOnTop(false);
                        break;
                    }
                }
            }
        }
    }

    // Due to API changes between IDEA 7 and 8, need to use reflection to do lookup
    private static final int IDEA_8_0 = 8000;
    static PsiFile[] filesByShortName (Project project, String filePath) {
        PsiFile[] psiFiles = null;
        String ver = ApplicationInfo.getInstance().getBuildNumber();
        int v = Integer.parseInt(ver);
        boolean isIdea8 = v > IDEA_8_0;
        PsiClass cls = null;
        try {
            if (isIdea8) {
				Class filenameIndexClass = Class.forName("com.intellij.psi.search.FilenameIndex");
                Method getFilesByName = filenameIndexClass.getMethod("getFilesByName", Project.class,
                        String.class, GlobalSearchScope.class);
				Class projectScopeClass = Class.forName("com.intellij.psi.search.ProjectScope");
                Method getProjectScope = projectScopeClass.getMethod("getProjectScope", Project.class);
                GlobalSearchScope scope = (GlobalSearchScope) getProjectScope.invoke(null, project);
                psiFiles = (PsiFile[]) getFilesByName.invoke(null, project, filePath, scope);
            } else {
				Class psiManagerClass = Class.forName("com.intellij.psi.PsiManager");
                Method getInstance = psiManagerClass.getMethod("getInstance", Project.class);
                Object inst = getInstance.invoke(null, project);
                Method getShortNamesCache = psiManagerClass.getMethod("getShortNamesCache");
				Class psiShortNamesCacheClass = Class.forName("com.intellij.psi.search.PsiShortNamesCache");
                Object cacheInstance = getShortNamesCache.invoke(inst);
                Method getFilesByName = psiShortNamesCacheClass.getMethod("getFilesByName", String.class);
                psiFiles = (PsiFile[]) getFilesByName.invoke(cacheInstance, filePath);
            }
        } catch (ClassNotFoundException e) {
   		    e.printStackTrace();
        } catch (NoSuchMethodException e) {
   		    e.printStackTrace();
        } catch (IllegalAccessException e) {
   		    e.printStackTrace();
        } catch (InvocationTargetException e) {
   		    e.printStackTrace();
        }
        return psiFiles;
    }

}
