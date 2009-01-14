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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWValidationErrorPage.java#10 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWValidationContext;
import ariba.ui.aribaweb.core.AWValidationContext.ValidationError;
import ariba.ui.outline.OutlineState;

import java.util.List;
import java.util.Map;

public final class AWValidationErrorPage extends AWComponent
{
    //----------------------------------------------------------------------
    //  Constants
    //----------------------------------------------------------------------
    public static final String Name = "AWValidationErrorPage";

    public boolean isStateless ()
    {
        return true;
    }

    //  <OutlineRepetition list="$rootObjects" item="$^currentObject" children="$^children" hasChildren="$hasChildren" selectionPath="$selectionPath">
    private List _packageNames;
    private List _currentErrorsPackageNames;
    public OutlineState _outlineState;
    public String _currentObject;
    public String _currentGeneralErrorKey;
    private List _selectedObjectPackageErrorList = null;

    // Validation Context for the "current" error page
    private AWValidationContext _validationContext;
    private boolean _isCurrentErrorsView;
    // the name of the page that corresponds to "currentErrorsView"
    private String _currentErrorPageName;


    public void init ()
    {
        super.init();

        _packageNames = ListUtil.list();
        _outlineState = new OutlineState();

        List packageNames = application().resourceManager().registeredPackageNames();

        // filter package names
        for (int i = 0, size = packageNames.size(); i < size; i++) {
            String packageName = (String) packageNames.get(i);

            List children = childrenForPackage(packageName);
            boolean errorFound = false;
            for (int j = 0, childrensize = children.size(); j < childrensize && !errorFound; j++) {
                String child = (String) children.get(j);
                errorFound = (AWComponentApiManager.getValidationErrorCount(child) > 0 ||
                              AWComponentApiManager.getComponentApiErrorCount(child) > 0 );
            }
            if (errorFound) {
                addToUniqueSortedList(_packageNames, packageName);
            }
        }
    }

    private void addToUniqueSortedList (List list, String element)
    {
        boolean inserted = false;
        for (int i=0,size=list.size(); !inserted && i<size; i++) {
            String curr = (String) list.get(i);
            if (curr.compareTo(element) > 0) {
                list.add(i,element);
                inserted = true;
            }
            else if (curr.compareTo(element) == 0) {
                inserted = true;
            }

        }
        if (!inserted) {
            list.add(element);
        }
    }

    ///////////////////////////////
    // Utility Methods
    ///////////////////////////////
    private boolean isPackageName (String name)
    {
        return (name.indexOf('.') != -1);
    }

    ///////////////////////////////
    // Methods for explorer pane (current object)
    ///////////////////////////////

    public List packageNames ()
    {
        List vReturn = null;
        if (_isCurrentErrorsView) {
            vReturn = currentErrorsPackageNames();
        }
        else {
            vReturn = _packageNames;
        }
        return vReturn;
    }

    // todo: remove this when no longer referenced ... find the right widget to use for package list
    public List children ()
    {
        return null;
    }

    private Map _childrenCache;
    private Map childrenCache ()
    {
        if (_childrenCache == null) {
            _childrenCache = MapUtil.map();
        }
        return _childrenCache;
    }

    private List childrenForPackage (String packageName)
    {
        List result = null;

        if (_isCurrentErrorsView) {
            result = _validationContext.componentsWithErrors(packageName);
        }
        else {
            Map cache = childrenCache();
            result = (List)cache.get(packageName);
            if (result == null) {
                result = AWComponentApiManager.sharedInstance().componentsWithErrors(packageName, true, true);
                cache.put(packageName, result);
            }
        }

        return result;
    }

    public boolean hasChildren ()
    {
        return children().size() != 0;
    }

    public String currentObjectTitle ()
    {
        StringBuffer sbReturn = new StringBuffer();
        sbReturn.append(_currentObject);
        int bindingErrors = 0;
        int apiErrors = 0;
        if (isPackageName(_currentObject)) {
            // title for a package
            // append sum of all childrens errors
            List children = childrenForPackage(_currentObject);
            for (int i = 0, childrensize = children.size(); i < childrensize; i++) {
                String child = (String) children.get(i);
                bindingErrors += AWComponentApiManager.getValidationErrorCount(child);
                apiErrors += AWComponentApiManager.getComponentApiErrorCount(child);
            }
        }
        else {
            // title for a component
            // append sum of errors
            bindingErrors = AWComponentApiManager.getValidationErrorCount(_currentObject);
            apiErrors = AWComponentApiManager.getComponentApiErrorCount(_currentObject);
        }
        sbReturn.append("(").append(bindingErrors).append("/").append(apiErrors).append(")");

        return sbReturn.toString();
    }

    ///////////////////////////////
    // Methods for details pane (selected object)
    ///////////////////////////////

    public String selectedObject ()
    {
        return (String)_outlineState.selectedObject();
    }

    public void setSelectedObject (String object)
    {
        _selectedObjectPackageErrorList = null;
    }

    //----------------------------
    // error filter selection
    //----------------------------
    public String selectedViewLabel ()
    {
        String sReturn = null;
        if (_isCurrentErrorsView) {
            sReturn = "Errors for " + _currentErrorPageName;
        }
        else {
            sReturn = "All Errors";
        }

        return sReturn;
    }

    private List currentErrorsPackageNames ()
    {
        if (_currentErrorsPackageNames == null) {
            _currentErrorsPackageNames = _validationContext.packagesWithErrors();
        }
        return _currentErrorsPackageNames;
    }

    public AWComponent setCurrentErrorsViewAction ()
    {
        _isCurrentErrorsView = true;
        _selectedObjectPackageErrorList = null;

        return null;
    }

