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

    $Id: //ariba/platform/ui/ideplugin/ariba/ideplugin/eclipse/wizards/AWProjectTemplatePage.java#1 $
*/
package ariba.ideplugin.eclipse.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;


public class AWProjectTemplatePage extends WizardPage
{

    private Text _description;
    private List _list;
    private ISelection _selection;
    private java.util.List<Map> _templates;

    /**
     * Constructor for AWProjectTemplatePage
     * 
     * @param pageName
     */
    public AWProjectTemplatePage (ISelection selection,
                                  java.util.List<Map> templates)
    {
        super("wizardPage");
        setTitle("Project template");
        setDescription("Please select a template below.");
        this._selection = selection;

        _templates = templates;
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl (Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 2;
        layout.verticalSpacing = 9;

        _list = new List(container, SWT.BORDER | SWT.SINGLE);
        String[] st = new String[_templates.size()];
        for (int i = 0; i < _templates.size(); i++) {
            st[i] = (String)_templates.get(i).get("title");
        }
        _list.setItems(st);
        _list.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected (SelectionEvent se)
            {
                _description.setText((String)_templates
                    .get(_list.getSelectionIndex()).get("description"));
            }

            public void widgetDefaultSelected (SelectionEvent se)
            {
                // TODO: should click next
            }
        });
        _list.setSelection(0);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.FILL_VERTICAL);
        _list.setLayoutData(gd);

        _description = new Text(container, SWT.BORDER | SWT.MULTI);
        _description.setText((String)_templates.get(0).get("description"));
        gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.FILL_VERTICAL);
        _description.setLayoutData(gd);

        initialize();
        setControl(container);
    }

    public Map getSelectedTemplate ()
    {
        return _templates.get(_list.getSelectionIndex());
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */

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
}