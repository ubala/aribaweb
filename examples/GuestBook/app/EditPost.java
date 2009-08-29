package app;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.util.AWUtil;

public class EditPost extends AWComponent
{
    public Post _post;
    public String _operation = "edit";

    public String title ()
    {
        return AWUtil.decamelize(_operation, ' ', true) + " Post";
    }

    public EditPost ()
    {
        System.out.println("Creating EditPost component!");
    }

    public void setPost (Post post) {
        _post = post;
    }

}
