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

    $Id: //ariba/platform/ui/metaui/ariba/ui/meta/editor/MetaInspectorPane.java#15 $
*/
package ariba.ui.meta.editor;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWDebugTrace;
import ariba.ui.aribaweb.util.AWEncodedString;
import ariba.ui.aribaweb.util.AWDebugTrace.ComponentTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.MetadataTraceNode;
import ariba.ui.aribaweb.util.AWDebugTrace.Assignment;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.Meta;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.meta.core.Rule;
import ariba.ui.meta.core.ObjectMeta;
import ariba.ui.widgets.AribaPageContent;
import ariba.ui.widgets.Confirmation;
import ariba.util.core.MapUtil;
import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;

public class MetaInspectorPane extends AWComponent
{
    public boolean _compactMode;
    public ComponentTraceNode _traceNode;
    public MetadataTraceNode _metadataNode;
    AWDebugTrace _debugTrace;
    boolean _didInvalidate;
    Context.AssignmentRecord _srec;
    public Context.InspectorInfo _contextInfo;
    public Map<Rule.AssignmentSource, List<Assignment>> _assignmentMap;
    public List<Rule.AssignmentSource> _assignmentLocations;
    public List<Assignment> _activeAssignments;

    EditManager _editManager;
    public EditManager.EditSet _editSet;
    public EditManager.EditorProperties _editorProperties;
    public List<String> _compatibleTraits;
    public List<String> _compatibleProperties;
    public List<String> _peerItems;

    public String currentView, selectedView;
    public String _refreshMainWindowActionId = null;

    public Rule.AssignmentSource _assignmentSource;
    public Assignment _assignment;
    public Object _menuId;
    public String _contextKey;
    public String _trait;

    Rule _editingRule;
    String _editingKey;
    public String _editableAssignmentString;
    public String _scopePropertyKey;

    public String _propertyKey;
    public String _itemName;
    public Context.AssignmentInfo _assignmentInfo;

    public boolean isStateless ()
    {
        return false;
    }

    public void init ()
    {
        _compactMode = booleanValueForBinding("compactMode");
        selectedView = "Properties";
    }

    public boolean editing ()
    {
        return _editManager != null && _editManager.editing();
    }

    public void renderResponse (AWRequestContext requestContext, AWComponent component)
    {
        UIMeta meta = UIMeta.getInstance();

        _editManager = EditManager.currentEditManager(meta, session(), true);
        Context.AssignmentRecord prevRec = _srec;
        _srec = _editManager.getSelectedRecord();

        ComponentTraceNode prevTraceNode = _traceNode;
        _traceNode = (ComponentTraceNode)valueForBinding("traceNode");
        if (_traceNode != null && (_srec == null || _traceNode != prevTraceNode)) {
            _srec = (Context.AssignmentRecord)_traceNode.associatedMetadataProvider();
            _editManager.setSelectedRecord(_srec);
        }

        if (prevRec != _srec || _didInvalidate) {
            _didInvalidate = false;
            _metadataNode = (_traceNode != null) ? _traceNode.associatedMetadata() : null;
            _contextInfo = Context.staticContext(meta, _srec, true);
            if (prevRec != _srec) {
                // EditManager.clearEditManager(meta, session());
                _editSet = _editManager.editSetForContext(_contextInfo);
            }

            // get assignments maps and list of active assignments
            _assignmentMap = Context.assignmentMap(_srec);
            _assignmentLocations = new ArrayList(_assignmentMap.keySet());
            _activeAssignments = ListUtil.list();
            for (List<Assignment>assignments : _assignmentMap.values()) {
                for (Assignment assignment : assignments) {
                    String key = assignment.getKey();
                    if (!assignment.isOverridden() && !key.endsWith("_trait")
                            && !key.equals("bindingsDictionary")
                            && !key.equals(_contextInfo.scopeKey)) {
                        _activeAssignments.add(assignment);
                    }
                }
            }
            
            _editorProperties = _editManager.editorPropertiesForContext(_contextInfo);
            _compatibleTraits = _editorProperties.compatibleTraits();
            _compatibleProperties = _editorProperties.compatibleProperties();
            _peerItems = _editorProperties.peerItems();
        }

        super.renderResponse(requestContext, component);
    }

    public String scopePropertyKey ()
    {
        return _contextInfo.scopeKey;
    }

    public String scopePropertyValue ()
    {
        return (_contextInfo.scopeKey != null)
                ? (String)_contextInfo.getSingleValue(_contextInfo.scopeKey)
                : null;
    }

    public Object contextValue ()
    {
        return _contextInfo.contextMap.get(_contextKey);
    }

    public boolean selectorUsesContextKey ()
    {
        for (Rule.Selector p : currentRule().getSelectors()) {
            if (p.getKey().equals(_contextKey)) return true;
        }
        return false;
    }

