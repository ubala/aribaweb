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

    $Id: //ariba/platform/ui/demoshell/ariba/ui/demoshell/ExcelFill.java#4 $
*/
package ariba.ui.demoshell;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.ui.aribaweb.core.AWCookie;
import ariba.ui.aribaweb.core.AWConcreteApplication;
import ariba.ui.aribaweb.core.AWApplication;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWResponse;
import java.util.List;

public class ExcelFill extends AWComponent
{

    public String rowFields[] = { "Commodity", "Supplier"};
    public String columnFields[] = {"Quarter"};
    public String dataFields[] = {"Amount"};
    String _dataUrl = null;

    public String downloadURL ()
    {
        // should point to a direct action...
        // return "http://localhost:8080/docroot/demoroot/RawTable.htm";
        if (_dataUrl == null) {
            AWComponent table = pageWithName("ExcelTable");
            _dataUrl = DemoShellActions.urlRegisteringResponseComponent(table);
        }
        return _dataUrl;
    }


    /*
    public AWResponseGenerating downloadTemplate ()
    {
        String templatePath = application().resourceManager().pathForResourceNamed("ExcelTemplate.xls");
        AWResponse response = application().createResponse();
        response.setContentFromFile(templatePath);
        response.setContentType(AWContentType.ApplicationVndMsexcel); // use unknow to avoid popping open in IE window

        return response;
    }
    */
}
