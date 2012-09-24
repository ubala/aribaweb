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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/MRUTabList.java#8 $
*/
package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWBindingNames;
import ariba.ui.aribaweb.core.AWBinding;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWChecksum;
import ariba.util.core.Sort;
import ariba.util.core.Compare;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class MRUTabList extends AWComponent
{
    static class Tab {
        Object item;
        String label;
        String semanticKey;
        int rank;
        boolean isVisible;
    }

    protected static final int DefaultMaxWidthChars = 100;

    public List<Tab> _visibles;
    public List<Tab> _all;
    public Tab _currentTab;
    protected Tab _selectedTab;
    protected Tab _nextTab;
    AWBinding _rankBinding;
    long _allChecksum = 0L;

    public boolean isStateless() {
        return false;
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        checkTabInfo();
        super.renderResponse(requestContext, component);
    }

    public void checkTabInfo ()
    {
        List<Object> items = (List)valueForBinding(AWBindingNames.list);
        Object selectedItem = valueForBinding(AWBindingNames.selection);

        // we use a checksum of the hash of our list values (and selection) to detect changes
        // (and avoid recomputation in the common case)
        long checksum = AWChecksum.crc32HashList(0L, items);
        if (selectedItem != null) checksum = AWChecksum.crc32(checksum, selectedItem.hashCode());

        if (_all != null && (checksum == _allChecksum)) return;

        _allChecksum = checksum;

        // Gather tab info and ranks
        AWBinding labelBinding = bindingForName(AWBindingNames.label);
        _rankBinding = bindingForName("rank");
        _all = new ArrayList();
        int defaultRank = MRUStartRank;
        for (Object item : items) {
            setValueForBinding(item, AWBindingNames.item);
            defaultRank += 1;
            Tab tab = new Tab();
            tab.item = item;
            tab.label = stringValueForBinding(labelBinding);
            tab.semanticKey = stringValueForBinding(bindingForName(AWBindingNames.awname));
            if (item == selectedItem) {
                _selectedTab = tab;
                tab.rank = -100000;
            } else {
                tab.rank = rankForCurrentItem(tab.label, _rankBinding, defaultRank);
            }
            _all.add(tab);
        }

        // Sort by rank
        Object[] byRank = _all.toArray();
        Sort.objects(byRank, new Compare() {
            public int compare (Object o1, Object o2)
            {
                int r1 = ((Tab)o1).rank, r2 = ((Tab)o2).rank;
                return (r1 == r2) ? 0 : (r1 > r2 ? 1 : -1);
            }
        });

        // find width cut off
        Number maxWidthChars = (Number)valueForBinding("maxWidthChars");
        int max = (maxWidthChars != null) ? maxWidthChars.intValue() : DefaultMaxWidthChars;
        int curWidth = 0;
        int pos = 0;
        for (; pos < byRank.length; pos++) {
            Tab tab = ((Tab)byRank[pos]);
            String label = tab.label;
            curWidth += label.length() + 4;
            if (curWidth > max) break;
            tab.isVisible = true;
        }
        if (pos < byRank.length) {
            _visibles = new ArrayList();
            for (Tab tab : _all) {
                if (tab.isVisible) _visibles.add(tab);
            }

        } else {
            _visibles = null;
        }
    }

    int rankForCurrentItem (String key, AWBinding rankBinding, int defaultRank)
    {
        Number rankVal = (Number)valueForBinding(rankBinding);
        if (rankVal == null) rankVal = getMRU(session(), componentReference()).rankForKey(key);
        return (rankVal != null) ? rankVal.intValue() : defaultRank;
    }

    public String currentTabSemanticKey ()
    {
        return _currentTab.semanticKey;
    }

    public String currentTabLabel ()
    {
        return _currentTab.label;
    }
    
    public Object selectedTab ()
    {
        return _selectedTab;
    }

    public void setSelectedTab (Tab selected)
    {
        _nextTab = selected;
    }

    public AWResponseGenerating tabSelected ()
    {
        getMRU(session(), componentReference()).updateMRUForKey(_currentTab.label);
        // invalidate list...
        _all = null;
        setValueForBinding(_nextTab.item, AWBindingNames.selection);
        AWResponseGenerating response = (AWResponseGenerating)valueForBinding(AWBindingNames.action);
        if (response == null || response == pageComponent()) {
            _selectedTab = _nextTab;
            recordBacktrackState(_selectedTab);
        }
        return response;
    }

    // Backtrack support
    public Object restoreFromBacktrackState (Object backtrackState)
    {
        Tab before = _selectedTab;
        _selectedTab = (Tab)backtrackState;
        setValueForBinding(((_selectedTab != null) ? _selectedTab.item : null),
                AWBindingNames.selection);
        checkTabInfo();
        return before;
    }

    static final String SessionMRUKey = SessionMRU.class.getName();
    static SessionMRU getMRU (AWSession session, Object key)
    {
        Map map = (Map)session.dict().get(SessionMRUKey);
        if (map == null) {
            map = new HashMap();
            session.dict().put(SessionMRUKey, map);
        }

        SessionMRU mru = (SessionMRU)map.get(key);
        if (mru == null) {
            mru = new SessionMRU();
            map.put(key, mru);
        }
        return mru;
    }

    static final int MRUStartRank = Integer.MAX_VALUE/2 - 100;
    static class SessionMRU
    {
        int _nextRank = MRUStartRank;
        Map <String, Integer> _itemRanks = new HashMap();

        Integer rankForKey (String key)
        {
            return _itemRanks.get(key);
        }

        void updateMRUForKey (String key)
        {
            _itemRanks.put(key, _nextRank--);
        }
    }
}
