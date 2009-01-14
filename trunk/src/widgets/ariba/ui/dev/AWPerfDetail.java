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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWPerfDetail.java#2 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.table.AWTDisplayGroup;
import ariba.util.core.PerformanceStateCore;

public class AWPerfDetail extends AWComponent
{
    public PerformanceStateCore.Instance _metric;
    public AWTDisplayGroup _displayGroup;

    public void setMetric (PerformanceStateCore.Instance metric)
    {
        _metric = metric;
    }
}
