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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/idea/GotoComponent.java#4 $
*/
package ariba.ideplugin.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import java.util.Vector;


public class GotoComponent extends AnAction
{
    private static final Logger LOG = Logger.getInstance(GotoComponent.class.getName());

    /*-----------------------------------------------------------------------
         Implementation of the AnAction interface.
      -----------------------------------------------------------------------*/

    /**
     * Find the file corresponding to an awl tag. The AWL file defining this tag will
     * open.
     * If the file does not exist the java file will open.
     *
     * @param event occurred
     */
    public void actionPerformed (AnActionEvent event)
    {

        LOG.info("User performed GotoComponent!");
        Project project = (Project)event.getDataContext().getData(DataConstants.PROJECT);

        VirtualFile[] selectedFile = FileEditorManager.getInstance(project).
                getSelectedFiles();

        if (selectedFile.length == 0) {
            showError("No file is currently selected", project);
            return;
        }
        VirtualFile currentFile = selectedFile[0];

        // ignore if the file is not awl
        String currentFileExtension = currentFile.getExtension();
        if (!currentFileExtension.equals("awl") && !currentFileExtension.equals("htm")) {
            return;
        }

        Editor editor = (Editor)event.getDataContext().getData(DataConstants.EDITOR);
        ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        Document document = editor.getDocument();
        CaretModel caret = editor.getCaretModel();

        String componentName = findComponentName(document, caret.getOffset());
        if (componentName == null) {
            showError("cannot figure out component name", project);
        }
        else {
            String javaFileName = componentName + ".java";
            String awlFileName = componentName + ".awl";

            // find components
            Vector components = new Vector();
            findComponents(rootManager, javaFileName, awlFileName, components);
            if (components.isEmpty()) {
                javaFileName = nameWithPrefix(componentName, "") + ".java";
                awlFileName = nameWithPrefix(componentName, "") + ".awl";
                findComponents(rootManager, javaFileName, awlFileName, components);
            }
            if (components.isEmpty()) {
                javaFileName = nameWithPrefix(componentName, "AW") + ".java";
                awlFileName = nameWithPrefix(componentName, "AW") + ".awl";
                findComponents(rootManager, javaFileName, awlFileName, components);
            }

            if (components.isEmpty()) {
                javaFileName = nameWithPrefix(componentName, "AWT") + ".java";
                awlFileName = nameWithPrefix(componentName, "AWT") + ".awl";
                findComponents(rootManager, javaFileName, awlFileName, components);
            }

            if (!components.isEmpty()) {
                openFile(project, components);
            }
            else {
                String errorMessage = "cannot find component " + componentName;
                showError(errorMessage, project);
            }
        }
    }

    private void openFile (Project project, Vector components)
    {
        // open the first one by default
        VirtualFile javaFile = (VirtualFile)components.firstElement();
        OpenFileDescriptor fd = new OpenFileDescriptor(project, javaFile);
        Editor newEditor = FileEditorManager.getInstance(project).
                openTextEditor(fd, true);
        if (newEditor == null) {
            showError("Can't open editor", project);
        }
    }

    private void findComponents (ProjectRootManager rootManager, String javaFileName,
                                 String awlFileName, Vector components)
    {

        VirtualFile[] rootDirectories = rootManager.getContentSourceRoots();
        for (int index = 0; index < rootDirectories.length; index++) {
            findJavaFileWithName(javaFileName, awlFileName, rootDirectories[index],
                    components);
        }
    }

    private String nameWithPrefix (String componentName, String prefix)
    {
        // has prefix
        if (componentName.charAt(1) == ':') {
            return prefix.concat(componentName.substring(2));

        }
        return componentName;
    }

    /*-----------------------------------------------------------------------
         Private Methods.
      -----------------------------------------------------------------------*/

    /**
     * Look for the name of the awl tag on which the pointer is
     *
     * @param document, current document
     * @param offset    in the document
     * @return the name of the tag
     */
    private String findComponentName (Document document, int offset)
    {
        char[] text = document.getChars();
        // search forward until "<" or " " is found
        int startIndex = -1;
        int endIndex = -1;
        for (int index = offset; index >= 0; index--) {
            char eachChar = text[index];
            if (Character.isWhitespace(eachChar) || eachChar == '<' || eachChar == '/') {
                startIndex = index + 1;
                break;
            }
        }

        for (int index = startIndex; index < text.length; index++) {
            char eachChar = text[index];
            if (Character.isWhitespace(eachChar) || eachChar == '>' || eachChar == '/') {
                endIndex = index - 1;
                break;
            }
        }
        int length = endIndex - startIndex + 1;
        if (length > 0) {
            char[] name = new char[length];
            System.arraycopy(text, startIndex, name, 0, length);
            return new String(name);
        }
        return null;
    }

    /**
     * This function look for the file awlFileName and put it into results,
     * if it does not exist put the java file corresponding into result
     *
     * @param javaFileName name of the java file
     * @param awlFileName  name of the awl file
     * @param file,        root directories
     * @param results,     the name of the files
     */
    private void findJavaFileWithName (String javaFileName, String awlFileName,
                                       VirtualFile file, Vector results)
    {
        if (results.isEmpty()) {
            if (file.isDirectory()) {
                VirtualFile[] children = file.getChildren();
                for (int index = 0; index < children.length; index++) {
                    findJavaFileWithName(javaFileName, awlFileName, children[index],
                            results);
                }
            }
            else if (file.getName().equals(javaFileName)) {
                VirtualFile awlFile = file.getParent().findChild(awlFileName);
                if (awlFile != null) {
                    results.insertElementAt(awlFile, 0);
                }
                else {
                    results.addElement(file);
                }
            }
        }
    }

    /**
     * Display an error into a popup window
     *
     * @param project, current project
     * @param error,   the message to display
     */
    private void showError (String error, Project project)
    {
        Messages.showMessageDialog(project, error, "Goto Component",
                Messages.getInformationIcon());
        LOG.info(error);
    }
}
