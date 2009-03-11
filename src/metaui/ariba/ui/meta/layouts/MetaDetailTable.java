/*
    Copyright 2008 Craig Federighi

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License.
    You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaDetailTable.java#5 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.table.AWTDataSource;
import ariba.ui.meta.persistence.DetailDataSource;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;

public class MetaDetailTable extends AWComponent
{
    public AWTDisplayGroup _displayGroup;
    
    public boolean isStateless()
    {
        return false;
    }

    public void init()
    {
        super.init();
        _displayGroup = new AWTDisplayGroup();

        Context context = MetaContext.currentContext(this);
        String field = (String)context.values().get(UIMeta.KeyField);
        DetailDataSource dataSource =  new DetailDataSource (null, field);

        _displayGroup.setDataSource(dataSource);
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        DetailDataSource dataSource = (DetailDataSource)_displayGroup.dataSource();
        dataSource.setParentObject(MetaContext.currentContext(this).values().get(UIMeta.KeyObject));

        super.renderResponse(requestContext, component);
    }
}
