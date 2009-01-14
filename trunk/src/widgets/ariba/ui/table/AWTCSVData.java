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

    $Id: //ariba/platform/ui/widgets/ariba/ui/table/AWTCSVData.java#8 $
*/
package ariba.ui.table;

import ariba.ui.aribaweb.core.AWElement;
import ariba.util.core.Assert;
import ariba.ui.aribaweb.core.AWBareString;
import ariba.ui.aribaweb.core.AWHtmlTemplateParser;

public final class AWTCSVData extends AWTDataTable.Column implements AWHtmlTemplateParser.LiteralBody
{
    public String rendererComponentName ()
    {
        return null;  // we don't actually serve as a column
    }

    public void initializeColumn (AWTDataTable table)
    {
        AWElement content = contentElement();
        Assert.that((content instanceof AWBareString), "Content of AWTCSVData must be a simple string (no embedded tags)");
        String csvString = ((AWBareString)content).string();
        csvString = csvString.trim();  // trim leading and trailing whitespace
        table.setDataSource(AWTCSVDataSource.dataSourceForCSVString(csvString));
    }
}
