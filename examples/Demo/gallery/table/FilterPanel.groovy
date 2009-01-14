package gallery.table

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.table.*
import ariba.util.core.*

class FilterPanel extends AWComponent
{
    def iter;
    def selectedFilterKey;
    def selectedFilterValues = null;
    def filterKeys;
    def allObjects;
    def okClosure;
    def displayGroup  = new AWTDisplayGroup();
    def NullMarker = "(None)";

    public boolean isStateless() { false }

    void init (list, keys, callback) {
        allObjects = list;
        filterKeys = keys;
        okClosure = callback;
        selectedFilterKey = filterKeys[0];
        updateValueList();
    }

    def safeNull (v) { return (v) ? v : NullMarker; }

    def updateValueList () {
        def s = new HashSet();
        allObjects.each { def v = it[selectedFilterKey]; s.add(safeNull(v)); };
        displayGroup.setObjectArray(new ArrayList(s));
        displayGroup.setSelectedObjects(displayGroup.allObjects());
    }

    def setSelectedFilterValue (value) {
        selectedFilterValue = value;
        // _resultList =  value ? allObjects.findAll() { it[selectedFilterKey] == value } : allObjects;
    }

    def okAction () {
        def selected = new HashSet(displayGroup.selectedObjects());
        def result =  allObjects.findAll() { selected.contains(safeNull(it[selectedFilterKey])) };
        return okClosure.call(result);
    }
}
