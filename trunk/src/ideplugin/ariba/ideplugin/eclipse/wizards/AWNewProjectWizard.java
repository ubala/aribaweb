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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/wizards/AWNewProjectWizard.java#3 $
*/
package ariba.ideplugin.eclipse.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.jface.viewers.ISelection;
import ariba.ideplugin.core.WrapperRuntimeException;
import ariba.ideplugin.core.AWScriptRunner;
import ariba.ideplugin.eclipse.Activator;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class AWNewProjectWizard extends Wizard implements INewWizard
{
    private AWProjectTemplatePage _page1;
    private AWProjectDetailsPage _page2;
    private ISelection selection;
    List<Map> _templates;
    AWScriptRunner _connector;

    public AWNewProjectWizard ()
    {
        super();
        setNeedsProgressMonitor(true);

        try {
            _connector = new AWScriptRunner(Activator.getDefault().getAWHome());
            _templates = _connector.loadTemplates();
        }
        catch (WrapperRuntimeException ae) {
            Status status = new Status(IStatus.ERROR, Activator.getDefault()
                .getBundle().getSymbolicName(), 0, ae.getMessage(), ae);
            ErrorDialog.openError(getShell(), null, null, status);
        }
    }

    public void addPages ()
    {
        if (_connector != null) {
            _page1 = new AWProjectTemplatePage(selection, _templates);
            addPage(_page1);
            _page2 = new AWProjectDetailsPage(selection);
            addPage(_page2);
        }
    }

    public IWizardPage getNextPage(IWizardPage page) {
        if (page == _page1) {
            _page2 = new AWProjectDetailsPage(selection);
            _page2.setWizard(this);
            return _page2;
        }
        return super.getNextPage(page);
    }

    public Map getSelectedTemplate ()
    {
        return _page1.getSelectedTemplate();
    }

    public List getRequiredData ()
    {
        return (List)(_page1.getSelectedTemplate()).get("parameters");
    }

    public boolean canFinish ()
    {
        return _page2.isVisible();
    }

    public boolean performFinish ()
    {
        String msg = _page2.validate();
        if (msg != null) {
            Status status = new Status(IStatus.ERROR, Activator.getDefault()
                .getBundle().getSymbolicName(), 0, msg, null);
            ErrorDialog.openError(getShell(), null, null, status);
            return false;
        }
        CreateProject cproj = new CreateProject(
                      getShell(), 
                      _connector, 
                      _page2.getProjectName(), 
                      (File)_page1.getSelectedTemplate().get(AWScriptRunner.TemplateDirKey), 
                      _page2.getParameterMap());
        ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
        try {
            pmd.run(true, false, cproj);
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    public void init (IWorkbench workbench, IStructuredSelection selection)
    {
        this.selection = selection;
    }
}