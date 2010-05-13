package app;

import ariba.util.core.Fmt;
import ariba.util.core.ListUtil;
import ariba.ui.aribaweb.util.AWChangeNotifier;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Date;

public class PostService
{
    private static List Posts = initialPosts();
    private static List<AWChangeNotifier> ChangeNotifiers =
        ListUtil.list();

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

    public static void registerChangeListener
        (AWChangeNotifier changeNotifier) {
        ChangeNotifiers.add(changeNotifier);
    }

    public static void addPost (Post post) {
        Posts.add(post);
        notifyChange();
    }

    public static void removePost (Post post) {
        Posts.remove(post);
        notifyChange();
    }

    public static List getPosts () {
        return ListUtil.cloneList(Posts);
    }

    private static void notifyChange () {
        for (AWChangeNotifier notifier : ChangeNotifiers) {
            notifier.notifyChange();
        }
    }
}
