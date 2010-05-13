package app;

import ariba.ui.aribaweb.core.AWComponent;

import java.util.List;

public class PostContent extends AWComponent
{
    private List _posts;
    public Post _currPost;

    public boolean shouldCachePage () {
        return false;
    }

    protected boolean shouldValidateSession () {
        return false;
    }

    public void setPosts (List posts) {
        _posts = posts;
    }

    public List posts () {
        return _posts;
    }
}
