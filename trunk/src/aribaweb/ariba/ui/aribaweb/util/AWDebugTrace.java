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

    $Id: //ariba/platform/ui/aribaweb/ariba/ui/aribaweb/util/AWDebugTrace.java#8 $
*/
package ariba.ui.aribaweb.util;

import ariba.util.core.MapUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.ClassExtension;
import ariba.util.core.ClassExtensionRegistry;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.core.AWComponentDefinition;
import ariba.ui.aribaweb.core.AWComponentReference;
import ariba.ui.aribaweb.core.AWBaseElement;
import ariba.ui.aribaweb.core.AWBindableElement;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

public class AWDebugTrace
{
    AWRequestContext _requestContext;
    ComponentTraceNode _currentComponentTraceNode;
    MetadataTraceNode _currentMetadataTraceNode;
    boolean _assignMetaDataToNext;
    boolean _doingPathTrace;
    List<ComponentTraceNode> _componentPathList;
    AWComponentDefinition _mainComponentDefinition;

    public AWDebugTrace (AWRequestContext requestContext)
    {
        _requestContext = requestContext;
        _mainComponentDefinition = requestContext.pageComponent().componentDefinition();
    }

    public void didFinishPathTracePhase ()
    {
        _currentComponentTraceNode = null;
        _currentMetadataTraceNode = null;
        _assignMetaDataToNext = false;
        _doingPathTrace = false;
        _mainComponentDefinition = null;
    }

    public List<ComponentTraceNode> componentPathList ()
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
    
    public void pushComponentPathEntry (AWBindableElement componentReference)
    {
        if (_componentPathList == null) _componentPathList = ListUtil.list();
        _currentComponentTraceNode = new ComponentTraceNode(componentReference, ListUtil.lastElement(_componentPathList));
        _componentPathList.add(_currentComponentTraceNode);
        if (_assignMetaDataToNext && _currentMetadataTraceNode != null) {
            _currentComponentTraceNode._associatedMetadata = _currentMetadataTraceNode;
            _assignMetaDataToNext = false;
        }
        _doingPathTrace = true;
    }

    public ComponentTraceNode componentTraceRoot ()
    {
        // move up to root (in case we're called when were still in the middle)
        AWDebugTrace.ComponentTraceNode node = currentComponentTraceNode();
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
                : componentTraceRoot().componentDefinition();
    }
    
