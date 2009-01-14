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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/ToggleAction.java#4 $
*/
package ariba.ideplugin.idea;


import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.AnActionEvent;


public class ToggleAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(ToggleAction.class.getName());

    /*-----------------------------------------------------------------------
         Implementation of the AnAction interface.
      -----------------------------------------------------------------------*/
    /**
     * Switch between an awl page and the corresponding java page.
     * @param event occurred
     */
    public void actionPerformed (AnActionEvent event)
    {
        LOG.info("User performed ToggleAction!");
        Project project = (Project)
        event.getDataContext().getData(DataConstants.PROJECT);

        if (project != null) {

            VirtualFile[] selectedFile =  FileEditorManager.getInstance(project).
                                                            getSelectedFiles();

            if (selectedFile.length == 0) {
                error(project, "No file is currently selected");
                return;
            }
            VirtualFile currEditorFile = selectedFile[0];

            if (currEditorFile != null) {

                String extension = currEditorFile.getExtension();

                String pairName;
                VirtualFile pairFile;

                if ("awl".equals(extension) || "htm".equals(extension)) {
                    pairName = currEditorFile.getNameWithoutExtension() + ".java";
                    pairFile = currEditorFile.getParent().findChild(pairName);

                    if (pairFile == null) {
                        pairName = currEditorFile.getNameWithoutExtension() + ".groovy";
                        pairFile = currEditorFile.getParent().findChild(pairName);
                    }

                    if (pairFile == null) {
                        error(project, "Could not find .java or .groovy file for " +
                              currEditorFile.getName());
                        return;
                    }
                }
                else if ("java".equals(extension) || "groovy".equals(extension)) {
                    pairName = currEditorFile.getNameWithoutExtension() + ".awl";
                    pairFile = currEditorFile.getParent().findChild(pairName);

                    if (pairFile == null) {
                        pairName = currEditorFile.getNameWithoutExtension() + ".htm";
                        pairFile = currEditorFile.getParent().findChild(pairName);
                    }

                    if (pairFile == null) {
                        error(project, "Could not find .awl or .htm file for " +
                                       currEditorFile.getName());
                        return;
                    }
                }
                else {
                    error(project, "Unexpected file type: " + extension);
                    return;
                }

                OpenFileDescriptor fd = new OpenFileDescriptor(project, pairFile);

                if (fd != null) {
                    FileEditorManager manager = FileEditorManager.
                            getInstance(project);
                    Editor e = manager.openTextEditor(fd, true);
                    if (e == null) {
                        error(project,"Editor is null");
                    }
                }
                else {
                    error(project,"Unable to open file: " + pairName);
                }
            }
            else {
                error(project,"No file is currently selected");
            }
        }
    }

    /*-----------------------------------------------------------------------
         Private Methods
      -----------------------------------------------------------------------*/

    /**
     * Display an error into a popup window
     * @param project, current project
     * @param msg, the message to display
     */
    private void error (Project project, String msg)
    {
        Messages.showMessageDialog(project,
                                   msg,
                                   "Toggle AW Information",
                                   Messages.getInformationIcon());
        LOG.info("No file is currently selected");
    }
}
