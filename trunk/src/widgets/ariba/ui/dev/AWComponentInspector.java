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

    $Id: //ariba/platform/ui/widgets/ariba/ui/dev/AWComponentInspector.java#18 $
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
import java.util.List;
import java.util.ArrayList;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

public class AWComponentInspector extends AWComponent
{    
    public ComponentTraceNode _traceNode;
    public MetadataTraceNode _metadataNode;
    public int _tabIndex = -1;
    public AWTDisplayGroup _traceDisplayGroup = new AWTDisplayGroup();
    public AWTDisplayGroup _metadataDisplayGroup = new AWTDisplayGroup();
    public AWTDisplayGroup _pathDisplayGroup = new AWTDisplayGroup();
    public AWTDisplayGroup _pathMetaDisplayGroup = new AWTDisplayGroup();
    public int _fileContentsTabIndex;
    public int _fileContentsTabIndexLastChosen = -1;
    AWDebugTrace _debugTrace;
    public boolean _showingMeta;

    public static boolean isComponentPathDebuggingEnabled (AWRequestContext requestContext)
    {
        AWSession session = requestContext.session(false);
        if (session == null) {
            return false;
        }
        else {
            Boolean flag = (Boolean)session.dict().get(AWConstants.ComponentPathDebugFlagKey);
            return (flag != null) && flag.booleanValue();
        }
    }

    public static String ciLinkBH (AWRequestContext requestContext)
    {
        return isComponentPathDebuggingEnabled(requestContext) ? null : "DOpCI";
    }

    public static void togglePathDebugging (AWRequestContext requestContext)
    {
        boolean shouldEnable = !isComponentPathDebuggingEnabled(requestContext);
        AWSession session = requestContext.session(false);
        if (session != null) {
            session.dict().put(AWConstants.ComponentPathDebugFlagKey,
                        (shouldEnable ? Boolean.TRUE: Boolean.FALSE ));
        }
        if (shouldEnable) {
            AribaPageContent.setMessage("Path Debugging Enabled!  You may also Alt-click on elements to see the path to a particular part of the page", session);
        }
    }


    void init (AWDebugTrace debugTrace)
    {
        setDebugTrace(debugTrace);
        setShowingFileContents(true);
    }

    void setDebugTrace (AWDebugTrace debugTrace)
    {
        _debugTrace = debugTrace;
        _traceNode = null;
        _metadataNode = null;
        
        List<ComponentTraceNode> path = debugTrace.componentPathList();
        _pathDisplayGroup.setObjectArray(path);
        _pathMetaDisplayGroup.setObjectArray(filterMeta(path));

        ComponentTraceNode traceRoot = debugTrace.componentTraceRoot().collapseChildren();
        if (traceRoot == null) traceRoot = debugTrace.componentTraceRoot();
        setUpTraceDisplayGroup(_traceDisplayGroup,
                               traceRoot,
                               path);


        // if (_debugTrace.rootMetadataTraceNode() != null) {
        setUpTraceDisplayGroup(_metadataDisplayGroup,
                               traceRoot.cloneTree().collapseNonMetadataChildren(),
                               path);

        if (_traceDisplayGroup.filteredObjects().isEmpty()) {
            _tabIndex = 1;
        } 
        else if (_tabIndex == -1) {
            _tabIndex = (path != null) ? 1 : 0;
        }
        else if (_tabIndex == 1 && (path == null)) {
            _tabIndex = 0;
        }
    }

    public void setShowMeta (boolean yn)
    {
        _showingMeta = yn;
    }

    void setUpTraceDisplayGroup (AWTDisplayGroup displayGroup, ComponentTraceNode traceRoot,
                                 List<ComponentTraceNode> path)
    {
        if (traceRoot != null && traceRoot.children() != null) {
            displayGroup.setObjectArray(ListUtil.list(traceRoot));

            if (path != null) {
                List <ComponentTraceNode>translatedPath = translatePath(traceRoot, path);
                for (ComponentTraceNode node : translatedPath) {
                    displayGroup.outlineState().setExpansionState(node, true);
                }
                displayGroup.setSelectedObject(ListUtil.lastElement(translatedPath));
            }
            else {
                expandIfChildrenReferenceDefinition(traceRoot, _debugTrace.mainComponentDefinition(),
                        displayGroup.outlineState());
            }
        }
        else {
            displayGroup.setObjectArray(null);
        }
    }

