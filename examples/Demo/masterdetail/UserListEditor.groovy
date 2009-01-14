package masterdetail

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.widgets.Confirmation

class UserListEditor extends AWComponent
{
    public boolean isStateless() { false }

    def panelId = null;

    def showUserEditor() {
        return Confirmation.showConfirmation(requestContext(), panelId);
    }

    def okAction ()
    {
        // hide the panel so if the current page is redisplayed the
        // confirmation will be closed
        Confirmation.hideConfirmation(requestContext());

        // show some page -- just returning current page in this example
        return null;
    }

    def users () {
        List list = valueForBinding("list");
        if (list.size() == 0) {
            list.add(new java.util.HashMap());
        }
        return list;
    }

    def add () {
        List list = users();
        list.add(new java.util.HashMap());
        return null;
    }
}
