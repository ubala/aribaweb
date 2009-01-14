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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTColumnManager.java#4 $
*/

package ariba.ui.table;

import ariba.ui.aribaweb.core.AWResponseGenerating;

/**
    Column managers, that implement this interface, can register themselves with an
    AWTDataTable in order to get callbacks before and after the table executes
    each phase.
    @aribaapi private
*/
public interface AWTColumnManager
{

    /**
        Do work before the table's takeValues
        @param table The table executing the phase
        @aribaapi private
    */
    public void preTake (AWTDataTable table);

    /**
        Do work after the table's takeValues
        @param table The table executing the phase
        @aribaapi private
    */
    public void postTake (AWTDataTable table);

    /**
        Do work before the table's invokeAction
        @param table The table executing the phase
        @aribaapi private
    */
    public void preInvoke (AWTDataTable table);

    /**
        Do work after the table's invokeAction
        @param table The table executing the phase
        @aribaapi private
    */
    public void postInvoke (AWTDataTable table, AWResponseGenerating result);

    /**
        Do work before the table's renderResponse
        @param table The table executing the phase
        @aribaapi private
    */
    public void preAppend (AWTDataTable table);

    /**
        Do work after the table's renderResponse
        @param table The table executing the phase
        @aribaapi private
    */
    public void postAppend (AWTDataTable table);
}