    List<ComponentTraceNode> filterMeta (List<ComponentTraceNode> list)
    {
        if (list == null) return list;
        List<ComponentTraceNode> result = ListUtil.list();
        for (ComponentTraceNode node : list) {
            if (node.associatedMetadataProvider() != null) result.add(node);
        }
        return result;
    }

    List<ComponentTraceNode> translatePath (ComponentTraceNode root, List<ComponentTraceNode>path)
    {
        List<ComponentTraceNode>result = new ArrayList();
        ComponentTraceNode treeNode = root;
        result.add(root);
        for (int i = path.size()-1; i >= 0; i--) {
            ComponentTraceNode pathNode = path.get(i);
            AWBindableElement element = pathNode.element();
            for (ComponentTraceNode child : treeNode.children()) {
                if (child.element() == element
                        && eq(child.associatedMetadataProvider(), pathNode.associatedMetadataProvider())) {
                    result.add(child);
                    treeNode = child;
                    break;
                }
            }
        }
        return result;
    }

    boolean eq (Object a, Object b) {
        return (a == null && b == null)
                || (a != null && a.equals(b));
    }

    public AWComponent refreshPage ()
    {
        setDebugTrace(requestContext().lastDebugTrace());
        currentDisplayGroup().setSelectedObject(null);
        return null;
    }

    public void mainTabSelectionChanged ()
    {
        // if (_pathDisplayGroup.allObjects().size() > 0) _selectionPathToRestore = _pathDisplayGroup.allObjects();  
    }

    // Try to do a "smart expand" by auto-opening elements rooted in the main page
    public static boolean expandIfChildrenReferenceDefinition (ComponentTraceNode node,
                                       AWComponentDefinition componentDefinition, OutlineState outline)
    {
        boolean childMatches = false;
        List children = node.children();
        int i = (children != null) ? children.size() : 0;
        while (i-- > 0) {
            if (expandIfChildrenReferenceDefinition((ComponentTraceNode)children.get(i),
                    componentDefinition, outline)) {
                childMatches = true;
            }   
        }
        if (childMatches) outline.setExpansionState(node, true);

        // open parent if child matches, or we do
        return childMatches || (node.element().templateName() == componentDefinition.templateName())
                || (node.componentDefinition() == componentDefinition)
                || (definitionTemplateName(node.element()) == componentDefinition.templateName());
    }

    public AWTDisplayGroup currentDisplayGroup ()
    {
        return (_tabIndex == 0) ? currentTraceDisplayGroup()
                                : currentPathDisplayGroup();
    }

    public AWBindableElement currentElement()
    {
        return (_tabIndex == 0) ? ((_traceNode != null) ?_traceNode.sourceReference() : null)
                                : ((_traceNode != null) ?_traceNode.element() : null);
    }

    String currentTemplatePath ()
    {
        return (_tabIndex == 0) ? definitionTemplateName(_traceNode.element())
                                : ((_traceNode != null) ?_traceNode.element().templateName() : null);
    }

    static String definitionTemplateName (AWBindableElement element)
    {
        return (element instanceof AWComponentReference)
                ? ((AWComponentReference)element).componentDefinition().templateName()
                : element.templateName();
    }

    public String currentResourcePath ()
    {
        String path = definitionTemplateName(currentElement());
        if (path == null) path = currentTemplatePath();
        return path;
    }
    
    public String currentReferencePath ()
    {
        String path = currentElement().templateName();
        if (path == null) path = currentTemplatePath();
        return path;
    }

    public String currentFileLocation ()
    {
        return Fmt.S("%s:%s", currentReferencePath(), _traceNode.sourceReference().lineNumber());    
    }

    public AWTDisplayGroup currentTraceDisplayGroup ()
    {
        return (_showingMeta) ? _metadataDisplayGroup : _traceDisplayGroup;
    }

    public AWTDisplayGroup currentPathDisplayGroup ()
    {
        return (_showingMeta) ? _pathMetaDisplayGroup : _pathDisplayGroup;
    }

