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

    $Id: //ariba/platform/ui/widgets/ariba/ui/chart/Chart.java#1 $
*/

package ariba.ui.chart;

import ariba.util.core.Assert;
import ariba.util.core.FastStringBuffer;
import ariba.util.core.ListUtil;
import ariba.util.core.StringUtil;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.html.BindingNames;


public class Chart extends AWComponent
{
    public Object _objId, _dataId;

    private static final String[] SupportedBindingNames = {
        BindingNames.type, BindingNames.width, BindingNames.height, BindingNames.filename,
        "animation"
    };

    public String[] supportedBindingNames ()
    {
        return SupportedBindingNames;
    }

    protected void sleep()
    {
        _objId = null;
        _dataId = null;
        super.sleep();
    }

    public String swfUrl ()
    {
        String filename = stringValueForBinding(BindingNames.filename);
        if (filename == null) {
            String type = stringValueForBinding("type");
            Assert.that(type != null, "Must specify filename or type for chart");
            filename = "fusioncharts/FCF_" + type + ".swf";
        }
        String url = session().resourceManager().urlForResourceNamed(filename);
        Assert.that(url !=null, "Couldn't find resource for chart file: %s", filename);
        return url;
    }
}
