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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWMetaInspectorPane.java#3 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWDebugTrace.ComponentTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.MetadataTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.AssignmentSource;
import ariba.ui.aribaweb.util.AWDebugTrace.Assignment;
import java.util.Map;

public class AWMetaInspectorPane extends AWComponent
{
    public ComponentTraceNode _traceNode;
    public MetadataTraceNode _metadataNode;
    AWDebugTrace _debugTrace;

    public boolean _showProperties;
    private String _propertyKey;
    private Object _propertyValue;
    public AssignmentSource _assignmentSource;
    public Assignment _assignment;

    public boolean isStateless ()
    {
        return false;
    }

    public String propertyKey ()
    {
        return _propertyKey;
    }

    public void setPropertyKey (String key)
    {
        _propertyKey = key;
        _propertyValue = null;
    }

    public Object propertyValue ()
    {
        if (_propertyValue == null) {
            Map map = _traceNode.associatedMetadataProperties();
            _propertyValue = map != null ? map.get(_propertyKey) : null;
        }
        return _propertyValue;
    }

    public String propertyValueAsString ()
    {
        Object value = propertyValue();
        return value != null ? value.toString() : null;
    }

    public int newlineIdx ()
    {
        String val = propertyValueAsString();
        return val != null ? val.indexOf('\n') : -1;
    }

    public boolean isPropertyValueMultiline ()
    {
        return newlineIdx() >= 0;
    }

    public Object propertyValueFirstLine ()
    {
        String val = propertyValueAsString();
        int idx = (val != null) ? val.indexOf('\n') : -1;
        if (idx >= 0) {
            return val.substring(0, idx);
        }
        return propertyValue();
    }

    protected void awake ()
    {
        _traceNode = (ComponentTraceNode)valueForBinding("traceNode");
        _metadataNode = _traceNode.associatedMetadata();
    }

    public boolean canShowAssignments ()
    {
        Map assignments = _traceNode.associatedMetadataAssignmentMap();
        return (assignments != null && assignments.size() > 1);
    }

    public boolean showProperties ()
    {
        return !canShowAssignments() || _showProperties;
    }
}