    public AWComponent setAllErrorsViewAction ()
    {
        _isCurrentErrorsView = false;
        _selectedObjectPackageErrorList = null;

        return null;
    }

    public boolean isShowCurrentErrorsView ()
    {
        return _isCurrentErrorsView;
    }
    public boolean isShowAllErrorsView ()
    {
        return !_isCurrentErrorsView;
    }

    public boolean isCurrentErrorsViewEnabled ()
    {
        return (_validationContext != null);
    }

    //----------------------------
    // for package detals view
    //----------------------------
    public final class ComponentNameError
    {
        public String componentName;
        public String error;
        public ComponentNameError(String name, String err)
        {
            componentName = name;
            error = err;
        }
    }

    private void addUnsupportedBindingDefinitions (List componentNameErrorList, List bindingList, String componentName)
    {
        if (bindingList == null) {
            return;
        }
        for (int i = 0, size = bindingList.size(); i < size; i++) {
            componentNameErrorList.add(new ComponentNameError(componentName,
                "AWApi: Binding declared in Api but not defined in the components supported binding list: <b>" + (String)bindingList.get(i) + "</b>"));
        }

    }

    private void addMissingSupportedBindingDefinitions (List componentNameErrorList, List bindingList, String componentName)
    {
        if (bindingList == null) {
            return;
        }
        for (int i = 0, size = bindingList.size(); i < size; i++) {
            componentNameErrorList.add(new ComponentNameError(componentName,"AWApi: Missing documentation for supported binding: <b>" + (String)bindingList.get(i) + "</b>"));
        }
    }

    private void addInvalidBindingAlternates (List componentNameErrorList, List bindingPairList, String componentName)
    {
        if (bindingPairList == null) {
            return;
        }
        for (int i = 0, size = bindingPairList.size(); i < size; i++) {
            String[] bindingPair = (String[]) bindingPairList.get(i);

            componentNameErrorList.add(new ComponentNameError(componentName,
                "AWApi: Unable to find definition for binding <b>" + bindingPair[1] +
                "</b> specified in alternates for bindings: <b>" + bindingPair[0] + "</b>"));
        }
    }

    private void addMismatchedBindingAlternates (List componentNameErrorList, List bindingPairList, String componentName)
    {
        if (bindingPairList == null) {
            return;
        }
        for (int i = 0, size = bindingPairList.size(); i < size; i++) {
            String[] bindingPair = (String[]) bindingPairList.get(i);

            componentNameErrorList.add(new ComponentNameError(componentName,
                "AWApi: Mismatched alternates defined for bindings: <b>" + bindingPair[1] +
                "</b>, <b>" + bindingPair[0] + "</b>"));
        }
    }

    private void addToComponentNameErrorList (List componentNameErrorList, List errorStringList, String componentName)
    {
        if (errorStringList == null) {
            return;
        }
        for (int i = 0, size = errorStringList.size(); i < size; i++) {
            String bindingError = (String) errorStringList.get(i);
            componentNameErrorList.add(new ComponentNameError(componentName,bindingError));
        }
    }

    public List selectedObjectPackageErrorList ()
    {
        if (_selectedObjectPackageErrorList == null) {
            _selectedObjectPackageErrorList = ListUtil.list();
            if (selectedObject() != null) {
                List components = childrenForPackage(selectedObject());
                for (int i = 0, size = components.size(); i < size; i++) {
                    String componentName = (String) components.get(i);
                    AWComponentDefinition componentDefinition = AWComponentApiManager.componentDefinition(componentName);
                    if (componentDefinition.validationErrorCount() != 0) {
                        addToComponentNameErrorList(_selectedObjectPackageErrorList,componentDefinition.bindingErrorList(),componentName);
                    }
                    if (componentDefinition.componentApiErrorCount() != 0) {
                        addUnsupportedBindingDefinitions(_selectedObjectPackageErrorList,componentDefinition.unsupportedBindingDefinitions(),componentName);
                        addMissingSupportedBindingDefinitions(_selectedObjectPackageErrorList,componentDefinition.missingSupportedBindingDefinitions(),componentName);
                        addInvalidBindingAlternates(_selectedObjectPackageErrorList,componentDefinition.invalidComponentBindingApiAlternates(), componentName);
                        addMismatchedBindingAlternates(_selectedObjectPackageErrorList,componentDefinition.mismatchedComponentBindingApiAlternates(), componentName);
                        addToComponentNameErrorList(_selectedObjectPackageErrorList,componentDefinition.templateParsingErrors(),componentName);
                    }
                }
            }
        }
        return _selectedObjectPackageErrorList;
    }

    /**
        @aribaapi ariba
    */
    public AWValidationContext context ()
    {
        return _validationContext;
    }

    /**
        @aribaapi ariba
    */
    public List<ValidationError> getGeneralErrorsForCurrentKey ()
    {
        return _validationContext.getGeneralErrorsFor(_currentGeneralErrorKey);
    }

    /**
        @aribaapi ariba
    */
    public List<String> getGeneralErrors ()
    {
        return _validationContext.getGeneralErrors();
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

    /**
     * Setup for case when a Validation Context for the "current" error page is available.
     *
     * @param validationContext
     * @param pageName
     */
    public void setup (AWValidationContext validationContext, String pageName, String packageName)
    {
        _validationContext = validationContext;
        _currentErrorPageName = pageName;
        _isCurrentErrorsView = (packageName != null);
        _currentErrorsPackageNames = null;

        // pre-select the provided package/page
        if (packageName != null) {
            List path = ListUtil.list();
            path.add(packageName);
            _outlineState.setExpansionPath(path);
            _outlineState.setSelectedObject(packageName);
        }
    }
}
