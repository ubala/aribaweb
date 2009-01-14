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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWNodeManager.java#6 $
*/
package ariba.ui.aribaweb.util;

import ariba.util.core.MultiKeyHashtable;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApplication;

public abstract class AWNodeManager
{
    private MultiKeyHashtable _directActionValidatorTable;
    private AWNodeValidator _defaultNodeValidator;

    public AWNodeManager ()
    {
        _directActionValidatorTable = new MultiKeyHashtable(2);
    }

    private void registerDirectAction (AWNodeValidator nv)
    {
        nv.setNodeManager(this);
        _directActionValidatorTable.put(nv.getActionClassName(), nv.getActionName(), nv);
    }

    private void registerComponentAction (AWNodeValidator nv)
    {
        nv.setNodeManager(this);
    }

    /**
     * Register a node validator as the default NodeValidator.
     * 
     * @param nv
     * @aribaapi private
     */
    public void setDefaultNodeValidator (AWNodeValidator nv)
    {
        _defaultNodeValidator = nv;
    }

    /**
     * Return the default NodeValidator
     * @aribaapi private
     */
    public AWNodeValidator defaultNodeValidator ()
    {
        return _defaultNodeValidator;
    }


    /**
     * Returns the nodevalidator registered for a given DirectAction
     * classname and action name.
     * @param className
     * @param actionName
     * @aribaapi private
     */
    public AWNodeValidator nodeValidatorForDirectAction (String className,
                                                         String actionName)
    {
        return (AWNodeValidator)_directActionValidatorTable.get(className, actionName);
    }

    /**
     * Follows the definition of adaptorUrl in AWApplication -- expect the fully
     * qualified URL including (in the case of a servlet adaptor) the context root of
     * the servlet.
     * @param nodeId
     * @aribaapi private
     */
    public abstract String adaptorUrlForNode (String nodeId);

    //
    // Static utility methods
    //

    private static AWNodeManager getNodeManager ()
    {
        AWApplication application = (AWApplication)AWConcreteApplication.sharedInstance();
        return application.getNodeManager();
    }

    public static void registerNodeValidator (AWNodeValidator nv)
    {
        AWNodeManager nm = getNodeManager();
        if (nm != null) {
            nm.registerDirectAction(nv);
        }
    }

    public static void registerComponentActionNodeValidator (AWNodeValidator nv)
    {
        AWNodeManager nm = getNodeManager();
        if (nm != null) {
            nm.registerComponentAction(nv);
        }
    }

    public static void registerDefaultNodeValidator (AWNodeValidator nv)
    {
        AWNodeManager nm = getNodeManager();
        if (nm != null) {
            nm.setDefaultNodeValidator(nv);
            nm.registerDirectAction(nv);
        }
    }

    public static AWNodeValidator getDefaultNodeValidator ()
    {
        AWNodeValidator nv = null;
        AWNodeManager nm = getNodeManager();
        if (nm != null) {
            nv = nm.defaultNodeValidator();
        }
        return nv;
    }

    public static AWNodeValidator getNodeValidatorForDirectAction (String className,
                                                                   String actionName)
    {
        AWNodeValidator nv = null;
        AWNodeManager nm = getNodeManager();
        if (nm != null) {
            nv = nm.nodeValidatorForDirectAction(className, actionName);
        }
        return nv;
    }
}