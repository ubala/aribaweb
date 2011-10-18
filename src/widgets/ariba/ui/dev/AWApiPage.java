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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWApiPage.java#7 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.*;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.table.AWTDisplayGroup;
import ariba.util.core.ArrayUtil;
import ariba.util.core.Assert;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class AWApiPage extends AWComponent
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------
    public static final String Name = "AWApiPage";
    public static boolean _didLoadAll = false;
    public List _packageNames = ListUtil.list();
    public AWTDisplayGroup _displayGroup;
    public Object _currentObject;
    private Map _selectedTabsByDefinition = MapUtil.map();
    public AWApi inlineApi;
    public Object _currentItem;

    @Override
    public void init ()
    {
        super.init();
        initPackageList();
        _displayGroup = new AWTDisplayGroup();
        _displayGroup.setSelectedObject(_packageNames);
    }

    public Object getSelectedTab ()
    {
        return _selectedTabsByDefinition.get(selectedObjectAWApi());
    }

    public void setSelectedTab (Object selectedTab)
    {
        _selectedTabsByDefinition.put(selectedObjectAWApi(), selectedTab);
    }

    protected void initPackageList ()
    {
//        _packageNames = application().resourceManager().registeredPackageNames();
        _packageNames.clear();

        _packageNames.add("ariba.ui.aribaweb.html");
        _packageNames.add("ariba.ui.aribaweb.core");
//        ListUtil.sortStrings(_packageNames, true);
    }

    void setSelectedComponent (AWComponentDefinition componentDefinition)
    {
        String pkg = AWComponentApiManager.packageNameForComponent(componentDefinition);
        int index = _packageNames.indexOf(pkg);
        if (index != -1) {
            List path = ListUtil.list(_packageNames.get(index), componentDefinition);
            _displayGroup.outlineState().setExpansionPath(path);
            _displayGroup.setSelectedObject(componentDefinition);
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
        List temp = ListUtil.list();
        if (isPackageName(_currentObject)) {
            result = AWComponentApiManager.sharedInstance().getSortedComponentDefinitionList(_currentObject);

            Iterator iterator = result.iterator();
            
            while(iterator.hasNext()) {
                AWComponentDefinition currentCompDefinition = (AWComponentDefinition)iterator.next();

                if (hardcode(currentCompDefinition))
                    temp.add(currentCompDefinition);
            }
        }
        else {
            result = ListUtil.list();
        }

        return temp;
    }

    public boolean hardcode (AWComponentDefinition compDef)
    {
        if ( compDef.componentName().equals("AWTextField")  || compDef.componentName().equals("AWPasswordField")
           || compDef.componentName().equals("AWSingleton"))
            return true;
        else
            return false;
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
            inlineApi = selectedComponentDefinition().componentApi();

            Assert.that(inlineApi != null, "SelectedObjectAWApi should only be called when an AWApi is available");
        
        return inlineApi;
    }

    public List<AWExampleApi> exampleApiList ()
    {
        selectedObjectAWApi();
        List<AWExampleApi> result = ListUtil.list();

        for (AWExampleApi inlineExampleApi : inlineApi.exampleApis()) {
            inlineExampleApi .setIsInline(true);
            result.add(inlineExampleApi );
        }

        for (AWIncludeExample awIncludeExample : inlineApi.includeExamples()) {

            AWApi includeExampleApi = awIncludeExample.exampleComponentApi();

            if (includeExampleApi == null || includeExampleApi.exampleApis() == null) {
                continue;
            }

            for (AWExampleApi importExampleApi : includeExampleApi.exampleApis()) {
                importExampleApi.setIsInline(false);
                importExampleApi._includeExampleName = awIncludeExample.componentName();
                result.add(importExampleApi);
            }
        }

        return result;
    }

    public boolean selectedObjectAWApiHasExamples ()
    {
        if (selectedObjectAWApi() == null) {
            return false;
        }
        return !(ArrayUtil.nullOrEmptyArray(selectedObjectAWApi().exampleApis())
                && ArrayUtil.nullOrEmptyArray(selectedObjectAWApi().includeExamples()));
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
        if (!isPage())
            setSelectedComponent((AWComponentDefinition)valueForBinding("componentDefinition"));
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
