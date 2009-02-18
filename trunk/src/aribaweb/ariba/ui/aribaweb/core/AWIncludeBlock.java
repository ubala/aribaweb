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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWIncludeBlock.java#4 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;
import ariba.util.fieldvalue.FieldValue;

/*
    AWIncludeBlock allows for reference to the content of an AWBlock.
    See AWApi file for complete user doc. 
*/

/*
 Warning:  There is a latent bug here.  I believe that if the user provides a
 dynamic binding for the templateName, we need to adjust the elementId just as
 switch component does.  This is not happening now.

 There are use cases where the application is dynamically binding in the name
 of a subtemplate.  In these cases, the application is responsible for keeping
 the same subtemplate name from renderResponse through the subsequent
 invokeAction
*/

public final class AWIncludeBlock extends AWComponent
{
    public AWBinding _templateNameBinding;

    static {
        FieldValue.registerClassExtension(AWIncludeBlock.class, new FieldValue_AWSubcomponent());
    }

    protected boolean useLocalPool ()
    {
        return true;
    }

    protected void takeBindings(AWBindingDictionary bindings) {
        super.takeBindings(bindings);
        if (_templateNameBinding == null) _templateNameBinding = bindings.get(AWBindingNames.name);        
    }

    /**
      Extracts the template from the parentComponent's template by searching for
      an AWBlock with the specified name.  If the name bidning is constant
      value, caches this template on the componentRef's userData,
      otherwise re-extracts everytime.
    */
    public AWTemplate template ()
    {
        AWTemplate template = (AWTemplate)componentReference().userData();
        if (template == null) {
            Assert.that(_templateNameBinding != null, "Unable to locate binding named 'templateName'");
            String subtemplateName = stringValueForBinding(_templateNameBinding);
            // Assumes hasMultipleTemplates == false
            AWTemplate parentTemplate = parent().template();
            AWBlock[] blocks = (AWBlock[])parentTemplate.extractElementsOfClass(AWBlock.class);
            for (int index = blocks.length - 1; index > -1; index--) {
                AWBlock currentBlock = blocks[index];
                if (currentBlock.name().equals(subtemplateName)) {
                    template = currentBlock.subtemplate();
                    break;
                }
            }
            Assert.that(template != null, "Unable to locate AWBlock named '%s'.  Hint: make sure its not enclosed within any container/tag in your component.", subtemplateName);
            if (_templateNameBinding.isConstantValue()) {
                componentReference().setUserData(template);
            }
            // else... we need to cache the extracted templates in a hashtable
            // for now only allow non-dynamic subtemplate names
        }
        return template;
    }

    protected AWApi componentApi ()
    {
        // Note: can't load template as in AWComponent since _templateNameBinding is not set up until
        // ComponentReference.renderResponse
        return null;
    }

        // recording & playback
    protected boolean _debugSemanticKeyInteresting ()
    {
        return false;
    }

    protected String _debugCompositeSemanticKey (String bestKeySoFar)
    {
        return parent()._debugCompositeSemanticKey(bestKeySoFar);
    }

    protected void validate (AWValidationContext validationContext)
    {
        // no op
    }

    protected String fullTemplateResourceUrl ()
    {
        return parent().fullTemplateResourceUrl();
    }

    public boolean useXmlEscaping ()
    {
        return parent().useXmlEscaping();
    }

    public AWBinding bindingForName (String bindingName, boolean recursive)
    {
        AWBinding localBinding = super.bindingForName(bindingName,  recursive);
        if (localBinding == null) {
            localBinding =  parent().bindingForName(bindingName,  recursive);
        }
        return localBinding;
    }
}
