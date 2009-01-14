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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWDebugTrace.java#6 $
*/
package ariba.ui.aribaweb.util;

import ariba.ui.aribaweb.core.*;
import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;

import java.util.Map;
import java.util.List;

public class AWDebugTrace
{
    AWRequestContext _requestContext;
    ComponentTraceNode _currentComponentTraceNode;
    MetadataTraceNode _currentMetadataTraceNode;
    List _componentPathList;
    AWComponentDefinition _mainComponentDefinition;

    public AWDebugTrace (AWRequestContext requestContext)
    {
        _requestContext = requestContext;
        _mainComponentDefinition = requestContext.pageComponent().componentDefinition();
    }

    public List componentPathList ()
    {
        return _componentPathList;
    }

    public ComponentTraceNode lastComponentTrace ()
    {
        return (ComponentTraceNode)_requestContext.session().dict().get("componentTrace");
    }

    public MetadataTraceNode rootMetadataTraceNode ()
    {
        return _currentMetadataTraceNode;
    }
    
    public void pushComponentPathEntry (AWComponentReference componentReference)
    {
        if (_componentPathList == null) _componentPathList = ListUtil.list();
        _componentPathList.add(componentReference);
    }

    public ComponentTraceNode componentTraceRoot ()
    {
        // move up to root (in case we're called when were still in the middle)
        AWDebugTrace.ComponentTraceNode node = _currentComponentTraceNode;
        while (node != null && node._parent != null) node = node._parent;
        return node;
    }

    public ComponentTraceNode currentComponentTraceNode ()
    {
        if (_currentComponentTraceNode == null) {
            _currentComponentTraceNode = new AWDebugTrace.ComponentTraceNode(AWComponentReference.create(_requestContext.pageComponent().componentDefinition()), null);
            _requestContext.session().dict().put("componentTrace", _currentComponentTraceNode);
        }
        return _currentComponentTraceNode;
    }

    public AWComponentDefinition mainComponentDefinition ()
    {
        return _mainComponentDefinition != null ? _mainComponentDefinition
                : componentTraceRoot().componentReference().componentDefinition();
    }
    
    public void pushTraceNode (AWComponent component)
    {
        _currentComponentTraceNode = currentComponentTraceNode().pushChild(component.componentReference());

        // Record the "main" component to highlight by default in the viewer
        if (_mainComponentDefinition == null) {
            if (component == component.pageComponent()) {
                _mainComponentDefinition = component.componentDefinition();
            }
            else if (!component.isStateless()) {
                _mainComponentDefinition = component.componentReference().componentDefinition();
            }
        }
    }

    public void popTraceNode ()
    {
        _currentComponentTraceNode = _currentComponentTraceNode.popChild();
    }

    public void suppressTraceForCurrentScopingElement ()
    {
        currentComponentTraceNode()._suppressingElement = _requestContext.getCurrentElement();
    }

    public void existingElement (AWBaseElement element)
    {
        if (currentComponentTraceNode()._suppressingElement == element) {
            currentComponentTraceNode()._suppressingElement = null;
        }
    }

    public void markNextComponentAsMainInTrace ()
    {
        _mainComponentDefinition = null;
    }

    public static String pathComponent(String path, boolean returnPrefix)
    {
        if (path == null) return null;
        int index = path.lastIndexOf('/');
        if (index == -1) index = path.lastIndexOf('\\');
        return (index > 0)
                ? (returnPrefix ? path.substring(0, index) : path.substring(index + 1))
                : path;
    }

    public static String basenameFromPath(String path)
    {
        path = pathComponent(path, false);
        return (path != null) ? path.replaceFirst("\\.[\\w]+", "") : null;
    }


    public static class ComponentTraceNode
    {
        AWComponentReference _componentReference;
        AWComponentReference _sourceReference;
        ComponentTraceNode _parent;
        List _children;
        int _suppressLevels = 0;
        AWBaseElement _suppressingElement;
        MetadataTraceNode _associatedMetadata;

        public ComponentTraceNode (AWComponentReference ref, ComponentTraceNode parent)
        {
            _parent = parent;
            _componentReference = ref;
        }

        protected boolean suppressing ()
        {
            ComponentTraceNode node = this;
            while (node != null) {
                if (node._suppressingElement != null) return true;
                node = node._parent;
            }
            return false;
        }

        public ComponentTraceNode pushChild (AWComponentReference ref)
        {
            // already suppressing children?
            boolean suppress = (_suppressLevels > 0) || (_suppressingElement != null);

            // check if we are to suppress this one
            if (!suppress) {
                // check for suppression match both in source template and referenced component
                Boolean sup = (Boolean)_ComponentDefinitionsToSuppress.get(ref.componentDefinition().componentName());
                if (sup != null && sup.booleanValue()) {
                    suppress = true;
                }
                else if (_children != null) {
                    ComponentTraceNode lastSibling = (ComponentTraceNode) ListUtil.lastElement(_children);
                    // Our sibling is a dup (from a repetition, for instance -- suppress this child (and his sub-tree)
                    // if (lastSibling._componentReference == ref) suppress = true;
                }
            }

            if (suppress) {
                _suppressLevels++;
                return this;
            }

            ComponentTraceNode child = new ComponentTraceNode(ref, this);
            if (_children == null) _children = ListUtil.list();
            _children.add(child);

            return child;
        }

