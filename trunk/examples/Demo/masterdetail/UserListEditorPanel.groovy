package masterdetail

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.widgets.ChooserSelectionSource
import ariba.ui.widgets.ChooserState

class UserListEditorPanel extends AWComponent
{
    public boolean isStateless() { false }

    def users, user;
    def testDate = new ariba.util.core.Date();
    ChooserState companyChooserState;
    ChooserSelectionSource companySource = new ChooserSelectionSource.ListSource(
        ('a'..'z').inject([]) { l, k -> (1..9).each { l += "${k}${it}".toString() }; l }, null);

    def okAction ()
    {
        // show some page -- just returning current page in this example
        return (errorManager().checkErrorsAndEnableDisplay()) ? null : pageComponent();
    }

    def add () {
        users.add([:]);
        return null;
    }

    def massAdd () {
        (1..40).each { users.add([Name: "Joe${it}", Title: "Employee", Company: "Ariba"]) }
        return null;
    }
}
