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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWValidationContext.java#11 $
*/

package ariba.ui.aribaweb.core;

import ariba.ui.aribaweb.util.AWBaseObject;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;
import ariba.util.core.SystemUtil;
import ariba.util.core.Fmt;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

public class AWValidationContext extends AWBaseObject
{
    //--------------------------------------------------------------------------
    // nested class

    /**
        @aribaapi ariba
    */
    public static class ValidationError
    {
        public String message;
        public Throwable exception;

        public ValidationError (String message, Throwable exception)
        {
            this.exception = exception;
            this.message = message;
        }

        public String getStackTrace ()
        {
            if (exception != null) {
                return SystemUtil.stackTrace(exception);
            }
            return null;
        }
    }

    private List _packagesWithErrors;
    private Map _packageErrorTable;
    private Map<String,List<ValidationError>> _generalErrors = new TreeMap();

    ///////////////////////
    // Constructor
    ///////////////////////
    public AWValidationContext ()
    {
    }

    ///////////////////////
    // Validation tracking
    ///////////////////////


    private List packagesWithErrorsList ()
    {
        if (_packagesWithErrors == null) {
            _packagesWithErrors = ListUtil.list();
        }
        return _packagesWithErrors;
    }

    private Map packageErrorsTable ()
    {
        if (_packageErrorTable == null) {
            _packageErrorTable = MapUtil.map();
        }
        return _packageErrorTable;
    }

    public List componentsWithErrors (String packageName)
    {
        List componentWithErrorsList = (List) packageErrorsTable().get(packageName);

        if (componentWithErrorsList == null) {
            componentWithErrorsList = ListUtil.list();
            packageErrorsTable().put(packageName, componentWithErrorsList);
        }
        return componentWithErrorsList;
    }

    public void addComponentWithError(AWComponentDefinition componentDefinition)
    {
        String componentName = componentDefinition.componentName();
        String packageName = AWComponentApiManager.packageNameForComponent(componentDefinition);
        addToUniqueSortedList(packagesWithErrorsList(),packageName);
        addToUniqueSortedList(componentsWithErrors(packageName),componentName);
    }

    public List packagesWithErrors ()
    {
        return _packagesWithErrors;
    }

    /**
        @aribaapi ariba
    */
    public void clearGeneralErrors (String key)
    {
        if (key == null) {
            key = "Other";
        }
        _generalErrors.remove(key);
    }


    public void addGeneralError (String key, String msg, Throwable exception)
    {
        if (key == null) {
            key = "Other";
        }
        ValidationError error = new ValidationError(msg, exception);
        MapUtil.merge(_generalErrors, key, error);
    }

    public void addGeneralError (String msg)
    {
        addGeneralError(null, msg, null);
    }

    /**
        @aribaapi ariba
    */
    public List<String> getGeneralErrorKeys ()
    {
        return new ArrayList(_generalErrors.keySet());
    }

    /**
        @aribaapi ariba
    */
    public List<ValidationError> getGeneralErrorsFor (String key)
    {
        List<ValidationError> result = _generalErrors.get(key);
        return result != null ? result : Collections.<ValidationError>emptyList();
    }

    public List<String> getGeneralErrors ()
    {
        List<String> result = ListUtil.list();
        for (String key : _generalErrors.keySet()) {
            List<ValidationError> errors = _generalErrors.get(key);
            for (ValidationError error : errors) {
                result.add(Fmt.S("%s: %s", key, error.message));
            }
        }
        return result;
    }

    /**
     * returns whether any error are registered for the given component or any others in its package.
     */
    public boolean hasErrorForComponentPackage (AWComponent component)
    {
        String packageName = AWComponentApiManager.packageNameForComponent(component.componentDefinition());
        List packagesWithErrors = packagesWithErrors();
        return packagesWithErrors != null && packagesWithErrors.contains(packageName);
    }

    // Used in the element and in the AWApi
//    public void addFoundBinding (AWComponent component, String bindingName)
//    {
//        // Is this info needed?
//        logMessage(" +++ found binding: ",component);
//        logBinding(bindingName,"\n");
//    }

    ///////////////////////
    // Logging
    ///////////////////////
    public boolean hasErrors ()
    {
       return hasValidationErrors() || hasGeneralErrors();
    }

    public boolean hasGeneralErrors ()
    {
        return (_generalErrors != null && _generalErrors.size() != 0);
    }

    public boolean hasValidationErrors ()
    {
        return (_packagesWithErrors != null && _packagesWithErrors.size() != 0);
    }

//    public String dumpBindingErrors ()
//    {
//        StringBuffer sbReturn = new StringBuffer();
//
//        if (_componentsWithErrors != null) {
//            AWApplication application = (AWApplication)AWConcreteApplication.SharedInstance;
//            // Must search for elementName first since componentName will always succeed (eg AWComponent)
//            // so we don't ever try the elementName (which is used for classless components).
//
//            for (int i = 0, size = _componentsWithErrors.size(); i < size; i++) {
//                String componentName = (String) _componentsWithErrors.get(i);
//                sbReturn.append(componentName).append("\n");
//                sbReturn.append(AWComponentApiManager.printBindingErrors(application.componentDefinitionForName(componentName)));
//            }
//        }
//
//        return sbReturn.toString();
//    }

    ///////////////////////
    // Utility
    ///////////////////////
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

}
