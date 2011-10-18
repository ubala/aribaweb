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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWComponentApi.java#6 $
*/

package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.core.AWApi;
import ariba.ui.aribaweb.core.AWBindingApi;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

public final class AWComponentApi extends AWComponent
{
    private static final String EmpiricalApiBinding = "empiricalApi";
    private static final String AWApiBinding        = "awApi";
    private static final String Overview            = "overview";

    protected Map _empiricalApiTable = null;
    protected List _empiricalApiBindingList = null;
    protected AWApi _apiContainer = null;
    protected AWApi _apiOverview = null;
    public AWBindingApi _binding;
    
    ////////////////////////////////
    // Methods state management
    ////////////////////////////////

    public boolean isStateless ()
    {
        return true;
    }

    protected void flushState ()
    {
        super.flushState();
        _empiricalApiTable = null;
        _empiricalApiBindingList = null;
        _apiContainer = null;
        _apiOverview = null;
    }

    ////////////////////////////////
    // Methods for displaying EmpiricalApi
    ////////////////////////////////
    public Map empiricalApiTable ()
    {
        if (_empiricalApiTable == null) {
            _empiricalApiTable = (Map)valueForBinding(EmpiricalApiBinding);
        }
        return _empiricalApiTable;
    }

    public List empiricalApiBindingList ()
    {
        if (_empiricalApiBindingList == null) {
            Object[] keys = empiricalApiTable().keySet().toArray();
            Arrays.sort(keys);
            _empiricalApiBindingList = ListUtil.list(keys.length);
            for (int i=0; i < keys.length; i++) {
                _empiricalApiBindingList.add(empiricalApiTable().get(keys[i]));
            }
        }
        return _empiricalApiBindingList;
    }

    ////////////////////////////////
    // Methods for displaying AWApi
    ////////////////////////////////
    public AWApi apiContainer ()
    {
        if (_apiContainer == null) {
            _apiContainer = (AWApi)valueForBinding(AWApiBinding);
        }
        return _apiContainer;
    }
    
    public AWApi apiOverview ()
    {
        if (_apiOverview == null) {
            _apiOverview = (AWApi)valueForBinding(Overview);
        }
        return _apiOverview;
    }

    public boolean bindingHasAlternates ()
    {
        return !StringUtil.nullOrEmptyString(_binding.alternates());
    }
}
