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

    $Id: //ariba/platform/ui/widgets/ariba/ui/validation/FormTable.java#10 $
*/
package ariba.ui.validation;

import ariba.ui.aribaweb.core.AWComponent;

public class FormTable extends AWComponent
{
    public boolean _omitOuterTable;
    public boolean _omitPaddingRow;
    public boolean _hasTop, _hasLeft, _hasRight, _hasBottom, _hasLR;

    public boolean alreadyInFormTable ()
    {
        // Don't need to support nested FormTable's yet
        // return env().peek("inFormTable") != null;
        return false;
    }

    protected void awake ()
    {
        super.awake();
        _omitOuterTable = booleanValueForBinding("omitTable");  // env().peek("inFormTable") != null)
        _omitPaddingRow = _omitOuterTable || booleanValueForBinding("omitLabelPadding");
        _hasTop = hasContentNamed("top");
        _hasLeft = hasContentNamed("left");
        _hasRight = hasContentNamed("right");
        _hasBottom = hasContentNamed("bottom");
        _hasLR = _hasLeft || _hasRight;
    }

    public String ftClass ()
    {
        boolean editable = booleanValueForBinding("editable", true);
        boolean labelsOnTop = booleanValueForBinding("showLabelsAboveControls");
        if (!editable && labelsOnTop) return "ftRO ftLOT";
        if (!editable) return "ftRO";
        if (labelsOnTop) return "ftLOT";
        return null;
    }
}
