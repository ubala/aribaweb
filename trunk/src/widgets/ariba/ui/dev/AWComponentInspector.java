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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWComponentInspector.java#8 $
*/
package ariba.ui.dev;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWComponentApiManager;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.outline.OutlineState;
import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.widgets.Log;
import ariba.util.core.ListUtil;
import ariba.util.core.URLUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.List;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class AWComponentInspector extends AWComponent
{    
    public AWDebugTrace.ComponentTraceNode _traceNode;
    public AWComponentReference _currentComponentReference;
    public AWDebugTrace.MetadataTraceNode _metadataNode;
    public int _tabIndex;
    public AWTDisplayGroup _traceDisplayGroup, _pathDisplayGroup, _metadataDisplayGroup;
    public int _fileContentsTabIndex;
    AWDebugTrace _debugTrace;

    void init (AWDebugTrace debugTrace)
    {
        _pathDisplayGroup = new AWTDisplayGroup();
        _metadataDisplayGroup = new AWTDisplayGroup();
        _traceDisplayGroup = new AWTDisplayGroup();
        setDebugTrace(debugTrace);
        setShowingFileContents(true);
    }

    void setDebugTrace (AWDebugTrace debugTrace)
    {
        _debugTrace = debugTrace;

        List path = debugTrace.componentPathList();
        _pathDisplayGroup.setObjectArray(path);

        if (_debugTrace.rootMetadataTraceNode() != null) {
            _metadataDisplayGroup.setObjectArray(_debugTrace.rootMetadataTraceNode().children());
        }

        AWDebugTrace.ComponentTraceNode traceRoot = debugTrace.componentTraceRoot();
        if (traceRoot != null) {
            _traceDisplayGroup.setObjectArray(ListUtil.list(traceRoot.collapseChildren()));
            expandIfChildrenReferenceDefinition(traceRoot, _debugTrace.mainComponentDefinition(),
                    _traceDisplayGroup.outlineState());
        }

        _tabIndex = (path != null) ? 1 : 0;
    }

    public AWComponent refreshPage ()
    {
        setDebugTrace(requestContext().lastDebugTrace());
        currentDisplayGroup().setSelectedObject(null);
        return null;
    }

    // Try to do a "smart expand" by auto-opening elements rooted in the main page
    public static boolean expandIfChildrenReferenceDefinition (AWDebugTrace.ComponentTraceNode node,
                                       AWComponentDefinition componentDefinition, OutlineState outline)
    {
        boolean childMatches = false;
        List children = node.children();
        int i = (children != null) ? children.size() : 0;
        while (i-- > 0) {
            if (expandIfChildrenReferenceDefinition((AWDebugTrace.ComponentTraceNode)children.get(i),
                    componentDefinition, outline)) {
                childMatches = true;
            }   
        }
        if (childMatches) outline.setExpansionState(node, true);

        // open parent if child matches, or we do
        return childMatches || (node.componentReference().templateName() == componentDefinition.templateName())
                || (node.componentReference().componentDefinition() == componentDefinition)
                || (node.componentReference().componentDefinition().templateName() == componentDefinition.templateName());
    }

    public AWTDisplayGroup currentDisplayGroup ()
    {
        return (_tabIndex == 0) ? _traceDisplayGroup : _pathDisplayGroup;
    }

    public AWComponentReference currentComponentReference ()
    {
        return (_tabIndex == 0) ? ((_traceNode != null) ?_traceNode.sourceReference() : null)
                                : _currentComponentReference;
    }

    String currentTemplatePath ()
    {
        // return (_tabIndex == 0) ? _traceNode.componentReference().componentDefinition().templateName()
        return (_tabIndex == 0) ? _traceNode.componentReference().componentDefinition().templateName()
                                : _currentComponentReference.templateName();
    }

    public String currentResourcePath ()
    {
        String path = currentComponentReference().componentDefinition().templateName();
        if (path == null) path = currentTemplatePath();
        return path;
    }
    
    public String currentReferencePath ()
    {
        String path = currentComponentReference().templateName();
        if (path == null) path = currentTemplatePath();
        return path;
    }

    public void makeSelectionCurrentItem ()
    {
        if (_tabIndex == 0) {
            _traceNode = (AWDebugTrace.ComponentTraceNode)_traceDisplayGroup.selectedObject();
        } else {
            _currentComponentReference = (AWComponentReference)_pathDisplayGroup.selectedObject();
        }

        // make sure that we're not on the java tab if it's not available
        if (currentComponentReference() == null || javaFullPath() == null && _fileContentsTabIndex==2) _fileContentsTabIndex = 0;
    }

    public String fileName ()
    {
        return AWDebugTrace.pathComponent(resourceFullPath(), false);
    }

    public String filePath ()
    {
        return AWDebugTrace.pathComponent(resourceFullPath(), true);
    }

    public String componentName ()
    {
        return AWDebugTrace.basenameFromPath(currentTemplatePath());
    }

    public AWComponent openComponentAPIPage ()
    {
        AWApiPage page = (AWApiPage)pageWithName(AWApiPage.Name);
        AWComponentDefinition defn = AWComponentApiManager.componentDefinition(componentName());
        page.setSelectedComponent(defn);
        return page;
    }

    public String resourceFullPath ()
    {
        AWResource resource = AWComponent.templateResourceManager().resourceNamed(currentResourcePath());
        return (resource != null) ? resource.fullUrl()
                : URLUtil.urlAbsolute(new File(currentResourcePath())).toExternalForm();
    }
    
    public String referenceResourceFullPath ()
    {
        AWResource resource = AWComponent.templateResourceManager().resourceNamed(currentReferencePath());
        return (resource != null) ? resource.fullUrl()
                : URLUtil.urlAbsolute(new File(currentReferencePath())).toExternalForm();
    }

    public String referenceFileLabel ()
    {
        return AWDebugTrace.pathComponent(referenceResourceFullPath(), false) + " : " + currentComponentReference().lineNumber();
    }

    public String resourceFileExtension ()
    {
        String path = resourceFullPath();
        int index = path.lastIndexOf('.');
        return (index > 0) ? path.substring(index + 1) : null;
    }

    public static File fileForURL (String urlString)
    {
        if (urlString == null) return null;
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }
        if (!url.getProtocol().equals("file")) return null;
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public String javaFullPath ()
    {
        File file = fileForURL(resourceFullPath());
        if (file == null) return null;
        file = new File(file.getAbsolutePath().replaceFirst("\\.(\\w)+", ".java"));
        return (file.exists()) ? file.getAbsolutePath() : null;
    }
    
    public String _currentFilePath;
    public AWResponseGenerating showFile (String path)
    {
        _currentFilePath = path;
/*
        AWResponse response = application().createResponse();
        response.setContentType(AWContentType.TextPlain);
        response.setContentFromFile(javaFullPath());
        return response;
*/
        return null;
    }

    public String currentFileContents ()
    {
        return AWUtil.stringWithContentsOfFile(_currentFilePath);        
    }

    public String fileContents (String urlString, int selectedLine)
    {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            return null;
        }

        String contents = null;
        try {
            contents = AWUtil.stringWithContentsOfInputStream(url.openStream());
        } catch (IOException e) {
            // Hack to workaround fact the AW renames .htm files to .awl
            urlString = urlString.replaceFirst("\\.awl+", ".htm");
            try {
                contents = AWUtil.stringWithContentsOfInputStream(url.openStream());
            } catch (IOException e1) {
                // ignore
            }
        }
        return (contents != null) ? emitAsPreTags(contents, selectedLine) : null;
    }

    public String referenceResourceFileContents ()
    {
        return fileContents(referenceResourceFullPath(), currentComponentReference().lineNumber());
    }

    public String resourceFileContents ()
    {
        return fileContents(resourceFullPath(), -1);
    }

    public String javaFileContents ()
    {
        return emitAsPreTags(AWUtil.stringWithContentsOfFile(javaFullPath()), -1);
    }

    static AWEncodedString OpenPre = new AWEncodedString("<pre class='pl'>");
    static AWEncodedString OpenPreSelected = new AWEncodedString("<pre class='plsel'>");
    static AWEncodedString ClosePre = new AWEncodedString("</pre>\n");

    protected String emitAsPreTags (String text, int selectedLine)
    {
        AWResponse response = response();
        int lineCount = 0;
        BufferedReader reader = new BufferedReader(new StringReader(text));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                lineCount++;
                response.appendContent(lineCount == selectedLine ? OpenPreSelected : OpenPre);
                response.appendContent(AWUtil.escapeHtml(line));
                response.appendContent(ClosePre);
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    public AWResponseGenerating openJava ()
    {
        return showFile(javaFullPath());
    }
    
    public AWResponseGenerating openAWL ()
    {
        return showFile(resourceFullPath());
    }

    public void renderResponse(AWRequestContext requestContext, AWComponent component) {
        // auto-select first item if we're showing details
        if (showingFileContents() && currentDisplayGroup().selectedObject() == null) {
            if (currentDisplayGroup() == _pathDisplayGroup) {
                currentDisplayGroup().setSelectedObject(currentDisplayGroup().displayedObjects().get(0));
                _fileContentsTabIndex = 0;
            } else if (_debugTrace != null && _debugTrace.componentTraceRoot() != null) {
                currentDisplayGroup().setSelectedObject(
                        _debugTrace.componentTraceRoot().findFirstNodeMatching(_debugTrace.mainComponentDefinition()));
                _fileContentsTabIndex = 1;
            }
        }
        super.renderResponse(requestContext, component);
    }

    public AWResponseGenerating invokeAction(AWRequestContext requestContext, AWComponent component)
    {
        // if they make a selection, then show files
        AWTDisplayGroup displayGroup = currentDisplayGroup();
        boolean hadSelection = (displayGroup.selectedObject() != null);
        AWResponseGenerating result =  super.invokeAction(requestContext, component);
        if (!hadSelection && (displayGroup.selectedObject() != null)) setShowingFileContents(true);
        return result;
    }

    public boolean showingFileContents ()
    {
        Boolean val = (Boolean)session().dict().get("AWCPP_ShowingFiles");
        return (val != null) && val.booleanValue();
    }

    public void setShowingFileContents (boolean showing)
    {
        session().dict().put("AWCPP_ShowingFiles", Boolean.valueOf(showing));
    }


    //
    // AWDebugger (remote) support
    //
    boolean _debuggerRunning;
    static boolean _DebuggerInstalled = new File("internal/classes/ariba.awdebugger.zip").exists();
    String _localAddr;

    String localAddr ()
    {
        if (_localAddr == null) {
            try {
 	            _localAddr = InetAddress.getLocalHost().getHostAddress();
 	        }
 	        catch (UnknownHostException e) {
 	            _localAddr = "127.0.0.1";
 	            Log.widgets.error("Gathering local ip", e);
 	        }
        }
        return _localAddr;
    }

    public boolean showDebuggerLink ()
    {
        String host = request().remoteHostAddress();
        return _DebuggerInstalled && !_debuggerRunning && ("127.0.0.1".equals(host) || localAddr().equals(host));
    }

    public AWResponseGenerating launchDebugger ()
    {
        try {
            String[] cmd = { "runjava", "ariba.ui.awdebugger.core.Launch", "", "" };
            if (currentComponentReference() != null) {
                cmd[2] = referenceResourceFullPath();
                cmd[3] = Integer.toString(currentComponentReference().lineNumber());
            }
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.environment().put("ARIBA_RUNJAVA_USE_JDI", "false");
                Process p = pb.start();
                dumpStream(p.getErrorStream());
                dumpStream(p.getInputStream());
                _debuggerRunning = true;
                setShowingFileContents(false);
            }
        catch (Exception e) {
            Log.widgets.error("Error launching debugger", e);
        }
        return null;
    }

    private void dumpStream (final InputStream stream)
    {
        Thread thr = new Thread("output reader") {
            public void run ()
            {
                BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                int i;
                try {
                    while ((i = in.read()) != -1) {
                        System.out.print((char)i);
                    }
                }
                catch (IOException ex) {
                    Log.widgets.error("Error reading process stream", ex);
                }
                _debuggerRunning = false;
            }
        };
        thr.setPriority(Thread.MAX_PRIORITY - 1);
        thr.start();
    }
}
