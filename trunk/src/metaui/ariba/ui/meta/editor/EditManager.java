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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/editor/EditManager.java#11 $
*/
package ariba.ui.meta.editor;

import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.Rule;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.PropertyValue;
import ariba.ui.aribaweb.util.AWFileResource;
import ariba.ui.aribaweb.util.AWUtil;
import ariba.ui.aribaweb.util.AWGenericException;
import ariba.ui.aribaweb.util.AWResource;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.URLUtil;
import ariba.util.core.ListUtil;
import ariba.util.core.MapUtil;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;

public class EditManager
{
    public static List<String> EditorContextKeys = Arrays.asList("class", "layout", "field", "operation");
    public static List<String> FieldDefaultContextKeys = Arrays.asList("class", "field");
    private static final String SessionKey = "meta.editor.EditManager";

    boolean _editing;
    UIMeta _meta;
    List<EditSet> _ruleSets;
    Context.AssignmentRecord _selectedRecord;
    Context.Info _selectedInfo;

    public static EditManager activeEditManager (UIMeta meta, AWSession session)
    {
        EditManager current = (EditManager)session.dict().get(SessionKey);
        return (current != null && current.editing()) ? current : null;
    }

    public static EditManager currentEditManager (UIMeta meta, AWSession session, boolean create)
    {
        EditManager current = (EditManager)session.dict().get(SessionKey);
        if (current == null && create) {
            current = new EditManager(meta);
            session.dict().put(SessionKey, current);
        }
        return current;
    }

    public static void clearEditManager (UIMeta meta, AWSession session)
    {
        session.dict().remove(SessionKey);
    }

    public EditManager (UIMeta meta)
    {
        _meta = meta;
    }

    public boolean editing ()
    {
        return _editing;
    }

    public void setEditing (boolean editing)
    {
        _editing = editing;
    }

    public Context.AssignmentRecord getSelectedRecord ()
    {
        return _selectedRecord;
    }

    public void setSelectedRecord (Context.AssignmentRecord selectedRecord)
    {
        if (_selectedRecord != selectedRecord) {
            _selectedRecord = selectedRecord;
            _selectedInfo = null;
        }
    }

    public boolean isCurrentFieldSelected (Context context)
    {
        if (_selectedRecord != null) {
            if (_selectedInfo == null) {
                _selectedInfo = Context.staticContext(_meta, _selectedRecord);
            }
            if (context.currentRecordPathMatches(_selectedInfo)) {
                // update our record to the current one in case we have a stale copy
                setSelectedRecord((Context.AssignmentRecord)context.debugTracePropertyProvider());
                return true;
            }
        }
        return false;
    }

    List<EditSet> editableRuleSets ()
    {
        if (_ruleSets == null) {
            _ruleSets = ListUtil.list();
            for (AWResource resource : _meta.loadedRuleSets().keySet()) {
                if (resource instanceof AWFileResource) {
                    _ruleSets.add(new EditSet((AWFileResource)resource));
                }
            }
        }
        return _ruleSets;
    }

    EditSet editSetForContext (Context.Info contextInfo)
    {
        String packageName = packageNameForContext(contextInfo);
        if (packageName == null) return null;

        for (EditSet e : editableRuleSets()) {
            if (e._packageName.equals(packageName)) return e;
        }
        return null;
    }

    EditorProperties editorPropertiesForContext (Context.Info contextInfo)
    {
        return new EditorProperties(contextInfo);
    }

    public EditSet editSetForRule (Rule r)
    {
        for (EditSet e : editableRuleSets()) {
            if (e._ruleSet == r.getRuleSet()) return e;
        }
        return null;
    }

    public boolean hasChanges ()
    {
        for (EditSet e : editableRuleSets()) {
            if (e._dirty) return true;
        }
        return false;
    }

    public void saveChanges ()
    {
        for (EditSet e : editableRuleSets()) {
            e.save();
        }
    }

    static String packageNameForContext (Context.Info contextInfo)
    {
        String className = (String)contextInfo.contextMap.get(UIMeta.KeyClass);
        return (className != null) ? AWUtil.pathToLastComponent(className, ".") : null;
    }

    List newListForVal (Object listOrSingle)
    {
        if (listOrSingle instanceof Meta.OverrideValue) {
            listOrSingle = ((Meta.OverrideValue)listOrSingle).value();
        }

        return (listOrSingle == null)
                ? new ArrayList()
                : ((listOrSingle instanceof List)
                    ? new ArrayList((List)listOrSingle)
                    : ListUtil.list(listOrSingle));
    }

