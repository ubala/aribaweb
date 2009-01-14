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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaForm.java#3 $
*/
package ariba.ui.meta.layouts;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWErrorInfo;
import ariba.ui.aribaweb.core.AWFullValidationHandler;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.Context;

import java.util.List;
import java.util.Map;

public class MetaForm extends AWComponent implements AWFullValidationHandler
{
    public static String[] ZonesTLRBD = {UIMeta.ZoneLeft, UIMeta.ZoneRight,
                                         UIMeta.ZoneTop, UIMeta.ZoneBottom,
                                         "zDetail"};
    public Object _object;
    public String _field;
    public Map<String, List<String>> _fieldsByZone;
    Context.Snapshot _contextSnapshot;
    public Object _properties; 

    public void init() {
        super.init();
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        Context context = MetaContext.currentContext(this);

        _object = context.values().get("object");

        // register validation callback if necessary
        Boolean editing = (Boolean)context.propertyForKey(UIMeta.KeyEditing);
        if (editing != null && editing.booleanValue()) {
            _contextSnapshot = context.snapshot();
            // ToDo: Need to change error manager not to add dups
            errorManager().registerFullValidationHandler(this);
        }
        super.renderResponse(requestContext, component);
    }

    public boolean isStateless() {
        return false;
    }

    public Object currentProperties ()
    {
        return MetaContext.currentContext(this).properties();
    }

    public void evaluateValidity(AWComponent pageComponent)
    {
        if (_fieldsByZone != null) {
            for (String zone : ZonesTLRBD) {
                List<String> fields = _fieldsByZone.get(zone);
                if (fields != null) processValidationForFields(fields);
            }
        }
    }

    void processValidationForFields (List<String> fields)
    {
        for (String fi : fields) {
            // restore context for validation evaluation
            Context context = _contextSnapshot.hydrate();
            context.push();
            context.set(UIMeta.KeyField, fi);
            String errorMessage = UIMeta.validationError(context);
            context.pop();
            if (errorMessage != null) {
                recordValidationError(new AWErrorInfo(_object, fi, null,
                    errorMessage, null, false));
            }
        }
    }
}
