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

    $Id: //ariba/platform/ui/widgets/ariba/ui/widgets/TableHeaderDelegate.java#7 $
*/

package ariba.ui.widgets;

/**
    The TableHeaderDelegate is specified as an attribute of a
    a TableHeaderRow tag.  It is consulted when the user clicks
    on a column to do sorting, or clicks the select all checkbox.
    The delegate is expected to carry out whatever is needed such
    that rendering the table will reflect the appropriate change
    (e.g. the data is sorted appropriately or all rows have their
    columns checked).
    @aribaapi ariba
*/
public interface TableHeaderDelegate
{
    /**
        This method is called when the user clicks the label for
        a sortable column.  The delegate should ensure that the
        data is sorted when it is displayed.
        @param key The unique key identifying the selected column
                   as specified when the column was constructed.
        @param ascending True if the data should be sorted in ascending order.
        @aribaapi ariba
    */
    public void sort (Object key, boolean ascending);
    
    /**
        This method is called when the user clicks the select
        all checkbox in the header.
        @param flag true if the selet all checkbox was checked on.
        @aribaapi ariba
    */
    public void selectAll (boolean flag);
}
