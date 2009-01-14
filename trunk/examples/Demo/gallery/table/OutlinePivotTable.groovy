package gallery.table

import ariba.ui.aribaweb.core.AWComponent

import ariba.ui.aribaweb.core.*
import ariba.ui.widgets.*
import ariba.ui.table.*
import ariba.ui.outline.*
import ariba.util.core.*

class OutlinePivotTable extends AWComponent
{
    def displayGroup, layoutChangeLatch, iter;
    def showSelection = false, filteringColumns = false;
    def isItemExpanded = [:]

    def files=[
        [ name:"OutlineData-Simple.csv",
          layouts:[
            [name:"Non-Pivot"],
            [name:"By Supplier (P)", c:["Supplier"], r:["Item", "Region"], a:["Price"]],
            [name:"By Supplier (P, Del)", c:["Supplier"], r:["Item", "Region"], a:["Price", "Delete"]],
            [name:"By Supplier", c:["Supplier"], r:["Item", "Region"], a:["BigFlag", "Price", "Quantity"]],
            [name:"By Supplier and Region", c:["Supplier", "Region"], r:["Item"], a:["Price", "Quantity"]],
            [name:"By Region and Supplier", c:["Region", "Supplier"], r:["Item"], a:["Price", "Quantity"]],
            [name:"By Supplier (Region Attr)", c:["Supplier"], r:["Item"], a:["Region", "BigFlag", "Price", "Quantity"]],
          ]
        ],
        [ name:"OutlineData-Complex.csv",
          layouts:[
            [name:"Non-Pivot"],
            [name:"By Supplier (P)", c:["Supplier"], r:["Item", "Region", "Year", "Quarter"], a:["Price"]],
            [name:"By Supplier", c:["Supplier"], r:["Item", "Year", "Quarter", "Region"], a:["BigFlag", "Price", "Quantity"]],
            [name:"By Supplier and Region", c:["Supplier", "Region"], r:["Item", "Year", "Quarter"], a:["Price", "Quantity"]],
            [name:"By Region and Supplier", c:["Region", "Supplier"], r:["Item", "Year", "Quarter"], a:["Price", "Quantity"]],
            [name:"By Date", c:["Year", "Quarter"], r:["Item", "Supplier", "Region"], a:["Price", "Quantity"]],
            [name:"By Date and Region", c:["Year", "Quarter", "Region"], r:["Item", "Supplier"], a:["Price", "Quantity"]],
            [name:"Qtr then Year", c:["Quarter", "Year"], r:["Item", "Region", "Supplier"], a:["Price", "Quantity", "ExtendedPrice"]],
            [name:"Supplier, No Col Attrs", c:["Supplier"], r:["Item", "Region", "Year", "Quarter"], a:[]],
            [name:"No CF, Two Attrs", c:[], r:["Item", "Region", "Year", "Quarter", "Supplier"], a:["Price", "Quantity"]],
            [name:"No CF, No Attrs", c:[], r:["Item", "Region", "Year", "Quarter", "Supplier"], a:[]],
          ]
       ]
    ];

    def file = files[0];
    def layouts () { return file.layouts; }
    def layout=layouts()[1];

    def rootList, fullList;

    public boolean isStateless() { false }

    void init () {
        updateFile();
    }

    void delete () {
        fullList.remove(displayGroup.currentItem())
        rootList = AWTCSVDataSource.computeOutlineList (fullList, "children", "Level");
        optionsChanged();
    }

    def updateFile () {
        // read CSV and then convert to nested tree by interpreting Level field
        fullList = AWTCSVDataSource.dataSourceForPath(file.name, this).fetchObjects();
        rootList = AWTCSVDataSource.computeOutlineList (fullList, "children", "Level");
        layout=layouts()[1];
        optionsChanged();
        return null;
    } 

    def showFilterPanel () {
        def panel = pageWithName("FilterPanel");
        panel.init(fullList, ["Supplier", "Region", "Year"], { fullList = it;
            rootList = AWTCSVDataSource.computeOutlineList (fullList, "children", "Level");
            return pageComponent();
        });
        panel.setClientPanel(true);
        return panel;
    }

    def hasChildren () {
        def children = displayGroup.currentItem().children;
        return children != null && children.size() != 0;
    }

    def answerColumnName () {
        return displayGroup.currentItem().Answer ? "Answer" : null;
    }

    def isQuestion () { return displayGroup.currentItem().Type == "Question" }
    def isItem () { return displayGroup.currentItem().Type == "Item" }

    def isColorBlank () { def it = displayGroup.currentItem()
        return !isItem() || !displayGroup.currentItem().Color }


    def showBigFlag () {
        // show only for items, and when on column edge
        return isItem() && AWTDataTable.currentInstance(this).pivotState().columnAttributes().find {
                 it == "BigFlag"
        }
    }

    def optionsChanged () {
        layoutChangeLatch = true;
        return null;
    }

    def rowClass () {
        def type = displayGroup.currentItem().Type;
        return (type == "Section") ?  "tableRowL1" : "tableRowL2";
    }

    def showFlag () {
        return displayGroup.currentItem().ExtendedPrice > 20000;
    }

    def CompetitionAttributes = ["Price", "ExtendedPrice"];
    def filteredAttributeColumns () {
        // if we're filtering column attrs (to show only CompetitionAttributes
        // And we're on a column that we should filter (any but for Supplier Best, in this case)..
        if (filteringColumns && displayGroup.currentItem().Supplier != "Best") {
            return AWTDataTable.currentInstance(this).pivotState().columnAttributes().findAll {
                CompetitionAttributes.contains(it.keyPathString())
            }
        }
        // null means use the default set
        return null;
    }

    /* Requirements and Attachments.  These multivalued attributes are encoded semi-colon delimited in the column */
    def requirements () {
        def str = displayGroup.currentItem().Requirements;
        return (!str || str=="") ? null : str.tokenize(";");
    }
    def curRequirement;

    def attachments () {
        def str = displayGroup.currentItem().Attachments;
        return (!str || str=="") ? null : str.tokenize(";");
    }
    def curAttachment;

    def collapseSingleColumnLevel () {
        // Don't collapse ColumnAttributes (e.g. "Price").
        // Do collapse Null fields, or singleton "Supplier" field
        def ps = AWTDataTable.currentInstance(this).pivotState();
        // println "collapseSingleColumnLevel(${ps.collapseCheckColumnKey()}, ${ps.collapseCheckMemberCount()}";
        return ps.collapseCheckColumnKey() != null && (ps.collapseCheckMemberCount() == 0 || ps.collapseCheckColumnKey() == "Supplier");
    }

    // Supplier sort ordering
    def supplierSortOrdering = new SupplierSorter();
}

/*
   Custom SortOrdering for Supplier which always puts Best, Initial, Historical
   before the "real" suppliers.  Also shows that we can override actual field/value
   used for sorting / grouping to be different than the AWTColumn "key" or the property
   displayed -- we use whatever is in getSortValue()
 */
class SupplierSorter extends AWTSortOrdering
{
    def RankBySupplier = ["Best" : 1, "Initial":2, "Historical":3];
    public SupplierSorter () { super(AWTSortOrdering.CompareAscending); }

    int rank (val) {
        if (!val) return 0;
        def rank = RankBySupplier[val];
        return (!rank) ? 10 : rank;
    }

    protected Object getSortValue(Object o) { return o.Supplier; }

    public int compareValues(Object v1, Object v2)
    {
        int res = rank(v1) - rank(v2);
        return handleOrdering((res != 0) ? res : super.compareValues(v1, v2));
    }
}
