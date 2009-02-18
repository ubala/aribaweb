package gallery.table

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.table.AWTCSVDataSource
import ariba.ui.table.AWTDisplayGroup
import ariba.ui.widgets.Confirmation

class DynamicPivotTable extends AWComponent
{
    AWTDisplayGroup displayGroup
    boolean layoutChangeLatch
    def iter, curCol
    
    def layouts = [
        [name:"Non-Pivot"],
        [name:"By Supplier", c:["Supplier"], r:["Item", "Year", "Quarter", "Region"], a:["Price", "Quantity"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"By Date", c:["Year", "Quarter"], r:["Item", "Region", "Supplier"], a:["Price", "Quantity"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"By Date and Region", c:["Year", "Quarter", "Region"], r:["Item", "Supplier"], a:["Price", "Quantity"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"Qtr then Year", c:["Quarter", "Year"], r:["Item", "Region", "Supplier"], a:["Price", "Quantity", "ExtendedPrice"], da:["Color", "Size"]],
        [name:"By Region", c:["Region"], r:["Item", "Year", "Quarter", "Supplier"], a:["Price", "Quantity", "ExtendedPrice"], da:["Color", "Size"]],
        [name:"No CF, Two Attrs", c:[], r:["Item", "Region", "Year", "Quarter", "Supplier"], a:["Price", "Quantity"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"No CF, No Attrs", c:[], r:["Item", "Region", "Year", "Quarter", "Supplier"], a:[], da:["Price", "Quantity", "ExtendedPrice", "Color", "Size"]],
        [name:"One CF, One Attr", c:["Supplier"], r:["Item", "Region", "Year", "Quarter"], a:["Price"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"Two CF, One Attr", c:["Year", "Quarter"], r:["Item", "Region", "Supplier"], a:["Price"], da:["ExtendedPrice", "Color", "Size"]],
        [name:"One CF, No Attrs", c:["Supplier"], r:["Item", "Region", "Year", "Quarter"], a:[], da:["ExtendedPrice", "Color", "Size"]],
        [name:"Two CF, No Attrs", c:["Year", "Quarter"], r:["Item", "Region", "Supplier"], a:[], da:["ExtendedPrice", "Color", "Size"]]
    ];
    def layout = layouts[2];
    def allObjects, list;
    def showSelection = true;

    public boolean isStateless() { false }

    void init () {
        list = allObjects = AWTCSVDataSource.dataSourceForPath("BidData.csv", this).fetchObjects();
    }

    def showFilterPanel () {
        def panel = pageWithName("FilterPanel");
        panel.init(allObjects, ["Supplier", "Region", "Year"], { list = it; return pageComponent() });
        panel.setClientPanel(true);
        return panel;
    }

    def optionsChanged () {
        layoutChangeLatch = true;
        return null;
    }

    def showFlag () {
        return displayGroup.currentItem().ExtendedPrice > 20000;
    }

    def answerColumnName () {
        return (displayGroup.currentItem().Answer) ? "Answer" : null;
    }

    def isQuestion () {
        return (displayGroup.currentItem().Answer != null);
    }

    def showDetails = false;
    def rowAttributes () {
        return (showDetails) ? layout["da"] : null;
    }

    def panelId = null;
    def inspectedObject = null;

    def inspect () {
        inspectedObject = displayGroup.currentItem();
        return Confirmation.showConfirmation(requestContext(), panelId);
    }
}
