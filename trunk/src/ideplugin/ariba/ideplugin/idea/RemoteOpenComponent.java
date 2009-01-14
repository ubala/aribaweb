package ariba.ideplugin.idea;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;

import java.awt.*;

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
            PsiFile[] files =
                PsiManager.getInstance(_project).getShortNamesCache().getFilesByName(shortName);
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

}
