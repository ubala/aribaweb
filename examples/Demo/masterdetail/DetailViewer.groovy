package masterdetail

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.widgets.Confirmation

class DetailViewer extends AWComponent
{
    public boolean isStateless() { false }

    def panel1Id, panel2Id, errorKey;

    def attendees () {
        def item = valueForBinding("currentItem");
        if (item.Attendees == null) { item.Attendees = []; }
        return item.Attendees;
    }

    def showPanel1() {
        return Confirmation.showConfirmation(requestContext(), panel1Id);
    }

    def showPanel2() {
        return Confirmation.showConfirmation(requestContext(), panel2Id);
    }

    def itemize () {
        def p = pageWithName("HotelPanel");
        p.setClientPanel(true);
        return p;
    }
}
