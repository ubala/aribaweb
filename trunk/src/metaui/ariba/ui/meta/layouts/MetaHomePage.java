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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaHomePage.java#5 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWStaticSiteGenerator;
import ariba.ui.meta.core.UIMeta;

import java.util.Map;
import java.util.Collections;

public class MetaHomePage extends AWComponent implements AWResponseGenerating.ResponseSubstitution,
        UIMeta.NavContextProvider, AWStaticSiteGenerator.Naming
{
    String _module;

    public ariba.ui.meta.core.ItemProperties currentModule ()
    {
        MetaNavTabBar.State state = MetaNavTabBar.getState(session());
        state.checkSelectedModule(pageComponent());
        return state.getSelectedModule();
    }

    public AWResponseGenerating replacementResponse ()
    {
        return MetaNavTabBar.getState(session()).redirectForPage(this);
    }

    public String getModule()
    {
        return _module;
    }

    public void setModule (String module)
    {
        _module = module;
    }

    public Map<String, String> currentNavContext()
    {
        return (_module != null) ? Collections.singletonMap(UIMeta.KeyModule, _module) : null;
    }

    public String staticPath()
    {
        return currentModule().name();
    }
}
