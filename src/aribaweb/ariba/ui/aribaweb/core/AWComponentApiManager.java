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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWComponentApiManager.java#14 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import java.util.Map;
import java.util.List;
import ariba.util.core.GrowOnlyHashtable;

public class AWComponentApiManager extends AWBaseObject
{
    private static AWComponentApiManager _componentApiManager;

    private Map _componentApiPackageTable;
    private Map _missingComponentApiPackageTable;

    private GrowOnlyHashtable _componentPackageTable;


    ///////////////////////
    // Constructor
    ///////////////////////
    public static AWComponentApiManager sharedInstance ()
    {
        if (_componentApiManager == null) {
            _componentApiManager = new AWComponentApiManager();
        }
        return _componentApiManager;
    }

    private AWComponentApiManager ()
    {
        _componentApiPackageTable = MapUtil.map();
        _missingComponentApiPackageTable = MapUtil.map();

        _componentPackageTable = new GrowOnlyHashtable();
    }

    ///////////////////////
    //
    ///////////////////////

//    // Used in the element and in the AWApi
//    public void addFoundBinding (AWComponent component, String bindingName)
//    {
//        // todo: is this info needed?
////        logMessage(" +++ found binding: ",component);
////        logBinding(bindingName,"\n");
//    }
//
//
//    // AWApi defined, supported bindings defined, and extraneous bindings are found ... report
//    // to make sure that these bindings are intentionally passed through
//    public void addPassThroughBinding (AWComponent component, String bindingName)
//    {
//        // todo: figure out when to display this
////        System.out.println(" iii pass through binding found: "+formatComponent(component) +
////                           " " + formatBinding(bindingName));
//    }

    public synchronized List getSortedComponentList (String packageName)
    {
        List componentList = (List)_componentPackageTable.get(packageName);
        if (componentList == null) {
            componentList = ListUtil.list();
            _componentPackageTable.put(packageName, componentList);
        }
        return componentList;
    }

    public synchronized List getSortedComponentDefinitionList (String packageName)
    {
        List names = getSortedComponentList(packageName);
        List result = ListUtil.list();
        int i =0, count = names.size();
        for ( ; i < count; i++) {
            result.add(componentDefinition((String)names.get(i)));
        }
        return result;
    }

    public List componentsWithErrors (String packageName, boolean includeValidationErrors, boolean includeApiErrors)
    {
        List result = ListUtil.list();
        List list = getSortedComponentList(packageName);
        for (int i = 0,size = list.size(); i < size; i++) {
            String componentName = (String) list.get(i);
            if ((includeValidationErrors && (getValidationErrorCount(componentName) != 0)) ||
                (includeApiErrors && (getComponentApiErrorCount(componentName) != 0 ))) {
                result.add(componentName);
            }
        }
        return result;
    }

    protected synchronized void insertIntoSortedComponentList
        (String packageName, String componentName)
    {
        List componentList = getSortedComponentList(packageName);
        insert(componentList,0,componentList.size(), componentName);
    }

    private void insert (List list, int lowerbound, int upperbound, String value)
    {
        if (upperbound == lowerbound) {
            list.add(upperbound,value);
        }
        else if (upperbound - lowerbound == 1) {
            String testVal = (String)list.get(lowerbound);
            int compare = value.compareTo(testVal);
            if (compare != 0) {
                list.add((compare > 0) ? upperbound : lowerbound, value);
            }
            // if == then ignore since already in the list
        } else {
            int pivot = (lowerbound + upperbound) / 2;
            String testVal = (String)list.get(pivot);
            int compare = value.compareTo(testVal);
            if (compare > 0) {
                insert(list, pivot, upperbound, value);
            }
            else if (compare < 0) {
                insert(list, lowerbound, pivot, value);
            }
            // if == then ignore since already in the list
        }       
    }

    public synchronized List getComponentApiList (String packageName)
    {
        List componentList = (List)_componentApiPackageTable.get(packageName);
        if (componentList == null) {
            componentList = ListUtil.list();
            _componentApiPackageTable.put(packageName, componentList);
        }
        return componentList;
    }

    private String componentName (AWComponentDefinition componentDefinition)
    {
        String componentName = componentDefinition.componentName();
        if (componentName.indexOf('.') != -1) {
            componentName = componentName.substring(componentName.lastIndexOf('.')+1);
        }
        return componentName;
    }

    public synchronized void registerComponentApi
        (AWComponentDefinition componentDefinition)
    {
        String componentName = componentName(componentDefinition);
        String packageName = packageNameForComponent(componentDefinition);

        List componentApiList = getComponentApiList(packageName);
        if (!componentApiList.contains(componentName)) {
            componentApiList.add(componentName);
        }

        insertIntoSortedComponentList(packageName, componentName);
    }

    ///////////////////////
    // AWApi validation
    ///////////////////////
    public synchronized List getMissingComponentApiList (String packageName)
    {
        List componentList = (List)_missingComponentApiPackageTable.get(packageName);
        if (componentList == null) {
            componentList = ListUtil.list();
            _missingComponentApiPackageTable.put(packageName, componentList);
        }
        return componentList;
    }

    public synchronized void addMissingAWApi (AWComponentDefinition componentDefinition)
    {
        String componentName = componentName(componentDefinition);
        String packageName = componentDefinition.componentPackageName();
        if (packageName == null) packageName = "unpackaged";

        List componentList = getMissingComponentApiList(packageName);
        if (!componentList.contains(componentName)) {
            componentList.add(componentName);
        }

        insertIntoSortedComponentList(packageName, componentName);
    }

    ///////////////////////
    // Logging
    ///////////////////////

    public static String printBindingErrors (AWComponentDefinition componentDefinition)
    {
        StringBuffer sbReturn = new StringBuffer();
        List errorList = componentDefinition.bindingErrorList();
        if (errorList != null)
        {
            for (int j = 0, errorListSize=errorList.size(); j < errorListSize; j++) {
                String error = (String)errorList.get(j);
                sbReturn.append("\t").append(error).append("\n");
            }
        }
        return sbReturn.toString();
    }

    ///////////////////////
    // Utility
    ///////////////////////

    public static String packageNameForComponent (AWComponentDefinition componentDefinition)
    {
        String packageName = componentDefinition.componentPackageName();
        if (packageName == null) packageName = "No Package";
        return packageName;
    }

    public static AWApi getAWApi (String componentName)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        AWComponentDefinition componentDefinition = application.componentDefinitionForName(componentName);
        return componentDefinition.componentApi();
    }

    public static Map getEmpiricalApi (String componentName)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        AWComponentDefinition componentDefinition = application.componentDefinitionForName(componentName);
        return componentDefinition.empiricalApiTable();
    }

    public static int getValidationErrorCount (String componentName)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        AWComponentDefinition componentDefinition = application.componentDefinitionForName(componentName);
        return componentDefinition.validationErrorCount();
    }

    public static int getComponentApiErrorCount (String componentName)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        AWComponentDefinition componentDefinition = application.componentDefinitionForName(componentName);
        return componentDefinition.componentApiErrorCount();
    }

    public static AWComponentDefinition componentDefinition (String componentName)
    {
        AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
        return application.componentDefinitionForName(componentName);
    }
}
