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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/wizards/AWProjectDetailsPage.java#2 $
*/
package ariba.ideplugin.eclipse.wizards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AWProjectDetailsPage extends WizardPage
{
    final int _EditableColumn = 1;
    private ISelection _selection;
    private String _currProject;
    private List _data;
    private HashMap<String, Text> _values;
    private Text _projectName;
    private boolean _visible;
    private IWorkspaceRoot _wroot;

    protected AWProjectDetailsPage (ISelection selection)
    {
        super("detailsPage");
        setTitle("Project Details Page");
        setDescription("Please fill in the details to set up the AW project");

        this._selection = selection;

        _wroot = ResourcesPlugin.getWorkspace().getRoot();
    }

    public void createControl (Composite parent)
    {
        _data =((AWNewProjectWizard)getWizard()).getRequiredData();

        Composite container = new Composite(parent, SWT.NULL);
        GridLayout glayout = new GridLayout();
        glayout.numColumns = 2;
        container.setLayout(glayout);
        GridData gdata = new GridData();

        Label label = new Label(container, SWT.LEFT);
        label.setText("Project name");
        gdata.horizontalAlignment = GridData.FILL;
        label.setLayoutData(gdata);

        _projectName = new Text(container, SWT.SINGLE | SWT.BORDER);
        gdata = new GridData();
        gdata.verticalIndent = 5;
        gdata.horizontalAlignment = GridData.FILL;
        gdata.grabExcessHorizontalSpace = true;
        _projectName.setLayoutData(gdata);
        _values = new HashMap<String, Text>();

        for(int i = 0; i<_data.size();i++){
            Map entry = (Map)_data.get(i);
            String slabel = (String) entry.get("description");
            String dval = (String)entry.get("default");

            Label slab = new Label(container, SWT.LEFT | SWT.WRAP);
            slab.setText(slabel);
            
            Text txt = new Text(container, SWT.SINGLE | SWT.BORDER);
            txt.setText(dval);
            gdata = new GridData();
            gdata.horizontalAlignment = GridData.FILL;
            gdata.grabExcessHorizontalSpace = true;
            txt.setLayoutData(gdata);
            
            _values.put((String)entry.get("key"), txt);
        }

        initialize();
        setControl(container);
    }

    private void initialize ()
    {
        if (_selection != null && _selection.isEmpty() == false
            && _selection instanceof IStructuredSelection)
        {
            IStructuredSelection ssel = (IStructuredSelection)_selection;
            if (ssel.size() > 1)
                return;
            Object obj = ssel.getFirstElement();
            if (obj instanceof IResource) {
                IContainer container;
                if (obj instanceof IContainer)
                    container = (IContainer)obj;
                else
                    container = ((IResource)obj).getParent();
            }
        }
    }

    public List getData ()
    {
        return _data;
    }

    public Map<String, String> getParameterMap ()
    {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < _data.size(); i++) {
            Map mdata = (Map)_data.get(i);
            String key = (String)mdata.get("key");
            map.put(key, _values.get(key).getText());
        }
        return map;
    }

    public String getProjectName ()
    {
        return _projectName.getText();
    }

    public void setVisible (boolean visible)
    {
        _visible = visible;
        super.setVisible(visible);
    }

    public boolean isVisible ()
    {
        return _visible;
    }

    public String validate ()
    {
        String pname = getProjectName();
        if (pname.length() == 0) {
            return "Enter Project Name";
        }
        else if (_wroot.findMember(pname) != null) {
            return "Project with same name already exists";
        }
        return null;
    }
}
