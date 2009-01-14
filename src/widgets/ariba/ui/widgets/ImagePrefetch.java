package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWComponent;
import ariba.ui.aribaweb.core.AWSession;
import ariba.util.core.ListUtil;

import java.util.List;

public class ImagePrefetch extends AWComponent
{
    private static String[] WidgetsImages =
    {
        "cssBtn.gif",
        "cssBtnApprove.gif",
        "cssBtnApproveOver.gif",
        "cssBtnBrand.gif",
        "cssBtnBrandOver.gif",
        "cssBtnDeny.gif",
        "cssBtnDenyOver.gif",
        "cssBtnHilite.gif",
        "cssBtnHiliteOver.gif",
        "cssBtnOver.gif",
        "cssBtnSpecial2.gif",
        "cssBtnSpecial2Over.gif",
        "cssBtnTotal.gif",
        "cssBtnTotalOver.gif",
        "cssBtnTab.gif",
        "cssBtnTabOver.gif",
    };

    private static final String IsImagePretched = "IsImagePretched";

    public static List Images;
    public String currentImage;

    static {
        Images = ListUtil.list();
        ListUtil.addToCollection(Images, WidgetsImages);
    }

    public static void addImage (String filename)
    {
        Images.add(filename);
    }

    public static void addImages (String[] filenames)
    {
        ListUtil.addToCollection(Images, filenames);
    }

    public boolean imagePretched ()
    {
        // if we don't have a session, then pretend images have been fetched
        AWSession session = session(false);
        return session == null || session().dict().get(IsImagePretched) != null;
    }

    public void setPreteched ()
    {
        session().dict().put(IsImagePretched, "1");        
    }

}
