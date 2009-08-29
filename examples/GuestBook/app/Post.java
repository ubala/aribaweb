package app;

import ariba.ui.meta.annotations.Trait;
import ariba.ui.meta.annotations.Property;
import ariba.ui.meta.annotations.Action;
import ariba.ui.meta.annotations.NavModuleClass;
import ariba.util.core.Fmt;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;

@NavModuleClass
public class Post {

    @Trait.Required
    @Property.Label("Name")
    @Property.Editable("${properties.editing && value!='admin'}")    
    public String userName;

    @Property.Valid("${object.isValidBirthday}")
    public Date birthday;

    @Trait.LabelField
    public String title;

    @Trait.RichText
    @Property.Visible("${properties.editing || !object.isPrivate}")
    public String comment;

    public int rating = 3;

    public boolean isPrivate;

    @Trait.Required
    public Continent continent;

    public Object isValidBirthday () {
        if (birthday == null || birthday.before(new java.util.Date())) {
            return true;
        }
        return "Birthday cannot be in the future";        
    }

    @Action(message="isPrivate set to %s")
    public boolean toggleIsPrivate () {
        isPrivate = !isPrivate;
        return isPrivate;        
    }

    public static List Posts = initialPosts();

    private static List initialPosts () {
        List posts = new ArrayList();
        for (Continent continent : EnumSet.allOf(Continent.class)) {
            addInitialPost(posts, continent);
        }
        return posts;
    }

    private static void addInitialPost (List posts, Continent continent) {
        Post post = new Post();
        post.userName = "admin";
        post.birthday = new Date();
        post.title = Fmt.S("First %s Post", continent);
        post.comment = Fmt.S("Hello %s!", continent);
        post.continent = continent;
        posts.add(post);
    }

    public static List postsForContinent (Continent continent) {
        List postsForContinent = new ArrayList();
        for (int i = 0; i < Posts.size(); i++) {
            Post post = (Post)Posts.get(i);
            if (post.continent == continent) {
                postsForContinent.add(post);
            }
        }
        return postsForContinent;
    }
}
