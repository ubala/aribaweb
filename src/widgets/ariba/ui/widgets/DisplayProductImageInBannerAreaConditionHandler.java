package ariba.ui.widgets;

import ariba.ui.aribaweb.core.AWRequestContext;
import ariba.ui.aribaweb.util.AWImageInfo;

/**
    See HideBannerImageConditionHandler 
*/
public class DisplayProductImageInBannerAreaConditionHandler extends ConditionHandler
{
    public boolean evaluateCondition (AWRequestContext requestContext)
    {
        boolean displayProductImageInBannerArea = false;
        AWImageInfo imageInfo = HideBannerImageConditionHandler.productImageBrandedOnly(requestContext);
        if (imageInfo != null) {
            displayProductImageInBannerArea = isImageWide(imageInfo);
        }
        return displayProductImageInBannerArea;
    }

    public static boolean isImageWide (AWImageInfo imageInfo)
    {
        return imageInfo.width > 250;
    }

}
