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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWMetaInspectorPane.java#1 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWBindableElement;
import ariba.ui.aribaweb.core.AWSession;
import ariba.ui.aribaweb.core.AWConstants;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWDebugTrace.ComponentTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.MetadataTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.AssignmentRecorder;
import ariba.ui.aribaweb.util.AWDebugTrace.AssignmentSource;
import ariba.ui.aribaweb.util.AWDebugTrace.Assignment;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.outline.OutlineState;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.widgets.Log;
import ariba.ui.widgets.AribaPageContent;
import ariba.util.core.ListUtil;
import ariba.util.core.URLUtil;
import ariba.util.core.Fmt;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class AWMetaInspectorPane extends AWComponent
{
    public ComponentTraceNode _traceNode;
    public MetadataTraceNode _metadataNode;
    AWDebugTrace _debugTrace;

    public boolean _showProperties;
    public String _propertyKey;
    public AssignmentSource _assignmentSource;
    public Assignment _assignment;

    public boolean isStateless ()
    {
        return false;
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