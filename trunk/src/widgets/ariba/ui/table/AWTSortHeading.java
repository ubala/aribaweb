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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTSortHeading.java#16 $
*/
package ariba.ui.table;

import java.util.List;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.core.AWComponent;

public final class AWTSortHeading extends AWComponent
{
    private AWTDisplayGroup _displayGroup;
    private String _key;
    private List _sortOrderingArray;
    private boolean _caseSensitive;

    protected void awake ()
    {
        _displayGroup = (AWTDisplayGroup)valueForBinding(BindingNames.displayGroup);
        _key = (String)valueForBinding(BindingNames.key);
        _caseSensitive = booleanValueForBinding(BindingNames.caseSensitiveSort);
    }

    protected void sleep ()
    {
        _displayGroup = null;
        _key = null;
        _sortOrderingArray = null;
        _caseSensitive = false;
    }

    private List sortOrderingArray ()
    {
        if (_sortOrderingArray == null) {
            List sortOrderingArray = null;
            if (_displayGroup != null) {
                sortOrderingArray = _displayGroup.sortOrderings();
            }
            else {
                sortOrderingArray = (List)valueForBinding(BindingNames.sortOrderings);
            }
            if (sortOrderingArray == null) {
                _sortOrderingArray = ListUtil.list();
            }
            else {
                _sortOrderingArray = ListUtil.collectionToList(sortOrderingArray);
            }
        }
        return _sortOrderingArray;
    }

    private AWTSortOrdering primaryOrdering ()
    {
        AWTSortOrdering primaryOrdering = null;
        List sortOrderingArray = sortOrderingArray();
        if (sortOrderingArray.size() > 0) {
            primaryOrdering = (AWTSortOrdering)sortOrderingArray.get(0);
        }
        return primaryOrdering;
    }

    public boolean isCurrentKeyPrimary ()
    {
        AWTSortOrdering primaryOrdering = primaryOrdering();
        if (primaryOrdering == null) {
            return false;
        }
        String key = _key;
        if (key == null) {
            AWTSortOrdering so = (AWTSortOrdering)valueForBinding(BindingNames.createSortOrdering);
            if (so == null) return false;
            return primaryOrdering.equals(so);
        } else {
            int commaIndex = key.indexOf(',');
            // if we have a multi-level sort key => extract out the first
            // key and check it against the primary ordering key
            if (commaIndex != -1) key = key.substring(0, commaIndex);
            return primaryOrdering.key().equals(key);
        }
    }

    private int primaryKeyOrderingSelector ()
    {
        return primaryOrdering().selector();
    }

    public String imageName ()
    {
        String imageName = "Unsorted.gif";
        if (isCurrentKeyPrimary()) {
            int currentState = primaryKeyOrderingSelector();
            if (isAscendingSelector(currentState)) {
				// this image points down
                imageName = "widg/ascending.gif";
            }
            if (isDescendingSelector(currentState)) {
				// this image points up
                imageName = "widg/descending.gif";
            }
        }
        return imageName;
    }

    private void removeOrdering (List orderingArray)
    {
        if (_key != null) {
            String key = _key;
            int index = key.indexOf(',');
            while (index != -1) {
                removeOrderingWithSingleKey(key.substring(0, index), orderingArray);
                key = key.substring(index+1);
                index = key.indexOf(',');
            }
            removeOrderingWithSingleKey(key, orderingArray);
        } else {
            orderingArray.remove(getSortOrdering(ascendingSelector(), null));
        }
    }

    private void removeOrderingWithSingleKey (String key, List orderingArray)
    {
        AWTSortOrdering match = AWTDisplayGroup.orderingMatchingKey(orderingArray, key);
        if (match != null) orderingArray.remove(match);
    }

    private void makePrimaryOrderingWithSelector (int aSelector, List orderingArray)
    {
            // handle a multi-level sort key (a key with multiple fields
            // separated by commas
        String key = _key;
        if (key != null) {
            int commaIndex = key.lastIndexOf(',');
            while (commaIndex != -1) {
                makePrimaryOrderingWithSelectorForSingleKey(
                    aSelector,
                    orderingArray,
                    key.substring(commaIndex + 1));
                key = key.substring(0, commaIndex);
                commaIndex = key.lastIndexOf(',');
            }
        }
        makePrimaryOrderingWithSelectorForSingleKey(aSelector,
                                                    orderingArray,
                                                    key);
    }

    protected AWTSortOrdering getSortOrdering(int aSelector, String key)
    {
        AWTSortOrdering ordering = (AWTSortOrdering)valueForBinding(BindingNames.createSortOrdering);

        if (ordering != null) {
            // intialize selector (ascending / descending)
            ordering.setSelector(aSelector);
        } else {
            // if no ordering bound, then default to using AWTSortOrdering
            ordering = AWTSortOrdering.sortOrderingWithKey(key, aSelector);
        }

        return ordering;
    }

    private void makePrimaryOrderingWithSelectorForSingleKey (int aSelector, List orderingArray, String key)
    {
        AWTSortOrdering aNewOrdering = getSortOrdering(aSelector,key);
        orderingArray.add(0, aNewOrdering);
        if (orderingArray.size() > 3) {
                // ** limits sorting to 3 levels
            ListUtil.removeLastElement(orderingArray);
        }
    }

        /////////////
        // Actions
        /////////////
    public AWComponent toggleClicked ()
    {
        List orderingArray = sortOrderingArray();
        int order = currentKeyToggledOrdering();
        removeOrdering(orderingArray);
        makePrimaryOrderingWithSelector(order, orderingArray);

        if (_displayGroup != null) {
            _displayGroup.setSortOrderings(orderingArray);
        }
        else {
            setValueForBinding(orderingArray, BindingNames.sortOrderings);
        }

        AWTDataTable table = AWTDataTable.currentInstance(this);
        table.pushTableConfig();
        return null;
    }

    int currentKeyToggledOrdering ()
    {
        int state = descendingSelector();
        if (isCurrentKeyPrimary()) {
            state = primaryKeyOrderingSelector();
        }
        else if (_displayGroup != null) {
            // If this key is in the primary orderings, then consider its ordering in there as the current ordering
            List orderings = _displayGroup.primarySortOrderings();
            if (orderings != null) {
                // Get ordering to match on (create or invoke create binding)
                AWTSortOrdering toMatch = getSortOrdering(state, _key);
                int index = orderings.indexOf(toMatch);
                if (index != -1) {
                    AWTSortOrdering ordering = (AWTSortOrdering)orderings.get(index);
                    state = ordering.selector();
                }
            }
        }

        return isAscendingSelector(state) ? descendingSelector() : ascendingSelector();
    }

    private int ascendingSelector ()
    {
        return _caseSensitive ? AWTSortOrdering.CompareAscending : AWTSortOrdering.CompareCaseInsensitiveAscending;
    }

    private int descendingSelector ()
    {
        return _caseSensitive ? AWTSortOrdering.CompareDescending : AWTSortOrdering.CompareCaseInsensitiveDescending;
    }

    private boolean isAscendingSelector (int selector)
    {
        return (selector == AWTSortOrdering.CompareCaseInsensitiveAscending) || (selector == AWTSortOrdering.CompareAscending);
    }

    private boolean isDescendingSelector (int selector)
    {
        return (selector == AWTSortOrdering.CompareCaseInsensitiveDescending) || (selector == AWTSortOrdering.CompareDescending);
    }
}
