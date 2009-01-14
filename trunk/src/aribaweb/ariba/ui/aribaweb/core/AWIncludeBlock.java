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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/core/AWIncludeBlock.java#3 $
*/

package ariba.ui.aribaweb.core;

import ariba.util.core.Assert;
import ariba.util.fieldvalue.FieldValue;

/**
    AWIncludeBlock allows for reference to the content of an AWBlock.
    The idea is to allow for a light-weight component which enables (template)
    code factoring without the overhead of a full-blown template.  Often,
    factoring a duplicated region of a component's template leads to an
    explosion of files, and an unnecessary clouding of what are the real
    high-level pieces of the application.  While AWIncludeBlock has is
    limitations (no code may be associated specifically with the subcomponent),
    it makes lightweight factoring easy and powerful.

    To use AWIncludeBlock, you must define an AWBlock within the same
    template where the AWIncludeBlock will reference the AWBlock.  Within
    an AWBlock, $ bindings are evaluated in the scope of the defining
    component.  However, $^ bindings apply to the values passed from the
    AWIncludeBlock reference and not to the defining component (perhaps I could
    do a cascased lookup?).  This is necessary as this is the only way to
    parameterize the sbcomponent.

 As an example, consider the following template fragment:

 ====
 :
 <td>
     <AWIncludeBlock templateName="ModuleBrowser" list="$moduleList1"/>
 </td>
 <td>
     <AWIncludeBlock templateName="ModuleBrowser" list="$moduleList2"/>
 </td>
  :
  :
 <AWBlock name="ModuleBrowser">
     <AWBrowser list="$^list" item="$currentModule" style="width:100%" size="10">\
         $currentModule.name\
     </AWBrowser>\
 </AWBlock>

====

 Here, we define the AWBlock named ModuleBrowser and refer to it via an AWIncludeBlock tag which has two bindings:  the templateName of the AWBlock to use, and an arbitrary binding 'list'.  In essence, AWIncludeBlock operates like AWIncludeComponent in that it switches in the appropriate component/template and makes the remaining bindings available to the subcomponent.  In this case, the 'list' binding can be accessed within the AWBlock by simply using the standard carat ($^) binding.  In the example above, you can see that the ModuleBrowser subtemplate accesses the $^list from the AWIncludeBlock reference, but all the other bindings (ie $currentString) are evaluated int the scope of the enclosing component.  By allowing for parameters to be passed like this, the AWBlock can be parameterized and used with different sets of data, while still using the rest of the values in the enclosing components scope/context.

 It should also be noted that AWIncludeContent works as usual.  For example, we could rewrite the above awl as folows, thus giving us even more control over how the subcomponent renders:

 ====
 :
 <td>
     <AWIncludeBlock templateName="ModuleBrowser" list="$moduleList1"/>
        (1) $currentModule.name\
     </AWIncludeBlock>
 </td>
 <td>
     <AWIncludeBlock templateName="ModuleBrowser" list="$moduleList2">
        (2) $currentModule.name\
     </AWIncludeBlock>
 </td>
  :
  :
 <AWBlock name="ModuleBrowser">
     <AWBrowser list="$^list" item="$currentModule" style="width:100%" size="10">\
         <AWIncludeContent/>
     </AWBrowser>\
 </AWBlock>

====

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
