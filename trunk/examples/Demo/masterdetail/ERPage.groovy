package masterdetail

import ariba.ui.aribaweb.core.*;
import ariba.ui.widgets.*;
import ariba.ui.table.*;
import ariba.ui.outline.*;
import ariba.util.core.*;

class ERPage extends AWComponent
{
    def tabIndex=0;
    def displayGroup;
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

    def addItem (String type)
    {
        // Clone selected item, add it to list, and select it
        def newItem = [:];  // new record
        newItem.Type = type;
        newItem.Year = new ariba.util.core.Date();
        def list = displayGroup.allObjects();
        list.add(newItem);
        displayGroup.setObjectArray(list);
        displayGroup.setSelectedObject(newItem);

        return null;  // same page
    }

    def deleteSelected () {
        displayGroup.setObjectArray(ListUtil.minus(displayGroup.allObjects(), displayGroup.selectedObjects()));
        return null;
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
        m.prepare("<h3>Step 1 of 2: Process items</h3>(Processed %s of %s)", displayGroup.allObjects().size());
        fakeDoProcessItems();

        m.prepare("<h3>Step 2 of 2: Optimizing items</h3>(Processed %s of %s)", displayGroup.allObjects().size());
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

    def chooserState;
    def selectionSource = new MatchSource();
    def selections;
    def selectAction () { return null; }
}

class MatchSource implements ChooserSelectionSource
{
    def NamesList = [
        "ytang_supplier1",
        "gforget_supplier1",
        "Sgorantla_supplier1",
        "Mwhitmore_supplier1",
        "Mtessel_supplier1",
        "Chak_supplier1",
        "Mdao_supplier1",
    ];

    public List match (String pattern, int max) {
        List matches = match(NamesList, pattern);
        return (matches.size() > max) ? matches.subList(0, max) : matches;
    }

    public List match(List selections, String pattern) {
        if (pattern == null)  return selections;
        return selections.findAll { name -> name.startsWith(pattern); }
    }
}