    public void toggleSelector ()
    {
        // add/remove _contextKey : _contextValue
        Set<String> predKeys = new HashSet();
        Rule rule = currentRule();
        for (Rule.Selector p : rule.getSelectors()) {
            predKeys.add(p.getKey());
        }

        if (selectorUsesContextKey()) {
            predKeys.remove(_contextKey);
        } else {
            predKeys.add(_contextKey);
        }

        EditManager.EditSet editSet = _editManager.editSetForRule(rule);
        editSet.updateRuleSelectors(_contextInfo, rule, predKeys);
        invalidateForEdit();
    }

    public Object activeTraits ()
    {
        return _contextInfo.properties.get(Meta.KeyTrait);
    }

    public boolean hasTrait ()
    {
        Object t = activeTraits();
        return (t instanceof List)
                ? ((List)t).contains(_trait)
                : _trait.equals(t);
    }

    public void toggleTrait ()
    {
        boolean adding = !hasTrait();
        EditManager.EditSet editSet = _editManager.editSetForContext(_contextInfo);
        Rule rule = editSet.editableRuleForContext(_contextInfo);
        Map newProp = _editManager.updatedListProperty(rule.getProperties(), Meta.KeyTrait,
                            _trait, _editManager.newListForVal(activeTraits()),
                            adding, true);

        editSet.updateRule(rule, newProp);
        invalidateForEdit();
    }

    public boolean itemVisible ()
    {
        Object scopeValue = _contextInfo.contextMap.get(_contextInfo.scopeKey);
        return scopeValue.equals(_itemName);
    }

    public void selectItem ()
    {
        _editManager.setSelectedRecord(_editorProperties.recordForPeerItem(_itemName));
    }

    public void selectAssignment ()
    {
        _editManager.setSelectedParentRecord(_assignmentInfo.rec);
    }

    public boolean canShowAssignments ()
    {
        return (_assignmentMap != null && _assignmentMap.size() > 1);
    }

    public String ruleIndicatorStyle ()
    {
        String color = (currentAssignmentRule().isEditable()) ? colorForRule(currentAssignmentRule()) : "#DDDDDD";
        return Fmt.S("height:6px;width:6px;border:1px solid gray;background-color:%s;",
                color);
    }

    void invalidateForEdit ()
    {
        _didInvalidate = true;
        UIMeta meta = UIMeta.getInstance();
        meta.invalidateRules();
        if (_metadataNode != null) _metadataNode.invalidate();
        _refreshMainWindowActionId = "\"refresh\""; // bogus actionId, causes refresh
    }

    void editProperty (Rule rule, String key, Object value)
    {
        _editingRule = rule;
        _editingKey = key;
        if (value == null) value = rule.getProperties().get(key);
        String valueString = OSSWriter.toOSS(value);
        _editableAssignmentString = (isPropertiesPanel() ? valueString : (_editingKey + " : " + valueString));
    }

    void addProperty (Rule rule, String key, Object value)
    {
        EditManager.EditSet editSet = _editManager.editSetForRule(rule);
        editSet.updateRule(rule, Collections.singletonMap(key, value));
        editProperty(rule, key, null);
        invalidateForEdit();
    }

    public boolean isPropertiesPanel ()
    {
        return selectedView.equals("Properties");
    }

    /*
        Actions
     */
    public void addProperty ()
    {
        if (_propertyKey == null && isPropertiesPanel()) {
            Confirmation.showConfirmation(requestContext(), AWEncodedString.sharedEncodedString("AddPropertyPanel"));
            return;
        }

        Rule rule = currentRule();
        if (rule == null) rule = _editSet.editableRuleForContext(_contextInfo);
        String key = "prop";
        if (_propertyKey != null) {
            key = _propertyKey;
        }
        Object value = rule.getProperties().get(key);
        if (value != null) {
            editProperty(rule, key, value);
        }
        else {
            value = _contextInfo.properties.get(key);
            if (value == null) value = "value";
            addProperty(rule, key, value);
        }
    }

    public void deleteRule ()
    {
        Rule rule = currentRule();
        _editManager.editSetForRule(rule).deleteRule(rule);
        invalidateForEdit();
    }

    public void deleteCurrentProperty ()
    {
        // FIXME!  Smashing rules directly is BAD!
        Rule rule = currentRule();
        Object val = null;

        if (!rule.isEditable()) {
            rule = _editSet.editableRuleForContext(_contextInfo);
            val = new Meta.OverrideValue(null);
        }
        _editSet.updateRule(rule, Collections.singletonMap(_assignment.getKey(), val));
        invalidateForEdit();
    }

    public void addRule ()
    {
        EditManager.EditSet editSet = _editManager.editSetForContext(_contextInfo);
        Rule rule = editSet.addRule(_contextInfo);
        addProperty(rule, "prop", "value");
    }

