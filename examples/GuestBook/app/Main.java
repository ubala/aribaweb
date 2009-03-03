package app;

import ariba.ui.aribaweb.core.AWComponent;
import java.util.ArrayList;
import java.util.List;

public class Main extends AWComponent {
	public List _posts = new ArrayList();
	public Post _newPost = new Post();
	public Post _currentPost;
	
	public void add () {
		if (errorManager().checkErrorsAndEnableDisplay()) return;
		_posts.add(_newPost);
		_newPost = new Post();
	}
	
	public void deleteCurrent () {
		_posts.remove(_currentPost);
	}  
}
