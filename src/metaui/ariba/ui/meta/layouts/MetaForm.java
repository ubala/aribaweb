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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/layouts/MetaForm.java#12 $
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
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.meta.editor.EditManager;
import ariba.ui.meta.editor.MetaSideInspector;
import ariba.ui.widgets.AribaPageContent;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

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
    EditManager _editManager;
    public String _className;
    public String _zonePath;
    public String _zone;
    public boolean renderedFirst;

    public boolean isStateless() {
        return false;
    }

    public Object currentProperties ()
    {
        return MetaContext.currentContext(this).properties();
    }

    public boolean shouldHideZone (String zone)
    {
        return dragType() == null && (_fieldsByZone == null || ListUtil.nullOrEmptyList(_fieldsByZone.get(zone)));
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

    public void initClass ()
    {
        Context context = MetaContext.currentContext(this);
        _className = (String)context.values().get(ObjectMeta.KeyClass);
        String zonePath = UIMeta.getInstance().zonePath(context);
        _zonePath = (zonePath == null) ? null : zonePath.concat(".");

        _editManager = EditManager.activeEditManager(UIMeta.getInstance(), session());

        // register validation callback if necessary
        Boolean editing = (Boolean)context.propertyForKey(UIMeta.KeyEditing);
        if (editing != null && editing.booleanValue()) {
            _object = context.values().get("object");
            _contextSnapshot = context.snapshot();
            // ToDo: Need to change error manager not to add dups
            errorManager().registerFullValidationHandler(this);
        }
    }

    public String zonePath ()
    {
        return (_zonePath == null) ? _zone : _zonePath.concat(_zone);
    }

    public String dropAreaClass ()
    {
        return "dropArea awDrp_".concat(_className);
    }

    public String dragType ()
    {
        return (_editManager == null) ? null : _className;
    }

    public String dragClass ()
    {
        String dragType = dragType();
        return (dragType != null)
                ? Fmt.S("dropArea awDrp_%s awDrgCnt_%s%s", dragType, dragType,
                    (isInspectedField() ? " selReg" : ""))
                : null;
    }

    static final String SessionFieldKey = "metaForm.F";

    public void dragAction ()
    {
        session().dict().put(SessionFieldKey, MetaContext.currentContext(this).debugTracePropertyProvider());
    }

    public void dropAction ()
    {
        Object dragContext = session().dict().get(SessionFieldKey);
        Object dropContext = MetaContext.currentContext(this).debugTracePropertyProvider();
        // AribaPageContent.setMessage(Fmt.S("Dragged %s onto %s", dragContext, dropContext), session());
        _editManager.handleDrop(dragContext, dropContext);
    }

    public void handleClickedAction ()
    {
        MetaSideInspector.initialize();
        Context context = MetaContext.currentContext(this);        
        _editManager.setSelectedRecord((Context.AssignmentRecord)context.debugTracePropertyProvider());
    }

    public boolean isInspectedField ()
    {
        Context context = MetaContext.currentContext(this);
        return _editManager.isCurrentFieldSelected(context);
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