    static Object collapseNullOrSingleton (List list, boolean collapseSingleton)
    {
        Object val = list;
        if (list != null) {
            if (list.isEmpty()) {
                val = null;
            }
            else if (list.size() == 1 && collapseSingleton) {
                val = list.get(0);
            }
        }
        return val;
    }

    Map updatedListProperty (Map props, String key, Object val, List all,
                                    boolean adding, boolean collapseSingletonLists)
    {
        List localVal = newListForVal(props.get(key));
        Object newVal = localVal;

        // Adding is easy: just add to current rule
        // Removing, if not assigned in current rule, requires a total list override
        if (adding) {
            Meta.PropertyMerger merger = _meta.mergerForProperty(key);
            if (merger != null) {
                newVal = merger.merge(localVal, val, false);
            } else {
                localVal.add(val);
            }
        } else {
            if (localVal.contains(val)) {
                localVal.remove(val);
                newVal = collapseNullOrSingleton(localVal, collapseSingletonLists);
            } else {
                localVal = new ArrayList(all);
                localVal.remove(val);
                newVal = collapseNullOrSingleton(localVal, collapseSingletonLists);
                newVal = new Meta.OverrideValue(newVal);
            }
        }
        return Collections.singletonMap(key, newVal);
    }

    public void handleDrop (Object dragRec, Object dropRec)
    {
        Context.Info dragContext = Context.staticContext(_meta, (Context.AssignmentRecord)dragRec);
        Context.Info dropContext = Context.staticContext(_meta, (Context.AssignmentRecord)dropRec);
        String draggedItem = (String)dragContext.contextMap.get(dragContext.scopeKey);
        String dropItem = (String)dropContext.contextMap.get(dropContext.scopeKey);
        EditSet editSet = editSetForContext(dragContext);

        // If drag onto ones-self, no-op
        if (editSet == null || draggedItem.equals(dropItem)) return;

        // Need to find all fields with `draggedItem` as predecessor and give them
        // its predecessor
        Context context = _meta.newContext();
        prepareContext(context, dragContext, dragContext.scopeKey);
        List<String> zones = (List)context.propertyForKey("zones");
        String[] zoneArr = (zones != null) ? zones.toArray(new String[zones.size()]) : UIMeta.ZonesTLRB;
        String defaultZone = zoneArr[0];
        Map<String, List> preds = _meta.predecessorMap(context, dragContext.scopeKey, defaultZone);

        // Any items after the dragged should now be after its predecessor
        String draggedItemPred = (String)dragContext.properties.get(UIMeta.KeyAfter);
        if (draggedItemPred == null) draggedItemPred = defaultZone;
        reparentFollowers(dragContext, draggedItem, draggedItemPred, editSet, preds);

        // Any items after the new predecessor should now follow item
        reparentFollowers(dragContext, dropItem, draggedItem, editSet, preds);

        // Now place this field after its dropTarget
        Rule rule = editSet.editableRuleForContext(dragContext);
        editSet.updateRule(rule, Collections.singletonMap(UIMeta.KeyAfter, (Object)dropItem));
        System.out.printf("Resetting predecessor of %s to %s\n", draggedItem, dropItem);

        if (_selectedRecord != null) {
            setSelectedRecord((Context.AssignmentRecord)dragRec);
        }
    }

    private void reparentFollowers (Context.Info dragContext, String predecessor, String newPredecessor,
                                    EditSet editSet, Map<String, List> preds)
    {
        List followers = preds.get(predecessor);
        if (!ListUtil.nullOrEmptyList(followers)) {
            for (Object item : followers) {
                if (item instanceof ItemProperties) {
                    String name = ((ItemProperties)item).name();
                    Rule rule = editSet.ruleForContextAlternate(dragContext, name);
                    editSet.updateRule(rule, Collections.singletonMap(UIMeta.KeyAfter, (Object)newPredecessor));
                    System.out.printf("Resetting predecessor of %s to %s\n", name, newPredecessor);
                }
            }
        }
    }

    public class EditSet
    {
        String _packageName;
        AWFileResource _resource;
        Meta.RuleSet _ruleSet;
        List <Rule> _editRules;
        boolean _dirty;

        public EditSet (AWFileResource resource)
        {
            _resource = resource;
            _packageName = AWUtil.pathToLastComponent(_resource.relativePath(), "/").replace("/", ".");
        }

        public String getPackageName ()
        {
            return _packageName;
        }

        Meta.RuleSet ruleSet ()
        {
            if (_ruleSet != _meta.loadedRuleSets().get(_resource)) {
                _ruleSet = _meta.loadedRuleSets().get(_resource);
                if (_editRules != null) {
                    for (Rule r : _editRules) {
                        r.disable();
                    }
                }
                _editRules = null;
            }
            return _ruleSet;
        }

