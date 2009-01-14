package masterdetail

import ariba.ui.aribaweb.core.*;
import ariba.ui.widgets.*;
import ariba.ui.table.*;
import ariba.ui.outline.*;
import ariba.util.core.*;

class ERPage extends AWComponent
{
    def tabIndex=0;
    AWTDisplayGroup displayGroup;
    def selectedItem;
    def currentItem = null;
    def expenseTypes = ["Meal", "Hotel", "Airfare", "Car", "Entertainment"];
    def expenseType;
    def lastSelectedObject;

    public boolean isStateless() { false }

    def viewerForSelectedItem () {
        def type = displayGroup.selectedObject().Type;
        if (type == "Meal" ) return "DetailViewer";
        if (type == "Hotel" ) return "DetailViewer";
        return "SimpleViewer";
    }

    def save() {
        if (!errorManager().checkErrorsAndEnableDisplay())
            displayGroup.setSelectedObject(null);
        return null;
    }

    void addItem (item, int pos) {
        List list = displayGroup.allObjects();
        if (pos == -1) pos = list.size()
        list.add(pos, item);
        displayGroup.setObjectArray(list);
        displayGroup.setSelectedObject(item);
    }

    void addItem (String type) {
        // Clone selected item, add it to list, and select it
        def newItem = [:];  // new record
        newItem.Type = type;
        newItem.Year = new ariba.util.core.Date();
        addItem(newItem, -1)
    }

    def deleteSelected () {
        displayGroup.setObjectArray(ListUtil.minus(displayGroup.allObjects(), displayGroup.selectedObjects()));
        return null;
    }

    def draggedCharge
    AWTDisplayGroup chargeDisplayGroup

    void addCharges () {
        chargeDisplayGroup.selectedObjects().each {
            addItem(it, -1)
        }
        chargeDisplayGroup.setObjectArray(ListUtil.minus(chargeDisplayGroup.allObjects(),
                chargeDisplayGroup.selectedObjects()));
    }
    
    void chargeDragged () {
        // remember dragged charge
        draggedCharge = chargeDisplayGroup.currentItem()
    }

    void chargeDropped () {
        addItem(draggedCharge, displayGroup.filteredObjects().indexOf(displayGroup.currentItem()))
        chargeDisplayGroup.setObjectArray(ListUtil.minus(chargeDisplayGroup.allObjects(), [draggedCharge]));
    }

    def rememberLastSelected () { lastSelectedObject = displayGroup.selectedObject(); }

    def checkSelection () {
        if (displayGroup.selectedObject() != lastSelectedObject) {
            tabIndex = 0;
        }
    }

    def bigPanel () {
        def p = pageWithName("TETestPanel");
        p.setClientPanel(true);
        return p;
    }

    def longRunningAction () {
        java.lang.Thread.sleep(4000); // pretend that it took us 4 sec before initing status string
        def m = ProgressMonitor.instance();
        m.prepare("<b>Step 1 of 2: Process items</b><br/>(Processed %s of %s)", displayGroup.allObjects().size());
        fakeDoProcessItems();

        m.prepare("<b>Step 2 of 2: Optimizing items</b><br/>(Processed %s of %s)", displayGroup.allObjects().size());
        fakeDoProcessItems();

        return null;
    }

    def secureAction () {
        session().assertAuthenticated();
        System.out.println("**** EXECUTED SECURE ACTION ****");
        AribaPageContent.setMessage("Secure action executed", session());
    }

    // Pretend that this is app code that doesn't know about the UI, but is doing some extensive processing
    // (like inserting items from a large file, or loading a large Sourcing event)
    def fakeDoProcessItems () {
        for (i in 1..displayGroup.allObjects().size()) {
            java.lang.Thread.sleep(1000); // pretend to work for one second
            ProgressMonitor.instance().incrementCurrentCount();
        }
    }
}
