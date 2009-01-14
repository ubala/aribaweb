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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXHTMLComponent.java#11 $
*/

// This component is used for any included HTML template.
//
// We implement a couple of convenience for such java-less components:
// 1) dynamic fields (i.e. "$myField" is auto-created)
// 2) action generation: ("$goto.somePage" does an HTML goto that page)
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWTemplate;
import ariba.ui.widgets.ActionInterceptor;
import ariba.ui.widgets.ActionHandler;
import ariba.ui.widgets.AribaAction;
import ariba.util.fieldvalue.Extensible;
import ariba.util.fieldvalue.FieldValue;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.fieldvalue.FieldValueAccessor;
import ariba.util.fieldvalue.FieldValueException;
import ariba.util.log.Logger;

public class AWXHTMLComponent extends AWComponent implements Extensible, ActionInterceptor
{
    static final String HtmSuffix = ".htm";

    /*
    protected AWResource safeTemplateResource ()
    {
        AWResourceManager resourceManager = resourceManager();
        String templateName = templateName();
        AWResource resource = resourceManager.resourceNamed(templateName);
        return resource;
    }
    */


    public boolean isStateless ()
    {
        return false;  // stateful -- in case we have dynamic fields
    }

    public AWTemplate loadTemplate ()
    {
        // okay, this is a little wacky, but we have to look this up by component definition
        return AWXHTMLComponentFactory.sharedInstance().templateForDefinition(componentDefinition());
    }

    protected String fullTemplateResourceUrl ()
    {
        return AWXHTMLComponentFactory.sharedInstance().pathForComponentDefinition(componentDefinition());

    }

    public static class GotoHTMLActionHandler extends ActionHandler {
        protected AWComponentDefinition _definition;

        public GotoHTMLActionHandler (AWComponentDefinition definition)
        {
            _definition = definition;
        }

        public AWResponseGenerating actionClicked (AWRequestContext requestContext)
        {
            return AWXHTMLComponentFactory.sharedInstance().createComponentFromDefinition(_definition, requestContext);
        }
    }

    // Utilitity for scripted subclasses
    public AWComponent pageWithName (String path)
    {
        AWComponent result = AWXHTMLComponentFactory.sharedInstance().createComponentForRelativePath(path, this);
        if (result == null) result = pageWithName(path);
        return result;
    }

    // called by GoTo with page that we need to return
    protected AWComponent pageWithKeyPath (String keyPath)
    {
        return AWXHTMLComponentFactory.sharedInstance().componentToReturnForKeyPath(keyPath, this);
    }
    public Logger logger() { return Log.demoshell; }

    public String toString ()
    {
        return super.toString() + " (" + AWXHTMLComponentFactory.sharedInstance().pathForComponentDefinition(this.componentDefinition()) + ") ";
    }

    // override ActionInterceptor to treat actions as page gotos
    public ActionHandler overrideAction (String action, ActionHandler defaultHandler, AWRequestContext requestContext)
    {
        // Use == to only do this when this is *literally* AribaAction.HomeAction
        if (action == AribaAction.HomeAction) return null;
        
        if (!action.endsWith(HtmSuffix)) {
            action += HtmSuffix;  // try putting it on
        }

        // see if we can find a page that matches this action name
        AWComponentDefinition definition = AWXHTMLComponentFactory.sharedInstance().componentDefinitionForRelativePath(action, this);
        if (definition != null) {
            return new GotoHTMLActionHandler(definition);
        }

        return null;
    }

    // used in action bindings to goto the given html page.
    // e.g.: action="$goto.MyPage"
    // will take the user to "MyPage.html" relative to the directory
    // of the component with the reference.
    public GoTo getGoto () {
        return new GoTo(this);
    }

        // adapter class returned by getGoto() to do dynamic FieldValue stuff
    protected static class GoTo
    {
        protected AWXHTMLComponent _container;

        static {
            FieldValue.registerClassExtension (AWXSJSHTMLComponent.GoTo.class, new AWXSJSHTMLComponent.FieldValue_GoTo());
        }

        public GoTo (AWXHTMLComponent container)
        {
            _container = container;
        }
    }// Field Value implementation for GoTo -- i.e. the real meat of GoTo.
    // This is to give us dynamic actions.  I.e.
    public static class FieldValue_GoTo extends FieldValue
    {
        // Note: since we want to interpret the whole path in one shot (to interpret
        // as a file path) we don't call the primitive to recurse

        public Object getFieldValue (Object receiver, FieldPath fieldPath)
        {
            String path = fieldPath.fieldPathString();
            return ((GoTo)receiver)._container.pageWithKeyPath(path);
        }

        // we interpret this access as a page goto
        public Object getFieldValuePrimitive (Object receiver, FieldPath fieldPath)
        {
            return getFieldValue(receiver, fieldPath);
        }

        /**
        Overridden to disable.  This throws an exception if called.
        */
        public FieldValueAccessor createAccessor (Object receiver, String fieldName, int type)
        {
            throw new FieldValueException(receiver.getClass().getName() +
                                          ": createAccessor() not suported");
        }

        /**
        Overridden to disable.  This throws an exception if called.
        */
        public FieldValueAccessor getAccessor (Object receiver, String fieldName, int type)
        {
            throw new FieldValueException(receiver.getClass().getName() +
                                          ": getAccessor() not suported");
        }

        /* We don't do sets */
        public void setFieldValue (Object receiver, FieldPath fieldPath,
                                            Object value)
        {
            setFieldValuePrimitive(receiver, fieldPath, value);
        }


        public void setFieldValuePrimitive (Object receiver, FieldPath fieldPath, Object value)
        {
            throw new FieldValueException(receiver.getClass().getName() +
                                          ": setFieldValuePrimitive() not suported");
        }

    }
}
