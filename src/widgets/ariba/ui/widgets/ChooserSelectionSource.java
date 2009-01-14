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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/ChooserSelectionSource.java#4 $
*/

package ariba.ui.widgets;

import ariba.util.core.ListUtil;
import ariba.util.fieldvalue.FieldPath;

import java.util.List;

public interface ChooserSelectionSource
{
    public List match (String pattern, int max);
    public List match (List selections, String pattern);

    // simple implementation to do infix string matches a list of items
    public static class ListSource implements ChooserSelectionSource {
        List _all;
        FieldPath _keyPath;

        // if key is null, items are assumed to be strings
        public ListSource (List items, String key) {
            _all = items;
            _keyPath = (key != null) ? new FieldPath(key) : null;
        }

        protected boolean matches (Object item, String pattern) {
            // Performance: perhaps we should be using a cached RegEx?
            Object val = (_keyPath != null) ? _keyPath.getFieldValue(item) : item;
            return pattern == null || (((String)val).toLowerCase().indexOf(pattern) > -1);
        }
        
        protected List match (List list, String pattern, int max)
        {
            if (pattern == null) return list;
            String toLowerPattern = pattern.toLowerCase();
            List result = ListUtil.list();
            for (int i=0, c=list.size(); i<c; i++) {
                Object item = list.get(i);
                if (matches(item, toLowerPattern)) {
                    result.add(item);
                    if (result.size() >= max) break;
                }
            }
            return result;
        }

        public List match (String pattern, int max) {
            return match(_all, pattern, max);
        }

        public List match (List selections, String pattern) {
            return match(selections, pattern, Integer.MAX_VALUE);
        }
    }
}
