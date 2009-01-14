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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWApiPage.java#6 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWApi;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.table.AWTDisplayGroup;

import java.util.List;
import ariba.util.core.Assert;
import java.util.Map;

public final class AWApiPage extends AWComponent
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------
    public static final String Name = "AWApiPage";
    public static boolean _didLoadAll = false;
    public List _packageNames;
    public AWTDisplayGroup _displayGroup;
    public Object _currentObject;

    public void init ()
    {
        super.init();
        initPackageList();
        _displayGroup = new AWTDisplayGroup();
        _displayGroup.setSelectedObject(_packageNames);
    }

    protected void initPackageList ()
    {
        _packageNames = application().resourceManager().registeredPackageNames();
        ListUtil.sortStrings(_packageNames, true);
    }

    void setSelectedComponent (AWComponentDefinition componentDefintion)
    {
        String pkg = AWComponentApiManager.packageNameForComponent(componentDefintion);
        int idx = _packageNames.indexOf(pkg);
        if (idx != -1) {
            List path = ListUtil.list(_packageNames.get(idx), componentDefintion);
            _displayGroup.outlineState().setExpansionPath(path);
            _displayGroup.setSelectedObject(componentDefintion);
        }
    }

    ///////////////////////////////
    // Utility Methods
    ///////////////////////////////
    private boolean hasEmpiricalApi (Object node)
    {
        return !isPackageName(node)
                && (((AWComponentDefinition)node).componentApi() == null);
    }

    private boolean isPackageName (Object o)
    {
        return o instanceof String;
    }

    ///////////////////////////////
    // Methods for explorer pane (current object)
    ///////////////////////////////

    public List children ()
    {
        return isCurrentObjectPackage() ? childrenForObject((String)_currentObject) : ListUtil.list();
    }

    public boolean isCurrentObjectPackage ()
    {
        return isPackageName(_currentObject);
    }

    public boolean currentObjectSelectable ()
    {
        return !isCurrentObjectPackage();
    }

    private List childrenForObject (String _currentObject)
    {
        List result;
        if (isPackageName(_currentObject)) {
            result = AWComponentApiManager.sharedInstance().getSortedComponentDefinitionList(_currentObject);
        }
        else {
            result = ListUtil.list();
        }
        return result;
    }

    public boolean hasChildren ()
    {
        return children().size() != 0;
    }

    public boolean currentObjectHasEmpiricalApi ()
    {
        return hasEmpiricalApi(_currentObject);
    }

    public boolean currentObjectHasReferences ()
    {
        return ((AWComponentDefinition)_currentObject).referencedByLocations() != null;
    }

    public void loadAllTemplates ()
    {
        _didLoadAll = true;
        ((AWConcreteApplication)application()).preinstantiateAllComponents(true, requestContext());
        initPackageList();
    }

    public boolean didLoadAll ()
    {
        return _didLoadAll;
    }

    ///////////////////////////////
    // Methods for details pane (selected object)
    ///////////////////////////////

    public Object selectedObject ()
    {
        return _displayGroup.selectedObject();
    }

    public AWComponentDefinition selectedComponentDefinition ()
    {
        return selectedObjectIsComponent() ? (AWComponentDefinition)selectedObject() : null;
    }

    public boolean selectedObjectIsComponent ()
    {
        Object selectedObject = selectedObject();
        return (selectedObject != null) && (selectedObject instanceof AWComponentDefinition);
    }

    public String selectedObjectFileLocation ()
    {
        String fileLocation = null;
        AWFileResource resource =
            (AWFileResource)application().resourceManager().resourceNamed(selectedComponentDefinition().templateName());

        if (resource != null) {
            fileLocation = resource._fullPath();
        }

        return fileLocation;
    }

    public boolean selectedObjectHasEmpiricalApi ()
    {
        return hasEmpiricalApi(selectedObject());
    }

    public Map selectedObjectEmpiricalApi ()
    {
        return selectedComponentDefinition().empiricalApiTable();
    }

    public AWApi selectedObjectAWApi ()
    {
        // _currentObject should be the name of a component
        AWApi api = selectedComponentDefinition().componentApi();

        Assert.that(api != null,
                    "SelectedObjectAWApi should only be called when an AWApi is available");

        return api;
    }

    public boolean isSelectedObjectPageLevel ()
    {
        return selectedComponentDefinition().isPageLevel();
    }

    public boolean selectedObjectHasReferences ()
    {
        return selectedComponentDefinition().referencedByLocations() != null;
    }

    public List selectedObjectReferencedByList ()
    {
        return selectedComponentDefinition().referencedByLocations();
    }

    ////////////////////////////
    // AWComponent
    ////////////////////////////

    public boolean isStateless() {
        return false;
    }

    public boolean isPage ()
    {
        return pageComponent() == this;
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        // disable scrolling
        page().setPageScrollTop(null);
        page().setPageScrollLeft(null);

        return super.invokeAction(requestContext, component);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        if (!isPage()) setSelectedComponent((AWComponentDefinition)valueForBinding("componentDefinition"));
        super.renderResponse(requestContext, component);
    }

    public AWComponent openValidationErrorPage ()
    {
        // always force a reload-all
        loadAllTemplates();

        AWValidationErrorPage errorPage = (AWValidationErrorPage)pageWithName(AWValidationErrorPage.Name);
        errorPage.setup(page().validationContext(), null, null);
        return errorPage;
    }
}
