package gallery.table

import ariba.ui.aribaweb.core.AWComponent

import ariba.ui.aribaweb.core.*;
import ariba.ui.widgets.*;
import ariba.ui.table.*;
import ariba.ui.outline.*;
import ariba.util.core.*;
import ariba.util.fieldvalue.*

class OutlineTable extends AWComponent
{
    AWTDisplayGroup displayGroup;
    def users;
    def item, markedItem;

    public boolean isStateless() { false }

    void init () {
        // prevent the sort!
        URL url = ariba.ui.table.ResourceLocator.urlForRelativePath("Users.xml", this);
        users = ListUtil.arrayToList(FieldValue.getFieldValue(XMLUtil.document(url, false, false, null).documentElement, "children"));

        displayGroup = new AWTDisplayGroup();
        displayGroup.setSortOrderings(ListUtil.list());
        displayGroup.setObjectArray(users);
    }

    def markItem ()
    {
        markedItem = ListUtil.cloneList(displayGroup.outlineState().currentPath());
        return null;
    }

    def makeVisible ()
    {
        displayGroup.setPathToForceVisible(markedItem);
        displayGroup.setSelectedObject(ListUtil.lastElement(markedItem));
        return null;
    }

    def swap () {
        def arr = displayGroup.allObjects();
        def first = arr.get(0);
        arr.remove(0);
        arr.add(1, first);
        displayGroup.setObjectArray(arr);
        return null;
    }

    def isJimSpandler ()
    {
        return FieldValue.getFieldValue(displayGroup.currentItem(), "name.text") == "Jim Spandler";
    }

    def expansionClicked ()
    {
    /*
        def v = displayGroup.outlineState().expansionPath().clone();
        // Log.demoshell.debug("path: %s", v);
        if (v && (v.count() > 0) && get(v.lastElement(), "name.text").equals("Jim Spandler")) {
            v.removeLastElement();
            displayGroup.outlineState().collapseAll();
            displayGroup.outlineState().setExpansionPath(v);
            // Log.demoshell.debug("new path: %s", v);
        }
    */
        return null;
    }
}