        public List<Rule> getEditRules ()
        {
            ruleSet();
            if (_editRules == null) {
                _editRules = _ruleSet.rules(true);

            }
            return _editRules;
        }

        protected List<Rule.Predicate> predicateForContext (Context.Info contextInfo,
                                                            Collection<String> useKeys)
        {
            // Sort keys: propertyContext key last, EditorContextKeys first (in order)
            final String propContextKey = contextInfo.scopeKey;
            List<String> sortedKeys = new ArrayList(useKeys);
            Collections.sort(sortedKeys, new Comparator<String>() {
                int val (String s) {
                    if (s.equals(propContextKey)) return 10000;
                    int i = EditorContextKeys.indexOf(s);
                    if (i != -1) return i - 10000;
                    return 0;
                }

                public int compare (String s1, String s2)
                {
                    int v1 = val(s1), v2 = val(s2);
                    return  (v1 == v2) ? s1.compareTo(s2) : (v1 - v2);
                }
            });

            List<Rule.Predicate> preds = ListUtil.list();
            for (String key : sortedKeys) {
                Object val = contextInfo.contextMap.get(key);
                if (val != null) {
                    preds.add(new Rule.Predicate(key, val));
                }
            }
            return preds;
        }

        protected List<Rule.Predicate> predicateForContext (Context.Info contextInfo)
        {
            List<String> useKeys = ("field".equals(contextInfo.scopeKey))
                    ? FieldDefaultContextKeys : EditorContextKeys;
            return predicateForContext(contextInfo, useKeys);
        }

        public Rule addRule (List<Rule.Predicate> predicates, Map<String, Object> properties)
        {
            _dirty = true;
            _meta._setCurrentRuleSet(ruleSet());
            Rule prevLast = ListUtil.lastElement(ruleSet().rules(false));
            int rank = Math.max(((prevLast != null) ? prevLast.getRank() + 1 : 0), Meta.EditorRulePriority);
            Rule rule = new Rule(predicates, properties, rank);
            UIMeta.getInstance().addRule(rule);
            getEditRules().add(rule);
            _meta._setCurrentRuleSet(null);
            return rule;
        }

        public Rule addRule (Context.Info contextInfo)
        {
            return addRule(predicateForContext(contextInfo), new HashMap());
        }

        boolean ruleMatchesContext (Rule rule, Context.Info contextInfo)
        {
            boolean match = false;
            Object notFoundClassVal = contextInfo.contextMap.get(ObjectMeta.KeyClass);
            Object scopeVal = contextInfo.contextMap.get(contextInfo.scopeKey);
            // Inadequate test: will match a field with the same name on *another class*!
            for (Rule.Predicate p : rule.getPredicates()) {
                String key = p.getKey();
                Object val = p.getValue();
                if (key.equals(contextInfo.scopeKey) && val.equals(scopeVal)) {
                    match = true;
                }
                else if (!Meta.isPropertyScopeKey(key) && !val.equals(contextInfo.contextMap.get(key))) {
                    // if predicate matches on key not in the context, we can't use it
                    return false;
                }

                if (notFoundClassVal != null && key.equals(ObjectMeta.KeyClass) && val.equals(notFoundClassVal)) {
                    notFoundClassVal = null;
                }
            }
            return match && (notFoundClassVal == null);
        }


        public Rule existingEditableRuleMatchingContext (Context.Info contextInfo)
        {
            List<Rule> rules = getEditRules();
            for (int i = rules.size()-1; i >=0; i--) {
                Rule rule = rules.get(i);
                if (ruleMatchesContext(rule, contextInfo)) return rule;
            }
            return null;
        }

        public Rule editableRuleForContext (Context.Info contextInfo)
        {
            Rule rule = existingEditableRuleMatchingContext(contextInfo);
            return (rule != null)
                    ? rule
                    : addRule(predicateForContext(contextInfo), new HashMap());
        }

        // Rule for context, but swapping the value of the context scope key
        // (e.g. changing field=foo to field=bar
        public Rule ruleForContextAlternate (Context.Info contextInfo, String alternateScopeVal)
        {
            Context.Info newContextInfo = new Context.Info(contextInfo, alternateScopeVal);
            return editableRuleForContext(newContextInfo);
        }

        public void updateRule (Rule rule, Map<String, Object> props)
        {
            _dirty = true;
            // FIXME!  Bogus to mutate map in place
            rule.getProperties().putAll(props);

            // Post pass to clear null
            for (Map.Entry e : props.entrySet()) {
                if (e.getValue() == null) rule.getProperties().remove(e.getKey());
            }
            
            _meta.invalidateRules();
        }

