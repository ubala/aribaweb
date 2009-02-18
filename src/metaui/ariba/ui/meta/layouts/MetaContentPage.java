/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaContentPage.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWStaticSiteGenerator;
import ariba.ui.meta.persistence.ObjectContext;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.widgets.ModalPageWrapper;
import ariba.util.fieldvalue.Extensible;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class MetaContentPage extends AWComponent implements Extensible,
        UIMeta.NavContextProvider, AWStaticSiteGenerator.Naming
{
    Map _contextValues = new HashMap();

    public boolean isStateless()
    {
        return false;
    }

    public Map getContextValues ()
    {
        return _contextValues;
    }

    // Extensible support
    public Map extendedFields()
    {
        return _contextValues;
    }

    public boolean useModal ()
    {
        return ModalPageWrapper.inClientPanel(this) || MetaContext.currentContext(this).booleanPropertyForKey("modal", false);
    }

    // Todo: should not be here -- move out to meta action buttons...
    public AWComponent save ()
    {
        if (errorManager().checkErrorsAndEnableDisplay()) return null;

        // Todo:  Catch optimistic locking failures and...  Merge? retry?
        ObjectContext.get().save();

        return ModalPageWrapper.returnPage(this);
    }

    // Should return map of the form [class:example.app.SomeClass]
    public Map<String, String> currentNavContext()
    {
        String moduleName = (String)_contextValues.get(UIMeta.KeyModule);
        if (moduleName != null) return Collections.singletonMap(UIMeta.KeyModule, moduleName);

        String className = (String)_contextValues.get(UIMeta.KeyClass);
        Object o;
        if (className == null && ((o = _contextValues.get(UIMeta.KeyObject)) != null)) {
            className = o.getClass().getName();
        }
        return (className != null)
                ? Collections.singletonMap(UIMeta.KeyClass, className)
                : Collections.EMPTY_MAP;
    }

    public String staticPath()
    {
        String page = (String)_contextValues.get("mainResource");
        if (page != null) return page.replaceFirst("\\.\\w+$", "");
        
        String moduleName = (String)_contextValues.get(UIMeta.KeyModule);
        return (moduleName != null) ? moduleName : templateName();
    }
}