        public ComponentTraceNode popChild ()
        {
            if (_suppressLevels > 0) {
                _suppressLevels--;
                return this;
            }
            return _parent;
        }

        // recursively collapse skip children
        // Skip if we're in the Suppress list and have only one child.
        // We preserve our reference as the "source" reference and then take our childs ref and children
        public ComponentTraceNode collapseChildren ()
        {
            if (_children != null) {
                int i = _children.size();
                while (i-- > 0) {
                    ComponentTraceNode child = (ComponentTraceNode)_children.get(i);
                    // recurse to children, and do replace / removal as necessary
                    child = child.collapseChildren();
                    if (child != null) _children.set(i, child); else _children.remove(i);
                }
            }
            // See if we should be removed
            Boolean sup = (Boolean)_ComponentDefinitionsToSuppress.get(_componentReference.componentDefinition().componentName());
            if (sup != null && !sup.booleanValue()) {
                if (_children == null || _children.size() == 0) {
                    // remove us
                    return null;
                } else if (_children.size() == 1) {
                    // use our child
                    ComponentTraceNode child = (ComponentTraceNode)_children.get(0);
                    child._sourceReference = _componentReference;
                    return child;
                }
            }

            return this;
        }

        public ComponentTraceNode findFirstNodeMatching (AWComponentDefinition componentDefinition)
        {
            if (_componentReference.componentDefinition() == componentDefinition) return this;
            if (_children != null) {
                int i=0, count = _children.size();
                for ( ; i < count; i++) {
                    ComponentTraceNode child = (ComponentTraceNode)_children.get(i);
                    ComponentTraceNode match = child.findFirstNodeMatching(componentDefinition);
                    if (match != null) return match;
                }
            }
            return null;
        }

        public void setSourceReference (AWComponentReference ref) { _sourceReference = ref; }
        public List children () { return _children; }
        public AWComponentReference componentReference () { return _componentReference; }
        public AWComponentReference sourceReference () { return (_sourceReference != null) ? _sourceReference : _componentReference; }
    }

    // When called in append (component inspector, need to call "pop", in invoke don't call pop)
    public void pushMetadata (String title, String details)
    {
        if (_currentMetadataTraceNode == null) _currentMetadataTraceNode = new MetadataTraceNode(null, null, null);
        MetadataTraceNode node = new MetadataTraceNode(_currentMetadataTraceNode, title, details);
        _currentMetadataTraceNode.addChild(node);

        // We nest in append (component inspector), but just keep a list in invoke (click path)
        if (_requestContext.currentPhase() == AWRequestContext.Phase_Render) _currentMetadataTraceNode = node;
        if (_currentComponentTraceNode != null) _currentComponentTraceNode._associatedMetadata = node;
    }

    public void popMetadata ()
    {
        _currentMetadataTraceNode = _currentMetadataTraceNode._parent;
    }

    public static class MetadataTraceNode
    {
        MetadataTraceNode _parent;
        public String _title;
        public String _details;
        List _children;

        public MetadataTraceNode (MetadataTraceNode parent, String title, String details)
        {
            _parent = parent;
            _title = title;
            _details = details;
        }

        public void addChild (MetadataTraceNode child)
        {
            if (_children == null) _children = ListUtil.list();
            _children.add(child);
        }

        public List children ()
        {
            return _children;
        }
    }


    static Map _ComponentDefinitionsToSuppress = MapUtil.map();

    public static void suppressTraceForComponentNamed (String componentName, boolean suppressChildren)
    {
        // AWComponentDefinition def = ((AWApplication) AWConcreteServerApplication.sharedInstance()).componentDefinitionForName(componentName);
        _ComponentDefinitionsToSuppress.put(componentName, suppressChildren ? Boolean.TRUE : Boolean.FALSE);
    }
    public static void suppressTraceForComponents (String[] componentNames, boolean suppressChildren)
    {
        int i = componentNames.length;
        while (i-- > 0) {
            suppressTraceForComponentNamed(componentNames[i], suppressChildren);
        }
    }

    static {
        suppressTraceForComponents(new String[] {
           "AWIncludeBlock",
           "AWRefreshRegion",
           "AWClientSideScript",
           "AWSingleton",
           "AWLazyDiv",
           "AWLazyDivInternals",
           "AWRelocatableDiv",
           "AWImage",
           "AWXBasicScriptFunctions",
           "AWRecordPlayback",

           // Widgets
           "AribaCommandBar",
           "AribaPageContent",
           "WidgetInclude",
           "WidgetsJavaScript",
           "AribaImage",
           "AWTScrollTableWrapper",
           "AWTExportConditional",
           "AWTExcelWrapper",
           "AWTNullWrapper",
           "BaseTabSet",
           "TabList",
           "OulineInnerRepetition",

           // Demoshell
           "AWXAnchorTag",

           // FieldsUI
           "APWGroupEditWrapper",
           "SelectableRegionWrapper",
           "DeferredDisplayWrapper",
           "The_End"
        }, false);
    }
}
