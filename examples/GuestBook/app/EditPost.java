package app;

import ariba.ui.aribaweb.core.AWComponent;

public class EditPost extends AWComponent
{
    public Post _post;

    public EditPost ()
    {
        System.out.println("Creating EditPost component!");
    }

    public void setPost (Post post) {
        _post = post;
    }
}
