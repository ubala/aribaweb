package app;

import ariba.ui.table.AWTDisplayGroup;
import ariba.ui.table.AWTDataSource;
import ariba.ui.meta.persistence.*;
import ariba.ui.meta.core.Context;
import ariba.ui.meta.core.MetaContext;
import ariba.ui.meta.core.UIMeta;
import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.widgets.ChooserState;
import ariba.ui.widgets.ChooserSelectionSource;
import ariba.appcore.User;
import ariba.util.core.*;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

public class UserTable extends AWComponent
{
	public boolean isStateless() { return false; }
    public AWTDisplayGroup displayGroup;
    public List<User> users;
    public User currentUser;

	public void init()
    {
        super.init();
        users = ObjectContext.get().executeQuery(new QuerySpecification(User.class.getName()));        
    }
}
