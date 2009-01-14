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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/wizards/AWProjectDetailsPage.java#1 $
*/
package ariba.ideplugin.eclipse.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class AWProjectDetailsPage extends WizardPage
{
    final int _EditableColumn = 1;
    private ISelection _selection;
    private String _currProject;
    private List _data;
    private TableViewer _tableViewer;
    private TableEditor _tableEditor;
    private Table _table;
    private Text _projectName;
    private ArrayList<TableItem> _items;
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

        _tableViewer = new TableViewer(container, SWT.SINGLE);
        _table = _tableViewer.getTable();

        _table.setHeaderVisible(true);
        _table.setLinesVisible(true);
        gdata = new GridData(GridData.FILL_BOTH);
        gdata.horizontalSpan = 2;
        _table.setLayoutData(gdata);

        _table.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected (SelectionEvent e)
            {
                rowSelection(e);
            }
        });
        TableColumn col1 = new TableColumn(_table, SWT.NULL);
        col1.setText("Name");
        col1.setWidth(200);
        TableColumn col2 = new TableColumn(_table, SWT.NULL);
        col2.setText("Value");
        col2.setWidth(250);

        _tableViewer.setLabelProvider(new TableLabelProvider());
        _tableViewer.setContentProvider(new ArrayContentProvider());

        _tableEditor = new TableEditor(_table);
        _tableEditor.horizontalAlignment = SWT.LEFT;
        _tableEditor.grabHorizontal = true;
        _tableEditor.minimumWidth = 50;

        initialize();
        setControl(container);
    }

    private void rowSelection (SelectionEvent e)
    {
        Control oldEditor = _tableEditor.getEditor();
        if (oldEditor != null)
            oldEditor.dispose();

        // Identify the selected row
        TableItem item = (TableItem)e.item;
        if (item == null)
            return;

        // The control that will be the editor must be a child of the Table
        Text newEditor = new Text(_table, SWT.NONE);
        newEditor.setText(item.getText(_EditableColumn));
        final int currRow = _table.getSelectionIndex();
        newEditor.addModifyListener(new ModifyListener() {
            public void modifyText (ModifyEvent e)
            {
                Text text = (Text)_tableEditor.getEditor();
                _tableEditor.getItem().setText(_EditableColumn, text.getText());
                ((Map)_data.get(currRow)).put("default", text.getText());
            }
        });
        newEditor.selectAll();
        newEditor.setFocus();
        _tableEditor.setEditor(newEditor, item, _EditableColumn);
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
            map.put((String)mdata.get("key"), (String)mdata.get("default"));
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
        if (visible) {
            AWNewProjectWizard wiz = (AWNewProjectWizard)getWizard();
            if (_currProject == null
                || !_currProject.equals(wiz.getSelectedTemplate()))
            {
                _table.removeAll();
                _data = wiz.getRequiredData();
                _tableViewer.setInput(_data.toArray());
            }
        }
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

    class TableLabelProvider extends LabelProvider implements
        ITableLabelProvider
    {
        public String getColumnText (Object element, int index)
        {
            switch (index) {
            case 0:
                return (String)((Map)element).get("description");
            case 1:
                return (String)((Map)element).get("default");
            }
            return "Unknown: " + index;
        }

        public Image getColumnImage (Object element, int columnIndex)
        {
            return null;
        }
    }
}
