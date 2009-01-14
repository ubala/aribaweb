package gallery.table

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.widgets.*;
import ariba.ui.table.*
import ariba.util.core.*
import ariba.util.fieldvalue.*

class AdvancedTable extends AWComponent
{
    def isMultiSelect = true
    def showSelectionControl = true
    def layoutChanged = false
    def item
    def list
    def menuId
    AWTDisplayGroup displayGroup
    Integer rowNum
    def draggedItem
    def descMinWidth
    int intZero = 0
    def users

    public boolean isStateless() { false }
    
    void init () {
        // initialize the display group and fetch objects ourselves so we can pre-select all
        list = AWTCSVDataSource.dataSourceForPath("SampleSpend.csv", this).fetchObjects()

        URL url = ariba.ui.table.ResourceLocator.urlForRelativePath("Users.xml", this);
        users = ListUtil.arrayToList(FieldValue.getFieldValue(XMLUtil.document(url, false, false, null).documentElement, "children"));

        displayGroup = new ariba.ui.table.AWTDisplayGroup()
    }

    def checkDescMinWidth () {
        if (displayGroup.currentItemExtras().moved) {
            descMinWidth = 300
        }
    }

    def itemDragged () {
        draggedItem = displayGroup.currentItem()
        return null
    }

    def itemDropped () {
        def v = displayGroup.allObjects()
        ListUtil.removeElementIdentical(v, draggedItem)
        displayGroup.setObjectArray(v)
        return null
    }

    def gotoRow () {
        if (rowNum) {
            def list = displayGroup.filteredObjects()
            def item = list.get(Math.min(rowNum, list.size()-1))
            println "Row (${rowNum}): ${}item"
            // displayGroup.checkObjectArray(list)
            displayGroup.setItemToForceVisible(item)
            displayGroup.setSelectedObject(item) // highlight it
        }
        return null
    }

    def inspectClicked () {
        def item = displayGroup.selectedObject()
        ariba.ui.widgets.AribaPageContent.setMessage("You last inspected: " + item,session())
        return null
    }

    def simulateMove () {
        def e = displayGroup.selectedObjects().iterator()
        while (e.hasNext()) {
            displayGroup.extrasForItem(e.next()).moved = true
        }
        displayGroup.clearSelection()
        return null
    }

    def simulateDelete () {
        def v = displayGroup.allObjects()
        def e = displayGroup.selectedObjects().iterator()
        while (e.hasNext()) {
            ListUtil.removeElementIdentical(v, e.next())
        }
        displayGroup.setObjectArray(v)
        return null
    }

    def currentNotMoved () {
        return !displayGroup.currentItemExtras().moved
    }

    def setTableConfig (config)
    {
        println "Table Config: ${config}"
        session().dict().tableConf = config
    }

    def tableConfig ()
    {
        try {
            return session().dict().tableConf
        } catch (e) {
        }
        return null
    }
}