    public void makeSelectionCurrentItem ()
    {
        _traceNode = (ComponentTraceNode)currentDisplayGroup().selectedObject();

        // make sure that we're not on the java tab if it's not available
        if (currentElement() == null || javaFullPath() == null && _fileContentsTabIndex==2) _fileContentsTabIndex = 0;
        if (_fileContentsTabIndexLastChosen == -1
                && _traceNode != null && _traceNode.associatedMetadata() != null) {
            _fileContentsTabIndex = 4;
        }
    }

    public void viewTabSelected ()
    {
        _fileContentsTabIndexLastChosen = _fileContentsTabIndex;
    }

    public String fileName ()
    {
        return AWDebugTrace.pathComponent(resourceFullPath(), false);
    }

    public String filePath ()
    {
        return AWDebugTrace.pathComponent(resourceFullPath(), true);
    }

    public String nodeTitle ()
    {
        String name = AWDebugTrace.elementName(_traceNode.element());
        MetadataTraceNode meta = _traceNode.associatedMetadata();
        return (meta != null)
                ? Fmt.S("%s (%s)", meta.title(_traceNode.element()), name)
                : name;
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
        return AWDebugTrace.pathComponent(referenceResourceFullPath(), false) + " : " + currentElement().lineNumber();
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
        if (!file.exists()) file = new File(file.getAbsolutePath().replaceFirst("\\.(\\w)+", ".groovy"));
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
        return fileContents(referenceResourceFullPath(), currentElement().lineNumber());
    }

    public String resourceFileContents ()
    {
        return fileContents(resourceFullPath(), -1);
    }

    public String javaFileContents ()
    {
        return emitAsPreTags(AWUtil.stringWithContentsOfFile(javaFullPath()), -1);
    }

    static AWEncodedString OpenPre = new AWEncodedString("<pre class='pl prettyprint'>");
    static AWEncodedString OpenPreSelected = new AWEncodedString("<pre class='plsel prettyprint'>");
    static AWEncodedString ClosePre = new AWEncodedString("</pre>\n");

    // emits lineCount lines and returns false if input ended prematurely
    boolean emitLines (BufferedReader reader, AWResponse response, int lineCount)
    {
        String line = null;
        try {
            while (lineCount-- > 0 && (line = reader.readLine()) != null) {
                response.appendContent(AWUtil.escapeHtml(line));
                response.appendContent("\n");
            }
            return line != null;
        } catch (IOException e) {
            throw new AWGenericException(e);
        }
    }

    protected String emitAsPreTags (String text, int selectedLine)
    {
        boolean more = true;
        AWResponse response = response();
        BufferedReader reader = new BufferedReader(new StringReader(text));
        if (selectedLine > 0) {
            response.appendContent(OpenPre);
            more = emitLines(reader, response, selectedLine-1);
            response.appendContent(ClosePre);
        }
        if (selectedLine >=0 && more) {
            response.appendContent(OpenPreSelected);
            more = emitLines(reader, response, 1);
            response.appendContent(ClosePre);
        }
        if (more) {
            response.appendContent(OpenPre);
            emitLines(reader, response, Integer.MAX_VALUE);
            response.appendContent(ClosePre);
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
            AWTDisplayGroup currentDisplayGroup = currentDisplayGroup();
            if (currentDisplayGroup == _pathDisplayGroup || currentDisplayGroup == _pathMetaDisplayGroup) {
                if (currentDisplayGroup.filteredObjects().size() > 0) {
                    currentDisplayGroup.setSelectedObject(currentDisplayGroup().displayedObjects().get(0));
                }
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
            if (currentElement() != null) {
                cmd[2] = referenceResourceFullPath();
                cmd[3] = Integer.toString(currentElement().lineNumber());
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

    public List<AWEncodedString> parentNodes ()
    {
        return _debugTrace.getHierarchy() == null ? null : new ArrayList<AWEncodedString>(
            _debugTrace.getHierarchy().keySet());
    }

    private AWEncodedString _parentNode;

    public AWEncodedString getParentNode ()
    {
        return _parentNode;
    }

    public void setParentNode (AWEncodedString parentNode)
    {
        this._parentNode = parentNode;
    }
    
    public List<AWEncodedString> childList ()
    {
        return _debugTrace.getHierarchy().get(_parentNode);
    }
    
    private AWEncodedString _currChild;

    public AWEncodedString getCurrChild ()
    {
        return _currChild;
    }

    public void setCurrChild (AWEncodedString child)
    {
        _currChild = child;
    }
}
