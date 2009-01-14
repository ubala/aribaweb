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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/AWXIncludeTag.java#6 $
*/

package ariba.ui.demoshell;

import ariba.ui.widgets.BrandingComponent;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWElement;
import ariba.ui.aribaweb.core.AWTemplate;

import java.io.File;

import ariba.ui.aribaweb.core.AWComponentDefinition;

public class AWXIncludeTag extends AWComponent
            implements AWXHTMLComponentFactory.ContentAcceptor
{
    private AWComponent _context;
    private String _componentOverride;

    public boolean isStateless ()
    {
        return false;  // we need to remember if our selection was swapped
    }

    public void awake ()
    {
        String templateName = stringValueForBinding(AWBindingNames.name);
        _context = BrandingComponent.componentWithTemplateNamed(templateName, this);
    }

    public void sleep ()
    {
        _context = null;
    }

    public boolean handleComponentName (String name, String target, AWComponent parent)
    {
        // default to *not* act as a container
        boolean container = hasBinding("container") && booleanValueForBinding("container");
        if (container && ((target == null) || target.equals(stringValueForBinding("targetName"))) ){
            // check if we can resolve this.  if so, we'll swap our content.  If not
            // we'll absorb this with just a warning
            AWComponentDefinition definition = AWXHTMLComponentFactory.sharedInstance().componentDefinitionForRelativePath(name, parent);
            if (definition != null) {
                _componentOverride = AWXHTMLComponentFactory.sharedInstance().pathForComponentDefinition(definition);
            } else {
                Log.demoshell.debug("*** Warning -- Include -- HREF that can't be resolved: %s",name);
            }
            return  true;  // cycle the page
        }
        return false;
    }

    public String componentName ()
    {
        if (_componentOverride != null) {
            return _componentOverride;
        }
        return stringValueForBinding("name");
    }

    public AWComponent context ()
    {
        return _context;
    }
}