    public void editCurrentAssignment ()
    {
        Rule rule = currentAssignmentRule();
        if (!rule.isEditable()) {
            Object origVal = rule.getProperties().get(_assignment.getKey());
            EditManager.EditSet editSet = _editManager.editSetForContext(_contextInfo);
            rule = editSet.editableRuleForContext(_contextInfo);
            Object editableVal = _editorProperties.editableOverrideForProperty(_assignment.getKey(), origVal);
            addProperty(rule, _assignment.getKey(), editableVal);
        } else {
            editProperty(rule, _assignment.getKey(), null);
        }
    }

    public Rule currentAssignmentRule ()
    {
        return (_assignment != null) ? ((Rule.AssignmentSource)_assignment.getSource()).getRule() : null;
    }

    public Rule currentRule ()
    {
        return (_assignmentSource != null)
                ? _assignmentSource.getRule()
                : currentAssignmentRule();
    }

    public boolean editingCurrentAssignment ()
    {
        return (_editingRule == currentAssignmentRule())
                && _assignment.getKey().equals(_editingKey);
    }

    public void doneEditing ()
    {
        if (_editingRule == null) return;
        UIMeta meta = UIMeta.getInstance();
        Map newProp = MapUtil.map();
        String newAssignmentString = (isPropertiesPanel())
                ? _editingKey + ": " + OSSWriter.escapeString(_editableAssignmentString)
                : _editableAssignmentString;
        String errorMessage = meta.parsePropertyAssignment(newAssignmentString, newProp);
        if (errorMessage != null) {
            recordValidationError("property", errorMessage, _editableAssignmentString);
        }
        if (newProp.isEmpty()) {
            recordValidationError("property", "Unable to parse property assignment", _editableAssignmentString);
        } else {
            // clear old key
            Rule rule = _editingRule;
            if (!newProp.containsKey(_editingKey)) newProp.put(_editingKey, null);
            EditManager.EditSet editSet = _editManager.editSetForRule(rule);
            editSet.updateRule(rule, newProp);
            _editingRule = null;
            invalidateForEdit();
        }
        errorManager().checkErrorsAndEnableDisplay();
    }

    public void saveChanges ()
    {
        doneEditing();
        if (errorManager().hasErrors()) return;
        
        _editManager.saveChanges();
        AribaPageContent.setMessage(Fmt.S("Changes saved to %s", _editSet._resource.relativePath()),
                session());

        invalidateForEdit();
        // BOGUS
        // _editManager = new EditManager(UIMeta.getInstance());
        // _editSet = _editManager.editSetForContext(_contextInfo);
        // EditManager.clearEditManager(_editManager._meta, session());
        
        // Force computation of component inspector trace
        _refreshMainWindowActionId = "_AWXRefreshMainActionId";
    }

    /*
        Deal out unique colors for different rule selectors
     */
    protected static String[] RuleColors = new String [] {
        "#9999FF", "#CCFF66", "#666699", "#FFCC66",
        "#99AD50", "#FFFF99", "#CCCCFF", "#6600FF",
        "#F280FF", "#0099FF", "#CCCCCC"
    };
    static Map<List<String>, Integer> _ColorIndexForKeyList = MapUtil.map();
    static int _NextColorIndex = 0;

    static int colorIndexForKeyList (List<String> keys)
    {
        Integer index = _ColorIndexForKeyList.get(keys);
        if (index != null) return index;

        keys = new ArrayList(keys);
        ListUtil.sortStrings(keys, true);
        index = _ColorIndexForKeyList.get(keys);
        if (index != null) return index;

        index = _NextColorIndex++;
        _ColorIndexForKeyList.put(keys, index);
        return index;
    }

    static String colorForKeyList (List<String> keys)
    {
        return RuleColors[colorIndexForKeyList(keys) % RuleColors.length];
    }

    static String colorForRule (Rule rule)
    {
        List<String> keys = new ArrayList(4);
        for (Rule.Selector pred : rule.getSelectors()) {
            if (!Meta.isPropertyScopeKey(pred.getKey())) keys.add(pred.getKey());
        }
        return colorForKeyList(keys);
    }

    // Initialize with a few common ones for stability across server launches
    static {
        colorIndexForKeyList(Arrays.asList(ObjectMeta.KeyClass));
        colorIndexForKeyList(Arrays.asList(ObjectMeta.KeyClass, ObjectMeta.KeyField));
        colorIndexForKeyList(Arrays.asList(UIMeta.KeyEditable, ObjectMeta.KeyClass, ObjectMeta.KeyField));
        colorIndexForKeyList(Arrays.asList(UIMeta.KeyLayout, ObjectMeta.KeyClass, ObjectMeta.KeyField));
    }
}