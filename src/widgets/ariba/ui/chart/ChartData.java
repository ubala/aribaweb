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

    $Id: //ariba/platform/ui/widgets/ariba/ui/chart/ChartData.java#1 $
*/
package ariba.ui.chart;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.util.fieldvalue.FieldPath;
import ariba.util.core.Assert;

import java.util.List;
import java.util.Arrays;

public class ChartData extends AWComponent
{
    public static List ChartTypes = Arrays.asList (
        "Bar2D", "Column2D", "Column3D", "Doughnut2D",
        "Funnel", "Line", "Pie2D", "Pie3D"
    );

    public static String[] colors = new String [] {
        "#9999FF", "#CCFF66", "#666699", "#FFCC66",
        "#99AD50", "#FFFF99", "#CCCCFF", "#6600FF",
        "#F280FF", "#0099FF", "#CCCCCC"
    };

    public Object _item;
    public int _index;
    FieldPath _label, _value;

    protected void awake()
    {
        super.awake();
        String labelKey = stringValueForBinding("labelKey");
        String valueKey = stringValueForBinding("valueKey");
        Assert.that(labelKey != null && valueKey != null, "Require labelKey and valueKey bindings");
        _label = FieldPath.sharedFieldPath(labelKey);
        _value = FieldPath.sharedFieldPath(valueKey);
    }

    protected void sleep()
    {
        super.sleep();
        _label = null;
        _value = null;
        _item = null;
    }

    public Object label ()
    {
        return _label.getFieldValue(_item);
    }

    public Object value ()
    {
        return _value.getFieldValue(_item);        
    }

    public String color()
    {
        return colors[_index % colors.length];
    }
}
