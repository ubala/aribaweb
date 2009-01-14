package masterdetail

import ariba.ui.aribaweb.core.AWComponent

class UserListEditorControl extends AWComponent
{
    public boolean isStateless() { false }

    def user
    
    def showUserEditor() {
        def panel = pageWithName("UserListEditorPanel");
        panel.users = users();
        panel.setClientPanel(true);

        return panel;
    }

    def users () {
        List list = valueForBinding("list");
        if (list.size() == 0) {
            list.add([:]);
        }
        return list;
    }
}
