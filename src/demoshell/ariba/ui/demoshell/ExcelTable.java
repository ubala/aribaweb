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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/ExcelTable.java#5 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import java.util.Map;

import java.util.Random;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.util.core.MapUtil;

public class ExcelTable extends AWComponent
{
    static Random _random = new Random();
    public String dimensionLabels[] = { "Supplier", "Commodity", "Quarter"};
    public String supplierValues[] = {"Dell", "Staples", "ManPower"};
    public String commodityValues[] = {"Office Supplies", "Services", "Computers"};
    public String quarterValues[] = {"Qtr 1", "Qtr 2", "Qtr 3", "Qtr 4"};

    public String currentSupplier;
    public String currentCommodity;
    public String currentQuarter;
    public String currentDimension;
    public boolean sendAsExcel = false;

    Map _valuesByDimension;

    public void init() {
        super.init();
        _valuesByDimension = MapUtil.map();
        _valuesByDimension.put(dimensionLabels[0], supplierValues);
        _valuesByDimension.put(dimensionLabels[1], commodityValues);
        _valuesByDimension.put(dimensionLabels[2], supplierValues);
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component)
    {
        // set content to Excel
        if (sendAsExcel) {
            requestContext.response().setContentType(AWContentType.ApplicationVndMsexcel);
        }
        super.renderResponse(requestContext, component);
    }

    public boolean hasValue ()
    {
        return _random.nextFloat() < 0.3;  // 30% density
    }

    public float currentValue () {
        return _random.nextFloat() * 20000;  // values between 0 and 20,000
    }
}
