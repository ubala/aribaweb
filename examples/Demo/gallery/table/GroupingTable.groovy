package gallery.table

import ariba.ui.aribaweb.core.AWComponent

class GroupingTable extends AWComponent
{
    def item = null;
    boolean showDetails = false;

    public boolean isStateless() { false }

    def toggleDetails () {
        showDetails = !showDetails;
        return null;
    }
}