    public void pushTraceNode (AWBindableElement element)
    {
        _currentComponentTraceNode = currentComponentTraceNode().pushChild(element);

        // See if we have a pending metadata association
        if (_assignMetaDataToNext && _currentMetadataTraceNode != null) {
            _currentComponentTraceNode._associatedMetadata = _currentMetadataTraceNode;
            _assignMetaDataToNext = false;
        }
        
        // Record the "main" component to highlight by default in the viewer
        if (_mainComponentDefinition == null) {
            if (element instanceof AWComponentReference) {
                AWComponentDefinition def = ((AWComponentReference)element).componentDefinition();
                if (!def.isStateless()) _mainComponentDefinition = def;
            }
            /*
            if (component == component.pageComponent()) {
                _mainComponentDefinition = component.componentDefinition();
            }
            */
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

    public static String elementName (AWBindableElement element)
    {
        return (element instanceof AWComponentReference)
                ? ((AWComponentReference)element).componentDefinition().componentName()
                : element.tagName();
    }


    public static class ComponentTraceNode implements Cloneable
    {
        AWBindableElement _element;
        AWBindableElement _sourceReference;
        ComponentTraceNode _parent;
        List<ComponentTraceNode> _children;
        int _suppressLevels = 0;
        AWBaseElement _suppressingElement;
        MetadataTraceNode _associatedMetadata;

        public ComponentTraceNode (AWBindableElement ref, ComponentTraceNode parent)
        {
            _parent = parent;
            _element = ref;
        }

        protected Object clone() throws CloneNotSupportedException
        {
            ComponentTraceNode clone = (ComponentTraceNode)super.clone();
            if (clone._children != null) {
                List<ComponentTraceNode> newList = new ArrayList();
                for (ComponentTraceNode child : clone._children) {
                    newList.add((ComponentTraceNode)child.clone());
                }
                clone._children = newList;
            }
            return clone;
        }

        public ComponentTraceNode cloneTree ()
        {
            try {
                return (ComponentTraceNode)clone();
            } catch (CloneNotSupportedException e) {
                // won't happen...
                return null;
            }
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

        public ComponentTraceNode pushChild (AWBindableElement ref)
        {
            // already suppressing children?
            boolean suppress = (_suppressLevels > 0) || (_suppressingElement != null);

            // check if we are to suppress this one
            if (!suppress) {
                // check for suppression match both in source template and referenced component
                Boolean sup = (Boolean)_ComponentDefinitionsToSuppress.get(elementName(ref));
                if (sup != null && sup.booleanValue()) {
                    suppress = true;
                }
                else if (_children != null) {
                    ComponentTraceNode lastSibling = ListUtil.lastElement(_children);
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

        interface NodeFilter {
            boolean shouldKeep (ComponentTraceNode node);
        }

        // recursively collapse skip children
        // Skip if we're in the Suppress list and have only one child.
        // We preserve our reference as the "source" reference and then take our childs ref and children
        public ComponentTraceNode collapseChildren (NodeFilter filter)
        {
            if (_children != null) {
                int i = _children.size();
                while (i-- > 0) {
                    ComponentTraceNode child = _children.get(i);
                    // recurse to children, and do replace / removal as necessary
                    child = child.collapseChildren(filter);
                    if (child != null) _children.set(i, child); else _children.remove(i);
                }
            }
            // See if we should be removed
            if (!filter.shouldKeep(this)) {
                if (_children == null || _children.size() == 0) {
                    // remove us
                    return null;
                } else if (_children.size() == 1) {
                    // use our child
                    ComponentTraceNode child = _children.get(0);
                    child._sourceReference = _element;
                    return child;
                }
            }

            return this;
        }

        // Collapse suppressed single-child parents
        public ComponentTraceNode collapseChildren ()
        {
            return collapseChildren(new NodeFilter () {
                public boolean shouldKeep(ComponentTraceNode node)
                {
                    Boolean sup = (Boolean)_ComponentDefinitionsToSuppress.get(elementName(node._element));
                    return (sup == null || sup.booleanValue());
                }
            });
        }

        // Collapse suppressed single-child parents
        public ComponentTraceNode collapseNonMetadataChildren ()
        {
            return collapseChildren(new NodeFilter () {
                public boolean shouldKeep(ComponentTraceNode node)
                {
                    Boolean sup = (Boolean)_ComponentDefinitionsToSuppress.get(elementName(node._element));
                    return (sup == null || sup.booleanValue()) && node.associatedMetadata() != null;
                }
            });
        }

        public ComponentTraceNode findFirstNodeMatching (AWComponentDefinition componentDefinition)
        {
            if (componentDefinition() == componentDefinition) return this;
            if (_children != null) {
                int i=0, count = _children.size();
                for ( ; i < count; i++) {
                    ComponentTraceNode child = _children.get(i);
                    ComponentTraceNode match = child.findFirstNodeMatching(componentDefinition);
                    if (match != null) return match;
                }
            }
            return null;
        }

        public void setSourceReference (AWComponentReference ref) { _sourceReference = ref; }
        public List<ComponentTraceNode> children () { return _children; }
        public AWBindableElement element() { return _element; }
        public AWBindableElement sourceReference () { return (_sourceReference != null) ? _sourceReference : _element; }
        public MetadataTraceNode associatedMetadata () { return _associatedMetadata; }

        public Object associatedMetadataProvider () {
            return (_associatedMetadata != null)
                    ? _associatedMetadata._metaPropertyProvider
                    : null;
        }

        public Map associatedMetadataProperties ()
        {
            return (_associatedMetadata != null)
                    ? _associatedMetadata.properties(_element)
                    : null;
        }

        public Map<AssignmentSource, List<Assignment>> associatedMetadataAssignmentMap ()
        {
            return (_associatedMetadata != null)
                    ? _associatedMetadata.assignmentMap(_element)
                    : null;
        }

        public AWComponentDefinition componentDefinition()
        {
            return (_element instanceof AWComponentReference) 
                    ? ((AWComponentReference)_element).componentDefinition()
                    : null;
        }

        public boolean elementIsStateless ()
        {
            return !(_element instanceof AWComponentReference)
                    || ((AWComponentReference)_element).componentDefinition().isStateless();
        }
    }

    // When called in append (component inspector, need to call "pop", in invoke don't call pop)
    public void pushMetadata (String title, Object metaPropertyProvider)
    {
        pushMetadata(title, metaPropertyProvider, false);
    }

    boolean assignMetaOnPush (boolean assignToNext)
    {
        // since ordering is reversed when doing a path inspect "Next" becomes previous...
        return _doingPathTrace ^ !assignToNext;
    }

    public void pushMetadata (String title, Object metaPropertyProvider, boolean assignToNext)
    {
        if (_currentMetadataTraceNode == null) _currentMetadataTraceNode = new MetadataTraceNode(null, null, null);
        MetadataTraceNode node = new MetadataTraceNode(title, metaPropertyProvider, _currentMetadataTraceNode);
        _currentMetadataTraceNode.addChild(node);

        // We nest in append (component inspector), but just keep a list in invoke (click path)
        // if (_requestContext.currentPhase() == AWRequestContext.Phase_Render)
        _currentMetadataTraceNode = node;
        _assignMetaDataToNext = !assignMetaOnPush(assignToNext);
        if (_currentComponentTraceNode != null && !_assignMetaDataToNext) _currentComponentTraceNode._associatedMetadata = node;
    }

    public void popMetadata ()
    {
        _currentMetadataTraceNode = _currentMetadataTraceNode._parent;
    }

    public static class MetadataTraceNode
    {
        MetadataTraceNode _parent;
        String _title;
        Object _metaPropertyProvider;
        List _children;

        AssignmentRecorder _recorder;
        Map _properties;

        public MetadataTraceNode (String title, Object propertyProvider, MetadataTraceNode parent)
        {
            _parent = parent;
            _title = title;
            _metaPropertyProvider = propertyProvider;
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

        public String title (AWBindableElement element)
        {
            return (_metaPropertyProvider != null)
                ? MetaProvider.get(_metaPropertyProvider).title(_metaPropertyProvider, _title, element)
                : null;
        }

        public Map properties (AWBindableElement element)
        {
            if (_properties == null) {
                _properties = (_metaPropertyProvider != null)
                    ? MetaProvider.get(_metaPropertyProvider).metaProperties(_metaPropertyProvider, element)
                    : null;
            }
            return _properties;
        }

        public Map<AssignmentSource, List<Assignment>> assignmentMap (AWBindableElement element)
        {
            if (_recorder == null) {
                _recorder = new AssignmentRecorder();
                MetaProvider.get(_metaPropertyProvider).recordAssignments(_metaPropertyProvider, _recorder, element);
            }
            return _recorder.getAssignments();
        }
    }

    abstract static public class MetaProvider extends ClassExtension
    {
        private static final ClassExtensionRegistry ClassExtensionRegistry = new ClassExtensionRegistry();

        // ** Thread Safety Considerations: ClassExtension cache can be considered read-only, so it needs no external locking.

        public static void registerClassExtension (Class receiverClass, MetaProvider providerClassExtension)
        {
            ClassExtensionRegistry.registerClassExtension(receiverClass, providerClassExtension);
        }

        public static MetaProvider get (Object target)
        {
            return (MetaProvider)ClassExtensionRegistry.get(target.getClass());
        }

        ////////////
        // Api
        ////////////
        abstract public Map<String, Object> metaProperties (Object receiver, AWBindableElement element);

        // default assignment record for a property map.  Subclasses should override...
        public void recordAssignments (Object receiver, AssignmentRecorder recorder, AWBindableElement element)
        {
            Map<String, Object>  map = metaProperties(receiver, element);
            recorder.setCurrentSource(0, "Values", "");
            for (Map.Entry<String, Object> e : map.entrySet()) {
                recorder.registerAssignment(e.getKey(), e.getValue());
            }
        }

        public String title (Object receiver, String title, AWBindableElement element)
        {
            return title;
        }
    }

    static public class AssignmentRecorder
    {
        Map <AssignmentSource, List<Assignment>> _assignments = new HashMap();
        AssignmentSource _currentSource;
        boolean _didProcess;

        public void setCurrentSource (int rank, String description, String location)
        {
            _currentSource = new AssignmentSource(rank, description, location);
            _assignments.put(_currentSource, new ArrayList());
        }

        public void registerAssignment (String key, Object value)
        {
            _assignments.get(_currentSource).add(new Assignment(key, value));
        }

        public Map<AssignmentSource, List<Assignment>> getAssignments()
        {
            if (!_didProcess) _process();
            return _assignments;
        }

        /**
            Marks all but the highest ranking assignments for a given key as overridden
         */
        void _process ()
        {
            _didProcess = true;
            List <AssignmentSource> sources = new ArrayList(_assignments.keySet());
            Collections.sort(sources, new Comparator<AssignmentSource> () {
                public int compare(AssignmentSource assignmentSource, AssignmentSource assignmentSource1)
                {
                    return assignmentSource.rank - assignmentSource1.rank;
                }
            });

            Map<String, Assignment> lastAssignmentForKey = new HashMap();
            for (AssignmentSource s : sources) {
                for (Assignment a : _assignments.get(s)) {
                    Assignment last = lastAssignmentForKey.get(a.key);
                    if (last != null) last.isOverridden = true;
                    lastAssignmentForKey.put(a.key, a);
                }
            }
        }
    }

    public static class Assignment
    {
        String key;
        Object value;
        boolean isOverridden;

        public Assignment(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public boolean isOverridden()
        {
            return isOverridden;
        }
    }

    public static class AssignmentSource
    {
        int rank;
        String description;
        String location;

        public AssignmentSource(int rank, String description, String location)
        {
            this.rank = rank;
            this.description = description;
            this.location = location;
        }

        public int getRank()
        {
            return rank;
        }

        public String getDescription()
        {
            return description;
        }

        public String getLocation()
        {
            return location;
        }

        public String locationShortName()
        {
            return (location != null) ? AWUtil.lastComponent(location, "/") : "Unknown";
        }
    }

    protected static class MetaProvider_Map extends MetaProvider
    {
        ////////////
        // Api
        ////////////
        public Map<String, Object> metaProperties (Object receiver, AWBindableElement element)
        {
            return (Map)receiver;
        }
    }

    static {
        MetaProvider.registerClassExtension(Map.class, new MetaProvider_Map());
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
           "AWHighLightedErrorScope",

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
