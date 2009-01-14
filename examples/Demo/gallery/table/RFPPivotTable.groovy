package gallery.table

import ariba.ui.aribaweb.core.AWComponent

import ariba.ui.aribaweb.core.*;
import ariba.ui.widgets.*;
import ariba.ui.table.*;
import ariba.ui.outline.*;
import ariba.util.core.*;

class RFPPivotTable extends AWComponent
{
    def displayGroup, layoutChangeLatch, iter;
    def showSelection = false, filteringColumns = false;
    def files=[
        [
          name:"OutlineData-RFP.csv",
          layouts:[
            [name:"By Supplier (P)", c:["Supplier"], r:["Item"], a:["Price"]]
          ]
        ]
    ];
    def file = files[0];
    def layouts () { return file.layouts; }
    def layout=layouts()[0];

    def rootList, fullList;

    public boolean isStateless() { false }

    void init () {
        updateFile();
    }

    def updateFile () {
        // read CSV and then convert to nested tree by interpreting Level field
        fullList = AWTCSVDataSource.dataSourceForPath(file.name, this).fetchObjects();
        rootList = AWTCSVDataSource.computeOutlineList (fullList, "children", "Level");
        layout=layouts()[0];
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

    def overrideColumnName () {
        def item = displayGroup.currentItem();
        return item.Type == "Question" || item.Type == "Section" ?  "Answer" : null;
    }

    def isQuestion () { return displayGroup.currentItem().Type == "Question" }
    def isItem () { return displayGroup.currentItem().Type == "Item" }

    def optionsChanged () {
        layoutChangeLatch = true;
        return null;
    }

    def rowClass () {
        def type = displayGroup.currentItem().Type;
        return (type == "Section") ?  "tableRowL1" : "tableRowL2";
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
    def buyerAttachments () {
        def str = displayGroup.currentItem().BuyerAttachments;
        return (!str || str=="") ? null : str.tokenize(";");
    }
    def curAttachment;

    def currentAttachmentDescription ()
    {
        def matcher = curAttachment =~ /(.*)\[(.*)\]\s*/
        return matcher.matches() ? matcher[0][1] : curAttachment;
    }

    def currentAttachmentFilename ()
    {
        def matcher = curAttachment =~ /(.*)\[(.*)\]\s*/
        return matcher.matches() ? matcher[0][2] : curAttachment;
    }

    def _IconsByExtension = [ ppt:"icn_ppt_nb.gif", xls:"icn_xls_nb.gif", doc:"icn_word_nb.gif", pdf:"icn_pdf_nb.gif", zip:"icn_document.gif" ];
    def currentAttachmentIcon ()
    {
        def matcher = curAttachment =~ /(.*)\[(.*\.(.+))\]\s*/
        return matcher.matches() ? _IconsByExtension[matcher[0][3]] : null;
    }

    def collapseSingleColumnLevel () {
        // Don't collapse ColumnAttributes (e.g. "Price").
        // Do collapse Null fields, or singleton "Supplier" field
        def ps = AWTDataTable.currentInstance(this).pivotState();
        // logger().debug("collapseSingleColumnLevel(${ps.collapseCheckColumnKey()}, ${ps.collapseCheckMemberCount()}");
        return ps.collapseCheckColumnKey() != null && (ps.collapseCheckMemberCount() == 0 || ps.collapseCheckColumnKey() == "Supplier");
    }

    // Supplier sort ordering
    def supplierSortOrdering = new CustomSupplierSorter();
}

/*
   Custom SortOrdering for Supplier which always puts Best, Initial, Historical
   before the "real" suppliers.  Also shows that we can override actual field/value
   used for sorting / grouping to be different than the AWTColumn "key" or the property
   displayed -- we use whatever is in getSortValue()
 */
class CustomSupplierSorter extends AWTSortOrdering
{
    def RankBySupplier = ["Best" : 1, "Initial":2, "Historical":3];
    public CustomSupplierSorter () { super(AWTSortOrdering.CompareAscending); }

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
