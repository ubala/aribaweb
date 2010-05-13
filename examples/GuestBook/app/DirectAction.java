package app;

import ariba.ui.aribaweb.core.AWDirectAction;
import ariba.ui.aribaweb.core.AWResponseGenerating;
import ariba.ui.aribaweb.core.AWResponse;
import ariba.ui.aribaweb.core.AWRequest;
import ariba.ui.aribaweb.util.AWContentType;
import ariba.util.core.HTTP;
import ariba.util.core.ListUtil;
import java.util.List;

public class DirectAction extends AWDirectAction
{
    public static final String PostsAction = "posts";

    public AWResponseGenerating postsAction ()
    {
        AWRequest request = request();
        AWResponse response = application().createResponse(request());
        response.setContentType(AWContentType.TextXml);
        if ("GET".equals(request.method())) {
            String pi = request.formValueForKey("pi");
            List posts = PostService.getPosts();
            if (pi != null) {
                int postIndex = Integer.parseInt(pi);
                posts = ListUtil.list(posts.get(postIndex));
            }
            PostContent postContent =
                (PostContent)pageWithName(PostContent.class.getName());
            postContent.setPosts(posts);
            postContent.generateResponse(response, requestContext());
        }
        else {
            response.setStatus(HTTP.CodeNotImplemented);
            response.appendContent("<Status>Not Implemented</Status>");
        }
        return response; 
    }

}