        public void updateRulePredicates (Context.Info contextInfo,
                                          Rule rule, Collection<String>keys)
        {
            _dirty = true;
            List<Rule.Predicate> preds = predicateForContext(contextInfo, keys);
            rule.setPredicates(preds);
            _meta.invalidateRules();
        }

        public void deleteRule (Rule rule)
        {
            _dirty = true;
            rule.disable();
            getEditRules().remove(rule);
            _meta.invalidateRules();
        }

        public boolean isDirty ()
        {
            return _dirty;
        }

        public void save ()
        {
            if (!_dirty) return;

            try {
                File file = URLUtil.file(new URL(_resource.fullUrl()), true);
                OSSWriter.updateEditorRules(file, getEditRules());
            } catch (MalformedURLException e) {
                throw new AWGenericException(e);
            }

            // disable our edit rules -- they're covered by what's in the file now
            for (Rule rule : getEditRules()) {
                rule.disable();
            }
            _meta.reloadRuleFile(_resource);

            _dirty = false;
        }
    }

    void prepareContext (Context context, Context.Info contextInfo, String stopBefore)
    {
        List<String> keys = contextInfo.contextKeys;
        int last = keys.size();
        if (stopBefore != null) {
            while (last-- > 0) {
                if (keys.get(last).equals(stopBefore)) {
                    if (last > 0 && keys.get(last-1).equals(stopBefore)) last--; 
                    break;
                }
            }
        }
        for (int i=0; i < last; i++) {
            String key = keys.get(i);
            // Don't want to put down boolean for object
            if (key.equals(UIMeta.KeyObject)) continue;

            context.set(key, contextInfo.contextMap.get(key));
        }

        if (stopBefore == null && contextInfo.scopeKey != null) {
            context.setContextKey(contextInfo.scopeKey);
        }
    }

    static List<String>PropertyContextMirrorKeys = Arrays.asList(UIMeta.KeyElementType,
            UIMeta.KeyType, UIMeta.KeyEditing, UIMeta.KeyEditable);

    class EditorProperties
    {
        Context.Info _contextInfo;
        Context _originalContext;
        Context _propertyContext;
        Context _parentContext;

        public EditorProperties (Context.Info contextInfo)
        {
            _contextInfo = contextInfo;
            _originalContext = _meta.newContext();
            _propertyContext = _meta.newContext();
            _parentContext = _meta.newContext();
            prepareContext(_originalContext, contextInfo, null);
            preparePropertyContext(_propertyContext, contextInfo);
            prepareContext(_parentContext, contextInfo, contextInfo.scopeKey);
        }

        void preparePropertyContext (Context context, Context.Info contextInfo)
        {
            context.set(UIMeta.KeyClass, "ariba.ui.meta.editor.Properties");
            context.set("scopeKey", contextInfo.scopeKey);

            for (String key: PropertyContextMirrorKeys) {
                Object value = contextInfo.contextMap.get(key);
                if (value != null) context.set(key, value);
            }
        }

        Object editableOverrideForProperty (String key, Object origVal)
        {
            if (origVal == null) return origVal;

            if (_meta.propertyWillDoMerge(key, origVal)) {
                if (origVal instanceof Map) return MapUtil.map();
                if (origVal instanceof List) return null;
                if (origVal instanceof PropertyValue.Expr) return new PropertyValue.Expr("true");
            }
            return (origVal instanceof PropertyValue.StaticallyResolvable)
                    ? _originalContext.propertyForKey(key)
                    : origVal;
        }

        List<String> compatibleTraits ()
        {
            _originalContext.push();
            _originalContext.set(UIMeta.KeyTrait, UIMeta.KeyAny);
            List<String> names = _meta.itemNames(_originalContext, UIMeta.KeyTrait);
            _originalContext.pop();
            return names;
        }

        List<String> sort (List<String> list)
        {
            ListUtil.sortStrings(list, true);
            return list;
        }

        List<String> compatibleProperties ()
        {
            return sort(_meta.itemNames(_propertyContext, UIMeta.KeyField));
        }

        List<String> peerItems ()
        {
            return _contextInfo.scopeKey != null
                    ? sort(_meta.itemNames(_parentContext, _contextInfo.scopeKey))
                    : null;
        }

        Context.AssignmentRecord recordForPeerItem (String item)
        {
            _parentContext.push();
            _parentContext.set(_contextInfo.scopeKey, item);
            Context.AssignmentRecord rec = (Context.AssignmentRecord)_parentContext.debugTracePropertyProvider();
            _parentContext.pop();
            return rec;
        }
    }
}
