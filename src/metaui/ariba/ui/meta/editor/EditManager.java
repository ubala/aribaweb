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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/editor/EditManager.java#21 $
*/
package ariba.ui.meta.editor;

import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.Rule;
import ariba.ui.meta.core.ItemProperties;
import ariba.ui.meta.core.Log;
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
    Context.InspectorInfo _selectedInfo;

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

    public void setSelectedParentRecord (Context.AssignmentRecord selectedRecord)
    {
        Context context = _meta.newContext();
        prepareContext(context, Context.staticContext(_meta, selectedRecord, true), null, true);
        setSelectedRecord((Context.AssignmentRecord)context.debugTracePropertyProvider());
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

    EditSet editSetForContext (Context.InspectorInfo contextInfo)
    {
        String packageName = packageNameForContext(contextInfo);
        if (packageName == null) return null;

        for (EditSet e : editableRuleSets()) {
            if (e._packageName.equals(packageName)) return e;
        }
        return null;
    }

    EditorProperties editorPropertiesForContext (Context.InspectorInfo contextInfo)
    {
        return new EditorProperties(contextInfo);
    }

    public EditSet editSetForRule (Rule r)
    {
        for (EditSet e : editableRuleSets()) {
            if (e.ruleSet() == r.getRuleSet()) return e;
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

    static String packageNameForContext (Context.InspectorInfo contextInfo)
    {
        String className = (String)contextInfo.getSingleValue(UIMeta.KeyClass);
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
        Context.InspectorInfo dragContext = Context.staticContext(_meta, (Context.AssignmentRecord)dragRec, true);
        Context.InspectorInfo dropContext = Context.staticContext(_meta, (Context.AssignmentRecord)dropRec, true);
        String draggedItem = (String)dragContext.contextMap.get(dragContext.scopeKey);
        String dropItem = (String)dropContext.contextMap.get(dropContext.scopeKey);
        EditSet editSet = editSetForContext(dragContext);

        // If drag onto ones-self, no-op
        if (editSet == null || draggedItem.equals(dropItem)) return;

        // Need to find all fields with `draggedItem` as predecessor and give them
        // its predecessor
        Context context = _meta.newContext();
        prepareContext(context, dragContext, dragContext.scopeKey, false);
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
        Log.meta.debug("Resetting predecessor of %s to %s\n", draggedItem, dropItem);

        if (_selectedRecord != null) {
            setSelectedRecord((Context.AssignmentRecord)dragRec);
        }
    }

    private void reparentFollowers (Context.InspectorInfo dragContext, String predecessor, String newPredecessor,
                                    EditSet editSet, Map<String, List> preds)
    {
        List followers = preds.get(predecessor);
        if (!ListUtil.nullOrEmptyList(followers)) {
            for (Object item : followers) {
                if (item instanceof ItemProperties) {
                    String name = ((ItemProperties)item).name();
                    Rule rule = editSet.ruleForContextAlternate(dragContext, name);
                    editSet.updateRule(rule, Collections.singletonMap(UIMeta.KeyAfter, (Object)newPredecessor));
                    Log.meta.debug("Resetting predecessor of %s to %s\n", name, newPredecessor);
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
        Map <Rule, List<Rule>> _extrasForRule = MapUtil.map();
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
                _extrasForRule.clear();
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

        protected List<Rule.Selector> selectorForContext (Context.InspectorInfo contextInfo,
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

            List<Rule.Selector> preds = ListUtil.list();
            for (String key : sortedKeys) {
                Object val = contextInfo.contextMap.get(key);
                if (val != null) {
                    preds.add(new Rule.Selector(key, val));
                }
            }
            return preds;
        }

        protected List<Rule.Selector> selectorForContext (Context.InspectorInfo contextInfo)
        {
            List<String> useKeys = UIMeta.KeyModule.equals(contextInfo.scopeKey) ? AWUtil.list(UIMeta.KeyModule) 
                        : (("field".equals(contextInfo.scopeKey))
                             ? FieldDefaultContextKeys : EditorContextKeys);
            if (contextInfo.scopeKey != null && !useKeys.contains(contextInfo.scopeKey)) {
                useKeys = new ArrayList(useKeys);
                useKeys.add(contextInfo.scopeKey);
            }
            return selectorForContext(contextInfo, useKeys);
        }

        public Rule addRule (List<Rule.Selector> selectors, Map<String, Object> properties)
        {
            _dirty = true;
            _meta._resumeEditingRuleSet(ruleSet());
            Rule prevLast = ListUtil.lastElement(ruleSet().rules(false));
            int rank = Math.max(((prevLast != null) ? prevLast.getRank() + 1 : 0), Meta.EditorRulePriority);
            Rule rule = new Rule(selectors, properties, rank);
            List<Rule> extras = UIMeta.getInstance()._addRuleAndReturnExtras(rule);
            if (extras != null) _extrasForRule.put(rule, extras);
            getEditRules().add(rule);
            _meta.endRuleSet();
            return rule;
        }

        public Rule addRule (Context.InspectorInfo contextInfo)
        {
            return addRule(selectorForContext(contextInfo), new HashMap());
        }

        boolean ruleMatchesContext (Rule rule, Context.InspectorInfo contextInfo)
        {
            boolean match = false;
            Object notFoundClassVal = contextInfo.contextMap.get(ObjectMeta.KeyClass);
            Object scopeVal = contextInfo.contextMap.get(contextInfo.scopeKey);
            // Inadequate test: will match a field with the same name on *another class*!
            for (Rule.Selector p : rule.getSelectors()) {
                String key = p.getKey();
                Object val = p.getValue();
                if (key.equals(contextInfo.scopeKey) && val.equals(scopeVal)) {
                    match = true;
                }
                else if (!Meta.isPropertyScopeKey(key) && !val.equals(contextInfo.contextMap.get(key))) {
                    // if selector matches on key not in the context, we can't use it
                    return false;
                }

                if (notFoundClassVal != null && key.equals(ObjectMeta.KeyClass) && val.equals(notFoundClassVal)) {
                    notFoundClassVal = null;
                }
            }
            return match && (notFoundClassVal == null);
        }


        public Rule existingEditableRuleMatchingContext (Context.InspectorInfo contextInfo)
        {
            List<Rule> rules = getEditRules();
            for (int i = rules.size()-1; i >=0; i--) {
                Rule rule = rules.get(i);
                if (ruleMatchesContext(rule, contextInfo)) return rule;
            }
            return null;
        }

        public Rule editableRuleForContext (Context.InspectorInfo contextInfo)
        {
            Rule rule = existingEditableRuleMatchingContext(contextInfo);
            return (rule != null)
                    ? rule
                    : addRule(selectorForContext(contextInfo), new HashMap());
        }

        // Rule for context, but swapping the value of the context scope key
        // (e.g. changing field=foo to field=bar
        public Rule ruleForContextAlternate (Context.InspectorInfo contextInfo, String alternateScopeVal)
        {
            Context.InspectorInfo newContextInfo = new Context.InspectorInfo(contextInfo, alternateScopeVal);
            return editableRuleForContext(newContextInfo);
        }

        public void updateRule (Rule rule, Map<String, Object> props)
        {
            _dirty = true;

            // need to force grabbing our edit rule list before mutating
            getEditRules();
            
            // FIXME!  Bogus to mutate map in place
            rule.getProperties().putAll(props);

            // Post pass to clear null
            for (Map.Entry e : props.entrySet()) {
                if (e.getValue() == null) rule.getProperties().remove(e.getKey());
            }

            // Disable this slot (and it's extras) and add a new copy
            _meta._resumeEditingRuleSet(ruleSet());
            List<Rule>extras =  _extrasForRule.get(rule);
            _extrasForRule.remove(rule);
            extras = _meta._updateEditedRule(rule, extras);
            if (extras != null) _extrasForRule.put(rule, extras);
            _meta.endRuleSet();
        }

        public void updateRuleSelectors (Context.InspectorInfo contextInfo,
                                          Rule rule, Collection<String>keys)
        {
            _dirty = true;
            List<Rule.Selector> preds = selectorForContext(contextInfo, keys);
            rule.setSelectors(preds);
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

            List<Rule>editRules = getEditRules();
            try {
                File file = URLUtil.file(new URL(_resource.fullUrl()), true);
                OSSWriter.updateEditorRules(file, editRules);
            } catch (MalformedURLException e) {
                throw new AWGenericException(e);
            }

            // disable our edit rules -- they're covered by what's in the file now
            for (Rule rule : editRules) {
                rule.disable();
            }
            
            _meta.reloadRuleFile(_resource);

            _dirty = false;
        }
    }

    void prepareContext (Context context, Context.InspectorInfo contextInfo, String stopBefore, boolean omitChained)
    {
        List<Context.AssignmentInfo> recs = contextInfo.assignmentStack;
        int last = recs.size();
        if (stopBefore != null) {
            while (last-- > 0) {
                if (recs.get(last).key.equals(stopBefore)) {
                    if (last > 0 && recs.get(last-1).key.equals(stopBefore)) last--;
                    break;
                }
            }
        }
        for (int i=0; i < last; i++) {
            Context.AssignmentInfo rec = recs.get(i);
            String key = rec.key;
            // Don't want to put down boolean for object
            if (key.equals(UIMeta.KeyObject) || (omitChained && rec.fromChaining) || rec.overridden) continue;

            context.set(key, rec.value);
        }

        if (stopBefore == null && contextInfo.scopeKey != null) {
            context.setScopeKey(contextInfo.scopeKey);
            context.setScopeKey(contextInfo.scopeKey);
        }
    }

    static List<String>PropertyContextMirrorKeys = Arrays.asList(UIMeta.KeyElementType,
            UIMeta.KeyType, UIMeta.KeyEditing, UIMeta.KeyEditable);

    public class EditorProperties
    {
        Context.InspectorInfo _contextInfo;
        Context _originalContext;
        Context _propertyContext;
        Context _parentContext;
        public List <Context.AssignmentInfo> activeAssignments;

        public EditorProperties (Context.InspectorInfo contextInfo)
        {
            _contextInfo = contextInfo;
            _originalContext = _meta.newContext();
            _propertyContext = _meta.newContext();
            _parentContext = _meta.newContext();
            prepareContext(_originalContext, contextInfo, null, false);
            preparePropertyContext(_propertyContext, contextInfo);
            prepareContext(_parentContext, contextInfo, contextInfo.scopeKey, false);

            activeAssignments = ListUtil.list();
            for (Context.AssignmentInfo ai : _contextInfo.assignmentStack){
                if (!ai.overridden && !ai.key.endsWith("_trait")) activeAssignments.add(ai);
            }
        }

        void preparePropertyContext (Context context, Context.InspectorInfo contextInfo)
        {
            context.set(UIMeta.KeyClass, "ariba.ui.meta.editor.Properties");
            context.set("scope", contextInfo.scopeKey);

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
            Object val =  (origVal instanceof PropertyValue.StaticallyResolvable)
                    ? _originalContext.propertyForKey(key)
                    : origVal;
            return (val instanceof PropertyValue.Dynamic) ? null : val;
        }

        List<String> compatibleTraits ()
        {
            _originalContext.push();
            List<String> names = _meta.itemNames(_originalContext, Meta.KeyTrait);
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
