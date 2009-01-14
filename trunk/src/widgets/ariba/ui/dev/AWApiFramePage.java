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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWApiFramePage.java#6 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWPage;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApi;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.dev.AWApiBodyPage;
import java.util.List;
import java.util.Map;
import ariba.util.core.Assert;

public final class AWApiFramePage extends AWComponent
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------
    public static final String Name = "AWApiFramePage";

    //  <OutlineRepetition list="$rootObjects" item="$^currentObject" children="$^children" hasChildren="$hasChildren" selectionPath="$selectionPath">
    public List _packageNames;

    public String _currentObject;
    protected String _selectedObject;
    protected AWComponentDefinition _selectedComponentDefinition;

    public List _selectionPath = ListUtil.list();

    public boolean isStateless ()
    {
        return false;
    }

    public void init (AWComponentReference componentReference, AWComponent parentComponent, AWPage page)
    {
        super.init(componentReference, parentComponent, page);
        initPackageList();
    }

    protected void initPackageList ()
    {
        _packageNames = AWConcreteApplication.sharedInstance().resourceManager().registeredPackageNames();
        ListUtil.sortStrings(_packageNames, true);
    }

    public AWComponent treePage ()
    {
        AWApiTreePage page = (AWApiTreePage)pageWithName("AWApiTreePage");
        page.setup(this);
        return page;
    }

    public AWComponent bodyPage ()
    {
        AWApiBodyPage page = (AWApiBodyPage)pageWithName("AWApiBodyPage");
        page.setup(this);
        return page;
    }

    ///////////////////////////////
    // Utility Methods
    ///////////////////////////////
    private boolean hasEmpiricalApi (String componentName)
    {
        return (isPackageName(componentName) ? false : AWComponentApiManager.getAWApi(componentName) == null);
    }

    private boolean isPackageName (String name)
    {
        return (name.indexOf('.') != -1);
    }

    ///////////////////////////////
    // Methods for explorer pane (current object)
    ///////////////////////////////

    public List children ()
    {
        return childrenForObject(_currentObject);
    }

    public boolean isCurrentObjectPackage ()
    {
        return isPackageName (_currentObject);
    }

    public boolean currentObjectSelectable ()
    {
        return !isCurrentObjectPackage();
    }

    private List childrenForObject (String _currentObject)
    {
        List result;
        if (isPackageName(_currentObject)) {
            result = AWComponentApiManager.sharedInstance().getSortedComponentList(_currentObject);
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
        return AWComponentApiManager.componentDefinition(_currentObject).referencedByLocations() != null;
    }

    ///////////////////////////////
    // Methods for details pane (selected object)
    ///////////////////////////////

    public String selectedObject ()
    {
        return _selectedObject;
    }

    public void setSelectedObject (String object)
    {
        _selectedObject = object;
        _selectedComponentDefinition = AWComponentApiManager.componentDefinition(_selectedObject);
    }

    public boolean selectedObjectIsComponent ()
    {
        return ((_selectedObject == null) ? false : !isPackageName(_selectedObject));
    }

    public String selectedObjectFileLocation ()
    {
        String fileLocation = null;
        AWFileResource resource =
            (AWFileResource)application().resourceManager().resourceNamed(_selectedComponentDefinition.templateName());

        if (resource != null) {
            fileLocation = resource._fullPath();
        }

        return fileLocation;
    }

    public boolean selectedObjectHasEmpiricalApi ()
    {
        return hasEmpiricalApi(_selectedObject);
    }

    public Map selectedObjectEmpiricalApi ()
    {
        return _selectedComponentDefinition.empiricalApiTable();
    }

    public AWApi selectedObjectAWApi ()
    {
        // _currentObject should be the name of a component
        AWApi api = AWComponentApiManager.getAWApi(_selectedObject);

        Assert.that(api != null,
                    "SelectedObjectAWApi should only be called when an AWApi is available");

        return api;
    }

    public boolean isSelectedObjectPageLevel ()
    {
        return _selectedComponentDefinition.isPageLevel();
    }

    public boolean selectedObjectHasReferences ()
    {
        return _selectedComponentDefinition.referencedByLocations() != null;
    }

    public List selectedObjectReferencedByList ()
    {
        return _selectedComponentDefinition.referencedByLocations();
    }

    ////////////////////////////
    // AWComponent
    ////////////////////////////
    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        // disable scrolling
        page().setPageScrollTop(null);
        page().setPageScrollLeft(null);

        return super.invokeAction(requestContext, component);
    }

}
