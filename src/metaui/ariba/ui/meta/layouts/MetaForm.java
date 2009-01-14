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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaForm.java#1 $
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
    public Object _object;
    public ItemProperties _field;
    public List<ItemProperties> _allFields;
    public Map<String, List<ItemProperties>> _fieldsByZone;
    Context _contextSnapshot;

    public void init() {
        super.init();
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        Context context = MetaContext.currentContext(this);
        UIMeta meta = (UIMeta)context.meta();

        _object = context.values().get("object");
        if (!hasBinding("useFourZone") || booleanValueForBinding("useFourZone")) {
            _fieldsByZone = meta.fieldsByZones(context);
            _allFields = null;
        } else {
            _allFields = meta.fieldList(context);
            _fieldsByZone = null;
        }

        // register validation callback if necessary
        Boolean editing = (Boolean)context.propertyForKey(UIMeta.KeyEditing);
        if (editing != null && editing.booleanValue()) {
            _contextSnapshot = context.snapshot();
            // FIXME!!  Need to change error manager not to add dups
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
            for (List<ItemProperties> fields : _fieldsByZone.values()) {
                processValidationForFields(fields);
            }
        } else {
            processValidationForFields(_allFields);
        }
    }

    void processValidationForFields (List<ItemProperties>fields)
    {
        for (ariba.ui.meta.core.ItemProperties fi : fields) {
            if (fi.properties().get(UIMeta.KeyValid) != null) {
                // restore context for validation evaluation
                _contextSnapshot.restoreActivation(fi.activation());
                String errorMessage = UIMeta.validationError(_contextSnapshot);
                _contextSnapshot.pop();
                if (errorMessage != null) {
                    recordValidationError(new AWErrorInfo(_object, fi.name(), null,
                        errorMessage, null, false));
                }
            }
        }
    }
}
